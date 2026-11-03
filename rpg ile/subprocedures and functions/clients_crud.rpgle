**free
// ---------------------------------------------------------------------
// CLIESRV - Client CRUD service module (NOMAIN)
//
// Business logic formerly in program CLIECRUD. No DSPLY: every exported
// procedure reports its outcome through a cliResponse_t parameter
// (status code + message + client image) and returns the status code.
//
// Validation chain (unchanged from CLIECRUD):
//   create          : dniIsValid -> not dniExists
//   read/update/del : dniIsValid -> dniExists -> clientActive
//
// Spec: docs/modernization/cliecrud-functional-spec.md
// ---------------------------------------------------------------------
ctl-opt nomain option(*srcstmt:*nodebugio);

dcl-f trclie usage(*update:*output) keyed usropn;

/include clients_crud_h.rpgleinc

// ---- Internal prototypes ------------------------------------------------
dcl-pr dniIsValid ind;
  dni zoned(8:0) const;
end-pr;

dcl-pr dniExists ind;
  dni zoned(8:0) const;
end-pr;

dcl-pr clientActive ind;
  dni zoned(8:0) const;
end-pr;

// ---- Messages (texts kept from CLIECRUD for parity) ---------------------
dcl-c MSG_DNI_INVALID   'DNI not valid';
dcl-c MSG_DNI_EXISTS    'DNI already exists';
dcl-c MSG_DNI_NOT_FOUND 'DNI not exists';
dcl-c MSG_DELETED       'Client marked as deleted';
dcl-c MSG_LOCKED        'Client record is locked by another job';
dcl-c MSG_CREATED       'Client created';
dcl-c MSG_READ          'Client found';
dcl-c MSG_UPDATED       'Client updated';
dcl-c MSG_DELETED_OK    'Client deleted';

dcl-c LOW_TS z'0001-01-01-00.00.00.000000';

// ---- Globals ------------------------------------------------------------
// Positions 254-263 of the PSDS: user name of the job (CLIECRUD source)
dcl-ds pgmStatus psds qualified;
  jobUser char(10) pos(254);
end-ds;

// =========================================================================
// Exported RPG-native API
// =========================================================================

dcl-proc createClient export;
  dcl-pi *n int(10);
    dni      zoned(8:0) const;
    name     char(30) const;
    response likeds(cliResponse_t);
    actor    char(10) const options(*nopass:*omit);
  end-pi;
  dcl-s actorPtr pointer inz(*null);

  if %parms() >= %parmnum(actor);
    actorPtr = %addr(actor);
  endif;

  clear response;
  if not ensureOpen(response);
    return response.status;
  endif;
  if not dniIsValid(dni);
    return setResult(response: CLI_DNI_INVALID: MSG_DNI_INVALID);
  endif;
  if dniExists(dni);
    return setResult(response: CLI_DNI_EXISTS: MSG_DNI_EXISTS);
  endif;

  clearFields();
  cldni  = dni;
  clname = name;
  clcrts = %timestamp();
  clcrus = auditUser(actorPtr);

  monitor;
    write retrclie;
  on-error 01021;
    return setResult(response: CLI_DNI_EXISTS: MSG_DNI_EXISTS);
  on-error;
    return setResult(response: CLI_IO_ERROR: 'Write to TRCLIE failed');
  endmon;

  loadClient(response.client);
  return setResult(response: CLI_OK: MSG_CREATED);
end-proc;

dcl-proc readClient export;
  dcl-pi *n int(10);
    dni      zoned(8:0) const;
    response likeds(cliResponse_t);
  end-pi;

  clear response;
  if not ensureOpen(response);
    return response.status;
  endif;
  if not checkActiveClient(dni: response);
    return response.status;
  endif;

  chain(n) dni trclie;
  if not %found(trclie);
    return setResult(response: CLI_DNI_NOT_FOUND: MSG_DNI_NOT_FOUND);
  endif;
  if cldeus <> *blanks;
    return setResult(response: CLI_DELETED: MSG_DELETED);
  endif;

  loadClient(response.client);
  return setResult(response: CLI_OK: MSG_READ);
end-proc;

dcl-proc updateClient export;
  dcl-pi *n int(10);
    dni      zoned(8:0) const;
    name     char(30) const;
    response likeds(cliResponse_t);
    actor    char(10) const options(*nopass:*omit);
  end-pi;
  dcl-s actorPtr pointer inz(*null);

  if %parms() >= %parmnum(actor);
    actorPtr = %addr(actor);
  endif;

  clear response;
  if not ensureOpen(response);
    return response.status;
  endif;
  if not checkActiveClient(dni: response);
    return response.status;
  endif;
  if not lockActiveClient(dni: response);
    return response.status;
  endif;

  clname = name;
  clupts = %timestamp();
  clupus = auditUser(actorPtr);

  monitor;
    update retrclie %fields(clname: clupts: clupus);
  on-error;
    unlock trclie;
    return setResult(response: CLI_IO_ERROR: 'Update of TRCLIE failed');
  endmon;

  loadClient(response.client);
  return setResult(response: CLI_OK: MSG_UPDATED);
