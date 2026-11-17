# How the digest decisions D1 to D30 are implemented

| # | Decision | Implementation |
|---|---|---|
| D1 | Follow the OpenAPI API shape | All 13 paths of the OpenAPI under `/v1` (`api/mod.rs::router`). The R07.02 draft paths (`/status`, `/vocabulary/{name}`, `versionLabel`, `/api`) are not offered. |
| D2 | Everything needs a token except `/health` | `/health` is routed outside the `require_auth` route layer; `/content/...` is protected. |
| D3 | VIEWER: every GET and the two search POSTs; ADMIN: everything | `auth.rs::required_access` and `require_auth`. Note: `POST /external-vocabularies/content` is ADMIN only, as D3 is written. |
| D4 | 401 and 403 as Problem Details | `problem.rs` (`Unauthorized`, `Forbidden`), Belgif types `noAccessToken`, `invalidAccessToken`, `missingPermission`, plus `WWW-Authenticate`. |
| D5 | OpenAPI Problem schema | `problem.rs::Problem`, `Issue` with `type`, `href`, `title`, `detail`, `in`, `name`, `value`, `location`; `instance` is a fresh `urn:uuid:`. Framework errors (405, 413) are rewritten to the same shape (`api/mod.rs::problem_for_bare_errors`). |
| D6 | Local OIDC issuer, test users and clients | Keycloak 26 in compose with `deploy/keycloak/simpl-realm.json`: realm roles mapped to a top-level `roles` claim on both clients, `vocabulary-tests` (public, password grant), `vocabulary-ui` (public, code + PKCE S256), users `ga-admin`, `ga-viewer`, `ga-norole`, password `password`. Fixed hostname for a stable issuer. |
| D7 | Integer versions from 1, +1 | `model.rs::Vocabulary::next_version_number`, `service.rs::create_vocabulary` (version 1). |
| D8 | Manual `version` field: 400 on `version` | `api/vocabularies.rs::version_input` adds a `schemaViolation` issue named `version`. Also applied to `POST /vocabularies`. |
| D9 | New version ACTIVE, all previous DEPRECATED, same transaction | `service.rs::create_version`; one SPARQL Update request in `store/fuseki.rs::save_vocabulary`. |
| D10 | Vocabulary status vs version status | `model.rs::Vocabulary::status` (ACTIVE if any version is ACTIVE); `service.rs::update_vocabulary_status` (DEPRECATED: all; ACTIVE: latest only); `service.rs::update_version_status` (409 if another version is ACTIVE). |
| D11 | New version on a deprecated vocabulary: 409 | `service.rs::create_version`. |
| D12 | UI sends "Initial version" | UI concern. The API requires `changelog` on create (OpenAPI), blank is 400. |
| D13 | Duplicate name: 409 | `service.rs::create_vocabulary`. |
| D14 | One term: Active, no publish step | A stored, valid version is ACTIVE at once; no publish endpoint. |
| D15 | Turtle only, else 415 | `api/support.rs::check_turtle_file` (content type and extension). RDFS and OWL content both accepted. |
| D16 | Syntax and structure rules | `validation.rs::parse_turtle` (oxttl, all errors with line and column), `validation.rs::check_structure` (non-empty, `owl:Ontology` or `skos:ConceptScheme`). |
| D17 | Semantic errors (a) to (d), warnings for unknown namespaces | `validation.rs::check_semantics`; context loaded by `service.rs::semantic_context`. Extra error from the OpenAPI example: literal value for an object property. |
| D18 | Reference convention `{base}/{Name}/{n}#`, `{base}/{Name}#`; own namespace is not a reference (clarified 28 Oct 2026) | `validation.rs::parse_internal_iri`, `collect_internal_refs`; base from `REFERENCE_BASE`; own name skipped via `SemanticContext::self_name` in `check_semantics` and `service.rs::semantic_context`. |
| D19 | Missing reference: 400 | Semantic errors are always 400 `VocabularyValidationProblem`. |
| D20 | 201 with `validationWarnings` | `api/vocabularies.rs::created_json` (field present only when there are warnings). |
| D21 | Visual design | Out of scope (UI). |
| D22 | List columns | API returns all the story's columns: name, description, version, status, creationDate, lastUpdateDate, createdBy. |
| D23 | Name filter: case-insensitive contains | `api/vocabularies.rs::list`; the name pattern is still validated (400 for `shacl`). |
| D24 | "Full details" download = Turtle content | `/content/...` returns the exact uploaded file. |
| D25 | Deprecation warnings in the UI | API support: status on every item, `latestVersionHref` to the latest ACTIVE version, deprecated-reference errors in validation. Display is a UI concern. |
| D26 | Hidden actions for VIEWER | UI concern; the API returns 403 on direct calls. |
| D27 | Fuseki persistence | `store/fuseki.rs`, Fuseki 5.3 TDB2 in compose (`deploy/fuseki/assembler.ttl`). |
| D28 | The synchronous response is the report | 400 issues list or 201 with warnings; no notification. |
| D29 | `pageSize` max 100 | `api/support.rs::parse_paging`. |
| D30 | 10 MB upload limit, configurable | `MAX_UPLOAD_BYTES`, checked while streaming the file part (`api/support.rs::read_form`). |
