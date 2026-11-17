# Glossary (final, step 30a): schema-manager

The one language for the build and the tests of the Schema Manager. It replaces the draft in [`../validation/glossary.md`](../validation/glossary.md). Decisions `D` are in [`decisions.json`](./decisions.json); findings `F-` and `D-` (with a hyphen) are in `../validation/`.

Rule for writers: use the **Term** in prose, test names, UI text and code identifiers. Use the **API names** exactly on the wire, even where they differ from the term (the frozen OpenAPI is not renamed in this build). Never use a **retired synonym**.

## Terms

### Actors, roles and components

| Term | Definition | CDM class | API names | Retired synonyms |
|---|---|---|---|---|
| Governance Authority | The actor (person or organisation) that creates, versions and revokes schemas in a data space. Never the system. | `GovernanceAuthority` (external actor) | none; the caller holds a role in its token | "Governance Authority" for the receiving system (SIMPL-24175); "GA client"; "governance authority service" (SIMPL-19992) |
| Schema admin role | Token role that allows every schema operation: reads, create, new version, revoke (D27). Includes the viewer role. | none | `GA_SCHEMA_ADMIN` in `realm_access.roles` | `governance-admin`; "role authorising schema management" |
| Schema viewer role | Token role that allows the schema reads only (D27). | none | `GA_SCHEMA_VIEWER` in `realm_access.roles` | `governance-viewer`; "role authorising schema viewing" |
| Participant | Any authenticated caller; may manage webhook subscriptions (D28). | none | none | none |
| Provider | Participant that publishes resource descriptions and receives the Provider notification. | none | none | "Provider end user" (only in SIMPL-28815, out of scope) |
| Schema Manager | This component. | the CDM itself | API title "Schema Management API" | "Simpl", "Schema Management Service (SMS)", "the service", "the system" (in prose) |
| Simpl-Open | The platform as a whole, as in BP02C. | none | none | "Simpl" |
| Vocabulary Manager | External registry of internal vocabularies used by semantic validation (D23). In tests replaced by the vocabulary registry stub (D42). | `VocabularyManager` (external service) | none | "Vocabulary Management Domain" |
| Federated Catalogue | External service that consumes published schemas, typically as a webhook subscriber. Not tested here (D18). | `FederatedCatalogue` (external service) | none | "data space catalogue" (in this component's prose) |
| Schema validator | The internal step that runs syntax, default property and semantic validation (D22). A step of the service, not a business object. | `RdfValidator` (kept in the CDM; the build uses the term only) | none | "RdfValidator" in prose |

### Schemas and versions

| Term | Definition | CDM class | API names | Retired synonyms |
|---|---|---|---|---|
| Schema | Versioned SHACL definition of the metadata of one resource type (RESOURCE) or of federated search (SEARCH). Carries the status. Identified by the key (schema name, schema type, resource type) (D4). | `Schema` | `SchemaCategory` (wire name of the PATCH response); path `/schemas/{schemaName}` | "schema concept", "schema category", "schema file" |
| Schema name | The name part of the schema key; PascalCase, alphanumeric, 3 to 64 characters. | `Schema.schemaName`, `SchemaMetadata.name` | `name` in every body; path parameter `schemaName`; `schemaName` in the POST /schemas response only | "Schema Name" as a variable name; a body field `schemaName` |
| Schema key | (schema name, schema type, resource type); resource type empty for SEARCH. Unique whatever the status (D4). | not yet in the CDM | path `schemaName` plus query or form `schemaType`, `resourceType` | "uniqueness key is the name" |
| Schema type | `RESOURCE` or `SEARCH`; omitted means `RESOURCE`. | `Schema.schemaType` | `schemaType` | "schematype", "SchemaType" |
| Resource description schema | A schema with schema type `RESOURCE`. | `Schema` with `schemaType = RESOURCE` | `schemaType: RESOURCE` | "Resource Definition Schema", "resource schema", "RESOURCE schema" (in prose) |
| Federated search schema | A schema with schema type `SEARCH`; has no resource type; at most one is PUBLISHED (D8). | `Schema` with `schemaType = SEARCH` | `schemaType: SEARCH` | "Federated Search Schema" (capitals), "SEARCH schema" (in prose), "FedSearch" as a term |
| Resource type | Closed set `Data`, `Application`, `Infrastructure` for resource description schemas (D6). Same set as offeringType of a resource description. | `Schema.resourceType` | `resourceType` | `data`, `application`, `infrastructure` as stored values (UI labels only), `data-schema`, `APP`, `INFRA`, `DATA` |
| Schema version | One immutable version of a schema: its schema definition, changelog and creation time. | `SchemaVersion` | items of the versions list; `CreateNewVersionSchemaResponsePayload` | "update" (for a new version) |
| Version | Identifier of a schema version: server-generated whole number as a string, "1", "2", ... (D3). | `SchemaVersion.versionNumber`, `SchemaMetadata.version` | `version` (lists, new-version response, path); `schemaVersion` (POST /schemas response); `latestVersion` (PATCH response) | `versionInfo`, "version number" as a separate concept, SemVer strings such as "1.2.0" |
| Latest version | The schema version with the highest version; it carries the status of its schema (D11). | none (derived) | `latestVersion`; `version` in GET /schemas items | "current version" |
| Previous version | Any version that is not the latest; its status is always REVOKED (D11). | none (derived) | `status` in the versions list | "superseded version", "unavailable" version, SUPERSEDED |
| Changelog | Short text describing the changes of a version; required for a new version. | `SchemaVersion.changelog` | `changelog` | "Latest Change" (UI column label only, mapped to changelog) |
| Schema definition | The uploaded Turtle file with the SHACL shapes of a version; stored and returned byte for byte (D21). | `SchemaVersion.rdfContent` | `schemaFile` (upload); `text/turtle` body of the two content reads | "raw SHACL content", "schema content", "schema definition file", "SHACL definition" (all fine in prose as descriptions, but the term is schema definition) |
| Schema metadata | The list view of a schema: name, title, description, resource type, schema type, latest version, changelog, status, created (D30). A view, not a stored object. | `SchemaMetadata` (view of Schema and SchemaVersion) | `GetAllSchemasResponsePayload.metadata[]` | "schema details", "core metadata", "the schema and its properties" |
| Title | Human-readable name of a schema; set at creation, never changed (D30). | `SchemaMetadata.title` (belongs to Schema) | `title` | none |
| Description | Description of a schema; set at creation, never changed (D30). | `SchemaMetadata.description` (belongs to Schema) | `description` | description per version |
| Version URI | Absolute URI `{PUBLIC_BASE_URL}/schemas/{name}/{version}?schemaType=...[&resourceType=...]` of a version (D15). Also its named graph. | none | `Location`, `Content-Location` headers; `graphName` in the versions list | "named graph" as a separate concept, "versioned URI", "version-subject URI" |
| Named graph | Same as version URI. | none | `graphName` | see version URI |
| Created | Creation time of a schema (list) or a version (versions list), ISO 8601 UTC. | `Schema.createdAt`, `SchemaVersion.createdAt` | `created`; `modified` on `SchemaCategory` | "Creation Date" (UI label only) |

### Lifecycle

| Term | Definition | CDM class | API names | Retired synonyms |
|---|---|---|---|---|
| Status | Lifecycle state of a schema: `PUBLISHED` or `REVOKED`. Stored on the schema only; a version's status is derived (D11). | `Schema.lifecycleStatus`; `SchemaVersion.lifecycleStatus` (derived) | `status` | `lifecycleStatus` in code, "state" |
| Publish | A schema or a version becomes PUBLISHED, by any route: creation (D7, D8), new version (D12), re-publication of a revoked schema by a new version (D12). Raises SchemaPublished (D24). There is no publish request. | `SchemaEvent.eventType = SchemaPublished` | status `PUBLISHED`; event `SchemaPublished` | "PUBLISH" (as a value), "publish" for a PATCH request |
| Revoke | Change a schema from PUBLISHED to REVOKED with PATCH; all its versions then show REVOKED. Raises SchemaRevoked. | `Schema.lifecycleStatus = REVOKED` | `PATCH /schemas/{schemaName}` with `{"status": "REVOKED"}` | "soft-delete" for schemas |
| Retain | Keep a revoked schema and all its versions stored and readable (D18). | none | the four read operations | none |
| New version | Create the next version of an existing schema (D14). | `SchemaVersion` | `POST /schemas/{schemaName}/versions` | "update", "Updating existing schema" |
| Scope (of the one-published rule) | For a resource description schema: all resource description schemas of the same resource type. For a federated search schema: all federated search schemas. At most one schema per scope is PUBLISHED. | none | none | "one schema per resource type" (BP02C wording, means one published) |

### Validation

| Term | Definition | CDM class | API names | Retired synonyms |
|---|---|---|---|---|
| Syntactic validation | Stage 1: the file parses as Turtle and has at least one sh:NodeShape (D22). | none | 400, detail prefix `Syntax Error: `; issue codes `syntaxError`, `noNodeShape` | "structural correctness", "syntax and integrity" |
| Default property | Baseline property that every resource description schema must already contain, checked by sh:path local name (D19). Not added by Simpl-Open. | none (list in `schema-manager-be-default-properties.md`) | 400, detail prefix `Default Properties Error: `; issue code `defaultPropertyMissing` | "mandatory property", "default or mandatory properties", "Mandatory properties error" |
| Mandatory property | Retired in the build. In BP02C it means a property a resource description must fill; the Schema Manager does not check it apart from default properties. | none | none | use default property |
| Custom property | Property defined by the Governance Authority in its schema; not checked beyond syntax and semantics. | none | none | none |
| Semantic validation | Stage 3: internal vocabulary namespaces and terms must exist; external namespaces only give warnings (D23). | none | 400, detail prefix `Semantic Validation Error: `; codes `localReferenceUnavailable`, `unknownVocabularyTerm`; warning code `externalReferenceSkipped` | "validation rules, relationships, and dependencies" as a separate check |
| Validation issue | One finding of validation: code, description, location. An error when in `Problem.issues` of a 400, a warning when in `validationWarnings` of a 201 (D22). | `SchemaValidationWarning` (to be renamed SchemaValidationIssue) | `SchemaValidationIssue` | "SchemaValidationWarning" for errors, "error message", "structured error response", "report", "feedback" |
| Internal vocabulary | Vocabulary whose namespace starts with INTERNAL_VOCABULARY_BASE and is known to the registry (D23, D42). | external, `InternalVocabulary` in the Vocabulary Manager CDM | none | "self managed vocabulary" |
| External vocabulary | Any other non-skipped namespace; never fetched. | none | warning `externalReferenceSkipped` | none |

### Events and notifications

| Term | Definition | CDM class | API names | Retired synonyms |
|---|---|---|---|---|
| Schema event | JSON message POSTed to subscribers when a schema is published or revoked (D24); sent from an outbox after the API answers, at least once, in order per subscription, with the same eventId on retries (D50). | `SchemaEvent` | `eventType` values `SchemaPublished`, `SchemaRevoked`; payload fields `eventId`, `eventType`, `occurredAt`, `name`, `schemaType`, `resourceType`, `version`, `status` | "event notification", "schema publishing events" |
| Webhook subscription | Registration of a target URL for one or more event types; no states; unique by target URL plus set of events (D29). | `Webhook` | `Webhook`, `CreateWebhookRequest`, `webhookId`, `targetUrl`, `events` | "subscription" alone is fine in prose; "active webhook subscription" |
| Webhook id | Server-generated UUID of a webhook subscription. | `Webhook.webhookId` | `webhookId` | "ID", "wh_123" style ids |
| Target URL | Absolute URL that receives schema events; https only and host on the allow list (D46). | `Webhook.targetUrl` | `targetUrl` | "endpoint" |
| Subscriber | The system behind a target URL. | none | none | "governance authority service" |
| Provider notification | Plain-text e-mail to the configured recipient for resource description schemas only, on publish and revoke (D25). Separate from schema events. | none (to be added to the CDM) | none (configuration `NOTIFICATION_RECIPIENT`) | "notify all Providers" as a per-Provider send |

### Out of scope in this build (D2)

| Term | Definition | CDM class | API names | Retired synonyms |
|---|---|---|---|---|
| Metadata quality profile | Governance template of quality rules per resource type. Out of scope: not built, not tested. The one term is metadata quality profile. | `MetadataQualityProfile` | `/metadataQualityProfiles/...`, `metadataQualityProfileName` | "Quality profile", "mqp", `simpl:QualityProfile` |
| Metadata quality profile version | Version of a metadata quality profile. Out of scope. | `MetadataQualityProfileVersion` | `mqpVersion` | `profileVersion` |
| Quality dimension | Part of a metadata quality profile. Out of scope. | `QualityDimension` | inside JSON-LD only | none |
| Quality rule | Part of a metadata quality profile. Out of scope. | `QualityRule` | inside JSON-LD only | none |
| Existence rule | Kind of quality rule. Out of scope. | `ExistenceRule` | inside JSON-LD only | none |
| Is-in rule | Kind of quality rule. Out of scope. | `IsInRule` | inside JSON-LD only | none |
| Custom SHACL rule | Kind of quality rule. Out of scope. | `CustomShaclRule` | inside JSON-LD only | none |
| Threshold | Part of a metadata quality profile. Out of scope. | `Threshold` | inside JSON-LD only | none |
| Score range | Part of a metadata quality profile. Out of scope. | `ScoreRange` | inside JSON-LD only | none |
| Classification | Part of a metadata quality profile. Out of scope. | `Classification` | inside JSON-LD only | none |
| Vocabulary | Owned by the Vocabulary Manager; the `/vocabularies` operations of this API are legacy and not built. | none here; `InternalVocabulary` in the Vocabulary Manager CDM | `/vocabularies/...` (not built) | "vocabulary" as a Schema Manager entity |
| Billing schema | A contract clause of a resource description, not a schema of this component; SIMPL-28815 is out of scope. | none here | none | "schema" for a contract clause |
| Resource description | Metadata a Provider creates from a schema; owned by the catalogue context. | none here; `ResourceDescription` in `catalogue-be.md` | none | none |

## Synonyms to retire (summary)

| Retire | Use instead |
|---|---|
| schema concept, schema category | schema |
| Resource Definition Schema, resource schema, RESOURCE schema | resource description schema |
| Federated Search Schema, SEARCH schema (in prose) | federated search schema |
| versionInfo, versionNumber (outside the CDM), SemVer versions | version |
| superseded version, unavailable | previous version, status REVOKED |
| mandatory property, Mandatory properties error | default property, Default Properties Error |
| SchemaValidationWarning (for errors) | validation issue |
| update (for a new version) | new version |
| governance-admin, governance-viewer | GA_SCHEMA_ADMIN, GA_SCHEMA_VIEWER |
| PUBLISH (value) | PUBLISHED |
| active (subscription) | (no state) |
| Simpl, SMS, the service, the system (in prose) | Schema Manager (component), Simpl-Open (platform) |
| Quality profile, mqp | metadata quality profile (out of scope) |

## State tables

### Schema status

One name per state. The API value, the CDM value and the UI text are the same word.

| State | Meaning | API | CDM | UI |
|---|---|---|---|---|
| PUBLISHED | The schema's latest version may be used for new resource descriptions (or for federated search). At most one per scope. | `PUBLISHED` | `PUBLISHED` | "PUBLISHED"; action Revoke shown |
| REVOKED | No version may be used for new descriptions; schema and versions stay readable. | `REVOKED` | `REVOKED` | "REVOKED"; no action button |

### Schema lifecycle (D13)

| From | Operation | Condition | To | API answer | Event | Provider e-mail (RESOURCE only) |
|---|---|---|---|---|---|---|
| (none) | POST /schemas, RESOURCE | no PUBLISHED schema with same resourceType | PUBLISHED | 201 | SchemaPublished | "Schema published" |
| (none) | POST /schemas, RESOURCE | a PUBLISHED schema with same resourceType exists | REVOKED | 201 | none | none |
| (none) | POST /schemas, SEARCH | no PUBLISHED SEARCH schema | PUBLISHED | 201 | SchemaPublished | none |
| (none) | POST /schemas, SEARCH | a PUBLISHED SEARCH schema exists | (nothing created) | 409 | none | none |
| any | POST /schemas | same key exists | (nothing created) | 409 | none | none |
| PUBLISHED | POST versions | valid | PUBLISHED, new latest version | 201, status PUBLISHED | SchemaPublished | "Schema version published" |
| REVOKED | POST versions | no other PUBLISHED schema in scope | PUBLISHED | 201, status PUBLISHED | SchemaPublished | "Schema version published" |
| REVOKED | POST versions, RESOURCE | another PUBLISHED schema in scope | REVOKED, new latest version | 201, status REVOKED | none | none |
| REVOKED | POST versions, SEARCH | another PUBLISHED SEARCH schema | (nothing created) | 409 | none | none |
| PUBLISHED | PATCH `{"status":"REVOKED"}` | | REVOKED | 200 | SchemaRevoked | "Schema revoked" |
| REVOKED | PATCH `{"status":"REVOKED"}` | | REVOKED (no change) | 200 | none | none |
| any | PATCH with any other status | | (no change) | 400 | none | none |
| any | any write | validation fails | (no change) | 400 | none | none |

### Schema version status (derived, D11)

| State | Rule | API | UI |
|---|---|---|---|
| PUBLISHED | latest version of a PUBLISHED schema | `status: PUBLISHED` in the versions list and new-version response | "PUBLISHED" |
| REVOKED | any previous version, or any version of a REVOKED schema | `status: REVOKED` | "REVOKED" |

### Schema type

| Value | API | CDM | UI |
|---|---|---|---|
| RESOURCE | `RESOURCE`, default when omitted | `RESOURCE` | "RESOURCE" (default selection) |
| SEARCH | `SEARCH` | `SEARCH` | "SEARCH"; resource type control cleared and disabled (D34) |

### Resource type (D6)

| Stored and API value | UI label | offeringType of a resource description |
|---|---|---|
| `Data` | data | data |
| `Application` | application | application |
| `Infrastructure` | infrastructure | infrastructure |

### Schema event type

| Value | Raised when (D24) |
|---|---|
| `SchemaPublished` | a schema or a version becomes PUBLISHED |
| `SchemaRevoked` | PATCH changes a schema from PUBLISHED to REVOKED |

### Webhook subscription

No states. A subscription exists from POST /webhooks (201) until DELETE (204).
