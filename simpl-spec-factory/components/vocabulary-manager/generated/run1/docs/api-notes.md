# API notes

The contract is the input OpenAPI (`input/specs/api/vocabulary_openapi-tier1.yaml`). `docs/openapi.yaml` is the
build copy: the same contract plus 401 and 403 on every operation except `/health` (D4), `pageSize` maximum 100
(D29), `createdBy` on `VocabularyMetadata`, and an `x-required-role` note per operation.

## Operations and roles (D3)

| Operation | Role |
|---|---|
| `GET /v1/health` | public |
| every other `GET` | VIEWER or ADMIN |
| `POST /v1/vocabularies/searches`, `POST /v1/external-vocabulary/searches` | VIEWER or ADMIN |
| everything else (`POST`, `PATCH`, `DELETE`, and `POST /v1/external-vocabularies/content`) | ADMIN |

`POST /external-vocabularies/content` is a read in practice, but D3 only lists the two search POSTs for viewers, so
it is admin only. See BUILD-LOG.

## Errors

All errors are `application/problem+json` with `type`, `title`, `status`, `detail`, `instance` (a `urn:uuid`) and,
where useful, `issues[]` with `in`, `name`, `value`, `detail`, `location`.

| Status | `type` | When |
|---|---|---|
| 400 | `urn:problem-type:belgif:badRequest` | invalid path, query, form or JSON input; invalid vocabulary content (issues with `name: vocabularyFile` and a `location`) |
| 401 | `urn:problem-type:belgif:noAccessToken` / `invalidAccessToken` | no token / invalid, expired or foreign token. `WWW-Authenticate: Bearer` is set |
| 403 | `urn:problem-type:belgif:missingPermission` | token without the required role |
| 404 | `urn:problem-type:belgif:resourceNotFound` | vocabulary, version or external vocabulary not found |
| 405 | `urn:problem-type:simpl:methodNotAllowed` | wrong HTTP method |
| 406 | `urn:problem-type:simpl:notAcceptable` | `Accept` excludes the only media type of the operation |
| 409 | `urn:problem-type:simpl:conflict` | duplicate name, status already set, second ACTIVE version, new version on a DEPRECATED vocabulary |
| 413 | `urn:problem-type:simpl:payloadTooLarge` | file above 10 MB |
| 415 | `urn:problem-type:simpl:unsupportedMediaType` | request is not multipart / JSON, or the file is not Turtle |
| 500 | `urn:problem-type:belgif:internalServerError` | unexpected error, Fuseki unreachable |

A 400 caused by vocabulary content can also carry `validationWarnings` (an extension member) so that warnings are
not lost when errors block the request.

## Behaviour details

- **List** (`GET /vocabularies`): filters `name` (case-insensitive contains, but the value must match the OpenAPI
  pattern, D23), `status`, `creationDate` and `lastUpdated` (a date or date-time; matches items created or updated
  on the same UTC day). Sort values as in the OpenAPI enum; default `name`; ties are broken by name. Unknown query
  parameters give 400. `page` from 1; `pageSize` 1 to 100 (D29). A page beyond the last returns an empty `items`.
- **Versions** (`GET /vocabularies/{name}/versions`): newest first by default. `sort=creationDate` and `status`
  sort by that field, ties by version number. `name` sorts by version number (all versions share the name).
- **Vocabulary fields**: `version` is the latest version number; `description` is the description of the latest
  version; `status` is the vocabulary status (D10).
- **href values** are relative (`/vocabularies/{name}`), as in the OpenAPI examples; `contentHref` is absolute,
  built from `APP_PUBLIC_BASE_URL`.
- **Create responses**: 201, `Location: /vocabularies/{name}/versions/{n}`, body `VocabularyVersionMetadataResponse`
  with `validationWarnings` always present (empty array when there are none).
- **Downloads**: `text/turtle;charset=UTF-8`, `Content-Disposition: attachment; filename="{Name}.ttl"` or
  `"{Name}-v{n}.ttl"`. The body is the file as uploaded.
- **Search** (`POST /vocabularies/searches`): items without `version` resolve to the latest metadata
  (`latest: true`, vocabulary status), items with `version` to that version (`latest: false`). Missing names or
  versions are left out. Duplicate request items are returned once. Unknown properties give 400
  (`additionalProperties: false`).
- **External vocabularies**: `aliasUris` may be sent as repeated form fields or comma separated. Namespace and
  alias comparison ignores a trailing `/` or `#`. A namespace or alias already registered by another external
  vocabulary gives 409. `namespaceUri`/`namespaceUris` search matches canonical namespaces only, `aliasUri`/`aliasUris`
  matches aliases only; `POST /external-vocabularies/content` matches either. The uploaded file gets syntax
  validation only.
- **Health**: `UP` when Fuseki answers `ASK {}`, otherwise 503 `DOWN`. `DEGRADED` is never returned.

## Validation (what produces a 400 on content)

| Kind | Rule | Location format |
|---|---|---|
| Syntax | file is UTF-8 and parses as Turtle (all errors reported, see BUILD-LOG for the recovery method) | `line L, column C` |
| Structure (D16) | at least one triple; at least one `owl:Ontology` or `skos:ConceptScheme` | `document` |
| Semantic (D17 c) | referenced internal vocabulary or version does not exist | `prefix 'p' -> 'ns'` or `namespace 'ns'` |
| Semantic (D17 b) | referenced internal version is DEPRECATED; message names the latest ACTIVE version if any | as above |
| Semantic (D17 a) | a term used from a referenced internal version does not exist in it | `resource '<iri>'` |
| Semantic (D17 d) | one IRI typed both as a class (`rdfs:Class`, `owl:Class`) and as a property (`rdf:Property`, `owl:*Property`) | `resource '<iri>'` |

Warning (201 still returned): `externalReferenceSkipped` for each namespace that is used, is not internal, is not a
registered external vocabulary (namespace or alias), is not a built-in language namespace (rdf, rdfs, owl, xsd,
skos, xml) and is not a namespace the file itself defines subjects in.

References follow D18: `{base}/{Name}/{n}#Term` (version n) and `{base}/{Name}#Term` (latest), with
`{base}` = `https://vocabulary.simpl.local/vocabularies`. Both prefix declarations and IRIs used in triples count as
references. IRIs under the vocabulary's own name are its own terms and are not checked.
