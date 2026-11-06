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

## Client management modernization

The client CRUD example (`CLIECRUD`, file `TRCLIE`) has been split so it can be called from outside a 5250 session:

- `rpg ile/subprocedures and functions/cliesrv.rpgle`: service program `CLIESRV` with `createClient`, `readClient`, `updateClient`, `deleteClient`, `dniIsValid`, `dniExists` and `clientActive`. It has no `DSPLY`: every call returns a 63-byte status DS (`success`, `code`, `message`) and a 146-byte client DS. Its public interface (codes, DS templates, prototypes) is in `cliesrv_p.rpgle`, and its exports are in `cliesrv.bnd`.
- `rpg ile/subprocedures and functions/clients_crud.rpgle`: `CLIECRUD` is now a thin driver. Called with 2-3 parms (`op dni [name]`) it shows the same console messages as before. Called with 5 parms (`op dni name status client`) it returns the status and client DS with no console output.
- `cl ile/build_cliesrv.clle`: builds the module, the service program, the binding directory and `CLIECRUD`.
- `sql-db2-examples/cliesrv_procedures.sql`: SQL procedure wrapper over `CLIECRUD`.
- `java/`: Spring Boot REST API over `CLIECRUD` (MBA-3805).
- `web/`: Angular UI for the REST API (MBA-3806).
