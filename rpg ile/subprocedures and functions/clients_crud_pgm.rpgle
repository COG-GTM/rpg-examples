**free
// ---------------------------------------------------------------------
// CLIECRUD - Command-line wrapper over the CLIESRV service program
//
// Keeps the original CALL interface and console messages:
//   CALL CLIECRUD PARM('C' '12345678' 'JOHN DOE')
//   CALL CLIECRUD PARM('R' '12345678')
//   CALL CLIECRUD PARM('U' '12345678' 'JOHN Q DOE')
//   CALL CLIECRUD PARM('D' '12345678')
// The DNI is passed as a character literal so it maps onto zoned(8:0).
// ---------------------------------------------------------------------
ctl-opt main(mainp) dftactgrp(*no) actgrp(*new) bnddir('CLIEBND')
        option(*srcstmt:*nodebugio);

/include clients_crud_h.rpgleinc

dcl-pr mainp extpgm('CLIECRUD');
  paop   char(1);
  padni  zoned(8:0);
  paname char(30) options(*nopass);
end-pr;

dcl-proc mainp;
  dcl-pi *n;
    paop   char(1);
    padni  zoned(8:0);
    paname char(30) options(*nopass);
  end-pi;
  dcl-ds response likeds(cliResponse_t) inz(*likeds);
  dcl-s hasName ind inz(*off);
  dcl-s msg char(52);

  hasName = (%parms() >= %parmnum(paname));

  select;
    when paop = 'C';
      dsply 'Creating';
      if not hasName;
        response.status = CLI_NAME_REQUIRED;
        response.message = 'Name required';
      else;
        createClient(padni: paname: response);
      endif;
    when paop = 'R';
      dsply 'Reading';
      readClient(padni: response);
    when paop = 'U';
      dsply 'Updating';
      if not hasName;
        response.status = CLI_NAME_REQUIRED;
        response.message = 'Name required';
      else;
        updateClient(padni: paname: response);
      endif;
    when paop = 'D';
      dsply 'Deleting';
      deleteClient(padni: response);
    other;
      response.status = CLI_INVALID_OP;
      response.message = 'OP. Error';
  endsl;

  if response.status <> CLI_OK;
    msg = response.message;
    dsply msg;
  elseif paop = 'R';
    msg = %char(response.client.dni) + '-' + response.client.name;
    dsply msg;
  endif;

  return;
end-proc;
