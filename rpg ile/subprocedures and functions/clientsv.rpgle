**free
// ---------------------------------------------------------------------------
// CLIENTSV - client CRUD service program over TRCLIE (record RETRCLIE).
//
// Refactor of the original CLIECRUD program: same business rules, no DSPLY.
// Every outcome is returned in clientsv_out_t (see CLIENTSV_H).
//   * Guard chain for R/U/D: valid DNI -> DNI exists -> client active.
//   * Create: valid DNI -> DNI must not exist (a soft-deleted DNI exists).
//   * Delete is a soft delete: cldets/cldeus are stamped, record is kept.
// TRCLIE is opened on demand and closed again by the procedure that
// opened it, so the service program holds no file state between calls.
//
// Build: cl ile/build_clientsv.clle (binder source clientsv.bnd)
// ---------------------------------------------------------------------------
ctl-opt nomain option(*srcstmt:*nodebugio);

dcl-f trclie usage(*update:*output) keyed usropn;

/copy QRPGLESRC,CLIENTSV_H

dcl-ds pgmStatus psds qualified;
  currentUser char(10) pos(254);
end-ds;

dcl-c STS_DUPKEY 1021;
dcl-c STS_LOCKED 1218;

// ---------------------------------------------------------------------------
dcl-proc clientsvExecute export;
  dcl-pi *n;
    in likeds(clientsv_in_t) const;
    out likeds(clientsv_out_t);
  end-pi;

  select;
    when in.op = CLIENTSV_OP_CREATE;
      createClient(in: out);
    when in.op = CLIENTSV_OP_READ;
      readClient(in: out);
    when in.op = CLIENTSV_OP_UPDATE;
      updateClient(in: out);
    when in.op = CLIENTSV_OP_DELETE;
      deleteClient(in: out);
    other;
      clear out;
      setResult(out: *off: CLIENTSV_INVOP: CLIENTSV_MSG_INVOP);
  endsl;
end-proc;

// ---------------------------------------------------------------------------
dcl-proc createClient export;
  dcl-pi *n;
    in likeds(clientsv_in_t) const;
    out likeds(clientsv_out_t);
  end-pi;
  dcl-s opened ind;
  dcl-s found ind;

  clear out;
  if not dniIsValid(in.dni);
    setResult(out: *off: CLIENTSV_INVDNI: CLIENTSV_MSG_INVDNI);
    return;
  endif;
  if not openTrclie(opened: out);
    return;
  endif;

  if not findClient(in.dni: found: out);
    // I/O error already set in out
  elseif found;
    setResult(out: *off: CLIENTSV_DUPDNI: CLIENTSV_MSG_DUPDNI);
  else;
    clearFields();
    cldni = in.dni;
    clname = in.name;
    clcrts = %timestamp();
    clcrus = pgmStatus.currentUser;
    write(e) retrclie;
    if %error();
      if %status(trclie) = STS_DUPKEY;
        setResult(out: *off: CLIENTSV_DUPDNI: CLIENTSV_MSG_DUPDNI);
      else;
        setIoError(out: %status(trclie));
      endif;
    else;
      loadClient(out.client);
      setResult(out: *on: CLIENTSV_OK: CLIENTSV_MSG_CREATED);
    endif;
  endif;

  closeTrclie(opened);
end-proc;

// ---------------------------------------------------------------------------
dcl-proc readClient export;
  dcl-pi *n;
    in likeds(clientsv_in_t) const;
    out likeds(clientsv_out_t);
  end-pi;
  dcl-s opened ind;

  clear out;
  if not dniIsValid(in.dni);
    setResult(out: *off: CLIENTSV_INVDNI: CLIENTSV_MSG_INVDNI);
    return;
  endif;
  if not openTrclie(opened: out);
    return;
  endif;

  if checkActive(in.dni: out);
    loadClient(out.client);
    setResult(out: *on: CLIENTSV_OK: CLIENTSV_MSG_READ);
  endif;

  closeTrclie(opened);
end-proc;

// ---------------------------------------------------------------------------
dcl-proc updateClient export;
  dcl-pi *n;
    in likeds(clientsv_in_t) const;
    out likeds(clientsv_out_t);
  end-pi;
  dcl-s opened ind;

  clear out;
  if not dniIsValid(in.dni);
    setResult(out: *off: CLIENTSV_INVDNI: CLIENTSV_MSG_INVDNI);
    return;
  endif;
  if not openTrclie(opened: out);
    return;
  endif;

  if checkActive(in.dni: out) and lockActive(in.dni: out);
    clname = in.name;
    clupts = %timestamp();
    clupus = pgmStatus.currentUser;
    update(e) retrclie;
    if %error();
      setIoError(out: %status(trclie));
    else;
      loadClient(out.client);
      setResult(out: *on: CLIENTSV_OK: CLIENTSV_MSG_UPDATED);
    endif;
  endif;

  closeTrclie(opened);
end-proc;

