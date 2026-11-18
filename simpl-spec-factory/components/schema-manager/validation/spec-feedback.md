# Spec feedback: Schema Manager

Step 29 of the spec factory, assembled on 18 Nov 2026 by `spec-factory/pipeline/build_feedback.py` (engine c14ceb7). For the owners of the stories, the API definitions, the CDMs and the business process. Every point cites the ids it is about and the question that needs an answer; the full evidence is in the linked files.

**What this is.** An automated reading of the published specifications, as a builder would read them, before anything is built. A finding says where two careful readers would build different things, or where a builder would have to guess. It is not a judgement on the people who wrote the specs: most points are normal for specifications written by several teams over time.

## In short

- **Input:** 55 stories with 210 live acceptance criteria, 18 API operations, 27 business process requirements, and the CDMs of each API.
- **Findings:** 118: **17 to decide before building**, 60 to confirm, 41 small.
- **Mechanical checks:** 110 hits on the acceptance criteria and 61 on the API and CDM names; the agents triaged them, and the real ones are part of the findings above.
- **Coverage:** 19 of 210 criteria link to no operation and no process requirement; 9 of 18 operations and 27 of 27 process requirements have no criterion.

## Decide first

The blockers. A builder or tester cannot proceed on these without a decision, and two builders would decide differently.

### Behaviour and interactions

**F-001** (AC-C04). The stories use two different version identifiers. Older stories expect a SemVer string supplied by the client or set to 1.0.0; newer stories and the OpenAPI expect a server-generated integer. Tests and code built from different stories will disagree on the version value, the request body and the 409 and 400 cases.

- Where: cross
- Question: Is the version a server-generated integer string ("1", "2", ...) everywhere, and are the SemVer criteria (client-supplied version, 409 on a duplicate version, 400 on a bad version format, the SemVer field in the UI) withdrawn?
  - Option: Server-generated integer per the OpenAPI and SIMPL-20528/20534/20770; SemVer criteria are obsolete
  - Option: SemVer (generated or client-supplied) per SIMPL-13353/13357/13358 and the VersioningContext example; the OpenAPI changes

**F-002** (AC-C05). Several criteria publish a schema by sending PATCH with status PUBLISHED. The OpenAPI allows only REVOKED in this request, so a builder following the contract rejects what these criteria require. One criterion also expects 201 Created from the PATCH.

- Where: cross
- Question: Can a REVOKED schema be set back to PUBLISHED through PATCH /schemas/{schemaName}, and if so with which response code and body?
  - Option: Yes: extend the OpenAPI enum with PUBLISHED, response 200 with SchemaCategory
  - Option: No: re-publication is not supported; SIMPL-20529/AC1, SIMPL-20530/AC2 to AC4 and SIMPL-31098/AC2 are withdrawn

**F-003** (AC-C04). The stories disagree on what status a newly created schema gets. Some stories store a new schema as REVOKED when another schema of the same resource type is published; the business process publishes every schema that passes validation, and the notification stories notify on every new schema.

- Where: cross
- Question: When a new schema is created for a resource type that already has a PUBLISHED schema, is it stored as REVOKED (and are Providers then not notified), is it rejected with 409, or is it published and the other one revoked?
  - Option: Stored as REVOKED, no notification (SIMPL-20686/32801)
  - Option: Rejected with 409 (one schema per resource type, BP02C overview)
  - Option: Published, and the previously published schema is revoked (BP02C.05 reading)

**F-004** (AC-C04). It is not clear whether status belongs to a schema version or to the schema concept. Two stories set the previous version to REVOKED when a new version is created; another story says versions have no status of their own. The UI stories show a status per version, but the versions response in the OpenAPI example has no status field.

- Where: cross
- Question: Does each version carry its own status (previous version becomes REVOKED), or does a version inherit the status of its schema? What does a revoked schema's version list show?
  - Option: Status per version, returned in the versions list
  - Option: Status per schema concept only; versions inherit it and the version-level status criteria are reworded

**F-005** (AC-C05). Retrieving the latest content of a schema uses GET /schemas/{schemaName}, which the OpenAPI does not offer. The stories also call it a Resolver Interface that is 'not a REST API', use another host, and say 'public' in the user story but 'authenticated' in the criteria. A builder cannot tell whether this endpoint belongs to this service and how it is secured.

- Where: cross
- Question: Is GET /schemas/{schemaName} (latest content, text/turtle, Content-Location header) part of this service's API? If yes, under which base path, with which authentication and roles, and will the OpenAPI be extended?
  - Option: Part of this service: add GET /schemas/{schemaName} to the OpenAPI, same OAuth2 and roles as other reads
  - Option: A separate public Resolver component: out of scope for this build, the UI download uses GET /schemas/{schemaName}/{version} with the latest version

**F-006** (AC-C04). The business process says Simpl-Open adds the default properties to a schema before validating it. The stories instead reject a schema that does not already contain them. The list of default properties is only in external tickets, so neither reading can be tested from the specs.

- Where: cross
- Question: Does Simpl-Open add missing default properties to a submitted schema, or reject the schema? Which exact properties (IRIs, constraints) are the default properties, including UUID and Version, per resource type?
  - Option: Enrich: Simpl-Open adds them; a schema without them is accepted
  - Option: Reject: the Governance Authority must include them; missing ones give 400 with a reason

**F-007** (AC-C04). The stories do not agree on what makes a schema name unique. This decides when POST returns 409 and which schema PATCH /schemas/{schemaName} and GET by name address.

