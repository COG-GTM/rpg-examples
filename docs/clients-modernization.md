# Client management modernization (TRCLIE)

How the legacy `CLIECRUD` program becomes a reusable RPGLE service program
(`CLIENTSV`) and is consumed by a Spring Boot REST API and an Angular UI —
without changing a single business rule.

```
 Angular (web/)  --HTTP /api/clients-->  Spring Boot (java/)  --jt400 ServiceProgramCall-->  CLIENTSV *SRVPGM  -->  TRCLIE
                                                         \--JDBC SELECT (list only)----------------------------------^
 Legacy callers  --CALL CLIECRUD (op, dni, name)-->  CLIECRUD *PGM (thin wrapper)  --bound call-->  CLIENTSV
```

| Step | What | Where |
|------|------|-------|
| 0 | Legacy program (before) | `git show main:"rpg ile/subprocedures and functions/clients_crud.rpgle"` |
| 1 | Service program + public copybook + binder source | `rpg ile/subprocedures and functions/clientsv*.rpgle`, `clientsv.bnd` |
| 1 | Backward-compatible `CLIECRUD` wrapper | `rpg ile/subprocedures and functions/clients_crud.rpgle` |
| 1 | IBM i build | `cl ile/build_clientsv.clle` |
| 2 | REST API | `java/` |
| 3 | UI | `web/` |

## 1. Schema — `TRCLIE` (confirmed, not inferred)

The DDS already exists in `database-files-examples/trclie.pf`; nothing had to be inferred.

| Field | DDS type | Meaning | `client_t` subfield | JSON |
|-------|----------|---------|---------------------|------|
| `CLDNI` | `8S 0`, unique key | DNI | `cldni zoned(8:0)` | `dni` |
| `CLNAME` | `30A` | Name | `clname char(30)` | `name` |
| `CLCRTS` / `CLCRUS` | `Z` / `10A` | Created at / by | `timestamp` / `char(10)` | `createdAt` / `createdBy` |
| `CLUPTS` / `CLUPUS` | `Z` / `10A` | Updated at / by | `timestamp` / `char(10)` | `updatedAt` / `updatedBy` |
| `CLDETS` / `CLDEUS` | `Z` / `10A` | Deleted at / by (soft delete) | `timestamp` / `char(10)` | `deletedAt` / `deletedBy` |

A client is **active** when `CLDEUS = ''` (JSON `active: true`). Unset timestamps are
`0001-01-01-00.00.00.000000` on IBM i and `null` in JSON.

## 2. Business rules preserved

Identical in `CLIENTSV`, the Java in-memory emulator, and (for DNI) the Angular form:

