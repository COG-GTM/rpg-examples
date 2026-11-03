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

## Client management modernization (CLIECRUD -> CLIESRV -> REST -> Angular)

The `clients_crud.rpgle` example (file `TRCLIE`, record `RETRCLIE`) is being moved toward a
modern stack in three phases. Each phase keeps the previous one working.

| Phase | Where | What |
|-------|-------|------|
| 1. RPGLE service program | `rpg ile/subprocedures and functions/` | `cliesrv.rpgle` (module/service program `CLIESRV`), `cliesrv_p.rpgle` (`/copy` prototypes + status/client templates), `cliesrv.bnd` (binder source), `clients_crud.rpgle` (thin `CLIECRUD` driver). Build with `cl ile/build_cliesrv.clle`. |
| 2. Java backend | `java/` | Spring Boot 3 REST API over a `ClientRepository` port. `Jt400ClientRepository` calls `CLIECRUD` -> `CLIESRV` with jt400 `ProgramCall`; `InMemoryClientRepository` re-implements the same rules for local dev. A JPA implementation directly on `TRCLIE` can replace jt400 later without touching the API. |
| 3. Angular frontend | `web/` | Client list, detail, create and edit screens calling the REST API, with DNI validation mirroring the RPG rule. |

### Phase 1 - CLIESRV

* Exports `createClient`, `readClient`, `updateClient`, `deleteClient`, `dniIsValid`,
  `dniExists`, `clientActive` (binder signature `CLIESRV V1`).
* No `DSPLY`: every CRUD procedure returns `cliesrv_status_t`
  (`success` ind, `code` char(10), `message` char(52)); `readClient` also fills a
  `cliesrv_client_t` output parameter. The messages are the original DSPLY texts.
* `CLIECRUD` keeps its original call interface (`op`, `dni`, optional `name`) and still displays
  results when called with 2-3 parameters. Passing two more output parameters (`status`, `client`)
  switches it to structured mode with no DSPLY; this is how Java calls it.
* Existence/active checks use `chain(n)` so a server job never keeps record locks between calls.

| Status code | Message | HTTP |
|-------------|---------|------|
| `OK` | `Client created` / `updated` / `deleted`, or `dni-name` on read | 200 / 201 / 204 |
| `INVDNI` | `DNI not valid` | 400 |
| `DUPDNI` | `DNI already exists` | 409 |
| `NOTFND` | `DNI not exists` | 404 |
| `DELETED` | `Client marked as deleted` | 410 |
| `IOERR` | `Error writing/updating/deleting client` | 500 |
| `INVOP` | `OP. Error` (driver only) | - |

### Preserved business invariants

These are enforced identically by CLIESRV, `InMemoryClientRepository`, the Bean Validation on
the API (`@ValidDni`) and the Angular `dniValidator`:

1. A DNI is valid only when `DNI > 999999` and `DNI < 100000000` (1,000,000 - 99,999,999).
2. Create requires a valid DNI that does **not** exist in `TRCLIE` - including soft-deleted rows,
   so a deleted DNI can never be re-created.
3. Read, update and delete require a valid DNI that exists **and** is active (`CLDEUS` blank).
4. Checks run in the original order: valid -> exists -> active; the first failure is returned.
5. Delete is a soft delete: it stamps `CLDETS`/`CLDEUS` and the row stays in `TRCLIE`.
6. Every write stamps audit fields: create `CLCRTS`/`CLCRUS`, update `CLUPTS`/`CLUPUS`, delete
   `CLDETS`/`CLDEUS`, with `%timestamp()` and the job's current user (PSDS positions 254-263).
   Through jt400 that user is the IBM i profile the API connects with.
7. "Active" everywhere means `CLDEUS = ''`; list endpoints return active clients only.

See `java/README.md` and `web/README.md` to run phases 2 and 3.