- Where: cross
- Question: What is the uniqueness key of a schema: name, name plus schemaType, or name plus schemaType plus resourceType? Which resource is addressed when a request names only the schemaName?
  - Option: name only
  - Option: name + schemaType
  - Option: name + schemaType + resourceType (three RESOURCE schemas may share a name)

**F-008** (AC-C03). The rule 'only one federated search schema' has two readings: any SEARCH schema, or only a published one. Three stories leave open, in their own notes, whether a new schema may reuse a revoked schema's name, and the way to publish a SEARCH schema again after revocation differs between stories.

- Where: cross
- Question: Does 409 apply when any SEARCH schema exists, or only when a PUBLISHED one exists? May a new SEARCH schema reuse a revoked SEARCH schema's name? After revocation, is a SEARCH schema published again by creating a new schema, a new version, or by PATCH?
  - Option: Only a PUBLISHED SEARCH schema blocks creation (SIMPL-33537 scope); a revoked name may not be reused
  - Option: Any SEARCH schema blocks creation (SIMPL-31098/AC3, SIMPL-32801/AC6)

**F-009** (AC-C03). Provider notifications have no defined recipients or channel. The criteria say 'all Providers', the technical notes say one e-mail address from configuration, only some criteria say e-mail, and a separate story offers webhooks. A tester cannot check that 'Providers' were notified.

- Where: cross
- Question: Who receives the notification (one configured mailbox, every Provider known to the data space, or webhook subscribers), through which channel, and how does a test observe it?
  - Option: One e-mail to a configured address (technical notes)
  - Option: One e-mail per Provider from a participant directory
  - Option: Webhook events only (SIMPL-19992)

**F-011** (AC-C03). The user story asks for the content of a schema version, but both positive criteria expect 'schema meta datas'. The OpenAPI returns the Turtle content. A builder and a tester will read this differently.

- Where: SIMPL-32798/AC3
- Question: Does GET /schemas/{schemaName}/{version} return the Turtle content (as the OpenAPI says) or the version metadata as JSON?
  - Option: Turtle content per the OpenAPI; 'meta datas' is a wording slip
  - Option: JSON metadata; the OpenAPI changes

**F-012** (AC-C03). This negative criterion contradicts itself: the Given says the schema exists for schema type SEARCH, the request asks for SEARCH, yet the expected result is 404. Its heading says the opposite case was meant.

- Where: SIMPL-32807/AC6
- Question: Is the case 'name exists as RESOURCE only, request with schemaType SEARCH, expect 404'? And is the 'version number that already exists' line still relevant now that versions are server-generated?
  - Option: Name exists as RESOURCE only, request SEARCH, 404 (heading reading)
  - Option: Name exists as SEARCH, request SEARCH, 404 (as written)

**F-013** (AC-C05). The resourceType values in the criteria do not match the OpenAPI pattern. The stories use lowercase or hyphenated values; the OpenAPI requires PascalCase alphanumeric. A request built from the story would be rejected with 400 by a service built from the contract.

- Where: cross
- Question: Which resourceType values are valid (exact spelling and case), is the set closed (data, application, infrastructure) or free text, and does the API compare them case-sensitively?
  - Option: Closed set in PascalCase: Data, Application, Infrastructure
  - Option: Free PascalCase string per the OpenAPI pattern; the UI offers three fixed values

### One language for the domain

**D-003** (DDD-04). The state of a version that a newer version replaces has three names. The business process and the stories call it REVOKED. The CDM and the API call it 'unavailable' for new resource descriptions. One story says a version has no status of its own. A builder cannot tell whether 'superseded' is a value of the status set or a different concept.

- Where: cross
- Question: Is a replaced version REVOKED (same value as a revoked schema), a separate state such as SUPERSEDED, or not a state at all because only the schema carries a status?
  - Option: REVOKED: one value set for schema and version; drop 'unavailable' from the CDM and the API text
  - Option: A distinct value (for example SUPERSEDED) added to SchemaVersion.lifecycleStatus and the API enum
  - Option: No version status: only Schema.lifecycleStatus; 'latest version' decides what Providers may use

### Entities, identities and states

**D-001** (DDD-09). The identity of a schema has two live names in the API and the stories: 'schemaName' and 'name'. The create request and every list or detail response call it 'name'; the path, the create response and the CDM call it 'schemaName'. The CDM also says 'schemaName identifies the schema', while the API and the stories address a schema by name plus schemaType and sometimes resourceType. A test writer and a code writer will each pick one field name and one key.

- Where: cross
- Question: Which single name does the schema identity carry in requests, responses and the CDM, and does the CDM record the full identity key of a schema?
  - Option: 'schemaName' everywhere (path, CDM, create response); rename the 'name' fields in the payloads
  - Option: 'name' in bodies and 'schemaName' only as the path parameter, written down in the glossary as the same identity
  - Option: Keep both, but add the key (name, schemaType, resourceType) to the CDM as the schema identity

**D-002** (DDD-09). The identifier of a schema version has five names: 'version', 'schemaVersion', 'latestVersion', 'versionNumber' and 'versionInfo'. The metadata quality profile adds 'mqpVersion' and 'profileVersion'. The same create flow returns 'schemaVersion' for a new schema and 'version' for a new version.

- Where: cross
- Question: Which one name does the version identifier carry in the API and the CDM, for schemas and for metadata quality profiles?
  - Option: 'version' in the API and 'version' in the CDM (rename versionNumber, schemaVersion, mqpVersion)
  - Option: 'versionNumber' everywhere, matching the CDM and the stories' 'version number'
  - Option: Keep 'latestVersion' only as a derived pointer on the schema, documented as such

