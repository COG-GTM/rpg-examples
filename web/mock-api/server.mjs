#!/usr/bin/env node
// Dependency-free mock of the Phase 2 clients API (Contract B, MBA-3805), so the
// Angular UI runs without Java. Mirrors the legacy CLIECRUD rules:
//   guard order: DNI valid -> exists (create: doesn't exist) -> active
//   a soft-deleted DNI still exists, so it can't be re-created (DUPDNI)
//   read/update/delete of a soft-deleted DNI -> 410 DELETED
// Usage: node mock-api/server.mjs   (PORT env var, default 8080)
import { createServer } from 'node:http';

const PORT = Number(process.env.PORT ?? 8080);
const USER = (process.env.MOCK_USER ?? 'MOCKAPI').slice(0, 10);
const NAME_MAX = 30;

const MESSAGES = {
  INVDNI: 'DNI not valid',
  DUPDNI: 'DNI already exists',
  NOTFND: 'DNI not exists',
  DELETED: 'Client marked as deleted',
};
const STATUS = { INVDNI: 400, VALIDATION: 400, NOTFND: 404, DUPDNI: 409, DELETED: 410, IOERR: 500, INVOP: 500 };
const TITLES = { 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 410: 'Gone', 500: 'Internal Server Error' };

/** ISO-8601 local date-time with microseconds, like Java's LocalDateTime. */
function now() {
  const d = new Date();
  const pad = (n, w = 2) => String(n).padStart(w, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}000`
  );
}

/** In-memory TRCLIE. Rows keep the record layout; the JSON view is built by toJson. */
const trclie = new Map();

function seed(dni, name, createdAt, createdBy, extra = {}) {
  trclie.set(dni, {
    dni, name, createdAt, createdBy,
    updatedAt: null, updatedBy: null, deletedAt: null, deletedBy: null,
    ...extra,
  });
}
seed(12345678, 'JUAN PEREZ', '2026-10-01T09:15:00.000000', 'QPGMR');
seed(23456789, 'MARIA GOMEZ', '2026-10-02T10:30:00.000000', 'QPGMR', {
  updatedAt: '2026-10-15T16:45:12.500000', updatedBy: 'CLERK01',
});
seed(34567890, 'CARLOS RODRIGUEZ', '2026-10-03T11:00:00.000000', 'CLERK01');

const isActive = (row) => !row.deletedBy;
const toJson = (row) => ({ ...row, active: isActive(row) });

/** Legacy dniIsValid: digits only and 999999 < dni < 100000000. */
function parseDni(text) {
  if (!/^\d+$/.test(text)) return null;
  const dni = Number(text);
  return dni > 999999 && dni < 100000000 ? dni : null;
}

class ApiError extends Error {
  constructor(code, detail) {
    super(detail ?? MESSAGES[code] ?? code);
    this.code = code;
  }
}

function send(res, status, body, headers = {}) {
  const payload = body === undefined ? '' : JSON.stringify(body);
  res.writeHead(status, {
    ...(body === undefined ? {} : { 'Content-Type': headers['Content-Type'] ?? 'application/json' }),
    ...headers,
  });
  res.end(payload);
}

function sendProblem(res, err, instance) {
  const status = STATUS[err.code] ?? 500;
  send(
    res,
    status,
    { type: 'about:blank', title: TITLES[status] ?? 'Error', status, detail: err.message, instance, code: err.code },
    { 'Content-Type': 'application/problem+json' },
  );
}

async function readName(req) {
  let raw = '';
  for await (const chunk of req) raw += chunk;
  let body;
  try {
    body = raw ? JSON.parse(raw) : {};
  } catch {
    throw new ApiError('VALIDATION', 'Request body must be JSON: {"name": "..."}');
  }
  const name = body?.name;
  if (typeof name !== 'string' || name.trim() === '') throw new ApiError('VALIDATION', 'name: must not be blank');
  if (name.length > NAME_MAX) throw new ApiError('VALIDATION', `name: size must be between 0 and ${NAME_MAX}`);
  return name.trim();
}

function requireActive(dni) {
  const row = trclie.get(dni);
  if (!row) throw new ApiError('NOTFND');
  if (!isActive(row)) throw new ApiError('DELETED');
  return row;
}

async function handle(req, res) {
  const url = new URL(req.url ?? '/', 'http://localhost');
  const path = url.pathname.replace(/\/+$/, '') || '/';

  // CORS for direct calls from http://localhost:4200 (the dev proxy doesn't need it).
  res.setHeader('Access-Control-Allow-Origin', 'http://localhost:4200');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Access-Control-Expose-Headers', 'Location');
  if (req.method === 'OPTIONS') return send(res, 204);

  if (path === '/clients') {
    if (req.method !== 'GET') return send(res, 405, undefined, { Allow: 'GET' });
    const active = [...trclie.values()].filter(isActive).sort((a, b) => a.dni - b.dni).map(toJson);
    return send(res, 200, active);
  }

  const match = /^\/clients\/([^/]+)$/.exec(path);
  if (!match) {
    return sendProblem(res, new ApiError('NOTFND', `No route for ${path}`), path);
  }
  try {
    const dni = parseDni(decodeURIComponent(match[1]));
    if (dni === null) throw new ApiError('INVDNI');
    switch (req.method) {
      case 'GET':
        return send(res, 200, toJson(requireActive(dni)));
      case 'POST': {
        const name = await readName(req);
        if (trclie.has(dni)) throw new ApiError('DUPDNI');
        const row = {
          dni, name, createdAt: now(), createdBy: USER,
          updatedAt: null, updatedBy: null, deletedAt: null, deletedBy: null,
        };
        trclie.set(dni, row);
        return send(res, 201, toJson(row), { Location: `/clients/${dni}` });
      }
      case 'PUT': {
        const name = await readName(req);
        const row = requireActive(dni);
        Object.assign(row, { name, updatedAt: now(), updatedBy: USER });
        return send(res, 200, toJson(row));
      }
      case 'DELETE': {
        const row = requireActive(dni);
        Object.assign(row, { deletedAt: now(), deletedBy: USER });
        return send(res, 204);
      }
      default:
        return send(res, 405, undefined, { Allow: 'GET,POST,PUT,DELETE' });
    }
  } catch (err) {
    if (err instanceof ApiError) return sendProblem(res, err, path);
    console.error(err);
    return sendProblem(res, new ApiError('IOERR', 'Unexpected mock API error'), path);
  }
}

createServer((req, res) => {
  handle(req, res).catch((err) => {
    console.error(err);
    if (!res.headersSent) sendProblem(res, new ApiError('IOERR', 'Unexpected mock API error'), req.url);
  });
}).listen(PORT, () => {
  console.log(`Mock clients API (Contract B) on http://localhost:${PORT}/clients — ${trclie.size} seeded clients`);
});
