# Glossary (draft, step 23b): schema-manager

Draft glossary for the Schema Manager bounded context. One row per concept. Terms are proposed, not decided: the spec owners choose in step 30. Where a choice was made among synonyms, the order of preference is the business process (BP02C), then the stories, then the CDM, then the API. Findings `D-` are in [`domain-findings.json`](./domain-findings.json); findings `F-` are from step 20b ([`ac-findings.json`](./ac-findings.json)).

Sources: `specs/functional/bp02c-manage-resource-description-schemas.md` (BP02C), `specs/stories/` (55 stories), `specs/api/schema_openapi.yaml` (ref f9e5205), `cdm/schema-manager-be.md` (class ids `CDM:schema-manager-be:<Class>`), context `cdm/vocabulary-manager-be.md` and `cdm/catalogue-be.md`, and the flowchart `cdm/schema-manager-be-default-properties.md` (not in the register).

Status: `agreed` = all sources already use one term; `proposed` = chosen among synonyms; `open` = the sources disagree on the meaning.

## Terms

### Actors and components

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Governance Authority | The actor that creates, versions and revokes schemas in a data space (BP02C Actors). | `CDM:schema-manager-be:GovernanceAuthority` (external actor) | none (roles in the token) | almost all stories, for example SIMPL-13343/AC1, SIMPL-17066/AC1 | "Governance Authority administrator" (SIMPL-13355, SIMPL-14795), "Governance Authority user" (SIMPL-14703), "GA user", "GA client" (SIMPL-13347, SIMPL-32788); also used for the receiving system in SIMPL-24175/AC1 | proposed (D-016) |
| Schema admin role | The role that may create, version, publish and revoke schemas (SIMPL-17066 technical info). | none | none (token role `GA_SCHEMA_ADMIN`) | SIMPL-17066/AC1 to AC4, SIMPL-13353/AC1, SIMPL-20529/AC1 | `governance-admin` (SIMPL-13343/AC4, SIMPL-13358/AC1, SIMPL-32808/AC2), "role authorising schema management" (SIMPL-14703, SIMPL-33694) | open (F-015) |
| Schema viewer role | The role that may list schemas and versions (SIMPL-17066 technical info). | none | none (token role `GA_SCHEMA_VIEWER`) | SIMPL-17066/AC5, AC6, SIMPL-13343/AC1 | `governance-viewer` (SIMPL-13343/AC4), "role authorising schema viewing" (SIMPL-33537/AC3) | open (F-015) |
| Provider | The participant who publishes resource descriptions and is informed about schema changes (BP02C.06). | none (actor not in this CDM) | none | SIMPL-14709, SIMPL-14712, SIMPL-14791, SIMPL-33694 | "Provider end user" (SIMPL-28815) | agreed |
| Schema Manager | The component that manages resource description schemas (CDM Purpose). | the CDM itself | API title "Schema Management API"; descriptions "simpl-schema-manager" | system side of every story | "Simpl", "Simpl-Open", "Schema Management Service (SMS)", "the service", "the system" | proposed (D-016) |
| Vocabulary Manager | External registry of vocabularies used to check schema semantics (CDM). | `CDM:schema-manager-be:VocabularyManager` (external service) | none | SIMPL-22407 ("referenced vocabularies") | "Vocabulary Management Domain" (CDM) | agreed |
| Federated Catalogue | External service that consumes published schemas for resource description validation and discovery (CDM). | `CDM:schema-manager-be:FederatedCatalogue` (external service) | none (webhook subscriber, example targetUrl `catalogue.example.com`) | SIMPL-14701/AC1 ("my catalogue"), SIMPL-24175 technical info ("e.g. Catalogue") | "data space catalogue" (BP02C.02, BP02C.05) | proposed |
| Quality Scoring Service | External service that resolves the active metadata quality profile for quality assessment (CDM). | `CDM:schema-manager-be:QualityScoringService` (external service) | none | none | none | agreed (CDM only) |
| Schema validator | Internal capability that parses and checks the Turtle and SHACL of a schema (CDM). | `CDM:schema-manager-be:RdfValidator` (owned service) | none | none (stories say "Simpl validates") | "RdfValidator" | proposed (D-017: may leave the CDM) |

