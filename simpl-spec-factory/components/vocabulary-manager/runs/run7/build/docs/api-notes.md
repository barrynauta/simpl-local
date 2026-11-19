# API notes

Base URL `http://localhost:8196/v1`. The contract is `input/specs/api/vocabulary_openapi-tier1.yaml`; where the decisions differ (D39, D79) the service follows the decisions. `href` values and `Location` headers are relative to the API base.

## Access (D16, D17, D40)

- Bearer token from Keycloak realm `simpl`, audience `vocabulary-manager-be`, issuer `http://localhost:8193/realms/simpl`. Scopes are not checked.
- `GA_VOCABULARY_VIEWER`: every GET, `POST /vocabularies/searches`, `POST /external-vocabulary/searches`, `POST /external-vocabularies/content`.
- `GA_VOCABULARY_ADMIN`: the above plus `POST /vocabularies`, `POST /vocabularies/{name}/versions`, both PATCH operations, `POST /external-vocabularies`, `DELETE /external-vocabularies/{name}`.
- No or bad token: 401 `urn:problem-type:simpl:unauthorized`. Missing role: 403 `urn:problem-type:simpl:forbidden`. `GET /health` is public.

## Problem types

| Status | Type |
|---|---|
| 400 | `urn:problem-type:belgif:badRequest`, issues with `in`, `name`, `detail` (schemaViolation) or, for content, `invalidInput` with `location` |
| 401, 403 | `urn:problem-type:simpl:unauthorized`, `...:forbidden` |
| 404 | `urn:problem-type:belgif:resourceNotFound` with one issue |
| 405 | `urn:problem-type:simpl:methodNotAllowed`, `Allow` header |
| 406, 409, 413, 415 | `urn:problem-type:simpl:notAcceptable`, `conflict`, `payloadTooLarge`, `unsupportedMediaType` |
| 500 | `urn:problem-type:belgif:internalServerError` |
| 503 | `urn:problem-type:belgif:serviceUnavailable`, `Retry-After: 5`, detail names the store or the identity provider |

Every response carries `X-Correlation-ID` (sent value when it is safe, else a new UUID).

## Behaviour beyond the contract

- `POST /vocabularies` and `POST .../versions` (multipart/form-data): fields `name` (create only), `description`, `changelog`, `vocabularyFile`. A `version` field, any other field, a repeated field, a part without a name or any query parameter gives 400. File part above 5 MB or request above 6 MB gives 413.
- 201 body: the new version with `createdBy`, `latestVersionHref` and `validationWarnings` (always an array; codes `externalReferenceSkipped`, `deprecatedVocabularyReference`).
- 409 cases: duplicate name (case-insensitive), PATCH to the current status, a new version with the same bytes and trimmed description as the latest version.
- `GET /vocabularies`: `name` (contains, case-insensitive, letters and digits), `status`, `creationDate` and `lastUpdated` (date: whole UTC day; date-time: to the second), `sort` (`name`, `status`, `creationDate`, `lastUpdated`, `-` for descending; ties by name), `page` (from 1), `pageSize` (1 to 100, default 20). Links keep every query parameter; `prev` beyond the last page points to the last page.
- `GET /vocabularies/{name}/versions`: default newest first.
- Downloads: `Content-Type: text/turtle` exactly, `Content-Disposition: attachment; filename="<Name>.ttl"` or `"<Name>-v<n>.ttl"`; bytes as uploaded. Accept must allow `text/turtle` or `*/*`.
- Searches: not paged; `page` 1, `pageSize` 100, `total`, `totalPages`, no links. Internal: 1 to 100 items, results in request order, duplicates once, missing left out. External: one of `namespaceUri`, `aliasUri`, or `namespaceUris` and/or `aliasUris` (at most 100 URIs), matched after removing a trailing `#` or `/`.
- External registration: `name`, `namespaceUri`, optional `aliasUris` (one part per URI, at most 20), `description`, `sourceUri`; syntax check only, empty file rejected.
- Names in paths are exact (`/vocabularies/HEALTH` is not `Health`). Invalid path values give 400.
- `GET /vocabularies/searches` or `GET /external-vocabularies/content`: 405 (fixed segments win, D77).
- Health: `GET /health` gives `UP`, `DEGRADED` (no realm keys and Keycloak unreachable) or 503 `DOWN` (store does not answer in 2 s).
