# Spec feedback: Catalogue

Step 29 of the spec factory, assembled on 18 Nov 2026 by `spec-factory/pipeline/build_feedback.py` (engine c6a73dd). For the owners of the stories, the API definitions, the CDMs and the business process. Every point cites the ids it is about and the question that needs an answer; the full evidence is in the linked files.

**What this is.** An automated reading of the published specifications, as a builder would read them, before anything is built. A finding says where two careful readers would build different things, or where a builder would have to guess. It is not a judgement on the people who wrote the specs: most points are normal for specifications written by several teams over time.

## In short

- **Input:** 33 stories with 77 live acceptance criteria, 39 API operations, 85 business process requirements, and the CDMs of each API.
- **Findings:** 126: **25 to decide before building**, 67 to confirm, 34 small.
- **Mechanical checks:** 53 hits on the acceptance criteria and 62 on the API and CDM names; the agents triaged them, and the real ones are part of the findings above.
- **Coverage:** 15 of 77 criteria link to no operation and no process requirement; 25 of 39 operations and 65 of 85 process requirements have no criterion.

## Decide first

The blockers. A builder or tester cannot proceed on these without a decision, and two builders would decide differently.

### Behaviour and interactions

**F-004** (AC-C03). The scale of the score is open. The quick-search API example uses a fraction (overallScore 0.92, minimumScore 0.8), the advanced-search example uses minimumScore 75, and the classification note uses percent ranges. A test with minimum score 75 passes under one reading and returns nothing under the other.

- Where: SIMPL-29292/AC2, SIMPL-29293/AC2
- Question: Is the score a fraction from 0 to 1 or a percentage from 0 to 100, and what is the upper bound the UI accepts?
  - Option: 0 to 1
  - Option: 0 to 100

**F-005** (AC-C03). Classifications and their order come from "the active MQP", but a quick search has no schema, and each schema has its own active profile. The UI cannot know which list of classifications to offer, nor which ranges order them.

- Where: SIMPL-29292/AC3
- Question: For quick search, which profile supplies the classification list and the ordering: one profile per data space, the profile of each result's schema, or a union?
  - Option: one data-space-wide profile
  - Option: per result, using the profile of its schema
  - Option: union of all active profiles

**F-011** (AC-C05). The criteria require POST /schemas/{schemaName}/versions with multipart form data and a 201. No entry OpenAPI file has this operation, so the request fields, the response body and the error statuses are undefined.

- Where: SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-30850/AC3, SIMPL-31817/AC1, SIMPL-31817/AC2, SIMPL-31817/AC3
- Question: Where is the versioning operation defined (which component, which contract), and which fields does it take?

**F-012** (AC-C04). AC1 says schemaType may be left empty and defaults to RESOURCE; AC2 lists schemaType as a required field; AC4 names only the changelog and the file as validated fields. The UI cannot both require the field and default it.

- Where: SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-30850/AC4
- Question: Is schemaType required on the Create New Version form, or optional with RESOURCE as default?
  - Option: required, no default
  - Option: optional, defaults to RESOURCE

**F-013** (AC-C03). Creating a new version of the one federated search schema is a main path, yet a 409 is required when "another federated search schema is already published". If the schema being versioned counts as already published, the main path can never succeed.

- Where: SIMPL-30850/AC3, SIMPL-30850/AC5, SIMPL-31817/AC1, SIMPL-31817/AC3
- Question: Does the 409 apply only when the published SEARCH schema has a different name than the one being versioned?
  - Option: 409 only when a SEARCH schema with another name is published
  - Option: 409 whenever any SEARCH schema is published, versioning needs revocation first

**F-022** (AC-C05). The Trustor-side search operation (receiving a forwarded quick or advanced search) is in no entry file; the adapter calls it "the separate SIMPL-31880 API". AC2 requires a 403 on an operation whose path, parameters, attribute header and response are undefined.

- Where: SIMPL-31106/AC1, SIMPL-31138/AC1, SIMPL-31138/AC2, SIMPL-31138/AC3, SIMPL-31139/AC1, SIMPL-31139/AC2 and 1 more
- Question: Which contract defines the Trustor-side search operations, and can it be added to the input set?

**F-026** (AC-C03). "At least one of the search terms is found in at least one of its properties" needs a rule for splitting the search string into terms. The API takes one searchString that may contain spaces. Two readings give different results for "climate data".

- Where: SIMPL-31111/AC1, SIMPL-31138/AC1
- Question: Is the search string split on whitespace with OR between terms, or matched as one phrase? Is matching case-insensitive and by substring?
  - Option: split on whitespace, any term matches (OR)
  - Option: whole string as one phrase

**F-030** (AC-C04). For a Trustor that no longer trusts, SIMPL-31113 rejects the call with 502, while SIMPL-31140 and SIMPL-31142 return an empty group for that catalogue and leave the others unaffected. A builder cannot tell whether the Consumer gets 502 or 200 with an empty group.

- Where: SIMPL-31113/AC2, SIMPL-31113/AC3, SIMPL-31140/AC3, SIMPL-31142/AC3
- Question: Is the 502 internal (between the GAs, turned into an empty group for the Consumer) or the status the Consumer receives?
  - Option: internal only; Consumer gets 200 with an empty group
  - Option: Consumer gets 502 for the whole search

**F-033** (AC-C05). The criteria require results grouped per catalogue, each group naming its catalogue and GA, with independent paging. The federated search operation returns one flat items list with one page block and says it does not define aggregation.

- Where: SIMPL-31140/AC1, SIMPL-31140/AC2, SIMPL-31142/AC1, SIMPL-31142/AC2
- Question: What is the grouped response shape, and how does a client ask for the next page of one group?

**F-034** (AC-C04). SIMPL-31140/AC1 returns own and federated results "in a single response", while the SIMPL-31142 technical design has the UI call the local search API and the federated search API separately and build the tabs itself. These are two different builds.

- Where: SIMPL-31140/AC1, SIMPL-31142/AC1
- Question: Does the backend merge own and federated results into one response, or does the UI combine two calls?
  - Option: backend returns one grouped response
  - Option: UI makes two calls and groups the results

**F-044** (AC-C04). Three stories require the schema to come back with a federated detail, while the Trustor and Trusted stories have the schema struck and the adapter contract says it returns no separate schema. The UI cannot render the detail without knowing where the schema comes from.

- Where: SIMPL-31148/AC1, SIMPL-31770/AC1, SIMPL-33885/AC1, SIMPL-31153/AC2, SIMPL-31150/AC4
- Question: Is the schema part of the federated detail response, and if not, where does the UI get the Trustor's schema?
  - Option: schema returned with the detail (contract and SIMPL-31150/31153 to change)
  - Option: no schema; UI uses a generic or SEARCH schema (SIMPL-31148/31770/33885 to change)