### Schemas and versions

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Schema | Defines structure and format for metadata describing resources: mandatory and optional properties, data types and validation rules (BP02C Overview). | `CDM:schema-manager-be:Schema` | `SchemaCategory`; path `{schemaName}`; `CreateSchemaPayload` | SIMPL-13353/AC1, SIMPL-13360/AC1, SIMPL-15837/AC1, SIMPL-20529/AC1 | "schema concept" (10 stories, for example SIMPL-13353/AC1, SIMPL-15837/AC3), "schema category" (API, 12 places), "schema file" (SIMPL-32800/AC3) | proposed (D-006) |
| Schema name | The name that identifies a schema; PascalCase, alphanumeric, 3 to 64 characters (SIMPL-13345/AC2). | `Schema.schemaName` | path `schemaName`; `CreateSchemaResponsePayload.schemaName`; `name` in `CreateSchemaPayload`, `SchemaCategory`, `GetAllSchemasResponsePayload.metadata[]`, `CreateNewVersionSchemaResponsePayload` | SIMPL-13345/AC1, SIMPL-13353/AC2, SIMPL-32800/AC1, SIMPL-32807/AC1 | `name` (API bodies, SIMPL-13353, SIMPL-13355), "Schema Name" (UI, SIMPL-15837/AC1) | open (D-001, F-007) |
| Schema type | Whether a schema is a resource description schema (`RESOURCE`) or the federated search schema (`SEARCH`); omitted means `RESOURCE` (SIMPL-33537). | `Schema.schemaType`, `SchemaMetadata.schemaType` | `schemaType` on 6 schemas and as query parameter on 4 operations | SIMPL-31098, SIMPL-32258/AC2, SIMPL-32801, SIMPL-32802, SIMPL-33537/AC3 | "SchemaType" (UI column, SIMPL-32790/AC1), "schematype" (technical info) | agreed |
| Resource description schema | A schema of type `RESOURCE`, used to create and validate resource descriptions of one resource type (BP02C Overview, SIMPL-32802 technical info). | `Schema` with `schemaType = RESOURCE` | `schemaType: RESOURCE` | SIMPL-32258/AC1, SIMPL-32802/AC1, SIMPL-33694/AC1 | "Resource Definition Schema" (SIMPL-32800, SIMPL-32802, SIMPL-32807), "resource schema" (SIMPL-32258, SIMPL-13346), "RESOURCE schema" (SIMPL-33694) | proposed (D-013) |
| Federated search schema | The single schema of type `SEARCH` with the common attributes of all federated catalogues; it has no resource type (SIMPL-33537, CDM). | `Schema` with `schemaType = SEARCH` | `schemaType: SEARCH` | SIMPL-31098/AC1, SIMPL-32801/AC4 to AC6, SIMPL-33537/AC1 to AC5 | "Federated Search schema" (SIMPL-33537), "SEARCH schema" (SIMPL-33694/AC5), "FedSearch" (example name) | proposed (D-013; F-008 for the one-schema rule) |
| Resource type | The kind of resource a resource description schema applies to (BP02C Overview: data, application, infrastructure). | `Schema.resourceType`, `SchemaMetadata.resourceType` | `resourceType` (pattern `^[A-Z][a-zA-Z0-9]*$`) | SIMPL-13355/AC1, SIMPL-20686/AC1 to AC3, SIMPL-32802/AC3 | `data`/`application`/`infrastructure` (SIMPL-13355), `APP`/`INFRA`/`DATA` (SIMPL-32802), `data-schema` (SIMPL-13343/AC2), "offeringType" (default-properties model, catalogue CDM) | open (D-012, F-013) |
| Schema version | One version of a schema with its own definition and changelog; a new version replaces the previous one for new resource descriptions (BP02C.02, API summary). | `CDM:schema-manager-be:SchemaVersion` | `CreateNewSchemaVersionPayload`, `CreateNewVersionSchemaResponsePayload`, `GetSchemaVersionsResponsePayload` (external file) | SIMPL-13345/AC1, SIMPL-13357/AC1, SIMPL-32786/AC1, SIMPL-32807/AC1 | "new version of an existing schema" (BP, many stories) | agreed |
| Version | The identifier of a schema version; server-generated integer as a string in the API (API descriptions). | `SchemaVersion.versionNumber`, `SchemaMetadata.version` | path `{version}`; `version`, `schemaVersion`, `latestVersion` | SIMPL-20528/AC1, SIMPL-20534/AC1, SIMPL-20770/AC2 | `versionNumber` (CDM), `schemaVersion` (API create response), `versionInfo` (SIMPL-13353/AC1), "version number" (SIMPL-20528), SemVer strings (SIMPL-13357/AC1) | open (D-002, F-001) |
| Latest version | The version with the highest number, which is the one in use (SIMPL-20528 technical details). | none (derived) | `SchemaCategory.latestVersion` | SIMPL-13347/AC1, SIMPL-32788/AC1, SIMPL-15837/AC1 ("Latest Version" column) | none | proposed |
| Changelog | Short description of the changes in a version (API). | `SchemaVersion.changelog`, `SchemaMetadata.changelog` | `changelog` | SIMPL-13345 user story, SIMPL-13357/AC1, SIMPL-32807/AC1 | "Latest Change" (UI label, SIMPL-13346/AC1, mapping written there) | agreed |
| Schema definition | The SHACL shapes of a schema version in Turtle, uploaded as a `.ttl` file and returned as `text/turtle` (API, SIMPL-13347). | `SchemaVersion.rdfContent` | `schemaFile` (upload); response body of `GET /schemas/{schemaName}/{version}` | SIMPL-13353/AC1, SIMPL-13347/AC1, SIMPL-32788/AC1, SIMPL-33537/AC4 | "SHACL definition" (5 stories), "schema definition file" (SIMPL-13358, SIMPL-20770), "raw SHACL content" (SIMPL-13347), "schema content" (API, SIMPL-13364), `rdfContent` (CDM) | proposed (D-014; F-023 on enrichment) |
| Schema metadata | The summary of a schema returned in lists: name, title, description, resource type, schema type, version, changelog, status (API, CDM). | `CDM:schema-manager-be:SchemaMetadata` (a view of Schema and SchemaVersion) | `GetAllSchemasResponsePayload.metadata[]` (inline) | SIMPL-13343/AC1, SIMPL-32258/AC1, SIMPL-15837/AC1 | "schemas metadata" (SIMPL-13343 title), "schema details" (SIMPL-17066/AC5, SIMPL-22407/AC1), "the schema and its properties" (SIMPL-14354/AC1), "core metadata" (SIMPL-13353) | proposed (D-017; F-016 for the field list) |
| Title | Longer, human-readable name of a schema (API). | `SchemaMetadata.title` (not on Schema) | `title` | SIMPL-13355/AC1, SIMPL-32800/AC1 | none | open on placement (D-014) |
| Description | Description of a schema (API). | `SchemaVersion.description`, `SchemaMetadata.description` | `description` | SIMPL-13355/AC1, SIMPL-32800/AC1 | none | open on placement (D-014) |
| Named graph | The versioned URI of a schema version, returned in Content-Location (SIMPL-13347/AC1). | none | versions example field `graphName`; header Content-Location | SIMPL-13347/AC1, SIMPL-32788/AC1 to AC3 | "versioned URI", "version-subject URI" (API) | open (D-014) |

