# Glossary: Vocabulary Manager (copy of the decided glossary of step 30a, D97)

Decided from the draft glossary of step 23b (`validation/glossary.md`). Use these terms in code, tests and UI texts. Decision ids (D1 to D37) are in [`decisions.md`](./decisions.md). CDM names are from `vocabulary-manager-be.md` (snapshot 70f951f, in review). API names are the wire names of `specs/api/vocabulary_openapi-tier1.yaml`; they stay as they are in this build, and a different code or CDM name is listed as an owner question.

## Terms

| Term | Definition | CDM class / attribute | API names | Retired synonyms | Decision |
|---|---|---|---|---|---|
| Vocabulary | A reusable set of terms in Turtle, managed by the Governance Authority, identified by its name and made of one or more versions. Say "internal vocabulary" only where a contrast with an external vocabulary is needed. | `InternalVocabulary` | schema `VocabularyCategory`; paths `/vocabularies`, `/vocabularies/{vocabularyName}` | VocabularyCategory (as a concept name), native vocabulary, local vocabulary, self managed vocabulary, ontology, onthology | D25, D31 |
| Vocabulary version | An immutable, numbered snapshot of a vocabulary: content, description, changelog, version status, creator, dates. | `InternalVocabularyVersion` | schemas `VocabularyMetadata`, `VocabularyVersionMetadataResponse` (version plus `validationWarnings`), `VocabularySearchItem` (search result); paths `/vocabularies/{vocabularyName}/versions[/{version}]` | "vocabulary metadata" for a version | D26 |
| Vocabulary name | The unique identity of a vocabulary: 3 to 64 characters, `^[A-Z][a-zA-Z0-9]*$`, unique case-insensitively. | `InternalVocabulary.name` | path `vocabularyName`; field and query `name` | none (the two API spellings are kept and mean the same) | D30, D37 |
| Version number | Integer assigned by the system: 1 at creation, then highest plus 1. | `InternalVocabularyVersion.version` | path `{version}`, field `version` | version identifier | D7 |
| Latest version | The version with the highest version number, whatever its status. | `InternalVocabulary.latestVersion` | `VocabularyCategory.version`, `latestVersionHref`, `VocabularySearchItem.latest`, `GET /content/{vocabularyName}` | current version, newest version | D5 |
| Latest ACTIVE version | The highest-numbered version whose version status is ACTIVE. Always written in full. | none | none (derived from the version list) | "latest version" when ACTIVE is meant | D5 |
| Vocabulary status | Lifecycle status of the vocabulary as a whole, stored on the vocabulary, changed only by hand. | `InternalVocabulary.status` | `VocabularyCategory.status`, enum `VocabularyStatus`, query `status`, `PATCH /vocabularies/{vocabularyName}` | "status" without level where the level matters | D3 |
| Version status | Lifecycle status of one version, stored on the version. | `InternalVocabularyVersion.status` | `VocabularyMetadata.status`, enum `VocabularyStatus` (shared), `PATCH .../versions/{version}` | none | D3, D4 |
| Effectively deprecated | A version whose own status is DEPRECATED or whose vocabulary status is DEPRECATED. Used for the deprecated-reference warning and the UI warnings, never stored. | none | none | none | D3 |
| Deprecate | Set a vocabulary or a version to DEPRECATED. Nothing is deleted; use is discouraged. | value `DEPRECATED` | `VocabularyStatus.DEPRECATED` | none | D3, D4 |
| Publish | The automatic event in which a validated new vocabulary or version is stored as ACTIVE, in the same request. No manual step, no extra state. | none | `201` of `POST /vocabularies` and `POST .../versions` | "made available", "automatically become available" | D6 |
| Vocabulary content | The Turtle definition of a version, stored as uploaded and served for download. | `InternalVocabularyVersion.rdfContent` | upload field `vocabularyFile`; `contentHref`; paths `/content/{vocabularyName}[/versions/{version}]` | file, full details, rdfContent (as a term) | D29 |
| Description | Human-readable text of a version. The vocabulary shows the description of its latest version. | `InternalVocabularyVersion.description` (`InternalVocabulary.description` to be removed) | `description` | none | D28 |
| Changelog | What changed in a version. "Initial version." for version 1 created through the UI. UI label "Change log". | `InternalVocabularyVersion.changelog` | `changelog` | change summary | D31, D32 |
| Creation date | When a vocabulary (its version 1), a version or an external vocabulary was created; UTC ISO 8601. UI label "Date created". | `creationDate` | field and query `creationDate` | date created (as a term) | D25, D31 |
| Last update date | When the vocabulary or any of its versions last changed (new version or status change); UTC ISO 8601. UI label "Last updated". | `lastUpdateDate` | field `lastUpdateDate`; query and sort value `lastUpdated` | last updated (as a term) | D25, D31 |
| Creator | The user who sent the create request: token `email` claim, else `preferred_username`. Stored per version and per external vocabulary; the vocabulary shows version 1's creator. | none (to be added) | `createdBy` (added to the version schemas) | none | D27 |
| External vocabulary | A registered cached copy of a vocabulary Simpl-Open does not control (for example Dublin Core), used to resolve references. Identified by name; namespace URI and aliases are unique lookup keys. | `ExternalVocabulary` | `ExternalVocabulary`, `CreateExternalVocabularyPayload`; paths `/external-vocabularies...` | copied external vocabulary (as a term) | D30 |
| Register an external vocabulary | Add an external vocabulary after a Turtle syntax check, without semantic validation. | relation "registers" | `POST /external-vocabularies` | create, cache, copy (as verbs) | D30 |
| Namespace URI | Canonical namespace of an external vocabulary; compared after removing a trailing `#` or `/`. | `ExternalVocabulary.namespaceUri` | `namespaceUri` | none | D30 |
| Alias URI | Another namespace URI that matches the same external vocabulary. | `ExternalVocabulary.aliasUris` | `aliasUris`, `aliasUri` | none | D30 |
| Source URI | Where the external vocabulary was copied from. | `ExternalVocabulary.sourceUri` | `sourceUri` | none | none |
| Vocabulary reference | A namespace declared with `@prefix` or `PREFIX` in uploaded content. Internal when it starts with the content URL of an internal vocabulary or version; external otherwise. | none (to be added) | warning and issue `location` ("prefix 'p' -> 'namespace'") | referenced vocabulary version, vocabularies used, semantic reference | D12 |
| Syntax validation | First validation step: the content must parse as Turtle. Failure blocks and stops validation. | none | 400 `VocabularyValidationProblem` | none | D9 |
| Structure validation | UI wording for semantic validation. Not a separate step. | none | none | structure validation (as a separate step) | D9 |
| Semantic validation | Second validation step: rules S1 to S7 of D9, all run, all errors returned. | none | 400 `VocabularyValidationProblem` | structure validation | D9 |
| Validation report | The transient list of validation errors returned when creation fails. Not stored. | none (transient, to be named) | `VocabularyValidationProblem` with `issues` | structured report, structured error response, validation feedback, notification | D14 |
| Validation error | A blocking problem from syntax or semantic validation; nothing is created. | none (to be split from `VocabularyValidationWarning`) | `VocabularyValidationIssue` | issue (as a business term), syntaxError and semanticError as warning codes | D10 |
| Validation warning | A non-blocking problem returned with a created version in `validationWarnings`; not stored, not returned elsewhere. | `VocabularyValidationWarning` | `VocabularyValidationWarning`, `validationWarnings` | none | D10 |
| Warning code | Machine-readable code of a validation warning, from a closed set (below). | `VocabularyValidationWarning.warningCode` | `VocabularyValidationWarning.code` | warningCode | D10, D31 |
| Vocabulary search | Read-only batch lookup of vocabulary or version metadata by name and optional version. A query, not a stored entity. | `VocabularySearch` (to be marked as a query) | `POST /vocabularies/searches` | semantic reference resolution | D10, D16 |
| External vocabulary search | Read-only lookup of external vocabularies by namespace or alias URI. A query, not a stored entity. | `ExternalVocabularySearch` (to be marked as a query) | `POST /external-vocabulary/searches`, `POST /external-vocabularies/content` | none | D16 |
| Governance Authority | The external actor that manages vocabularies. Its users act through the two roles below. Write "Governance Authority user" for the person. | `GovernanceAuthority` (external actor) | none (token subject) | governance authority agent, Governance Authority End User, user with the Vocabulary Management role | D16, D31 |
| Vocabulary management role | Either Keycloak realm role: `GA_VOCABULARY_VIEWER` (read operations) or `GA_VOCABULARY_ADMIN` (read and write). Write operations need the admin role. | none (referenced, owned by identity) | OAuth2 scopes `read`, `write` (not checked) | vocabulary management permissions, admin rights, Tier1 Role | D16 |
| Vocabulary Hub | The business name of this component's store of vocabularies; the component is the Vocabulary Manager. | none | API title "Vocabulary Management API" | vocabulary repository, vocabulary registry | D31 |
| Schema Manager | The component that consumes vocabularies to validate schemas. | `SchemaManager` (external service) | caller of the search and content operations | none | D1 |
| Vocabulary list | UI screen at `/vocabularies` with all vocabularies. | none | none | Vocabulary Management screen, Vocabulary Management page | D31, D34 |
| Vocabulary details | UI screen at `/vocabularies/<Name>` with the vocabulary header and its version history. | none | none | history page, history interface | D31, D35 |

