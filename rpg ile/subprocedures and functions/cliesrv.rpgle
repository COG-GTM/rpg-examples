**free
// CLIESRV - client management service program (UI-agnostic).
// Owns file TRCLIE; every CRUD procedure returns a cliesrv_status_t.
ctl-opt nomain option(*srcstmt:*nodebugio);

dcl-f trclie usage(*update:*output) keyed usropn;

/copy QRPGLESRC,CLIESRV_P

dcl-ds pgmStatus psds qualified;
  currentUser char(10) pos(254);
end-ds;

dcl-s lastIoStatus int(10) inz(0);

//------------------------------------------------------------------
dcl-proc createClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    name char(30) const;
    client likeds(cliesrv_client_t) options(*nopass:*omit);
  end-pi;

  if not dniIsValid(dni);
    return newStatus(*off : CLIESRV_INVDNI : CLIESRV_MSG_INVDNI);
  endif;
  if not openFile();
    return ioError();
  endif;
  if dniExists(dni);
    return newStatus(*off : CLIESRV_DUPDNI : CLIESRV_MSG_DUPDNI);
  endif;
  if lastIoStatus <> 0;
    return ioError();
  endif;

  clearFields();
  cldni = dni;
  clname = name;
  clcrts = %timestamp();
  clcrus = pgmStatus.currentUser;
  write(e) retrclie;
  if %error();
    lastIoStatus = %status(trclie);
    if lastIoStatus = 1021;
      return newStatus(*off : CLIESRV_DUPDNI : CLIESRV_MSG_DUPDNI);
    endif;
    return ioError();
  endif;

  if %parms() >= %parmnum(client);
    if %addr(client) <> *null;
      client = currentImage();
    endif;
  endif;
  return newStatus(*on : CLIESRV_OK : 'Client created');
end-proc;

