**free
//==============================================================================
// CLIESRV - Client management service program (module source).
//
// Business logic extracted from CLIECRUD (clients_crud.rpgle). No DSPLY: every
// operation returns cliesrv_status_t so callers decide how to present results.
//
// Preserved business rules:
//   - DNI is valid when > 999999 and < 100000000.
//   - create : DNI valid and NOT already in TRCLIE.
//   - read/update/delete : DNI valid, exists, and active (CLDEUS = blanks).
//   - delete is a soft delete: stamps CLDETS/CLDEUS, the row is never removed.
//   - every write stamps audit fields with %timestamp() and the job's current
//     user (PSDS positions 254-263).
//   - validations run in the original order: valid -> exists -> active.
//
// Build: see cl ile/build_cliesrv.clle (CRTRPGMOD + CRTSRVPGM with cliesrv.bnd)
//==============================================================================
ctl-opt nomain option(*nodebugio:*srcstmt) actgrp(*caller);

dcl-f trclie usage(*update:*output) keyed usropn;

/copy QRPGLESRC,CLIESRV_P

dcl-ds psds psds qualified;
  currentUser char(10) pos(254);
end-ds;

dcl-c LOW_TS z'0001-01-01-00.00.00.000000';

//------------------------------------------------------------------------------
// createClient - insert a new client (DNI must be valid and not exist)
//------------------------------------------------------------------------------
dcl-proc createClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni  zoned(8:0) const;
    name char(30)   const;
  end-pi;

  if not dniIsValid(dni);
    return fail(CLIESRV_INVALID_DNI:'DNI not valid');
  endif;
  if dniExists(dni);
    return fail(CLIESRV_DUPLICATE:'DNI already exists');
  endif;

  clearFields();
  cldni  = dni;
  clname = name;
  clcrts = %timestamp();
  clcrus = psds.currentUser;
  monitor;
    write retrclie;
  on-error;
    return fail(CLIESRV_IO_ERROR:'Error writing client');
  endmon;

  return ok('Client created');
end-proc;

//------------------------------------------------------------------------------
// readClient - return the image of an active client
//------------------------------------------------------------------------------
dcl-proc readClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni    zoned(8:0) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);

  clear client;
  status = checkActive(dni);
  if not status.success;
    return status;
  endif;

  chain(n) dni trclie;
  if not %found(trclie) or cldeus <> *blanks;
    return fail(CLIESRV_NOT_FOUND:'DNI not exists');
  endif;

  client.dni       = cldni;
  client.name      = clname;
  client.createdTs = clcrts;
  client.createdBy = clcrus;
  client.updatedTs = clupts;
  client.updatedBy = clupus;
  client.deletedTs = cldets;
  client.deletedBy = cldeus;

  return ok(%char(cldni) + '-' + clname);
end-proc;

//------------------------------------------------------------------------------
// updateClient - change the name of an active client
//------------------------------------------------------------------------------
dcl-proc updateClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni  zoned(8:0) const;
    name char(30)   const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);

  status = checkActive(dni);
  if not status.success;
    return status;
  endif;

  chain dni trclie;
  if not %found(trclie);
    return fail(CLIESRV_NOT_FOUND:'DNI not exists');
  endif;
  clname = name;
  clupts = %timestamp();
  clupus = psds.currentUser;
  monitor;
    update retrclie;
  on-error;
    return fail(CLIESRV_IO_ERROR:'Error updating client');
  endmon;

  return ok('Client updated');
end-proc;

//------------------------------------------------------------------------------
// deleteClient - soft delete: stamp CLDETS/CLDEUS, keep the row
//------------------------------------------------------------------------------
dcl-proc deleteClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);

  status = checkActive(dni);
  if not status.success;
    return status;
  endif;

  chain dni trclie;
  if not %found(trclie);
    return fail(CLIESRV_NOT_FOUND:'DNI not exists');
  endif;
  cldets = %timestamp();
  cldeus = psds.currentUser;
  monitor;
    update retrclie;
  on-error;
    return fail(CLIESRV_IO_ERROR:'Error deleting client');
  endmon;

  return ok('Client deleted');
end-proc;

//------------------------------------------------------------------------------
// dniIsValid - DNI range check
//------------------------------------------------------------------------------
dcl-proc dniIsValid export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  return (dni > 999999 and dni < 100000000);
end-proc;

//------------------------------------------------------------------------------
// dniExists - row exists in TRCLIE (active or soft deleted)
//------------------------------------------------------------------------------
dcl-proc dniExists export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  openFile();
  chain(n) dni trclie;
  return %found(trclie);
end-proc;

//------------------------------------------------------------------------------
// clientActive - row exists and has not been soft deleted (CLDEUS = blanks)
//------------------------------------------------------------------------------
dcl-proc clientActive export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  openFile();
  chain(n) dni trclie;
  if %found(trclie);
    return cldeus = *blanks;
  endif;
  return *off;
end-proc;

//==============================================================================
// Internal helpers (not exported)
//==============================================================================

// Common precondition of read/update/delete: valid -> exists -> active
dcl-proc checkActive;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
  end-pi;

  if not dniIsValid(dni);
    return fail(CLIESRV_INVALID_DNI:'DNI not valid');
  endif;
  if not dniExists(dni);
    return fail(CLIESRV_NOT_FOUND:'DNI not exists');
  endif;
  if not clientActive(dni);
    return fail(CLIESRV_DELETED:'Client marked as deleted');
  endif;
  return ok('');
end-proc;

dcl-proc openFile;
  if not %open(trclie);
    open trclie;
  endif;
end-proc;

dcl-proc clearFields;
  cldni  = 0;
  clname = *blanks;
  clcrts = LOW_TS;
  clcrus = *blanks;
  clupts = LOW_TS;
  clupus = *blanks;
  cldets = LOW_TS;
  cldeus = *blanks;
end-proc;

dcl-proc ok;
  dcl-pi *n likeds(cliesrv_status_t);
    message varchar(52) const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;

  status.success = *on;
  status.code    = CLIESRV_OK;
  status.message = message;
  return status;
end-proc;

dcl-proc fail;
  dcl-pi *n likeds(cliesrv_status_t);
    code    char(10)     const;
    message varchar(52)  const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;

  status.success = *off;
  status.code    = code;
  status.message = message;
  return status;
end-proc;
