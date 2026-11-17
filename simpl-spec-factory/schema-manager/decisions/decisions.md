# Decisions (step 30a): schema-manager

Decisions that close the gaps found in steps 20b and 23b, so the test writer and the code writer build the same thing. Full text, options, basis and owner question per decision: [`decisions.json`](./decisions.json). Final terms and state tables: [`glossary.md`](./glossary.md). Gate result: [`gate.md`](./gate.md).

The spec owners were not available. Every decision marked `provisional` is a choice between readings that the owner should confirm; `follows the specs` means the sources already settle it once read in the order below; `accepted risk` means no source decides it and the writers use the stated assumption.

## Source hierarchy

When sources disagree, the first one in this list wins. Apply it everywhere, also to cases no decision names.

1. **These decisions** (`decisions.json`) and the final glossary.
2. **The OpenAPI** (`specs/api/schema_openapi.yaml`, ref f9e5205) for the wire format and for what exists: paths, methods, field names, enums, status codes, media types. It is the canonical contract of October 2026 and newer than most stories. Where it is silent (no example, a missing operation that the newest implementation story names, an external file that is not in the specs), the next source fills the gap without contradicting it.
3. **Acceptance criteria** for behaviour. Among stories, the newer [ENHANCE] or SIMPL-33537 story wins over the story it restates (D1).
4. **Technical notes and hints** in the stories.
5. **The business process** BP02C: intent and vocabulary, not detailed behaviour.
6. **The CDM** (`schema-manager-be.md`, with the default-properties model as reference data): names and concepts. It is not approved and describes another API revision (D-010), so its behavioural sentences (for example 'version creation does not emit a lifecycle event') rank last.

Why this order: the writers must agree on bytes on the wire first, and the OpenAPI is the only source that is precise and current. Criteria come next because they are what the tests check. The process and the CDM are general and partly out of date.

## Counts


48 decisions: 7 follow the specs, 40 provisional, 1 accepted risk. Of these, 6 are environment choices (D38 to D43). D44 to D48 were added after step 21 (see the section at the end).


## Sources and scope

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D1 | Older stories versus [ENHANCE] and SIMPL-33537 stories | Newer [ENHANCE] or SIMPL-33537 story replaces the older one where they differ; typos read as meant. | provisional | F-030, F-057 |
| D2 | Scope of the build | Build schema, webhook, event, e-mail and UI parts; vocabularies, quality profiles, SIMPL-28815 and catalogue checks out. | provisional | F-044, D-008, D-009, D-010, D-019, F-065 |
| D36 | Terms for schema, schema types, actors and update | Terms: schema, resource description schema, federated search schema, Schema Manager, new version. | provisional | D-006, D-013, D-016, D-018 |

## Identity, versions and resource types

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D3 | Version identifier | Version is a server-generated whole number as a string, from "1"; SemVer criteria withdrawn; wire names unchanged. | follows the specs | F-001, D-002 |
| D4 | Schema identity key and addressing | Key is (name, schemaType, resourceType); 409 on same key whatever the status; RESOURCE needs resourceType; ambiguous name gives 400. | provisional | F-007, F-048, F-063 |
| D5 | One name for the schema identity | Term 'schema name'; wire keeps `name` in bodies and `schemaName` in path and create response; UI sends `name` and `resourceType`. | provisional | D-001, F-047 |
| D6 | Resource type values | resourceType is the closed set Data, Application, Infrastructure; other values 400; UI labels lowercase. | provisional | F-013, D-012 |

## Lifecycle

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D7 | Status of a new RESOURCE schema | New RESOURCE schema is PUBLISHED, or REVOKED if its resource type already has a PUBLISHED one; no event or e-mail then. | provisional | F-003, F-067 |
| D8 | Only one published federated search schema | At most one PUBLISHED SEARCH schema; 409 only against a PUBLISHED one; revoked names stay taken. | follows the specs | F-008, F-062 |
| D9 | No publication through PATCH | PATCH accepts only REVOKED; PUBLISHED gives 400; re-publish by a new version; no Publish button. | provisional | F-002, F-043, F-038, F-053 |
| D10 | Revoking a schema that is already revoked | Revoking a REVOKED schema is 200, no change, no event. | provisional | F-022 |
| D11 | Status of a version | Status is stored on the schema; version status derived: latest = schema status, older = REVOKED. | provisional | F-004, D-003 |
| D12 | New version of a revoked schema | New version re-publishes a REVOKED schema if its scope has no PUBLISHED schema; else RESOURCE stays REVOKED, SEARCH 409. | provisional | F-021 |
| D13 | Lifecycle of a schema in one table | The lifecycle table in glossary.md is the only lifecycle definition. | provisional | D-004 |

