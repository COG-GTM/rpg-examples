// In-memory mock of the Phase 2 Spring Boot clients API (REST contract B).
// Usage: node mock-api/server.mjs [--port 8080]
import http from 'node:http';

const port = Number(process.env.PORT ?? argValue('--port') ?? 8080);
const USER = process.env.MOCK_USER ?? 'QUSER';
const DNI_MIN = 999_999;
const DNI_MAX = 100_000_000;
const NAME_MAX = 30;

const MESSAGES = {
  INVDNI: 'DNI not valid',
  DUPDNI: 'DNI already exists',
  NOTFND: 'DNI not exists',
  DELETED: 'Client marked as deleted',
};
const MAX_BODY_BYTES = 16 * 1024;
const STATUS = { INVDNI: 400, VALIDATION: 400, NOTFND: 404, DUPDNI: 409, DELETED: 410, IOERR: 500 };
const TITLES = { 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 410: 'Gone', 500: 'Internal Server Error' };

/** @type {Map<number, object>} */
const clients = new Map();
seed(12345678, 'ANA PEREZ');
seed(23456789, 'JUAN GOMEZ');
seed(34567890, 'MARIA LOPEZ');

function argValue(flag) {
  const i = process.argv.indexOf(flag);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

function now() {
  // ISO-8601 local date-time with microseconds, like LocalDateTime from an RPG timestamp.
  const d = new Date();
  const pad = (n, w = 2) => String(n).padStart(w, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}000`;
}

function seed(dni, name) {
  clients.set(dni, newClient(dni, name));
}

function newClient(dni, name) {
  return {
    dni, name,
    createdAt: now(), createdBy: USER,
    updatedAt: null, updatedBy: null,
    deletedAt: null, deletedBy: null,
    active: true,
  };
}

class ApiError extends Error {
  constructor(code, detail = MESSAGES[code] ?? code) {
    super(detail);
    this.code = code;
    this.status = STATUS[code] ?? 500;
  }
}

function parseDni(raw) {
  if (!/^-?\d+$/.test(raw)) throw new ApiError('INVDNI');
  const dni = Number(raw);
  if (!(dni > DNI_MIN && dni < DNI_MAX)) throw new ApiError('INVDNI');
  return dni;
}

function parseName(body) {
  const name = body && typeof body.name === 'string' ? body.name : null;
  if (name === null || name.trim() === '') throw new ApiError('VALIDATION', 'name is required');
  if (name.length > NAME_MAX) throw new ApiError('VALIDATION', `name must be at most ${NAME_MAX} characters`);
  return name.trim();
}

function requireActive(dni) {
  const client = clients.get(dni);
  if (!client) throw new ApiError('NOTFND');
  if (!client.active) throw new ApiError('DELETED');
  return client;
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.on('data', (chunk) => {
      data += chunk;
      if (data.length > MAX_BODY_BYTES) {
        req.removeAllListeners('data');
        req.resume();
        reject(new ApiError('VALIDATION', `request body exceeds ${MAX_BODY_BYTES} bytes`));
      }
    });
    req.on('end', () => {
      if (!data) return resolve({});
      try { resolve(JSON.parse(data)); } catch { reject(new ApiError('VALIDATION', 'malformed JSON body')); }
    });
    req.on('error', reject);
  });
}

function send(res, status, body, headers = {}) {
  res.writeHead(status, { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...headers });
  res.end(body !== undefined ? JSON.stringify(body) : undefined);
}

function sendProblem(res, err, instance) {
  const status = err.status ?? 500;
  const code = err.code ?? 'IOERR';
  res.writeHead(status, { 'Content-Type': 'application/problem+json' });
  res.end(JSON.stringify({
    type: 'about:blank',
    title: TITLES[status] ?? 'Error',
    status,
    detail: err.message,
    instance,
    code,
  }));
}

async function handle(req, res) {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const path = url.pathname.replace(/\/+$/, '');
  const method = req.method;

  if (path === '/clients' && method === 'GET') {
    const list = [...clients.values()].filter((c) => c.active).sort((a, b) => a.dni - b.dni);
    return send(res, 200, list);
  }

  const match = /^\/clients\/([^/]+)$/.exec(path);
  if (!match) return sendProblem(res, Object.assign(new Error('No route'), { status: 404, code: 'NOROUTE' }), path);

  const dni = parseDni(decodeURIComponent(match[1]));
  switch (method) {
    case 'GET': {
      const client = requireActive(dni);
      return send(res, 200, client);
    }
    case 'POST': {
      const name = parseName(await readBody(req));
      if (clients.has(dni)) throw new ApiError('DUPDNI');
      const client = newClient(dni, name);
      clients.set(dni, client);
      return send(res, 201, client, { Location: `/clients/${dni}` });
    }
    case 'PUT': {
      const name = parseName(await readBody(req));
      const client = requireActive(dni);
      Object.assign(client, { name, updatedAt: now(), updatedBy: USER });
      return send(res, 200, client);
    }
    case 'DELETE': {
      const client = requireActive(dni);
      Object.assign(client, { deletedAt: now(), deletedBy: USER, active: false });
      return send(res, 204);
    }
    default:
      res.setHeader('Allow', 'GET, POST, PUT, DELETE');
      return sendProblem(res, Object.assign(new Error('Method not allowed'), { status: 405, code: 'INVOP' }), path);
  }
}

const server = http.createServer(async (req, res) => {
  const started = Date.now();
  try {
    await handle(req, res);
  } catch (err) {
    sendProblem(res, err instanceof ApiError ? err : Object.assign(err, { status: 500, code: 'IOERR' }), req.url);
  } finally {
    console.log(`${req.method} ${req.url} -> ${res.statusCode} (${Date.now() - started}ms)`);
  }
});

server.listen(port, () => console.log(`Mock clients API listening on http://localhost:${port}`));
