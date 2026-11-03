**free
// CLIESRV_P - prototypes, templates and status codes of service program CLIESRV
//   /copy QRPGLESRC,CLIESRV_P
/if defined(CLIESRV_P)
/eof
/endif
/define CLIESRV_P

// Status codes (cliesrv_status_t.code)
dcl-c CLIESRV_OK 'OK';
dcl-c CLIESRV_INVDNI 'INVDNI';
dcl-c CLIESRV_DUPDNI 'DUPDNI';
dcl-c CLIESRV_NOTFND 'NOTFND';
dcl-c CLIESRV_DELETED 'DELETED';
dcl-c CLIESRV_IOERR 'IOERR';
dcl-c CLIESRV_INVOP 'INVOP';

// Status messages (legacy DSPLY wording)
dcl-c CLIESRV_MSG_INVDNI 'DNI not valid';
dcl-c CLIESRV_MSG_DUPDNI 'DNI already exists';
dcl-c CLIESRV_MSG_NOTFND 'DNI not exists';
dcl-c CLIESRV_MSG_DELETED 'Client marked as deleted';
dcl-c CLIESRV_MSG_INVOP 'OP. Error';

// "Not set" timestamp
dcl-c CLIESRV_NO_TS z'0001-01-01-00.00.00.000000';

// Status returned by every CRUD procedure (63 bytes)
dcl-ds cliesrv_status_t qualified template;
  success ind;
  code char(10);
  message char(52);
end-ds;

// Client image (146 bytes)
dcl-ds cliesrv_client_t qualified template;
  dni zoned(8:0);
  name char(30);
  createdTs timestamp;
  createdBy char(10);
  updatedTs timestamp;
  updatedBy char(10);
  deletedTs timestamp;
  deletedBy char(10);
end-ds;

dcl-pr createClient likeds(cliesrv_status_t) extproc('createClient');
  dni zoned(8:0) const;
  name char(30) const;
  client likeds(cliesrv_client_t) options(*nopass:*omit);
end-pr;

dcl-pr readClient likeds(cliesrv_status_t) extproc('readClient');
  dni zoned(8:0) const;
  client likeds(cliesrv_client_t);
end-pr;

dcl-pr updateClient likeds(cliesrv_status_t) extproc('updateClient');
  dni zoned(8:0) const;
  name char(30) const;
  client likeds(cliesrv_client_t) options(*nopass:*omit);
end-pr;

dcl-pr deleteClient likeds(cliesrv_status_t) extproc('deleteClient');
  dni zoned(8:0) const;
  client likeds(cliesrv_client_t) options(*nopass:*omit);
end-pr;

dcl-pr dniIsValid ind extproc('dniIsValid');
  dni zoned(8:0) const;
end-pr;

dcl-pr dniExists ind extproc('dniExists');
  dni zoned(8:0) const;
end-pr;

dcl-pr clientActive ind extproc('clientActive');
  dni zoned(8:0) const;
end-pr;
