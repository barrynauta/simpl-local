# API notes

Base path `/v1`. All operations of `vocabulary_openapi-tier1.yaml` are implemented with the same
paths, payloads and status codes. Errors are `application/problem+json` (RFC 9457, Belgif types).
This page lists behaviour that the OpenAPI does not pin down, and what this build does.

## Common behaviour

| Topic | Behaviour |
|---|---|
| Authentication | Every operation except `GET /v1/health` needs `Authorization: Bearer <JWT>`. Missing token: 401 `urn:problem-type:belgif:noAccessToken`. Invalid, expired, wrong issuer or unknown key: 401 `urn:problem-type:belgif:invalidAccessToken`. Both carry `WWW-Authenticate: Bearer`. |
| Authorisation | `GA_VOCABULARY_VIEWER`: all GETs, `POST /vocabularies/searches`, `POST /external-vocabulary/searches`. `GA_VOCABULARY_ADMIN`: everything. Otherwise 403 `urn:problem-type:belgif:missingPermission`. |
| Order of checks | 401, 403, 406 (`Accept`), 400 (path and query), 415 (request content type), 413 (size), 400 (fields), 415 (file type), 404/409 (state), 400 (content validation). |
| `Accept` | JSON operations need `application/json`, `application/*` or `*/*` (or no header). Download operations need `text/turtle`. Otherwise 406. |
| Unknown query parameters | 400 on the list operations (SIMPL-18164: unsupported filter is a validation error). |
| Paging | `page` >= 1, `pageSize` 1 to 100 (D29), default 20. Non-integer values give 400. A page past the end returns 200 with empty `items`. Links (`self`, `first`, `prev`, `next`, `last`) are relative, without `/v1`, like the OpenAPI examples. `prev`, `next` and `last` are left out when they do not apply. |
| Dates | ISO 8601 UTC with seconds, for example `2026-10-27T20:58:35Z`. |
| `href` and `Location` | Relative and without `/v1`, as in the OpenAPI examples. `contentHref` is absolute (`PUBLIC_BASE_URL`). |
| Names | `^[A-Z][a-zA-Z0-9]*$`, 3 to 64 characters, in path, query and body. A path name that breaks the pattern gives 400, not 404. |
| Unknown paths | 404 Problem. Wrong method: 405 Problem. |
| CORS | Preflight answered for `http://localhost:4333` and `http://localhost:4323`; `Location` and `Content-Disposition` are exposed. |

## Internal vocabularies

| Operation | Notes |
|---|---|
| `GET /vocabularies` | Filters: `name` (case-insensitive contains, the pattern is still enforced, D23), `status`, `creationDate`, `lastUpdated` (date or date-time; matches the UTC calendar day). Sort: the eight enum values; default `name` ascending; ties broken by name. Status sorts ACTIVE before DEPRECATED. `version` and `description` come from the latest version; `status` is the vocabulary status (ACTIVE if any version is ACTIVE). `lastUpdateDate` is the latest change of any version. |
| `POST /vocabularies` | Multipart: `name`, `description`, `changelog`, `vocabularyFile` required and not blank. A `version` field is rejected (400). Duplicate name: 409 (D13). Version 1 is ACTIVE. 201 with `Location: /vocabularies/{name}/versions/1`. The response also carries `createdBy`. |
| `POST /vocabularies/searches` | Body `{"items":[{"name":..,"version":..}]}`, at least one item, no extra properties. Without `version`: latest metadata, `latest: true`, `status` of the vocabulary. With `version`: that version, `latest: false`. Missing references are left out. Response `page` 1, `pageSize` max(20, number of results). |
| `GET /vocabularies/{name}` | `VocabularyCategory` of the latest version. |
| `PATCH /vocabularies/{name}` | `{"status":"ACTIVE"|"DEPRECATED"}`. DEPRECATED: all versions DEPRECATED. ACTIVE: the latest version becomes ACTIVE (D10). Same status as now: 409 ("Vocabulary is already in status ..."). Other values: 400. |
| `GET /vocabularies/{name}/versions` | Default order: newest version first (as in the OpenAPI example). Sort values `name`, `status`, `creationDate` (and `-` variants); ties by version number. `lastUpdated` is not in the enum here, so it gives 400. |
| `POST /vocabularies/{name}/versions` | As create, without `name`. 404 if the vocabulary does not exist, 409 if it is DEPRECATED (D11). New number = highest + 1. All older versions become DEPRECATED in the same store transaction (D9). |
| `GET /vocabularies/{name}/versions/{n}` | `VocabularyMetadata`. `latestVersionHref` points to the latest ACTIVE version (null when there is none), so the UI can link from a deprecated version (SIMPL-18559). Also returns `createdBy` (SIMPL-18176 AC3 asks for the creator). |
| `PATCH /vocabularies/{name}/versions/{n}` | Same body. A version cannot become ACTIVE while another version is ACTIVE (409). Same status: 409. |
| `GET /content/{name}` and `/content/{name}/versions/{n}` | The exact uploaded Turtle text, `text/turtle; charset=utf-8`, `Content-Disposition: attachment; filename="{name}.ttl"` or `"{name}-v{n}.ttl"`. |

