# CLIECRUD — Functional Requirements & Modernization Spec

| | |
|---|---|
| Legacy program | `CLIECRUD` — `rpg ile/subprocedures and functions/clients_crud.rpgle` (pre-refactor) |
| Data | Physical file `TRCLIE`, record format `RETRCLIE` — `database-files-examples/trclie.pf` |
| Target | Service program `CLIESRV` + thin wrapper program `CLIECRUD` + DB2 external procedures |

## 1. Purpose

CLIECRUD maintains a client master (`TRCLIE`) keyed by national ID number (DNI). It
supports create, read, update and soft delete, with a fixed validation chain and audit
stamps on every write. This document records the legacy behavior as testable requirements
and defines the service-program contract that replaces it so the same logic can be called
from RPGLE, SQL (JDBC/ODBC) and, later, a Java/Angular front end through a REST layer.

## 2. Legacy system summary

### 2.1 Entry point

```
CALL CLIECRUD PARM(op dni [name])
  op    CHAR(1)      'C' | 'R' | 'U' | 'D'   (case-sensitive)
  dni   ZONED(8,0)
  name  CHAR(30)     optional (options(*nopass))
```

`mainp` opens `TRCLIE` (update + add, keyed, `usropn`), dispatches on `op` to
`createClient` / `readClient` / `updateClient` / `deleteClient`, then closes the file. An
unknown `op` displays `OP. Error`. All feedback goes to the job log / console via `DSPLY`;
the caller gets nothing back.

### 2.2 Data — `TRCLIE` (`RETRCLIE`, UNIQUE key `CLDNI`)

| Field | Type | Meaning | Written by |
|---|---|---|---|
| `CLDNI`  | 8S 0 | Client DNI (key) | create |
| `CLNAME` | 30A  | Client name | create, update |
| `CLCRTS` | Z    | Created timestamp | create |
| `CLCRUS` | 10A  | Created by user | create |
| `CLUPTS` | Z    | Last updated timestamp | update |
| `CLUPUS` | 10A  | Last updated by user | update |
| `CLDETS` | Z    | Deleted timestamp | delete |
| `CLDEUS` | 10A  | Deleted by user | delete |

Unset timestamps are `0001-01-01-00.00.00.000000`; unset users are blank.

## 3. Functional requirements

### FR-1 Operations
The system shall expose four operations — Create, Read, Update, Delete — each keyed by DNI.

### FR-2 DNI rule
A DNI is valid when it has 7 or 8 digits: `999999 < DNI < 100000000`, i.e.
1,000,000 – 99,999,999. Zero, negative and ≤ 6-digit values are invalid (`dniIsValid`).

### FR-3 Create
1. `dniIsValid` must be true, else **DNI not valid**.
2. `dniExists` must be false, else **DNI already exists**. This applies to soft-deleted
   clients too: a deleted DNI can never be re-created.
3. All record fields are cleared (`clearFields`), then `CLDNI`, `CLNAME`, `CLCRTS =
   current timestamp`, `CLCRUS = audit user` are set and the record is written.
4. Update and delete stamps remain unset.

### FR-4 Read
1. Validation chain **dniIsValid → dniExists → clientActive**; first failure wins with
   **DNI not valid** / **DNI not exists** / **Client marked as deleted**.
2. On success returns DNI and name (legacy displays `<dni>-<name>`).

### FR-5 Update
1. Same chain as FR-4.
2. Sets `CLNAME = name`, `CLUPTS = current timestamp`, `CLUPUS = audit user`. Create
   stamps are untouched.

### FR-6 Soft delete
1. Same chain as FR-4.
2. Sets `CLDETS = current timestamp`, `CLDEUS = audit user`; the record is **not** removed.
3. A client is *active* if and only if `CLDEUS` is blank (`clientActive`). `CLDETS` is not
   consulted.
4. After deletion, Read / Update / Delete return **Client marked as deleted**; Create
   returns **DNI already exists**.

### FR-7 Audit user
The audit user stamped into `CLCRUS` / `CLUPUS` / `CLDEUS` is the job user name from
PSDS positions 254–263.

### FR-8 Invalid operation
Any `op` other than `C`, `R`, `U`, `D` is rejected with **OP. Error** and no file change.

### 3.1 Behavior matrix

| Op | DNI invalid | DNI not on file | Active client | Soft-deleted client |
|---|---|---|---|---|
| C | DNI not valid | **write record** | DNI already exists | DNI already exists |
| R | DNI not valid | DNI not exists | **return client** | Client marked as deleted |
| U | DNI not valid | DNI not exists | **update name + upd stamps** | Client marked as deleted |
| D | DNI not valid | DNI not exists | **set del stamps** | Client marked as deleted |

### 3.2 Legacy defects and quirks found

