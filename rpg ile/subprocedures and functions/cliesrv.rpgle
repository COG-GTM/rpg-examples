**free
// ---------------------------------------------------------------------
// CLIESRV - UI-agnostic client CRUD over TRCLIE (*SRVPGM)
//
// Business logic extracted from the legacy CLIECRUD program. No console
// I/O: every outcome is returned in cliesrv_status_t (see CLIESRV_P).
// Build: cl ile/build_cliesrv.clle (binder source QSRVSRC,CLIESRV).
// ---------------------------------------------------------------------
ctl-opt nomain option(*srcstmt:*nodebugio);

dcl-f trclie usage(*update:*output) keyed usropn;

/copy QRPGLESRC,CLIESRV_P

dcl-ds pgmStatus psds qualified;
  currentUser char(10) pos(254);
end-ds;

dcl-c NO_TIMESTAMP z'0001-01-01-00.00.00.000000';
dcl-c MIN_DNI 999999;
dcl-c MAX_DNI 100000000;

// --- internal helpers -------------------------------------------------
dcl-pr openTrclie ind;
end-pr;

dcl-pr closeTrclie;
  openedHere ind const;
end-pr;

dcl-pr clearFields;
end-pr;

dcl-pr toClient;
  client likeds(cliesrv_client_t);
end-pr;

dcl-pr succeeded likeds(cliesrv_status_t);
  message char(52) const;
end-pr;

dcl-pr rejected likeds(cliesrv_status_t);
  code char(10) const;
  message char(52) const;
end-pr;

dcl-pr checkActiveClient likeds(cliesrv_status_t);
  dni zoned(8:0) const;
end-pr;

dcl-pr ioFailure likeds(cliesrv_status_t);
end-pr;

// =====================================================================
// createClient - DNI must be valid and must not exist yet (a soft
// deleted DNI still exists).
// =====================================================================
dcl-proc createClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    name char(30) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);
  dcl-s openedHere ind;

  clear client;
  monitor;
    openedHere = openTrclie();
    if not dniIsValid(dni);
      status = rejected(CLIESRV_INVDNI : CLIESRV_MSG_INVDNI);
    elseif dniExists(dni);
      status = rejected(CLIESRV_DUPDNI : CLIESRV_MSG_DUPDNI);
    else;
      clearFields();
      cldni = dni;
      clname = name;
      clcrts = %timestamp();
      clcrus = pgmStatus.currentUser;
      write retrclie;
      toClient(client);
      status = succeeded(CLIESRV_MSG_CREATED);
    endif;
  on-error;
    status = ioFailure();
  endmon;
  if not closeTrclie(openedHere) and status.success;
    clear client;
    status = ioFailure();
  endif;
  return status;
end-proc;