### Error cases and testability

**F-010** (AC-C01). The story covers creating, listing and deleting webhook subscriptions, but no criterion and no part of the OpenAPI describes the event that is delivered to the target URL. A builder has to invent the payload, the trigger points and the failure handling; the subscribers depend on exactly these.

- Where: SIMPL-19992
- Question: What does the HTTP POST to the target URL contain, which actions raise SchemaPublished (new schema, new version, re-publication) and SchemaRevoked (revocation, implicit revocation of a previous version), and what happens when delivery fails (retries, timeout)?

### Business process flow

**B-001** (FLOW). Step BP02C.05 says that a schema or version is published whenever validation succeeds, and the Overview says each resource type has one schema. The decided lifecycle has three outcomes the process never reaches: a new RESOURCE schema stored as REVOKED when its resource type already has a published one, a SEARCH create or new version refused with 409 after validation passed, and a REVOKED schema published again by a new version. No decision says that the decisions win over the business process text (D1 covers stories, D44 the OpenAPI), so a builder who follows BP02C.05 publishes every valid schema and a builder who follows D13 does not.

- Where: BP02C.05
- Question: Process owner: is the D13 lifecycle the process (one published schema per scope, create may store REVOKED, a new version may re-publish a revoked schema, SEARCH conflicts end in 409)? Then BP02C.05, the Overview and BP02C.03 need the same wording. For the build: may step 30 add one decision that the decisions win over BP02C text wherever they differ (.04 enrich, .05 publish, Overview 'one schema')?
  - Option: Decisions win over BP02C; process owner aligns BP02C.03, .04, .05 and the Overview to D7, D8, D12, D13 and D19
  - Option: BP02C wins: every valid schema is published; D7 and D12 are reopened

## Terms to agree

The draft glossary ([`glossary.md`](./glossary.md)) has 54 terms: 13 already used the same way everywhere, 16 where the draft proposes one term among synonyms, and 25 where the sources disagree on the meaning. One agreed term per concept, used the same way in the stories, the API and the CDM, removes a large part of the findings above.

Open: Schema admin role, Schema viewer role, Schema name, Resource type, Version, Title, Description, Named graph, Publish, Superseded version, Validation issue, Default property, Mandatory property, Provider notification, Metadata quality profile, Metadata quality profile version, Quality dimension, Quality rule, Existence rule, Is-in rule, Custom SHACL rule, Threshold, Score range, Classification, Vocabulary.

## To confirm

The majors: the build can proceed on a sensible default, but the default should be confirmed.