| # | Finding | Handling in target |
|---|---|---|
| Q1 | `C`/`U` without the optional name parameter reference an unpassed parm (pointer error / MCH3601). | Wrapper returns status 91 **Name required**; core API takes `name` as required. |
| Q2 | `dniExists` / `clientActive` use `chain` on an update file, leaving the record locked until the next I/O. | Lookups use `chain(n)`; only Update/Delete take a lock, immediately before the write. |
| Q3 | Between the checks and the final `chain`, another job could delete the client; the legacy program would still update it. | Soft-delete flag is re-checked under the lock (`lockActiveClient`). |
| Q4 | If the final `chain` is not found, the legacy program silently does nothing. | Returns **DNI not exists**. |
| Q5 | No success feedback for C/U/D; callers cannot tell outcome. | Every call returns status + message + record image. |
| Q6 | The PSDS field is named `currentUser` but positions 254–263 hold the *job* user. Under JDBC/ODBC server jobs (QZDASOINIT) that is `QUSER`, not the connecting or end user. | Kept as the default for parity (FR-7); callers can pass an explicit `actor` (see §4.3). |
| Q7 | `op` is case-sensitive (`'c'` → OP. Error). | Kept. |
| Q8 | Messages use legacy wording (`DNI not exists`). | Kept verbatim for parity; status codes are the stable contract. |
| Q9 | From a command line, a numeric literal is passed as packed 15,5 and does not map to `ZONED(8,0)`. | Documented: pass the DNI as a character literal, e.g. `'12345678'`. |
| Q10 | Duplicate-key write (race with another job) would end the program with an unhandled exception. | `on-error 01021` maps it to **DNI already exists**. |

## 4. Target design

```
 RPGLE callers ──► CLIECRUD (wrapper, DSPLY only here)
                        │  /include clients_crud_h.rpgleinc
 Other RPGLE ───────────┼──────────────► CLIESRV  (*SRVPGM, ACTGRP(*CALLER))
                        │                 createClient / readClient /
 JDBC/ODBC ──► CLIELIB.CLIENT_* ───────►  updateClient / deleteClient
 (Java, REST)    (external SQL procs)     clientCreateSql / ... (flat adapters)
                                                │
                                              TRCLIE
```

| Artifact | File |
|---|---|
| Service module `CLIESRV` (NOMAIN, no DSPLY) | `rpg ile/subprocedures and functions/clients_crud.rpgle` |
| Public copybook (prototypes, templates, status codes) | `rpg ile/subprocedures and functions/clients_crud_h.rpgleinc` |
| Binder source (exports + signature) | `rpg ile/subprocedures and functions/clients_crud.bnd` |
| Wrapper program `CLIECRUD` (legacy CALL interface) | `rpg ile/subprocedures and functions/clients_crud_pgm.rpgle` |
| Regression driver `CLIETEST` | `rpg ile/subprocedures and functions/clients_crud_test.rpgle` |
| Build script | `cl ile/build_clients_crud.clle` |
| DB2 external procedures | `sql-db2-examples/clients_crud_procedures.sql` |

### 4.1 Data contracts (`clients_crud_h.rpgleinc`)

```
cliClient_t                         cliResponse_t
  dni        zoned(8:0)               status   int(10)
  name       char(30)                 message  varchar(80)
  createdTs  timestamp                client   likeds(cliClient_t)
  createdBy  char(10)
  updatedTs  timestamp
  updatedBy  char(10)
  deletedTs  timestamp
  deletedBy  char(10)
  active     ind        (*on when deletedBy is blank)
```

`response.client` holds the record image after the operation on success and is cleared on
any failure.

### 4.2 Status codes

| Code | Constant | Message | Suggested HTTP |
|---|---|---|---|
| 0  | `CLI_OK` | Client created / found / updated / deleted | 200 (201 for create) |
| 10 | `CLI_DNI_INVALID` | DNI not valid | 400 |
| 20 | `CLI_DNI_EXISTS` | DNI already exists | 409 |
| 30 | `CLI_DNI_NOT_FOUND` | DNI not exists | 404 |
| 40 | `CLI_DELETED` | Client marked as deleted | 410 |
| 50 | `CLI_RECORD_LOCKED` | Client record is locked by another job | 409 / 423 |
| 90 | `CLI_INVALID_OP` | OP. Error (wrapper only) | — |
| 91 | `CLI_NAME_REQUIRED` | Name required (wrapper only) | — |
| 99 | `CLI_IO_ERROR` | Unable to open / write / update TRCLIE | 500 |

### 4.3 Exported procedures

| Procedure | Parameters | Validation chain | Effect |
|---|---|---|---|
| `createClient` | `dni const, name const, response, actor const *nopass:*omit` | dniIsValid → !dniExists | write; `CLCRTS/CLCRUS` |
| `readClient` | `dni const, response` | dniIsValid → dniExists → clientActive | none |
| `updateClient` | `dni const, name const, response, actor const *nopass:*omit` | same | `CLNAME, CLUPTS/CLUPUS` |
| `deleteClient` | `dni const, response, actor const *nopass:*omit` | same | `CLDETS/CLDEUS` (soft) |

All four return `int(10)` equal to `response.status`, so callers can write
`if createClient(dni: name: resp) = CLI_OK;`.

`actor`: when passed and not blank it is stamped as the audit user; otherwise the job user
(FR-7). A REST layer that authenticates end users should pass the end user's ID here (Q6).

Inputs are `const`, so RPGLE callers can pass literals and expressions.

