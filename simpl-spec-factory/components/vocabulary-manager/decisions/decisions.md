# Decisions: Vocabulary Manager (step 30a)

Provisional decisions for the test writer and the code writer, taken without the spec owners. Machine-readable list: [`decisions.json`](./decisions.json). Terms and states: [`glossary.md`](./glossary.md). Finding ids: F-nnn in `validation/ac-findings.json`, D-nnn in `validation/domain-findings.json`.

## Source hierarchy

When sources disagree, the higher one wins:

1. **These decisions.** They exist to settle the conflicts below.
2. **Acceptance criteria** for observable behaviour (what happens, what is shown, what is stored). If a story contradicts itself (for example SIMPL-18184 AC2 against AC4), it does not settle the point and the next source decides.
3. **OpenAPI** (`vocabulary_openapi-tier1.yaml`) for the wire format: paths, field names, status codes, error bodies, media types. Where a criterion needs behaviour the OpenAPI cannot carry, a decision extends the OpenAPI (D17, D19, D27) and says so.
4. **CDM** (`vocabulary-manager-be.md`, not yet approved) for the meaning of concepts and the glossary terms.
5. **Technical notes** in the stories (roles, Fuseki, versioning library).
6. **Business process** BP02B, for intent where nothing above speaks.

Why: the criteria are what the tests check, so they lead on behaviour; the OpenAPI is the only exact source for the wire; the CDM is in review and the process is high level. Where all sources are silent, the decision takes what is simplest to build and test and says so.

Totals: 65 decisions (52 provisional, 13 follows the specs), of which D38 to D41 were added after step 21, D42 to D46 after step 22, and D47 to D61 and E4 after run 4. Finding ids P-nnn are in `validation/api-findings.json`, B-nnn in `validation/bp-findings.json`.

## Scope

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D1 | Stories outside this build | Build only the SIMPL-181xx stories, SIMPL-18452 and SIMPL-18559. SIMPL-28680 (Metadata Quality Profile) belongs to Schema Manager and is not built or tested here. SIMPL-393 is met by the generic upload: no seed data, the component starts empty; a test may upload a CPSV-like Turtle file and find it with GET /vocabularies. SIMPL-395 is replaced by the SIMPL-181xx stories: no deletion of internal vocabularies, no wrapping of catalogue calls. Vocabulary Manager is the owner of internal and external vocabularies in this build; the legacy Schema Manager /vocabularies endpoints are out of scope. The pairwise traceability tables of the two CDMs serve as the context map. | provisional | F-010, F-041, F-042, F-053, D-010, D-019 |
| D2 | CDM snapshot | Work from the pinned CDM snapshot (vocabulary-manager-be.md, commit 70f951f). Where a decision here differs from the CDM text, the decision wins for this build and the difference is listed as an owner question. | provisional | D-009 |

## Lifecycle

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D3 | Vocabulary status and version status | Two separate stored statuses. The vocabulary status is a flag on the vocabulary, set ACTIVE at creation and changed only by PATCH /vocabularies/{vocabularyName}. Changing it never changes any version status, and creating a version never changes it. GET /vocabularies, the status filter and the list UI show the vocabulary status. A new version may be created for a DEPRECATED vocabulary; the vocabulary stays DEPRECATED. For the deprecated-reference check (D11) and the UI warnings (D36) a version counts as deprecated when its own status is DEPRECATED or its vocabulary status is DEPRECATED ('effectively deprecated'). This gives BP02B.05 'Full Vocabulary Deprecation' its effect without writing to every version. | provisional | F-001 |
| D4 | Version transitions and same-status requests | Build PATCH /vocabularies/{vocabularyName}/versions/{version} as in the OpenAPI (API only, no UI control). It sets only that version's status. Creating a version sets it ACTIVE and sets every older version that is ACTIVE to DEPRECATED, in the same transaction, with their lastUpdateDate set to the creation time. After a manual reactivation more than one version may be ACTIVE; the next new version deprecates all of them. A PATCH (vocabulary or version) that asks for the current status returns 409 with the ConflictResponse body, detail 'Vocabulary is already in status ACTIVE' or 'Vocabulary version is already in status DEPRECATED'. A status value other than ACTIVE or DEPRECATED returns 400. The UI status-change action offers only the other status. The state tables are in glossary.md. | provisional | F-015, F-038, D-007 |
| D5 | Meaning of latest version | 'Latest version' is the version with the highest version number, whatever its status. 'Latest ACTIVE version' is the highest-numbered version with version status ACTIVE and is always written out in full. GET /vocabularies, GET /vocabularies/{vocabularyName}, GET /content/{vocabularyName} and the search item with latest=true use the latest version. latestVersionHref is always filled with the href of the latest version (also on the latest version itself). 'Current version' and 'newest version' are retired; the UI column is labelled 'Latest version'. | follows the specs | D-006 |
| D6 | Publication and manual review | No review or approval state. A new vocabulary or version that passes validation is stored and ACTIVE in the same request ('publish' = this automatic event). The manual review of BP02B.02 happens outside the system, before upload. UI labels: 'Active' shows ACTIVE, 'Deprecated' shows DEPRECATED; the API and the store use the upper-case values. | follows the specs | F-012, D-016 |
| D7 | Version numbering | Version numbers are integers. Version 1 at creation; each new version gets the highest existing number plus 1. A request to POST .../versions that carries a 'version' form field returns 400 (InputValidationProblem, in: body, name: version) and creates nothing (SIMPL-18190/AC2). | follows the specs | F-014 |
| D8 | Nothing stored on failure | Any 4xx or 5xx on POST /vocabularies or POST .../versions leaves the store unchanged: no vocabulary, no version, no status change of older versions. There is no draft or rejected state. | follows the specs | F-034 |

## Validation

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D9 | Validation steps and rules | Two steps on every POST /vocabularies and POST .../versions: syntax, then semantic. 'Structure validation' (SIMPL-18180) is the UI word for the semantic step; there is no third step. Syntax: the file must parse as Turtle (RDF 1.1). On failure return 400 with one issue for the first parse error, location 'line N, column M', detail starting 'Encountered invalid Turtle syntax'; semantic validation does not run. Semantic (all rules run, all errors are returned in one response): S1 a literal is the object of a property declared owl:ObjectProperty in the file (detail 'Literal value for object property'); S2 an IRI or blank node is the object of a property declared owl:DatatypeProperty in the file (detail 'Resource value for datatype property'); S3 one IRI is declared both owl:ObjectProperty and owl:DatatypeProperty (detail 'Property declared as both object and datatype property'); S4 one subject has two skos:prefLabel values with the same language tag (detail 'Duplicate preferred label for the same language'); S5 one resource is typed with two classes that the file declares owl:disjointWith (detail 'Resource is an instance of disjoint classes'); S6 the file holds no triples (detail 'Vocabulary contains no statements', location 'document'); S7 a reference to an internal vocabulary or version that does not exist (D12). Location for S1 to S5 is the offending statement or resource as in the OpenAPI example. Every error blocks. Read SIMPL-18182/AC1 as: on submit, semantic validation runs after syntax validation passes. | provisional | F-005, F-048, F-050 |
| D10 | Validation errors versus validation warnings | Two concepts. A validation error blocks: 400 VocabularyValidationProblem, one VocabularyValidationIssue per error (type urn:problem-type:belgif:input-validation:invalidInput, in: body, name: vocabularyFile, detail, location). A validation warning never blocks: it is returned in validationWarnings of the 201 response with code, description and location, and is not stored or returned by any other endpoint. Error kinds: syntax (D9 syntax) and semantic (S1 to S7). Warning codes, closed set: externalReferenceSkipped, deprecatedVocabularyReference, semanticReasoningSkipped (reserved, not emitted in this build). syntaxError and semanticError are not warning codes. A creation that has warnings and no errors returns 201. validationWarnings is always present, as an empty array when there are none. | provisional | F-004, D-003, D-011 |
| D11 | Reference to a deprecated internal version | Warn, do not block. Each reference (D12) to an internal version that is effectively deprecated (D3) gives one warning: code deprecatedVocabularyReference, description "Referenced internal vocabulary '<Name>' version <n> is deprecated." followed by " Use version <m>, the latest ACTIVE version." when a latest ACTIVE version exists, location "prefix '<prefix>' -> '<namespace>'". The version is created (201). Applies to POST /vocabularies and POST .../versions. | provisional | F-003 |
| D12 | What a vocabulary reference is | A vocabulary reference is a namespace declared with @prefix or PREFIX in the uploaded content. It is internal when it starts with '<contentBase>/content/<Name>' (any version of <Name>, latest) or '<contentBase>/content/<Name>/versions/<n>', followed by nothing, '#' or '/'; contentBase is set in E2. An internal reference to a name or version that does not exist is a validation error S7 (400, detail "Referenced internal vocabulary '<Name>' version <n> does not exist."). A reference without version resolves to the latest version. References to the vocabulary being created or versioned are ignored. Any other namespace is external. It gives warning externalReferenceSkipped unless it (after removing a trailing '#' or '/') equals the namespace URI or an alias URI of a registered external vocabulary, or is one of rdf, rdfs, owl, xsd, skos (W3C namespaces), or is the vocabulary's own namespace (a namespace that is the prefix of at least one subject IRI in the file). The check runs on both POST /vocabularies and POST .../versions. | provisional | F-032, D-005 |
| D13 | Validation on file upload in the UI | When a file is chosen in the create or new version form, the UI parses it in the browser with a Turtle parser library and shows syntax errors with line and column, or the message 'Syntax check passed.' It also refuses files over 5 MB (D15). The submit button stays disabled while the upload check shows errors. Semantic validation runs only on submit (server). No validate-only endpoint is added; the server check on submit is authoritative. | provisional | F-006 |
| D14 | Validation report and success feedback | Success indicator: the 201 response itself; no validation status field. The validation report is the 400 VocabularyValidationProblem (transient, not stored). UI on 400: a report panel above the form lists every issue with its detail and location; the form stays open. UI on 201: a success notification with the assigned version number ('Version <n> of <Name> was created.') and, when validationWarnings is not empty, a warnings panel listing each warning's description and location. This panel is where the deprecated-reference warning of SIMPL-18184/AC4 appears. | provisional | F-031, F-033, F-040, D-004 |
| D15 | File format, size and media type | Turtle only. The upload limit is 5 MB (5 242 880 bytes); above it the API returns 413 with the UploadTooLargeResponse body and the UI shows its detail text. A request that is not multipart/form-data on the POST operations (or not application/json on PATCH and the search operations) returns 415. A file part that is not valid Turtle returns 400 (syntax error). A duplicate vocabulary name returns 409 (detail "Vocabulary with name '<Name>' already exists."). SIMPL-18173/AC6 'unsupported format' is the 415 case. | follows the specs | F-013, F-036, F-037 |