**F-052** (AC-C05). The Trusted-side operations (xfsc-advsearch-be to the Trusted adapter, and Trusted adapter to Trustor adapter) are required by the story but are in no entry file; the only federated detail operation is the Trustor-side one. "I forward the request" has no observable contract.

- Where: SIMPL-31153/AC1, SIMPL-31153/AC2, SIMPL-31153/AC3
- Question: Where is the Trusted-side inbound operation defined, and is the Trustor-side operation the forwarding target?

**F-054** (AC-C05). The named operation catalogue-be POST /schemas takes a raw RDF or SHACL file, has no schemaType, name, title or resourceType field, requires no role ("N/A"), and has no 409 response. None of the three criteria can be met through this operation as defined.

- Where: SIMPL-31815
- Question: Is POST /schemas in SIMPL-31815 the catalogue-be operation, or the schema management API referred to as SIMPL-30422 / SIMPL-33537?
  - Option: catalogue-be POST /schemas, contract to be extended
  - Option: another component's schema API

**F-056** (AC-C04). The uniqueness rule differs: SIMPL-31815 rejects a second SEARCH schema only when another is published, SIMPL-31816 when one "already exists", and the technical notes say "only one". The hints add open questions on revoked schemas and name reuse.

- Where: SIMPL-31815/AC3, SIMPL-31816/AC2
- Question: Is the limit one SEARCH schema in any state, or one published SEARCH schema? May a new schema reuse a revoked schema's name?
  - Option: one published SEARCH schema; drafts and revoked allowed
  - Option: one SEARCH schema in any state

**F-057** (AC-C03). The UI must let the user "provide the schema properties, their data types, constraints, validation rules and any relationships". This reads as a structured schema editor, while the related version story uses a file upload. Two very different UIs.

- Where: SIMPL-31816/AC1
- Question: Does creating a federated search schema in the UI mean uploading a SHACL file, or editing properties in a form?
  - Option: upload a SHACL file plus name, title, schemaType
  - Option: form-based schema editor

**F-061** (AC-C04). With local and federated catalogues selected together, SIMPL-35874 shows only the SEARCH schema fields, while the SIMPL-31142 design searches the local catalogue with the filters of the selected resource schema. The local search in a mixed selection is built differently under each.

- Where: SIMPL-35874/AC2, SIMPL-31142/AC1
- Question: In a mixed selection, is the local catalogue searched with the SEARCH schema fields too?
  - Option: yes, SEARCH fields for all catalogues
  - Option: no, the user also picks a resource schema for the local part

### One language for the domain

**D-001** (DDD-01). The main business object has two live names. The stories, the business process and all three CDMs say resource description (CDM class ResourceDescription). The catalogue-be API says SelfDescription in every schema and path, the adapter uses both (/selfDescriptions for search, /resourceDescriptions for participant and federated detail), and catalogue-client-be uses selfDescriptions. The federated operations still to be defined (F-022, F-052) force a builder to pick one.

- Where: cross
- Question: Which name do new operations, schemas and events use, and is self-description kept only as a legacy wire name with a written mapping?
  - Option: Resource description in all new contracts and texts; existing SelfDescription schemas and /selfDescriptions paths stay as documented legacy names, mapped in the glossary
  - Option: Rename the existing schemas and paths to ResourceDescription (breaking change, versioned API)
  - Option: Keep self-description on the wire everywhere and resource description in business texts, with the mapping written down

**D-003** (DDD-01). The overall quality result has four names for the score and four for the classification. The criteria filter on overallCategory, which no API or CDM has. The Consumer-facing search (catalogue-client-be v1) returns qualityScore and qualityClassification, while catalogue-be and the adapter return overallScore and overallClassification. A test written from the criteria asserts on a field the response does not carry.

- Where: cross
- Question: Which field names does a search result and a detail carry for the overall score and classification, and is overallCategory meant as overallClassification?
  - Option: Business terms overall quality score and overall quality classification; wire names overallScore and overallClassification in every API; CDM attributes renamed to match
  - Option: Wire names qualityScore and qualityClassification as in the CDM and the Consumer-facing API; catalogue-be and adapter renamed
  - Option: Keep both, with a written mapping per API, and correct the criteria to the Consumer-facing names

**D-004** (DDD-02). "Federated catalogue" names two different things. In the stories and the business process it is a catalogue of another (Trustor) data space. In the CDMs and API texts the Federated Catalogue is the catalogue-be service of the own data space, and the adapter's local search says it searches "the Federated Catalogue". A builder of the federated search can read a requirement on federated catalogues as one on the own catalogue service.

- Where: cross
- Question: Which name does the catalogue-be service carry in business texts and CDMs, so that federated catalogue only means a catalogue of a Trustor data space?
  - Option: Federated catalogue only for a Trustor data space's catalogue (as BP06); the service becomes catalogue service (catalogue-be) in CDMs and API texts
  - Option: Keep Federated Catalogue for the service, and say catalogue of a federated data space (or Trustor catalogue) in stories and APIs
  - Option: Define both meanings in the glossary and qualify each use

### Entities, identities and states

**D-002** (DDD-09). A resource description is identified under at least eight names, and it is not stated which of them name the same value. The search result items carry only claimsGraphUri, the detail call takes selfDescriptionId, the adapter takes resourceDescriptionId, and catalogue-be has both a per-record id and a resourceKey "stable across versions". A tester cannot pass the identifier from a search result to the detail call with confidence, and a builder cannot tell whether revoke and QAR act on one version or on the whole resource description.

- Where: cross
- Question: Which identifier names a resource description across its versions, which names one version, and is the claimsGraphUri of a search result the selfDescriptionId and resourceDescriptionId of the detail calls?
  - Option: resourceIdentifier (= resourceKey) for the resource description across versions, id for one version; search results expose resourceIdentifier; paths use {resourceDescriptionId}
  - Option: One identifier only (the credential subject id, today claimsGraphUri); versions addressed by version number
  - Option: Keep all names, and add a written mapping per API to the CDM identifier

### API guidelines and coverage

**P-001** (COV-02). The Consumer-facing API has no search operation. The v2 entry file of catalogue-client-be holds only schema operations and the detail. D24 says the UI makes one call to catalogue-client-be, which forwards to the adapter, and that own-only searches use 'the existing local search operations'. No decision names the catalogue-client-be path, its parameters or where the grouped response lives. The only local search operations are tier 2 (catalogue-be quickSearch, adapter GET /selfDescriptions), which a Tier 1 UI does not call directly. D23 and D32 move GET /catalogues and the QAR operation from v1 into v2, but no decision moves the v1 search operations. SIMPL-31142 names a catalogue-client-be POST /selfDescriptions that no file has.