### 4.4 SQL adapters and stored procedures

DB2 cannot pass RPG data structures, so the service program also exports flat adapters
(`clientCreateSql`, `clientReadSql`, `clientUpdateSql`, `clientDeleteSql`) that call the
core procedures and spread `cliResponse_t` into scalar OUT parameters. They are registered
as `CLIELIB.CLIENT_CREATE / _READ / _UPDATE / _DELETE` with `LANGUAGE RPGLE PARAMETER
STYLE GENERAL PROGRAM TYPE SUB NO SQL`.

| SQL type | RPG type |
|---|---|
| `NUMERIC(8,0)` | `zoned(8:0)` |
| `CHAR(n)` | `char(n)` |
| `VARCHAR(80)` | `varchar(80)` |
| `INTEGER` | `int(10)` |
| `TIMESTAMP` | `timestamp` |

Java (JDBC) example:

```java
try (CallableStatement cs = conn.prepareCall(
        "CALL CLIELIB.CLIENT_READ(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
    cs.setBigDecimal(1, new BigDecimal(12345678));
    cs.registerOutParameter(2, Types.INTEGER);
    cs.registerOutParameter(3, Types.VARCHAR);
    cs.registerOutParameter(4, Types.CHAR);
    // 5..11: TIMESTAMP, CHAR, TIMESTAMP, CHAR, TIMESTAMP, CHAR, CHAR
    cs.execute();
    int status = cs.getInt(2);   // map with table 4.2
}
```

Suggested REST surface for the Angular client:

| Method & path | Procedure | Body |
|---|---|---|
| `POST /clients` | `CLIENT_CREATE` | `{ "dni": 12345678, "name": "..." }` |
| `GET /clients/{dni}` | `CLIENT_READ` | — |
| `PUT /clients/{dni}` | `CLIENT_UPDATE` | `{ "name": "..." }` |
| `DELETE /clients/{dni}` | `CLIENT_DELETE` | — (soft delete) |

### 4.5 Runtime behavior

- **File lifecycle**: `TRCLIE` is opened on first use (`ensureOpen`) and stays open for the
  life of the activation group. `CLIESRV` is `ACTGRP(*CALLER)`; `CLIECRUD` and `CLIETEST`
  run in `ACTGRP(*NEW)`, so the file is closed when they return — same as the legacy
  open/close per call.
- **Locking**: reads never lock. Update/Delete lock with `chain`, re-check `CLDEUS`, then
  `update ... %fields(...)` only the columns they own; a lock wait timeout (`01218`) returns
  status 50.
- **No commitment control**: like the legacy program, writes are immediate. A SQL caller's
  transaction does not roll back `TRCLIE` changes.
- **Versioning**: new exports must be appended to `clients_crud.bnd` without changing the
  signature; removing or reordering exports requires a new signature and a rebuild of callers.

## 5. Build and run

Prerequisite: `TRCLIE` exists in the target library (built from `database-files-examples/trclie.pf`).

```
CRTBNDCL PGM(MYLIB/BLDCLIE) SRCSTMF('/home/me/rpg-examples/cl ile/build_clients_crud.clle')
CALL MYLIB/BLDCLIE PARM('MYLIB' '/home/me/rpg-examples')
CALL MYLIB/CLIETEST
CALL MYLIB/CLIECRUD PARM('C' '12345678' 'JOHN DOE')
RUNSQLSTM SRCSTMF('/home/me/rpg-examples/sql-db2-examples/clients_crud_procedures.sql') COMMIT(*NONE)
```

(Replace `CLIELIB` in the SQL script with your library first.)

## 6. Verification — `CLIETEST`

| Test | Requirement | Expectation |
|---|---|---|
| T01–T03 | FR-2 | 6-digit and negative DNIs rejected (10); 1,000,000 accepted by the DNI rule |
| T04 | FR-3, FR-7 | Create returns 0 with record image, create stamps set, active |
| T05 | FR-3.2 | Duplicate create returns 20 |
| T06–T07 | FR-4 | Read active returns 0 + name; unknown DNI returns 30 |
| T08–T09 | FR-5 | Update changes name + update stamps; unknown DNI returns 30 |
| T10 | FR-6 | Delete sets delete stamps, record kept, `active = *off` |
| T11–T15 | FR-6.4 | After delete: R/U/D return 40, C returns 20; delete of unknown DNI returns 30 |
| T16–T18 | §4.3 | Explicit `actor` is stamped; omitted actor falls back to job user |

Legacy-parity check through the wrapper: the same `CALL CLIECRUD` sequences produce the
same console lines as before (`Creating` / `Reading` / `Updating` / `Deleting`, then the
legacy error text or `<dni>-<name>` for a successful read).

## 7. Open questions

1. Should the audit default move from the job user to the current user profile (PSDS
   358–367) or the SQL `USER` register before the REST layer goes live (Q6)?
2. Should Create be allowed to *reactivate* a soft-deleted DNI? Today it is permanently
   blocked (FR-3.2).
3. Should `TRCLIE` be journaled and opened under commitment control so SQL callers get
   transactional behavior?