## Access

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D16 | Roles and what they allow | Two Keycloak realm roles. GA_VOCABULARY_VIEWER: every GET operation and the read-only POST operations /vocabularies/searches, /external-vocabulary/searches and /external-vocabularies/content; in the UI the list, details, history and download. GA_VOCABULARY_ADMIN: everything the viewer can do plus POST /vocabularies, POST .../versions, both PATCH operations, POST and DELETE /external-vocabularies; in the UI create, new version and status change. An authenticated user with neither role gets 403 on every secured operation; a viewer gets 403 on write operations. 'The vocabulary management role' in the criteria means GA_VOCABULARY_ADMIN for writes and either role for reads; 'any role' (SIMPL-18559) means either role. The OAuth2 scopes read and write in the OpenAPI are not checked. The Governance Authority is an external actor; its users are the users holding these roles. | follows the specs | F-002, D-018 |
| D17 | Where 401 and 403 come from | The service validates the bearer token (JWT from Keycloak, E1) and the roles itself, so the rules can be tested without a gateway; in production the Tier 1 gateway applies the same rules first. No or invalid token: 401, problem+json, type urn:problem-type:simpl:unauthorized, title 'Unauthorized'. Valid token, missing role: 403, problem+json, type urn:problem-type:simpl:forbidden, title 'Forbidden'. Authentication and role checks run before body validation. GET /health needs no token and is outside the access rules. | provisional | F-011, F-052 |
| D18 | Restricted actions in the UI | For a user without GA_VOCABULARY_ADMIN the actions 'New Vocabulary +', 'Update' (new version) and status change are hidden. Opening the URL of the create or update form directly shows the message 'You are not authorized to perform this action.' Any 403 from the API shows the same message. A user without a token is sent to the Keycloak login page. | provisional | F-039 |

## List and queries

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D19 | Name filter | The name query parameter of GET /vocabularies is a case-insensitive 'contains' match on the vocabulary name. Its schema is relaxed to pattern ^[A-Za-z0-9]+$, minLength 1, maxLength 64; other values return 400. The UI name filter sends the typed text as is. | provisional | F-009 |
| D20 | Date filters | creationDate and lastUpdated filters: a date (YYYY-MM-DD) matches every value on that UTC day; a date-time matches values equal to it, to the second. A value matching the pattern but not a valid date returns 400. | provisional | F-016 |
| D21 | Unknown query parameters | On every GET operation an unknown query parameter returns 400 InputValidationProblem with one issue (in: query, name: the parameter). Invalid values of known parameters also return 400. | follows the specs | F-017 |
| D22 | Pagination limits | page minimum 1, default 1; pageSize minimum 1, maximum 100, default 20. Values outside these bounds or not integers return 400. A page beyond the last returns 200 with an empty items array and the correct total and totalPages. Paging links: self and first always; prev, next and last when they exist (last is omitted when total is 0). | provisional | F-018 |
| D23 | Sorting and default order | GET /vocabularies sorts by the sort values in the OpenAPI; default is name ascending. lastUpdated sorts on lastUpdateDate. Ties are broken by name ascending. GET .../versions default order is version number descending (newest first); its sort values sort on that field with ties broken by version descending. UI list: sortable columns Name, Status, Date created, Last updated; first click ascending, second click descending; default Name ascending. Other columns are not sortable. | provisional | F-021, F-025 |
| D24 | Status codes for generic criteria | 'Validation error' and 'rejected' in the criteria mean 400; 'could not be found' means 404 with the VocabularyNotFoundResponse body; 'rejected due to insufficient permissions' means 403; 'not authenticated' means 401 (D17). UI tests for 'no longer available' (SIMPL-18174/AC2) stub the API to return 404 for the PATCH; UI error tests stub 500. | follows the specs | F-046, F-049 |

## Data and naming

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D25 | The vocabulary object (VocabularyCategory) | The object returned by GET /vocabularies, GET and PATCH /vocabularies/{vocabularyName} is the vocabulary (CDM InternalVocabulary), with some values taken from its latest version. The wire format stays as in the OpenAPI; code and tests call it 'vocabulary'. Field meaning: name; version = latest version number; description = latest version description (D28); status = vocabulary status (D3); createdBy and creationDate = creator and creation time of the vocabulary (version 1); lastUpdateDate = time of the last change to the vocabulary or any of its versions (new version, either status change); href = /vocabularies/<Name>; contentHref = <contentBase>/content/<Name>. | provisional | D-001, F-019 |
| D26 | The vocabulary version object | VocabularyMetadata is the vocabulary version (CDM InternalVocabularyVersion). VocabularyVersionMetadataResponse is the same vocabulary version plus validationWarnings (a creation wrapper). VocabularySearchItem is a search result pointing at a vocabulary or a version. Wire names stay; code and tests say 'vocabulary version'. href = /vocabularies/<Name>/versions/<n>; contentHref = <contentBase>/content/<Name>/versions/<n>; Location header = href. | provisional | D-002 |
| D27 | Creator | The creator is the user who sent the create request: the email claim of the token, or preferred_username when there is no email. It is stored on each vocabulary version and on each external vocabulary. Add createdBy (string) to VocabularyMetadata and VocabularyVersionMetadataResponse. The vocabulary shows the creator of version 1. | provisional | F-029, D-013 |
| D28 | Description | Description belongs to the version. The create request's description is version 1's description; a new version carries its own. The vocabulary description shown in lists is the latest version's description; there is no separate stored vocabulary description. | provisional | D-014 |
| D29 | Content and download | Term: vocabulary content (the Turtle definition of a version). vocabularyFile is only the upload form field. Downloads return the stored Turtle bytes as uploaded, media type text/turtle, Content-Disposition attachment filename '<Name>.ttl' (latest) or '<Name>-v<n>.ttl'. No metadata goes into the file. 'Full details' in SIMPL-18165 and SIMPL-18166 means the version metadata (JSON, with contentHref) plus the content download; full metadata is shown on the details page. | follows the specs | F-022, F-024, D-015 |
| D30 | External vocabulary identity and registration | The name is the identity of both internal and external vocabularies (paths use it). For an external vocabulary the namespace URI and each alias URI are unique lookup keys, compared after removing a trailing '#' or '/'. POST /external-vocabularies with an existing name, namespace URI or alias returns 409. Registration checks Turtle syntax only (400 on error), no semantic validation, no warnings. The verb is 'register'; 'cached copy' describes what is stored. GET and DELETE /external-vocabularies/{name} return 404 when the name is unknown. | provisional | D-008, D-020 |
| D31 | One term per concept | Use the terms of glossary.md in code, tests and UI texts. Actor: Governance Authority user. Screens: Vocabulary list (route /vocabularies) and Vocabulary details (route /vocabularies/<Name>), the details page holds the version history. Field pairs: last update date (wire field lastUpdateDate, query and sort value lastUpdated), changelog (UI label 'Change log', 'change summary' retired), creation date (UI label 'Date created'), creator (wire createdBy), warning code (wire code). 'Internal vocabulary' only where a contrast with external vocabulary is needed; ontology, native, local and self managed are retired. The identifier of SIMPL-18187 is the Location header and href of the 201 response. | follows the specs | F-043, F-044, F-045, F-051, D-012, D-017 |

## UI

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D32 | Changelog on creation | The creation form has no change log field; the UI sends changelog 'Initial version.' The API keeps changelog required. | follows the specs | F-007 |
| D33 | New version form | The 'Update' action opens a form pre-filled with the latest version's description and with the latest version's content attached as the file (shown as '<Name>.ttl (current content)'). The change log field is empty and required. The user may change the description, replace the file, or both. On submit the UI always sends a file: the new one, or the current content fetched from GET /content/<Name>. So a description-only change creates a new version. | provisional | F-008 |
| D34 | List paging and empty state | The list UI pages on the server with 20 rows per page and page controls. With no vocabularies it shows 'No vocabularies found.' On a load error it shows 'The vocabulary list could not be loaded.' and the problem detail. | provisional | F-020 |
| D35 | Details and history page | One screen, Vocabulary details, opened by 'View vocabulary details' or 'history'. Header: name, vocabulary status, description of the latest version. Below: the version list, newest first, columns Version, Status, Date created, Description, Change log, Actions (Download). The UI loads versions with pageSize 100 and fetches further pages when total is higher. Selecting a version opens a panel with version, status, date created, creator, description, change log and Download. No search or filter on versions; browsing this list meets SIMPL-18176/AC2. Errors: 404 shows 'Vocabulary not found.' with a link back to the list; other errors show 'The version history could not be loaded.' and the problem detail. | provisional | F-023, F-026, F-027, F-028 |
| D36 | Deprecation warnings in the UI | Vocabulary list: a row whose vocabulary status is DEPRECATED shows a warning icon next to the status with the text 'This vocabulary is deprecated. Its use is discouraged.' The details page of such a vocabulary shows the same text as a banner. Version list and version panel: an effectively deprecated version (D3) shows a warning 'This version is deprecated.' followed by 'Latest ACTIVE version: <n>' as a link to that version when one exists (SIMPL-18559/AC1); without an ACTIVE version only the first sentence (AC2). | provisional | F-030, F-047 |
| D37 | Form field rules and messages | Name: required, 3 to 64 characters, ^[A-Z][a-zA-Z0-9]*$, unique case-insensitively (409 otherwise). Description: required, 1 to 1000 characters after trimming. Changelog: required on new versions, 1 to 1000 characters after trimming. The API applies the same limits (400). The UI shows field errors under the field before submit; server errors (400 report, 409, 413) appear in a message above the form. | provisional | F-035 |

## Added after step 21 (17 Nov 2026)