### Lifecycle

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Status | The lifecycle state of a schema: `PUBLISHED` or `REVOKED` (CDM, API). | `Schema.lifecycleStatus`, `SchemaVersion.lifecycleStatus`, `SchemaMetadata.status` | `status` | SIMPL-13360/AC1, SIMPL-20529/AC1, AC2, SIMPL-20686/AC1 to AC3 | `lifecycleStatus` (CDM), "state" (SIMPL-20529, SIMPL-20530) | proposed (D-014); owner of the state open (D-003, F-004) |
| Publish | To make a schema or version available for new resource descriptions (BP02C.05). | `SchemaEvent.eventType = SchemaPublished` | status `PUBLISHED`; event `SchemaPublished` | SIMPL-14709/AC1, SIMPL-20529/AC1, SIMPL-31098/AC1, SIMPL-33537/AC1 | "PUBLISH" (SIMPL-20530/AC2), "automatically publish" (SIMPL-31098/AC1) | open (D-005, F-002, F-010) |
| Revoke | To make a schema unavailable for new resource descriptions, without changing existing ones (BP02C.03, BP Assumptions). | `Schema.lifecycleStatus = REVOKED`; `SchemaEvent.eventType = SchemaRevoked` | `PATCH /schemas/{schemaName}` with `SchemaStatusUpdateRequest.status = REVOKED` | SIMPL-13360/AC1, SIMPL-13361/AC1, SIMPL-20529/AC2, SIMPL-33537/AC5 | "soft-delete" (CDM, for profiles only) | agreed |
| Superseded version | A version that a newer version of the same schema has replaced (CDM "supersedes"). | none (CDM text says "unavailable") | none (API summary says "unavailable") | SIMPL-13357/AC1, SIMPL-32807/AC1 to AC3 (say REVOKED) | "REVOKED" (BP02C.05, stories), "unavailable" (CDM, API), "inherits the status" (SIMPL-20530 technical details) | open (D-003, F-004) |
| Retain | To keep a revoked schema stored and retrievable so existing resource descriptions can still refer to it (BP 2C.7.3). | none | none (retrieval operations) | SIMPL-14354/AC1, SIMPL-14701/AC1, SIMPL-32436/AC1, SIMPL-32497/AC1 | none | agreed (F-024, F-025 on what is testable) |
| New version | To create the next version of an existing schema with a new definition (BP02C.02). | `SchemaVersion` | `POST /schemas/{schemaName}/versions` | SIMPL-13357/AC1, SIMPL-32807/AC1, SIMPL-20770/AC2 | "update" (SIMPL-32807 headings, SIMPL-33694/AC5) | proposed (D-019) |