## State tables

### Vocabulary status

| State | Stored value | API enum | UI label | Process word |
|---|---|---|---|---|
| ACTIVE | `ACTIVE` | `VocabularyStatus.ACTIVE` | Active | not named |
| DEPRECATED | `DEPRECATED` | `VocabularyStatus.DEPRECATED` | Deprecated | deprecated, Full Vocabulary Deprecation |

| From | To | Caused by | Result |
|---|---|---|---|
| (none) | ACTIVE | `POST /vocabularies` succeeds | 201 |
| ACTIVE | DEPRECATED | `PATCH /vocabularies/{vocabularyName}` status DEPRECATED (admin) | 200; versions unchanged |
| DEPRECATED | ACTIVE | `PATCH /vocabularies/{vocabularyName}` status ACTIVE (admin) | 200; versions unchanged |
| ACTIVE | ACTIVE, or DEPRECATED to DEPRECATED | PATCH with the current status | 409, no change |
| any | unchanged | `POST .../versions` | vocabulary status does not change |

### Version status

| State | Stored value | API enum | UI label | Process word |
|---|---|---|---|---|
| ACTIVE | `ACTIVE` | `VocabularyStatus.ACTIVE` (shared) | Active | published |
| DEPRECATED | `DEPRECATED` | `VocabularyStatus.DEPRECATED` (shared) | Deprecated | deprecated |