- Where: catalogue-client-be
- Question: Which catalogue-client-be operations does the search UI call for quick and advanced search, own-only and mixed, with which parameters, and do they return the D24 grouped response in both cases?
  - Option: Take v1 GET /selfDescriptions (q, catalogueIds, quality filters, paging) and POST /selfDescriptions/advanced into v2, both returning {groups: [...]} per D24, also for an own-only selection (one group)
  - Option: Same two operations, but return the flat v1 list for an own-only selection and {groups} only when a Trustor catalogue is selected
  - Option: Use POST /selfDescriptions as SIMPL-31142 names it, for both search modes

**P-002** (API-60). It is not defined how the Consumer's identity attributes reach the services that apply the discovery access policy. Local tier 2 searches (catalogue-be quickSearch and advancedSearch, adapter GET /selfDescriptions and POST /selfDescriptions/advancedSearch) take User-Attributes as an optional header. D20 and E1 say tier 2 services read User-Attributes and that no gateway is built, so tests send the gateway headers. In an end-to-end search the call comes from catalogue-client-be or the Trusted adapter, not from a test, and the Tier 1 operations have no User-Attributes header. Nothing says which service turns the Consumer's JWT into User-Attributes, from which claim, and D30 has the Trusted adapter send User-Attributes to the Trustor without saying where it gets them. D20 defines 400 for a missing header only on Trustor operations; on local operations one builder returns 400, another treats it as no attributes (D21: only descriptions with an empty list), a third skips filtering.

- Where: cross
- Question: Which JWT claim carries the identity attribute codes, which service sets User-Attributes on calls to tier 2 operations when no gateway is built, and what does a local tier 2 search return when the header is missing?
  - Option: catalogue-client-be and the Trusted adapter copy the codes from a named JWT claim into User-Attributes; a missing header on any tier 2 search gets 400
  - Option: Tier 2 services read the forwarded JWT themselves; User-Attributes is used only between Governance Authorities
  - Option: A missing header means no attributes: only descriptions with an empty policy list are returned (D21)

### Scope

**F-055** (AC-C06). The schema stories point to the "Schema management API" (SIMPL-30422) and SIMPL-33537, which are not in this component's input set. It is open whether the catalogue factory builds schema creation, versioning and the SEARCH type at all.

- Where: SIMPL-30850, SIMPL-31815, SIMPL-31816, SIMPL-31817
- Question: Which component owns schema management for these four stories, and are they in scope for the catalogue build?
  - Option: in scope: catalogue-be schema operations
  - Option: out of scope: schema manager component

### Business process flow

**B-001** (MSV-03). Informing Consumers about a new version or a revocation is a main step of BP05B, but its party, its trigger and its receiver are all open. The step texts are passive ('the Consumers of this resource are informed'). The requirement titles 5B.6 and 5B.9 make the Provider the sender, while the 5B.8 description makes the Governance Authority the sender. No decision says which service learns of the change and how: D18 and D35 define publication of a new version and revocation as synchronous calls that emit nothing, and no event, webhook or callback exists for either change. The catalogue also does not know who the 'Consumers of this resource' are: they are the parties with usage contracts (BP07), which live outside the catalogue. Two builders would build different flows: one emits an event from catalogue-be, the other builds nothing.

- Where: BP05B.12, BP05B.15, BP5B.6.1, BP5B.8.1, BP5B.9.1
- Question: Which party informs the Consumers (Provider, Governance Authority, or the catalogue service on their behalf), what signal starts it (an event from catalogue-be on publication of a new version and on revocation, or an action in another component), and who resolves the list of Consumers with a contract? Or are both notification steps out of the catalogue build, to be recorded as a decision?
  - Option: Out of the catalogue build: record a step 30 addendum that BP05B.12 and BP05B.15 belong to the contract or connector side, with no event from catalogue-be
  - Option: catalogue-be publishes an event (resource description superseded, resource description revoked) after the state change and does not wait; another component resolves Consumers and informs them
  - Option: The Provider informs Consumers itself through its own connector after a successful revoke or publication response

### Other