### Validation

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Syntactic validation | Check of a schema against the imposed structure and format (BP02C.04). | none (RdfValidator) | 400 `SchemaValidationBadRequestResponse` | SIMPL-14703/AC1, SIMPL-14795/AC1, SIMPL-24172/AC1 | "structural correctness" (SIMPL-14703), "syntax and integrity" (SIMPL-13353/AC1), "syntax validation" (SIMPL-14795 technical info) | proposed |
| Semantic validation | Check that the schema properties are consistent with the vocabularies they use (BP02C.04, SIMPL-22407). | none (VocabularyManager) | 400 response; `validationWarnings` on 201 | SIMPL-22407/AC1 to AC4, SIMPL-14797/AC1, SIMPL-24174/AC1 | "validation rules, relationships, and dependencies" (SIMPL-22407 notes: part of semantic validation) | agreed |
| Validation issue | One problem found during validation, with a code, a description and a location (API). | `CDM:schema-manager-be:SchemaValidationWarning` | `SchemaValidationIssue` (code, description, location) in `Problem.issues` and `validationWarnings` | SIMPL-22407/AC3, AC4, SIMPL-14795/AC1, SIMPL-14796/AC1 | "SchemaValidationWarning" (CDM), "error message" (SIMPL-14795), "structured error response", "validation error details" (SIMPL-22407), "report" (BP02C.07), "feedback" (BP 2C.5) | open (D-007, F-035, F-036) |
| Default property | A property that Simpl-Open imposes on every schema and adds before validation (BP Overview, BP02C.04); includes UUID and Version (SIMPL-26105). | none in this CDM; flowchart in `schema-manager-be-default-properties.md` | none | SIMPL-14703/AC1, SIMPL-14796/AC1, SIMPL-24173/AC1, SIMPL-26105/AC1 | "default or mandatory properties" (SIMPL-14796), "Default Properties Error" (SIMPL-24173), "Mandatory properties error" (SIMPL-14796) | open (D-011, F-006) |
| Mandatory property | A property a resource description must contain; may be default or custom (BP Overview, BP02C.01). | none | none | SIMPL-14796/AC1 | "default property" (SIMPL-14796) | open (D-011) |
| Custom property | A mandatory or recommended property defined by the Governance Authority (BP02C.01). | none | none | none | "custom recommended properties" (BP02C.01) | agreed (BP only) |

