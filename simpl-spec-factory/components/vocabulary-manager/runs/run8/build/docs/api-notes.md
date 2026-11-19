# API notes

## Base URL and contract

- Base URL: `http://localhost:8196/v1` in this stack (E2, E4; every URL comes from the environment). Through the UI
  origin the same API is at `http://localhost:4433/v1`.
- Published contract: `specs/api/vocabulary_openapi-tier1.yaml` (unchanged, D39). The behaviour built is the decided
  one: [openapi-decided.yaml](openapi-decided.yaml) is the published file with the changes of D79 applied (D87).
- `href` values and `Location` headers are relative to the API base (`/vocabularies/<Name>/versions/<n>`).
  `contentHref` is absolute: `<contentBase>/content/<Name>[/versions/<n>]`, with contentBase from
  `VOCABULARY_CONTENT_BASE_URL` (default `https://authority.example/vocabulary-manager`). The same contentBase marks
  internal references in uploaded content (D12).
- Times are UTC to the second with `Z` (D58).

## Authentication and roles

- Bearer JWT from Keycloak realm `simpl` (E1). The backend validates the signature with the realm keys, the issuer
  (`ISSUER_URI`, one value), the audience `vocabulary-manager-be` (E5) and the expiry. Scopes are not checked (D16, D40).
- Roles from `realm_access.roles`: GA_VOCABULARY_VIEWER for every GET and the read-only POSTs
  (`/vocabularies/searches`, `/external-vocabulary/searches`, `/external-vocabularies/content`); GA_VOCABULARY_ADMIN
  for everything, including the writes (POST `/vocabularies`, POST `.../versions`, both PATCH, POST and DELETE
  `/external-vocabularies`).
- `GET /v1/health` needs no token.
- Creator (`createdBy`) is the token's `email`, or `preferred_username` without one (D27). Logs carry only the token
  subject (D80).

## Problem types (D48)

| Status | type | When |
|---|---|---|
| 400 | `urn:problem-type:belgif:badRequest` | request shape (issues `schemaViolation`), content validation (issues `invalidInput` with `location`), firewall refusal (no issues) |
| 401 | `urn:problem-type:simpl:unauthorized` | no token, invalid token, wrong issuer or audience |
| 403 | `urn:problem-type:simpl:forbidden` | role missing |
| 404 | `urn:problem-type:belgif:resourceNotFound` | vocabulary, version, external vocabulary or path not found |
| 405 | `urn:problem-type:simpl:methodNotAllowed` | undeclared or unknown method, with `Allow` |
| 406 | `urn:problem-type:simpl:notAcceptable` | Accept not servable (D63) |
| 409 | `urn:problem-type:simpl:conflict` | duplicate name, same status, identical version, external URI taken |
| 413 | `urn:problem-type:simpl:payloadTooLarge` | file part over 5 MB or request over 6 MB |
| 415 | `urn:problem-type:simpl:unsupportedMediaType` | not multipart on the POST uploads, not JSON on PATCH and searches |
| 500 | `urn:problem-type:belgif:internalServerError` | unexpected error, no internals in the body |
| 503 | `urn:problem-type:belgif:serviceUnavailable` | store or identity provider not available, `Retry-After: 5` |

Every error body is `application/problem+json` with an `instance` of the form `urn:uuid:<random>`. Keeping belgif
types for 400, 404, 500 and 503 is a recorded deviation from API-46 (D48, see `DEVIATIONS.md`).

## Order of checks

405, firewall 400, 401, 403, 406, 415, 413, 400 request shape, 404, 409, content validation (syntax, then semantic).
Details in [architecture.md](architecture.md#request-pipeline).

## Decided changes to the published contract (D79)

Stricter than the published file:

1. 401 and 403 on every secured operation (D17); 503 with Retry-After on every secured operation (D44).
2. `pageSize` at most 100 (D22); a page beyond the last returns an empty page, `prev` points to the last page (D59);
   paging links keep every filter and sort parameter (D78).
3. Name 3 to 64 characters, pattern `^[A-Z][a-zA-Z0-9]*$`, unique case-insensitively (D37, D56).
4. Description and changelog 1 to 1000 characters after trimming; external description at most 1000 (D37, D56).
5. Unknown form fields and JSON properties, unknown or repeated query parameters, query parameters on POST, PATCH and
   DELETE, and multipart parts with an empty name return 400 (D21, D52, D70).
6. A `version` form field on POST `.../versions` returns 400 (D7).
7. Searches: 1 to 100 items; external search at most 100 URIs in total, each array with at least one entry; no paging
   (D38, D68).
8. At most 20 `aliasUris`, no duplicates, none equal to the namespace URI (D56).
9. `namespaceUri`, `aliasUris`, `sourceUri` must be absolute URIs (D73).
10. The file must be valid, non-empty Turtle (D9, D70); 0 bytes gives S6.
11. Invalid path values (name pattern, non-positive or non-numeric version) return 400 (D49).
12. 406 for an Accept header the operation cannot serve (D63).
13. 405 for an undeclared method (D67, D77).
14. The multipart request is at most 6 MB (D76).
15. POST `.../versions` returns 409 for an identical version (D46).
16. GET and DELETE `/external-vocabularies/{name}` return 404 with the ExternalVocabularyNotFoundResponse body (D41).

Wider than the published file:

1. The name filter of GET `/vocabularies` is a case-insensitive contains match, pattern `^[A-Za-z0-9]+$`, 1 to 64
   characters (D19).
2. `createdBy` on VocabularyMetadata and VocabularyVersionMetadataResponse (D27).
3. `validationWarnings` always present, codes `externalReferenceSkipped`, `deprecatedVocabularyReference`,
   `semanticReasoningSkipped` (reserved, never emitted) (D10).
4. `latestVersionHref` always filled, also on the 201 response (D5).

## Media types

- Uploads: `multipart/form-data` with parts `vocabularyFile`, `name`, `description`, `changelog` (internal) or
  `vocabularyFile`, `name`, `namespaceUri`, `aliasUris` (one part per URI), `description`, `sourceUri` (external).
- Downloads: `Content-Type: text/turtle` exactly, bytes as uploaded, `Content-Disposition: attachment;
  filename="<Name>.ttl"` (latest) or `"<Name>-v<n>.ttl"` (D29, D70).
- JSON operations accept `application/json`, `application/problem+json`, `*/*` or no Accept header (D63).