| From | To | Caused by | Result |
|---|---|---|---|
| (none) | ACTIVE | `POST /vocabularies` (version 1) or `POST .../versions` (new version) succeeds | 201 |
| ACTIVE | DEPRECATED | a newer version of the same vocabulary is created | every older ACTIVE version becomes DEPRECATED in the same transaction |
| ACTIVE | DEPRECATED | `PATCH .../versions/{version}` status DEPRECATED (admin) | 200 |
| DEPRECATED | ACTIVE | `PATCH .../versions/{version}` status ACTIVE (admin) | 200; other versions unchanged, so two may be ACTIVE |
| same | same | PATCH with the current status | 409, no change |

No other transitions. Nothing is deleted. A failed creation changes no status (D8).

### Validation outcome

| Outcome | HTTP | Body | Stored |
|---|---|---|---|
| Passed, no warnings | 201 | version, `validationWarnings: []` | yes |
| Passed with warnings | 201 | version, `validationWarnings` filled | yes (warnings not stored) |
| Syntax error | 400 | `VocabularyValidationProblem`, one issue, location `line N, column M` | no |
| Semantic errors | 400 | `VocabularyValidationProblem`, one issue per error | no |

### Warning codes (closed set)

| Code | When | Emitted in this build |
|---|---|---|
| `externalReferenceSkipped` | an external namespace is not registered, not a W3C core namespace (rdf, rdfs, owl, xsd, skos) and not the vocabulary's own namespace | yes |
| `deprecatedVocabularyReference` | a reference to an effectively deprecated internal version | yes |
| `semanticReasoningSkipped` | reserved for skipped OWL reasoning | no |

`syntaxError` and `semanticError` are not warning codes. They are kinds of validation error.

### External vocabulary

No states. It exists from registration until `DELETE /external-vocabularies/{name}` (hard delete) and can be registered again.
