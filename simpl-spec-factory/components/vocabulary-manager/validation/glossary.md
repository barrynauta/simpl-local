# Draft glossary: Vocabulary Manager (step 23b)

Draft, for the spec owners to decide. One row per concept. The proposed term follows the business process and the stories first, then the CDM, then the API. CDM ids are from `cdm/inventory.json` (snapshot of `vocabulary-manager-be.md`, commit 70f951f, not yet approved). API names are from `specs/api/vocabulary_openapi-tier1.yaml`. Finding ids D-nnn are in [`domain-findings.json`](./domain-findings.json); F-nnn are step 20b findings in [`ac-findings.json`](./ac-findings.json).

Status: `agreed` = all sources already use one term; `proposed` = chosen among synonyms; `open` = the sources disagree on the meaning.

## Terms

| Term | Definition (from the specs) | CDM class / attribute | API schema(s) and field(s) | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| Vocabulary | A reusable knowledge base in Turtle format defining concept schemes, hierarchies or controlled vocabularies, managed by the Governance Authority (CDM). | `CDM:vocabulary-manager-be:InternalVocabulary` | `VocabularyCategory`; paths `/vocabularies`, `/vocabularies/{vocabularyName}` | SIMPL-18161/AC1, 18162/AC1, 18164/AC1, 18170/AC1, 18174/AC1, 18175/AC1, 18178/AC1; BP02B.01, 2B.1 to 2B.6 | InternalVocabulary (CDM); VocabularyCategory (API); internal vocabulary (API info, SIMPL-18184 note); native vocabulary (BP Overview); local vocabulary (API `searchVocabularies`); self managed vocabulary (SIMPL-18183 note); ontology, onthology (SIMPL-393, 395) | proposed (D-001, D-017) |
| Vocabulary version | Immutable snapshot of a vocabulary with its content, description, changelog and version status; numbered 1, 2, 3 (CDM). | `CDM:vocabulary-manager-be:InternalVocabularyVersion` | `VocabularyMetadata`, `VocabularyVersionMetadataResponse`, `VocabularySearchItem` (search result); paths `/vocabularies/{vocabularyName}/versions[/{version}]` | SIMPL-18166/AC1, 18167/AC1, 18168/AC1, 18172/AC2, 18173/AC1, 18176/AC2, 18184/AC1, 18189/AC1, 18191/AC1, 18452/AC1, 18559/AC1; BP02B.04, 2B.3 | InternalVocabularyVersion (CDM); VocabularyMetadata, VocabularyVersionMetadataResponse (API); "vocabulary metadata" (API name; in stories it means the vocabulary-level data) | proposed (D-002) |
| Vocabulary name | Unique PascalCase alphanumeric name that identifies a vocabulary (CDM, API). | `InternalVocabulary.name` | path `vocabularyName`; `name` in `CreateVocabularyPayload`, `VocabularyNameQuery`, `VocabularySearchItemRequest`, `VocabularyCategory` | SIMPL-18169/AC2 ("The vocabulary name is unique"), 18162/AC1, 18164/AC1 | vocabularyName (API path) vs name (API body and query) | proposed (D-008) |
| Version number | The sequential integer, starting at 1, that the system assigns to each new version (CDM, API, SIMPL-18190). | `InternalVocabularyVersion.version` | path `{version}`, field `version` | SIMPL-18167/AC1, 18170/AC1, 18189/AC1, 18190/AC1 and AC2 ("version number"); 18173/AC1, 18176/AC3, 18189/AC3 ("version identifier") | version identifier (SIMPL-18173, 18176, 18189, 18190) | proposed (see F-014) |
| Latest version | The most recently created version of a vocabulary (CDM) or the most recent ACTIVE version (stories). | `InternalVocabulary.latestVersion` | `VocabularyCategory.version`, `VocabularyMetadata.latestVersionHref`, `VocabularySearchItem.latest`; `GET /content/{vocabularyName}` | SIMPL-18170/AC1, 18165/AC1, 18161/AC1, 18184/AC2 and AC4, 18559/AC1 | current version (SIMPL-18161), newest version (SIMPL-18165), latest ACTIVE version (SIMPL-18184, 18559) | open (D-006) |
| Vocabulary status | Lifecycle status of a vocabulary as a whole, set to ACTIVE at creation and changed by hand (SIMPL-18170/AC2). | `InternalVocabulary.status` | `VocabularyCategory.status`, enum `VocabularyStatus`, `VocabularyStatusPatchRequest`, query `status`; `PATCH /vocabularies/{vocabularyName}` | SIMPL-18161/AC1, 18163/AC1, 18170/AC1 and AC2, 18174/AC1, 18175/AC1 and AC3 | "status" for both levels; Active/Deprecated labels (SIMPL-18161, 18163) | open (relation to version status, F-001; D-016) |
| Version status | Lifecycle status of one vocabulary version (CDM). | `InternalVocabularyVersion.status` | `VocabularyMetadata.status`, enum `VocabularyStatus` (shared); `PATCH /vocabularies/{vocabularyName}/versions/{version}` | SIMPL-18172/AC2, 18173/AC1, 18176/AC2, 18184/AC1 and AC2, 18186/AC1, 18191/AC1, 18559/AC1 and AC2 | "status" (all sources) | proposed (D-007, F-015) |
| Deprecate | Mark a vocabulary or a version DEPRECATED; it is not deleted and stays available for reference, but its use is discouraged (BP02B.05, 2B.6). | value `DEPRECATED` | `VocabularyStatus.DEPRECATED` | SIMPL-18174/AC1, 18175/AC1, 18178/AC1, 18191/AC1, 18559/AC1; BP02B.05, 2B.5.3, 2B.6.1 | none | agreed (scope per level open, F-001) |
| Publish | The moment a validated new vocabulary or version becomes ACTIVE and available, done automatically (SIMPL-18186, 18187). | none (CDM open question SIMPL-18201) | none; `201` of `POST /vocabularies` and `POST .../versions` | SIMPL-18186/AC1, 18187/AC1 and AC2; BP02B.03, 2B.5.1 | "automatically become available" (SIMPL-18186), "persisted and made available" (SIMPL-18187), ACTIVE | proposed (D-016; manual step open in F-012) |
| Vocabulary content | The Turtle RDF definition of a vocabulary version, uploaded as a file and downloadable (CDM, API). | `InternalVocabularyVersion.rdfContent` | form field `vocabularyFile`; paths `/content/{vocabularyName}[/versions/{version}]`; field `contentHref` | SIMPL-18165/AC1, 18169/AC1, 18170/AC1, 18172/AC2, 18452/AC2; BP02B.01 | rdfContent (CDM), vocabularyFile (API), file, full details (SIMPL-18165, 18452) | proposed (D-015, F-022) |
| Description | Human-readable text about a vocabulary or a version (CDM). | `InternalVocabulary.description`, `InternalVocabularyVersion.description` | `description` in `CreateVocabularyPayload`, `CreateNewVocabularyVersionPayload`, `VocabularyCategory`, `VocabularyMetadata` | SIMPL-18161/AC1, 18162/AC1, 18167/AC1, 18169/AC1 | one word for two attributes | open (D-014) |
| Changelog | Human-readable entry that says what changed in a version (CDM). | `InternalVocabularyVersion.changelog` | `changelog` in both create payloads and in `VocabularyMetadata` | SIMPL-18167/AC1, 18169/AC1, 18172/AC2, 18176/AC3 | change log (SIMPL-18167, 18169), change summary (SIMPL-18172, 18176) | proposed (D-012, F-045, F-007) |
| Creation date | When a vocabulary, version or external vocabulary was created; ISO 8601 (CDM). | `creationDate` on InternalVocabulary, InternalVocabularyVersion, ExternalVocabulary | `creationDate` field and query parameter | SIMPL-18161/AC1, 18162/AC1, 18164/AC1, 18167/AC1, 18176/AC3 | date created (SIMPL-18161, 18167), creation date (SIMPL-18176) | proposed (D-012) |
| Last update date | When a vocabulary or version last changed; ISO 8601 (CDM). | `lastUpdateDate` on InternalVocabulary, InternalVocabularyVersion | field `lastUpdateDate`; query and sort `lastUpdated` | SIMPL-18161/AC1, 18162/AC1, 18164/AC1 and AC3 | lastUpdated (API query, SIMPL-18164), last updated (SIMPL-18161) | proposed (D-012) |
| Creator | Who created a vocabulary, version or external vocabulary. | none | `createdBy` on `VocabularyCategory`, `ExternalVocabulary` | SIMPL-18161/AC1, 18176/AC3 | createdBy (API) | open (D-013, F-019, F-029) |
| External vocabulary | A cached copy of a vocabulary that Simpl-Open does not control (for example DCAT, Dublin Core), registered so references to it can be validated (CDM, BP). | `CDM:vocabulary-manager-be:ExternalVocabulary` | `ExternalVocabulary`, `CreateExternalVocabularyPayload`; paths `/external-vocabularies...` | notes of SIMPL-18183 and 18184 only; BP Overview, Assumptions | cached external vocabulary, copied external vocabulary (API) | proposed (D-020) |
| Register an external vocabulary | Add a cached copy of an external vocabulary after a Turtle syntax check, without semantic validation (CDM). | relation "registers" | `POST /external-vocabularies` ("Create a cached external vocabulary") | none (SIMPL-18196 blanket only) | create, cache, copy (API) | proposed (D-020) |
| Namespace URI | The canonical namespace of an external vocabulary, used to resolve references (CDM). | `ExternalVocabulary.namespaceUri` | `namespaceUri` in `ExternalVocabulary`, `CreateExternalVocabularyPayload`, `ExternalVocabularyContentRequest`, `ExternalVocabularySearchRequest` | none | none | agreed (role as identity open, D-008) |
| Alias URI | Another namespace URI that matches the same external vocabulary during lookup (CDM). | `ExternalVocabulary.aliasUris` | `aliasUris`, `aliasUri` | none | none | agreed |
| Source URI | Where the external vocabulary was copied from (CDM, API). | `ExternalVocabulary.sourceUri` | `sourceUri` | none | none | agreed |
| Vocabulary reference | A namespace used inside vocabulary content that points to an internal vocabulary version or an external vocabulary (SIMPL-18184, API warning location; Schema Manager CDM "vocabulary references"). | none | warning `location` ("prefix 'legacy' -> ..."); `VocabularySearchItemRequest` (name, version) | SIMPL-18184/AC1 to AC4; 2B.4.3 | referenced vocabulary version (SIMPL-18184), vocabularies used (2B.4.3), semantic reference (API) | proposed (D-005) |
| Syntax validation | Automated check that the uploaded content is valid Turtle; failure stops creation (SIMPL-18181). | none (CDM text "Turtle syntax check") | 400 `VocabularyValidationProblem`, example `syntaxError` | SIMPL-18180/AC1 to AC7, 18181/AC1 and AC2, 18193, 18194; BP02B.02, 2B.4.1 | none | agreed |
| Structure validation | Check that the file meets "required structure rules" (SIMPL-18180 only). | none | none | SIMPL-18180/AC6 | none | open (F-005) |
| Semantic validation | Check that terms are unique, consistent and logically coherent, after syntax validation (SIMPL-18182). | none (CDM text "OWL semantic consistency checking") | 400 `VocabularyValidationProblem`, example `semanticError` | SIMPL-18182/AC1 to AC3, 18183/AC1 and AC2; BP02B.02, 2B.4.2 | none | agreed |
| Validation report | The structured list of validation errors with nature and location, returned when a submitted vocabulary fails validation (SIMPL-18193, 18194). | none | `VocabularyValidationProblem` with `issues` | SIMPL-18173/AC2, 18181/AC2, 18182/AC3, 18193/AC2, 18194/AC1; BP02B.06, 2B.4.4 | structured report (SIMPL-18182, 18193, 18194), structured error response (SIMPL-18181), validation feedback (SIMPL-18173), notification (2B.4.4) | proposed (D-004) |
| Validation error | A blocking problem found by syntax or semantic validation; the vocabulary or version is not created (SIMPL-18181/AC2, 18183/AC2). | none (folded into VocabularyValidationWarning) | `VocabularyValidationIssue` | SIMPL-18180/AC5 to AC8, 18181/AC2, 18183/AC2, 18193/AC2, 18194/AC1 | syntax errors, semantic errors, issues | proposed (D-003) |
| Validation warning | A non-blocking problem returned with a created vocabulary or version, for example an unresolved external namespace (API, SIMPL-18183 note). | `CDM:vocabulary-manager-be:VocabularyValidationWarning` | `VocabularyValidationWarning`, `validationWarnings` | SIMPL-18183 note, 18184/AC1 and AC4 | the CDM also uses it for blocking errors | open (D-003, F-003, F-004) |
| Warning code | Machine-readable code of a validation warning (API). | `VocabularyValidationWarning.warningCode` | `VocabularyValidationWarning.code` | none | warningCode (CDM) | proposed (D-012) |
| Vocabulary search | Read-only batch lookup of vocabulary metadata by name and optional version, used by Schema Manager (CDM). | `CDM:vocabulary-manager-be:VocabularySearch` | `POST /vocabularies/searches`, `VocabularySearchRequest`, `VocabularySearchItemRequest`, `VocabularySearchResponse`, `VocabularySearchItem` | none (SIMPL-18196 blanket only) | semantic reference resolution (API) | proposed (D-011) |
| External vocabulary search | Read-only batch lookup of external vocabularies by namespace or alias URI (CDM). | `CDM:vocabulary-manager-be:ExternalVocabularySearch` | `POST /external-vocabulary/searches`, `ExternalVocabularySearchRequest`; `POST /external-vocabularies/content` | none | none | proposed (D-011) |
| Governance Authority | The actor that manages the vocabulary lifecycle in the data space (BP Actors, CDM). | `CDM:vocabulary-manager-be:GovernanceAuthority` (external actor) | none (OAuth2 caller) | all stories; BP Actors | Governance Authority user, governance authority agent, Governance Authority End User, user with the Vocabulary Management role | proposed (F-043, D-018) |
| Vocabulary management role | The permission that allows vocabulary management actions (SIMPL-18196). | none | OAuth2 scopes `read`, `write` | SIMPL-18162/AC3, 18181, 18195/AC1 and AC2, 18196/AC1 to AC4 | GA_VOCABULARY_VIEWER, GA_VOCABULARY_ADMIN, vocabulary management permissions, admin rights, Tier1 Role | open (F-002) |
| Vocabulary Hub | The central store where the Governance Authority's vocabularies are kept (BP Assumptions). | none (CDM: Vocabulary Manager is "the authoritative vocabulary registry") | API title "Vocabulary Management API" | SIMPL-18187/AC1 and AC2, SIMPL-395 | vocabulary repository, vocabulary registry (SIMPL-18187), Vocabulary Manager (CDM) | proposed (F-051) |
| Schema Manager | The component that consumes vocabularies to validate schema definitions (CDM, context CDM). | `CDM:vocabulary-manager-be:SchemaManager` (external service) | none (caller of the search and content operations) | SIMPL-28680 | none | agreed |

