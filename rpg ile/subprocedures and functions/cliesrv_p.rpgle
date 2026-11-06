**free
// ---------------------------------------------------------------------
// CLIESRV_P - public interface of service program CLIESRV
//
// /copy QRPGLESRC,CLIESRV_P
//
// Contract A (RPG wire format). Phase 2 (Java / jt400) depends on the
// byte layouts below: do not reorder, resize or retype any subfield.
//   cliesrv_status_t  63 bytes
//   cliesrv_client_t 146 bytes
// ---------------------------------------------------------------------
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

// Status messages (cliesrv_status_t.message) - legacy CLIECRUD texts
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

dcl-ds cliesrv_status_t qualified template;   // 63 bytes
  success ind;          // '1' ok, '0' rejected/failed
  code char(10);        // CLIESRV_OK ... CLIESRV_INVOP
  message char(52);     // CLIESRV_MSG_*
end-ds;

dcl-ds cliesrv_client_t qualified template;   // 146 bytes
  dni zoned(8:0);
  name char(30);
  createdTs timestamp;
  createdBy char(10);
  updatedTs timestamp;
  updatedBy char(10);
  deletedTs timestamp;
  deletedBy char(10);
end-ds;

// CRUD procedures: every outcome is returned in the status DS and the
// client image (cleared on failure) is returned in the output parm.
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
