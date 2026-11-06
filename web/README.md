# web/ — Angular client management UI (MBA-3806, Phase 3)

Angular 19 front end for the legacy `CLIECRUD` client module (`rpg ile/subprocedures and functions/clients_crud.rpgle`,
file `TRCLIE` / record `RETRCLIE`). It talks to the Phase 2 Spring Boot API (`java/`, MBA-3805) through
**Contract B**, or to the dependency-free mock in `mock-api/` so it runs without Java or an IBM i.

Standalone components, Reactive Forms, `HttpClient` and the router; plain CSS, no UI framework.

| Route | Screen |
| --- | --- |
| `/clients` | Active clients. Delete opens a confirm dialog, soft-deletes, then reloads the list. |
| `/clients/:dni` | Detail with every audit field and an Active/Deleted badge. |
| `/clients/new` | Create form. |
| `/clients/:dni/edit` | Edit form; the DNI is read-only. |

## Run

Requires Node 18.19+ / 20+.

```bash
cd web
npm ci
npm run mock-api     # terminal 1: Contract B mock on http://localhost:8080 (3 seeded clients)
npm start            # terminal 2: ng serve on http://localhost:4200, /api proxied to :8080
```

`proxy.conf.json` forwards `/api/*` to `http://localhost:8080` and strips `/api`, so `ClientService`
calls `/api/clients[/{dni}]` and the API serves `/clients[/{dni}]`. To use the real API, stop the mock
and start the Spring Boot app from `java/` on port 8080 instead.

If `ng serve` fails with `ENOSPC` (inotify watcher limit), serve the production build with the
fallback static server + proxy (same `/api` rewrite, same body-less 500 when the API is down):

```bash
npm run build
npm run serve:dist   # http://localhost:4200 (PORT / API_TARGET env vars to override)
```

## Test

```bash
npx ng build
npx ng test --watch=false --browsers=ChromeHeadless
# containers / root: npm run test:ci   (ChromeHeadlessNoSandbox launcher from karma.conf.js)
# Chrome not found: CHROME_BIN=/path/to/chrome npx ng test --watch=false --browsers=ChromeHeadless
```

Specs cover the DNI validator boundaries (999999 ✗, 1000000 ✓, 99999999 ✓, 100000000 ✗),
`ClientService` HTTP calls (`HttpTestingController`), problem+json error mapping, the form component's
inline/banner error handling, and the list's confirm-then-delete flow.

## Rules mirrored from the legacy program

- **DNI** (`dniIsValid`): digits only and `dni > 999999 and dni < 100000000` —
  "DNI must be between 1,000,000 and 99,999,999".
- **Name**: required, not blank, at most 30 characters (`CLNAME 30A`).

## Error handling

API errors are RFC 7807 `application/problem+json` with a `code` (`src/app/clients/api-error.ts`):

| `code` | HTTP | Message shown | Where |
| --- | --- | --- | --- |
| `INVDNI` | 400 | DNI not valid | inline on DNI |
| `VALIDATION` | 400 | problem `detail` | inline on Name |
| `NOTFND` | 404 | DNI not exists | banner |
| `DUPDNI` | 409 | DNI already exists | inline on DNI |
| `DELETED` | 410 | Client marked as deleted | banner |
| `IOERR` / other | 500 | problem `detail` | banner |
| — | 0, or 5xx with no problem body | API unreachable | banner |

The Angular dev proxy answers **HTTP 500 with an empty body** when the target is down (not status 0),
so a body-less 5xx is treated as "API unreachable", while a real 5xx problem+json keeps its `detail`.

## Mock API (`mock-api/server.mjs`)

Node `http` only, port 8080 (`PORT` to override), in memory, seeded with 3 clients; restart to reset.
Implements Contract B with the legacy guard order (DNI valid → exists / doesn't exist → active):

- `GET /clients` → 200, active clients ordered by DNI
- `GET /clients/{dni}` → 200 `Client`
- `POST /clients/{dni}` `{"name"}` → 201 `Client` + `Location: /clients/{dni}`
- `PUT /clients/{dni}` `{"name"}` → 200 `Client`
- `DELETE /clients/{dni}` → 204 (soft delete: stamps `deletedAt` / `deletedBy`)

A soft-deleted DNI still exists, so re-creating it returns 409 `DUPDNI`; reading, updating or deleting
it returns 410 `DELETED`. `Client` JSON is
`{dni, name, createdAt, createdBy, updatedAt, updatedBy, deletedAt, deletedBy, active}` with `null` for
empty timestamps/users; timestamps are ISO-8601 local date-times with microseconds. Audit users are
`MOCKAPI` (`MOCK_USER` to override).