### Events and subscriptions

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Schema event | A signal raised when a schema is published or revoked (CDM). | `CDM:schema-manager-be:SchemaEvent` | `Webhook.events`, `CreateWebhookRequest.events` (enum only; no event payload) | SIMPL-19992/AC1, SIMPL-24175/AC1 | "event notifications" (SIMPL-19992 user story), "schema publishing events" (SIMPL-24175) | proposed (D-005, F-010) |
| Webhook subscription | A registration of a target URL for one or more schema event types (CDM, API). | `CDM:schema-manager-be:Webhook` | `Webhook` (webhookId, targetUrl, events), `CreateWebhookRequest` | SIMPL-19992/AC1 to AC5, SIMPL-24175/AC1 | "Webhook" (CDM, API), "subscription" (SIMPL-19992, SIMPL-24175), "active webhook subscriptions" (SIMPL-19992/AC2) | proposed (D-015) |
| Webhook id | The identifier of a webhook subscription, generated by the service (CDM). | `Webhook.webhookId` | `webhookId`; path `{webhookId}` | SIMPL-19992/AC1, AC3, AC5 | "ID" (SIMPL-19992) | agreed |
| Target URL | The http or https URL that receives the events (API, SIMPL-19992/AC4). | `Webhook.targetUrl` | `targetUrl` | SIMPL-19992/AC4 | "endpoint" (SIMPL-19992/AC1) | agreed |
| Provider notification | A message that informs Providers about a published, new-version or revoked schema (BP02C.06, "e.g. via email"). | none | none | SIMPL-14709/AC1, SIMPL-14712/AC1, SIMPL-14791/AC1, SIMPL-33694/AC1 to AC5 | "notify Providers", "e-mail" | open (D-005, F-009) |

### Metadata quality profiles (in the API and the CDM, in no story)

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Metadata quality profile | Governance template that defines quality rules, weighting and score-to-classification mapping for one resource type (CDM). | `CDM:schema-manager-be:MetadataQualityProfile` | `CreateMetadataQualityProfileResponsePayload` (metadataQualityProfileName, mqpVersion, status), `ActiveMetadataQualityProfileResponsePayload` (JSON-LD); path `{metadataQualityProfileName}` | none | "Quality profile" (API tag, path `qualityProfiles/active`, operationId `getActiveQualityProfile`), "mqp" (field `mqpVersion`), `simpl:QualityProfile` (JSON-LD type), `profileName` (CDM) | open (D-009) |
| Metadata quality profile version | Immutable version of a metadata quality profile (CDM). | `CDM:schema-manager-be:MetadataQualityProfileVersion` | `mqpVersion` only | none | `profileVersion` (CDM attribute on the profile) | open (D-009, D-010) |
| Quality dimension | A quality perspective that groups rules and adds weighted impact to the overall assessment (CDM). | `CDM:schema-manager-be:QualityDimension` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Quality rule | A quality expectation checked against a resource description, with a name and a weight (CDM). | `CDM:schema-manager-be:QualityRule` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Existence rule | Rule that requires a target property to be present (CDM). | `CDM:schema-manager-be:ExistenceRule` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Is-in rule | Rule that requires a target property value to be in an approved value set (CDM). | `CDM:schema-manager-be:IsInRule` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Custom SHACL rule | Rule expressed as a custom SHACL constraint (CDM). | `CDM:schema-manager-be:CustomShaclRule` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Threshold | Decision boundary at profile level, scoped to the overall assessment or one dimension (CDM). | `CDM:schema-manager-be:Threshold` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Score range | Score interval mapped to a classification (CDM). | `CDM:schema-manager-be:ScoreRange` | inside the JSON-LD `@graph` only | none | none | open (D-009) |
| Classification | Quality label assigned from a score range, defined by the profile (CDM). | `CDM:schema-manager-be:Classification` | inside the JSON-LD `@graph` only | none | none | open (D-009) |

### Concepts owned by other contexts

| Term | Definition | CDM class | API schema and fields | Stories and ACs | Synonyms found | Status |
|---|---|---|---|---|---|---|
| Resource description | Metadata describing a resource, created by a Provider from a schema (BP02C Overview, Outcomes). | none here; `ResourceDescription` in `catalogue-be.md` | none | SIMPL-14701/AC1, SIMPL-32436/AC1, SIMPL-32497/AC1 | "Resource Descriptions" (SIMPL-32436) | agreed (external) |
| Vocabulary | A reusable Turtle knowledge base used to check schema semantics; owned by the Vocabulary Manager (CDM note, `vocabulary-manager-be.md`). | none here; `InternalVocabulary`, `InternalVocabularyVersion` in `vocabulary-manager-be.md` | `VocabularyListItem`, `VocabularyVersionMetadata`, `VocabularyStatusUpdateRequest`, `CreateVocabularyPayload`, `CreateNewVocabularyVersionPayload`, `VocabularyVersionDetailResponsePayload` | SIMPL-22407/AC1 ("referenced vocabularies"); no story for the API operations | "internal vocabulary" (API summary, Vocabulary Manager CDM), "self managed vocabularies" (SIMPL-22407 notes), "external vocabularies" | open (D-008) |
| Billing schema | A contract clause type in a resource description, not a Schema of this component (BP Assumptions, SIMPL-28815). | none here | none | SIMPL-28815/AC1 to AC4 | "contract clause of type 'Billing Schema'" | proposed as a term to avoid here (D-018, F-044) |

