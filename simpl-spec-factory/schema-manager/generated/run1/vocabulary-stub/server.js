// Vocabulary Manager stub, exactly the D60 contract:
//   GET /vocabularies/{name}/content -> 200 text/turtle with the stored content, or 404
//   PUT /vocabularies/{name}/content (body text/turtle) -> 204, seeds or replaces
// In memory, no authentication. GET /health is for the container healthcheck.
const http = require('http');

const store = new Map();
const PORT = parseInt(process.env.PORT || '8099', 10);
const PATH = /^\/vocabularies\/([^/]+)\/content\/?$/;

const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://stub');
  if (url.pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('ok');
    return;
  }
  const m = PATH.exec(url.pathname);
  if (!m) {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('not found');
    return;
  }
  const name = decodeURIComponent(m[1]);
  if (req.method === 'GET' || req.method === 'HEAD') {
    if (!store.has(name)) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('vocabulary not found');
      return;
    }
    const body = store.get(name);
    res.writeHead(200, { 'Content-Type': 'text/turtle', 'Content-Length': body.length });
    res.end(req.method === 'HEAD' ? undefined : body);
    return;
  }
  if (req.method === 'PUT') {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => {
      store.set(name, Buffer.concat(chunks));
      res.writeHead(204);
      res.end();
    });
    return;
  }
  res.writeHead(405, { Allow: 'GET, PUT' });
  res.end();
});

server.listen(PORT, () => console.log(`vocabulary-stub listening on ${PORT}`));
process.on('SIGTERM', () => server.close(() => process.exit(0)));
