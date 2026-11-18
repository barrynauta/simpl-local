# Spec feedback: Vocabulary Manager

Step 29 of the spec factory, assembled on 18 Nov 2026 by `spec-factory/pipeline/build_feedback.py` (engine c14ceb7). For the owners of the stories, the API definitions, the CDMs and the business process. Every point cites the ids it is about and the question that needs an answer; the full evidence is in the linked files.

**What this is.** An automated reading of the published specifications, as a builder would read them, before anything is built. A finding says where two careful readers would build different things, or where a builder would have to guess. It is not a judgement on the people who wrote the specs: most points are normal for specifications written by several teams over time.

## In short

- **Input:** 34 stories with 85 live acceptance criteria, 19 API operations, 25 business process requirements, and the CDMs of each API.
- **Findings:** 94: **14 to decide before building**, 48 to confirm, 32 small.
- **Mechanical checks:** 26 hits on the acceptance criteria and 34 on the API and CDM names; the agents triaged them, and the real ones are part of the findings above.
- **Coverage:** 3 of 85 criteria link to no operation and no process requirement; 1 of 19 operations and 25 of 25 process requirements have no criterion.

## Decide first

The blockers. A builder or tester cannot proceed on these without a decision, and two builders would decide differently.

### Behaviour and interactions

**F-001** (AC-C03). The specs use two status levels, vocabulary status and version status, but no criterion says how they relate. A builder cannot tell what deprecating or reactivating a vocabulary does to its versions, which status the list shows, or whether a deprecated vocabulary can get a new version.

- Where: cross
- Question: When the vocabulary status is set to DEPRECATED or back to ACTIVE, what happens to the status of each version, which status does GET /vocabularies show, and can a new version be created for a DEPRECATED vocabulary (and if so, does the vocabulary become ACTIVE)?
  - Option: Vocabulary status is a separate flag; version statuses do not change
  - Option: Deprecating the vocabulary deprecates all versions; reactivating restores only the latest version to ACTIVE
  - Option: Vocabulary status is derived from the latest version status (one status, not two)

**F-002** (AC-C03). The role model is named five different ways and the criteria do not say which role may read and which may write. With a read-only role and an admin role in the notes, the blanket 403 rule can be read two ways.

- Where: cross
- Question: Which roles exist for this component, which operations and screens does each role allow, and is 'the vocabulary management role' in the criteria GA_VOCABULARY_ADMIN only, or either of the two roles? Is the check done by the gateway (Tier 1) or by the service?
  - Option: GA_VOCABULARY_VIEWER may call all GET endpoints and see all read screens; only GA_VOCABULARY_ADMIN may create, version or change status; anyone else gets 403
  - Option: Only GA_VOCABULARY_ADMIN may use the API and UI at all (viewer role not built)
  - Option: Any authenticated Governance Authority user may read (SIMPL-18559 'any role'); write needs GA_VOCABULARY_ADMIN

**F-003** (AC-C05). The criterion blocks a new version that references a deprecated vocabulary version, while the OpenAPI creates the version and returns a warning for exactly that case, and the process says deprecated vocabularies can still be referenced.

- Where: SIMPL-18184/AC2
- Question: Does a reference to a DEPRECATED internal vocabulary version block creation (400) or create the version with a warning (201 with validationWarnings)?
  - Option: Block: 400 with one issue per deprecated reference, nothing created (story)
  - Option: Warn: 201 with a deprecatedVocabularyReference warning per reference (OpenAPI and BP)

**F-004** (AC-C03). The negative scenario returns 'errors or warnings' and then does not create the vocabulary, so it reads as if warnings also block. The story note and the OpenAPI treat warnings as non-blocking.

- Where: SIMPL-18183/AC2
- Question: Do semantic warnings (for example an unresolved external namespace) stop creation, or is creation refused only when there is at least one semantic error?
  - Option: Only errors block; warnings are returned in the 201 response
  - Option: Errors and warnings both block creation

**F-006** (AC-C05). Validation must run when a file is uploaded, before submission, but the API offers no validate-only operation and the API story states there is no separate validation endpoint.

- Where: SIMPL-18180/AC1
- Question: Where does validation on upload run: in the browser, through a new validate-only endpoint, or not at all (validation only on submit)?
  - Option: UI parses the Turtle file in the browser on upload (syntax only); full validation on submit
  - Option: Add a validate-only endpoint to the API
  - Option: No validation on upload; AC1 is met by validation on submit