Step 21b (API review) ran after step 30 and reopened the gate with one blocker (P-001) and three majors (P-002 to P-004). The decisions above stand. D39 and D40 add no new behaviour: they record that the decisions win over the OpenAPI file and what the API owner should change. D41 also closes the minors P-005 and P-006, because the tests of the external vocabulary operations meet them.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D38 | Search operations: paging, limits, order | Neither search is paged. POST /vocabularies/searches takes 1 to 100 items; POST /external-vocabulary/searches takes at most 100 URIs in total (namespaceUris plus aliasUris, each array with at least 1 entry when present). More returns 400 (InputValidationProblem, in: body, name: items, namespaceUris or aliasUris). Every reference matches at most one result, so all results fit in one response: page 1, pageSize 100, total = number of items returned, totalPages 1 (0 when total is 0), and no self, first, prev, next or last links. page, pageSize or any other query parameter returns 400 (as D21). Internal search: results in request order; a missing name or version is left out (as the OpenAPI says); a repeated reference (same name and same version, or same name both without version) gives one result at its first position; {name} and {name, version} are different references and give two results even when the version is the latest. A result with latest true carries the latest version number and the vocabulary status (as D25); a result with latest false carries that version's own status. External search: URIs are normalised as in D30 (trailing '#' or '/' removed) and looked up against the namespace URI and the alias URIs; results in the order of the first matching URI, namespaceUris before aliasUris; an external vocabulary matched by several URIs appears once. A body that fits more than one oneOf branch (for example namespaceUri with aliasUri, or namespaceUri with namespaceUris) or none (empty object) returns 400, as JSON Schema oneOf requires. Roles as D16. | provisional | P-001 |
| D39 | Decided behaviour the OpenAPI file does not yet show | The decisions win over the OpenAPI file (source hierarchy). Writers build and test the decided behaviour, not the file, on these points: 401 and 403 on every secured operation (D17); name filter relaxed (D19); pageSize maximum 100 (D22); createdBy on version objects (D27); validationWarnings always present with the closed code set (D10); description and changelog 1 to 1000 characters (D37); version form field rejected (D7); latestVersionHref always filled (D5); unknown query parameters rejected (D21); search limits (D38). The writers do not edit the file in specs/api. A writer who generates stubs, clients or contract tests from it adds these points by hand in its own build. No decision is changed by this entry. | follows the specs | P-002 |
| D40 | Security scheme in the build | Treat the security scheme as OpenID Connect through the Keycloak realm of E1. Clients (the UI and the tests) get a bearer token from Keycloak and request no scopes beyond openid; the scopes read and write are neither requested nor checked (D16). The backend validates the token and the realm roles (D17). Writers ignore the placeholder URLs of OAuth2Auth; a generated client or API console is configured with the Keycloak URLs of E1 and E2. | follows the specs | P-003 |
| D41 | Test basis for operations without a story | The OpenAPI responses plus D4, D16, D17, D22, D27, D29 and D30 are the test basis for the external vocabulary operations and the manual version status change; the missing stories are a backlog item, not a blocker. External vocabularies: POST /external-vocabularies returns 201 with Location and href /external-vocabularies/<Name> and the ExternalVocabulary body with createdBy and creationDate; invalid Turtle returns 400 with one issue as the D9 syntax error (in: body, name: vocabularyFile), no semantic errors and no warnings; 409, 413 and 415 as D15 and D30. GET /external-vocabularies is paged as D22, default and only order name ascending, no filter or sort parameters (others 400 as D21). GET /external-vocabularies/{name} returns 200 with ExternalVocabulary. GET /external-vocabularies/{name}/content returns the stored Turtle bytes, text/turtle, filename '<Name>.ttl'. DELETE /external-vocabularies/{name} returns 204 and removes metadata and content; a later GET returns 404; internal vocabularies already stored are not touched. Every 404 on an external vocabulary path uses the ExternalVocabularyNotFoundResponse body with detail "External vocabulary with name '<Name>' was not found." and one issue (in: path, name: name). Version status change: as D4, and 404 (VocabularyNotFoundResponse) for an unknown name or version. | provisional | P-004, P-005, P-006 |

## Added after step 22 (17 Nov 2026)

Step 22b (business process review of BP02B) ran after step 30 and reopened the gate with six majors (B-001 to B-006) and no blockers. The decisions above stand. B-002 (reactivation and manual version deprecation have no process step) is already settled by D3 and D4: reactivation stays a correction outside the BP02B steps, the vocabulary status in the UI and the API, a version status in the API only. D4 now also closes B-002; the process question is listed below. D44 refines D8: a write that times out after the store received it may have committed, all or nothing, and the client checks with GET. The minors B-007 to B-009 ask the process owner to restate the text and need no decision.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D42 | External vocabularies in the process | Registering and removing an external vocabulary is an administration action outside the BP02B steps in this build. A user with GA_VOCABULARY_ADMIN (D16) does it at any time through the operations of D41. The system enforces no order: an internal vocabulary or version created before the external vocabulary is registered is stored, with warning externalReferenceSkipped for that namespace (D12); after registering, the Governance Authority may create a new version to clear the warning. Removal is allowed while internal versions reference the namespace: DELETE /external-vocabularies/{name} returns 204 with no reference check and no warning (as D41). Stored internal versions keep their content and status. A later creation or new version that references the removed namespace gets externalReferenceSkipped. | provisional | B-001 |
| D43 | Concurrent changes to one vocabulary | Changes to the same vocabulary are serialised: POST .../versions, PATCH /vocabularies/{vocabularyName} and PATCH .../versions/{version} for one vocabulary run one after another, and each sees the stored result of the one before. So two overlapping version creations both succeed with 201 and get numbers n+1 and n+2; the later one deprecates the earlier one (D4); the store never holds two versions with the same number. A version creation and a status PATCH that overlap give the result of running them in one order or the other; both orders are valid outcomes (D4 allows two ACTIVE versions after a reactivation). Two identical PATCH requests that overlap give 200 and then 409 (same status, D4). Two POST /vocabularies with the same name (compared case-insensitively, D37) that overlap give one 201 and one 409; only one vocabulary is stored. Changes to different vocabularies are not serialised against each other. Each request runs its whole change in one store transaction, so a failure leaves nothing half done (D8). This build runs one backend instance: a lock per vocabulary name (lower case) in the service plus one store transaction per request meets the rule. Time spent waiting for the lock counts towards the request timeout of D44; when it runs out the request returns 503 as D44 and stores nothing. No new status code: overlapping creations never return 409. Test: with version 1 stored, send 5 POST .../versions in parallel; expect five 201 responses, versions 2 to 6 each exactly once, distinct Location headers, version 6 ACTIVE and versions 1 to 5 DEPRECATED. | provisional | B-003 |
| D44 | Store and identity provider slow or down | Store (Fuseki, E3): connect timeout 2 s, response timeout 10 s per store call. Read-only store queries are retried at most 2 times on a connection failure or timeout, after 200 ms and then 400 ms. Store updates are never retried by the service. When the store stays unreachable or times out, the operation returns 503, application/problem+json, type urn:problem-type:belgif:serviceUnavailable, title 'Service Unavailable', detail 'The vocabulary store is not available. Retry later.', with header Retry-After: 5. A store error that is not a timeout or a connection failure stays 500 (InternalServerErrorResponse). Each write is one store transaction, so after a 503 the store holds the whole change or none of it; when the store was unreachable it holds none (D8). After a 503 on a write the client checks with GET before it resubmits. Identity provider (Keycloak, E1): the service validates tokens locally with the realm keys. It fetches the keys at start and again when a token names an unknown key id, timeout 2 s, no retry inside a request. Cached keys keep working while Keycloak is down. When a token names a key id that is not cached and the keys cannot be fetched, the request fails closed with 503 (same body, detail 'The identity provider is not available. Retry later.'); it is never accepted without a signature check. This runs before the role and body checks (D17). GET /health: 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when the store answers but no realm keys are loaded and Keycloak does not answer; otherwise 200 UP. UI: a 503 is shown like a 500 (the generic error with the advice to retry or contact support, SIMPL-18172/AC3, D24). Tests: start the backend with the store URL on a port where nothing listens; GET /vocabularies and POST /vocabularies return 503 with this body and Retry-After, and GET /health returns 503 DOWN. The identity provider case is covered by unit tests in the code, not by the API tests. | provisional | B-004 |
| D45 | How consumers learn of publication and deprecation | Pull only. Vocabulary Manager publishes no events in this build. Consumers (Schema Manager, D1) read the state at use time through the search operations (D38) and the content operations (D29). The content of a stored version never changes, so a consumer may keep a copy of it, but it reads the status again through a search each time it validates a schema. Telling the providers whose schemas use a version that has just been deprecated is an off-line task of the Governance Authority, like the email route of BP02B Assumptions; the system sends nothing. The deprecation warnings that consumers see come from the status in the search results and the warnings of D11 and D36. | provisional | B-005 |
| D46 | Limits and abuse cases on version creation | Identical-version check: POST .../versions is refused with 409 (ConflictResponse body, type urn:problem-type:simpl:conflict, detail "Version <n> of vocabulary '<Name>' already has this content and description.") when the uploaded file bytes equal the stored content bytes of the latest version (D5) and the trimmed description equals its description. The changelog is not compared. Nothing is stored (D8). The check runs after the authentication, role, 404 and field checks (400, 413, 415) and before validation. A description-only change still creates a new version (D33). UI: no confirmation step (SIMPL-18172/AC2 submits and creates); the new version form shows the fixed line 'Submitting creates version <n+1> and deprecates every older ACTIVE version.', where n is the latest version number; a 409 appears in the message above the form (D37). No call-rate limit in the service in this build; in production the Tier 1 gateway applies the platform call-rate limit (OWA-API4), outside this build. Abuse cases, as decisions: (1) resubmitting the latest version unchanged returns 409 and deprecates nothing; (2) a viewer or a user without a role who tries to create a version gets 403 (D16, D17); (3) an admin who churns changed versions is not stopped by the service; recovery is a PATCH back to ACTIVE (D4) and the actions are traceable through createdBy (D27). | provisional | B-006 |

## Feedback from run 4 (added 18 Nov 2026)