Left out on purpose: health status (`HealthStatus` UP, DEGRADED, DOWN) and the paging and problem fields, which are transport, not domain.

## Value sets

### Vocabulary status (InternalVocabulary.status)

| State | CDM | API enum | Acceptance criteria | UI label | Process |
|---|---|---|---|---|---|
| ACTIVE | `ACTIVE` | `VocabularyStatus.ACTIVE` | SIMPL-18170/AC1, AC2; 18174/AC1; 18175/AC1, AC3 | `Active` (SIMPL-18161/AC1); `ACTIVE` (SIMPL-18174/AC1) | not named |
| DEPRECATED | `DEPRECATED` | `VocabularyStatus.DEPRECATED` | SIMPL-18170/AC2; 18174/AC1; 18175/AC1, AC3; 18178/AC1 ("deprecated") | `Deprecated` (SIMPL-18161/AC1, 18163/AC1); `DEPRECATED` (SIMPL-18174/AC1) | "deprecates a vocabulary" (2B.6), "Full Vocabulary Deprecation" (BP02B.05) |

### Version status (InternalVocabularyVersion.status)

| State | CDM | API enum | Acceptance criteria | UI label | Process |
|---|---|---|---|---|---|
| ACTIVE | `ACTIVE` | `VocabularyStatus.ACTIVE` (shared) | SIMPL-18170/AC1, AC2; 18172/AC2; 18173/AC1; 18184/AC1; 18186/AC1; 18191/AC1; 18559/AC1 | `Active` (SIMPL-18167/AC1) | "published" (BP02B.03, 2B.5.1) |
| DEPRECATED | `DEPRECATED` | `VocabularyStatus.DEPRECATED` (shared) | SIMPL-18172/AC2; 18173/AC1; 18176/AC2; 18184/AC2, AC4; 18191/AC1; 18559/AC1, AC2 | `Deprecated` (SIMPL-18167/AC1); `DEPRECATED` (SIMPL-18176/AC2) | "deprecated" (BP02B.03, BP02B.05, 2B.5.3) |

