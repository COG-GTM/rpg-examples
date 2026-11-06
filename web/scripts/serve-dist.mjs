#!/usr/bin/env node
// Fallback for `ng serve` (e.g. ENOSPC inotify limits): serves the built app from
// dist/web/browser and proxies /api/* to the API with the /api prefix stripped,
// matching proxy.conf.json. Usage: npm run build && npm run serve:dist
// Env: PORT (default 4200), API_TARGET (default http://localhost:8080)
import { createReadStream, existsSync, statSync } from 'node:fs';
import { createServer, request as httpRequest } from 'node:http';
import { request as httpsRequest } from 'node:https';
import { extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const PORT = Number(process.env.PORT ?? 4200);
const API_TARGET = new URL(process.env.API_TARGET ?? 'http://localhost:8080');
const request = API_TARGET.protocol === 'https:' ? httpsRequest : httpRequest;
const ROOT = resolve(fileURLToPath(new URL('.', import.meta.url)), '../dist/web/browser');
const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
  '.svg': 'image/svg+xml',
  '.txt': 'text/plain; charset=utf-8',
};

if (!existsSync(join(ROOT, 'index.html'))) {
  console.error(`No build found in ${ROOT}. Run "npm run build" first.`);
  process.exit(1);
}

function proxy(req, res) {
  const upstream = request(
    {
      hostname: API_TARGET.hostname,
      port: API_TARGET.port || (API_TARGET.protocol === 'https:' ? 443 : 80),
      method: req.method,
      path: req.url.replace(/^\/api/, '') || '/',
      headers: { ...req.headers, host: API_TARGET.host },
    },
    (up) => {
      res.writeHead(up.statusCode ?? 502, up.headers);
      up.pipe(res);
    },
  );
  // Same behaviour as the Angular dev proxy when the target is down: body-less 500.
  upstream.on('error', () => {
    if (!res.headersSent) res.writeHead(500);
    res.end();
  });
  req.pipe(upstream);
}

function serveStatic(req, res) {
  let path;
  try {
    path = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  } catch {
    res.writeHead(400).end();
    return;
  }
  let file = normalize(join(ROOT, path));
  if (!file.startsWith(ROOT) || !existsSync(file) || statSync(file).isDirectory()) {
    file = join(ROOT, 'index.html'); // SPA fallback for router paths
  }
  res.writeHead(200, { 'Content-Type': TYPES[extname(file)] ?? 'application/octet-stream' });
  createReadStream(file).pipe(res);
}

createServer((req, res) => (req.url.startsWith('/api/') || req.url === '/api' ? proxy(req, res) : serveStatic(req, res))).listen(
  PORT,
  () => console.log(`Serving ${ROOT} on http://localhost:${PORT} (/api -> ${API_TARGET.origin})`),
);
