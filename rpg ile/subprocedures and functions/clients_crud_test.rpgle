**free
// ---------------------------------------------------------------------
// CLIETEST - Regression driver for the CLIESRV service program
//
// Exercises every row of the behaviour matrix in
// docs/modernization/cliecrud-functional-spec.md against TRCLIE and
// DSPLYs PASS/FAIL per case plus a summary. Uses DNIs derived from the
// current time so it can be re-run without cleaning the file.
//   CALL CLIETEST
// ---------------------------------------------------------------------
ctl-opt main(mainp) dftactgrp(*no) actgrp(*new) bnddir('CLIEBND')
        option(*srcstmt:*nodebugio);

/include clients_crud_h.rpgleinc

dcl-pr mainp extpgm('CLIETEST');
end-pr;

dcl-s passed int(10) inz(0);
dcl-s failed int(10) inz(0);

dcl-proc mainp;
  dcl-ds r likeds(cliResponse_t) inz(*likeds);
  dcl-s dni     zoned(8:0);
  dcl-s unused  zoned(8:0);
  dcl-s actDni  zoned(8:0);
  dcl-s seconds int(20);
  dcl-s msg     char(52);

  // 4 DNIs per second of runtime, all in the valid 8-digit range
  seconds = %diff(%timestamp(): z'2020-01-01-00.00.00.000000': *seconds);
  dni    = 10000000 + %rem(seconds * 4: 89999996);
  unused = dni + 1;
  actDni = dni + 2;

  // FR-2 DNI rule
  check('T01 create 6-digit DNI rejected'
       : createClient(999999: 'SHORT': r) = CLI_DNI_INVALID
         and r.message = 'DNI not valid');
  check('T02 read negative DNI rejected'
       : readClient(-1234567: r) = CLI_DNI_INVALID);
  check('T03 7-digit lower bound accepted'
       : readClient(1000000: r) <> CLI_DNI_INVALID);

  // FR-3 create + audit
  check('T04 create new client'
       : createClient(dni: 'ORIGINAL NAME': r) = CLI_OK
         and r.client.dni = dni
         and r.client.name = 'ORIGINAL NAME'
         and r.client.createdBy <> *blanks
         and r.client.createdTs > z'0001-01-01-00.00.00.000000'
         and r.client.deletedBy = *blanks
         and r.client.active);
  check('T05 create duplicate rejected'
       : createClient(dni: 'DUP': r) = CLI_DNI_EXISTS
         and r.message = 'DNI already exists');

  // FR-4 read
  check('T06 read active client'
       : readClient(dni: r) = CLI_OK
         and r.client.name = 'ORIGINAL NAME');
  check('T07 read unknown DNI'
       : readClient(unused: r) = CLI_DNI_NOT_FOUND
         and r.message = 'DNI not exists');

  // FR-5 update + audit
  check('T08 update active client'
       : updateClient(dni: 'UPDATED NAME': r) = CLI_OK
         and r.client.name = 'UPDATED NAME'
         and r.client.updatedBy <> *blanks
         and r.client.updatedTs >= r.client.createdTs);
  check('T09 update unknown DNI'
       : updateClient(unused: 'X': r) = CLI_DNI_NOT_FOUND);

  // FR-6 soft delete
  check('T10 delete active client'
       : deleteClient(dni: r) = CLI_OK
         and r.client.deletedBy <> *blanks
         and r.client.deletedTs >= r.client.updatedTs
         and not r.client.active
         and r.client.name = 'UPDATED NAME');
  check('T11 read deleted client'
       : readClient(dni: r) = CLI_DELETED
         and r.message = 'Client marked as deleted');
  check('T12 update deleted client'
       : updateClient(dni: 'AGAIN': r) = CLI_DELETED);
  check('T13 delete deleted client'
       : deleteClient(dni: r) = CLI_DELETED);
  check('T14 re-create deleted DNI'
       : createClient(dni: 'AGAIN': r) = CLI_DNI_EXISTS);
  check('T15 delete unknown DNI'
       : deleteClient(unused: r) = CLI_DNI_NOT_FOUND);

  // Actor override for callers that know the end user (REST layer)
  check('T16 create with explicit actor'
       : createClient(actDni: 'ACTOR TEST': r: 'WEBUSER01') = CLI_OK
         and r.client.createdBy = 'WEBUSER01');
  check('T17 update with omitted actor uses job user'
       : updateClient(actDni: 'ACTOR TEST 2': r: *omit) = CLI_OK
         and r.client.updatedBy <> *blanks
         and r.client.updatedBy <> 'WEBUSER01');
  check('T18 delete with explicit actor'
       : deleteClient(actDni: r: 'WEBUSER02') = CLI_OK
         and r.client.deletedBy = 'WEBUSER02');

  msg = 'CLIETEST passed ' + %char(passed) + ' failed ' + %char(failed);
  dsply msg;
  return;
end-proc;

dcl-proc check;
  dcl-pi *n;
    name varchar(44) const;
    ok   ind const;
  end-pi;
  dcl-s msg char(52);

  if ok;
    passed += 1;
    msg = 'PASS ' + name;
  else;
    failed += 1;
    msg = 'FAIL ' + name;
  endif;
  dsply msg;
end-proc;
