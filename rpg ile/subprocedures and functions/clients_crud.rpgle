**free
// ---------------------------------------------------------------------
// CLIECRUD - client CRUD driver over service program CLIESRV
//
// Legacy mode (2-3 parms), unchanged console behaviour:
//   CALL CLIECRUD PARM(op dni [name])
// Structured mode (5 parms), no console output:
//   CALL CLIECRUD PARM(op dni name status client)
//   status = cliesrv_status_t (63 bytes), client = cliesrv_client_t
//   (146 bytes), both output. An unknown op returns code INVOP.
// op: C create, R read, U update, D delete (soft).
// Build: cl ile/build_cliesrv.clle
// ---------------------------------------------------------------------
ctl-opt main(mainp) dftactgrp(*no) bnddir('CLIESRV')
        option(*srcstmt:*nodebugio);

/copy QRPGLESRC,CLIESRV_P

dcl-pr mainp extpgm('CLIECRUD');
  op char(1);
  dni zoned(8:0);
  name char(30) options(*nopass);
  status likeds(cliesrv_status_t) options(*nopass);
  client likeds(cliesrv_client_t) options(*nopass);
end-pr;

dcl-pr showLegacyOutput;
  op char(1) const;
  status likeds(cliesrv_status_t) const;
  client likeds(cliesrv_client_t) const;
end-pr;

dcl-c OP_CREATE 'C';
dcl-c OP_READ 'R';
dcl-c OP_UPDATE 'U';
dcl-c OP_DELETE 'D';
dcl-c PARM_NAME 3;
dcl-c PARM_CLIENT 5;

dcl-proc mainp;
  dcl-pi *n;
    op char(1);
    dni zoned(8:0);
    name char(30) options(*nopass);
    status likeds(cliesrv_status_t) options(*nopass);
    client likeds(cliesrv_client_t) options(*nopass);
  end-pi;
  dcl-s structured ind;
  dcl-s wwname char(30);
  dcl-s wwbanner char(52);
  dcl-ds wwstatus likeds(cliesrv_status_t);
  dcl-ds wwclient likeds(cliesrv_client_t);

  structured = (%parms() >= PARM_CLIENT);
  if %parms() >= PARM_NAME;
    wwname = name;
  endif;

  select;
    when op = OP_CREATE;
      wwbanner = 'Creating';
    when op = OP_READ;
      wwbanner = 'Reading';
    when op = OP_UPDATE;
      wwbanner = 'Updating';
    when op = OP_DELETE;
      wwbanner = 'Deleting';
    other;
      wwbanner = *blanks;
  endsl;
  if not structured and wwbanner <> *blanks;
    dsply wwbanner;
  endif;

  select;
    when op = OP_CREATE;
      wwstatus = createClient(dni : wwname : wwclient);
    when op = OP_READ;
      wwstatus = readClient(dni : wwclient);
    when op = OP_UPDATE;
      wwstatus = updateClient(dni : wwname : wwclient);
    when op = OP_DELETE;
      wwstatus = deleteClient(dni : wwclient);
    other;
      clear wwclient;
      wwstatus.success = *off;
      wwstatus.code = CLIESRV_INVOP;
      wwstatus.message = CLIESRV_MSG_INVOP;
  endsl;

  if structured;
    status = wwstatus;
    client = wwclient;
  else;
    showLegacyOutput(op : wwstatus : wwclient);
  endif;
  return;
end-proc;

// Legacy console output: the rejection text, or 'dni-name' after a read.
// Successful create/update/delete print nothing beyond the banner.
dcl-proc showLegacyOutput;
  dcl-pi *n;
    op char(1) const;
    status likeds(cliesrv_status_t) const;
    client likeds(cliesrv_client_t) const;
  end-pi;
  dcl-s zzmsg char(52);

  if not status.success;
    zzmsg = status.message;
    dsply zzmsg;
  elseif op = OP_READ;
    zzmsg = %char(client.dni) + '-' + client.name;
    dsply zzmsg;
  endif;
end-proc;
