# web (Phase 3)

Angular 19 client for the clients REST API (`java/`).

* `/clients` - active clients list (delete from the list)
* `/clients/new` - create form
* `/clients/:dni` - detail with audit fields
* `/clients/:dni/edit` - edit form (DNI read-only)

DNI validation mirrors CLIESRV `dniIsValid` (`src/app/clients/dni.validator.ts`): integer,
`> 999999` and `< 100000000`. Server-side messages (e.g. `DNI already exists`) are shown verbatim.

```bash
npm ci
npm start          # http://localhost:4200, proxies /api -> http://localhost:8080 (proxy.conf.json)
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```