**F-007** (AC-C05). The creation form must not show a change log field, but the API requires a changelog to create a vocabulary.

- Where: SIMPL-18169/AC1
- Question: What changelog value does the UI send when creating a vocabulary, or should changelog be optional on POST /vocabularies?
  - Option: UI sends a fixed text such as 'Initial version.'
  - Option: Make changelog optional for POST /vocabularies
  - Option: Show a change log field on the creation form

**F-008** (AC-C03). It is unclear whether a new version can be created from a metadata change alone. 'and/or' allows it, 'including ... uploading an updated file' and the API (file required) do not, and AC1 promises a form pre-filled with the content.

- Where: SIMPL-18172/AC2
- Question: Must the user upload a new file for every new version, or may the UI resend the current content when only the description changes? What does 'pre-filled with ... content' show?
  - Option: File upload is mandatory for each new version; the form shows the current file name only
  - Option: Metadata-only change is allowed; the UI resubmits the current content as the file

**F-009** (AC-C05). The UI example filters on a part of the name in lower case, while the API name filter accepts only a full PascalCase name.

- Where: SIMPL-18163/AC1
- Question: Is the name filter a case-insensitive 'contains' search or an exact name match?
  - Option: Contains, case-insensitive: the API name filter must change
  - Option: Exact PascalCase match: the UI example is wrong
  - Option: UI filters the loaded page on the client side

### One language for the domain

**D-003** (DDD-02). The CDM uses 'validation warning' for two concepts: blocking errors and non-blocking warnings. Its warning codes include syntaxError and semanticError, which the API treats as blocking errors (400, no vocabulary created). The API keeps them apart: VocabularyValidationIssue in the 400 report, VocabularyValidationWarning in the 201 response. The API warning code deprecatedVocabularyReference is not in the CDM value set. A code writer working from the CDM would return syntax errors as warnings on a created version.

- Where: vocabulary-manager-be.md#VocabularyValidationWarning
- Question: Are blocking validation errors and non-blocking validation warnings two concepts, and what is the full code set of each?
  - Option: Two CDM concepts: VocabularyValidationError (listed in the validation report, blocks creation) and VocabularyValidationWarning (returned with the created version), each with its own code set
  - Option: One CDM concept VocabularyValidationIssue with a severity (error or warning) and one code set; API schemas named after it

### Entities, identities and states

**D-001** (DDD-06). The vocabulary has three names: 'vocabulary' in the stories and the process, InternalVocabulary in the CDM and VocabularyCategory in the API. 'Category' appears in no story, no process step and no CDM class. The schema also mixes two CDM classes: it is described as the latest version metadata, but it carries the vocabulary-level status. A builder cannot tell whether this object is the vocabulary or a view of its latest version.

- Where: vocabulary-manager-be#VocabularyCategory
- Question: Is the object returned by GET /vocabularies, GET and PATCH /vocabularies/{vocabularyName} the vocabulary itself (CDM InternalVocabulary), or a read view of its latest version, and which one name should the CDM and the API use for it?
  - Option: It is the vocabulary: name the CDM class and the schema Vocabulary, and rename the field version to latestVersion (stories use 'vocabulary' most)
  - Option: It is the vocabulary: keep the CDM name InternalVocabulary and rename the schema to InternalVocabulary
  - Option: It is a read view of the latest version: add it to the CDM as a named read model (as Schema Manager does with SchemaMetadata) and name the schema after it, for example VocabularySummary

**D-002** (DDD-06). The vocabulary version has four names: 'vocabulary version' in the stories, InternalVocabularyVersion in the CDM, and VocabularyMetadata, VocabularyVersionMetadataResponse and (in part) VocabularySearchItem in the API. The API name VocabularyMetadata also clashes with the stories, where 'vocabulary metadata' means the vocabulary-level data (DDD-02).

- Where: vocabulary-manager-be#VocabularyMetadata
- Question: Which one name should the version carry in the CDM and the API, and is the creation response a separate concept or the same version object with warnings attached?
  - Option: VocabularyVersion in CDM and API (stories say 'vocabulary version'); the 201 response is VocabularyVersion plus a validationWarnings list, named as a wrapper
  - Option: Keep the CDM name InternalVocabularyVersion and use it for the API schema too