| Id | Theme | Where | Finding | Question |
|---|---|---|---|---|
| F-014 | Behaviour and interactions | cross | Several criteria expect 401 Unauthorized for a missing or invalid token, but no schema or webhook operation in the OpenAPI declares 401. The OpenAPI defines an UnauthorizedErrorResponse but uses it only on the Metadata Quality Profile operations. | Does this service return 401 itself for a missing or invalid token (and should the OpenAPI list it on every operation), or is 401 produced by the gateway in front of it? |
| F-015 | Behaviour and interactions | cross | Role names differ between stories. Most use GA_SCHEMA_ADMIN and GA_SCHEMA_VIEWER; three criteria use governance-admin and governance-viewer. A builder must know which role names to check in the token. | Are governance-admin and governance-viewer the same roles as GA_SCHEMA_ADMIN and GA_SCHEMA_VIEWER, and which exact role names appear in the token? |
| F-017 | Behaviour and interactions | cross | The UI tables show columns that the API does not return. The schema list has no creation date and no creator; the versions list has no status, resource type or creator. | Should the API add creator and creation date to the schema list, and status, resourceType and creator to the versions list, or should the UI drop these columns? |
| F-018 | Behaviour and interactions | cross | The UI lists must be sortable, but the schema and version endpoints have no sort parameter and return pages. Sorting can mean the current page only or the whole result set. | Does sorting apply to the whole result (needs a sort parameter in the API) or only to the rows of the current page? Which columns exactly are sortable? |
| F-019 | Behaviour and interactions | cross | The create criteria describe the request and the Location header differently from the OpenAPI. The stories send 'JSON metadata' as one multipart part and point Location to the schema concept; the OpenAPI has separate form fields and its Location example points to a version URI with a UUID. | Is the request body flat form fields as in the OpenAPI? What exactly is the Location URI: the schema concept by name, or a version URI, and with a name or a UUID? |
| F-020 | Behaviour and interactions | cross | The new-version criteria describe a 'metadata' part and a 'file' part, and in SIMPL-32807 schemaType appears both as its own field and inside the metadata part. The OpenAPI has flat fields schemaFile, changelog, resourceType and schemaType, with the schema name only in the path. | Is the version request body exactly the OpenAPI CreateNewSchemaVersionPayload (flat fields, no schemaName in the body)? If schemaType is sent and does not match the stored schema, what is the response? |
| F-022 | Behaviour and interactions | SIMPL-20529/AC7 | This criterion expects 409 when the requested status equals the current one, but the OpenAPI for PATCH /schemas/{schemaName} has no 409 response, and the revoke story is silent on revoking an already revoked schema. | Is revoking an already revoked schema a 409 (add it to the OpenAPI) or an idempotent 200? |
| F-023 | Behaviour and interactions | cross | The content returned for a version is called 'the complete, raw SHACL content', and creation stores 'the submitted SHACL file'. If Simpl-Open enriches the schema with default properties, the returned content differs from the uploaded file. A test cannot compare without knowing which one is stored. | Is the content returned by the retrieval endpoints byte-identical to the uploaded file, or the stored (possibly enriched or normalised) graph? What may a test compare? |
| F-024 | Behaviour and interactions | cross | The negative retrieval criteria return 404 for a version that 'has not been published'. A revoked version was published once; the retention stories require revoked schemas to stay retrievable. It is open whether a revoked version gives 200 or 404. | Do the retrieval endpoints return revoked versions (200) and return 404 only for versions that do not exist? |
| F-027 | Behaviour and interactions | cross | The expected error 'detail' texts differ between stories and from the OpenAPI. The same failure (missing default properties) has two different prefixes, and no API criterion names the status code. | Which status code and which detail text (or issues[].code) identify a syntax error, a missing default or mandatory property, and a semantic error? Is the detail prefix part of the contract that tests may assert? |
| F-029 | Behaviour and interactions | cross | Two UI stories describe revocation differently: one with a confirmation popup and one success text, the other without a popup and with another success text. | Is there a confirmation popup before revoking, and which success text is shown? |
| F-031 | Behaviour and interactions | cross | SIMPL-13343 asks for a list of all schemas, but since the schema type was introduced, GET /schemas without a filter returns only RESOURCE schemas. A test written from SIMPL-13343 may expect SEARCH schemas in the list. | Confirm that GET /schemas without schemaType returns only RESOURCE schemas, and that there is no way to list both types in one call. |
| F-033 | Behaviour and interactions | SIMPL-13364 | The story names no endpoint, no role and no content type, and its note says it is 'not a REST API'. The OpenAPI offers GET /schemas/{schemaName}/{version} returning text/turtle, so it is unclear whether this story is that operation or the Resolver. | Is this story implemented by GET /schemas/{schemaName}/{version}? Which roles may call it, and is 403 expected without one? |
| F-035 | Behaviour and interactions | SIMPL-22407 | The criteria say any failed semantic check blocks creation. The story notes and the OpenAPI also have non-blocking warnings. A builder cannot tell which checks block and which only warn, nor against which vocabularies the schema is checked. | Which semantic issues are errors (400, nothing created) and which are warnings (201 with validationWarnings)? Which vocabularies count as 'self managed' (the /vocabularies of this service)? |
| F-036 | Behaviour and interactions | SIMPL-22407/AC3 | The fields each semantic error must carry do not match the OpenAPI issue object, and the status code is not named. | Map the four required items to the OpenAPI issue fields (code, description, location), or extend the issue object. Is the status 400? |
| F-038 | Behaviour and interactions | SIMPL-17066/AC4 | There is no 'publish a schema' operation to protect. Publication happens on creation, and the only status change in the OpenAPI is revocation. | Which request is the 'publish a schema' action? If none, are AC4 and AC10 covered by the create criteria AC1, AC2, AC7, AC8? |
| F-039 | Behaviour and interactions | SIMPL-17066/AC5 | 'List schema details' matches no single operation. It can mean the schema list, the content of a version, or the latest content. | Which operations does 'list schema details' cover, and do GA_SCHEMA_VIEWER rules also apply to webhooks and to SEARCH schemas? |
| F-041 | Behaviour and interactions | SIMPL-19992/AC1 | 'The system validates the endpoint URL' can mean a format check or a reachability check (calling the URL). | Is validation a syntax check only (http or https absolute URL), or must the service also reach the URL before accepting it? |
| F-042 | Behaviour and interactions | SIMPL-19992/AC4 | For an unknown event type the story gives no status code, and the OpenAPI returns 406 Not Acceptable for it, which is an unusual code for a body validation error. A test cannot assert the code. | Is an unknown event type answered with 406 (as in the OpenAPI) or 400 like the other body errors? |
| F-043 | Behaviour and interactions | SIMPL-20530/AC2 | After a successful Publish the criterion expects the status to become REVOKED, sends 'PUBLISH' instead of 'PUBLISHED', and gives a success text that differs from the technical details. | Confirm: body value PUBLISHED, status shown PUBLISHED, and which success text. |
| F-046 | Behaviour and interactions | SIMPL-32797 | The download criteria point to the versions list endpoint, which returns metadata, not the file. The file content comes from GET /schemas/{schemaName}/{version}. | Confirm that the download uses GET /schemas/{schemaName}/{version}?schemaType=... and give the expected file name and extension. |
| F-047 | Behaviour and interactions | SIMPL-32800/AC1 | The UI must send 'schemaName, schemaType, title, description and file' but not resourceType, although the user selects it and the status rules depend on it. The OpenAPI field is 'name', not 'schemaName'. | Confirm that the UI sends resourceType for RESOURCE schemas and uses the OpenAPI field names (name, schemaFile). |
| F-049 | Behaviour and interactions | SIMPL-32808/AC2 | The new-version form lets the user choose a schemaType, although the version belongs to an existing schema whose type is known. The criteria do not say what happens when the chosen type differs from the schema's type. | Should the form take the schemaType from the schema being viewed (no free choice), or keep a selection box, and what happens on a mismatch? |
| F-050 | Behaviour and interactions | SIMPL-32808/AC5 | This criterion expects 409 when no former version exists for the chosen schemaType; the API story expects 404 for the same case. | Is the response 404 (schema of that type not found) as in SIMPL-32807, and which case gives 409 on this endpoint? |
| F-051 | Behaviour and interactions | SIMPL-33537/AC1 | SEARCH schemas must be validated against 'the rules and default properties for SEARCH', which no spec defines. | Which default properties and validation rules apply to SEARCH schemas, and do they differ from RESOURCE? |
| D-005 | One language for the domain | cross | The word 'publish' names four different things: a new schema that passes validation, a new version that passes validation, a status change from REVOKED to PUBLISHED, and the SchemaPublished event. The CDM says version creation raises no event, while the business process and the notification stories treat a new version as 'published'. Provider notification by e-mail and the webhook event are two concepts, but only the event is in the CDM. | Which actions count as 'publish' in this component, and does the CDM need a concept for the Provider notification next to SchemaEvent? |
| D-007 | One language for the domain | SchemaValidationWarning | One API object, SchemaValidationIssue, carries both blocking errors (inside the 400 problem response) and non-blocking warnings (in the 201 response). The CDM calls it SchemaValidationWarning, which suggests warnings only. The stories call it 'error message', 'structured error response' or 'validation error details', and the business process calls the outcome a 'report' and 'feedback'. The issue has no severity field to tell the two apart. | Is a validation issue one concept with a severity (error or warning), and what is its one name in the CDM, the API and the stories? |
| D-009 | One language for the domain | MetadataQualityProfile | Metadata quality profiles are in the API and in the CDM (ten classes), but in no story and no process step, so the specs give no language to agree on. Inside the API the concept has four names: 'Metadata Quality Profile', 'Quality profile', 'mqp' and the JSON-LD type 'simpl:QualityProfile'. Its fields differ from the CDM, and the status update returns a schema named for creation. | Is metadata quality profile management in the scope of this component's specs, and if so which one name and which field names does it use? |
| D-011 | One language for the domain | cross | 'Default property' and 'mandatory property' are used as if they were one concept, while the business process separates them: default properties are imposed by Simpl-Open, and both default and custom properties can be mandatory or recommended. The main CDM has no class for either; the only model of default properties is a flowchart that is not in the CDM register. | Are 'default property' and 'mandatory property' two concepts (who defines it, and whether it is required), and should the CDM name them? |
| D-012 | One language for the domain | cross | The resource type of a schema has four value spellings, and a fifth term for what may be the same set. The stories and the UI use data, application, infrastructure; one story uses APP, INFRA, DATA; the CDM and the API use an open set of PascalCase names (Application, LegalPerson); the default-properties model and the catalogue CDM call the set offeringType with application, data, infrastructure. No mapping from UI label to stored value is written down. | Is the schema's resourceType a closed set equal to the resource description's offeringType, or an open set of names, and what is the written mapping from UI label to stored value? |
| D-004 | Entities, identities and states | Schema | The schema lifecycle is not defined in one place. The CDM gives only the value set PUBLISHED, REVOKED and no transitions. The transitions are spread over stories, the API and the business process, and they disagree: creation may give PUBLISHED or REVOKED, PATCH allows only REVOKED in the API but PUBLISHED in three stories, and the CDM says every POST /schemas 'generates SchemaPublished event'. | Can the spec owners add one state and transition table for Schema (and for SchemaVersion, see D-003) next to the CDM, with the operation and the event for each transition? |
| D-006 | Entities, identities and states | Schema | The aggregate the CDM calls Schema is called 'schema category' in the API and 'schema concept' in the stories. 'Category' appears in no story, in no process step and in no CDM. A builder could read 'category' as a grouping of schemas (for example per resource type), which is a different concept. | Can the API rename SchemaCategory to Schema and drop 'category' from its descriptions, and can the stories drop 'schema concept' for 'schema'? |
| D-008 | Boundaries with other components | Vocabulary | The API offers six vocabulary operations that manage a concept owned by the Vocabulary Manager. The Schema Manager CDM calls them legacy and does not model Vocabulary at all; the Vocabulary Manager CDM names the concept InternalVocabulary and InternalVocabularyVersion. The API does not mark the operations as deprecated, no story covers them, and the status request adds 'reason' and 'effectiveDate', which the Vocabulary Manager CDM lists as an open question. | Are the /vocabularies operations part of the Schema Manager contract to build and test, or should they be marked deprecated or removed in favour of the Vocabulary Manager? |
| F-016 | Error cases and testability | cross | List criteria require 'all the relevant metadata' or 'all its metadata' without naming the fields. For the versions list the OpenAPI response schema is a reference to a file that is not in the specs, so the field list is only known from an example. | Which fields must each list item contain, for the schema list and for the versions list? Is the OpenAPI GetAllSchemasResponsePayload and the versions example (name, graphName, version, changelog, created, schemaType) the full list? |
| F-021 | Error cases and testability | cross | No criterion says what happens when a new version is posted for a REVOKED schema. The revocation story says revocation prevents any version from being used, and the status stories give versions their own status, so the builder has to guess between 201 (and re-publication) and an error. | What does POST /schemas/{schemaName}/versions return for a REVOKED schema: 201 with the new version PUBLISHED (re-publishing the schema), 201 with the version REVOKED, or an error (which code)? |
| F-026 | Error cases and testability | cross | The notification content cannot be checked: 'links for instructions', 'relevant instructions or links' and 'suggested follow-up actions' are not defined, and the revocation criterion of the newer story gives no content at all. | What exactly does each notification contain (fields, link targets, fixed text), for a new schema, a new version and a revocation? |
| F-028 | Error cases and testability | cross | The UI validation criteria require 'an error message about the schema validation failure and the reason' without saying where it appears or what it shows. Other UI stories describe the error location and text in different ways. | Where does the UI show validation errors (top of the form, toast, inline), and does it show the Problem detail, the issues list, or a fixed text? |
| F-032 | Error cases and testability | SIMPL-13355/AC2 | The negative scenario has the same Given and When as the positive one (a new name and a valid file), so the error is never triggered. Its only expected outcome is in a bullet after the Then. | Which inputs trigger the error case (invalid file, existing name), and which message is shown for each status? |
| F-034 | Error cases and testability | SIMPL-14354/AC1 | The criterion does not say which request is used, what 'identifier' means, or which fields form 'the schema and its properties'. A tester has no oracle. | Which operation and identifier (name, name plus version) are meant, and what must the response contain for a revoked schema? |
| F-037 | Error cases and testability | SIMPL-24175 | The outcomes 'handles the request' and 'does not handle the request' have no observable result, and the technical info says the check happens in the Tier 2 gateway because the service has no ingress. It is unclear what this component must do and how a test observes it. | Which status codes are returned without Tier 2 credentials and without authentication, and is this enforced by the gateway (out of scope here) or by the service? |
| F-040 | Error cases and testability | SIMPL-17066 | The negative criteria say Simpl 'does not authorise ... and does not allow to perform it' without a status code or a UI behaviour. | Confirm 403 Forbidden with a Problem body for every denied action, and say what the UI does (hide the action or show an error). |
| F-045 | Error cases and testability | SIMPL-32790/AC2 | The UI may combine a schemaType SEARCH filter with a resourceType filter, which the API rejects with 400. The criterion does not say how the UI prevents or shows this. | When SEARCH is selected, does the UI disable and clear the resourceType filter, or show the 400 error? |
| F-048 | Error cases and testability | SIMPL-32802 | The technical info makes resourceType mandatory for RESOURCE schemas, but the OpenAPI marks it optional and no criterion covers a RESOURCE schema without resourceType. | What does POST /schemas return for a RESOURCE schema without resourceType: 400, or 201 with no resource type? |
| D-010 | Data model governance | schema-manager-be | A CDM exists next to the API, but it describes another revision of it and is not approved. Its endpoint table lists four metadata quality profile operations the frozen API does not have, it names two gateway APIs that are not in the specs, and it is 'In review, approved No' in the register. The versions response of the API points to a file outside the specs, so the CDM cannot be checked against it. | Can the CDM be aligned with the API revision the factory builds (or the API with the CDM), and approved, before step 40 uses the glossary? |
| P-001 | API guidelines and coverage | schema-manager-be (specs/api/schema_openapi.yaml) | The security scheme does not fit the decided authentication and roles. The OpenAPI declares an OAuth2 authorizationCode flow 'provided by gateway' with placeholder URLs and two scopes, read and write, that no operation requests. The decisions say the service itself validates an OIDC JWT bearer token and authorises on realm roles GA_SCHEMA_ADMIN and GA_SCHEMA_VIEWER. No in-scope operation names the role it needs, and the webhook operations list 403 although D28 grants them to any authenticated caller. A builder who follows the OpenAPI may leave token checks to a gateway or enforce scopes; one who follows the decisions checks roles in the service. | Should the OpenAPI declare an openIdConnect (or http bearer JWT) scheme without scopes, state the required role per operation (for example in the description or an x-required-roles extension), drop 'provided by gateway', and remove 403 from the webhook operations? |
| P-002 | API guidelines and coverage | schema-manager-be (specs/api/schema_openapi.yaml) | Any authenticated participant can register any http or https URL, and the service then POSTs event data to it. D29 checks the URL syntax only and D28 requires no role. This is a server-side request forgery path (internal hosts, cloud metadata addresses, localhost) and conflicts with the MUST rule that outgoing requests to URLs taken from input are checked against an allow list. Plain http is also accepted for event delivery. A builder who applies OWASP-11 adds an allow list and a 400 for other hosts; a builder who follows D29 does not. The two give different results on the same test. | Should D29 add a configured allow list of target hosts (for example WEBHOOK_ALLOWED_HOSTS, with the local test receiver of D43 on it), reject other hosts with 400, and decide whether plain http stays allowed outside tests? |
| P-003 | API guidelines and coverage | schema-manager-be (specs/api/schema_openapi.yaml) | The same 'not found' case has different problem types across operations. GET /schemas/{schemaName}/{version} answers urn:problem-type:iana:notFound with title 'Not Found', which is not a Simpl problem type; the shared SchemaNotFoundResponse, used by PATCH and the versions operations, answers urn:problem-type:simpl:notFound with title 'Resource Not Found'. The new GET /schemas/{schemaName} of D16 has no example at all. A client that branches on type sees two values for one case, and tests copied from the examples assert different types per operation. | Is urn:problem-type:simpl:notFound with title 'Resource Not Found' the one 404 type for every schema and webhook operation, including GET /schemas/{schemaName} (D16)? |
| P-004 | API guidelines and coverage | schema-manager-be (specs/api/schema_openapi.yaml) | The OpenAPI schemas accept values the decisions forbid. resourceType is an open PascalCase pattern in six places, while D6 makes it a closed set of Data, Application and Infrastructure with 400 for anything else; a service that validates against the OpenAPI accepts LegalPerson. The version path parameter of GET /schemas/{schemaName}/{version} is any non-empty string, while D3 makes it a whole number written as a string; its own 404 example uses '9.9.9'. A builder can answer 'abc' or '1.0.0' with 400 or with 404, and both fit the contract. The SEARCH plus resourceType rule of D4 is stated only on GET /schemas. | Add enum [Data, Application, Infrastructure] to every resourceType, pattern '^[1-9][0-9]*$' to the version path parameter, and decide whether a version that is not a whole number gives 400 or 404? |
| F-025 | Scope | cross | Four retention criteria are about resource descriptions in the catalogue and about federated search, which live outside the schema manager. The outcome cannot be observed through this component's API, and one story says the function already exists. | For the schema manager, is the testable requirement only that a revoked schema and its versions stay retrievable by name and version? Who owns the catalogue and federated search checks? |
| F-030 | Scope | cross | Eleven [ENHANCE] stories, and stories marked 'covered with the implementation of SIMPL-33537', restate earlier stories with changes. The specs do not say whether the earlier criteria still apply. Several of them now conflict (version field, redirect target, roles, default schema type). | For each pair, does the newer story replace the older one (older criteria no longer tested) or extend it (both sets must pass)? |
| F-044 | Scope | SIMPL-28815 | This story is about a Provider adding a billing contract clause in the resource description wizard. It names no schema manager operation and no BP02C step, so it most likely belongs to another component. | Is SIMPL-28815 out of scope for the schema manager build? If not, which schema manager behaviour does it require (for example a billing default property)? |
| B-002 | Business process flow | BP02C.01, BP02C.02, BP02C.03 (event delivery) | The Governance Authority's create, new version and revoke requests wait for every webhook subscriber. D24 sends one POST per matching subscription after the change is stored and before the API answers, with a 5 second timeout each. Any authenticated participant may add subscriptions (D28), and nothing limits how many. If the POSTs go one after another, ten slow subscribers hold one publish request for up to 50 seconds, and a participant can do this on purpose. The rule asks that the publisher does not wait for its consumers, and that flows open to automation have a limit. | Should delivery happen after the API has answered (asynchronous, for example an outbox or a queue), and should the number of subscriptions per participant or in total have a limit? If delivery stays synchronous, is it sequential or parallel, and what is the longest a Governance Authority request may wait? |
| B-003 | Business process flow | BP02C.06 and event delivery | The hand-over from a stored schema change to the other parties is at most once and has no way to recover. The e-mail and each webhook POST get one attempt; a failure is only logged. If the service stops between storing the change and sending, the event and the e-mail are lost. A subscriber that misses SchemaRevoked keeps treating a revoked schema as usable, and Providers are not told. The specs do not say how a subscriber or the operator finds out and catches up. | Is at-most-once delivery accepted for the process? If not: should events and e-mails go through a stored outbox with retries, and should subscribers be told to reconcile by reading GET /schemas (and with which role, see B-006)? Who is alerted when delivery fails? |
| B-005 | Business process flow | BP02C (no step): webhook subscription and schema events | The process has no step and no actor for the second hand-over the build makes: schema events to subscribing services. BP02C names only the Governance Authority as actor and only e-mail to Providers (BP02C.06, 2C.8). The decided build sends SchemaPublished and SchemaRevoked to any authenticated participant that subscribed, for RESOURCE and SEARCH schemas. The two webhook stories therefore have no place in the process, and the process does not say who the subscribers are (catalogue, federated search, other participants) or what they must do with an event. | Should BP02C add a step (or extend BP02C.05, BP02C.03 and BP02C.06) for publishing schema events to subscribers, name the subscribing parties as actors, and say what a subscriber is expected to do on SchemaPublished and SchemaRevoked? Or does this belong to another business process? |
| B-006 | Business process flow | BP02C Outcomes (hand-over to BP05B) | The main outcome of the process, a schema that 'becomes available ... for Providers (Business Process 5B)', has no decided read path. Every read operation needs GA_SCHEMA_VIEWER or GA_SCHEMA_ADMIN, the latest-content read is 'not public', and the service has no ingress. The specs do not say how a Provider, or the catalogue component that builds resource descriptions for Providers, finds the published schema of a resource type and with which credential. Inside this build that is consistent; across the process the hand-over is open. | Who reads the published schema for BP05B (the Provider through a UI, the catalogue component inside the agent, or both), through which operation, and with which role? Should D27 get a reader role for that party, or is the read path owned by another component? |
| B-007 | Business process flow | BP02C.01, BP02C.03, BP02C.05 | Replacing the published schema of a scope by another schema (another name) has no step, and the decided states force a gap. A new RESOURCE schema is stored as REVOKED while the old one is published (D7); a new SEARCH schema is refused (D8). So the Governance Authority must revoke the old schema first and then publish the new one by a new version (D12). In between, the scope has no published schema: Providers cannot create resource descriptions of that type, and for SEARCH federated search across Governance Authorities stops. Revoking the last published schema has the same effect, with no warning in the API. | Is the gap accepted, and should the process say that a change of content goes through a new version (BP02C.02) and a change of name through revoke then re-publish? Or should there be an atomic 'replace' (publish the new schema and revoke the old one in one step)? Should the API warn or refuse when the last published schema of a scope is revoked? |
| B-009 | Business process flow | POST /schemas and POST /schemas/{schemaName}/versions (BP02C.04 before BP02C.05) | The order of the checks on create and new version is not decided. BP02C runs validation (BP02C.04) before publication (BP02C.05), but the decisions do not say whether the 409 for an existing key (D4) or for a second published SEARCH schema (D8, D12) is checked before or after the three validation stages. A request that is both invalid and in conflict gets 400 from one builder and 409 from another, and the 'report' of BP02C.07 is sent in one case and not in the other. | What is the order of checks on POST /schemas and POST /schemas/{schemaName}/versions: authentication and role, request fields (400), key and scope conflicts (409, 404), then validation stages (400)? Or validation first? |
| P-005 | Other | schema-manager-be (specs/api/schema_openapi.yaml) | The frozen OpenAPI and the decided contract now differ in many places, and no decision says which one the builder implements from. D1 lets decisions win over story text, not over the OpenAPI. A builder who generates server stubs or contract tests from the OpenAPI gets 18 operations, no GET /schemas/{schemaName}, no 401, list items without created and a versions response that does not resolve. A builder who hand-codes from the decisions gets a different surface. | Will step 30 hand the builder a patched OpenAPI (with the changes above and the external reference inlined) as the build contract, or is the rule 'decisions win over the OpenAPI' to be added to D1? |
| B-004 | Other | BP02C.04 | Semantic validation in BP02C.04 depends on the Vocabulary Manager (prerequisite BP02B). Its behaviour when that dependency is slow or down is not defined: no timeout, no retry, and no answer for the Governance Authority. A builder can let every internal reference fail as localReferenceUnavailable, which returns 400 and blames the Governance Authority's schema, or answer 503 and keep the schema unvalidated. Whether a reference to a deprecated vocabulary passes is also open. | When the Vocabulary Manager cannot be reached in time, does create or new version fail closed with 503 (nothing stored, the Governance Authority may retry), and what is the timeout? Does a term from a DEPRECATED vocabulary give an error, a warning or nothing? |
| B-008 | Other | BP02C.05 (one published schema per scope) | The rule 'at most one published schema per scope' is checked when a schema is created or re-published, but nothing says what happens when two such requests run at the same time. Two Governance Authority users who each create the first Data schema, or a create and a re-publishing new version in the same scope, can both see 'no published schema' and both end PUBLISHED. The same holds for the 409 on a duplicate key. The state the process relies on ('one schema per resource type') can then break without an error. | Must the one-published rule and the key uniqueness hold under concurrent requests (for example a unique constraint or a lock per scope), and what does the losing request get (409, or REVOKED as in D7)? |

