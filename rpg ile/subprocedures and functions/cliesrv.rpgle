**free
// ---------------------------------------------------------------------------
// CLIESRV - UI-agnostic client CRUD over TRCLIE (service program module).
// No console output: every outcome is returned in cliesrv_status_t.
// Build: cl ile/build_cliesrv.clle (binder source cliesrv.bnd).
// ---------------------------------------------------------------------------
ctl-opt nomain option(*srcstmt:*nodebugio);

dcl-f trclie usage(*update:*output) keyed usropn;

/copy QRPGLESRC,CLIESRV_P

dcl-ds sysds psds qualified;
  currentUser char(10) pos(254);
end-ds;

dcl-c STS_DUPKEY 1021;
dcl-c STS_LOCKED 1218;

// ---------------------------------------------------------------------------
dcl-proc createClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    name char(30) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;
  dcl-s opened ind;
  dcl-s found ind;

  clear client;
  if not dniIsValid(dni);
    setStatus(status: *off: CLIESRV_INVDNI: CLIESRV_MSG_INVDNI);
    return status;
  endif;
  if not openTrclie(opened: status);
    return status;
  endif;

  if not findClient(dni: found: status);
    // I/O error already in status
  elseif found;
    // a soft-deleted DNI still exists
    setStatus(status: *off: CLIESRV_DUPDNI: CLIESRV_MSG_DUPDNI);
  else;
    clearFields();
    cldni = dni;
    clname = name;
    clcrts = %timestamp();
    clcrus = sysds.currentUser;
    write(e) retrclie;
    if %error();
      if %status(trclie) = STS_DUPKEY;
        setStatus(status: *off: CLIESRV_DUPDNI: CLIESRV_MSG_DUPDNI);
      else;
        setIoError(status: %status(trclie));
      endif;
    else;
      loadClient(client);
      setStatus(status: *on: CLIESRV_OK: CLIESRV_MSG_CREATED);
    endif;
  endif;

  closeTrclie(opened);
  return status;
end-proc;

// ---------------------------------------------------------------------------
dcl-proc readClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;
  dcl-s opened ind;

  clear client;
  if not dniIsValid(dni);
    setStatus(status: *off: CLIESRV_INVDNI: CLIESRV_MSG_INVDNI);
    return status;
  endif;
  if not openTrclie(opened: status);
    return status;
  endif;

  if checkActive(dni: status);
    loadClient(client);
    setStatus(status: *on: CLIESRV_OK: CLIESRV_MSG_READ);
  endif;

  closeTrclie(opened);
  return status;
end-proc;

// ---------------------------------------------------------------------------
dcl-proc updateClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    name char(30) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;
  dcl-s opened ind;

  clear client;
  if not dniIsValid(dni);
    setStatus(status: *off: CLIESRV_INVDNI: CLIESRV_MSG_INVDNI);
    return status;
  endif;
  if not openTrclie(opened: status);
    return status;
  endif;

  if checkActive(dni: status) and lockActive(dni: status);
    clname = name;
    clupts = %timestamp();
    clupus = sysds.currentUser;
    update(e) retrclie;
    if %error();
      setIoError(status: %status(trclie));
    else;
      loadClient(client);
      setStatus(status: *on: CLIESRV_OK: CLIESRV_MSG_UPDATED);
    endif;
  endif;

  closeTrclie(opened);
  return status;
end-proc;

// ---------------------------------------------------------------------------
// Soft delete: stamps cldets/cldeus and keeps the record.
dcl-proc deleteClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    client likeds(cliesrv_client_t);
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;
  dcl-s opened ind;

  clear client;
  if not dniIsValid(dni);
    setStatus(status: *off: CLIESRV_INVDNI: CLIESRV_MSG_INVDNI);
    return status;
  endif;
  if not openTrclie(opened: status);
    return status;
  endif;

  if checkActive(dni: status) and lockActive(dni: status);
    cldets = %timestamp();
    cldeus = sysds.currentUser;
    update(e) retrclie;
    if %error();
      setIoError(status: %status(trclie));
    else;
      loadClient(client);
      setStatus(status: *on: CLIESRV_OK: CLIESRV_MSG_DELETED_OK);
    endif;
  endif;

  closeTrclie(opened);
  return status;
end-proc;

// ---------------------------------------------------------------------------
dcl-proc dniIsValid export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  return (dni > 999999 and dni < 100000000);
end-proc;

