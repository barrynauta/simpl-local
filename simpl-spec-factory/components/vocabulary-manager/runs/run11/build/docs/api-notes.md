# API notes

Base URL: `http://localhost:8196/v1` (through the proxy). The contract the build implements is
[openapi-decided.yaml](openapi-decided.yaml): the published `vocabulary_openapi-tier1.yaml` with the
changes of D79 (and D91, D110, D116, D125) applied by `tools/make_openapi_decided.py`. The
published file is not changed (D39). `DecidedContractTest` compares the route table and the limits in
the code with that file (D93).

## Tokens and roles

Get a token from `http://localhost:8193/realms/simpl/protocol/openid-connect/token` (client
`vocabulary-tests`, password grant for the test users). The token must carry the audience
`vocabulary-manager-be` and the issuer `http://localhost:8193/realms/simpl` (E5, E7).

| Role | Operations |
|---|---|
| GA_VOCABULARY_VIEWER | every GET, POST /vocabularies/searches, POST /external-vocabulary/searches, POST /external-vocabularies/content |
| GA_VOCABULARY_ADMIN | everything above plus POST /vocabularies, POST .../versions, both PATCH, POST and DELETE /external-vocabularies |
| none | GET /health only |

## Error bodies

All errors are `application/problem+json` (RFC 9457). Types (D48): `urn:problem-type:belgif:` for
400, 404, 500, 503; `urn:problem-type:simpl:` for 401, 403, 405, 406, 409, 413, 415. 400 issues carry
`in` and `name`; request shape issues have type `urn:problem-type:belgif:input-validation:schemaViolation`,
content issues `...:invalidInput` with a `location`. A validation report holds at most 1000 issues
and then has `"truncated": true` (D125). Every response carries `X-Correlation-Id`.

## Points worth knowing

- Integers in paths and queries are digits only, 1 to 2147483647; pageSize at most 100.
- Any unknown, repeated or (on POST, PATCH, DELETE) any query parameter gives 400.
- Unknown form fields and JSON properties give 400, one issue per name. A multipart part with an
  empty name is ignored (D121).
- `Accept`: JSON operations accept `application/json`, `application/problem+json`, `*/*`,
  `application/*`; downloads `text/turtle`, `*/*`, `application/*`. Others: 406.
- Downloads: `Content-Type: text/turtle` (no charset), the bytes as uploaded,
  `Content-Disposition: attachment; filename="<Name>.ttl"` or `"<Name>-v<n>.ttl"`.
- Paging links put `page` and `pageSize` first, then the other parameters in request order; a page
  beyond the last has `prev` pointing to the last page.
- Searches are not paged: `page` 1, `pageSize` 100, `total`, `totalPages` 0 or 1, no links.
- `GET /health`: `UP`, `DEGRADED` (store answers, no realm keys and the last key fetch failed) or
  `DOWN` with 503.
- 503 bodies carry `Retry-After: 5`: store unreachable or timed out, circuit breaker open, identity
  provider keys not fetchable, more than 4 uploads in progress, conditional write retries exhausted.
- The proxy answers itself for TRACE (405 with Allow), malformed requests (400) and bodies above
  7 MB (413), with problem+json bodies (D122).
