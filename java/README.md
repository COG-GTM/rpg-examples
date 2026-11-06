# clients-api (Phase 2, MBA-3805)

Spring Boot 3.3 / Java 17 REST API over the CLIECRUD client module (`TRCLIE`). It runs on port 8080.

## Run

```bash
cd java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # default `java` may be JDK 11
mvn test                 # unit + web tests (no IBM i needed)
mvn spring-boot:run      # in-memory repository on http://localhost:8080
```

| Mode | How to select | Notes |
|---|---|---|
| In-memory (default) | `CLIENTS_REPOSITORY=memory` or unset | Same rules as CLIESRV, data lost on restart. Audit user from `CLIENTS_AUDIT_USER` (default `WEBAPP`, max 10 chars). |
| IBM i (jt400) | `CLIENTS_REPOSITORY=jt400` or `--spring.profiles.active=jt400` | Calls `CLIECRUD` → `CLIESRV` over IBM Toolbox for Java. Needs the `IBMI_*` settings below. Fails fast at startup when they're missing. |
| JPA (reserved) | `CLIENTS_REPOSITORY=jpa` | Hook only; startup fails with "not implemented yet". See below. |

Other settings: `PORT` (default 8080), `CLIENTS_CORS_ORIGINS` (default `http://localhost:4200`, comma-separated).

### IBM i settings (jt400 mode)

Supply these as environment variables only. Never commit them.

| Variable | Meaning |
|---|---|
| `IBMI_HOST` | IBM i host name |
| `IBMI_USER` / `IBMI_PASSWORD` | user profile and password |
| `IBMI_LIBRARY` | library holding `CLIECRUD` and `CLIESRV` |
| `IBMI_DATA_LIBRARY` | library holding `TRCLIE` (defaults to `IBMI_LIBRARY`) |
| `IBMI_CCSID` | CCSID of the character parameters (default 37) |

```bash
CLIENTS_REPOSITORY=jt400 IBMI_HOST=myibmi IBMI_USER=ME IBMI_PASSWORD=... \
IBMI_LIBRARY=CLIELIB IBMI_DATA_LIBRARY=CLIEDTA mvn spring-boot:run
```

How the jt400 path works:

- **Create/read/update/delete**: `CommandCall` runs `ADDLIBLE <IBMI_DATA_LIBRARY>`, because CLIECRUD opens `TRCLIE` unqualified (CPF2103 "already in list" is ignored). Then `ProgramCall` calls `<IBMI_LIBRARY>/CLIECRUD` in 5-parm structured mode: `op char(1)`, `dni zoned(8:0)`, `name char(30)`, `status char(63)` out, `client char(146)` out.
- **Decoding**: the status and client buffers are decoded with `AS400Text` (CCSID `IBMI_CCSID`) and `AS400ZonedDecimal` in `CliesrvCodec`. Non-OK status codes become problem responses, with the CLIESRV message as `detail`.
- **List**: the list uses JDBC (`AS400JDBCDataSource`, system naming, library list = data library):
  `SELECT CLDNI, CLNAME, CHAR(CLCRTS), CLCRUS, CHAR(CLUPTS), CLUPUS, CHAR(CLDETS), CLDEUS FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI`.
  `CHAR()` returns timestamps in the same 26-char *ISO form as the program buffer, which avoids JDBC calendar problems with the year-0001 sentinel.
- **Connections**: each program call opens and closes its own `AS400` connection. Add a connection pool before production use.

> The jt400 path is unit-tested only (codec byte layouts, and the repository against a fake gateway). It has not been run against a real IBM i. It depends on `CLIESRV`/structured `CLIECRUD` from the Phase 1 PR (MBA-3804).

### Future `JpaClientRepository`

`RepositoryConfig` reserves `CLIENTS_REPOSITORY=jpa`. A JPA implementation would map an `@Entity` directly onto `TRCLIE`:

- `@Id CLDNI NUMERIC(8,0)`, `CLNAME CHAR(30)`
- `CLCRTS/CLUPTS/CLDETS TIMESTAMP`, `CLCRUS/CLUPUS/CLDEUS CHAR(10)`
- the `0001-01-01-00.00.00.000000` sentinel becomes `null`
- it re-implements the CLIESRV rules in Java

Because it bypasses CLIESRV, the RPG and Java copies of the rules would have to be kept in sync. See `com.example.clients.repository.jpa`.

## API (Contract B)

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/clients` | | 200, `Client[]` of active clients ordered by dni |
| GET | `/clients/{dni}` | | 200, `Client` |
| POST | `/clients/{dni}` | `{"name": "..."}` | 201, `Client`, `Location: /clients/{dni}` |
| PUT | `/clients/{dni}` | `{"name": "..."}` | 200, `Client` |
| DELETE | `/clients/{dni}` | | 204 (soft delete: stamps `deletedAt/By`) |

`Client` JSON fields: `dni`, `name`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `deletedAt`, `deletedBy`, `active`.

- `*At` fields are ISO-8601 local date-time with microseconds (`2026-11-06T18:00:36.639640`), or `null` for the `0001-01-01` sentinel.
- `*By` fields are trimmed, or `null` when blank.
- `active` is true when `deletedBy` (CLDEUS) is blank.

### Errors

Errors are RFC 7807 `application/problem+json` with an extra `code` field. For CLIESRV codes, `detail` is the legacy message.

| code | HTTP | detail |
|---|---|---|
| INVDNI | 400 | `DNI not valid`: dni outside 1000000..99999999, non-numeric or overflowing |
| VALIDATION | 400 | e.g. `name: must not be blank`, `name: size must be between 0 and 30` |
| NOTFND | 404 | `DNI not exists` |
| DUPDNI | 409 | `DNI already exists`, including a soft-deleted DNI (it cannot be re-created) |
| DELETED | 410 | `Client marked as deleted`: read/update/delete of a soft-deleted client |
| IOERR / INVOP | 500 | I/O or IBM i failure / `OP. Error` |

Checks run in the same order as the RPG: INVDNI, then DUPDNI/NOTFND, then DELETED. An invalid dni wins over an invalid body.

CORS allows `http://localhost:4200` (the Angular dev server) and exposes `Location`.

### curl examples

```bash
curl -i -X POST -H 'Content-Type: application/json' -d '{"name":"ANA GARCIA"}' localhost:8080/clients/12345678
# 201, Location: /clients/12345678
curl -i localhost:8080/clients/12345678                     # 200
curl -i -X PUT -H 'Content-Type: application/json' -d '{"name":"ANA MARIA GARCIA"}' localhost:8080/clients/12345678
curl -i localhost:8080/clients                              # 200 [...]
curl -i -X DELETE localhost:8080/clients/12345678           # 204
curl -i localhost:8080/clients/12345678                     # 410 DELETED
curl -i -X POST -H 'Content-Type: application/json' -d '{"name":"X"}' localhost:8080/clients/999999
# 400 {"title":"Invalid DNI","status":400,"detail":"DNI not valid","code":"INVDNI",...}
```

## Layout

```
src/main/java/com/example/clients/
  api/          ClientController, ClientNameRequest, ProblemHandler (problem+json)
  config/       ClientsProperties, RepositoryConfig (repository selection), WebConfig (CORS)
  domain/       Client DTO, Dni rule, ClientErrorCode, ClientException
  repository/   ClientRepository
    memory/     InMemoryClientRepository
    jt400/      Jt400ClientRepository, CliecrudGateway (+ Jt400CliecrudGateway), CliesrvCodec, Jt400Properties
    jpa/        package-info: seam for the future JpaClientRepository
```