* **DNI valid**: `dni > 999999 and dni < 100000000` (7 or 8 digits, 1,000,000–99,999,999).
* **Create**: valid DNI → DNI must **not** exist (a soft-deleted row still exists, so its DNI can't be reused).
* **Read / Update / Delete**: valid DNI → DNI exists → client active.
* **Delete is soft**: stamps `CLDETS`/`CLDEUS`, the row stays.
* Create stamps `CLCRTS`/`CLCRUS`; update stamps `CLUPTS`/`CLUPUS`; user = job user.

## 3. Phase 1 — `CLIENTSV` service program

### Public API (`CLIENTSV_H`, include with `/copy QRPGLESRC,CLIENTSV_H`)

| Export | Signature | Notes |
|--------|-----------|-------|
| `clientsvExecute` | `(in likeds(clientsv_in_t) const, out likeds(clientsv_out_t))` | Dispatcher on `in.op` = C/R/U/D; the single entry point used by Java |
| `createClient` / `readClient` / `updateClient` / `deleteClient` | same as above | One per operation |
| `dniIsValid` | `ind (dni zoned(8:0) const)` | Range check only |
| `dniExists` | `ind (dni zoned(8:0) const)` | `chain(n)` + `%found` |
| `clientActive` | `ind (dni zoned(8:0) const)` | Found and `cldeus = ''` |

Binder signature: `CLIENTSV V1.0` (`clientsv.bnd`). Append new exports at the end only.

### Wire contract (byte layouts are frozen — `java/.../ClientsvCodec.java` encodes them)

| DS | Bytes | Layout |
|----|-------|--------|
| `clientsv_in_t` | 39 | `op char(1)` · `dni zoned(8:0)` · `name char(30)` |
| `client_t` | 146 | `cldni zoned(8:0)` · `clname char(30)` · 3 × (`timestamp` 26 + `char(10)` user) |
| `clientsv_out_t` | 209 | `success ind` · `code char(10)` · `message char(52)` · `client client_t` |

Result codes (`out.code`) and the legacy messages they carry (`out.message`):

| Code | Message (unchanged from `CLIECRUD`) | HTTP |
|------|-------------------------------------|------|
| `OK` | `Client created/read/updated/deleted` | 200 / 201 |
| `INVDNI` | `DNI not valid` | 400 |
| `DUPDNI` | `DNI already exists` | 409 |
| `NOTFND` | `DNI not exists` | 404 |
| `DELETED` | `Client marked as deleted` | 410 |
| `INVOP` | `OP. Error` | 400 |
| `IOERR` | `I/O error on TRCLIE` (+ `: record locked` / file status) | 503 |

### `CLIECRUD` wrapper

Same `extpgm('CLIECRUD')` signature as before (`op 1a`, `dni 8s 0`, `name 30a options(*nopass)`)
and the same `DSPLY` output. It just builds `clientsv_in_t` and calls `clientsvExecute`,
so existing `CALL CLIECRUD PARM('R' 12345678)` callers keep working.

### Build on IBM i

1. Upload sources into `MYLIB` (member names are listed at the top of `cl ile/build_clientsv.clle`):
   `QRPGLESRC/CLIENTSV`, `QRPGLESRC/CLIENTSV_H`, `QRPGLESRC/CLIECRUD`, `QSRVSRC/CLIENTSV`, `QDDSSRC/TRCLIE`.
2. `CRTBNDCL PGM(MYLIB/BUILDCLSV) SRCFILE(MYLIB/QCLSRC) SRCMBR(BUILD_CLIENTSV)`
3. `CALL MYLIB/BUILDCLSV PARM('MYLIB')` — creates `TRCLIE` if missing, `CLIENTSV` *SRVPGM, binding
   directory `CLIENTSV`, and `CLIECRUD` *PGM.
4. Smoke test: `CALL CLIECRUD PARM('C' 12345678 'ANA')`, then `PARM('R' 12345678)`.

`TRCLIE` is opened unqualified, so the job's library list must contain the data library
(the Java API adds it with `ADDLIBLE` on its connection).

## 4. Phase 2a — REST API (`java/`, Spring Boot 3, Java 17)

| Method | Path | CLIENTSV op | Success |
|--------|------|-------------|---------|
| `POST` | `/clients/{dni}` body `{"name": "..."}` | `C` | 201 + `Location` + client JSON |
| `GET` | `/clients/{dni}` | `R` | 200 + client JSON |
| `PUT` | `/clients/{dni}` body `{"name": "..."}` | `U` | 200 + client JSON |
| `DELETE` | `/clients/{dni}` | `D` (soft) | 200 + client JSON with `deletedAt`/`deletedBy` |
| `GET` | `/clients?includeDeleted=false` | — (SQL `SELECT … FROM TRCLIE`) | 200 + array; for the UI list, since `CLIENTSV` has no list op |

Failures are RFC 7807 `application/problem+json` with an extra `code` (table above); body
problems (`name` blank / > 30 chars / malformed JSON) are `400 VALIDATION`. The DNI is
validated before the body, so a bad DNI always yields `INVDNI` first, as in RPG.

```json
{ "status": 410, "title": "Client deleted", "detail": "Client marked as deleted", "code": "DELETED", "instance": "/clients/31222333" }
```

### Backends (`CLIENTS_BACKEND`)

| Value | Behaviour |
|-------|-----------|
| `memory` (default) | `InMemoryClientsvGateway` — an emulator of `CLIENTSV` with the same guard chain and soft delete. Seeds 3 demo clients (`CLIENTS_SEED=false` to disable). For local dev, tests and demos without an IBM i. |
| `jt400` | `Jt400ClientsvGateway` — `ServiceProgramCall` to `IBMI_LIBRARY/CLIENTSV(clientsvExecute)` with the 39/209-byte buffers; list via JDBC. |

| Env var | Default | |
|---------|---------|---|
| `IBMI_HOST`, `IBMI_USER`, `IBMI_PASSWORD` | — | required for `jt400`; never commit them |
| `IBMI_LIBRARY` | — | library with `CLIENTSV` |
| `IBMI_DATA_LIBRARY` | `IBMI_LIBRARY` | library with `TRCLIE` (added to the library list) |
| `IBMI_CCSID` | `37` | EBCDIC CCSID of the character fields |
| `CLIENTS_AUDIT_USER` | `WEBAPP` | audit user for the memory backend (IBM i uses the job user) |
| `CLIENTS_CORS_ORIGINS` | `http://localhost:4200` | |
| `PORT` | `8080` | |

```bash
cd java
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test          # codec, emulator, gateway byte path, REST
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run
curl -s -XPOST localhost:8080/clients/12345678 -H 'Content-Type: application/json' -d '{"name":"ANA"}'
```

## 5. Phase 2b — Angular UI (`web/`, Angular 19)

* `/clients` — list (active by default, "Show deleted" toggle), view/edit/delete with confirmation.
* `/clients/:dni` — detail with all audit fields and Active/Deleted badge.
* `/clients/new`, `/clients/:dni/edit` — form; DNI digits-only and `> 999999 and < 100000000`
  (`dni.validator.ts`, same rule as `dniIsValid`), name required, ≤ 30 chars.
* API errors are shown with the legacy messages (`DNI already exists`, …); `DUPDNI`/`INVDNI`
  inline on the DNI field. An unreachable API shows "API unreachable".

```bash
cd web
npm ci
npm start                 # ng serve on :4200, proxies /api -> :8080
# if ng serve fails with ENOSPC (inotify limit):
npx ng build && npm run serve:dist
npm run test:ci           # Karma, headless Chrome
```

## 6. Verification status and limits

* Java (`mvn test`) and Angular (`npm run test:ci`, `ng build`) run in CI-like fashion locally.
* `CLIENTSV`/`CLIECRUD` were checked with `rpglint` (copybook inlined): `clientsv.rpgle` is clean;
  the only findings in the wrapper are "no reference" on public copybook definitions it doesn't
  use (constants/prototypes), which is expected for a shared header.
* **Not verified here:** RPG compilation, the binder source and the jt400 call against a real
  IBM i (no IBM i was available). The jt400 byte path is tested against the emulator via
  `ClientsvCodec`; run the IBM i smoke test above before switching `CLIENTS_BACKEND=jt400`.
