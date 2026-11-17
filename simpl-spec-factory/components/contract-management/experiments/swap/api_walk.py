#!/usr/bin/env python3
"""Same HTTP calls on two contract manager backends, statuses compared.

  api_walk.py <base url> <api key> <existing contractAgreementId> <label>

Prints one line per call (method, path, status, problem type) and the field names of the
agreement read. Uses only the OpenAPI operations (specs/api/contract-manager-be).
"""
import json, sys, uuid, urllib.request

base, key, aid, label = sys.argv[1].rstrip('/'), sys.argv[2], sys.argv[3], sys.argv[4]
R = lambda: str(uuid.uuid4())
rid, rnid, rdid = R(), R(), R()

def call(method, path, body=None, headers=None, key_=key):
    h = {'Accept': 'application/json'}
    if key_ is not None: h['X-Api-Key'] = key_
    if body is not None: h['Content-Type'] = 'application/json'
    h.update(headers or {})
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=20) as r: return r.status, r.read()
    except urllib.error.HTTPError as e: return e.code, e.read()

CALLS = [
    ('health', 'GET', '/health', None, {}, key),
    ('read existing', 'GET', f'/agreements/{aid}', None, {}, key),
    ('file existing', 'GET', f'/agreements/{aid}/file', None, {'Accept': 'text/plain'}, key),
    ('read unknown', 'GET', f'/agreements/{rid}', None, {}, key),
    ('read malformed id', 'GET', '/agreements/not-a-uuid', None, {}, key),
    ('file unknown', 'GET', f'/agreements/{rid}/file', None, {'Accept': 'text/plain'}, key),
    ('read no key', 'GET', f'/agreements/{aid}', None, {}, None),
    ('read wrong key', 'GET', f'/agreements/{aid}', None, {}, 'wrong-key'),
    ('read as xml', 'GET', f'/agreements/{aid}', None, {'Accept': 'application/xml'}, key),
    ('delete (no such method)', 'DELETE', f'/agreements/{aid}', None, {}, key),
    ('search all, empty body', 'POST', '/agreements/search', {}, {}, key),
    ('search by nid, empty body', 'POST', f'/agreements/{rnid}/search', {}, {}, key),
    ('search by nid, with assetId', 'POST', f'/agreements/{rnid}/search', {'assetId': 'asset-walk', 'consumerId': 'consumer-01', 'providerId': 'provider-01'}, {}, key),
    ('status unknown ids', 'PATCH', f'/agreements/{rid}/definitions/{rdid}/status', {'status': 'FINALIZED'}, {}, key),
    ('issue, empty body', 'POST', f'/credentials/agreements/{rid}/definitions/{rdid}/issue', {}, {}, key),
    ('create, empty body', 'POST', f'/credentials/agreements/{rid}/definitions/{rdid}/create', {}, {}, key),
    ('verification, empty body', 'PUT', f'/verifications/{rnid}', {}, {}, key),
    ('offer, unknown nid', 'POST', f'/contractOffers/{rnid}', {}, {}, key),
    ('offer accept, unknown nid', 'PATCH', f'/contractOffers/{rnid}/accept', {}, {}, key),
    ('offer update, unknown nid', 'PUT', f'/contractOffers/{rnid}/update', {'status': 'REJECTED'}, {}, key),
]
fields = None
for name, m, p, b, h, k in CALLS:
    st, raw = call(m, p, b, h, k)
    ptype = ''
    try:
        j = json.loads(raw or b'null')
        if isinstance(j, dict):
            ptype = j.get('type') or j.get('code') or ''
            if name == 'read existing' and st == 200: fields = sorted(j.keys())
    except Exception: pass
    print(f'{label}\t{name}\t{m}\t{st}\t{ptype}')
print(f'{label}\tagreement fields\t{",".join(fields or [])}')