// =====================================================================
// readClient - DNI must be valid, exist and be active.
// =====================================================================
dcl-proc readClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);
  dcl-s openedHere ind;

  clear client;
  monitor;
    openedHere = openTrclie();
    status = checkActiveClient(dni);
    if status.success;
      chain(n) dni trclie;
      if not %found(trclie);
        status = rejected(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
      elseif cldeus <> *blanks;
        status = rejected(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
      else;
        toClient(client);
        status = succeeded(CLIESRV_MSG_READ);
      endif;
    endif;
  on-error;
    status = ioFailure();
  endmon;
  if not closeTrclie(openedHere) and status.success;
    clear client;
    status = ioFailure();
  endif;
  return status;
end-proc;

// =====================================================================
// updateClient - DNI must be valid, exist and be active.
// =====================================================================
dcl-proc updateClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    name char(30) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);
  dcl-s openedHere ind;

  clear client;
  monitor;
    openedHere = openTrclie();
    status = checkActiveClient(dni);
    if status.success;
      // Re-check the active marker under the record lock: another job
      // may have soft deleted the client since checkActiveClient.
      chain dni trclie;
      if not %found(trclie);
        status = rejected(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
      elseif cldeus <> *blanks;
        unlock trclie;
        status = rejected(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
      else;
        clname = name;
        clupts = %timestamp();
        clupus = pgmStatus.currentUser;
        update retrclie;
        toClient(client);
        status = succeeded(CLIESRV_MSG_UPDATED);
      endif;
    endif;
  on-error;
    status = ioFailure();
  endmon;
  if not closeTrclie(openedHere) and status.success;
    clear client;
    status = ioFailure();
  endif;
  return status;
end-proc;

// =====================================================================
// deleteClient - soft delete: stamps cldets/cldeus, keeps the record.
// =====================================================================
dcl-proc deleteClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);
  dcl-s openedHere ind;

  clear client;
  monitor;
    openedHere = openTrclie();
    status = checkActiveClient(dni);
    if status.success;
      // Re-check the active marker under the record lock: another job
      // may have soft deleted the client since checkActiveClient.
      chain dni trclie;
      if not %found(trclie);
        status = rejected(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
      elseif cldeus <> *blanks;
        unlock trclie;
        status = rejected(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
      else;
        cldets = %timestamp();
        cldeus = pgmStatus.currentUser;
        update retrclie;
        toClient(client);
        status = succeeded(CLIESRV_MSG_DELETED_OK);
      endif;
    endif;
  on-error;
    status = ioFailure();
  endmon;
  if not closeTrclie(openedHere) and status.success;
    clear client;
    status = ioFailure();
  endif;
  return status;
end-proc;

// =====================================================================
// dniIsValid - 7 or 8 significant digits.
// =====================================================================
dcl-proc dniIsValid export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  return (dni > MIN_DNI and dni < MAX_DNI);
end-proc;

// =====================================================================
// dniExists - a record with this DNI exists (active or soft deleted).
// =====================================================================
dcl-proc dniExists export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;
  dcl-s openedHere ind;
  dcl-s found ind;

  openedHere = openTrclie();
  chain(n) dni trclie;
  found = %found(trclie);
  closeTrclie(openedHere);
  return found;
end-proc;

// =====================================================================
// clientActive - the record exists and has not been soft deleted.
// =====================================================================
dcl-proc clientActive export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;
  dcl-s openedHere ind;
  dcl-s active ind;

  openedHere = openTrclie();
  chain(n) dni trclie;
  if %found(trclie);
    active = (cldeus = *blanks);
  else;
    active = *off;
  endif;
  closeTrclie(openedHere);
  return active;
end-proc;

// --- internal helpers -------------------------------------------------

// Shared read/update/delete precondition: valid -> exists -> active.
dcl-proc checkActiveClient;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
  end-pi;

  if not dniIsValid(dni);
    return rejected(CLIESRV_INVDNI : CLIESRV_MSG_INVDNI);
  endif;
  if not dniExists(dni);
    return rejected(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;
  if not clientActive(dni);
    return rejected(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
  endif;
  return succeeded(*blanks);
end-proc;

// Opens TRCLIE if needed; returns *on when this call opened it so the
// same caller closes it (the file is never left open between calls).
dcl-proc openTrclie;
  dcl-pi *n ind;
  end-pi;

  if %open(trclie);
    return *off;
  endif;
  open trclie;
  return *on;
end-proc;

// Closes TRCLIE when this caller opened it; returns *off if the close
// failed so CRUD callers can report IOERR instead of OK.
dcl-proc closeTrclie;
  dcl-pi *n ind;
    openedHere ind const;
  end-pi;

  if openedHere and %open(trclie);
    close(e) trclie;
    return not %error();
  endif;
  return *on;
end-proc;

dcl-proc clearFields;
  cldni = 0;
  clname = *blanks;
  clcrts = NO_TIMESTAMP;
  clcrus = *blanks;
  clupts = NO_TIMESTAMP;
  clupus = *blanks;
  cldets = NO_TIMESTAMP;
  cldeus = *blanks;
end-proc;

dcl-proc toClient;
  dcl-pi *n;
    client likeds(cliesrv_client_t);
  end-pi;

  client.dni = cldni;
  client.name = clname;
  client.createdTs = clcrts;
  client.createdBy = clcrus;
  client.updatedTs = clupts;
  client.updatedBy = clupus;
  client.deletedTs = cldets;
  client.deletedBy = cldeus;
end-proc;

dcl-proc succeeded;
  dcl-pi *n likeds(cliesrv_status_t);
    message char(52) const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);

  status.success = *on;
  status.code = CLIESRV_OK;
  status.message = message;
  return status;
end-proc;

dcl-proc rejected;
  dcl-pi *n likeds(cliesrv_status_t);
    code char(10) const;
    message char(52) const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t);

  status.success = *off;
  status.code = code;
  status.message = message;
  return status;
end-proc;

dcl-proc ioFailure;
  dcl-pi *n likeds(cliesrv_status_t);
  end-pi;

  return rejected(CLIESRV_IOERR
                 : CLIESRV_MSG_IOERR + ' (status '
                   + %char(%status(trclie)) + ')');
end-proc;
