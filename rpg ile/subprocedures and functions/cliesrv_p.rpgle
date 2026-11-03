**free
//==============================================================================
// CLIESRV_P - Prototypes and templates for the CLIESRV service program.
//
// Usage:  /copy QRPGLESRC,CLIESRV_P      (source physical file member)
//         /copy 'cliesrv_p.rpgle'        (IFS stream file)
//
// Every CRUD procedure returns a cliesrv_status_t instead of using DSPLY, so
// the service is UI-agnostic and callable from CL, RPG, SQL or Java (jt400).
// The data structure layouts below are fixed (no extname) because they are
// also the wire format mapped by the Java backend (Jt400ClientRepository).
//==============================================================================
/if defined(CLIESRV_P)
/eof
/endif
/define CLIESRV_P

// Status codes returned in cliesrv_status_t.code
dcl-c CLIESRV_OK          'OK';
dcl-c CLIESRV_INVALID_DNI 'INVDNI';
dcl-c CLIESRV_DUPLICATE   'DUPDNI';
dcl-c CLIESRV_NOT_FOUND   'NOTFND';
dcl-c CLIESRV_DELETED     'DELETED';
dcl-c CLIESRV_IO_ERROR    'IOERR';
dcl-c CLIESRV_INVALID_OP  'INVOP';

// Result of every CRUD operation (63 bytes)
//   success : '1' = operation done, '0' = rejected/failed
//   code    : one of the CLIESRV_* constants above
//   message : human readable text (same wording as the legacy DSPLY messages)
dcl-ds cliesrv_status_t qualified template;
  success ind;
  code    char(10);
  message char(52);
end-ds;

// Client image returned by readClient (mirrors TRCLIE/RETRCLIE, 146 bytes)
// Timestamps use the *ISO character form YYYY-MM-DD-hh.mm.ss.ffffff.
dcl-ds cliesrv_client_t qualified template;
  dni       zoned(8:0);   // CLDNI
  name      char(30);     // CLNAME
  createdTs timestamp;    // CLCRTS
  createdBy char(10);     // CLCRUS
  updatedTs timestamp;    // CLUPTS
  updatedBy char(10);     // CLUPUS
  deletedTs timestamp;    // CLDETS
  deletedBy char(10);     // CLDEUS  (blank = active)
end-ds;

//--- CRUD ---------------------------------------------------------------------
dcl-pr createClient likeds(cliesrv_status_t) extproc(*dclcase);
  dni  zoned(8:0) const;
  name char(30)   const;
end-pr;

dcl-pr readClient likeds(cliesrv_status_t) extproc(*dclcase);
  dni    zoned(8:0) const;
  client likeds(cliesrv_client_t);
end-pr;

dcl-pr updateClient likeds(cliesrv_status_t) extproc(*dclcase);
  dni  zoned(8:0) const;
  name char(30)   const;
end-pr;

dcl-pr deleteClient likeds(cliesrv_status_t) extproc(*dclcase);
  dni zoned(8:0) const;
end-pr;

//--- Validations --------------------------------------------------------------
dcl-pr dniIsValid ind extproc(*dclcase);
  dni zoned(8:0) const;
end-pr;

dcl-pr dniExists ind extproc(*dclcase);
  dni zoned(8:0) const;
end-pr;

dcl-pr clientActive ind extproc(*dclcase);
  dni zoned(8:0) const;
end-pr;
