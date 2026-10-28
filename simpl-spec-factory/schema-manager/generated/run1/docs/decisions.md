# How each decision is implemented

Code paths are relative to `backend/src/main/java/eu/simpl/schemamanager/` unless they start
with `ui/`, `keycloak/` or another top-level folder of `build/`.

| D | Topic | Implementation |
|---|---|---|
| D1 | Base path | `server.servlet.context-path: /v1` (`application.yml`); compose publishes it as `http://localhost:8098/v1` |
| D2 | Absolute URIs | `SchemaService.conceptUri` and `versionUri` build `{SM_PUBLIC_BASE_URL}/schemas/...`, with `?schemaType=SEARCH` for SEARCH. Create sets `Location` to the concept URI, new version to the versioned URI |
| D3 | 401 | `SecurityConfig` entry points write problem JSON with type `unauthorized` for missing, malformed, expired or foreign tokens |
| D4 | Role names | `SecurityConfig.roles` maps the top-level `roles` claim to `ROLE_*`; only `GA_SCHEMA_ADMIN` and `GA_SCHEMA_VIEWER` are used |
| D5 | Access matrix | URL rules in `SecurityConfig.filterChain`; they run in the filter chain, before any controller validation |
| D6 | "Public" URI | `GET /schemas/{name}` is under the viewer rule, no anonymous access |
| D7 | Webhook access | `/webhooks/**` requires `authenticated()` only; 403 is never produced there |
| D8 | Media types | `web/Negotiation`: `requireJson`, `content`, `requireMultipart`, `requireJsonBody`. All bodies are written as bytes with an explicit content type, so errors never fall back to Spring negotiation |
| D9 | Latest content | `SchemaController.latest`: `text/turtle`, raw bytes of the highest version, `Content-Location` = versioned URI; works for REVOKED concepts; `Accept: application/json` gives 406 |
| D10 | Version endpoint | `SchemaController.version`: content by default, version item on `Accept: application/json`; sets `Content-Location` and `Location`; `Params.version` turns anything but a positive integer into 404 |
| D11 | Identity | Unique `(schema_type, name)` in `V1__init.sql`; every lookup goes through `SchemaService.find(type, name, resourceType)`; resourceType mismatch on GET and PATCH gives 404 |
| D12 | Name rules | `Params.isPascal` (3 to 64, `^[A-Z][a-zA-Z0-9]*$`) for the `name` field and every `{schemaName}`; names are never reused within a type (409 `A schema with that name already exists.`) |
| D13 | resourceType | Free PascalCase string (`Params.resourceTypeFilter`); required for RESOURCE create; rejected with 400 for SEARCH on create and on every query; UI offers `Data`, `Application`, `Infrastructure` |
| D14 | Version numbers | `SchemaService.create` inserts version 1; `newVersion` uses `max + 1` under the slot lock; a `version` form field is ignored |
| D15 | No 409 on version | `newVersion` has no 409 path |
| D16 | Slot rule | `Concept.slotKey`; `SchemaRepository.lockSlot` (row lock) plus partial unique index; status rules in `create` and `newVersion`; `SchemaService.versionStatus` derives version status |
| D17 | Second SEARCH | `create`: name clash checked first, then the SEARCH slot; both re-checked under the lock |
| D18 | PATCH | `SchemaController.patch` and `SchemaService.changeStatus`: REVOKED and PUBLISHED, 200 with `SchemaCategory`, 400 for anything else, 409 for same state or a taken slot with the fixed texts |
| D19 | Initial changelog | `SchemaService.INITIAL_CHANGELOG = "Initial version."` |
| D20 | Required fields | `SchemaController.create` and `newVersion` (`requireField`); new version `resourceType` mismatch gives 400 in `SchemaService.newVersion` |
| D21 | Retention | No delete operation exists; all GETs work for REVOKED concepts and both types |
| D22 | Federated search disabled | Observable only: revoke sets REVOKED and fires `SchemaRevoked` |
| D23 | Processing order | Security (401, 403), then `Negotiation` (406, 415), `Params` and field checks (400), `SchemaService.find` (404), conflict checks (409), `SchemaValidator` (400, 500) |
| D24 | List item | `SchemaController.listItem` plus `created`, `modified`, `createdBy` (`preferred_username`, else `sub`) |
| D25 | Version item | `SchemaController.versionItem`; `graphName` is the absolute versioned URI |
| D26 | Paging and order | `Params.page` and `pageSize` (1 to 100); `SchemaRepository.listConcepts` orders by `name collate "C"`, then type; versions numeric ascending; list page past the end is 200 empty, versions page past the end 404 (`SchemaService.versions`) |
| D27 | Filters | `GET /schemas` combines `status`, `resourceType`, `schemaType` with AND; unknown parameters are ignored because handlers bind only known names |
| D28 | Create responses | OpenAPI shapes plus `status`, `schemaType`, `resourceType`; `validationWarnings` always present |
| D29 | Multipart | Flat fields read from `MultipartHttpServletRequest`; `schemaFile` bytes are not checked for filename or part type |
| D30 | Syntax | `SchemaValidator.syntax` with Apache Jena (`Rdf.parseTurtle`, strict error handler) and a `sh:NodeShape` presence check |
| D31 | Default properties | `SM_DEFAULT_PROPERTIES` (default `simpl:uuid`, `simpl:version`); RESOURCE only |
| D32 | Presence check | `SchemaValidator.defaultProperties`; no enrichment; stored bytes are the upload |
| D33 | Prefixes | `SYNTAX_PREFIX`, `DEFAULT_PREFIX`, `SEMANTIC_PREFIX` constants; stages stop at the first failure |
| D34 | Local and external | `SchemaValidator.isLocal`, `isWellKnown` (RDF, RDFS, XSD, OWL, SHACL, DC terms, SKOS, default property namespace), `vocabularyName` |
| D35 | Semantic checks | `SchemaValidator.semantic`: checks 1 to 4 exactly; all errors collected; warnings returned |
| D36 | VM outage | `VocabularyClient.fetch` (5 s timeout) throws on connection error, timeout or 5xx; `SchemaValidator` maps it to 500 with the fixed detail; no call without local references |
| D37 | Stages on all writes | `SchemaValidator.validate` is called by create and new version for both types |
| D38 | No bus | No Kafka; two channels only |
| D39 | E-mail transport | `ProviderMailNotifier` (Spring Mail, `SMTP_HOST`, `SMTP_PORT`, no auth), `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`; failures logged |
| D40 | E-mail content | `ProviderMailNotifier.subject` and `body`; events are only raised for PUBLISHED results and PATCH changes |
| D41 | Webhook contract | `WebhookController`: URL check (`isHttpUrl`), events required, unknown event 406 with the fixed detail, duplicate 409, list as array, delete 204 or 404 |
| D42 | Triggers | `SchemaEvent.Kind` PUBLISHED, NEW_VERSION_PUBLISHED (both `SchemaPublished`), REVOKED (`SchemaRevoked`); both schema types |
| D43 | Payload | `WebhookNotifier.payload`, header `X-Simpl-Event`, `Content-Type: application/json` |
| D44 | Delivery | `WebhookNotifier.attempt`: after commit, 5 s timeout, retries after 1 s and 5 s, then dropped and logged; no signature |
| D45 | UI routes and sign-in | `ui/src/router.ts`; `ui/src/auth.ts` (keycloak-js, `login-required`, PKCE S256, client `schema-ui`) |
| D46 | Test ids | All listed `data-testid` values are in `ui/src/views/*.vue` and `ui/src/App.vue` |
| D47 | RESOURCE only e-mail | `ProviderMailNotifier.onEvent` returns for SEARCH |
| D48 | Role-dependent UI | `isAdmin()` hides create, Revoke, Publish and New Version; users without a role get `error-state` `You do not have permission to view this page.` |
| D49 | Create form | `ui/src/views/CreateSchema.vue` |
| D50 | New version form | `ui/src/views/NewVersion.vue`; schema type shown as a disabled select |
| D51 | Revoke | `SchemaList.vue`: `confirm-dialog`, `Schema "{name}" revoked successfully.` |
| D52 | Publish | `SchemaList.vue`: no popup, `Schema {name} published successfully!`, errors show `detail` |
| D53 | List page | `SchemaList.vue`: header texts, filters, client-side sort, paging with `pageSize=20`, empty and error states |
| D54 | Details page | `SchemaDetails.vue`: header fields, versions table, default version descending, sort by Version and Creation Date |
| D55 | No version field | Not present in `NewVersion.vue` |
| D56 | Download latest | `SchemaList.vue` `download`; file `{name}-{version}.ttl` with the version from `Content-Location` |
| D57 | Download version | `SchemaDetails.vue` `download`; uses the page schema type; fixed success and error texts |
| D58 | Persistence | PostgreSQL 16, Flyway, JDBC; content stored as `bytea` |
| D59 | Identity provider | `keycloak/realm-simpl.json`; issuer and JWKS in `SecurityConfig.jwtDecoder` |
| D60 | Doubles and config | `vocabulary-stub/server.js`; Mailpit; `host.docker.internal:host-gateway`; `Settings` reads the D60 names |
| D61 | Test data | No server-side assumption; names and resource types are free within the rules |
| D62 | Quality profiles | Not built; unmatched paths answer 404 problem JSON (`ErrorAdvice.notFound`) |
| D63 | Problem types | `Problems.typeName`: `urn:problem-type:simpl:` + `badRequest`, `unauthorized`, `forbidden`, `notFound`, `notAcceptable`, `conflict`, `unsupportedMediaType`, `internalServerError`; `status` equals the HTTP status |