end-proc;

dcl-proc deleteClient export;
  dcl-pi *n int(10);
    dni      zoned(8:0) const;
    response likeds(cliResponse_t);
    actor    char(10) const options(*nopass:*omit);
  end-pi;
  dcl-s actorPtr pointer inz(*null);

  if %parms() >= %parmnum(actor);
    actorPtr = %addr(actor);
  endif;

  clear response;
  if not ensureOpen(response);
    return response.status;
  endif;
  if not checkActiveClient(dni: response);
    return response.status;
  endif;
  if not lockActiveClient(dni: response);
    return response.status;
  endif;

  // Soft delete: the record stays on file, stamped with CLDETS/CLDEUS
  cldets = %timestamp();
  cldeus = auditUser(actorPtr);

  monitor;
    update retrclie %fields(cldets: cldeus);
  on-error;
    unlock trclie;
    return setResult(response: CLI_IO_ERROR: 'Update of TRCLIE failed');
  endmon;

  loadClient(response.client);
  return setResult(response: CLI_OK: MSG_DELETED_OK);
end-proc;

// =========================================================================
// Exported SQL adapters (flat parameters for DB2 external procedures)
// =========================================================================

dcl-proc clientCreateSql export;
  dcl-pi *n;
    dni        zoned(8:0) const;
    name       char(30) const;
    actor      char(10) const;
    oStatus    int(10);
    oMessage   varchar(80);
    oName      char(30);
    oCreatedTs timestamp;
    oCreatedBy char(10);
    oUpdatedTs timestamp;
    oUpdatedBy char(10);
    oDeletedTs timestamp;
    oDeletedBy char(10);
    oActive    char(1);
  end-pi;
  dcl-ds response likeds(cliResponse_t);

  createClient(dni: name: response: actor);
  toSqlOutput(response: oStatus: oMessage: oName: oCreatedTs: oCreatedBy
             : oUpdatedTs: oUpdatedBy: oDeletedTs: oDeletedBy: oActive);
end-proc;

dcl-proc clientReadSql export;
  dcl-pi *n;
    dni        zoned(8:0) const;
    oStatus    int(10);
    oMessage   varchar(80);
    oName      char(30);
    oCreatedTs timestamp;
    oCreatedBy char(10);
    oUpdatedTs timestamp;
    oUpdatedBy char(10);
    oDeletedTs timestamp;
    oDeletedBy char(10);
    oActive    char(1);
  end-pi;
  dcl-ds response likeds(cliResponse_t);

  readClient(dni: response);
  toSqlOutput(response: oStatus: oMessage: oName: oCreatedTs: oCreatedBy
             : oUpdatedTs: oUpdatedBy: oDeletedTs: oDeletedBy: oActive);
end-proc;

dcl-proc clientUpdateSql export;
  dcl-pi *n;
    dni        zoned(8:0) const;
    name       char(30) const;
    actor      char(10) const;
    oStatus    int(10);
    oMessage   varchar(80);
    oName      char(30);
    oCreatedTs timestamp;
    oCreatedBy char(10);
    oUpdatedTs timestamp;
    oUpdatedBy char(10);
    oDeletedTs timestamp;
    oDeletedBy char(10);
    oActive    char(1);
  end-pi;
  dcl-ds response likeds(cliResponse_t);

  updateClient(dni: name: response: actor);
  toSqlOutput(response: oStatus: oMessage: oName: oCreatedTs: oCreatedBy
             : oUpdatedTs: oUpdatedBy: oDeletedTs: oDeletedBy: oActive);
end-proc;

dcl-proc clientDeleteSql export;
  dcl-pi *n;
    dni        zoned(8:0) const;
    actor      char(10) const;
    oStatus    int(10);
    oMessage   varchar(80);
    oName      char(30);
    oCreatedTs timestamp;
    oCreatedBy char(10);
    oUpdatedTs timestamp;
    oUpdatedBy char(10);
    oDeletedTs timestamp;
    oDeletedBy char(10);
    oActive    char(1);
  end-pi;
  dcl-ds response likeds(cliResponse_t);

  deleteClient(dni: response: actor);
  toSqlOutput(response: oStatus: oMessage: oName: oCreatedTs: oCreatedBy
             : oUpdatedTs: oUpdatedBy: oDeletedTs: oDeletedBy: oActive);
end-proc;

