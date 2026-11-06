**free
// ---------------------------------------------------------------------------
// CLIECRUD - thin driver over service program CLIESRV.
//
// Legacy mode (2-3 parms):  CALL CLIECRUD PARM(op dni [name])
//   Same console (DSPLY) output as the original CLIECRUD.
// Structured mode (5 parms): CALL CLIECRUD PARM(op dni name status client)
//   status char(63) = cliesrv_status_t, client char(146) = cliesrv_client_t.
//   No DSPLY. An unknown op returns INVOP.
// ---------------------------------------------------------------------------
ctl-opt dftactgrp(*no) main(mainp) option(*srcstmt:*nodebugio)
        bnddir('CLIESRV');

/copy QRPGLESRC,CLIESRV_P

dcl-c OP_CREATE 'C';
dcl-c OP_READ 'R';
dcl-c OP_UPDATE 'U';
dcl-c OP_DELETE 'D';

dcl-pr mainp extpgm('CLIECRUD');
  op char(1);
  dni zoned(8:0);
  name char(30) options(*nopass);
  status char(63) options(*nopass);
  client char(146) options(*nopass);
end-pr;

// ---------------------------------------------------------------------------
dcl-proc mainp;
  dcl-pi *n;
    paop char(1);
    padni zoned(8:0);
    paname char(30) options(*nopass);
    pastatus char(63) options(*nopass);
    paclient char(146) options(*nopass);
  end-pi;
  dcl-ds wkstatus likeds(cliesrv_status_t) inz;
  dcl-ds wkclient likeds(cliesrv_client_t) inz;
  dcl-s wkname char(30);
  dcl-s structured ind;

  structured = (%parms() >= %parmnum(paclient));
  wkname = *blanks;
  if %parms() >= %parmnum(paname);
    wkname = paname;
  endif;

  select;
    when paop = OP_CREATE;
      legacyDsply(structured: 'Creating');
      wkstatus = createClient(padni: wkname: wkclient);
    when paop = OP_READ;
      legacyDsply(structured: 'Reading');
      wkstatus = readClient(padni: wkclient);
    when paop = OP_UPDATE;
      legacyDsply(structured: 'Updating');
      wkstatus = updateClient(padni: wkname: wkclient);
    when paop = OP_DELETE;
      legacyDsply(structured: 'Deleting');
      wkstatus = deleteClient(padni: wkclient);
    other;
      clear wkclient;
      wkstatus.success = *off;
      wkstatus.code = CLIESRV_INVOP;
      wkstatus.message = CLIESRV_MSG_INVOP;
  endsl;

  if structured;
    pastatus = wkstatus;
    paclient = wkclient;
    return;
  endif;

  // Legacy console output
  if not wkstatus.success;
    legacyDsply(structured: wkstatus.message);
  elseif paop = OP_READ;
    legacyDsply(structured: %char(wkclient.dni) + '-' + wkclient.name);
  endif;
  return;
end-proc;

// ---------------------------------------------------------------------------
// DSPLY is used only here, and only in legacy (2-3 parm) mode.
dcl-proc legacyDsply;
  dcl-pi *n;
    structured ind const;
    text varchar(52) const;
  end-pi;
  dcl-s zzmsg char(52);

  if structured;
    return;
  endif;
  zzmsg = text;
  dsply zzmsg;
end-proc;
