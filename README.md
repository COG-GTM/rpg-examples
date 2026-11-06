# rpg-examples

This is an IBM RPG programming language examples repository

## Notes on Free RPG

If you're using SEU, keep in mind that it does not behave well when you use full free-form syntax

## Some activities

### Basics and iterations

1. Show on screen the first N numbers, given an arbitrary N.
1. Show on screen the SUM of the first N numbers, given an arbitrary N.
1. Show on screen the factorial of an arbitrary number N.
1. Show on screen the first N numbers of fibonacci sequence, given an arbitrary N.
1. Given an arbitrary number (N) determine if it is even.
1. Determine if a given number N is a multiple of another number M.
1. Given two arbitrary numbers N and M, calculate N^M.
1. Given an arbitrary number (N), get the binary representation of it.
1. Given two arbitrary numbers, M and N, get the n-base representation of M
1. Given an arbitrary number N, determine if it is a prime number.
1. Show on screen all the even numbers from 1 to N, given an arbitrary N.
1. Print on screen the first N prime numbers, given an arbitrary N.
1. Given a date in YYYYMMDD format, parse it to DDMMYYYY format

### File managing

#### Sequential access

1. Read a file and show it's records on screen.
1. Read a file and count the amount of records.
1. Read a file and calculate the sum of one numeric field.
1. Read a file and calculate the product of one numeric field.
1. Read a file and calculate the average of one numeric field.
1. Read a file and update one column on all the records.
1. Read a file and update one column on one specific record, based on a given Key.
1. Mass insert records on a file.

#### Indexed access

1. TO DO

### Report Generation

1. TO DO

## Client CRUD service program (CLIESRV)

`CLIECRUD` (`rpg ile/subprocedures and functions/clients_crud.rpgle`) is a thin driver over the
UI-agnostic service program `CLIESRV` (`cliesrv.rpgle`, interface in the `/copy` member
`cliesrv_p.rpgle`, exports in `cliesrv.bnd`). The service never uses `DSPLY`: every outcome is
returned in `cliesrv_status_t` (63 bytes: success, code, message) and the record in
`cliesrv_client_t` (146 bytes).

* Legacy mode: `CALL CLIECRUD PARM('R' 12345678)` - same console output as before.
* Structured mode: `CALL CLIECRUD PARM(op dni name status client)` - no console output.
  Codes: `OK`, `INVDNI`, `DUPDNI`, `NOTFND`, `DELETED`, `IOERR`, `INVOP`.

Build with `cl ile/build_cliesrv.clle` (CRTRPGMOD, CRTSRVPGM, binding directory, CRTPGM).
`sql-db2-examples/cliecrud_procedure.sql` exposes the structured mode as SQL procedure `CLIECRUD_SP`.