The 22 owner questions of run 4 (`runs/run4/report/REPORT.md`, section 6) are findings R-001 to R-022 in `validation/run-findings.json`. These decisions close them. Earlier decisions are not rewritten; they carry `refined_by` and `later_change` in `decisions.json`.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| E4 | Run environment from the build profile | The build profile of the run (build.yaml) fixes ports, URLs, issuer and the test users' access; E2's ports and URLs are defaults only and every value is read from environment variables, so code and suite run unchanged on any ports. The store is never reset by the tests: every scenario creates its own data with a unique run id and does not depend on an empty store (counts and lists are checked through filters on its own names). E3's reset sentence is withdrawn; the rest of E3 stands. | provisional | R-001, R-002 |
| D47 | Deprecated and missing reference texts | Deprecated reference (D11): the description is "Referenced internal vocabulary '<Name>' version <n> is deprecated.". The sentence " Use version <m>, the latest ACTIVE version." is added only when the vocabulary status is ACTIVE and a version <m> other than <n> has version status ACTIVE. When the vocabulary status is DEPRECATED the description is followed by " The vocabulary '<Name>' is deprecated." and no version is recommended. Missing reference (D12, S7): with a version "Referenced internal vocabulary '<Name>' version <n> does not exist."; without a version (name unknown) "Referenced internal vocabulary '<Name>' does not exist.". | provisional | R-003, R-004 |
| D48 | Problem types: belgif and simpl | Keep the types the OpenAPI and D44 fix: urn:problem-type:belgif:... for 400, 404, 500 and 503. Use urn:problem-type:simpl:... for 401, 403, 409, 413 and 415 (D17, D4, D15). This is a recorded deviation from API-46 for the four belgif codes; the code writer lists it in DEVIATIONS.md and the suite checks API-46 only on the simpl codes. | provisional | R-005 |
| D49 | Invalid name in a path | A GET, PATCH or DELETE whose path name or version breaks its pattern (D37 for names, a positive integer for versions) returns 400 InputValidationProblem with one issue (type urn:problem-type:belgif:input-validation:schemaViolation, in: path, name: the parameter). A valid name that is not stored returns 404 (D24, D41). Tests prove 'nothing stored' through the list with a name filter, not through a GET on the rejected name. | provisional | R-006 |
| D50 | Click on the active sort column | UI list: a click on a sortable column that is not the active sort sorts it ascending. A click on the active sort column reverses its order. So on the default list (Name ascending) the first click on Name sorts descending. | provisional | R-007 |
| D51 | Order of checks on write operations | Every write runs its checks in this order and stops at the first failure: 401, 403 (D17); 415 media type; 413 size; 400 request shape (path values D49, unknown fields and parameters D52, field rules D37, the version field D7); 404 vocabulary or version not found; 409 conflicts (duplicate name D15 and D37, same status D4, identical version D46); then validation of the content (syntax, then semantic, D9). So a duplicate name with invalid content gives 409, and a malformed request to an unknown name gives 400. | provisional | R-008, R-009 |
| D52 | Unknown fields and parameters | Any form field or JSON property the OpenAPI schema does not define returns 400 InputValidationProblem with one issue per unknown name (in: body), also where the schema does not say additionalProperties: false. Any query parameter on a POST, PATCH or DELETE returns 400 (in: query), as D21 does for GET. A query parameter given twice returns 400. | provisional | R-010, R-011 |
| D53 | Labels without a language tag (S4) | For S4, a literal without a language tag counts as one language ('no language'). Two untagged skos:prefLabel values on one subject are an S4 error. One untagged and one tagged value are not. | provisional | R-012 |
| D54 | Case of names in paths | Paths match the stored name exactly. /vocabularies/HEALTH for a stored 'Health' returns 404. The case-insensitive rule of D37 applies only to uniqueness on create and registration. Search by name (D38) is exact too; the list name filter stays case-insensitive (D19). | provisional | R-013 |
| D55 | Problem texts the decisions do not give | Fixed: version not found "Version <n> of vocabulary '<Name>' was not found."; external namespace conflict "An external vocabulary with namespace or alias '<uri>' is already registered.". 405 and an unknown path return application/problem+json with status 405 and 404; their type and detail are free. Field messages (400 issues) are free text. Tests check status, type, issue 'in' and 'name', and only the detail texts a decision fixes. | provisional | R-014 |
| D56 | External vocabulary registration details | Name: the D37 rules, unique case-insensitively among external vocabularies (409). Internal and external names are separate: an external vocabulary may share a name with an internal one. aliasUris: one multipart part 'aliasUris' per URI, optional, at most 20; an alias that equals the namespace URI or repeats another alias (compared as D30) returns 400 (in: body, name: aliasUris). description: optional, at most 1000 characters after trimming. | provisional | R-015 |
| D57 | Base IRI of uploaded content | Relative IRIs in uploaded Turtle resolve against the content URL of the new version: <contentBase>/content/<Name>/versions/<n> (D26). The stored bytes stay as uploaded (D29); the base is used only for validation. | provisional | R-016 |
| D58 | Times and name order | Times are stored and returned in UTC to the second, ISO 8601 with 'Z' (2026-11-18T09:27:00Z). Sort, ties and filters (D20, D23) use this value. The name sort compares by code point, so 'AZ' comes before 'Ab'. | provisional | R-017, R-018 |
| D59 | prev link beyond the last page | On a page beyond the last page the prev link points to the last page. No prev link when total is 0. | provisional | R-019 |
| D60 | UI texts and steps | Menu item 'Vocabularies'. Status change: action label 'Deprecate' on an ACTIVE vocabulary and 'Activate' on a DEPRECATED one, no confirmation dialog, success message 'Status of <Name> changed to <Active/Deprecated>.'. Dates shown in UTC as 'YYYY-MM-DD HH:mm UTC'. Link back on the details page: 'Back to vocabulary list'. A 500 or 503 shows 'Something went wrong. Retry later or contact support.' with the problem detail (D24, D44). Form labels: 'Name', 'Description', 'Change log', 'Vocabulary file'; buttons 'Submit' and 'Cancel'. Syntax errors in the browser (D13) show the line; the column is shown when the parser gives one, and tests check the line only. | provisional | R-020, R-021 |
| D61 | SIMPL-28680 leaves the live set | SIMPL-28680 is listed under stories.out_of_scope in sources.yaml: its spec stays collected, but its six criteria leave the inventory, the live set and the reports. It stays out of scope as in D1. | provisional | R-022 |

Earlier decisions changed by these:

- E2: refined by E4, ports and URLs are defaults; the build profile fixes them.
- E3: refined by E4, tests no longer reset the store; unique data per scenario.
- D11: refined by D47, no recommendation of a version of a DEPRECATED vocabulary.
- D12: refined by D47, S7 text for a reference without a version.
- D23: refined by D50, a click on the active sort column reverses it.
- D24: refined by D49, an invalid path value gives 400, not 404.
- D46: refined by D51, full order of checks on writes.
- D21: refined by D52, also POST, PATCH and DELETE, and repeated parameters.
- D9: refined by D53, untagged labels count as one language in S4.
- D30: refined by D56, registration details: names, aliases, description.
- D22: refined by D59, prev link beyond the last page.
- D13: refined by D60, column shown only when the parser gives one.
- D1: refined by D61, SIMPL-28680 out of the inventory.

## Feedback from runs 5 and 6 (added 19 Nov 2026)

The owner questions of run 5 (report section 6), the questions of the two suite reviews (step 45), the SOA-09 path check and the questions of run 6 (report section 6 and design review) are findings R-023 to R-065 in `validation/run-findings.json`. Run 6's report numbered its questions R-036 to R-054 by mistake; the evidence of each finding gives the mapping. Earlier decisions are not rewritten; they carry `refined_by` and `later_change` in `decisions.json`.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D62 | Submit with field errors | Submit is disabled only while the upload check of D13 shows a syntax error or a refused file (D76). Field errors of D37 do not disable it. A click on Submit with field errors sends nothing, keeps the errors under the fields and moves the focus to the first field with an error. | provisional | R-023 |
| D63 | 406 Not Acceptable | The Accept header is checked after 401 and 403 and before 415 (D51). A JSON operation accepts application/json, application/problem+json, */* or no Accept header; a content download accepts text/turtle, */* or no Accept header. Anything else returns 406, application/problem+json, type urn:problem-type:simpl:notAcceptable, title 'Not Acceptable', no issues. | provisional | R-024 |
| D64 | Latest ACTIVE version link in the UI | The UI follows D47: for a version of a DEPRECATED vocabulary it shows 'This version is deprecated.' and no 'Latest ACTIVE version' line. A version never links to itself: when the latest ACTIVE version is the version shown, the line is left out. | provisional | R-025 |
| D65 | Location texts of issues and warnings | Locations follow the OpenAPI examples. Syntax: 'line <n>, column <m>' (D9, column only when the parser gives one, D60). S1 and S2: statement '<subject> <predicate> <object>' with full IRIs and the literal as written. S3: resource '<property IRI>'. S4: resource '<subject IRI>', language '<tag>' ('none' for an untagged label, D53), one issue per subject and language. S5: resource '<IRI>'. S6: 'document'. S7, externalReferenceSkipped and deprecatedVocabularyReference: prefix '<prefix>' -> '<namespace>'. | provisional | R-026, R-027, R-031 |
| D66 | Not-found and conflict texts | Internal vocabulary not found: "Vocabulary with name '<Name>' was not found." with one issue (in: path, name: vocabularyName). POST /external-vocabularies/content with an unknown URI: 404, ExternalVocabularyNotFoundResponse, detail "External vocabulary with namespaceUri '<uri>' was not found." with one issue (in: body, name: namespaceUri); D41's name text stays for the paths with a name. In D55's conflict text and in these texts '<uri>' is the URI as the client sent it in this request. | provisional | R-028, R-029, R-030 |
| D67 | Requests rejected before routing | Order before any operation runs: a method the server does not know (for example QUERY) returns 405; a path the firewall refuses (encoded slash, control character) returns 400 problem+json, type urn:problem-type:belgif:badRequest, no issues; then the token (D17), so an unknown path gives 401 without a token and 404 with one. /actuator paths do not exist on the API port: 401 without a token, 404 with one. Health and metrics are served on a separate management port that is not exposed outside the deployment; GET /health on the API stays as D44. | provisional | R-032, R-057, R-058, R-059 |
| D68 | Issue names in search requests | Internal search: an invalid item gives in: body, name: items[<i>].name or items[<i>].version (index from 0). A body that matches no or several oneOf branches gives in: body, name: body. Over 100 URIs in an external search gives name: aliasUris when both arrays are present, otherwise the name of the array sent. | provisional | R-033 |
| D69 | Internal reference shapes | A namespace '<contentBase>/content/<Name>/versions/<v>' where <v> is not a positive integer is an S7 error with the text of D47 using <v> as given. A namespace '<contentBase>/content/' with no name, or with a name that breaks D37's pattern, is external (externalReferenceSkipped as D12). | provisional | R-034 |
| D70 | Content and multipart details | Downloads: Content-Type exactly 'text/turtle', no charset, bytes as uploaded (D29). A 0-byte vocabularyFile passes the field check and fails validation with S6 (400, location 'document'). Each aliasUris part holds one URI; commas are not split, so 'a,b' is one URI and fails D73. External registration rejects a file with no statements: 400 with one issue, detail 'Vocabulary contains no statements', in: body, name: vocabularyFile. A multipart part with an empty name is an unknown field: 400, in: body, name: ''. | provisional | R-035, R-036, R-037, R-050, R-051 |
| D71 | Time budget of writes | A create or new version with a file at the 5 MB limit completes within 30 s (Q1); the storage design must meet this. D44's 10 s store response timeout applies to reads; a store update may take up to 45 s before 503. The wait for the per-vocabulary lock (D43) is at most 30 s, then 503 as D44. Writes to different vocabularies may wait for each other in the store; that wait counts towards the same budgets. | provisional | R-039, R-052, R-053 |
| D72 | UI labels and texts | Filters: 'Name contains', 'Vocabulary status', 'Created on'. Row actions: 'View vocabulary details', 'History', 'Download vocabulary', 'Update', 'Deprecate' or 'Activate' (D60). After a successful creation the UI opens the details page of the new vocabulary and shows the D14 notification and warnings there. Failed status change: 404 shows 'Vocabulary not found. The status was not changed.'; 500 and 503 show 'The vocabulary status could not be updated.' followed by the D60 text and the problem detail. Loading the list or history: 500 and 503 show the D34 or D35 text and the problem detail, not the D60 text. List deprecation: the warning icon's accessible name and tooltip is D36's text; it is not visible row text. | provisional | R-040, R-041, R-042, R-044, R-056 |
| D73 | URI form, order and validation | namespaceUri, aliasUris and sourceUri must be absolute URIs (RFC 3986, with a scheme); otherwise 400, in: body, name: the field (also in the body of POST /external-vocabularies/content). They are stored and returned exactly as sent; aliases come back in the order sent. Only comparisons (uniqueness, lookups, D12) remove a trailing '#' or '/' (D30). | provisional | R-043, R-054 |
| D74 | Routes of the forms | Create form: /vocabularies/new. New version form: /vocabularies/<Name>/versions/new. 'new' cannot be a vocabulary name because names start with an upper-case letter (D37). | provisional | R-045 |
| D75 | UI semantics the tests may use | UI tests use only roles, decided labels and decided texts, never test ids or names taken from a build. The UI provides: the list and the version history as tables with column headers named as in D23 and D35; one actions menu button per row named 'Actions for <Name>' (version rows 'Actions for version <n>'); the version panel as a region named 'Version <n>'; the validation report as a region named 'Validation report' and warnings as 'Validation warnings'; success messages with role status; error messages with role alert; form fields labelled as D60. | provisional | R-046 |
| D76 | Where the upload limit applies | The 5 MB limit of D15 is on the vocabularyFile part. The whole multipart request may be at most 6 MB; above that the API returns 413 with the same body. In the UI a chosen file over 5 MB gives a field error under 'Vocabulary file': 'The file is larger than 5 MB.', and Submit is disabled (D62). The 413 detail above the form appears only when the server answers 413. | provisional | R-047, R-049 |
| D77 | Paths that can match the same URL | In this contract a fixed segment wins over a path parameter: /vocabularies/searches and /external-vocabularies/content are never read as a name. No stored name can equal them, because names start with an upper-case letter (D37). A method those fixed paths do not offer (for example GET /vocabularies/searches) returns 405. A later contract version should move the operations so no two paths overlap (SOA-09). | provisional | R-048 |
| D78 | Paging links keep the request | self, first, prev, next and last keep every query parameter of the request (filters and sort) with its value; only page changes. | provisional | R-055 |
| D79 | Contract changes | The decisions differ from the published OpenAPI here; writers build and test the decisions, fuzzing treats these as expected (fuzz-known.yaml), and the owners should put them in the next contract version. Stricter: 401 and 403 on every secured operation (D17); pageSize at most 100 (D22); name 3 to 64 characters and pattern on create (D37); description and changelog 1 to 1000 characters after trimming (D37, D56); unknown fields, unknown or repeated query parameters and parts with an empty name rejected (D21, D52, D70); version form field rejected (D7); search array sizes and totals (D38); at most 20 aliasUris, no duplicates (D56); absolute URIs (D73); file must be valid, non-empty Turtle, not any binary (D9, D70); invalid path values 400 (D49); 406 (D63); 405 on undeclared methods (D67); multipart request at most 6 MB (D76). Wider: name filter pattern relaxed (D19); createdBy on version objects (D27); validationWarnings always present (D10); latestVersionHref always filled (D5). | provisional | R-061 |
| D80 | Personal data in logs | Logs never contain the email or name of a user. They carry the token subject (sub) as the user id. createdBy in the data stays as D27. | provisional | R-062 |
| D81 | Several backend instances | Accepted risk for this build: exactly one backend instance runs (the deployment sets one replica). D43's in-process lock is valid only then. Running more instances needs a store-side check first (for example a conditional update on the latest version number); that is a later decision. | accepted risk | R-063 |