### Error cases and testability

**F-005** (AC-C02). No spec lists the syntax, structure and semantic rules, so a tester cannot build a passing and a failing vocabulary for each criterion. The UI story also names a 'structure' check that the API stories do not have.

- Where: cross
- Question: Which rules make up structure validation and semantic validation (with one example per rule), and is 'structure' a separate step from 'semantic' or the same thing under another name?
  - Option: Structure validation is part of semantic validation (two steps: syntax, semantic)
  - Option: Structure validation is a third step between syntax and semantic, with its own rule list

### API guidelines and coverage

**P-001** (COV-01). The two search operations have no story and no decision, and their contract is open on the points a tester needs. Both responses extend CollectionPage (page, pageSize, total, totalPages), but neither request takes page or pageSize, and the request arrays have no maxItems. One builder returns every match in one response, another cuts at the default page size of 20 and drops the rest without a way to fetch them. The result order, the handling of duplicate references, and the outcome of an external search body that matches more than one oneOf branch (for example namespaceUri plus namespaceUris) are not stated either.

- Where: vocabulary-manager-be#POST /vocabularies/searches; POST /external-vocabulary/searches
- Question: Are the search results paged, how many references may one request hold, in which order are results returned, and is a body that fits two oneOf branches rejected?
  - Option: No paging: all matches in one response with page 1, totalPages 1 and pageSize equal to the number of items; at most 100 references per request (400 above); results in request order, duplicates removed; a body that fits two branches returns 400
  - Option: Paging as on the GET lists: page and pageSize query parameters on the POST, default 20, maximum 100; results ordered by name then version; duplicates removed; two branches return 400

### Scope

**F-010** (AC-C06). The story is about a Schema Manager endpoint for Metadata Quality Profiles that is not in the Vocabulary Management API, and its note puts the only Vocabulary Manager part out of scope. A builder of this component cannot tell whether to build it.

- Where: SIMPL-28680
- Question: Is SIMPL-28680 part of the Vocabulary Manager build, or only of Schema Manager? If the MQP vocabulary must exist in Vocabulary Manager, is that seed data for this component?
  - Option: Out of scope for Vocabulary Manager; drop from this component
  - Option: In scope only as seed data: the MQP vocabulary is loaded into Vocabulary Manager
  - Option: In scope: build the MQP endpoints here

## Terms to agree

The draft glossary ([`glossary.md`](./glossary.md)) has 34 terms: 7 already used the same way everywhere, 20 where the draft proposes one term among synonyms, and 7 where the sources disagree on the meaning. One agreed term per concept, used the same way in the stories, the API and the CDM, removes a large part of the findings above.

Open: Latest version, Vocabulary status, Description, Creator, Structure validation, Validation warning, Vocabulary management role.

## To confirm

The majors: the build can proceed on a sensible default, but the default should be confirmed.