// =========================================================================
// Validation functions (business rules unchanged from CLIECRUD)
// =========================================================================

// DNI must have 7 or 8 digits: 1,000,000 - 99,999,999
dcl-proc dniIsValid;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  return (dni > 999999 and dni < 100000000);
end-proc;

// A record exists for the DNI (active or soft-deleted)
dcl-proc dniExists;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  chain(n) dni trclie;
  return %found(trclie);
end-proc;

// Client is active when the delete user (CLDEUS) is blank
dcl-proc clientActive;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  chain(n) dni trclie;
  if %found(trclie);
    return (cldeus = *blanks);
  endif;
  return *off;
end-proc;

// =========================================================================
// Internal helpers
// =========================================================================

// Read/update/delete chain: dniIsValid -> dniExists -> clientActive
dcl-proc checkActiveClient;
  dcl-pi *n ind;
    dni      zoned(8:0) const;
    response likeds(cliResponse_t);
  end-pi;

  if not dniIsValid(dni);
    setResult(response: CLI_DNI_INVALID: MSG_DNI_INVALID);
    return *off;
  endif;
  if not dniExists(dni);
    setResult(response: CLI_DNI_NOT_FOUND: MSG_DNI_NOT_FOUND);
    return *off;
  endif;
  if not clientActive(dni);
    setResult(response: CLI_DELETED: MSG_DELETED);
    return *off;
  endif;
  return *on;
end-proc;

// Chain for update and re-check the soft-delete flag under the lock
dcl-proc lockActiveClient;
  dcl-pi *n ind;
    dni      zoned(8:0) const;
    response likeds(cliResponse_t);
  end-pi;

  monitor;
    chain dni trclie;
  on-error 01218;
    setResult(response: CLI_RECORD_LOCKED: MSG_LOCKED);
    return *off;
  endmon;

  if not %found(trclie);
    setResult(response: CLI_DNI_NOT_FOUND: MSG_DNI_NOT_FOUND);
    return *off;
  endif;
  if cldeus <> *blanks;
    unlock trclie;
    setResult(response: CLI_DELETED: MSG_DELETED);
    return *off;
  endif;
  return *on;
end-proc;

dcl-proc ensureOpen;
  dcl-pi *n ind;
    response likeds(cliResponse_t);
  end-pi;

  if %open(trclie);
    return *on;
  endif;
  monitor;
    open trclie;
  on-error;
    setResult(response: CLI_IO_ERROR: 'Unable to open TRCLIE');
    return *off;
  endmon;
  return *on;
end-proc;

// Explicit actor when passed and not blank, otherwise the job user
dcl-proc auditUser;
  dcl-pi *n char(10);
    actorPtr pointer value;
  end-pi;
  dcl-s actor char(10) based(actorPtr);

  if actorPtr <> *null;
    if actor <> *blanks;
      return actor;
    endif;
  endif;
  return pgmStatus.jobUser;
end-proc;

dcl-proc setResult;
  dcl-pi *n int(10);
    response likeds(cliResponse_t);
    status   int(10) const;
    message  varchar(80) const;
  end-pi;

  response.status  = status;
  response.message = message;
  return status;
end-proc;

dcl-proc loadClient;
  dcl-pi *n;
    client likeds(cliClient_t);
  end-pi;

  client.dni       = cldni;
  client.name      = clname;
  client.createdTs = clcrts;
  client.createdBy = clcrus;
  client.updatedTs = clupts;
  client.updatedBy = clupus;
  client.deletedTs = cldets;
  client.deletedBy = cldeus;
  client.active    = (cldeus = *blanks);
end-proc;

dcl-proc clearFields;
  cldni  = 0;
  clname = '';
  clcrts = LOW_TS;
  clcrus = '';
  clupts = LOW_TS;
  clupus = '';
  cldets = LOW_TS;
  cldeus = '';
end-proc;

dcl-proc toSqlOutput;
  dcl-pi *n;
    response   likeds(cliResponse_t) const;
    oStatus    int(10);
    oMessage   varchar(80);
    oName      char(30);
    oCreatedTs timestamp;
    oCreatedBy char(10);
    oUpdatedTs timestamp;
    oUpdatedBy char(10);
    oDeletedTs timestamp;
    oDeletedBy char(10);
    oActive    char(1);
  end-pi;

  oStatus    = response.status;
  oMessage   = response.message;
  oName      = response.client.name;
  oCreatedTs = response.client.createdTs;
  oCreatedBy = response.client.createdBy;
  oUpdatedTs = response.client.updatedTs;
  oUpdatedBy = response.client.updatedBy;
  oDeletedTs = response.client.deletedTs;
  oDeletedBy = response.client.deletedBy;
  oActive    = response.client.active;
end-proc;
