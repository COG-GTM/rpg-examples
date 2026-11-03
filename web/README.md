# Client management UI (Angular)

Phase 3 of the CLIECRUD modernization: an Angular 19 single-page app (standalone components,
Reactive Forms, HttpClient, router) for the RPG client management module. It consumes the Spring
Boot clients API (REST contract B) through a `/api` proxy.

## Screens

| Route | Screen |
| --- | --- |
| `/clients` | Active clients (DNI, name, created) with view / edit / delete (confirm, soft delete) |
| `/clients/new` | Create form (DNI + name) |
| `/clients/:dni` | Detail with all audit fields and an Active / Deleted badge |
| `/clients/:dni/edit` | Edit form (DNI read-only, name) |

Validation mirrors the RPG rules (`src/app/clients/dni.validator.ts`):

- DNI: required, integer, `> 999999` and `< 100000000` → "DNI must be between 1,000,000 and 99,999,999" (`dniValidator`)
- Name: required, not blank, max 30 characters

API errors (RFC 7807 `application/problem+json` with a `code` property) are mapped in
`src/app/clients/api-error.ts` and shown inline next to the field and as a toast:

| `code` | HTTP | Message |
| --- | --- | --- |
| `DUPDNI` | 409 | DNI already exists |
| `NOTFND` | 404 | DNI not exists |
| `DELETED` | 410 | Client marked as deleted |
| `INVDNI` | 400 | DNI not valid |
| `VALIDATION` / other | 400 / 500 | the problem `detail` |

## Prerequisites

- Node.js 20+ and npm
- Google Chrome / Chromium for the headless unit tests (set `CHROME_BIN` if it is not on the `PATH`)

```bash
cd web
npm install
```

## Run against the mock API

`mock-api/server.mjs` is a dependency-free, in-memory implementation of contract B on port 8080
(seeded with three clients, same DNI / name / active rules and problem+json errors as the real API).

```bash
# terminal 1
npm run mock-api            # http://localhost:8080/clients

# terminal 2
npm start                   # ng serve on http://localhost:4200, /api -> http://localhost:8080
```

The proxy (`proxy.conf.json`) strips the `/api` prefix, so the UI calls `/api/clients` and the
API serves `/clients`.

## Run against the real Spring Boot API

Start the Phase 2 API from `java/` on port 8080 (see `java/README.md`), then run `npm start`.
Nothing else changes — the UI only knows about `/api/clients`.

## If `ng serve` fails with ENOSPC (inotify watcher limit)

Either raise the limit (`sudo sysctl fs.inotify.max_user_watches=524288`) or serve the
production build with the same `/api` proxy:

```bash
npm run build
npm run serve:dist          # http://localhost:4200, /api -> http://localhost:8080
# options: node scripts/serve-dist.mjs --port 4300 --api http://localhost:8081
```

## Tests and build

```bash
npx ng test --watch=false --browsers=ChromeHeadless
# on containers/VMs where Chrome needs --no-sandbox:
npm run test:ci             # ng test --watch=false --browsers=ChromeHeadlessCI
npm run build               # ng build (production) -> dist/clients-web
```

Specs cover the `dniValidator` boundaries (999999 / 1000000 / 99999999 / 100000000), the
`ClientService` HTTP calls (`HttpTestingController`), the problem+json → message mapping and the
form's inline error handling.
