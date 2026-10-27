# How decisions D1 to D30 are implemented

Paths are relative to `build/backend/src/main/java/eu/simpl/vocabulary/` (backend) or `build/ui/src/` (UI).

| # | Decision | Implementation |
|---|---|---|
| D1 | Follow the OpenAPI | All 13 paths and 16 operations under `/v1` with the OpenAPI payloads: `web/VocabularyController`, `web/ContentController`, `web/ExternalVocabularyController`, `web/HealthController`. The R07.02 draft API is not implemented |
| D2 | Token everywhere except `/health` | `config/SecurityConfig.filterChain`: only `GET /v1/health` is `permitAll` |
| D3 | VIEWER: every GET and the two search POSTs; ADMIN: everything | `config/SecurityConfig.filterChain` request matchers; roles from the `roles` claim (`authenticationConverter`) |
| D4 | 401 and 403 as Problem Details | `SecurityConfig.problemEntryPoint` (401 `noAccessToken` / `invalidAccessToken`, `WWW-Authenticate`), `problemDeniedHandler` (403 `missingPermission`). Added to the build copy `docs/openapi.yaml` |
| D5 | OpenAPI Problem schema | `problem/Problem`, `problem/ProblemIssue`, `problem/ProblemWriter`, `web/ApiExceptionHandler` |
| D6 | Local OIDC issuer, test users, `roles` claim | `keycloak/simpl-realm.json` (realm `simpl`, clients `vocabulary-tests` with password grant and `vocabulary-ui` with code + PKCE, users `ga-admin`, `ga-viewer`, `ga-norole`, realm-role mapper to claim `roles`); issuer handling in `docker-compose.yml` and `SecurityConfig.jwtDecoder` |
| D7 | Integer versions from 1, plus 1 | `domain/VocabularyLifecycle.create` and `addVersion` |
| D8 | Manual `version` field: 400 | `web/VocabularyController.rejectVersionField` (on new version and also on create) |
| D9 | New version ACTIVE, all previous DEPRECATED, same transaction | `VocabularyLifecycle.addVersion`; `store/JenaStore.save` writes metadata and content in one SPARQL Update request |
| D10 | Deprecate vocabulary: all versions; re-activate: latest only; version PATCH cannot create a second ACTIVE (409) | `VocabularyLifecycle.changeStatus`, `changeVersionStatus` |
| D11 | New version on a DEPRECATED vocabulary: 409 | `service/VocabularyService.createVersion` (checked before the file) and `VocabularyLifecycle.addVersion` |
| D12 | UI sends `changelog = "Initial version"` | `api.ts` `api.create`; `views/CreateView.vue` has no version or change log field |
| D13 | Duplicate name: 409, shown as field error | `VocabularyService.create`; `CreateView.vue` maps 409 to the name field |
| D14 | One term: Active | No publish step; stored valid versions are ACTIVE. UI labels "Active" / "Deprecated" (`format.ts`) |
| D15 | Turtle only, else 415 | `service/Upload.looksLikeTurtle` (content type or `.ttl`), `VocabularyService.checkUpload` |
| D16 | Syntax = parses as Turtle (line, column); structure = not empty and declares `owl:Ontology` or `skos:ConceptScheme` | `validation/TurtleSyntaxValidator`, `validation/StructureValidator` |
| D17 | Semantic errors (a) to (d), warnings for unknown namespaces | `validation/SemanticValidator` |
| D18 | Reference by namespace `{base}/{Name}/{n}#`, `{base}/{Name}#` = latest, base configurable | `validation/InternalReference`; `app.reference-base` (`APP_REFERENCE_BASE`); UI `turtle.ts internalReferences` and the reference picker in `NewVersionView.vue` |
| D19 | Missing reference: 400 with a validation issue | `VocabularyService.checkUpload` turns all validation errors into a 400 `VocabularyValidationProblem` |
| D20 | 201 with `validationWarnings`; UI shows them on the confirmation | `web/ResourceMapper.created`; `notify.ts` shows warnings with the success notice |
| D21 | No Figma; plain component library | Pico CSS (classless CSS library) plus own small components |
| D22 | Story columns, no file format column | `views/ListView.vue` |
| D23 | API: case-insensitive contains; UI capitalises the first letter | `VocabularyController.list` (pattern check, then lower-case contains); `ListView.vue nameParam` |
| D24 | "Full details" download = Turtle content | `ContentController`; UI download actions in list, details and version views |
| D25 | Deprecation warning everywhere | `components/DeprecationWarning.vue` used in details and version views and per deprecated history row; list banner and row text in `ListView.vue`; picker and content-reference warnings in `NewVersionView.vue` |
| D26 | Actions hidden for VIEWER | `session.isAdmin` checks in `ListView.vue`, `DetailsView.vue`, `CreateView.vue`, `NewVersionView.vue`; router meta `admin` |
| D27 | Fuseki | `docker-compose.yml` service `fuseki`; `store/JenaStore` via `RDFConnection` |
| D28 | The synchronous response is the report | No notification channel; the 400 body is the report, rendered by `components/ProblemReport.vue` |
| D29 | `pageSize` max 100, above: 400 | `web/RequestChecks.paging`, `app.max-page-size` |
| D30 | 10 MB upload limit, configurable | `app.max-upload-bytes` (`APP_MAX_UPLOAD_BYTES`), `VocabularyService.checkUpload`; container ceiling 100 MB and nginx `client_max_body_size 50m` so the app can answer 413 itself |