### Quality targets

| Id | Topic | Target | Status | Closes |
|---|---|---|---|---|
| Q1 | Response time and throughput | Measured on the build's own stack at the design load of Q4, after warm-up. Reads (lists, details, searches, downloads under 1 MB): p95 at most 300 ms. Create or new version: p95 at most 3 s for files up to 1 MB; at most 30 s for a file at the 5 MB limit (D71). Throughput: 20 reads per second and 1 write per second sustained with no errors. | provisional |  |
| Q2 | Limits | The limits of D15, D22, D37, D38, D56, D71 and D76 are the component's limits (NFR-04). Each is enforced with a defined error, and each has a test at the limit and one above. | provisional |  |
| Q3 | Availability and recovery | Availability 99.5 % per month in office hours (08:00 to 18:00 CET, working days). Recovery time 30 minutes, recovery point 24 hours. Daily backup of the whole store, kept outside the host. Restore never leaves the store emptier than before: it loads the backup into a new dataset, checks the count, and only then replaces the old one. A backup and restore round trip is tested on every build. | provisional | R-060, R-064 |
| Q4 | Design load and scaling | Design load: 20 concurrent users of the Governance Authority, 1000 vocabularies, 20 versions each, files up to 5 MB, 10 consuming services that search. One backend instance (D81); scale up, not out, until a store-side check exists. | provisional |  |
| Q5 | Maintainability | Backend line coverage at least 80 % and branch coverage at least 60 % from the build's own tests; no HIGH or CRITICAL finding in own images and dependencies (Trivy); the reference check needs at most one store query per upload (bounded, NFR-05). | provisional | R-065 |
| Q6 | Accessibility | WCAG 2.1 AA: no axe violations on the list, details and both form pages; every action reachable by keyboard. | provisional |  |

Earlier decisions changed by these:

- D13: refined by D62, D76, Submit disabled on upload errors only, not on field errors; browser refusal is a field error.
- D51: refined by D63, 406 sits between 403 and 415.
- D36: refined by D64, D72, no link for a DEPRECATED vocabulary, never to itself; list warning is the icon's name and tooltip.
- D9: refined by D65, exact location text per rule.
- D41: refined by D66, 404 text for the content lookup by namespace.
- D55: refined by D66, '<uri>' is the URI as sent.
- D17: refined by D67, 405 and firewall 400 come before 401.
- D38: refined by D68, issue names fixed.
- D12: refined by D69, non-numeric version is S7.
- D29: refined by D70, Content-Type without charset.
- D30: refined by D70, D73, empty external file rejected; stored as sent, URI validation.
- D52: refined by D70, part with an empty name is unknown.
- D56: refined by D70, D73, one URI per part; alias order as sent.
- D43: refined by D71, D81, lock wait at most 30 s; one replica only.
- D44: refined by D71, store update timeout 45 s.
- D23: refined by D72, filter labels.
- D60: refined by D72, failed status change and load texts.
- D14: refined by D72, details page after creation.
- D31: refined by D74, form routes added.
- D15: refined by D76, limit on the file part, request cap 6 MB.
- D22: refined by D78, links keep filter and sort.
- D39: refined by D79, full list of contract changes in D79.
- D27: refined by D80, logs use the token subject.

## Feedback from run 7 (added 19 Nov 2026)

Run 7 raised 28 owner questions (R-066 to R-093, report section 6). The seven with weight, from the design review, are findings R-087 to R-093 and are decided here. The other 21 are small wire details; until they are decided the build's own choice, as written in run 7's report, is the default. D81 is withdrawn by D82.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D82 | Concurrency through the store | D43's outcomes stay, but the service holds no lock state. Each write to a vocabulary is a conditional store update: it succeeds only when the latest version number (or, for a status change, the status) is still the one the request read. When the condition fails the service reads again and retries, at most 5 times within the budget of D71; then 503 as D44. A create checks the case-insensitive name in the same update. So several backend instances may run on one store. D81 is withdrawn. The build proves it with its own test of two service instances on one store; the suite's D43 test stays. | provisional | R-088 |
| D83 | Store access | The store accepts no anonymous access. It has two users: a read user and an update user, with passwords from the environment (OpenBao in production, E6). The backend uses the read user for queries and the update user for writes. Admin endpoints are off, or need a third admin user that only the backup and restore scripts use. The store sits on an internal network of the deployment and publishes no port to the host; backup and restore reach it from inside that network. | provisional | R-089 |
| D84 | Authorisation by default deny | Every operation of the OpenAPI has an explicit role rule (D16). A request that matches no declared operation never reaches a handler: 401 without a token, 404 with one (D67), and it is never served by a handler that has no rule. The build fails at start, or in its own test, when a handler exists without a role rule. Nothing is allowed unless a rule allows it. | provisional | R-090 |
| D85 | Bounded store queries | Filtering, sorting and paging run in the store: every list query has ORDER BY, LIMIT and OFFSET, and the total comes from a COUNT query. No store query runs without a LIMIT; the highest LIMIT is 100 rows plus one, except where a decision fixes a bound (search: at most 100 references, D38; reference check: one query per upload, Q5). The version list of the details page uses the same paging (D35). | provisional | R-091 |
| D86 | Metrics and trace context | Metrics in Prometheus format on the management port (D67) at /actuator/prometheus: per operation the request count, the error count by status and the duration. The service reads the W3C traceparent header and X-Correlation-Id (creating them when absent), returns X-Correlation-Id, writes both in every log line, and forwards both on its calls to the store and to the identity provider. No tracing backend is part of the build. | provisional | R-092 |
| D87 | Model and contract deliverables | The build delivers in docs/: a logical data model (entities, attributes, identifiers and relations, as tables and one diagram) traced to the CDM of D2; a physical model for the RDF store as SHACL shapes of each named graph, which the build's own tests run against stored data; and openapi-decided.yaml, the published OpenAPI with every change of D79 applied. The files in specs/api stay unchanged (D39). Step 70 keeps fuzzing the published contract. | provisional | R-093 |

Earlier decisions changed by these:

- E1: refined by E7, second client and second realm for negative token tests.
- E5: refined by E7, tested through E7.
- D43: refined by D82, conditional store update instead of the in-process lock.
- D81: refined by D82, withdrawn by D82.
- Q4: refined by D82, scale out allowed.
- E3: refined by D83, store with read and update users, internal network only.
- D16: refined by D84, default deny, rule per operation.
- D17: refined by D84, no handler without a rule.
- Q5: refined by D85, no store query without LIMIT.
- D67: refined by D86, metrics content and trace forwarding fixed.
- D79: refined by D87, build delivers openapi-decided.yaml.
- D2: refined by D87, LDM traced to the CDM.

## Feedback from run 8 (added 19 Nov 2026)