## Requests and retrieval

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D14 | New-version request and target schema | Flat OpenAPI fields; schemaType and resourceType select the schema; no match gives 404; UI type read-only. | provisional | F-020, F-049, F-050, F-012 |
| D15 | Create request and Location header | Flat CreateSchemaPayload; base /v1; 400 for missing or bad fields, 415 for non-multipart; Location is the version URI. | provisional | F-019, F-071 |
| D16 | Latest content endpoint GET /schemas/{schemaName} | Add GET /schemas/{schemaName}: latest Turtle with Content-Location; authenticated, viewer or admin. | provisional | F-005, F-064 |
| D17 | Version content endpoint and revoked versions | GET /schemas/{schemaName}/{version} returns Turtle for any status; 404 only when missing. | follows the specs | F-011, F-033, F-024 |
| D18 | Retention of revoked schemas: what is tested here | Retention tested by the four reads after revocation; catalogue and search checks not here. | provisional | F-025, F-034 |
| D30 | Fields of the schema list and the versions list | List fields fixed; `created` added to schema items, `status` and `resourceType` to version items; no creator. | provisional | F-016, F-017, F-031, F-070, D-014 |
| D31 | Ordering, paging and sorting | Fixed server order; numeric version order; UI sorts the current page only. | provisional | F-018 |

## Validation

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D19 | Default properties: check, not enrich | Reject, do not enrich: RESOURCE schemas need named default properties by sh:path local name. | provisional | F-006, F-054, D-011 |
| D20 | Validation rules for SEARCH schemas | SEARCH schemas get no default property check (assumption). | accepted risk | F-051 |
| D21 | Stored and returned content | Content stored and returned byte for byte. | follows the specs | F-023 |
| D22 | Validation errors: status, detail and issues | 400 Problem with issues; stages syntax, default properties, semantics; fixed detail prefixes and issue codes. | provisional | F-027, F-036, D-007 |
| D23 | Semantic validation: errors and warnings | Internal namespaces checked against the registry (errors); external ones warned; W3C and common namespaces skipped. | provisional | F-035 |

## Events and notifications

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D24 | Webhook events: triggers, payload, delivery | SchemaPublished on any route to PUBLISHED, SchemaRevoked on PATCH; defined JSON payload; one synchronous attempt, 5 s. | provisional | F-010, D-005 |
| D25 | Provider notifications by e-mail | One plain-text e-mail to a configured recipient, RESOURCE only, fixed subjects and labelled lines. | provisional | F-009, F-026, F-066 |

## Security

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D26 | Authentication and authorisation responses | Service returns 401 for bad tokens and 403 for missing roles; UI hides write actions. | provisional | F-014, F-040, F-059 |
| D27 | Role names and role matrix | Roles GA_SCHEMA_ADMIN and GA_SCHEMA_VIEWER only; reads need either, writes need admin. | follows the specs | F-015, F-039 |
| D28 | Access to webhook operations | Webhook operations need a valid token, no role; Tier 2 is the gateway's job. | provisional | F-037 |
| D29 | Webhook subscription rules | Syntax-only URL check; 406 for unknown event; 409 for same URL and events; DELETE 204. | follows the specs | F-041, F-042, F-060, F-061, D-015 |

## UI

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D32 | UI: errors, messages and navigation | Form errors in a top banner with detail and issues; list action errors as notifications; fixed 409 texts. | provisional | F-028, F-032, F-056, F-055 |
| D33 | UI: revoking a schema | Confirmation popup; success text 'Schema "{name}" revoked successfully.'. | provisional | F-029 |
| D34 | UI: schema type and resource type filters | SEARCH selection clears and disables resourceType in the UI. | provisional | F-045 |
| D35 | UI: downloads and story scope | Downloads use the content reads; file {name}-v{version}.ttl; compare and edit out of scope. | provisional | F-046, F-058, F-068, F-069 |

## Testing

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D37 | Criteria that need an internal failure | Internal-failure criteria only in component tests with fault injection. | provisional | F-052 |

## Environment choices

The specs do not fix these; both writers use them as given.

| Id | Topic | Choice |
|---|---|---|
| D38 | base URL and ports | Backend at http://localhost:8080 with base path /v1. PUBLIC_BASE_URL (default http://localhost:8080/v1) builds Location, Content-Location, graphName and e-mail links. The UI runs at http://localhost:4200 and calls the backend base URL. |
| D39 | identity provider and test users | OIDC with JWT bearer tokens signed RS256; issuer and JWKS location from OIDC_ISSUER_URI and OIDC_JWKS_URI; roles in realm_access.roles (Keycloak layout); audience is not checked. Test users: ga-admin (GA_SCHEMA_ADMIN), ga-viewer (GA_SCHEMA_VIEWER), ga-norole (no schema role), participant (no role, used for webhooks). Tests may mint tokens with a local test key pair published at the JWKS location instead of running Keycloak. |
| D40 | store and test isolation | The store is the builder's choice but the service must start with an empty store in tests (for example an in-memory mode, STORE=memory). Because the one-published rules are global per resource type and for SEARCH, each test scenario runs against a fresh, empty instance. No reset or test-only endpoint is added. |
| D41 | mail stub | SMTP stub at NOTIFICATION_SMTP_HOST:NOTIFICATION_SMTP_PORT (default localhost:1025) with a MailHog-compatible HTTP API on port 8025 for tests. NOTIFICATION_RECIPIENT default providers@simpl.test, NOTIFICATION_SENDER default schema-manager@simpl.test, NOTIFICATION_INSTRUCTIONS_URL default https://simpl.test/schemas/instructions. |
| D42 | vocabulary registry stub | Known internal vocabularies come from a JSON file at VOCABULARY_FIXTURE: a list of objects {"namespace": IRI, "terms": [IRI, ...]}. INTERNAL_VOCABULARY_BASE default https://vocabulary.simpl.test/. The production adapter to the Vocabulary Manager is behind the same port and not tested here. |
| D43 | webhook receiver | Tests run a local HTTP receiver that records each POST (headers and JSON body) and can answer 200 or 500 or delay beyond 5 seconds, to check D24 delivery and that failures do not change the API response. |

