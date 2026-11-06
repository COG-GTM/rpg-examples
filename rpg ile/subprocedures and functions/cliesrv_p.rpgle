**free
// ---------------------------------------------------------------------------
// CLIESRV_P - public interface of service program CLIESRV (client CRUD).
// Include with /copy QRPGLESRC,CLIESRV_P
//
// Contract A (RPG wire format, frozen - the Java API depends on it):
//   cliesrv_status_t  63 bytes : success ind, code char(10), message char(52)
//   cliesrv_client_t 146 bytes : dni zoned(8:0), name char(30),
//                                createdTs/updatedTs/deletedTs timestamp,
//                                createdBy/updatedBy/deletedBy char(10)
// ---------------------------------------------------------------------------
/if defined(CLIESRV_P)
/eof
/endif
/define CLIESRV_P

// Status codes
dcl-c CLIESRV_OK 'OK';
dcl-c CLIESRV_INVDNI 'INVDNI';
dcl-c CLIESRV_DUPDNI 'DUPDNI';
dcl-c CLIESRV_NOTFND 'NOTFND';
dcl-c CLIESRV_DELETED 'DELETED';
dcl-c CLIESRV_IOERR 'IOERR';
dcl-c CLIESRV_INVOP 'INVOP';

// Status messages (legacy CLIECRUD texts)
dcl-c CLIESRV_MSG_INVDNI 'DNI not valid';
dcl-c CLIESRV_MSG_DUPDNI 'DNI already exists';
dcl-c CLIESRV_MSG_NOTFND 'DNI not exists';
dcl-c CLIESRV_MSG_DELETED 'Client marked as deleted';
dcl-c CLIESRV_MSG_INVOP 'OP. Error';
dcl-c CLIESRV_MSG_CREATED 'Client created';
dcl-c CLIESRV_MSG_READ 'Client read';
dcl-c CLIESRV_MSG_UPDATED 'Client updated';
dcl-c CLIESRV_MSG_DELETED_OK 'Client deleted';
dcl-c CLIESRV_MSG_IOERR 'I/O error on TRCLIE';

// "No value" timestamp used by TRCLIE for unset audit columns
dcl-c CLIESRV_TS_NONE z'0001-01-01-00.00.00.000000';

dcl-ds cliesrv_status_t qualified template;
  success ind;
  code char(10);
  message char(52);
end-ds;

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

// CRUD procedures: outcome in the returned status, record image in client
dcl-pr createClient likeds(cliesrv_status_t) extproc('createClient');
  dni zoned(8:0) const;
  name char(30) const;
  client likeds(cliesrv_client_t);
end-pr;

dcl-pr readClient likeds(cliesrv_status_t) extproc('readClient');
  dni zoned(8:0) const;
  client likeds(cliesrv_client_t);
end-pr;

dcl-pr updateClient likeds(cliesrv_status_t) extproc('updateClient');
  dni zoned(8:0) const;
  name char(30) const;
  client likeds(cliesrv_client_t);
end-pr;

dcl-pr deleteClient likeds(cliesrv_status_t) extproc('deleteClient');
  dni zoned(8:0) const;
  client likeds(cliesrv_client_t);
end-pr;

// Validators
dcl-pr dniIsValid ind extproc('dniIsValid');
  dni zoned(8:0) const;
end-pr;

dcl-pr dniExists ind extproc('dniExists');
  dni zoned(8:0) const;
end-pr;

dcl-pr clientActive ind extproc('clientActive');
  dni zoned(8:0) const;
end-pr;