## Synonyms to retire (proposed)

| Retire | Use instead | Where found |
|---|---|---|
| schema concept, schema category, SchemaCategory | schema (`Schema`) | stories (10), API (12) |
| Resource Definition Schema, resource schema, RESOURCE schema | resource description schema | SIMPL-32800, SIMPL-32802, SIMPL-32807, SIMPL-32258, SIMPL-33694 |
| Federated Search Schema (capitals), SEARCH schema (in prose) | federated search schema | SIMPL-33537, SIMPL-33694 |
| versionInfo, schemaVersion, versionNumber, mqpVersion, profileVersion | one version identifier name (open, D-002) | SIMPL-13353, API, CDM |
| unavailable (for a replaced version) | the term chosen in D-003 | CDM, API |
| SchemaValidationWarning (if it also covers errors) | validation issue | CDM |
| Quality profile, mqp, simpl:QualityProfile | metadata quality profile | API |
| update (for a new version) | new version | SIMPL-32807, SIMPL-33694 |
| governance-admin, governance-viewer | the role names chosen in F-015 | SIMPL-13343, SIMPL-13358, SIMPL-32808 |
| Simpl, SMS, the service, the system | Schema Manager (component) or Simpl-Open (platform) | many stories |

## Value sets

Per entity, the states and, per state, the name in the CDM, the API enums, the acceptance criteria and the UI.

### Schema status

| State | CDM | API enum | ACs | UI |
|---|---|---|---|---|
| PUBLISHED | `Schema.lifecycleStatus`, `SchemaMetadata.status`: PUBLISHED | `SchemaCategory.status`, `GetAllSchemasResponsePayload.metadata[].status`, `CreateNewVersionSchemaResponsePayload.status`, `UpdateSchemaResponsePayload.status`, query `status` on GET /schemas: PUBLISHED. Not allowed in `SchemaStatusUpdateRequest.status` (only REVOKED) | SIMPL-20686/AC1, AC3; SIMPL-32801/AC1, AC3 to AC5; SIMPL-20529/AC1 (as PATCH target, see F-002); SIMPL-13360/AC1 (Given); "PUBLISH" in SIMPL-20530/AC2 to AC4 (F-043) | Status column "PUBLISHED" (SIMPL-13361, SIMPL-20530); button "Publish"; message "Schema {schema_name} published successfully!" (SIMPL-20530 technical details) or "Schema state changed." (SIMPL-20530/AC2) |
| REVOKED | `Schema.lifecycleStatus`, `SchemaMetadata.status`: REVOKED | same schemas: REVOKED; `SchemaStatusUpdateRequest.status`: REVOKED | SIMPL-13360/AC1; SIMPL-20529/AC2; SIMPL-20686/AC2; SIMPL-32801/AC2; SIMPL-33537/AC5 | Status column "REVOKED"; button "Revoke"; message "Schema "{schema_name}" revoked successfully." (SIMPL-13361/AC1) or "Schema state changed." (SIMPL-20530/AC1) |
| (invalid examples) | none | none | ARCHIVED (SIMPL-13360/AC4), INVALID_STATUS (SIMPL-13343, SIMPL-32258): negative tests only | none |

The mapping from value to UI text is written in the stories for the status column (same word) but the success texts differ (F-029, F-043).

### Schema version status

