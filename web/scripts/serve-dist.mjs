// Serves the production build (dist/clients-web/browser) with the same /api proxy as proxy.conf.json.
// Fallback for VMs where `ng serve` hits inotify ENOSPC limits.
// Usage: npm run build && node scripts/serve-dist.mjs [--port 4200] [--api http://localhost:8080]
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const argValue = (flag) => {
  const i = process.argv.indexOf(flag);
  return i >= 0 ? process.argv[i + 1] : undefined;
};
const port = Number(argValue('--port') ?? process.env.PORT ?? 4200);
const api = new URL(argValue('--api') ?? process.env.API_URL ?? 'http://localhost:8080');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../dist/clients-web/browser');
const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css',
  '.ico': 'image/x-icon', '.svg': 'image/svg+xml', '.png': 'image/png', '.json': 'application/json',
};

function proxy(req, res) {
  const target = req.url.replace(/^\/api/, '') || '/';
  const upstream = http.request(
    { hostname: api.hostname, port: api.port || 80, path: target, method: req.method,
      headers: { ...req.headers, host: api.host } },
    (up) => { res.writeHead(up.statusCode ?? 502, up.headers); up.pipe(res); },
  );
  upstream.on('error', () => {
    res.writeHead(502, { 'Content-Type': 'application/problem+json' });
    res.end(JSON.stringify({ title: 'Bad Gateway', status: 502, detail: `API not reachable at ${api.origin}` }));
  });
  req.pipe(upstream);
}

function resolveStatic(url) {
  let urlPath;
  try {
    urlPath = decodeURIComponent(new URL(url, 'http://x').pathname);
  } catch {
    return null;
  }
  const file = path.resolve(root, '.' + urlPath);
  const rel = path.relative(root, file);
  if (rel === '' || rel.startsWith('..') || path.isAbsolute(rel)) return null;
  return fs.existsSync(file) && fs.statSync(file).isFile() ? file : null;
}

function serveStatic(req, res) {
  const file = resolveStatic(req.url) ?? path.join(root, 'index.html'); // SPA fallback for client-side routes
  res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream' });
  fs.createReadStream(file).pipe(res);
}

if (!fs.existsSync(path.join(root, 'index.html'))) {
  console.error(`No build found at ${root}. Run "npm run build" first.`);
  process.exit(1);
}

http
  .createServer((req, res) => (req.url === '/api' || req.url.startsWith('/api/') ? proxy(req, res) : serveStatic(req, res)))
  .listen(port, () => console.log(`Serving ${root} on http://localhost:${port} (/api -> ${api.origin})`));
