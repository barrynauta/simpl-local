# API notes

Base URL: `SM_BASE_URL` (`http://localhost:8098/v1` in compose). Absolute URIs in `Location`,
`Content-Location` and `graphName` use `SM_PUBLIC_BASE_URL` (defaults to `SM_BASE_URL`).

## Operations

| Method and path | Roles | Success | Notes |
|---|---|---|---|
| `GET /schemas` | viewer, admin | 200 page | Filters `status`, `resourceType`, `schemaType` (default RESOURCE); `page` from 1, `pageSize` 1 to 100 (default 20) |
| `POST /schemas` | admin | 201 | multipart: `schemaFile`, `name`, `title`, `description`, `resourceType` (RESOURCE only, required), `schemaType` |
| `GET /schemas/{name}` | viewer, admin | 200 `text/turtle` | Latest content, `Content-Location` = versioned URI. Query `schemaType`, `resourceType` |
| `PATCH /schemas/{name}` | admin | 200 `SchemaCategory` | Body `{"status":"REVOKED"}` or `{"status":"PUBLISHED"}`. Query `schemaType`, `resourceType` |
| `GET /schemas/{name}/versions` | viewer, admin | 200 page | Numeric version order; a page past the end is 404 |
| `POST /schemas/{name}/versions` | admin | 201 | multipart: `schemaFile`, `changelog`, `schemaType`, `resourceType` (must match). A `version` field is ignored |
| `GET /schemas/{name}/{version}` | viewer, admin | 200 | `text/turtle` by default; `Accept: application/json` gives the version metadata item |
| `GET /webhooks` | any valid token | 200 array | Not paged, not filtered by caller |
| `POST /webhooks` | any valid token | 201 `Webhook` | `{"targetUrl","events"}` |
| `DELETE /webhooks/{id}` | any valid token | 204 | 404 when unknown or not a UUID |

`/vocabularies...` and the quality profile paths are not implemented and answer 404 (D62).
`GET /actuator/health` (under `/v1`) is open for the healthcheck.

## Status codes

| Code | When |
|---|---|
| 400 | Bad name pattern (path or field), bad enum, bad page or pageSize, missing field, `resourceType` with SEARCH, invalid PATCH body, validation failure (detail prefixes `Syntax Error: `, `Default Properties Error: `, `Semantic Validation Error: `) |
| 401 | Missing, malformed, expired token, or a token not signed by the realm |
| 403 | Role missing (schema operations only) |
| 404 | No concept for (schemaType, name), resourceType filter mismatch on GET or PATCH, unknown version, version not a positive integer, empty versions page, unknown webhook |
| 406 | Accept does not admit the response type; webhook event name not in `SchemaPublished`, `SchemaRevoked` |
| 409 | Name exists in the schema type; published SEARCH exists (create); PATCH to the current state; PATCH to PUBLISHED with the slot taken; duplicate webhook |
| 415 | Create or new version not `multipart/form-data`; PATCH or POST webhook not `application/json` |
| 500 | Vocabulary Manager unreachable, 5xx or timeout (`Semantic validation unavailable: vocabulary manager unreachable`) |

Errors are `application/problem+json` with `type` `urn:problem-type:simpl:<name>`, `href`,
`title`, `status`, `detail`, `instance` and, for validation failures, `issues`
(`code`, `description`, `location`).

Fixed 409 details: `A schema with that name already exists.`,
`A published federated search schema already exists.`,
`Another schema is in PUBLISHED state for the resource type.`

## Payloads

Schema list item (`GET /schemas`):
`name, description, title, resourceType (null for SEARCH), schemaType, version (string, highest),
changelog (of the highest version), status, created, modified, createdBy`.

Version item (`GET .../versions` and `GET .../{version}` with JSON):
`name, version, changelog, created, createdBy, schemaType, resourceType, status (derived),
graphName (absolute versioned URI), title, description`.

Create response: `schemaName, schemaVersion, validationWarnings, status, schemaType, resourceType`.

New version response: `name, description, title, resourceType, schemaType, version, changelog,
status, validationWarnings, created, createdBy, graphName`.

PATCH response (`SchemaCategory`): `name, title, description, resourceType, schemaType, status,
latestVersion, created, modified, createdBy`.

