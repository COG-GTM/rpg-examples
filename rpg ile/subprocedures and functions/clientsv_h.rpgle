**free
// ---------------------------------------------------------------------------
// CLIENTSV_H - public API of service program CLIENTSV (client CRUD on TRCLIE)
// Include with: /copy QRPGLESRC,CLIENTSV_H
//
// API contract (byte layouts are frozen; the Java API encodes them):
//   client_t       146 bytes  mirrors record format RETRCLIE
//   clientsv_in_t   39 bytes  op char(1) | dni zoned(8:0) | name char(30)
//   clientsv_out_t 209 bytes  success ind | code char(10) | message char(52)
//                             | client client_t
// Timestamps are 26-char *ISO ('YYYY-MM-DD-hh.mm.ss.uuuuuu');
// '0001-01-01-00.00.00.000000' means "not set".
// ---------------------------------------------------------------------------
/if defined(CLIENTSV_H)
/eof
/endif
/define CLIENTSV_H

// Operation codes (same values as the legacy CLIECRUD paop parameter)
dcl-c CLIENTSV_OP_CREATE 'C';
dcl-c CLIENTSV_OP_READ 'R';
dcl-c CLIENTSV_OP_UPDATE 'U';
dcl-c CLIENTSV_OP_DELETE 'D';

// Result codes returned in clientsv_out_t.code
dcl-c CLIENTSV_OK 'OK';
dcl-c CLIENTSV_INVDNI 'INVDNI';
dcl-c CLIENTSV_DUPDNI 'DUPDNI';
dcl-c CLIENTSV_NOTFND 'NOTFND';
dcl-c CLIENTSV_DELETED 'DELETED';
dcl-c CLIENTSV_INVOP 'INVOP';
dcl-c CLIENTSV_IOERR 'IOERR';

// Result messages (failure texts are the legacy CLIECRUD DSPLY texts)
dcl-c CLIENTSV_MSG_INVDNI 'DNI not valid';
dcl-c CLIENTSV_MSG_DUPDNI 'DNI already exists';
dcl-c CLIENTSV_MSG_NOTFND 'DNI not exists';
dcl-c CLIENTSV_MSG_DELETED 'Client marked as deleted';
dcl-c CLIENTSV_MSG_INVOP 'OP. Error';
dcl-c CLIENTSV_MSG_IOERR 'I/O error on TRCLIE';
dcl-c CLIENTSV_MSG_CREATED 'Client created';
dcl-c CLIENTSV_MSG_READ 'Client read';
dcl-c CLIENTSV_MSG_UPDATED 'Client updated';
dcl-c CLIENTSV_MSG_REMOVED 'Client deleted';

// "Not set" value of the TRCLIE audit timestamps
dcl-c CLIENTSV_TS_NONE z'0001-01-01-00.00.00.000000';

// Client record: same field names, types and order as RETRCLIE (TRCLIE)
dcl-ds client_t qualified template;
  cldni zoned(8:0);
  clname char(30);
  clcrts timestamp;
  clcrus char(10);
  clupts timestamp;
  clupus char(10);
  cldets timestamp;
  cldeus char(10);
end-ds;

// Op-in: request
dcl-ds clientsv_in_t qualified template;
  op char(1);
  dni zoned(8:0);
  name char(30);
end-ds;

// Op-out: result + record image after the operation
dcl-ds clientsv_out_t qualified template;
  success ind;
  code char(10);
  message char(52);
  client likeds(client_t);
end-ds;

// ---------------------------------------------------------------------------
// Exported procedures
// ---------------------------------------------------------------------------

// Dispatcher: runs createClient/readClient/updateClient/deleteClient by in.op
dcl-pr clientsvExecute extproc('clientsvExecute');
  in likeds(clientsv_in_t) const;
  out likeds(clientsv_out_t);
end-pr;

// Create: valid DNI -> DNI must not exist (soft-deleted rows count)
dcl-pr createClient extproc('createClient');
  in likeds(clientsv_in_t) const;
  out likeds(clientsv_out_t);
end-pr;

// Read/update/delete: valid DNI -> DNI exists -> client active
dcl-pr readClient extproc('readClient');
  in likeds(clientsv_in_t) const;
  out likeds(clientsv_out_t);
end-pr;

dcl-pr updateClient extproc('updateClient');
  in likeds(clientsv_in_t) const;
  out likeds(clientsv_out_t);
end-pr;

// Soft delete: stamps cldets/cldeus, the record is kept
dcl-pr deleteClient extproc('deleteClient');
  in likeds(clientsv_in_t) const;
  out likeds(clientsv_out_t);
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
