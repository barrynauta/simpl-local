# API notes

The wire format follows `input/specs/api/vocabulary_openapi-tier1.yaml`. Where the decisions change or add to it
(D39), the build follows the decisions. The OpenAPI file itself was not edited.

## Added or changed against the OpenAPI file

| Point | Behaviour in this build | Decision |
|---|---|---|
| 401 and 403 | every secured operation; problem types `urn:problem-type:simpl:unauthorized` and `...:forbidden`; 401 also sends `WWW-Authenticate: Bearer` | D17 |
| 503 | every operation that reads or writes the store; type `urn:problem-type:belgif:serviceUnavailable`, `Retry-After: 5` | D44 |
| `name` filter | case-insensitive contains, `^[A-Za-z0-9]+$`, 1 to 64 characters | D19 |
| `pageSize` | 1 to 100, default 20; `page` from 1 | D22 |
| Unknown query parameters | 400 on every operation, one issue per parameter (`in: query`); a parameter given twice is 400 too | D21 |
| `createdBy` | on `VocabularyMetadata` and `VocabularyVersionMetadataResponse` | D27 |
| `latestVersionHref` | always filled; also returned on the 201 response of version creation | D5 |
| `validationWarnings` | always present (empty array when none); codes `externalReferenceSkipped`, `deprecatedVocabularyReference` | D10 |
| `description`, `changelog` | 1 to 1000 characters after trimming; stored trimmed | D37 |
| `version` form field on POST .../versions | 400, `in: body`, `name: version` | D7 |
| Unknown form fields or JSON properties | 400, one issue per field (no mass assignment) | OWA-API3, see BUILD-LOG |
| Identical version | 409 on POST .../versions | D46 |
| Searches | not paged: `page` 1, `pageSize` 100, `total`, `totalPages`; no links; at most 100 references or URIs | D38 |
| External 404 | `ExternalVocabularyNotFoundResponse` body, detail "External vocabulary with name '<Name>' was not found.", issue `in: path`, `name: name` | D41 |
| Version not found | 404 with detail "Version <n> of vocabulary '<Name>' was not found.", issue `in: path`, `name: version` | D24 |
| Unknown path | 404 `resourceNotFound`; wrong method 405 with `Allow` | none (BUILD-LOG) |

## Content types

- POST /vocabularies, POST .../versions, POST /external-vocabularies: `multipart/form-data`, else 415.
- PATCH and the three JSON POST operations: `application/json`, else 415.
- Downloads: `text/turtle`, `Content-Disposition: attachment; filename="<Name>.ttl"` or `"<Name>-v<n>.ttl"`.
- `aliasUris` on POST /external-vocabularies may be sent as repeated form fields or as one JSON array text.
- Errors are always `application/problem+json`, whatever the Accept header.

## Error bodies

Belgif problem fields: `type`, `href` (Belgif types only), `title`, `status`, `detail`, `instance` (`urn:uuid:...`),
`issues`. Field issues use `urn:problem-type:belgif:input-validation:schemaViolation`; vocabulary content errors use
`urn:problem-type:belgif:input-validation:invalidInput` with `in: body`, `name: vocabularyFile` and `location`.

Location formats: `line N, column M` (syntax), `statement '<s> <p> <o>'` (S1, S2), `resource '<iri>'` (S3 to S5),
`document` (S6), `prefix '<p>' -> '<namespace>'` (S7 and warnings).
