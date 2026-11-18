# API notes

Base URL: `http://localhost:8196/v1`. The wire format is the one of `input/specs/api/vocabulary_openapi-tier1.yaml`,
with the decided behaviour of D39 on top. The OpenAPI file is not edited (D39); the points below are where the build
differs from the file.

## Operations

| Method and path | Role (D16) | Notes |
|---|---|---|
| GET `/health` | none | `{"status":"UP"\|"DEGRADED"\|"DOWN"}` (D44) |
| GET `/vocabularies` | viewer or admin | Filters `name` (contains, case-insensitive, `^[A-Za-z0-9]+$`, 1 to 64), `status`, `creationDate`, `lastUpdated` (date or date-time, D20); `sort` (D23); `page`, `pageSize` 1 to 100 (D22). |
| POST `/vocabularies` | admin | multipart: `vocabularyFile`, `name`, `description`, `changelog`. 201 with Location and `validationWarnings`. |
| POST `/vocabularies/searches` | viewer or admin | `{"items":[{"name":…,"version":…}]}`, 1 to 100 items, not paged (D38). |
| GET `/vocabularies/{vocabularyName}` | viewer or admin | The vocabulary (D25). |
| PATCH `/vocabularies/{vocabularyName}` | admin | `{"status":"ACTIVE"\|"DEPRECATED"}`; same status 409 (D4). |
| GET `/vocabularies/{vocabularyName}/versions` | viewer or admin | Newest first by default; `sort` `name`, `status`, `creationDate` (and `-`); paged. |
| POST `/vocabularies/{vocabularyName}/versions` | admin | multipart: `vocabularyFile`, `description`, `changelog`; a `version` field is 400 (D7); identical content and description 409 (D46). |
| GET `/vocabularies/{vocabularyName}/versions/{version}` | viewer or admin | The version (D26) with `createdBy` and `latestVersionHref`. |
| PATCH `/vocabularies/{vocabularyName}/versions/{version}` | admin | Version status (D4). |
| GET `/content/{vocabularyName}` | viewer or admin | Latest content, `text/turtle`, `attachment; filename="<Name>.ttl"`. |
| GET `/content/{vocabularyName}/versions/{version}` | viewer or admin | `filename="<Name>-v<n>.ttl"`. |
| GET `/external-vocabularies` | viewer or admin | Paged, name ascending, only `page` and `pageSize`. |
| POST `/external-vocabularies` | admin | multipart: `vocabularyFile`, `name`, `namespaceUri`, `aliasUris` (one part per URI, at most 20), `description`, `sourceUri`. Syntax check only (D30, D56). |
| POST `/external-vocabulary/searches` | viewer or admin | Exactly one of `namespaceUri`, `aliasUri`, or `namespaceUris` and/or `aliasUris` (at most 100 URIs). |
| POST `/external-vocabularies/content` | viewer or admin | `{"namespaceUri":…}`, matched against namespace and aliases. |
| GET `/external-vocabularies/{name}` | viewer or admin | |
| GET `/external-vocabularies/{name}/content` | viewer or admin | `filename="<Name>.ttl"`. |
| DELETE `/external-vocabularies/{name}` | admin | 204, no reference check (D42). |

## Additions to the OpenAPI file (D39)

- 401 and 403 on every secured operation: `application/problem+json`, types `urn:problem-type:simpl:unauthorized` and
  `urn:problem-type:simpl:forbidden` (D17). 401 also sends `WWW-Authenticate: Bearer`.
- 503 on every operation when the store or (for an unknown key) Keycloak is not available, type
  `urn:problem-type:belgif:serviceUnavailable`, header `Retry-After: 5` (D44).
- `createdBy` on `VocabularyMetadata` and `VocabularyVersionMetadataResponse` (D27); `latestVersionHref` always filled,
  also on the 201 response (D5).
- `validationWarnings` always present on 201, codes `externalReferenceSkipped` and `deprecatedVocabularyReference`
  (D10).
- Name filter relaxed to "contains" (D19); `pageSize` maximum 100 (D22); description and changelog 1 to 1000
  characters after trimming (D37).
- Unknown query parameters, repeated query parameters, query parameters on writes, unknown form fields and unknown JSON
  properties give 400 (D21, D52).
- Invalid path values give 400 with an issue `in: path` (D49); names in paths match exactly (D54).
- 409 on `POST …/versions` for an identical version (D46).
- 405 and unknown paths give `application/problem+json` (D55).
- 406 when the `Accept` header excludes the response type.

## Problem types (D48)

| Status | type |
|---|---|
| 400 | `urn:problem-type:belgif:badRequest`, issues `…:input-validation:schemaViolation` (request shape) or `…:invalidInput` (content validation) |
| 401, 403 | `urn:problem-type:simpl:unauthorized`, `urn:problem-type:simpl:forbidden` |
| 404 | `urn:problem-type:belgif:resourceNotFound`, one issue `in: path` |
| 405, 406 | `urn:problem-type:simpl:methodNotAllowed`, `urn:problem-type:simpl:notAcceptable` |
| 409, 413, 415 | `urn:problem-type:simpl:conflict`, `…:payloadTooLarge`, `…:unsupportedMediaType` |
| 500, 503 | `urn:problem-type:belgif:internalServerError`, `urn:problem-type:belgif:serviceUnavailable` |

## Validation report

A 400 from content validation has one issue per error, `in: body`, `name: vocabularyFile`, with `detail` and
`location`:

| Rule | detail | location |
|---|---|---|
| Syntax | `Encountered invalid Turtle syntax: <parser message>` | `line N, column M` (only the first error) |
| S1 | `Literal value for object property` | `statement '<s> <p> <o>'` |
| S2 | `Resource value for datatype property` | `statement '<s> <p> <o>'` |
| S3 | `Property declared as both object and datatype property` | `resource '<p>'` |
| S4 | `Duplicate preferred label for the same language` | `resource '<s>'` |
| S5 | `Resource is an instance of disjoint classes` | `resource '<r>'` |
| S6 | `Vocabulary contains no statements` | `document` |
| S7 | `Referenced internal vocabulary '<Name>' [version <n>] does not exist.` | `prefix '<p>' -> '<namespace>'` |

Errors come in rule order (S1 to S7); within a rule, ordered by location. Relative IRIs resolve against
`<contentBase>/content/<Name>/versions/<n>` (D57).
