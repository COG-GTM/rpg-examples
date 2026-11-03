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

## CLIESRV service program (client management)

`rpg ile/subprocedures and functions/` contains a UI-agnostic version of the clients CRUD over file `TRCLIE` (`database-files-examples/trclie.pf`):

| Source | Member | Object |
|--------|--------|--------|
| `cliesrv.rpgle` | QRPGLESRC/CLIESRV | NOMAIN module → `*SRVPGM CLIESRV` |
| `cliesrv_p.rpgle` | QRPGLESRC/CLIESRV_P | `/copy` member: status codes, templates, prototypes |
| `cliesrv.bnd` | QSRVSRC/CLIESRV | binder source, signature `CLIESRV V1` |
| `clients_crud.rpgle` | QRPGLESRC/CLIECRUD | `*PGM CLIECRUD`, thin driver bound to CLIESRV |
| `cl ile/build_cliesrv.clle` | | build script |
| `sql-db2-examples/cliesrv_procedures.sql` | | SQL `CALL` wrappers over CLIECRUD |

Exported procedures: `createClient`, `readClient`, `updateClient`, `deleteClient` (each returns `cliesrv_status_t`, and optionally the client image `cliesrv_client_t`), plus `dniIsValid`, `dniExists`, `clientActive`. Nothing is displayed by the service program.

Status DS (63 bytes):

```
dcl-ds cliesrv_status_t qualified template;
  success ind;          // '1' done, '0' rejected/failed
  code    char(10);     // OK | INVDNI | DUPDNI | NOTFND | DELETED | IOERR | INVOP
  message char(52);     // e.g. 'DNI not valid', 'DNI already exists', 'DNI not exists', 'Client marked as deleted'
end-ds;
```

`CLIECRUD` parameters: `op char(1)` (C/R/U/D), `dni zoned(8:0)`, `name char(30)`, `status char(63)`, `client char(146)`. Called with 2-3 parameters it behaves like the original program (DSPLY messages); with all 5 it only returns the status and client image (used by Java / SQL callers).

Build: upload the members above to `QRPGLESRC` / `QSRVSRC`, adjust `&LIB` in `build_cliesrv.clle` and run it (CRTRPGMOD → CRTSRVPGM → CRTBNDDIR/ADDBNDDIRE → CRTRPGMOD/CRTPGM). Then e.g. `CALL CLIECRUD PARM('C' 12345678 'ANA PEREZ')`.