// ---------------------------------------------------------------------------
// *off when the DNI is not on file or TRCLIE cannot be read.
dcl-proc dniExists export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;
  dcl-s opened ind;
  dcl-s found ind;

  if not openTrclie(opened: status);
    return *off;
  endif;
  if not findClient(dni: found: status);
    found = *off;
  endif;
  closeTrclie(opened);
  return found;
end-proc;

// ---------------------------------------------------------------------------
// *on only when the DNI is on file and not marked as deleted.
dcl-proc clientActive export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;
  dcl-ds status likeds(cliesrv_status_t) inz;
  dcl-s opened ind;
  dcl-s active ind;

  if not openTrclie(opened: status);
    return *off;
  endif;
  active = checkActive(dni: status);
  closeTrclie(opened);
  return active;
end-proc;

// ---------------------------------------------------------------------------
// Internal helpers (not exported)
// ---------------------------------------------------------------------------

// Read without locking. Returns *off on I/O error (status set).
dcl-proc findClient;
  dcl-pi *n ind;
    dni zoned(8:0) const;
    found ind;
    status likeds(cliesrv_status_t);
  end-pi;

  found = *off;
  chain(ne) (dni) trclie;
  if %error();
    setIoError(status: %status(trclie));
    return *off;
  endif;
  found = %found(trclie);
  return *on;
end-proc;

// Exists -> active, in legacy order. Returns *on when the client is active;
// otherwise status holds NOTFND, DELETED or IOERR.
dcl-proc checkActive;
  dcl-pi *n ind;
    dni zoned(8:0) const;
    status likeds(cliesrv_status_t);
  end-pi;
  dcl-s found ind;

  if not findClient(dni: found: status);
    return *off;
  endif;
  if not found;
    setStatus(status: *off: CLIESRV_NOTFND: CLIESRV_MSG_NOTFND);
    return *off;
  endif;
  if cldeus <> *blanks;
    setStatus(status: *off: CLIESRV_DELETED: CLIESRV_MSG_DELETED);
    return *off;
  endif;
  return *on;
end-proc;

// Re-read the record for update (locks it) and re-check it is still active,
// in case it was changed between validation and the write.
dcl-proc lockActive;
  dcl-pi *n ind;
    dni zoned(8:0) const;
    status likeds(cliesrv_status_t);
  end-pi;

  chain(e) (dni) trclie;
  if %error();
    setIoError(status: %status(trclie));
    return *off;
  endif;
  if not %found(trclie);
    setStatus(status: *off: CLIESRV_NOTFND: CLIESRV_MSG_NOTFND);
    return *off;
  endif;
  if cldeus <> *blanks;
    unlock(e) trclie;
    setStatus(status: *off: CLIESRV_DELETED: CLIESRV_MSG_DELETED);
    return *off;
  endif;
  return *on;
end-proc;

// Open TRCLIE unless already open. opened = *on when the caller must close.
dcl-proc openTrclie;
  dcl-pi *n ind;
    opened ind;
    status likeds(cliesrv_status_t);
  end-pi;

  opened = *off;
  if %open(trclie);
    return *on;
  endif;
  open(e) trclie;
  if %error();
    setIoError(status: %status(trclie));
    return *off;
  endif;
  opened = *on;
  return *on;
end-proc;

dcl-proc closeTrclie;
  dcl-pi *n;
    opened ind const;
  end-pi;

  if opened and %open(trclie);
    close(e) trclie;
  endif;
end-proc;

dcl-proc clearFields;
  cldni = 0;
  clname = *blanks;
  clcrts = CLIESRV_TS_NONE;
  clcrus = *blanks;
  clupts = CLIESRV_TS_NONE;
  clupus = *blanks;
  cldets = CLIESRV_TS_NONE;
  cldeus = *blanks;
end-proc;

// Copy the current TRCLIE record image to the client DS.
dcl-proc loadClient;
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

dcl-proc setStatus;
  dcl-pi *n;
    status likeds(cliesrv_status_t);
    success ind const;
    code char(10) const;
    message char(52) const;
  end-pi;

  status.success = success;
  status.code = code;
  status.message = message;
end-proc;

dcl-proc setIoError;
  dcl-pi *n;
    status likeds(cliesrv_status_t);
    fileStatus zoned(5:0) const;
  end-pi;
  dcl-s detail varchar(32) inz;

  clear detail;
  if fileStatus = STS_LOCKED;
    detail = ': record locked';
  endif;
  setStatus(status: *off: CLIESRV_IOERR:
            CLIESRV_MSG_IOERR + detail
            + ' (status ' + %editc(fileStatus: 'X') + ')');
end-proc;