//------------------------------------------------------------------
dcl-proc readClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    client likeds(cliesrv_client_t);
  end-pi;

  if not dniIsValid(dni);
    return newStatus(*off : CLIESRV_INVDNI : CLIESRV_MSG_INVDNI);
  endif;
  if not openFile();
    return ioError();
  endif;
  if not dniExists(dni);
    return rejectOrIoError(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;
  if not clientActive(dni);
    return rejectOrIoError(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
  endif;

  chain(ne) dni trclie;
  if %error();
    lastIoStatus = %status(trclie);
    return ioError();
  endif;
  if not %found(trclie);
    return newStatus(*off : CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;

  client = currentImage();
  return newStatus(*on : CLIESRV_OK : 'Client read');
end-proc;

//------------------------------------------------------------------
dcl-proc updateClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    name char(30) const;
    client likeds(cliesrv_client_t) options(*nopass:*omit);
  end-pi;

  if not dniIsValid(dni);
    return newStatus(*off : CLIESRV_INVDNI : CLIESRV_MSG_INVDNI);
  endif;
  if not openFile();
    return ioError();
  endif;
  if not dniExists(dni);
    return rejectOrIoError(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;
  if not clientActive(dni);
    return rejectOrIoError(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
  endif;

  chain(e) dni trclie;
  if %error();
    lastIoStatus = %status(trclie);
    return ioError();
  endif;
  if not %found(trclie);
    return newStatus(*off : CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;

  clname = name;
  clupts = %timestamp();
  clupus = pgmStatus.currentUser;
  update(e) retrclie;
  if %error();
    lastIoStatus = %status(trclie);
    return ioError();
  endif;

  if %parms() >= %parmnum(client);
    if %addr(client) <> *null;
      client = currentImage();
    endif;
  endif;
  return newStatus(*on : CLIESRV_OK : 'Client updated');
end-proc;

//------------------------------------------------------------------
// Soft delete: stamps CLDETS/CLDEUS, the record is kept.
dcl-proc deleteClient export;
  dcl-pi *n likeds(cliesrv_status_t);
    dni zoned(8:0) const;
    client likeds(cliesrv_client_t) options(*nopass:*omit);
  end-pi;

  if not dniIsValid(dni);
    return newStatus(*off : CLIESRV_INVDNI : CLIESRV_MSG_INVDNI);
  endif;
  if not openFile();
    return ioError();
  endif;
  if not dniExists(dni);
    return rejectOrIoError(CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;
  if not clientActive(dni);
    return rejectOrIoError(CLIESRV_DELETED : CLIESRV_MSG_DELETED);
  endif;

  chain(e) dni trclie;
  if %error();
    lastIoStatus = %status(trclie);
    return ioError();
  endif;
  if not %found(trclie);
    return newStatus(*off : CLIESRV_NOTFND : CLIESRV_MSG_NOTFND);
  endif;

  cldets = %timestamp();
  cldeus = pgmStatus.currentUser;
  update(e) retrclie;
  if %error();
    lastIoStatus = %status(trclie);
    return ioError();
  endif;

  if %parms() >= %parmnum(client);
    if %addr(client) <> *null;
      client = currentImage();
    endif;
  endif;
  return newStatus(*on : CLIESRV_OK : 'Client deleted');
end-proc;

//------------------------------------------------------------------
dcl-proc dniIsValid export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  return (dni > 999999 and dni < 100000000);
end-proc;

//------------------------------------------------------------------
// chain(n): existence checks never lock the record.
dcl-proc dniExists export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  if not openFile();
    return *off;
  endif;
  chain(ne) dni trclie;
  if %error();
    lastIoStatus = %status(trclie);
    return *off;
  endif;
  return %found(trclie);
end-proc;

//------------------------------------------------------------------
dcl-proc clientActive export;
  dcl-pi *n ind;
    dni zoned(8:0) const;
  end-pi;

  if not openFile();
    return *off;
  endif;
  chain(ne) dni trclie;
  if %error();
    lastIoStatus = %status(trclie);
    return *off;
  endif;
  if %found(trclie);
    return cldeus = *blanks;
  else;
    return *off;
  endif;
end-proc;

//------------------------------------------------------------------
// Internal procedures
//------------------------------------------------------------------
dcl-proc clearFields;
  cldni = 0;
  clname = *blanks;
  clcrts = CLIESRV_NO_TS;
  clcrus = *blanks;
  clupts = CLIESRV_NO_TS;
  clupus = *blanks;
  cldets = CLIESRV_NO_TS;
  cldeus = *blanks;
end-proc;

// Opens TRCLIE on first use and keeps it open; resets lastIoStatus.
dcl-proc openFile;
  dcl-pi *n ind;
  end-pi;

  lastIoStatus = 0;
  if not %open(trclie);
    open(e) trclie;
    if %error();
      lastIoStatus = %status(trclie);
      return *off;
    endif;
  endif;
  return *on;
end-proc;

dcl-proc currentImage;
  dcl-pi *n likeds(cliesrv_client_t);
  end-pi;

  dcl-ds image likeds(cliesrv_client_t) inz;

  image.dni = cldni;
  image.name = clname;
  image.createdTs = clcrts;
  image.createdBy = clcrus;
  image.updatedTs = clupts;
  image.updatedBy = clupus;
  image.deletedTs = cldets;
  image.deletedBy = cldeus;
  return image;
end-proc;

dcl-proc newStatus;
  dcl-pi *n likeds(cliesrv_status_t);
    success ind const;
    code char(10) const;
    message char(52) const;
  end-pi;

  dcl-ds status likeds(cliesrv_status_t) inz;

  status.success = success;
  status.code = code;
  status.message = message;
  return status;
end-proc;

dcl-proc ioError;
  dcl-pi *n likeds(cliesrv_status_t);
  end-pi;

  return newStatus(*off : CLIESRV_IOERR
                 : 'I/O error on TRCLIE, status ' + %char(lastIoStatus));
end-proc;

// A failed dniExists/clientActive check is an I/O error if the chain failed.
dcl-proc rejectOrIoError;
  dcl-pi *n likeds(cliesrv_status_t);
    code char(10) const;
    message char(52) const;
  end-pi;

  if lastIoStatus <> 0;
    return ioError();
  endif;
  return newStatus(*off : code : message);
end-proc;
