# clients-api (Phase 2)

Spring Boot 3.3 / Java 17 REST API over the RPG client module (`TRCLIE`, record `RETRCLIE`).
CRUD rules stay in RPG: the `jt400` repository calls the `CLIECRUD` program (bound to the
`CLIESRV` service program) in structured 5-parameter mode. The default in-memory repository
implements the same rules so the API and the Angular app (`web/`) run without an IBM i.

## Run

```bash
cd java
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run   # http://localhost:8080
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test
```

## REST contract

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/clients` | – | 200 `Client[]` (active only, ordered by DNI) |
| GET | `/clients/{dni}` | – | 200 `Client` |
| POST | `/clients/{dni}` | `{"name":"..."}` | 201 `Client`, `Location: /clients/{dni}` |
| PUT | `/clients/{dni}` | `{"name":"..."}` | 200 `Client` |
| DELETE | `/clients/{dni}` | – | 204 (soft delete: stamps `deletedAt`/`deletedBy`) |

```json
{ "dni": 12345678, "name": "ANA PEREZ",
  "createdAt": "2026-11-03T10:15:30.123456", "createdBy": "QUSER",
  "updatedAt": null, "updatedBy": null, "deletedAt": null, "deletedBy": null,
  "active": true }
```

Validation: `dni` 1,000,000..99,999,999 (`dniIsValid`); `name` required, not blank, max 30 chars.

Errors are RFC 7807 `application/problem+json` with `code` (CLIESRV status) and `detail` (RPG message):

| code | HTTP | detail |
|---|---|---|
| `INVDNI` | 400 | DNI not valid (also non-numeric `{dni}`) |
| `VALIDATION` | 400 | name is required / name must be at most 30 characters / body missing or malformed |
| `NOTFND` | 404 | DNI not exists |
| `DUPDNI` | 409 | DNI already exists (also for a soft-deleted DNI, as in the legacy program) |
| `DELETED` | 410 | Client marked as deleted |
| `IOERR` | 500 | IBM i request failed / unexpected error (`INVOP` also maps to 500) |

Precedence follows the RPG code: `INVDNI`, then `DUPDNI`/`NOTFND`, then `DELETED`.
CORS allows `http://localhost:4200` (`clients.cors-origins`).

## Configuration

| Env var | Property | Default | Notes |
|---|---|---|---|
| `CLIENTS_REPOSITORY` | `clients.repository` | `memory` | `memory` or `jt400` (or run with profile `jt400`) |
| `CLIENTS_AUDIT_USER` | `clients.audit-user` | `WEBAPP` | user stamped by the in-memory repo (max 10 chars) |
| `IBMI_HOST` | `clients.jt400.host` | – | required for jt400 |
| `IBMI_USER` | `clients.jt400.user` | – | required; CLIESRV stamps this profile in `CL*US` |
| `IBMI_PASSWORD` | `clients.jt400.password` | – | required; never commit it |
| `IBMI_LIBRARY` | `clients.jt400.library` | – | library holding `CLIECRUD` / `CLIESRV` |
| `IBMI_DATA_LIBRARY` | `clients.jt400.data-library` | `IBMI_LIBRARY` | library holding `TRCLIE` (added to the server job's library list) |
| `IBMI_CCSID` | `clients.jt400.ccsid` | `37` | EBCDIC CCSID of the character fields |

### Switch to jt400

```bash
export CLIENTS_REPOSITORY=jt400 IBMI_HOST=my.ibmi.host IBMI_USER=MYUSER IBMI_LIBRARY=MYLIB
read -rs IBMI_PASSWORD && export IBMI_PASSWORD
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run
```

- CRUD: `ProgramCall` to `/QSYS.LIB/<IBMI_LIBRARY>.LIB/CLIECRUD.PGM` with
  `op char(1)`, `dni zoned(8:0)`, `name char(30)`, `status char(63)` (out), `client char(146)` (out).
  `CliesrvCodec` encodes/decodes the `cliesrv_status_t` / `cliesrv_client_t` layouts
  (`0001-01-01-00.00.00.000000` and blanks map to `null`).
- List: JDBC (`AS400JDBCDriver`)
  `SELECT CLDNI, CLNAME, CHAR(CLCRTS), CLCRUS, CHAR(CLUPTS), CLUPUS, CHAR(CLDETS), CLDEUS FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI`.
- The jt400 path is unit-tested only (codec + repository with a fake gateway); it has not
  been run against a real IBM i.

## Code map

```
com.example.clients
├── api/          ClientController, ClientNameRequest, @ValidDni, GlobalExceptionHandler
├── config/       ClientsProperties, RepositoryConfig (selects the repository), WebConfig (CORS)
├── domain/       Client, Dni, ClientErrorCode, ClientException
└── repository/
    ├── ClientRepository        port: create / findActive / update / softDelete / findAllActive
    ├── memory/                 InMemoryClientRepository (default)
    └── jt400/                  Jt400ClientRepository, CliecrudGateway, Jt400CliecrudGateway, CliesrvCodec
```

### Future: `JpaClientRepository`

To read/write TRCLIE directly, add a JPA entity mapped onto TRCLIE (`CLDNI` id, `CLNAME`,
`CLCRTS`/`CLCRUS`, `CLUPTS`/`CLUPUS`, `CLDETS`/`CLDEUS`; convert the `0001-01-01` sentinel and
blank users to `null`), implement `ClientRepository` with the same INVDNI → DUPDNI/NOTFND → DELETED
order, and register it in `RepositoryConfig` under `clients.repository=jpa`. Controllers and tests
against the port do not change. Note that this bypasses `CLIESRV`, so the rules would then live in
two places.

## Sample calls

```bash
curl -s localhost:8080/clients
curl -si -X POST localhost:8080/clients/12345678 -H 'Content-Type: application/json' -d '{"name":"ANA PEREZ"}'
curl -s localhost:8080/clients/12345678
curl -s -X PUT localhost:8080/clients/12345678 -H 'Content-Type: application/json' -d '{"name":"ANA MARIA PEREZ"}'
curl -si -X DELETE localhost:8080/clients/12345678
curl -s localhost:8080/clients/12345678      # 410 DELETED
curl -s localhost:8080/clients/999999        # 400 INVDNI
```