Timestamps are ISO 8601 UTC with milliseconds, for example `2026-10-28T07:54:47.165Z`.

Paging envelope: `page, totalPages, total, pageSize, next, metadata`. `next` is relative, starts
with `/schemas`, keeps the filters and always carries the effective `schemaType`; it is `null` on
the last page. `totalPages` is 0 when there are no items.

## Validation rules (D30 to D36)

1. Syntax: the bytes must parse as Turtle (UTF-8) and contain at least one `sh:NodeShape`.
2. Default properties (RESOURCE only): one `sh:NodeShape` must have a `sh:property` with
   `sh:path` equal to each IRI in `SM_DEFAULT_PROPERTIES` (default `simpl:uuid`, `simpl:version`
   with `simpl: <https://simpl-programme.ec.europa.eu/ns/default#>`). Issue code
   `missingDefaultProperty`, `location` = the IRI.
3. Semantic:
   - Local IRIs (starting with `VOCABULARY_NAMESPACE_BASE`) used in `sh:path`, `sh:class`,
     `sh:targetClass`, `sh:datatype` or `sh:in`: the vocabulary must exist (`localReferenceUnavailable`)
     and the term must be a subject in it (`unknownVocabularyTerm`).
   - `sh:node` and `sh:qualifiedValueShape` must point to a node that is the subject of at least
     one triple in the same document (`unresolvedShapeReference`).
   - All errors are collected. `location` reads `property '<path IRI>' sh:<predicate> '<IRI>'`, or
     `shape '<shape>' sh:<predicate> '<IRI>'` when the node has no `sh:path`.
   - Each prefix bound to an external namespace gives the warning `externalReferenceSkipped`,
     `location` `prefix '<p>' -> '<namespace>'`, returned in `validationWarnings`.

## Events

E-mail subjects (RESOURCE only):
`Schema published: {name} (version {version})`,
`Schema new version published: {name} (version {version})`,
`Schema revoked: {name}`.
Body lines: `Name`, `Title`, `Description`, `Resource type`, `Version`, `Change description`,
`Timestamp`, `Instructions`.

Webhook body (both schema types), header `X-Simpl-Event`:

```json
{"eventId":"...","eventType":"SchemaPublished","occurredAt":"2026-10-28T07:54:47Z",
 "schemaName":"TSem27600","schemaType":"RESOURCE","resourceType":"RtSem27600",
 "version":"1","status":"PUBLISHED","schemaUri":"http://localhost:8098/v1/schemas/TSem27600/1"}
```

## Configuration (D60 names)

| Name | Compose value | Used for |
|---|---|---|
| `SM_BASE_URL`, `SM_PUBLIC_BASE_URL` | `http://localhost:8098/v1` | Absolute URIs |
| `SM_UI_URL` | `http://localhost:4334` | Default for `CORS_ALLOWED_ORIGINS` |
| `KEYCLOAK_URL` | `http://localhost:8185` | Accepted issuer `{KEYCLOAK_URL}/realms/simpl` |
| `KEYCLOAK_JWKS_URL` (extra, not a D60 name) | `http://keycloak:8080/realms/simpl/protocol/openid-connect/certs` | Signing keys; defaults to the issuer's JWKS URL |
| `VOCABULARY_MANAGER_URL` | `http://vocabulary-stub:8099` | Vocabulary client |
| `VOCABULARY_NAMESPACE_BASE` | `https://vocabulary.simpl.test/content/` | Local namespace test |
| `SM_DEFAULT_PROPERTIES` | the two default IRIs | Default property check |
| `SMTP_HOST`, `SMTP_PORT` | `mailpit`, `1025` | E-mail |
| `NOTIFICATION_FROM` | `schema-manager@simpl.test` | E-mail sender |
| `PROVIDER_NOTIFICATION_EMAILS` | `providers@simpl.test` | Recipients, comma-separated |
| `PROVIDER_INSTRUCTIONS_URL` | `http://localhost:4334/schemas` | `Instructions` line |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4334` | CORS |
| `MAIL_API_URL`, `WEBHOOK_RECEIVER_URL` | (tests only) | Not read by the service |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` (extra) | postgres | Database |
