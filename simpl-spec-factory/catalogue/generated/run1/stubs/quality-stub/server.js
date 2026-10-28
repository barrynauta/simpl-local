// Quality Scoring and Metadata Quality Profile stand-in (digest section 6.5). No authentication.
'use strict';
const http = require('http');
const crypto = require('crypto');

const PORT = Number(process.env.PORT || 8000);
const DIM = ['completeness', 'accuracy', 'validity'];
const DIM_NAME = { completeness: 'Completeness', accuracy: 'Accuracy', validity: 'Validity' };
const entries = new Map(); // RD id -> { overall, dimensions } | { mode }

const CONTEXT = { '@vocab': 'https://w3id.org/simpl/quality#', simpl: 'https://w3id.org/simpl#' };

function profile(withThresholds) {
  const cls = (c) => `urn:simpl:quality:classification:${c}`;
  return {
    '@context': CONTEXT,
    '@graph': [
      {
        '@id': 'urn:simpl:mqp:default', '@type': 'MetadataQualityProfile', profileVersion: '1.0.0',
        name: 'Default Metadata Quality Profile',
        includesDimension: DIM.map((d) => ({ '@id': `urn:simpl:quality:dimension:${d}` })),
        includesThreshold: withThresholds ? [
          { '@id': 'urn:simpl:quality:threshold:overall' },
          { '@id': 'urn:simpl:quality:threshold:completeness' },
        ] : [],
        hasClassification: ['Red', 'Yellow', 'Green'].map((c) => ({ '@id': cls(c) })),
        hasScoreRange: ['red', 'yellow', 'green'].map((r) => ({ '@id': `urn:simpl:quality:range:${r}` })),
      },
      ...DIM.map((d) => ({ '@id': `urn:simpl:quality:dimension:${d}`, '@type': 'QualityDimension', name: DIM_NAME[d] })),
      ...['Red', 'Yellow', 'Green'].map((c) => ({ '@id': cls(c), '@type': 'QualityClassification', label: c })),
      { '@id': 'urn:simpl:quality:range:red', '@type': 'ScoreRange', minScore: 0, maxScore: 60, rangeClassification: { '@id': cls('Red') } },
      { '@id': 'urn:simpl:quality:range:yellow', '@type': 'ScoreRange', minScore: 60, maxScore: 80, rangeClassification: { '@id': cls('Yellow') } },
      { '@id': 'urn:simpl:quality:range:green', '@type': 'ScoreRange', minScore: 80, maxScore: 100, rangeClassification: { '@id': cls('Green') } },
      { '@id': 'urn:simpl:quality:threshold:overall', '@type': 'Threshold', thresholdValue: 70, classificationScope: 'overall' },
      {
        '@id': 'urn:simpl:quality:threshold:completeness', '@type': 'Threshold', thresholdValue: 90,
        classificationScope: { '@id': 'urn:simpl:quality:dimension:completeness' },
      },
    ],
  };
}