**P-003** (BUILDER). The publication operation, the main path of the quality gate, has no request contract. The body is 'type: string' with no example. The description says validation covers 'schema version, resource version number, and provider ownership' and rejects a conflicting version (409), but no field is named. A builder needs to know where the schema reference sits (D17 takes the threshold from that schema's active profile), how a new version names its resource description and carries its version number (D18 revokes the previous version, D7 addresses versions by number), and how ownership is checked (against which header or claim). The 201 response returns SelfDescription without the quality assessment, while SelfDescriptionPublicationResponse with qualityAssessment hangs on POST /self-descriptions, which D4 does not build; D11 refers to 'the publication response'. Two builders will accept different payloads and return different 201 bodies, and a tester cannot write a valid fixture.

- Where: catalogue-be POST /self-descriptions/resource
- Question: What is the publication request (credential fields for schema name and version, resource description id and version number, provider identity), which checks give 400, 403, 409 and 422, and which schema does the 201 body use?
  - Option: Body is the signed credential; schema from credentialSubject dct:conformsTo (dct:schemaName, dct:hasVersion), version from currentVersion in a named property, ownership from providedBy against User-Attributes; 201 returns SelfDescriptionPublicationResponse
  - Option: Same body, 201 returns SelfDescription only and the quality is read through the QAR operation
  - Option: Add an explicit JSON envelope {schemaName, schemaVersion, version, credential}

## Terms to agree

The draft glossary ([`glossary.md`](./glossary.md)) has 49 terms: 11 already used the same way everywhere, 26 where the draft proposes one term among synonyms, and 12 where the sources disagree on the meaning. One agreed term per concept, used the same way in the stories, the API and the CDM, removes a large part of the findings above.

Open: resource description identifier, resource type, quality score recalculation, search result group, catalogue identifier, federated catalogue, catalogue service (catalogue-be), federation enabled, DOME Marketplace offering, schema, schema name, schema administrator role.

## To confirm

The majors: the build can proceed on a sensible default, but the default should be confirmed.

| Id | Theme | Where | Finding | Question |
|---|---|---|---|---|
| F-002 | Behaviour and interactions | SIMPL-27998/AC3 | AC2 says a rejected description is not stored; AC3 says only that it is not published. A builder can read AC3 either way. The technical error also has no status and "the failure reason is logged" has no observable oracle. | On a technical failure of the quality assessment, is the description stored (unpublished) or not stored at all, and which status is returned? |
| F-006 | Behaviour and interactions | SIMPL-29293/AC3 | Advanced search has a selected schema, so its active profile is the natural source of the classifications. The example ranges overlap at the bounds (40 in Poor and Sufficient), and the API examples use other labels (Green, good). The sensible default should be confirmed. | Is the classification list taken from the active profile of the selected schema, and is a range bound inclusive at the lower end? |
| F-010 | Behaviour and interactions | SIMPL-30848/AC1 | The role is left blank and "endpoint details" are not named. A note gives a URL rule (base URL plus a catalogue suffix), but the AC does not say what is provided or what is checked. No negative case (invalid endpoint, missing role) exists. | Which role, which fields make up the endpoint configuration, and what happens on an invalid or unreachable endpoint? |
| F-016 | Behaviour and interactions | SIMPL-31817/AC3 | AC3 combines a resourceType in the request (a 400 under AC2) with an existing published schema (a 409). The order of the checks is not stated, so a test of AC3 may get 400 instead of 409. The status is also written "409 Config" here and "409 Conflict" in SIMPL-31815/AC3. | Is the resourceType clause in AC3 intended, and if so, which check wins: 400 or 409? |
| F-018 | Behaviour and interactions | SIMPL-31102/AC1, SIMPL-31102/AC2, SIMPL-31103/AC1, SIMPL-31103/AC2 | The list of selectable catalogues comes from an API "implemented under ticket SIMPL-33471" that is in no entry file. The criteria cannot be built or tested against a defined response. | Which operation returns the selectable catalogues, and what is its response shape? |
| F-019 | Behaviour and interactions | SIMPL-31102/AC1, SIMPL-31103/AC1, SIMPL-35874/AC1 | The selection list holds "the catalogues of all trustor authorities". SIMPL-31142 and SIMPL-35874 also talk about selecting the local catalogue. It is open whether own catalogues appear in the same selector. | Does the catalogue selector list own catalogues together with the Trustor catalogues? |
| F-020 | Behaviour and interactions | SIMPL-31102/AC1, SIMPL-31103/AC1 | The business process also allows a search in all federated catalogues without selecting them, while the federated search API requires a non-empty catalogue selection and no story offers "all". | Is "search all federated catalogues" in scope for this release? |
| F-023 | Behaviour and interactions | SIMPL-31138/AC2, SIMPL-31139/AC2, SIMPL-31113/AC2 | The Trustor rejects a non-trusted caller with 403, while the Trusted side expects a failed mTLS handshake (502) for the same broken trust. A failed handshake never reaches HTTP, so a 403 can only happen for a caller with a valid certificate. The two tests need different set-ups. | Which caller gets 403 at the Trustor (valid certificate but no federation?), and which case ends in a handshake failure? |
| F-027 | Behaviour and interactions | SIMPL-31112/AC1, SIMPL-31139/AC1 | "Each search term is found in the property to which it was submitted" does not say whether found means equal, contains, or a typed comparison (numbers, dates). | Which comparison applies per property type in advanced search? |
| F-028 | Behaviour and interactions | SIMPL-31111/AC1, SIMPL-31141/AC1, SIMPL-31767/AC1 | Local quick search must keep only descriptions from the catalogues in the query, but the local quick-search operations have no catalogue parameter (the local advanced search body does have catalogueIds). | How does a local quick search carry the catalogue selection, or is the own data space always one catalogue? |
| F-031 | Behaviour and interactions | SIMPL-31113/AC3 | A catalogue that belongs to no federation has no Trustor to call, so no handshake can fail. A 400 for an unknown catalogue identifier is the other natural reading. | For an unknown or unfederated catalogue identifier, is the result 502, 400, or an empty group? |
| F-035 | Behaviour and interactions | SIMPL-31140/AC1 | "Own catalogues (if relevant)" does not say when the own catalogue is searched in a federated search. | Is the own catalogue searched only when the Consumer selected it? |
| F-037 | Behaviour and interactions | SIMPL-31140/AC4 | Federated groups must omit the quality score, but the federated search operation returns overallScore and overallClassification per item and accepts minimumScore and minimumClassification as filters. It is open whether the quality filter applies to federated catalogues. | For federated catalogues, are the quality fields removed from the response, and is a quality filter ignored or applied by the Trustor? |
| F-040 | Behaviour and interactions | SIMPL-31141/AC1, SIMPL-31767/AC1 | The process requires the own-catalogue overview to show the overall quality score and classification and to be sortable on main and quality properties. The overview criteria mention neither. | Are quality fields and sorting in the overview in scope for these stories? |
| F-041 | Behaviour and interactions | SIMPL-31143/AC1, SIMPL-31769/AC1 | The detail must include overall and per-dimension quality, but the detail operation returns only the credential, and quality sits in a separate QAR operation that answers 204 when there is no record. No criterion says what to show when a description has no QAR. | Which operation returns the quality block for the detail, and what is shown for a description without a QAR? |
| F-047 | Behaviour and interactions | SIMPL-33885/AC1, SIMPL-33885/AC2, SIMPL-31770/AC1, SIMPL-31770/AC5 | SIMPL-33885 adds a "dataspace/catalogue identifier" (one value or two?), while SIMPL-31770 says the API accepts "the resource identifier returned by the federated search" and keeps the own-catalogue path unchanged. The contract makes catalogueId required for every call, own catalogue included. | Is the identifier a single catalogueId, and is it required for own-catalogue detail requests too? |
| F-050 | Behaviour and interactions | SIMPL-31150 | The story's technical description puts authentication, attribute mapping and policy evaluation in the adapter operation, but the adapter contract says the gateway does these and the adapter does not repeat them. Which component a test targets depends on the answer. | Is mapping and policy evaluation built in the adapter or in the gateway? |
| F-053 | Behaviour and interactions | SIMPL-31153/AC3 | AC3 forwards the Trustor's error to the Consumer, while the technical description asks to translate errors without exposing federation details. These give different responses for the same error. | Is a Trustor error passed through unchanged or mapped to a fixed set of Consumer-facing errors? |
| F-059 | Behaviour and interactions | SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-31815/AC1, SIMPL-31816/AC1, SIMPL-31102/AC1, SIMPL-31103/AC1 | One permission has four names: a role "authorising schema management", governance-admin, GA_SCHEMA_ADMIN, and for search a role "authorising searching" against the API's ABAC roles. A tester cannot set up the user. | Confirm the role names: GA_SCHEMA_ADMIN for schema management, CATALOGUE_SEARCHER (or which set) for search? |
| F-060 | Behaviour and interactions | SIMPL-35874/AC3 | The UI must call the schema retrieval API with schemaType=SEARCH, but the schema list operation only has a resourceType parameter. | Which operation takes schemaType, and will the contract add it? |
| D-006 | One language for the domain | cross | "Active" carries three meanings: a lifecycle value of a resource description, the Metadata Quality Profile resolved for the latest published schema version (whose own lifecycle has no active value), and a catalogue's federation flag. The trust relation behind that flag is also called trusted, no longer trusted, federation enabled and authenticated. | Which word names each of the three concepts, and what are the values of the federation (trust) status? |
| D-008 | One language for the domain | cross | Resource type and offering type look like one concept with two names and four spellings: lower case (data, application, infrastructure), upper case (DATA, APPLICATION), short forms (App, Infra, Data) and PascalCase shape names (Application, DataOffering). The schema stories reject a SEARCH schema when a resourceType is given, so the accepted values matter for the 400 tests. | Are resource type and offering type the same concept, and which values and spelling do the API, the CDM and the criteria use? |
| D-009 | One language for the domain | cross | "Schema" names different things in one component. catalogue-be's /schemas operations manage the original catalogue store of ontologies, shapes and vocabularies (OntologySchema, latest composite schema), while its CDM says schema management is external and those legacy endpoints are excluded. The Schema Manager schema has a schemaName, a schemaType and versions. The schema is identified as schemaId, schemaName, name or uri depending on the API. The stories send schema creation to "POST /schemas", which in catalogue-be is the legacy store. | Which schema concept do catalogue-be's /schemas operations expose, are they still in scope, and which identifier names a schema? |
| D-014 | One language for the domain | cross | The catalogue selection of a search has many names and no CDM concept. The process calls it the search scope (own, federated, DOME). The APIs use catalogueIds (required for the adapter's local and Governance Authority searches, optional in the Consumer-facing quick search) and catalogueId for a detail; one story asks for a "dataspace/catalogue identifier". The client CDM says the Governance Authority scoping adds "no additional concept". | Is the catalogue selection (search scope) a concept in the client CDM, and is the catalogue identifier one value named catalogueId everywhere? |
| D-005 | Entities, identities and states | ResourceDescription | The lifecycle of a resource description is not defined once. The CDM and API know active and revoked; the criteria and the process speak of submitted, valid, published, not published and stored. The process revokes the previous version when a new one is published, but no transition or state covers that. The publish operation can answer 202 "accepted but the validation is not finished yet", a pending state that no value set has. DELETE removes a description "entirely", a step the process does not have. | Is published the business label of active, is there a pending state after submission, does a superseded version become revoked or a separate state, and is deletion part of the lifecycle? |
| D-011 | Entities, identities and states | cross | The search result item is an anonymous node (n in catalogue-be and the adapter, i in catalogue-client-be v1) that holds the general service properties summary, the quality summary and claimsGraphUri. No CDM class names it. Its fields (name, description, inLanguage) differ from the catalogue-be CDM attributes (serviceName, serviceDescription, serviceLanguage), and the referencing CDM lists yet other ones. The stories call the same content "the main properties" without listing them. | Are the main properties the GeneralServiceProperties summary, and which attribute names does the CDM use for it? |
| D-007 | Boundaries with other components | QualityAssessmentRecord | The parts of a Quality Assessment Record have three sets of names. The owning CDM (Quality Scoring Service) has OverallQualityRating, DimensionRating and RuleOutcome. The catalogue-be API redefines them as SelfDescriptionQualityOverallResult, ...DimensionResult and ...RuleResult with other attribute names, and the Consumer-facing record uses SelfDescriptionScore, DimensionScore and RuleEvaluation. The record is also exposed twice: as an opaque JSON-LD graph and as a structured qualityAssessment in the publication response. The threshold source is called metadata quality definition, Metadata Quality Profile and Minimum Quality Profile. | Does the catalogue API use the owner's names for the record's parts, and which representation is the Quality Assessment Record the catalogue stores and returns? |
| D-010 | Boundaries with other components | cross | Identity attributes, their federation mapping and discovery access policies carry the federated search criteria, but no CDM in the input set models them, as owned or as external. The only related class, AccessPolicyFilter, is owned by the Query Mapper, is "interaction only", and never appears in the stories. The same ideas have many names: identity attributes, public identity attributes, Consumer's attributes, mapped attributes, User-Attributes; discovery access policies, access policy values, discovery ABAC. | Which context owns identity attributes, the attribute mapping and the discovery access policy, and which external classes do the catalogue CDMs add for them? |
| F-001 | Error cases and testability | SIMPL-27998/AC2 | The rejection names no status code. The publish operation offers several error statuses, so a tester cannot write the assertion. The error text in the technical details covers only the overall score, not a failing dimension. | Which status code and which message does a quality rejection return, for an overall-score failure and for a dimension-score failure? |
| F-003 | Error cases and testability | SIMPL-27998 | The technical details list an invalid-QAR scenario and an endpoint-failure scenario that no criterion describes as an outcome; AC3 only gives a missing threshold as an example. It is also not said which metadata quality definition applies when several schemas have their own active profile. | Does an invalid QAR or an unreachable scoring endpoint fall under AC3, and is the threshold taken from the active profile of the description's schema? |
| F-007 | Error cases and testability | SIMPL-29292, SIMPL-29293 | AC6 (description without a score while a filter is active) and AC7 (invalid filter input) are struck, but the API returns descriptions with overallScore null and defines 400 errors for invalid values, and the technical details still ask for inline validation. A builder has to guess both behaviours. | With a quality filter active, are descriptions without a score excluded, and how does the UI handle an invalid score or a negative value? |
| F-014 | Error cases and testability | SIMPL-30850 | No criterion covers a user without the schema-management role (button hidden, or 403 shown), and AC1 and AC2 give two different role descriptions for the same action. | What does the UI do for a user without the role, and which role is meant? |
| F-017 | Error cases and testability | SIMPL-31815, SIMPL-31817 | Invalid SHACL, a missing name or title, and a caller without GA_SCHEMA_ADMIN are left to tickets outside this input set, so a builder has no expected status for them. | Which statuses apply to an invalid definition, a missing field and a caller without the role? |
| F-024 | Error cases and testability | SIMPL-31138, SIMPL-31139, SIMPL-31114 | No criterion covers a forwarded request without identity attributes, an attribute with no mapping, an unavailable mapping service, or an invalid request. SIMPL-31150 lists these cases for the detail request; the search side is silent. | What does the Trustor return when attributes are missing, unmapped, or the mapping service is down? |
| F-029 | Error cases and testability | SIMPL-31111, SIMPL-31112 | The linkage-only stories for local search start from "the Consumer is authorised" and give no outcome for a Consumer without the search role or without a token. The ABAC roles in the API (CATALOGUE_SEARCHER and others) are not named. | What does a search return for a Consumer without a search role: 403, or an empty list filtered by policy? |
| F-032 | Error cases and testability | SIMPL-31113 | No criterion covers a Trustor that is unreachable or slow, and no timeout value is named, while SIMPL-31140/AC3 only covers "cannot authenticate, or returns no results". | What happens when a Trustor does not answer, and after how long? |
| F-036 | Error cases and testability | SIMPL-31140/AC3 | An authentication failure and a genuine empty result both give an empty group, so the Consumer cannot tell them apart and a tester cannot tell a fault from no matches. | Should a failed group carry a status or error marker next to the empty list? |
| F-039 | Error cases and testability | SIMPL-31141/AC1, SIMPL-31767/AC1, SIMPL-31768/AC1 | "The main properties" is the pass condition for every overview, but no story or API lists them. | Which properties are the main properties (for example name, description, offering type, access point)? |
| F-042 | Error cases and testability | SIMPL-31143, SIMPL-31769 | The own-catalogue detail stories have only the happy path. The federated twins (SIMPL-31148, SIMPL-31770) define not-found, failure and invalid-request cases; here a builder must guess them. | Do the error criteria of SIMPL-31148 and SIMPL-31770 also apply to own-catalogue details? |
| F-046 | Error cases and testability | SIMPL-31770/AC4, SIMPL-33885/AC4 | Downstream failures must return "the corresponding error response according to the defined API contract", but the contract of the detail operation has only 400, 401, 404 and 500. The technical description asks for forbidden and timeout responses that the contract does not have, and SIMPL-33885/AC4 says 500 for unexpected errors. | Which statuses apply to policy-denied, Trustor timeout and Trustor unreachable (403, 504, 502, or 500 for all)? |
| F-049 | Error cases and testability | SIMPL-31150/AC2, SIMPL-31150/AC3 | "Refuses the request" names no status. The adapter contract says gateway authentication and policy errors are outside it, and the technical description says the resource must not be exposed when it does not exist. A tester cannot tell 401, 403 or 404. | Which status does the Trusted GA receive for an untrusted caller, missing attributes and a policy denial; is a denial disguised as 404? |
| F-051 | Error cases and testability | SIMPL-31150 | The technical description lists not-found, mapping-unavailable and downstream-error cases and test cases for them, but no criterion states their outcome. The contract has a 404. | What does the Trustor return for not-found, mapping unavailable and catalogue unavailable? |
| F-058 | Error cases and testability | SIMPL-31816 | No criterion covers an invalid file, an API error or a user without the role on the create form, unlike the version form in SIMPL-30850/AC4 and AC5. | Do the error criteria of SIMPL-30850/AC4 and AC5 also apply to the create form? |
| F-062 | Error cases and testability | SIMPL-35874/AC2 | No criterion covers the case where no SEARCH schema exists yet or the retrieval fails, so the form's behaviour is unknown. | What does the form show when there is no published SEARCH schema? |
| D-012 | Data model governance | cross | Each API has a CDM, but the alignment tables no longer match the entry files. The catalogue-be CDM says legacy schema and session endpoints are excluded, yet the tier 1 entry file has them, and the generic /query operation has no concept at all. The Query Mapper CDM lists neither the Governance Authority search operations nor the federated detail operation, and its scope does not mention federation. The catalogue-client-be CDM lists /governanceAuthority operations that its v1 file does not have, and places the detail operation with catalogueId in v1 while it is in the v2 entry file. | Are the CDMs updated to the entry APIs, or are the extra operations (session, query, legacy schemas) removed from the contracts? |
| D-013 | Data model governance | RecalcQualityScoreJob | RecalcQualityScoreJob is modelled as an owned business record, but the API only returns a 202 acknowledgement without an id that can be read again. The CDM also contradicts itself on the trigger: one note says the schema-events webhook "is not the trigger for recalculation", the endpoint table says a SchemaPublished or SchemaRevoked event "triggers recalculation". The attribute names differ from the API. | Is a recalculation a business record with its own identity and states, or only an acknowledged request, and which event triggers it? |
| P-004 | API guidelines and coverage | cross | The security schemes do not describe the decided authentication and roles. catalogue-be uses OAuth2Auth at root but defines only 'jwt' (http bearer), so the reference is broken in both entry files and in the root spec. The adapter defines an oauth2 authorizationCode flow with placeholder URLs (auth.example.com) and scopes read and write, not Keycloak and not roles. catalogue-client-be uses a plain bearer JWT 'IAA cloud gateway'. The role CATALOGUE_SEARCHER of D20 and E1 appears only in descriptions of catalogue-be tier 2 operations, not on any Tier 1 operation. D20 gives Tier 1 401 for no token and 403 without the role, but the adapter Tier 1 searches list neither 401 nor 403 and catalogue-client-be lists no 403 anywhere. catalogue-be tier 2 operations also declare Authorization as a header parameter, which OpenAPI ignores when a security scheme applies. |  |
| P-006 | API guidelines and coverage | cross | The contracts still allow and show the 0 to 100 scale that D14 rejected. minimumScore has no maximum in any operation, and catalogue-be quickSearch and the adapter local search have no minimum either (only the description says '>= 0'). In the advanced search bodies minimumScore and minimumClassification are not declared properties at all (catalogue-be body is 'type: string', the adapter payload only mentions them in the description). Examples send minimumScore 75 and minimumClassification 'good', results show 'Green', and the active profile example in catalogue-client-be uses score ranges 0 to 100 and thresholds 70, 85 and 90. A validator generated from these files accepts 75 and returns 200, while D14 requires 400. |  |
| P-007 | API guidelines and coverage | query-mapper-adapter-be advancedSearchRequestPayload | catalogueIds in the shared advanced search payload is required but has no minItems, and its description says 'If no Ids provided no catalogue will be searched', which reads as 200 with no results. D5 keeps at least one item and D25 gives 400 for an unknown catalogue. The same payload is used by the local tier 2 advanced search, where D22 says the own data space has one catalogue and a local search needs no catalogue parameter; it is open whether the local operation ignores catalogueIds, requires the own catalogue id (E3), or rejects other values. | What do the local and the Governance Authority advanced searches do with an empty catalogueIds and with a value that is not the own catalogue? |
| P-009 | API guidelines and coverage | catalogue-be | catalogue-be returns two error formats. The shared responses ClientError, NotFound, Unauthorized, Forbidden, ServerError, Conflict and Unprocessible are application/json with Error {code, message}; the QAR, quick and advanced search, recalculation and webhook errors are application/problem+json Problem. A client of the built operations has to parse both (for example quickSearch 400 is Problem, 500 is Error). D17 builds on Error.code (QUALITY_THRESHOLD_NOT_MET, QUALITY_ASSESSMENT_FAILED). 21a did not report API-45 for these responses. catalogue-client-be v2 defines five copies of the problem schema with a property literally named additionalProperties. | Do the built catalogue-be operations move to problem+json, with the D17 codes as a Problem extension member, or does D17 keep Error for the publication operation only? |
| P-011 | API guidelines and coverage | catalogue-client-be | A schema has three identifiers in the client API and nothing maps them. GET /schemas returns items with schemaId (dataOfferingShape), name ('health', 'Data offering') and resourceType, in an untyped object. Content and version operations take schemaId; the active profile operation takes a PascalCase schemaName (pattern ^[A-Z][a-zA-Z0-9]*$) that none of the example names match. A credential points to its schema with dct:schemaName and dct:hasVersion. The UI needs the profile of a schema (D15) and the schema of an own detail (D29), and two builders will derive the path values differently; one renders own details with the latest content, the other with the version the credential conforms to. | Which value from GET /schemas or from a credential is passed as schemaId and as schemaName, and does an own detail render with the latest schema content or with the version it conforms to? |
| P-012 | API guidelines and coverage | catalogue-be | Two operations that the build needs have no story: POST /self-descriptions/{selfDescriptionId}/revoke and GET /self-descriptions/{selfDescriptionId}/versions. D4 keeps them as fixtures and D18 defines revoke (no-op 200 on a revoked one, superseded versions revoked), and BP05B revokes the previous version, but no acceptance criterion traces this behaviour. A story for the Provider revoking a resource description and reading its versions would make D18 testable from requirements. |  |
| P-013 | API guidelines and coverage | catalogue-be, catalogue-client-be | The versioning in the URLs does not match. catalogue-be serves at '{baseUrl}' with a default ending in /fc, no apiName and no major version. catalogue-client-be v2 has info.version '1.0' but its server URL ends in /v2. The adapter is consistent (/v1, 1.x). A client cannot tell the major version of catalogue-be from the URL, and the client API says major 1 and 2 at once. |  |
| F-009 | Scope | SIMPL-30848 | The note says the story is implemented by the authentication provider, a component outside this input set, and that the story exists only for linkage. Its Then also asks to let the Consumer choose the catalogue, which SIMPL-31102 and SIMPL-31103 cover. | Is anything in SIMPL-30848 built or tested in the catalogue component, or is it out of scope here? |
| F-038 | Scope | SIMPL-31142 | The four criteria of this UI story are copies of the backend criteria of SIMPL-31140, written from the GA's view. The UI behaviour (one tab per catalogue, navigation) is only in the technical design, so no criterion tests the UI. | Should SIMPL-31142 get UI criteria for the tabs, with SIMPL-31140 owning the backend criteria? |
| B-002 | Business process flow | BP05B.11, BP05B.12, BP05B.15, BP5B.1.3 | The process gives a superseded version and a revoked resource description two different follow-ups (BP05B.12 inform about new version, BP05B.15 inform about revocation), but the decided lifecycle has one state for both: revoked. Neither the stored record nor the version list says why a version was revoked. Any later notification (B-001) or the Provider's version history (5B.1.3) cannot tell 'replaced by version n+1' from 'withdrawn by the Provider'. The 409 rule of D35 works only because the latest version is revoked solely by an explicit revoke, which is an implicit rule, not a stored fact. | Does a superseded version need to be told apart from a withdrawn one (a separate state such as superseded, or a revocation reason on the version), so that BP05B.12 and BP05B.15 and the version history can be driven from the record? |
| B-003 | Business process flow | BP05B.13, BP05B.14, BP5B.7.1, BP5B.8.1 | The process makes revocation a hand-over between two parties: the Provider requests (BP05B.13), then the Governance Authority revokes 'upon receiving a request' (BP05B.14). The decisions collapse this into one synchronous Provider call with immediate effect: POST .../revoke with a publisher role and ownership check. That is a valid reading if the catalogue service acts for the Governance Authority automatically, but it is not stated, and there is no path for a Governance Authority to refuse a request or to revoke on its own initiative (for example a non-compliant description). D37 gives the Governance Authority no role on revoke at all. | Is the Governance Authority's revocation the automatic execution of a valid Provider request by the catalogue service (no review), and can the Governance Authority also revoke a resource description without a Provider request? |
| B-004 | Business process flow | BP05B.04, BP05B.05, BP05B.07, bp05b-dynamic-view | The two BP05B files give a different order for the main path. The process steps validate first and publish after (BP05B.05 validate, BP05B.06 remediate, BP05B.07 publish), which matches the decided synchronous gate (D17, D18: 201 or an error, no pending state). The dynamic view signs and 'publishes the signed self-description to the Catalogue' first, and only 'after publication' runs semantic validation and the quality check, then stores the description and notifies the end user. A builder who follows the dynamic view builds an asynchronous flow with a submitted or pending state that D18 rejects. | Can the dynamic view be aligned with the process steps (validation and the quality gate before publication, one synchronous answer), and the word 'publishes' in step 5 changed to 'submits'? |
| B-005 | Business process flow | BP05B.04 to BP05B.07, bp05b-dynamic-view steps 4 and 5 | Publication spans several components before the catalogue answers: the dynamic view registers the description as an asset in the Connector (step 4), signs it with the Signer and stores a signed copy in the Provider's Wallet (step 5), and only then submits it to the catalogue. The catalogue can still refuse it (400, 403, 409, 422, 500 under D35 and D17). Nothing says what happens to the connector asset and the wallet copy of a refused description: undone, kept as a draft, or reused on the resubmission that BP05B.06 asks for. A refusal can leave an offer registered in the connector for a description that is in no catalogue, and a resubmission can register a second asset. | When the catalogue refuses a submission, what happens to the connector asset and the wallet copy made before it, and does a resubmission reuse them? Is registering the asset after a successful publication an option? |
| B-006 | Business process flow | BP05B.05, BP5B.5.7, BP5B.5.8 | Semantic validation of a submitted description against its schema and the vocabulary is a named main-path check (BP05B.05) and the dynamic view gives it to the catalogue, but no criterion and no decision defines it. D35 checks the JSON shape, four required fields, that the schema version is published, ownership and the revoked state; D17 adds the quality gate. Whether the credential subject is validated against the SHACL shapes of the schema version, and what a violation returns, is open. One builder will reject a description that breaks its shapes, another will publish it. | Does catalogue-be validate the credential subject against the SHACL shapes of the referenced schema version before the quality gate, and does a violation return 400 invalidInput with the failing shape in detail? Or is semantic validation part of the Quality Scoring Service call? |
| B-007 | Business process flow | BP05B assumptions, BP5B.2, BP5B.3, BP5B.5.2 | The process assumes a new description or version 'always uses the latest version of a schema', and 5B.5.2 has the Governance Authority notify the Provider about an inactive schema. D35 accepts any schema name and version that is 'a published RESOURCE schema version'. It is not said whether an older version stays published after a newer one is published, so a submission against an older version is accepted by one builder and refused by another, and the refusal for an inactive schema has no own outcome. | Must the submitted dct:hasVersion be the latest published version of the schema, and does a submission against an older or revoked version get its own 400 detail? |
| B-008 | Business process flow | BP06.04, BP06.11 | BP06.04 is titled 'Conduct a quality rating of the resources' but its text describes the Consumer accessing the returned results per scope. It sits before the federated steps BP06.05 to BP06.10 and the collection step BP06.11, so read in order the Consumer sees results before the Trustor results are collected. The decided flow returns one grouped response after all groups are done (D24, D25), and no quality rating happens at search time: the score comes from the record stored at publication (D17, D26). The title and the position of BP06.04 disagree with the decided flow. | Can BP06.04 be renamed (for example 'Consult the search results') and moved after BP06.11, and is any quality rating done at search time, or only the stored rating applied as a filter? |
| B-010 | Business process flow | BP06.05, BP06.08, BP6.5.1 | A federated search creates load at every selected Trustor Governance Authority, another party, on behalf of one Consumer. The decided limits are a page size cap of 100 (D33) and a 10 second timeout (D25). There is no limit on the number of catalogueIds in one request, no rate limit per Consumer at the Trusted side, and no limit at the Trustor per Trusted Governance Authority. An automated Consumer can fan out many searches per second to all Trustors. No criterion or decision covers this abuse case. | Which limits apply to federated search: a maximum number of catalogueIds per request, a rate limit per Consumer at the Trusted side, a rate limit per Trusted Governance Authority at the Trustor (and its status, 429)? Which of these belong to the gateway? |
| P-005 | Other | catalogue-be POST /self-descriptions/{selfDescriptionId}/qualityAssessmentRecord | The operation that creates or replaces a Quality Assessment Record requires no role ('Required ABAC roles: N/A') yet lists 401 and 403. D4 builds it and D17 says publication calls the scoring service and stores the record itself. If any caller may write a record, a Provider can replace a failing record with a passing one and change the scores the search filters on. One builder leaves it open, another restricts it to an internal caller; a tester cannot write the 403 case. | Who may call POST .../qualityAssessmentRecord, and is it used by the build at all, or does publication store the record internally? |
| P-008 | Other | query-mapper-adapter-be | The Governance Authority advanced search that D24 uses is a draft outside the composed root spec. The adapter README says only the quick-search path is composed into the root, and the root drives code generation. Its operationId advancedSearchSelfDescriptions is the same as the local tier 2 advanced search, so composing both gives a duplicate operationId. The root says version 1.2.0 while both tier files say 1.1.0. |  |
| P-010 | Other | catalogue-be | Several built catalogue-be operations have no response structure. The detail (GET /self-descriptions/{selfDescriptionId}) and the version list (GET .../versions) return 'type: string' under application/json, and the advanced search request is 'type: string'. D7 addresses versions by number only through the version list and D18 relies on it in tests, but nothing says what a version item holds, whether the requested version is included ('Returns all versions except the requested Self-Description'), or how revoked versions appear. The adapter has a versionHistoryResponsePayload for its own version list that catalogue-be does not reuse. | What does GET /self-descriptions/{selfDescriptionId}/versions return per item (id, version number, status, dates), and does it include the requested and revoked versions? |
| B-009 | Other | BP06.03, BP06.11, BP6.6.1 | The failure of a Trustor in a federated search is defined (D25: group status unavailable, still 200), but the failure of the own catalogue is not. In a mixed search, the Trusted adapter searches its own catalogue-be as one group: if that call fails or times out, it is open whether the own group gets status unavailable or the whole search fails. In an own-only search (D33 routing to the local tier 2 operations), the status list names 400, 401 and 403 but no outcome for a failing catalogue-be. The UI texts of D23, D29 and D31 cover no search failure. | When the own catalogue fails during a search, does its group get status unavailable (mixed search) and does an own-only search return 500 internalServerError, and what text does the UI show? |

The 34 minor findings (wording, numbering, duplicates) are in [`ac-findings.json`](./ac-findings.json) and [`domain-findings.json`](./domain-findings.json).

## Coverage

From the links between criteria, operations and process requirements (step 12b, [`../inventory/inventory-review.md`](../inventory/inventory-review.md)). A gap is not always wrong, but each one is a place where the specs do not say how the parts connect.

- Operations with no criterion (25): `catalogue-be:GET /schemas`, `catalogue-be:GET /schemas/{schemaId}`, `catalogue-be:PUT /schemas/{schemaId}`, `catalogue-be:DELETE /schemas/{schemaId}`, `catalogue-be:GET /schemas/latest`, `catalogue-be:POST /schemas/{schemaName}/recalcQualityScore`, `catalogue-be:GET /verification`, `catalogue-be:POST /verification`, `catalogue-be:GET /session`, `catalogue-be:DELETE /session`, `catalogue-be:POST /webhooks/schema-events`, `catalogue-be:GET /self-descriptions`, `catalogue-be:POST /self-descriptions`, `catalogue-be:GET /self-descriptions/{selfDescriptionId}`, `catalogue-be:GET /self-descriptions/{selfDescriptionId}/versions`, `catalogue-be:DELETE /self-descriptions/{self_description_hash}`, `catalogue-be:POST /self-descriptions/{selfDescriptionId}/revoke`, `catalogue-be:GET /query`, `catalogue-be:POST /query`, `catalogue-client-be:GET /schemas/{schemaId}/content`, `catalogue-client-be:GET /schemas/{schemaId}/{version}`, `query-mapper-adapter-be:GET /participants/resourceDescriptions`, `query-mapper-adapter-be:GET /participants/resourceDescriptions/{resourceDescriptionId}`, `query-mapper-adapter-be:PATCH /participants/resourceDescriptions/{resourceDescriptionId}`, `query-mapper-adapter-be:GET /participants/resourceDescriptions/{resourceDescriptionId}/version`
- Process requirements with no criterion (65): `BP05B.01`, `BP05B.02`, `BP05B.03`, `BP05B.04`, `BP05B.05`, `BP05B.06`, `BP05B.07`, `BP05B.08`, `BP05B.09`, `BP05B.10`, `BP05B.11`, `BP05B.12`, `BP05B.13`, `BP05B.14`, `BP05B.15`, `BP5B.1.1`, `BP5B.1.2`, `BP5B.1.3`, `BP5B.2.1`, `BP5B.2.2`, `BP5B.2.3`, `BP5B.2.4`, `BP5B.2.5`, `BP5B.2.6`, `BP5B.3.1`, `BP5B.3.2`, `BP5B.3.3`, `BP5B.3.4`, `BP5B.3.5`, `BP5B.4.1`, `BP5B.5.1`, `BP5B.5.2`, `BP5B.5.3`, `BP5B.5.4`, `BP5B.5.5`, `BP5B.5.6`, `BP5B.5.7`, `BP5B.5.8`, `BP5B.5.10`, `BP5B.6.1`, `BP5B.7.1`, `BP5B.8.1`, `BP5B.9.1`, `BP06.01`, `BP06.02`, `BP06.03`, `BP06.04`, `BP06.05`, `BP06.06`, `BP06.07`, `BP06.08`, `BP06.09`, `BP06.10`, `BP06.11`, `BP06.12`, `BP6.1`, `BP6.2`, `BP6.3`, `BP6.4`, `BP6.5`, `BP6.6`, `BP6.7`, `BP6.8`, `BP6.9`, `BP6.10`
- Criteria with no operation and no process requirement: 15, listed in the review.

## The data models (CDMs)

| API | CDM | Register status | Approved | Branch |
|---|---|---|---|---|
| catalogue-be | catalogue-be.md | In review | No | feature/add-cdm |
| query-mapper-adapter-be | query-mapper-adapter-be.md | In review | No | feature/add-cdm |
| catalogue-client-be | catalogue-client-be.md | In review | No | feature/add-cdm |

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