Run 8 raised 12 owner questions (R-094 to R-105, report section 6). All 12 are decided here. The run 7 questions that are still open (R-066 to R-086) keep the build's own choice as default, except R-083, which D99 settles.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D88 | Realm key refetch | The service fetches the realm keys again at most once per 10 s. A token whose key id is not cached gets one fetch when the last fetch is older than 10 s. Inside the 10 s window, or when the fetched keys still lack the key id, the token gets 401 (invalid token, D17). 503 stays for the case where the keys cannot be fetched at all (D44). | provisional | R-094 |
| D89 | Restore and later writes | A restore brings the store back to the backup; writes made after that backup are lost, which is within the recovery point of Q3 (24 h). Before a restore the backend is stopped (or scaled to 0), so no write lands during it; it is started again after the count check. The build delivers the backup script, an example schedule (cron line or Kubernetes CronJob) and a configurable copy target outside the host; installing the schedule is an operations task, documented in docs/. | provisional | R-095 |
| D90 | Validation report semantics | The validation report is a region (section with aria-label) named 'Validation report' that holds the list of issues. The region has no role of its own beyond region. Inside it, a first line with role alert says how many errors were found ('<n> errors found'), so screen readers announce it. Warnings use a region named 'Validation warnings' with a status line (role status). Other error messages keep role alert (D75). | provisional | R-096 |
| D91 | Integer range | Every integer in a path, query or JSON body is a 32-bit signed integer: version numbers, page and pageSize from 1 to 2147483647 unless a decision sets a lower maximum (pageSize 100, D22). A value outside the range, or one that is not a whole number, gives 400 with the same problem body as other invalid values (D49, D22). The decided contract states format int32 and the maximum. | provisional | R-097 |
| D92 | Concurrent uploads | One backend instance handles at most 4 create or new-version requests at the same time. A fifth gets 503 as D44 with Retry-After 5 and detail 'Too many uploads in progress. Retry later.'. Content validation (D9) and the reference check (D12) run once per request, before the conditional write; a retry of D82 only reads the latest state again and repeats the condition and the update. The heap must hold 4 uploads at the 6 MB request limit (D76) with their parsed form. | provisional | R-098 |
| D93 | Request validation against the contract | The build validates requests either with a maintained OpenAPI request validator that loads openapi-decided.yaml, or with its own code plus an own test that compares that code with openapi-decided.yaml (every operation, parameter, limit and pattern). The build chooses and says which in REUSE.md. Every own component of more than about 100 lines that does infrastructure work (validation, parsing, caching, query building, HTTP plumbing) has a row in REUSE.md with the product it could have reused and why it did not. | provisional | R-099 |
| D94 | Library choices | Unless REUSE.md says why not: SPARQL queries and updates are built with Jena (ParameterizedSparqlString, or the query and update builders), never by joining strings with values; realm keys are fetched and cached with Nimbus JWKSourceBuilder or the Spring Security resource server; W3C trace context and the correlation id run through Micrometer Tracing (D86). | provisional | R-100 |
| D95 | Lint, format and UI tests | The finish line includes: mvn verify runs a configured formatter check (Spotless with a fixed style); the UI build runs ESLint and a Prettier check and fails on errors; each UI view has at least one component test (Vitest or similar) in ui/tests that runs in the UI build. A pipeline file is not required (no CI in the factory). | provisional | R-101 |
| D96 | Structured log fields | Logs are JSON lines. Every line carries traceId and correlationId (D86) and operationId from the MDC; a line about a vocabulary also carries vocabularyName and, when known, version, as separate fields. No email or token in logs (D80). | provisional | R-102 |
| D97 | CDM, licence and glossary in the build | The CDM files named in sources.yaml are part of the build input (input/cdm/, from run 9 on). The logical model of D87 uses their class names where the glossary does not decide otherwise and lists each difference. The build ships a LICENSE file with the EUPL 1.2 text and a docs/glossary.md that copies or links the decided glossary. | provisional | R-103 |
| D98 | Local stack hardening | The local stack may run Keycloak in dev mode, but publishes its port on 127.0.0.1 only. The management port of the backend (D67) is on an internal network of the stack and is not on the front network; step 70 reads metrics through the stack's own scripts. Container CPU and memory limits are variables in the compose file with the current values as defaults. | provisional | R-104 |
| D99 | Load test shape | The seed of Q4 is 1000 vocabularies with 20 versions each (20 000 versions), files of about 10 KB. Q1 throughput is an offered rate: the load test sends 20 reads per second and 1 write per second (files up to 1 MB) for 5 minutes; met when the achieved rate is at least 95 % of the offered rate with no errors and the p95 targets hold. The 5 MB writes of D71 run as a separate test, not in the throughput mix. | provisional | R-105 |

Earlier decisions changed by these:

- D44: refined by D88, key refetch at most once per 10 s; unknown key id inside the window is 401.
- Q3: refined by D89, writes after the backup are lost within the RPO; backend stopped during a restore; schedule and copy target delivered as examples.
- D75: refined by D90, report is a region with an alert line inside.
- D79: refined by D91, format int32 and maximum on every integer.
- D49: refined by D91, range 1 to 2147483647.
- D82: refined by D92, validation outside the retry loop.
- Q2: refined by D92, at most 4 uploads in parallel per instance.
- D87: refined by D93, request validation tied to openapi-decided.yaml.
- E6: refined by D93, REUSE.md row per own infrastructure component.
- E6: refined by D94, named libraries for SPARQL, keys and tracing.
- Q5: refined by D95, format, lint and UI tests on the finish line.
- D86: refined by D96, JSON log lines with operationId, vocabularyName and version.
- D87: refined by D97, LDM uses the CDM in input/cdm.
- D2: refined by D97, CDM part of the build input.
- D67: refined by D98, management port on an internal network only.
- D83: refined by D98, Keycloak port on 127.0.0.1 only.
- Q1: refined by D99, throughput as offered rate, 5 MB writes measured apart.
- Q4: refined by D99, seed 1000 by 20 versions.

## Feedback from run 9 (added 20 Nov 2026)

Run 9 raised 19 owner questions (R-106 to R-124, report section 6). All 19 are decided here.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D100 | List columns | The vocabulary list has these columns, in this order, with these headers: Name, Status, Description, Creator, Date created, Last updated, Actions. Creator shows createdBy of version 1 (D27); Description the description of the latest version. Sortable columns stay as D23. | provisional | R-106 |
| D101 | Errors on Submit | A click on Submit checks every field, touched or not, including an empty Vocabulary file, and shows every error under its field; nothing is sent and the focus moves to the first field with an error (D62). Error texts have reserved space, so an error that appears on blur never moves Submit away from the pointer. | provisional | R-107 |
| D102 | Two backend instances | Q4's sentence on one instance is replaced by D82: the delivered stack must work with two backend instances on one store. The finish line runs the stack with two backends (for example docker compose up --scale backend=2) behind the UI proxy and passes smoke.sh on it. Nothing in the stack fixes a container name or host port per backend. | provisional | R-108 |
| D103 | Circuit breaking | Resilience4j circuit breakers around the store calls and the key fetch. Each opens when at least half of the last 20 calls failed (at least 10 calls), stays open 10 s, then lets 3 probe calls through. While open the caller gets 503 as D44 at once, without waiting for a timeout. Breaker state is in the metrics (D86). | provisional | R-109 |
| D104 | Exception model | No shared Simpl exception library is used. The problem types of D48 are the error codes: the build has one exception base class carrying the problem type, title and status, and one handler that maps it to problem+json. | provisional | R-110 |
| D105 | Simpl log format | The Simpl log format (LogWrapper) and log shipping are out of scope for the factory's builds, because the library is not in the build input. The JSON lines of D96 are the format. The build records CODE-21, CODE-52 and SEC-11 as deviations with this decision as reason. | provisional | R-111 |
| D106 | Where file bytes live | The uploaded file stays in the RDF store, next to its metadata, so one store transaction holds both (D8). The build may store it as one literal or in chunks. Object storage is not used: at 5 MB per file and the Q4 load it adds a second store without need. | provisional | R-112 |
| D107 | No host port for the store | D83 wins: the store publishes no host port, also not for debugging. The build profile drops the Fuseki port; debugging goes through docker exec or the backup scripts. | provisional | R-113 |
| D108 | Realm keys: library and first fetch | D94 is changed for keys: the build uses Nimbus JWKSet and JWSVerifier with its own fetch timing, so D88's cooldown and outcomes hold. The first fetch may be lazy, on the first token; until then GET /health reports UP when the store answers, and DEGRADED only after a fetch failed. | provisional | R-114, R-115 |
| D109 | Counts in report texts | Errors line: '1 error found' or '<n> errors found', role alert. Warnings line: '1 warning found' or '<n> warnings found', role status, as the first line of the region 'Validation warnings'. | provisional | R-116 |
| D110 | Open contract details | Decided as run 9 built them: a JSON body over 1 MB gives 413; more than 64 multipart parts give 400; a date-time without an offset is read as UTC; the 201 body carries createdBy and latestVersionHref; aliasUris is always present in responses (empty list allowed); a bad element of a search array is named '<array>[<index>]' in the issue; paging links put page and pageSize first, then the other parameters in request order. These are added to the D79 list. | provisional | R-117 |
| D111 | Paging links always complete | Every paging link (self, first, prev, next, last) carries page and pageSize, also when the request did not send them, with the values used. | provisional | R-118 |
| D112 | What an integer is | The text decides: an integer in a path or query is one or more digits, nothing else ('2.0', '1e1', '+2' and ' 2' give 400). In a JSON body it is a JSON number without fraction or exponent. | provisional | R-119 |
| D113 | After a new version | After a successful new version from the Update form, the UI opens the details page of that vocabulary with the new version's panel open, and shows the D14 notification and warnings there, as D72 does after a create. | provisional | R-120 |
| D114 | One alert per event | A load error or a failed status change shows one element with role alert that holds the decided text followed by the problem detail. | provisional | R-121 |
| D115 | Page controls and banner | The list has buttons 'Previous page' and 'Next page' and the text 'Page <n> of <m>'. The deprecation banner on the details page has role status and the D36 text. | provisional | R-122 |
| D116 | More contract formats | Added to the D79 list and to openapi-decided.yaml: format date or date-time on the date filters (D20), the Turtle media type text/turtle on vocabularyFile (D9), and minimum 1 on the version in paths (D49). | provisional | R-123 |
| D117 | CDM findings | The CDM findings go to the CDM owner; the build does not wait for them. The logical model of D87 and D97 uses typed attributes, leaves search request classes out of the domain model, names timestamps creationDate and lastUpdateDate (glossary), and lists each difference with the CDM. | provisional | R-124 |

Earlier decisions changed by these:

- D23: refined by D100, full column list.
- D62: refined by D101, all fields checked on Submit, no layout shift.
- Q4: refined by D102, two instances on the finish line, D81 sentence replaced.
- D44: refined by D103, circuit breakers, open gives 503 at once.
- D48: refined by D104, problem types serve as error codes.
- D96: refined by D105, stays the log format, Simpl LogWrapper out of scope.
- E3: refined by D106, file bytes in the store.
- D83: refined by D107, also no debug port.
- D94: refined by D108, own fetch timing around Nimbus for keys.
- D44: refined by D108, lazy first key fetch allowed.
- D90: refined by D109, singular forms and warnings line.
- D79: refined by D110, D110 details added.
- D78: refined by D111, page and pageSize always present.
- D91: refined by D112, text form decides.
- D72: refined by D113, also after a new version.
- D72: refined by D114, one alert element per event.
- D36: refined by D115, banner role status.
- D34: refined by D115, page control names.
- D79: refined by D116, D116 formats added.
- D97: refined by D117, model differences with the CDM listed.

## Feedback from run 10 (added 20 Nov 2026)

Run 10 raised 24 owner questions (R-125 to R-148, report section 6). All 24 are decided here.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D118 | Latest version link | The whole text 'Latest ACTIVE version: <n>' is one link to that version. | provisional | R-125 |
| D119 | Parallel writes in the D43 test | D92 stays: a fifth upload on one instance gets 503. The D43 test sends at most 4 parallel new versions. | provisional | R-126 |
| D120 | Management port with replicas | The management port may sit on the front network when it is published on no host port and the proxy never forwards /actuator. Step 70 reads metrics from inside the stack. | provisional | R-127 |
| D121 | Part with an empty name | The 400 for a part with an empty name is dropped from D70. The container ignores such a part; the request is judged without it. No own multipart parser. | provisional | R-128 |
| D122 | Front proxy answers | The proxy's body limit is 7 MB, above D76, so the service decides on sizes. Where the proxy answers itself (TRACE, malformed request, above 7 MB) it returns problem+json with the D48 type and status (405, 400, 413). | provisional | R-129 |
| D123 | Content-Security-Policy | The UI sends Content-Security-Policy: default-src 'self'; connect-src 'self' and the identity provider URL; no inline scripts or styles; frame-ancestors 'none'. UI tests run axe with the browser's CSP bypass. | provisional | R-130 |
| D124 | Base IRI of external files | For an external vocabulary file the namespace URI is the base for relative IRIs. | provisional | R-131 |
| D125 | Size of the validation report | The validation report holds at most 1000 issues. When there are more, it holds the first 1000 in file order and the problem body has "truncated": true. Added to D79. | provisional | R-132 |
| D126 | HEAD and OPTIONS | HEAD and OPTIONS on declared paths get 405 with an Allow header, like other undeclared methods (D67). The UI is same-origin through the proxy, so no CORS. | provisional | R-133 |
| D127 | Accept ranges | application/* is accepted like */*; the response uses the operation's normal media type. | provisional | R-134 |
| D128 | Status sort order | Status sorts alphabetically: ACTIVE before DEPRECATED ascending. This is also the business order. | provisional | R-135 |
| D129 | Unknown key after a failed fetch | Inside the 10 s window after a failed key fetch, an unknown key id gives 503 (keys not available, D44). After a successful fetch it gives 401 (D88). | provisional | R-136 |
| D130 | Date-time offsets | An offset beyond plus or minus 18:00 gives 400, like any invalid date (D20). | provisional | R-137 |
| D131 | Start command | The build profile names bash stack.sh up as the start command; stack.sh creates the local secrets. The .env file is never committed. | provisional | R-138 |
| D132 | Token claim checks | Issuer, audience, expiry and not-before are checked with Nimbus DefaultJWTClaimsVerifier or Spring Security JwtValidators. Own code only for the key fetch timing of D88. | provisional | R-139 |
| D133 | Routing and negotiation | Requests are routed with Spring MVC mappings that declare produces and consumes; query parameters are bound by the framework, with the strict checks of D52 on top. An own test fixes the order 404, 405, 406, 415. | provisional | R-140 |
| D134 | Breaker failures and health | A store 5xx and a timeout count as breaker failures; a 4xx from the store does not. GET /v1/health never fetches keys; it reports the cached key state and checks the store with one bounded call. | provisional | R-141 |
| D135 | Interfaces for single implementations | No interface for a class with one implementation; CODE-11 is a recorded deviation with this decision as reason. | provisional | R-142 |
| D136 | Q1 per operation | The read p95 of Q1 holds per operation, including the paged list at the full D99 seed. | provisional | R-143 |
| D137 | Issue name for an empty array | The issue is named after the array (namespaceUris or aliasUris), in body. | provisional | R-144 |
| D138 | Repeated query parameter | Exactly one issue, in query, named after the parameter. | provisional | R-145 |
| D139 | Known method on a declared path | A known method that the path does not declare (for example PUT /vocabularies) gets 405 with Allow, with or without a token. 405 comes before 401 (D67). | provisional | R-146 |
| D140 | Literal in S1 locations | The literal is given in N-Triples form: the lexical form in double quotes, followed by @lang or ^^<datatype> when present. | provisional | R-147 |
| D141 | Required fields | Every required field has aria-required="true" and a visible '*' after its label; the label text stays as D60. | provisional | R-148 |

Earlier decisions changed by these:

- D36: refined by D118, whole text is the link.
- D43: refined by D119, test with at most 4 parallel writes.
- D98: refined by D120, front network allowed when unpublished and not proxied.
- D70: refined by D121, empty part name no longer 400.
- D79: refined by D121, empty part name dropped from the list.
- D48: refined by D122, proxy errors also problem+json.
- D57: refined by D124, also for external files.
- D9: refined by D125, at most 1000 issues.
- D79: refined by D125, truncated field.
- D67: refined by D126, HEAD and OPTIONS also 405.
- D63: refined by D127, application/* accepted.
- D23: refined by D128, status sort alphabetical.
- D88: refined by D129, 503 after a failed fetch.
- D20: refined by D130, offset limit 18:00.
- E2: refined by D131, start via stack.sh.
- D108: refined by D132, claims via library.
- D93: refined by D133, framework routing.
- D103: refined by D134, store 5xx counts.
- D44: refined by D134, health does not fetch keys.
- Q1: refined by D136, per operation.
- D38: refined by D137, issue named after the array.
- D52: refined by D138, one issue per repeated parameter.
- D84: refined by D139, 405 before 401 and 404.
- D65: refined by D140, literal in N-Triples form.
- D60: refined by D141, required markers.

## Feedback from run 11 (added 20 Nov 2026)

Run 11 raised 26 owner questions (R-149 to R-174, report section 6). All 26 are decided here.

| Id | Topic | Decision | Status | Closes |
|---|---|---|---|---|
| D142 | Language tags in S4 | Language tags are compared case-insensitively (BCP 47): @EN and @en are the same language and give one S4 issue. The location writes the tag in lower case. | provisional | R-149 |
| D143 | S5 granularity | One S5 issue per resource, whatever the number of disjoint pairs. The location stays resource '<IRI>' (D65). | provisional | R-150 |
| D144 | S3 position | One S3 issue per property. The location is resource '<property IRI>' as D65, with no line, so the order of the declarations does not matter. | provisional | R-151 |
| D145 | Prefix declared twice | Each distinct namespace IRI is checked once. The same IRI declared under two prefixes, or twice under one prefix, gives one result, with the location of its first declaration. | provisional | R-152 |
| D146 | Other paths under an internal name | D12's 'followed by /' covers any further path: <contentBase>/content/<Name>/<anything else> is an internal reference to the latest version of <Name>. It is S7 only when <Name> does not exist. Only /versions/<v> selects a version (D69 stays for a bad <v>). | provisional | R-153 |
| D147 | External name conflict | Checks run in this order and stop at the first conflict: name, namespace URI, then alias URIs in request order. The name text is "An external vocabulary with name '<Name>' is already registered."; namespace and alias keep the text of D55. | provisional | R-154 |
| D148 | Blank optional external description | A description that is blank after trimming is treated as absent: it is not stored and the response has no description. | provisional | R-155 |
| D149 | Query parameters on GET /health | D21 applies to GET /health as to every GET operation: an unknown query parameter gives 400. The container health check sends none (D152). | provisional | R-156 |
| D150 | JSON body over 1 MB | 413 with the UploadTooLargeResponse type of D15, detail 'The request body is larger than 1 MB.', and no issues. Tests check status, type and detail. | provisional | R-157 |
| D151 | Accept problem+json on JSON operations | Accepted, as D63 says. A success carries application/json; only errors carry application/problem+json. | provisional | R-158 |
| D152 | Container health check | The container health check uses the liveness probe on the management port (/actuator/health/liveness), which does not check the store or the identity provider. GET /v1/health stays the readiness view of D44 and D134. A backend stays running and healthy while the store is down. | provisional | R-159 |
| D153 | Search result paging fields | Search results carry no paging links. page is 1, pageSize 100, total the number of items; totalPages is 1 when there are results and 0 when there are none. | provisional | R-160 |
| D154 | Version list sort by name | Confirmed: for both sort=name and sort=-name the tie-break is version descending (D23), so both give the default order. | provisional | R-161 |
| D155 | Proxy internal paths | Proxy internal paths are not reachable from outside. A request to one is handled like any unknown path: 401 without a token, 404 with one, both problem+json (D67). | provisional | R-162 |
| D156 | TDB2 compaction | The build ships a compaction script using the Fuseki online compact (/$/compact, delete old) and an example weekly schedule, run after the daily backup of Q3. No downtime is planned; operations own the schedule in production. | provisional | R-163 |
| D157 | Retry backoff for conditional writes | Retries of D82 wait 50, 100, 200, 400 and 800 ms, each with up to 50 % random jitter, inside the D71 budget. Content validation is never repeated on a retry (D92 stays). | provisional | R-164 |
| D158 | Rejections by the HTTP server | Every answer of the backend is problem+json, also when its HTTP server refuses the request before routing. Bad encoding and too many parameters: 400, type urn:problem-type:belgif:badRequest, no issues. URL too long: 414; headers too large: 431; their type is free (D55). The build raises the server limits so the service's own checks (D21, D138) see normal requests. | provisional | R-165 |
| D159 | Latest version column | The list gets a column 'Latest version' between Status and Description, showing the number of the latest version (D5). D100's order becomes: Name, Status, Latest version, Description, Creator, Date created, Last updated, Actions. The column is not sortable. | provisional | R-166 |
| D160 | Version lastUpdateDate | A PATCH that changes a version's status sets that version's lastUpdateDate and the vocabulary's lastUpdateDate to the time of the change. A same-status request (409, D4) changes nothing. | provisional | R-167 |
| D161 | 409 on the forms | The alert shows the problem detail as the API sends it, as for 413 (D15) and the 400 report. The form stays open with the values entered. | provisional | R-168 |
| D162 | Order of checks on reads | Reads run their checks in this order and stop at the first failure: 401, 403, 406 (D63), 400 for path and query values (D21, D49), then the store: 404 or 503. A query error is reported without touching the store; 403 comes before 404. | provisional | R-169 |
| D163 | UI selection and sort markers | A version is selected by its version number, a link to /vocabularies/<Name>/versions/<n>; the selected row has aria-current="true". A sortable column header holds a button; the th carries aria-sort ascending, descending or none, and an arrow shows the direction. | provisional | R-170 |
| D164 | Failed-token limiting | Limiting repeated token failures belongs to the Tier 1 gateway in production. The local stack does not limit them; the build records OWT-A07 as a deviation with this decision as reason. | provisional | R-171 |
| D165 | createdBy as personal data | createdBy stays the email (D27); viewers may see it, as all users are Governance Authority staff. The logical model and the CDM differences mark it as personal data. It is not written to logs. Encryption at rest belongs to the platform, recorded with the API-25 deviation. | provisional | R-172 |
| D166 | TRACE inside the network | The backend refuses TRACE itself with 405 and Allow (D67); allowTrace stays off. | provisional | R-173 |
| D167 | UI component library | E6 wins: the UI uses PrimeVue in unstyled mode with TailwindCSS from npm. build.yaml is aligned. The participant-fe-components library itself stays out, because it is not available to the factory. | provisional | R-174 |

Earlier decisions changed by these:

- D65: refined by D142, tags compared case-insensitively, written lower case.
- D65: refined by D143, one S5 issue per resource.
- D12: refined by D145, each distinct namespace checked once.
- D12: refined by D146, any further path under <Name> is the latest version.
- D55: refined by D147, external name conflict text and check order.
- D56: refined by D148, blank description is absent.
- D21: refined by D149, also GET /health.
- D110: refined by D150, 413 text for JSON bodies.
- D63: refined by D151, 200 stays application/json.
- D44: refined by D152, container uses liveness, not GET /health.
- D38: refined by D153, no links, totalPages 0 for an empty result.
- D122: refined by D155, internal paths behave as unknown paths.
- Q3: refined by D156, weekly compaction after the backup.
- D82: refined by D157, backoff fixed.
- D48: refined by D158, server-level refusals also problem+json.
- D122: refined by D158, same for the backend's own server.
- D100: refined by D159, Latest version column added.
- D4: refined by D160, manual PATCH also sets lastUpdateDate.
- D25: refined by D160, version PATCH updates the vocabulary's date.
- D37: refined by D161, 409 alert shows the API detail.
- D46: refined by D161, same.
- D51: refined by D162, read order added.
- D35: refined by D163, selection by version link.
- D23: refined by D163, sort markers.
- D17: refined by D164, failed-token limiting is the gateway's.
- D27: refined by D165, marked as personal data, not logged.
- D67: refined by D166, backend refuses TRACE itself.
- E6: refined by D167, PrimeVue and Tailwind from npm, build.yaml aligned.

## Environment choices

| Id | Topic | Choice |
|---|---|---|
| E1 | Identity provider and test users | Keycloak, realm 'simpl', public client 'vocabulary-manager-ui' and bearer-only audience 'vocabulary-manager-be'; roles are realm roles read from realm_access.roles. Users (password 'Password1!'): ga-admin (email ga-admin@example.com, GA_VOCABULARY_ADMIN), ga-viewer (ga-viewer@example.com, GA_VOCABULARY_VIEWER), ga-norole (ga-norole@example.com, no role). |
| E2 | Ports and base URLs | Backend on http://localhost:8080 with the API under /v1 (for example http://localhost:8080/v1/vocabularies). href values and Location headers are relative to the API base, as in the OpenAPI examples. contentBase is configuration VOCABULARY_CONTENT_BASE_URL, default https://authority.example/vocabulary-manager, used for contentHref and for recognising internal references (D12). UI on http://localhost:4200, Keycloak on http://localhost:8180, Fuseki on http://localhost:3030. |
| E3 | Store and stubs | Apache Jena Fuseki, dataset 'vocabulary', as the store (SIMPL-18170 note 'same persistence layer as Schema Management - Fuseki DB'). Tests reset the dataset before each test. No external services are called: external vocabularies are never fetched from the internet, only registered copies are used. UI tests may stub the API for error cases (D24). |
| E5 | Tokens: audience and issuer | Tokens must carry the audience 'vocabulary-manager-be'; Keycloak adds it with an audience mapper on the clients that call the API (vocabulary-manager-ui and the test client). Tokens without it get 401. The accepted issuer is one configured URL (ISSUER_URI, default http://localhost:<KEYCLOAK_PORT>/realms/simpl); other issuers get 401. |
| E6 | Reuse choices | From guidelines/reuse-catalogue.yaml: identity provider Keycloak (E1); RDF store Apache Jena Fuseki (E3); in production the Tier 1 gateway (Spring Cloud Gateway) does authorisation first, the build checks tokens itself (D17); secrets from environment variables locally, OpenBao in production; UI on the stack of participant-fe-components (Vue 3, PrimeVue unstyled, TailwindCSS), built here because the library is not available to the factory. Not needed: relational store, messaging (pull only, D45), object storage, connector, http-client (no outgoing calls except the realm keys). |
| E7 | Test clients for token checks | The realm 'simpl' has a second public client 'vocabulary-tests-noaud' without the audience mapper, and a second realm 'simpl-other' with client 'vocabulary-tests' (with the mapper) and the same three test users of E1. Both allow the password grant. A token from 'vocabulary-tests-noaud' gets 401 (wrong audience); a token from realm 'simpl-other' gets 401 (wrong issuer). The build profile gives their token URLs to the suite. |

## Questions for the spec owners, by impact

1. **D11 Reference to a deprecated internal version.** Confirm that a deprecated reference is a warning; SIMPL-18184/AC2 then needs rewording.
2. **D3 Vocabulary status and version status.** Should deprecating a vocabulary also write DEPRECATED to each of its versions, and should a new version be refused for a DEPRECATED vocabulary?
3. **D43 Concurrent changes to one vocabulary.** Confirm that overlapping changes to one vocabulary are serialised rather than refused, and whether production runs several backend instances.
4. **D9 Validation steps and rules.** Give the real list of semantic and structure rules, with one example each; confirm that reporting only the first syntax error is acceptable.
5. **D16 Roles and what they allow.** Confirm the role names and that the read-only POST searches are open to the viewer.
6. **D38 Search operations.** Confirm the searches are not paged and the limit of 100 references; add maxItems 100 (minItems 1 on the external arrays) and state the result order.
7. **D4 Version transitions and same-status requests.** Confirm manual version status change is wanted, and whether two ACTIVE versions are allowed.
8. **D4 and B-002 Reactivation in the process.** Is reactivating a vocabulary or a version a BP02B step (with a requirement 2B.7), or a correction outside the process as built? May a single version be deprecated or reactivated from the UI?
9. **D46 Limits and abuse cases on version creation.** Add 409 to POST .../versions, confirm byte equality as the identical-version test, and set the gateway call-rate limit for writes.
10. **D41 Test basis for operations without a story.** Add stories for external vocabulary registration and for the version status change; point the external 404 at ExternalVocabularyNotFoundResponse and fix the 400 example of POST /external-vocabularies.
11. **D44 Store and identity provider slow or down.** Add a 503 ServiceUnavailableResponse with Retry-After to every secured operation; confirm the timeouts and that a missing identity provider fails with 503.
12. **D19 Name filter.** Confirm the API change from exact match to contains.
13. **D12 What a vocabulary reference is.** Confirm references are recognised by namespace prefix declarations and by the content URL, and name the namespaces that never need a cached external copy.
14. **D45 How consumers learn of publication and deprecation.** Does Schema Manager need events on publication and status changes? Who tells providers that a version they use was deprecated?
15. **D17 Where 401 and 403 come from.** Should the OpenAPI list 401 and 403 on each operation, with these problem types?
16. **D39 OpenAPI catch-up.** Update the OpenAPI file with the points of D5, D7, D10, D17, D19, D21, D22, D27 and D37 (full list in decisions.json).
17. **D40 Security scheme.** Replace OAuth2Auth with an openIdConnect scheme on the Tier 1 Keycloak, drop the read and write scopes, and name the two realm roles per operation.
18. **D10 Validation errors versus validation warnings.** Split the CDM class VocabularyValidationWarning into an error and a warning concept with these code sets.
19. **D33 New version form.** Confirm a new version may change only the description.
20. **D27 Creator.** Add createdBy to the CDM and to the version schemas.
21. **D1 Stories outside this build.** Confirm SIMPL-393 and SIMPL-395 can be closed as replaced, and that the CPSV vocabulary needs no pre-load.
22. **D13 Validation on file upload in the UI.** Confirm that a syntax-only check in the browser meets SIMPL-18180/AC1.
23. **D25 The vocabulary object (VocabularyCategory).** Rename the schema VocabularyCategory to Vocabulary and its field version to latestVersion in a later API version.
24. **D28 Description.** Remove InternalVocabulary.description from the CDM.
25. **D37 Form field rules and messages.** Confirm case-insensitive uniqueness and the 1000-character limits.
26. **D30 External vocabulary identity and registration.** Confirm the name is the identity and the namespace URI a unique key.
27. **D42 External vocabularies in the process.** Add register and remove steps to BP02B or keep them outside; confirm removal of a referenced external vocabulary is allowed without a check.
28. **D22 Pagination limits.** Confirm the maximum page size.
29. **D20 Date filters.** Is a range (from, to) wanted instead?
30. **D35 Details and history page.** Confirm the details page and the history page are one screen.
31. **D18 Restricted actions in the UI.** Confirm hidden rather than disabled.
32. **D36 Deprecation warnings in the UI.** Supply the wording and form from the Figma mock-up if it differs.
33. **D5 Meaning of latest version.** Retire current version and newest version in SIMPL-18161 and SIMPL-18165.
34. **D7 Version numbering.** Confirm the Schema Manager versioning library gives the same integers.
35. **D15 File format, size and media type.** Confirm RDF/XML and OWL/XML (BP02B.01) are not needed.
36. **D6 Publication and manual review.** Confirm the manual semantic review of BP02B.02 is an off-line step.
37. **D2 CDM snapshot.** Approve the CDM with the changes in D3, D10, D25 to D28, or say which ones to drop.
38. **D14 Validation report and success feedback.** Confirm the CDM may name the validation report as a transient concept.
39. **D26 The vocabulary version object.** Rename VocabularyMetadata to VocabularyVersion in a later API version.
