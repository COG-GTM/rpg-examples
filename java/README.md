# clients-api (Phase 2)

Spring Boot 3 / Java 17 REST API for the TRCLIE clients, backed by the CLIESRV RPGLE service program.

| Method | Path | Body | Success | CLIESRV call |
|--------|------|------|---------|--------------|
| GET | `/clients` | - | 200 active clients | JDBC `SELECT ... WHERE CLDEUS = ''` |
| GET | `/clients/{dni}` | - | 200 `Client` | `readClient` |
| POST | `/clients/{dni}` | `{"name": "..."}` | 201 `Client` | `createClient` |
| PUT | `/clients/{dni}` | `{"name": "..."}` | 200 `Client` | `updateClient` |
| DELETE | `/clients/{dni}` | - | 204 | `deleteClient` (soft) |

Errors are RFC 7807 problem details; `code` is the CLIESRV status code and `detail` its message
(e.g. `{"code":"DUPDNI","detail":"DNI already exists"}`).

## Layout

* `domain/` - `Client` DTO (record), `@ValidDni`, `ClientRules`, `ClientStatus`, `ClientOperationException`
* `repository/ClientRepository` - port; implementations selected by `clients.repository`
  * `memory/InMemoryClientRepository` (`in-memory`, default) - same rules as CLIESRV, no IBM i needed
  * `jt400/Jt400ClientRepository` (`jt400`) - `ProgramCall` to `CLIECRUD` (structured mode), layouts in `CliesrvCodec`
  * future: a JPA implementation mapped directly on `TRCLIE` can implement the same interface
* `api/` - `ClientController`, `ApiExceptionHandler`

## Run

```bash
mvn test
mvn spring-boot:run                      # in-memory store on :8080

# against IBM i (TRCLIE, CLIESRV and CLIECRUD built with cl ile/build_cliesrv.clle)
CLIENTS_REPOSITORY=jt400 IBMI_HOST=... IBMI_USER=... IBMI_PASSWORD=... IBMI_LIBRARY=... mvn spring-boot:run
```