| State | CDM | API enum | ACs | UI |
|---|---|---|---|---|
| PUBLISHED | `SchemaVersion.lifecycleStatus`: PUBLISHED | `CreateNewVersionSchemaResponsePayload.status`; the versions list example has no status (F-004) | SIMPL-13357/AC1, SIMPL-32807/AC1 to AC3 ("the new schema version has status PUBLISHED") | "Status" column in the versions table (SIMPL-13346/AC1, SIMPL-32787/AC1) |
| REVOKED | `SchemaVersion.lifecycleStatus`: REVOKED | as above | SIMPL-13357/AC1, SIMPL-32807/AC1 to AC3 (previous version "changed from PUBLISHED to REVOKED") | as above |
| replaced ("unavailable") | text only: "implicit *unavailable* status" (relationship notes) | text only: POST /schemas/{schemaName}/versions summary | none | none |

Open: D-003 and F-004 (does a version have its own status, and what is a replaced version called).

### Schema type

| Value | CDM | API enum | ACs | UI |
|---|---|---|---|---|
| RESOURCE | `Schema.schemaType`, `SchemaMetadata.schemaType`: RESOURCE | `schemaType` (default RESOURCE) on CreateSchemaPayload, CreateNewSchemaVersionPayload, SchemaCategory, list items, responses, query parameters | SIMPL-32258/AC1, SIMPL-32801/AC1 to AC3, SIMPL-32802/AC1 | selection "RESOURCE" (default), SIMPL-32790 technical info, SIMPL-32800/AC1 |
| SEARCH | `Schema.schemaType`: SEARCH | same | SIMPL-32258/AC2, SIMPL-32801/AC4 to AC6, SIMPL-33537/AC1 to AC5 | selection "SEARCH"; resourceType field disabled (SIMPL-32800/AC2) |

Agreed in all sources.

### Resource type

| Value | CDM | API | ACs | UI |
|---|---|---|---|---|
| open set | "open value set ... PascalCase ... e.g. *Application*, *DataOffering*" | pattern `^[A-Z][a-zA-Z0-9]*$`; examples Application, LegalPerson | `data`, `application`, `infrastructure` (SIMPL-13355/AC1, SIMPL-32800/AC1, SIMPL-15837/AC2); `data-schema` (SIMPL-13343/AC2); `APP`, `INFRA`, `DATA` (SIMPL-32802 technical info) | options "data, infrastructure, application" (SIMPL-13355/AC1); no label-to-value mapping |

Open: D-012, F-013.

### Schema event type (SchemaEvent, Webhook)

| Value | CDM | API enum | ACs | UI |
|---|---|---|---|---|
| SchemaPublished | `SchemaEvent.eventType`, `Webhook.events` | `Webhook.events`, `CreateWebhookRequest.events` | SIMPL-19992/AC1 | none |
| SchemaRevoked | same | same | SIMPL-19992/AC1 | none |
| (invalid example) | none | none | SchemaExploded (SIMPL-19992/AC4) | none |

Agreed values; which actions raise them is open (D-005, F-010).

### Webhook subscription state

| State | CDM | API enum | ACs | UI |
|---|---|---|---|---|
| active | none | none | SIMPL-19992/AC2 ("all active webhook subscriptions") | none |

Open: D-015.

### Metadata quality profile status

| State | CDM | API enum | ACs | UI |
|---|---|---|---|---|
| PUBLISHED | `MetadataQualityProfile.lifecycleStatus`, `MetadataQualityProfileVersion.lifecycleStatus` | `MetadataQualityProfileStatus` | none | none |
| REVOKED | same; "soft-delete" by DELETE | same; DELETE /metadataQualityProfiles/{metadataQualityProfileName} also sets REVOKED | none | none |

No AC uses these values (D-009).

### Threshold scope and classification (CDM only)

| Value set | CDM | API | ACs | UI |
|---|---|---|---|---|
| `Threshold.classificationScope` | overall, dimension | inside JSON-LD only | none | none |
| `Classification.classificationName` | profile-defined, e.g. Green/Yellow/Red or Poor/Medium/Good | inside JSON-LD only | none | none |

### Vocabulary status (concept owned by the Vocabulary Manager)

| State | Schema Manager CDM | API enum | ACs | UI |
|---|---|---|---|---|
| ACTIVE | none (Vocabulary Manager CDM: InternalVocabulary.status, InternalVocabularyVersion.status) | `VocabularyListItem.status`, `VocabularyVersionMetadata.status`, `VocabularyStatusUpdateRequest.status`, query `status` on GET /vocabularies | none | none |
| DEPRECATED | none (Vocabulary Manager CDM: same) | same | none | none |

Open: D-008.
