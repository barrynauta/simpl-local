// Schema Manager stand-in (digest section 6.4). Read-only, from the seed files. No authentication.
'use strict';
const http = require('http');
const fs = require('fs');
const path = require('path');

const SEED = process.env.SEED_DIR || '/seed/schemas';
const PORT = Number(process.env.PORT || 8000);

const RESOURCE_TYPES = {
  DataOfferingShape: 'DATA',
  ApplicationOfferingShape: 'APPLICATION',
  InfrastructureOfferingShape: 'INFRASTRUCTURE',
};

function title(name) {
  return name.replace(/([a-z0-9])([A-Z])/g, '$1 $2');
}

function load() {
  const out = [];
  for (const f of fs.readdirSync(SEED).filter((x) => x.endsWith('.ttl')).sort()) {
    const name = f.replace(/\.ttl$/, '');
    const search = !RESOURCE_TYPES[name];
    out.push({
      schemaId: name,
      name,
      title: title(name),
      description: search ? 'SHACL shape used for federated search forms.' : `SHACL shape for ${title(name).toLowerCase()} resource descriptions.`,
      resourceType: search ? null : RESOURCE_TYPES[name],
      schemaType: search ? 'SEARCH' : 'RESOURCE',
      version: '1',
      content: fs.readFileSync(path.join(SEED, f), 'utf8'),
    });
  }
  return out;
}

const schemas = load();

function send(res, status, type, body) {
  res.writeHead(status, { 'Content-Type': type });
  res.end(typeof body === 'string' ? body : JSON.stringify(body));
}

function problem(res, status, title, detail) {
  send(res, status, 'application/problem+json', {
    type: status === 404 ? 'urn:problem-type:simpl:notFound' : 'urn:problem-type:simpl:badRequest',
    title, status, detail,
  });
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const parts = url.pathname.split('/').filter(Boolean).map(decodeURIComponent);
  if (url.pathname === '/health') return send(res, 200, 'application/json', { status: 'UP' });
  if (parts[0] === 'stub' && parts[1] === 'slow') {
    // The "slow" catalogue: answers after 10 s with 503.
    req.resume();
    return setTimeout(() => problem(res, 503, 'Service Unavailable', 'The slow catalogue is not available.'), 10000);
  }
  if (req.method !== 'GET') return problem(res, 405, 'Method Not Allowed', 'Only GET is supported.');
  if (parts.length === 1 && parts[0] === 'schemas') {
    const rt = url.searchParams.get('resourceType');
    const st = url.searchParams.get('schemaType');
    const list = schemas
      .filter((s) => !rt || (s.resourceType || '').toLowerCase() === rt.toLowerCase())
      .filter((s) => !st || s.schemaType === st)
      .map(({ content, ...meta }) => meta);
    return send(res, 200, 'application/json', { schemas: list });
  }
  if (parts.length === 3 && parts[0] === 'schemas') {
    const s = schemas.find((x) => x.schemaId === parts[1]);
    if (!s || (parts[2] !== 'content' && parts[2] !== s.version)) {
      return problem(res, 404, 'Resource not found', `Schema with identifier '${parts[1]}' was not found`);
    }
    return send(res, 200, 'text/turtle', s.content);
  }
  return problem(res, 404, 'Not Found', 'No such path.');
});

server.listen(PORT, () => console.log(`schema-stub listening on ${PORT} with ${schemas.length} schemas`));