// ---------------------------------------------------------------------------
dcl-proc deleteClient export;
  dcl-pi *n;
    in likeds(clientsv_in_t) const;
    out likeds(clientsv_out_t);
  end-pi;
  dcl-s opened ind;

  clear out;
  if not dniIsValid(in.dni);
    setResult(out: *off: CLIENTSV_INVDNI: CLIENTSV_MSG_INVDNI);
    return;
  endif;
  if not openTrclie(opened: out);
    return;
  endif;

  if checkActive(in.dni: out) and lockActive(in.dni: out);
    cldets = %timestamp();
    cldeus = pgmStatus.currentUser;
    update(e) retrclie;
    if %error();
      setIoError(out: %status(trclie));
    else;
      loadClient(out.client);
      setResult(out: *on: CLIENTSV_OK: CLIENTSV_MSG_REMOVED);
    endif;
  endif;

  closeTrclie(opened);
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
  dcl-ds out likeds(clientsv_out_t) inz;
  dcl-s opened ind;
  dcl-s found ind;

  if not openTrclie(opened: out);
    return *off;
  endif;
  if not findClient(dni: found: out);
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
  dcl-ds out likeds(clientsv_out_t) inz;
  dcl-s opened ind;
  dcl-s active ind;

  if not openTrclie(opened: out);
    return *off;
  endif;
  active = checkActive(dni: out);
  closeTrclie(opened);
  return active;
end-proc;

// ---------------------------------------------------------------------------
// Internal helpers (not exported)
// ---------------------------------------------------------------------------

// Chain without lock. Returns *off on I/O error (out set).
dcl-proc findClient;
  dcl-pi *n ind;
    dni zoned(8:0) const;
    found ind;
    out likeds(clientsv_out_t);
  end-pi;

  found = *off;
  chain(ne) (dni) trclie;
  if %error();
    setIoError(out: %status(trclie));
    return *off;
  endif;
  found = %found(trclie);
  return *on;
end-proc;

// Exists -> active, in legacy order. *on when the client is active,
// otherwise out holds NOTFND, DELETED or IOERR.
dcl-proc checkActive;
  dcl-pi *n ind;
    dni zoned(8:0) const;
    out likeds(clientsv_out_t);
  end-pi;
  dcl-s found ind;

  if not findClient(dni: found: out);
    return *off;
  endif;
  if not found;
    setResult(out: *off: CLIENTSV_NOTFND: CLIENTSV_MSG_NOTFND);
    return *off;
  endif;
  if cldeus <> *blanks;
    setResult(out: *off: CLIENTSV_DELETED: CLIENTSV_MSG_DELETED);
    return *off;
  endif;
  return *on;
end-proc;

// Chain for update (locks the record) and re-check it is still active,
// in case another job changed it after the validation chain.
dcl-proc lockActive;
  dcl-pi *n ind;
    dni zoned(8:0) const;
    out likeds(clientsv_out_t);
  end-pi;

  chain(e) (dni) trclie;
  if %error();
    setIoError(out: %status(trclie));
    return *off;
  endif;
  if not %found(trclie);
    setResult(out: *off: CLIENTSV_NOTFND: CLIENTSV_MSG_NOTFND);
    return *off;
  endif;
  if cldeus <> *blanks;
    unlock(e) trclie;
    setResult(out: *off: CLIENTSV_DELETED: CLIENTSV_MSG_DELETED);
    return *off;
  endif;
  return *on;
end-proc;

// Open TRCLIE unless already open; opened = *on when the caller must close.
dcl-proc openTrclie;
  dcl-pi *n ind;
    opened ind;
    out likeds(clientsv_out_t);
  end-pi;

  opened = *off;
  if %open(trclie);
    return *on;
  endif;
  open(e) trclie;
  if %error();
    setIoError(out: %status(trclie));
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
  clcrts = CLIENTSV_TS_NONE;
  clcrus = *blanks;
  clupts = CLIENTSV_TS_NONE;
  clupus = *blanks;
  cldets = CLIENTSV_TS_NONE;
  cldeus = *blanks;
end-proc;

// Copy the current RETRCLIE record image into the API client DS.
dcl-proc loadClient;
  dcl-pi *n;
    client likeds(client_t);
  end-pi;

  client.cldni = cldni;
  client.clname = clname;
  client.clcrts = clcrts;
  client.clcrus = clcrus;
  client.clupts = clupts;
  client.clupus = clupus;
  client.cldets = cldets;
  client.cldeus = cldeus;
end-proc;

dcl-proc setResult;
  dcl-pi *n;
    out likeds(clientsv_out_t);
    success ind const;
    code char(10) const;
    message char(52) const;
  end-pi;

  out.success = success;
  out.code = code;
  out.message = message;
end-proc;

dcl-proc setIoError;
  dcl-pi *n;
    out likeds(clientsv_out_t);
    fileStatus zoned(5:0) const;
  end-pi;
  dcl-s detail varchar(20) inz;

  if fileStatus = STS_LOCKED;
    detail = ': record locked';
  endif;
  setResult(out: *off: CLIENTSV_IOERR:
            CLIENTSV_MSG_IOERR + detail
            + ' (status ' + %editc(fileStatus: 'X') + ')');
end-proc;