Transitions are not defined in one place (D-007): creation sets ACTIVE (SIMPL-18170/AC1, 18186/AC1); a new version deprecates "the previous active version" (CDM) or "all previous versions" (SIMPL-18191/AC1); manual change of a version status has no story (F-015).

### Validation warning code

| Code | CDM | API | Acceptance criteria / notes | UI |
|---|---|---|---|---|
| syntaxError | listed as a warning code | example key of the 400 error report, not a warning | SIMPL-18181/AC2: syntax errors reject the request | error list (SIMPL-18180/AC5) |
| semanticError | listed as a warning code | example key of the 400 error report, not a warning | SIMPL-18183/AC2: semantic issues reject the request | report (SIMPL-18182/AC3) |
| externalReferenceSkipped | listed | example warning code (201 of `POST /vocabularies`) | SIMPL-18183 note: "we issue warnings for external vocabularies that cannot be resolved" | not named |
| semanticReasoningSkipped | listed | not shown | not named | not named |
| deprecatedVocabularyReference | missing | example warning code (201 of `POST .../versions`) | SIMPL-18184/AC2 treats the same case as a blocking error (F-003) | warning (SIMPL-18184/AC4) |

The API has no enum for the warning code; the set is open (D-003).

### Validation result

| Value | CDM | API | Acceptance criteria |
|---|---|---|---|
| passed | none ("there is no `validationStatus` field") | none; success is the 201 | SIMPL-18180/AC4 "Marks the validation as successful"; 18181/AC1 "indicates that syntax validation has passed" (F-033); 18193/AC1; 18194/AC2 |
| failed | none | 400 with `VocabularyValidationProblem` | SIMPL-18180/AC5 to AC8; 18181/AC2; 18183/AC2; 18193/AC2; 18194/AC1 |

### External vocabulary

No states. It exists from registration until a hard delete (`DELETE /external-vocabularies/{name}`); the CDM says it "can be deleted and re-registered with newer content".
