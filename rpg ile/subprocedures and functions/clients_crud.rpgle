**free
// CLIECRUD - thin driver over service program CLIESRV.
//   Legacy mode     (2-3 parms): CALL CLIECRUD PARM('R' 12345678)
//                                 results are DSPLYed exactly like before.
//   Structured mode (4-5 parms): no DSPLY; status (cliesrv_status_t) and
//                                 client image (cliesrv_client_t) returned.
ctl-opt main(mainp) option(*srcstmt:*nodebugio);

/copy QRPGLESRC,CLIESRV_P

dcl-c OP_CREATE 'C';
dcl-c OP_READ 'R';
dcl-c OP_UPDATE 'U';
dcl-c OP_DELETE 'D';

dcl-pr mainp extpgm('CLIECRUD');
  op char(1);
  dni zoned(8:0);
  name char(30) options(*nopass);
  status likeds(cliesrv_status_t) options(*nopass);
  client likeds(cliesrv_client_t) options(*nopass);
end-pr;

dcl-proc mainp;
  dcl-pi *n;
    op char(1);
    dni zoned(8:0);
    name char(30) options(*nopass);
    status likeds(cliesrv_status_t) options(*nopass);
    client likeds(cliesrv_client_t) options(*nopass);
  end-pi;

  dcl-s structured ind;
  dcl-s wwname char(30) inz;
  dcl-s zzmsg char(52);
  dcl-ds wwstatus likeds(cliesrv_status_t) inz;
  dcl-ds wwclient likeds(cliesrv_client_t) inz;

  structured = (%parms() >= %parmnum(status));
  if %parms() >= %parmnum(name);
    wwname = name;
  endif;

  select;
    when op = OP_CREATE;
      if not structured;
        dsply 'Creating';
      endif;
      wwstatus = createClient(dni : wwname : wwclient);
    when op = OP_READ;
      if not structured;
        dsply 'Reading';
      endif;
      wwstatus = readClient(dni : wwclient);
    when op = OP_UPDATE;
      if not structured;
        dsply 'Updating';
      endif;
      wwstatus = updateClient(dni : wwname : wwclient);
    when op = OP_DELETE;
      if not structured;
        dsply 'Deleting';
      endif;
      wwstatus = deleteClient(dni : wwclient);
    other;
      wwstatus.success = *off;
      wwstatus.code = CLIESRV_INVOP;
      wwstatus.message = CLIESRV_MSG_INVOP;
  endsl;

  if not structured;
    if not wwstatus.success;
      dsply wwstatus.message;
    elseif op = OP_READ;
      zzmsg = %char(wwclient.dni) + '-' + wwclient.name;
      dsply zzmsg;
    endif;
    return;
  endif;

  status = wwstatus;
  if %parms() >= %parmnum(client);
    client = wwclient;
  endif;
  return;
end-proc;