| Id | Theme | Where | Finding | Question |
|---|---|---|---|---|
| F-011 | Behaviour and interactions | cross | Several criteria require 403 and an authentication error, but no operation in the OpenAPI declares 401 or 403. | Are 401 and 403 returned by the gateway or by the service, with which body (problem+json?), and should the OpenAPI list them? |
| F-012 | Behaviour and interactions | cross | The process has a manual review in validation and a publish step by the Governance Authority, while the stories publish automatically right after automated validation. | Is the manual review a step outside the system (before upload), or does the component need a review or approval state before publication? |
| F-013 | Behaviour and interactions | cross | The accepted file formats and the upload size limit are not stated in any criterion. The process names RDF Schema and OWL, the API accepts Turtle only and has a 5 MB limit. | Which file formats (Turtle only, or also RDF/XML, OWL/XML, JSON-LD) and which maximum size apply, and what does the UI show when the file is too large? |
| F-014 | Behaviour and interactions | cross | The version identifier format is left to 'the versioning algorithm' and 'the same versioning library as schema management', while the API uses an integer and SIMPL-18170 starts at 1. | Is the next version always the previous integer plus 1? |
| F-016 | Behaviour and interactions | SIMPL-18164/AC1 | The date filters do not say what a match is: the same day, the exact instant, or a from-date. | For creationDate and lastUpdated, does a date match the whole day, does a date-time match exactly, or is it a lower bound? |
| F-017 | Behaviour and interactions | SIMPL-18164/AC4 | The criterion rejects an unsupported filter field, but the OpenAPI does not say that unknown query parameters are refused; many frameworks ignore them. | Must an unknown query parameter (for example ?owner=x) return 400, or is only an unsupported sort value or invalid filter value rejected? |
| F-019 | Behaviour and interactions | SIMPL-18161/AC1 | The list row shows 'date created', 'last updated' and 'creator', but the API list returns the metadata of the latest version, so these could be values of the vocabulary or of its latest version. | Do 'date created' and 'creator' in the list mean when and by whom the vocabulary was first created, or the latest version? |
| F-021 | Behaviour and interactions | SIMPL-18163/AC2 | The criterion does not say which columns are sortable or how the direction is chosen. The list shows eight columns, the API sorts on four. | Which columns are sortable, what is the default order, and does a second click reverse the direction? |
| F-022 | Behaviour and interactions | cross | 'Full details' of a downloaded version can mean the Turtle content only or the content plus metadata, and 'review its full details' has no observable outcome. | Is the download the Turtle file as returned by /content, and where does the user 'review full details' (a details screen, or the file)? |
| F-024 | Behaviour and interactions | SIMPL-18166/AC1 | The criterion asks for structure and metadata in one answer, while the API operation for a version returns metadata only and serves the content through a separate download. | Is the criterion met by the metadata response with a contentHref link to the Turtle content, or must one response carry both? |
| F-025 | Behaviour and interactions | SIMPL-18167/AC1 | 'Chronological' does not give the direction, and opening 'the history' leads to 'the vocabulary details page', which other stories call the history page. | Is the version list newest first or oldest first, and are the details page and the history page the same screen? |
| F-027 | Behaviour and interactions | SIMPL-18168/AC1 | The criterion asks for a list of all versions with 'metadata needed for comparison', while the API returns a paginated list with a fixed field set. | Is a paginated answer acceptable for 'all versions', and which fields count as 'metadata needed for comparison'? |
| F-028 | Behaviour and interactions | SIMPL-18176/AC2 | The criterion assumes the user can search and filter vocabulary versions, but no UI story and no API operation offers search or filter on versions. | Is browsing the history of one vocabulary enough, or is a search or filter over versions required? |
| F-029 | Behaviour and interactions | SIMPL-18176/AC3 | The criterion shows the creator of a version, but the API version metadata has no creator field. | Should version metadata include createdBy, or is the creator not shown for versions? |
| F-031 | Behaviour and interactions | SIMPL-18184/AC4 | The UI has no control to select a referenced vocabulary version; references are inside the uploaded file. It is unclear when the warning appears. | Is the warning shown after the file is uploaded or submitted (from the API warnings), or does the form need a picker for referenced vocabularies? |
| F-034 | Behaviour and interactions | SIMPL-18181/AC2 | 'not created as an ACTIVE vocabulary' suggests the vocabulary could still be stored in another state after a syntax error. | After a syntax error, is nothing stored at all, or is a non-active record kept? |
| F-037 | Behaviour and interactions | SIMPL-18173/AC6 | 'Unsupported format' can mean a wrong request content type (415) or a file that is not Turtle (400). | Which case does this criterion test, and which status code is expected? |
| F-039 | Behaviour and interactions | SIMPL-18195/AC2 | 'hidden or disabled' gives two behaviours, and an error on an attempt is only possible if the action is visible. | Are restricted actions hidden or shown disabled, and when does the authorization error message appear (for example on a direct URL)? |
| D-006 | One language for the domain | cross | 'Latest version' can mean two things: the most recently created version (CDM, API), or the most recent ACTIVE version (stories). The stories also say 'current' and 'newest'. Once a version status can be changed by hand, the latest version may be DEPRECATED, and the two meanings give different results for the list, the download and the search. | Does 'latest version' mean the highest version number whatever its status, or the most recent ACTIVE version, and should 'current' and 'newest' be retired? |
| D-004 | Entities, identities and states | vocabulary-manager-be#VocabularyValidationProblem | The validation report is a business concept in the process and the stories, but the CDM has no class for it, and it has five names across the sources. The API carries it in an error wrapper, which is fine as a transport, but the concept behind it is missing from the CDM. | Should the CDM name the validation report as a (transient) concept, and which name should the stories and the API use? |
| D-005 | Entities, identities and states | cross | The vocabulary reference drives the status check of the process and the deprecation warning, but the CDM has no concept for it. It is not stated how a reference inside the uploaded content identifies an internal vocabulary version. The API examples suggest the namespace of an internal version is its content URL, while the CDM gives a namespace only to external vocabularies. | What is a vocabulary reference (a namespace prefix in the content?), how does it point to a specific internal vocabulary version, and should the CDM model it? |
| D-007 | Entities, identities and states | cross | The states of the vocabulary and of the version are listed in the CDM, but the allowed transitions are not written down in one place, and the CDM text and the stories differ on what a new version deprecates. Each operation that changes a status cannot be checked against a transition table. | Can the spec owners add one transition table per entity (vocabulary, vocabulary version) next to the CDM, naming for each transition the operation and the criteria that cause it? |
| D-008 | Entities, identities and states | vocabulary-manager-be#ExternalVocabulary | The identity of an external vocabulary is stated two ways. The CDM says it is identified by its canonical namespace URI (plus aliases); the API addresses it by name in every path and also looks up its content by namespace URI. For the internal vocabulary the identity is the name, but it is called vocabularyName in paths and name in bodies, queries and the search, while the external path parameter is called name. | Is an external vocabulary identified by its name or by its namespace URI, and should the name parameter of both vocabulary kinds use one spelling? |
| D-010 | Boundaries with other components | cross | The ownership of 'vocabulary' is not yet single. Schema Manager still exposes legacy /vocabularies endpoints, and two stories in this set speak the catalogue's and Schema Manager's language (ontology, schema, SHACL constraints, uploads to the catalogue). The CDM says Vocabulary Manager is the authoritative registry. | Does this component own every vocabulary in the data space, and when are the Schema Manager /vocabularies endpoints retired? Should SIMPL-393, SIMPL-395 and SIMPL-28680 be restated in this component's language or moved? |
| F-015 | Error cases and testability | cross | The API lets a single version status be changed by hand, but no story covers it, and the stories assume only the newest version is ACTIVE. A builder must guess whether two versions may be ACTIVE and which ones a new version deprecates. | Is manual change of a version status in scope, may more than one version be ACTIVE, and does a new version deprecate all previous versions or only the active one? |
| F-018 | Error cases and testability | SIMPL-18162 | Pagination limits are not named: there is no maximum page size and no rule for a page past the last one. | What is the maximum pageSize, and what does a request for a page beyond totalPages return (200 with empty items, or 400)? |
| F-020 | Error cases and testability | SIMPL-18161 | The UI list story does not cover an empty list or more vocabularies than one page, while the API is paginated with a default of 20. | Does the UI page through the list (and with which page size), and what does it show when there are no vocabularies? |
| F-023 | Error cases and testability | SIMPL-18452/AC1 | 'I can access that selected version' does not say what is shown, so there is no pass condition. | What does the user see after selecting a version (which fields, which actions)? |
| F-026 | Error cases and testability | SIMPL-18167 | The history story has no criterion for a load failure or a vocabulary that is not found, while the list story has one for errors. | What does the history page show when loading fails or the vocabulary is not found? |
| F-030 | Error cases and testability | SIMPL-18178/AC1 | The criterion does not say where the warning appears or what it looks like, and the list already shows the status, so a tester cannot tell what passes. | On which screens is the warning shown (list row, details page, download), in which form (icon, banner, tooltip), and with what text? |
| F-032 | Error cases and testability | SIMPL-18184 | The status check covers only new versions, while the process asks for it on new vocabularies too, and the criteria do not say how a reference to an internal vocabulary version is recognised in the file. | Does the deprecated-reference check also run on POST /vocabularies, and is a reference internal when its namespace equals a vocabulary contentHref (latest or versioned)? |
| F-033 | Error cases and testability | SIMPL-18181/AC1 | The pass condition 'the API response or processing state indicates that syntax validation has passed' has no field in the API, and 'or' leaves two places to look. | Is a 201 response the indicator that syntax validation passed, or is a field needed? |
| F-035 | Error cases and testability | SIMPL-18169/AC2 | The field format rules and the error messages are not named in the story, so 'field formats are correct' and 'appropriate error messages' have no oracle. The API has the name rule. | Which format rules apply per field (name as in the OpenAPI? description length?), is uniqueness case-sensitive, and where is each error shown (per field or as one message)? |
| F-036 | Error cases and testability | SIMPL-18170 | The API create story has no criterion for a duplicate name, a file that is too large, or a wrong content type, which the OpenAPI answers with 409, 413 and 415. The same gaps apply to new versions in SIMPL-18173. | Confirm that a duplicate name returns 409, an oversized file 413 and a wrong content type 415, as in the OpenAPI. |
| F-038 | Error cases and testability | cross | Neither status story says what happens when the requested status equals the current one; the OpenAPI answers 409 for that case. | Does a request for the current status return 409 (API) and does the UI offer only the other status? |
| F-040 | Error cases and testability | SIMPL-18193 | The UI report story covers errors only. The API can return non-blocking warnings on success, and the story does not say whether or how the UI shows them. | Does the UI show validation warnings after a successful creation, and where? |
| D-009 | Data model governance | vocabulary-manager-be | The CDM of this component and the context CDM of Schema Manager exist and point to their APIs, but neither is approved for delivery. A glossary built on them can still change. | When will the Vocabulary Manager CDM be approved, and should the factory wait for it or work from this snapshot? |
| P-003 | API guidelines and coverage | vocabulary-manager-be#components/securitySchemes/OAuth2Auth | The security scheme does not fit the decided access model. It declares an OAuth2 authorization code flow with placeholder URLs and two scopes, read and write, while D16 grants access by two Keycloak realm roles and states that the scopes are not checked. A client generated from the file will ask for scopes that mean nothing, and a builder can reasonably enforce read and write scopes instead of the roles. SEC-07 asks for OpenID Connect through the Tier 1 Keycloak; the file names neither. |  |
| P-004 | API guidelines and coverage | vocabulary-manager-be#external vocabulary operations; PATCH /vocabularies/{vocabularyName}/versions/{version} | Seven operations are reached by no acceptance criterion except the blanket access rules of SIMPL-18196. For six of them, the external vocabulary operations, no story exists at all; D30 fixes their behaviour, but there are no criteria for listing, reading, reading content by name or deleting an external vocabulary. The manual version status change is decided in D4 but also has no story. 21a counted these operations as covered because the inferred SIMPL-18196 links point at every secured operation. | Which story holds the acceptance criteria for registering, listing, reading and deleting external vocabularies, and for the manual version status change? |
| F-041 | Scope | SIMPL-393 | The story has no Given/When/Then criteria and an open question with the customer. It is unclear whether this component must ship the CPSV vocabulary as seed data. | Must Vocabulary Manager be deployed with the Core Public Services vocabulary pre-loaded, and is GET /vocabularies listing it the acceptance test? |
| F-042 | Scope | SIMPL-395 | The story has no criteria, is still in grooming, and asks for things the other stories and the API do not have: removing a vocabulary and a UI that wraps the catalogue's vocabulary calls. | Is SIMPL-395 replaced by the SIMPL-181xx stories, or does it still add a requirement (deletion, catalogue integration)? |
| B-001 | Business process flow | BP02B (Overview, Assumptions); /external-vocabularies operations | The process names the registration and use of external vocabularies in its overview, but no step and no requirement covers registering or removing one. So the process does not say who registers an external vocabulary, that this must happen before an internal vocabulary that references it is created, or what a removal does to the vocabularies and lookups that still use it. | Should BP02B get a step 'Register external vocabulary' (and one for removal), placed before BP02B.01 and BP02B.04, with the Governance Authority admin role as actor? When an external vocabulary is removed while internal versions reference it, is the removal allowed, refused, or followed by a warning to the Governance Authority? |
| B-002 | Business process flow | BP02B.05; BP2B.6; vocabulary and version state tables | The decided states let a vocabulary and a version go back from DEPRECATED to ACTIVE, and let one version be deprecated by hand through the API only. No process step reaches these transitions: the process only deprecates. The stories that restore ACTIVE (and the manual version change, which has no story) have no clear place in the process. | Is reactivating a vocabulary or a version a governance action of BP02B? If yes, which step and requirement hold it, and may the Governance Authority deprecate or reactivate a single version from the UI, or only through the API? |
| B-005 | Business process flow | BP02B Outcomes; BP02B.03, BP02B.05 | The process ends with a vocabulary 'provided for semantic schema validation' and with older versions deprecated, but it does not say how the consumers learn this. Schema Manager reads vocabularies through the search and content operations; no event is named for publication or deprecation. If a consumer keeps a copy, a deprecation or a new version stays unseen. The providers whose schemas use a version that becomes deprecated are not told either. | Do consumers read the vocabulary state at use time only (no copy kept), or must Vocabulary Manager publish an event when a version is published or a vocabulary or version is deprecated? Who tells the providers whose schemas reference a version that has just been deprecated? |
| B-006 | Business process flow | BP02B.04, BP02B.05 (automatic deprecation) | Creating a version deprecates every older ACTIVE version at once, which puts a cost on everyone whose schemas use them. There is no limit and no check on this flow: no cap on versions, no refusal of a version identical to the latest, and no confirmation before the deprecating submit. A script or a mistake with the admin role can churn versions and deprecate the version in use within seconds. No abuse case is written as a criterion or decision. | Should version creation have a limit (per vocabulary or per time), a check that refuses a version whose content and description equal the latest version, and a confirmation in the UI that names the versions it will deprecate? Which abuse cases should become criteria? |
| P-002 | Other | vocabulary-manager-be | Step 30 decided several behaviours that the OpenAPI file does not yet show. A builder who generates server stubs, clients or contract tests from the file builds the old behaviour: no 401 or 403 responses, a strict PascalCase name filter, an unbounded pageSize, no creator on version objects, an optional validationWarnings with a free-text code, unbounded description and changelog, a version form field that the schema does not forbid, a nullable latestVersionHref, and silence on unknown query parameters. |  |
| B-003 | Other | BP02B.03, BP02B.04, BP02B.05 (version creation and automatic deprecation) | The outcome of two changes to the same vocabulary at the same time is not defined. Version numbers are 'highest plus 1' and a new version deprecates every older ACTIVE version, but nothing says what happens when two new versions are submitted together, or when a status change runs during a version creation. Two builders could give both requests the same number, store two ACTIVE versions, or refuse one. | When two version creations (or a version creation and a status PATCH) for the same vocabulary overlap, are they serialised so that each gets its own number and the later one deprecates the earlier, or does the second request fail (with which status, for example 409) and store nothing? |
| B-004 | Other | BP02B.01 to BP02B.05 (store and identity provider) | Every step depends on the store and on the identity provider, but their behaviour when slow or down is not defined: no timeout, no retry, and no rule on what the caller sees. The outcome of a store failure is only 'nothing stored'. It is also open whether token validation fails closed when the identity provider keys cannot be fetched. | For the store and for the identity provider: which timeout and how many retries apply, does the caller get 500 or 503 (with which problem type), and does a request fail closed with 401 or 503 when the token keys cannot be fetched? |

