**free
// ---------------------------------------------------------------------------
// CLIECRUD - backward-compatible wrapper over service program CLIENTSV.
//
// Same call signature and console output as the original program:
//   CALL CLIECRUD PARM(op dni [name])
//     op   char(1)      C=create R=read U=update D=delete
//     dni  zoned(8:0)
//     name char(30)     optional (create/update)
// All business rules live in CLIENTSV; this program only maps the
// positional parameters to clientsv_in_t and DSPLYs the outcome.
// ---------------------------------------------------------------------------
ctl-opt dftactgrp(*no) actgrp(*new) main(mainp)
        option(*srcstmt:*nodebugio) bnddir('CLIENTSV');

/copy QRPGLESRC,CLIENTSV_H

dcl-pr mainp extpgm('CLIECRUD');
  op char(1);
  dni zoned(8:0);
  name char(30) options(*nopass);
end-pr;

// ---------------------------------------------------------------------------
dcl-proc mainp;
  dcl-pi *n;
    paop char(1);
    padni zoned(8:0);
    paname char(30) options(*nopass);
  end-pi;
  dcl-ds req likeds(clientsv_in_t) inz;
  dcl-ds res likeds(clientsv_out_t) inz;

  req.op = paop;
  req.dni = padni;
  req.name = *blanks;
  if %parms() >= %parmnum(paname);
    req.name = paname;
  endif;

  select;
    when paop = CLIENTSV_OP_CREATE;
      say('Creating');
    when paop = CLIENTSV_OP_READ;
      say('Reading');
    when paop = CLIENTSV_OP_UPDATE;
      say('Updating');
    when paop = CLIENTSV_OP_DELETE;
      say('Deleting');
    other;
      // legacy behaviour: only 'OP. Error' is shown (below)
  endsl;

  clientsvExecute(req: res);

  if not res.success;
    say(res.message);
  elseif paop = CLIENTSV_OP_READ;
    say(%char(res.client.cldni) + '-' + res.client.clname);
  endif;
  return;
end-proc;

// ---------------------------------------------------------------------------
dcl-proc say;
  dcl-pi *n;
    text varchar(52) const;
  end-pi;
  dcl-s zzmsg char(52);

  zzmsg = text;
  dsply zzmsg;
end-proc;