The 41 minor findings (wording, numbering, duplicates) are in [`ac-findings.json`](./ac-findings.json) and [`domain-findings.json`](./domain-findings.json).

## Coverage

From the links between criteria, operations and process requirements (step 12b, [`../inventory/inventory-review.md`](../inventory/inventory-review.md)). A gap is not always wrong, but each one is a place where the specs do not say how the parts connect.

- Operations with no criterion (9): `schema_openapi:GET /vocabularies`, `schema_openapi:POST /vocabularies`, `schema_openapi:PATCH /vocabularies/{vocabularyName}`, `schema_openapi:GET /vocabularies/{vocabularyName}/versions`, `schema_openapi:POST /vocabularies/{vocabularyName}/versions`, `schema_openapi:GET /vocabularies/{vocabularyName}/versions/{version}`, `schema_openapi:PATCH /metadataQualityProfiles/{metadataQualityProfileName}`, `schema_openapi:DELETE /metadataQualityProfiles/{metadataQualityProfileName}`, `schema_openapi:GET /schemas/{schemaName}/qualityProfiles/active`
- Process requirements with no criterion (27): `BP02C.01`, `BP02C.02`, `BP02C.03`, `BP02C.04`, `BP02C.05`, `BP02C.06`, `BP02C.07`, `BP2C.1`, `BP2C.2`, `BP2C.3`, `BP2C.4`, `BP2C.5`, `BP2C.6`, `BP2C.7`, `BP2C.8`, `BP2C.1.1`, `BP2C.2.1`, `BP2C.2.2`, `BP2C.3.1`, `BP2C.3.2`, `BP2C.4.1`, `BP2C.5.1`, `BP2C.6.1`, `BP2C.7.1`, `BP2C.7.2`, `BP2C.7.3`, `BP2C.8.1`
- Criteria with no operation and no process requirement: 19, listed in the review.

