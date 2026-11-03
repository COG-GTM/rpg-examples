**free
//==============================================================================
// CLIECRUD - Thin driver over the CLIESRV service program.
//
// Backward compatible entry point: CALL CLIECRUD PARM(op dni [name])
//   op  : 'C' create | 'R' read | 'U' update | 'D' delete
// With 2-3 parameters the program behaves like the original example and shows
// the result with DSPLY.
//
// Structured mode (used by Java / jt400 ProgramCall and SQL procedures):
//   CALL CLIECRUD PARM(op dni name status client)
//   status : cliesrv_status_t (63 bytes, output)
//   client : cliesrv_client_t (146 bytes, output, filled by 'R')
// When status is passed nothing is displayed.
//==============================================================================
ctl-opt option(*nodebugio:*srcstmt) dftactgrp(*no) actgrp(*caller)
        bnddir('CLIESRV') main(mainp);

/copy QRPGLESRC,CLIESRV_P

dcl-pr mainp extpgm('CLIECRUD');
  op     char(1);
  dni    zoned(8:0);
  name   char(30) options(*nopass);
  status likeds(cliesrv_status_t) options(*nopass);
  client likeds(cliesrv_client_t) options(*nopass);
end-pr;

dcl-proc mainp;
  dcl-pi *n;
    op     char(1);
    dni    zoned(8:0);
    name   char(30) options(*nopass);
    status likeds(cliesrv_status_t) options(*nopass);
    client likeds(cliesrv_client_t) options(*nopass);
  end-pi;
  dcl-ds result likeds(cliesrv_status_t);
  dcl-ds image  likeds(cliesrv_client_t);
  dcl-s  wwname char(30);
  dcl-s  structured ind;

  structured = %parms() >= %parmnum(status);
  if %parms() >= %parmnum(name);
    wwname = name;
  endif;

  select;
    when op = 'C';
      say(structured:'Creating');
      result = createClient(dni:wwname);
    when op = 'R';
      say(structured:'Reading');
      result = readClient(dni:image);
    when op = 'U';
      say(structured:'Updating');
      result = updateClient(dni:wwname);
    when op = 'D';
      say(structured:'Deleting');
      result = deleteClient(dni);
    other;
      result.success = *off;
      result.code    = CLIESRV_INVALID_OP;
      result.message = 'OP. Error';
  endsl;

  if structured;
    status = result;
    if %parms() >= %parmnum(client);
      client = image;
    endif;
  elseif not result.success or op = 'R';
    // Legacy behaviour: errors and the read result ("dni-name") are displayed
    say(structured:result.message);
  endif;

  return;
end-proc;

dcl-proc say;
  dcl-pi *n;
    structured ind const;
    text       varchar(52) const;
  end-pi;
  dcl-s msg char(52);

  if not structured;
    msg = text;
    dsply msg;
  endif;
end-proc;