## Added after step 21 (17 Nov 2026)

Step 21b (API review, [`api-findings.json`](../validation/api-findings.json)) ran after step 30 and found 5 majors without a default. These decisions give each one. Earlier decisions are unchanged; D46 adds checks on top of D29.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D44 | Build contract | Frozen OpenAPI plus decisions; decisions win where they differ. Listed differences; generated stubs and validators must apply them. Versions response built as the paging wrapper with D30 items. | provisional | P-005 |
| D45 | Security scheme | Service validates the token itself and checks roles only, never OAuth2 scopes; webhook operations never answer 403. | follows the specs | P-001 |
| D46 | Webhook target allow list | targetUrl host must be in WEBHOOK_ALLOWED_HOSTS (exact, fail closed), https only unless WEBHOOK_ALLOW_HTTP; 400 otherwise; host checked again at delivery, no redirects. | provisional | P-002 |
| D47 | One not-found problem type | Every in-scope 404 uses urn:problem-type:simpl:notFound, title 'Resource Not Found'. | provisional | P-003 |
| D48 | Value rules for resourceType and version | D6 and the SEARCH rule of D4 apply on every operation; any unknown version, malformed or not, gives 404. | provisional | P-004 |

The minors of step 21b (P-006 to P-013) need no decision for the build: they are example, naming and wording points for the API owner, or are settled by D3, D5, D6, D15, D17, D29 and D44.

## Questions for the spec owners, by impact

1. **D46 (webhook allow list).** OWASP-11 is a MUST rule. Confirm the configured allow list with a fail-closed default, who maintains it, and that plain http is refused outside tests. Add the https rule and the two 400 texts to the OpenAPI.
2. **D44, D45, D47, D48 (revised OpenAPI).** Publish a revised OpenAPI with the differences listed in D44: GET /schemas/{schemaName}, 401 everywhere, an openIdConnect scheme without scopes and with roles per operation, no 403 on webhooks, one 404 type, the resourceType enum, the D30 fields, the D15 Location format, the event callbacks and the inlined versions response. That file then becomes the build contract.
3. **D9, D12 (publication route).** Re-publication through PATCH with PUBLISHED is dropped in favour of a new version, following the OpenAPI. Confirm, or extend the PATCH enum and codes. Affects SIMPL-20529, SIMPL-20530, SIMPL-31098.
4. **D7, D8 (one published schema per scope).** A second RESOURCE schema for a resource type is created as REVOKED; a second published SEARCH schema is rejected with 409. Confirm the asymmetry and that Providers are not told about a schema created as REVOKED.
5. **D19, D20 (default properties).** Reject instead of enrich, with properties matched by local name from the default-properties model. Publish the full IRIs per resource type, say what applies to SEARCH, and correct BP02C.04.
6. **D4 (schema key).** (name, schemaType, resourceType), with revoked names still taken and 400 for an ambiguous name. Record the key in the CDM.
7. **D24 (webhook events).** Payload, triggers (including new versions) and single-attempt delivery are invented here. Define them in the OpenAPI callbacks or an AsyncAPI file.
8. **D25 (Provider e-mail).** One configured recipient, fixed subjects and fields. Confirm, and say what the instruction link points to.
9. **D16 (GET /schemas/{schemaName}).** Added to this service and not public. Add it to the OpenAPI, or confirm a separate Resolver.
10. **D11 (version status).** Derived, not stored; previous versions show REVOKED. Confirm and add the transition table to the CDM (D13).
11. **D3 (version).** SemVer withdrawn; rename schemaVersion to version in the next API revision.
12. **D6 (resource type).** Closed set Data, Application, Infrastructure. Confirm or name the open set.
13. **D2 (scope).** Deprecate or remove /vocabularies; decide whether metadata quality profiles belong here and, if so, write stories.
14. **D22, D23 (validation errors).** Confirm the detail prefixes and issue codes as contract, and how the Vocabulary Manager is queried in production.
15. **D26, D28 (401 and webhook access).** Add 401 to every operation; confirm any authenticated participant may manage subscriptions.
16. **D30, D31 (lists).** Provide common-components.yaml, confirm the added fields, say whether a creator column is needed and whether sorting must cover the whole result.
17. **D29 (unknown event type).** 406 follows the contract; API-12 would prefer 400.
18. **D1 (story pairs).** Cancel or update the originals of each [ENHANCE] story.
19. UI texts and behaviour (D32 to D35): confirm placements, texts and file names.