## Validation report (SIMPL-18180/18181/18182/18183/18184/18194)

400 `VocabularyValidationProblem`; one issue per finding, all with `in: body`, `name: vocabularyFile`,
type `urn:problem-type:belgif:input-validation:invalidInput`, and a `location`:

| Stage | Finding | `location` |
|---|---|---|
| Syntax | Each Turtle parse error (the parser recovers and continues, max 100) | `line L, column C` (1-based) |
| Structure | Empty graph; no `owl:Ontology` or `skos:ConceptScheme` | `document` |
| Semantics | Reference to an internal vocabulary or version that does not exist | `prefix 'p' -> 'ns'` or `namespace 'ns'` |
| Semantics | Reference to a DEPRECATED version (detail names the latest ACTIVE version) | same |
| Semantics | Term not defined in the referenced version | `resource '<iri>'` |
| Semantics | IRI typed both as class and as property | `resource '<iri>'` |
| Semantics | Literal value for an `owl:ObjectProperty` (OpenAPI example) | `statement 's p literal'` |

A stage with errors stops the pipeline: syntax errors skip structure and semantics, structure
errors skip semantics. Within a stage all findings are reported.

Warnings (201 still returned, `validationWarnings` present only when not empty): code
`externalReferenceSkipped` for each namespace that is used or declared, is not defined by the
file itself, is not internal, not a registered external vocabulary (namespace or alias), and is
not one of RDF, RDFS, OWL, XSD, XML or SKOS.

Internal references (D18): `https://vocabulary.simpl.local/vocabularies/{Name}/{n}#Term` or
`.../{Name}#Term` (latest version). The vocabulary's own namespace (its own name, with or without a version number) is where it defines terms and is not checked as a reference (D18 clarification, 28 Oct 2026). A term exists when the referenced version has a subject IRI
with the same local name.

## Upload limits and file type

- Upload limit 10 MB (D30); larger files give 413 with the Problem shape of the OpenAPI example.
- Turtle only (D15): accepted when the part content type is `text/turtle` (or `application/x-turtle`,
  `application/turtle`), or when it is generic (`application/octet-stream`, `text/plain`, none)
  and the file name ends with `.ttl` or has no extension. Anything else: 415.

## External vocabularies (OpenAPI only)

| Operation | Notes |
|---|---|
| `POST /external-vocabularies` | Multipart `name`, `namespaceUri`, `vocabularyFile` required; `aliasUris` (repeat the field; comma separated or a JSON array also work), `description`, `sourceUri` optional. URIs must be absolute. Namespace and aliases are normalised (trailing `/` and `#` removed). 409 on a duplicate name or a namespace/alias already registered. Content must parse as Turtle and not be empty (no ontology rule). |
| `GET /external-vocabularies` | Paged, sorted by name. |
| `POST /external-vocabulary/searches` | Exactly one of `namespaceUri`, `aliasUri`, or a batch with `namespaceUris` and/or `aliasUris`; anything else 400. Namespace values match canonical namespaces, alias values match aliases. No match: 200 with empty `items`. |
| `POST /external-vocabularies/content` | Matches canonical namespace or alias after normalisation. 404 Problem if none. Needs the ADMIN role (D3 lists only the two searches as viewer POSTs). |
| `GET /external-vocabularies/{name}`, `/content`, `DELETE` | 404 Problem if unknown; DELETE removes all three graphs and returns 204. |

## Health

`GET /v1/health` (public): `UP` (200); `DEGRADED` (200) when the identity provider JWKS cannot be
fetched; `DOWN` (503) when Fuseki does not answer a SPARQL `ASK {}`.
