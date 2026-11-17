// Environment stand-in (4 Nov 2026) for SD Tooling and the signer of the data provider, only the five
// operations the PSO suite calls to prepare a live-signed self-description:
//   POST /v1/policies/access, POST /v1/policies/usage   ODRL policy from the request (no checks)
//   GET  /v2/schemas                                     the Data schema synced to SD Tooling (fixed)
//   GET  /v1/resourceAddresses/sharingMethods/{m}/templates   one template per sharing method (fixed)
//   POST /v1/selfDescriptions/finalized?schemaId=...     enrich, wrap as a VC and sign
// Enrichment: dct:conformsTo with the schema name and version (D59) and the four EDC registration ids.
// Signing: the proof profile of D22 with the fixed test key "provider-a" (D23), which the build's key
// registry for data space A already lists, through the build's own tool tools/sign-rd.mjs.
// Every operation needs a bearer token and does not check it.
import http from 'node:http';
import crypto from 'node:crypto';
import { sign, keyFor } from '/factory-tools/sign-rd.mjs';

const KEY = process.env.SIGNING_KEY || 'provider-a';
const SCHEMA = { id: process.env.DATA_SCHEMA || 'DataOfferingShape', version: process.env.DATA_SCHEMA_VERSION || '1' };

const policy = (body) => ({
  '@context': 'http://www.w3.org/ns/odrl.jsonld',
  '@type': 'Set',
  uid: `urn:uuid:${crypto.randomUUID()}`,
  permission: (body.permissions || []).map((p) => ({
    action: p.action, assignee: p.assignee,
    constraint: [
      p.fromDatetime && { leftOperand: 'dateTime', operator: 'gteq', rightOperand: p.fromDatetime },
      p.toDatetime && { leftOperand: 'dateTime', operator: 'lteq', rightOperand: p.toDatetime },
    ].filter(Boolean),
  })),
});

function finalize(schemaId, body) {
  const subject = { ...body.sdJson };
  subject['@context'] = { ...(subject['@context'] || {}), dct: 'http://purl.org/dc/terms/', simpl: 'http://w3id.org/gaia-x/simpl#' };
  subject['dct:conformsTo'] = { 'dct:schemaName': schemaId, 'dct:hasVersion': SCHEMA.version };
  subject['simpl:edcRegistration'] = {
    'rdf:type': { '@id': 'simpl:EdcRegistration' },
    'simpl:assetId': crypto.randomUUID(),
    'simpl:contractDefinitionId': crypto.randomUUID(),
    'simpl:accessPolicyId': crypto.randomUUID(),
    'simpl:servicePolicyId': crypto.randomUUID(),
  };
  const vc = {
    '@context': ['https://www.w3.org/2018/credentials/v1'],
    type: ['VerifiableCredential'],
    id: `urn:uuid:${crypto.randomUUID()}`,
    issuer: keyFor(KEY).did,
    issuanceDate: new Date().toISOString(),
    credentialSubject: subject,
  };
  return sign(vc, KEY);
}

http.createServer((req, res) => {
  let data = '';
  req.on('data', (c) => { data += c; });
  req.on('end', () => {
    const send = (s, b) => { res.writeHead(s, { 'content-type': 'application/json' }); res.end(JSON.stringify(b)); };
    const url = new URL(req.url, 'http://x');
    const p = url.pathname;
    if (p === '/health') return send(200, { status: 'UP' });
    if (!/^Bearer /i.test(req.headers.authorization || '')) return send(401, { status: 401, title: 'Unauthorized' });
    let body = {};
    try { body = data ? JSON.parse(data) : {}; } catch { return send(400, { status: 400, detail: 'body is not JSON' }); }
    if (req.method === 'POST' && (p === '/v1/policies/access' || p === '/v1/policies/usage')) return send(200, policy(body));
    if (req.method === 'GET' && p === '/v2/schemas') {
      return send(200, { schemas: [{ id: SCHEMA.id, name: SCHEMA.id, version: SCHEMA.version, resourceType: 'Data', status: 'PUBLISHED' }] });
    }
    const m = p.match(/^\/v1\/resourceAddresses\/sharingMethods\/([^/]+)\/templates$/);
    if (req.method === 'GET' && m) return send(200, [{ id: 1, label: `Data Template ${m[1]}`, sharingMethod: m[1], offeringType: url.searchParams.get('offeringType') }]);
    if (req.method === 'POST' && p === '/v1/selfDescriptions/finalized') {
      if (!body.sdJson || typeof body.sdJson !== 'object') return send(400, { status: 400, detail: 'sdJson is required' });
      return send(200, finalize(url.searchParams.get('schemaId') || SCHEMA.id, body));
    }
    return send(404, { status: 404 });
  });
}).listen(8080);