function qar(vc, entry) {
  const cs = (vc && vc.credentialSubject) || {};
  const rdId = cs['@id'] || 'urn:unknown';
  const rdType = (cs['rdf:type'] && cs['rdf:type']['@id']) || cs['@type'] || 'simpl:DataOffering';
  const scores = { overall: 100, dimensions: {} };
  if (entry && typeof entry.overall === 'number') scores.overall = entry.overall;
  for (const d of DIM) {
    const v = entry && entry.dimensions ? entry.dimensions[DIM_NAME[d]] : undefined;
    scores.dimensions[d] = typeof v === 'number' ? v : 100;
  }
  const uid = crypto.randomUUID();
  const sdsId = `urn:simpl:quality:self-description-score:${uid}`;
  const dimIds = DIM.map((d) => `urn:simpl:quality:dimension-score:${d}:${uid}`);
  const ruleId = `urn:simpl:quality:rule-evaluation:required-name:${uid}`;
  const graph = [
    { '@id': rdId, '@type': rdType },
    { '@id': 'urn:simpl:mqp:default', '@type': 'MetadataQualityProfile', profileVersion: '1.0.0' },
    ...DIM.map((d) => ({ '@id': `urn:simpl:quality:dimension:${d}`, '@type': 'QualityDimension', name: DIM_NAME[d] })),
    ...DIM.map((d, i) => ({
      '@id': dimIds[i], '@type': 'DimensionScore', dimension: `urn:simpl:quality:dimension:${d}`,
      score: scores.dimensions[d], maxScore: 100,
    })),
    { '@id': sdsId, '@type': 'SelfDescriptionScore', computedOn: rdId, score: scores.overall, maxScore: 100, dimensionScores: dimIds },
    {
      '@id': ruleId, '@type': 'RuleEvaluation', name: 'Required name', evaluatedAgainst: 'urn:simpl:mqp:default',
      computedOn: rdId, dimension: 'urn:simpl:quality:dimension:completeness', result: 'passed', score: 100,
    },
  ];
  if (!(entry && entry.mode === 'invalid')) {
    graph.push({
      '@id': `urn:uuid:${crypto.randomUUID()}`, '@type': 'QualityAssessmentRecord', computedOn: rdId,
      evaluatedAgainst: 'urn:simpl:mqp:default', profileVersion: '1.0.0', evaluationId: crypto.randomUUID(),
      correlationId: crypto.randomUUID(), assessmentDate: new Date().toISOString(), selfDescriptionScore: sdsId,
      ruleEvaluations: [ruleId],
    });
  }
  return { '@context': CONTEXT, '@graph': graph };
}

function send(res, status, type, body) {
  res.writeHead(status, { 'Content-Type': type });
  res.end(body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body));
}

function problem(res, status, title, detail) {
  send(res, status, 'application/problem+json', {
    type: status === 404 ? 'urn:problem-type:simpl:notFound' : status === 400 ? 'urn:problem-type:simpl:badRequest'
      : 'urn:problem-type:simpl:internalServerError',
    title, status, detail,
  });
}

function readBody(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const p = url.pathname;
  const body = await readBody(req);
  if (p === '/health') return send(res, 200, 'application/json', { status: 'UP' });

  if (p === '/v1/score-validation' && req.method === 'POST') {
    let vc;
    try { vc = JSON.parse(body); } catch (e) { return problem(res, 400, 'Bad Request', 'Body is not JSON.'); }
    const id = vc && vc.credentialSubject && vc.credentialSubject['@id'];
    const entry = id ? entries.get(id) : undefined;
    if (entry && entry.mode === 'error') return problem(res, 500, 'Internal Server Error', 'Scoring failed (stub mode error).');
    const answer = () => send(res, 200, 'application/ld+json', qar(vc, entry));
    if (entry && entry.mode === 'timeout') return setTimeout(answer, 15000);
    return answer();
  }

  if (p === '/v1/profiles/active' && req.method === 'GET') {
    const name = url.searchParams.get('schemaName');
    if (name === 'NoProfileShape') {
      return problem(res, 404, 'Not Found', `No active Metadata Quality Profile is linked to schema '${name}'.`);
    }
    return send(res, 200, 'application/ld+json', profile(name !== 'NoThresholdShape'));
  }

  const m = p.match(/^\/stub\/scores\/(.+)$/);
  if (m) {
    const id = decodeURIComponent(m[1]);
    if (req.method === 'PUT') {
      let e;
      try { e = JSON.parse(body); } catch (err) { return problem(res, 400, 'Bad Request', 'Body is not JSON.'); }
      entries.set(id, e);
      return send(res, 200, 'application/json', { id, ...e });
    }
    if (req.method === 'DELETE') {
      entries.delete(id);
      return send(res, 204, 'application/json');
    }
    if (req.method === 'GET') {
      return entries.has(id) ? send(res, 200, 'application/json', entries.get(id)) : problem(res, 404, 'Not Found', 'No entry.');
    }
  }
  return problem(res, 404, 'Not Found', 'No such path.');
});

server.listen(PORT, () => console.log(`quality-stub listening on ${PORT}`));