## The data models (CDMs)

| API | CDM | Register status | Approved | Branch |
|---|---|---|---|---|
| schema-manager-be | schema-manager-be.md | In review | No | feature/add-cdm |

See [`cdm/README.md`](../../../cdm/README.md) for the state of the CDM register as a whole.

## How this was made

| Step | What | File |
|---|---|---|
| 12 | every story, criterion, operation and process requirement with a stable id, and the links between them | [`../inventory/`](../inventory/inventory.md) |
| 20a, 20b | the acceptance criteria: structure and wording (script), then completeness, testability, ambiguity, consistency and correctness (agent) | [`ac-checks.md`](./ac-checks.md), [`ac-review.md`](./ac-review.md) |
| 21a, 21b | the API definitions against the API, security and OWASP rules, and the coverage between operations and stories | [`api-checks.md`](./api-checks.md), [`api-review.md`](./api-review.md) |
| 22a, 22b | the business process as a flow: coverage, order against the decided states, hand-overs between parties, failure paths | [`bp-checks.md`](./bp-checks.md), [`bp-review.md`](./bp-review.md) |
| 23a, 23b | the domain language: API against CDM against the stories, by the `DDD-` and CDM rules; the draft glossary | [`domain-checks.md`](./domain-checks.md), [`domain-review.md`](./domain-review.md) |

The agents worked only from the published specs and each other's output, not from code or earlier builds. Their findings were not checked one by one by a person; treat each as a well-argued question, not a verdict.