The 32 minor findings (wording, numbering, duplicates) are in [`ac-findings.json`](./ac-findings.json) and [`domain-findings.json`](./domain-findings.json).

## Coverage

From the links between criteria, operations and process requirements (step 12b, [`../inventory/inventory-review.md`](../inventory/inventory-review.md)). A gap is not always wrong, but each one is a place where the specs do not say how the parts connect.

- Operations with no criterion (1): `vocabulary_openapi-tier1:GET /health`
- Process requirements with no criterion (25): `BP02B.01`, `BP02B.02`, `BP02B.03`, `BP02B.04`, `BP02B.05`, `BP02B.06`, `BP2B.1`, `BP2B.2`, `BP2B.3`, `BP2B.4`, `BP2B.5`, `BP2B.6`, `BP2B.1.1`, `BP2B.1.2`, `BP2B.1.3`, `BP2B.2.1`, `BP2B.3.1`, `BP2B.4.1`, `BP2B.4.2`, `BP2B.4.3`, `BP2B.4.4`, `BP2B.5.1`, `BP2B.5.2`, `BP2B.5.3`, `BP2B.6.1`
- Criteria with no operation and no process requirement: 3, listed in the review.

## The data models (CDMs)

| API | CDM | Register status | Approved | Branch |
|---|---|---|---|---|
| vocabulary-manager-be | vocabulary-manager-be.md | In review | No | feature/add-cdm |

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

