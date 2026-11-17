# Domain review: Vocabulary Manager (step 23b)

Judgement of the domain language across the stories (35), the business process BP02B, the OpenAPI `vocabulary_openapi-tier1.yaml` (19 operations) and the CDM `vocabulary-manager-be.md` (context: `schema-manager-be.md`). Findings are in [`domain-findings.json`](./domain-findings.json), the draft glossary in [`glossary.md`](./glossary.md). Step 20b findings (F-nnn) are referred to, not repeated.

## Summary

The component speaks one language on most details: the status values ACTIVE and DEPRECATED are the same in the CDM, the API enum and the criteria, and the external vocabulary fields (namespace URI, alias URIs, source URI) match one to one. The gaps are in the names of the two main objects and in the validation outcome. The vocabulary and its version each carry three or four names across the sources, and the CDM uses "warning" for blocking errors as well as for warnings.

20 findings: 3 blockers, 7 major, 10 minor.

| Rule | Blocker | Major | Minor | Total |
|---|---|---|---|---|
| DDD-01 | | | 2 | 2 |
| DDD-02 | 1 | 1 | 1 | 3 |
| DDD-05 | | | 1 | 1 |
| DDD-06 | 2 | | | 2 |
| DDD-07 | | 2 | | 2 |
| DDD-08 | | | 3 | 3 |
| DDD-09 | | 1 | | 1 |
| DDD-12 | | 1 | | 1 |
| DDD-13 | | | 1 | 1 |
| DDD-14 | | 1 | | 1 |
| DDD-15 | | | 1 | 1 |
| DATA-01 | | 1 | | 1 |
| DATA-04 | | | 1 | 1 |
| **Total** | **3** | **7** | **10** | **20** |

### The 5 findings with most impact

1. **D-003 (blocker).** The CDM lists `syntaxError` and `semanticError` as validation warning codes, while the API rejects those cases with a 400 report. Errors and warnings need to be two concepts (or one concept with a severity).
2. **D-001 (blocker).** The vocabulary is "vocabulary" in the stories, `InternalVocabulary` in the CDM and `VocabularyCategory` in the API, and that schema mixes vocabulary-level status with latest-version data.
3. **D-002 (blocker).** The version is "vocabulary version", `InternalVocabularyVersion`, `VocabularyMetadata` and `VocabularyVersionMetadataResponse`; "vocabulary metadata" means the vocabulary level in the stories.
4. **D-006 (major).** "Latest version" means the most recently created version in the CDM and API, and the most recent ACTIVE version in the stories.
5. **D-005 (major).** The vocabulary reference, which drives the status check of 2B.4.3 and the deprecation warning, has no concept in the CDM, and it is not stated how a reference identifies an internal version.

## Blockers and their questions

| Id | Target | Question | Options |
|---|---|---|---|
| D-001 | `VocabularyCategory` | Is the object of `GET /vocabularies` and `GET`/`PATCH /vocabularies/{vocabularyName}` the vocabulary (InternalVocabulary) or a read view of its latest version, and which one name should the CDM and the API use? | Vocabulary, named Vocabulary (stories use this most); keep InternalVocabulary in both; or a named read model such as VocabularySummary |
| D-002 | `VocabularyMetadata` | Which one name should the version carry in the CDM and the API, and is the creation response a separate concept? | VocabularyVersion in both, the 201 as version plus warnings; or InternalVocabularyVersion in both |
| D-003 | `VocabularyValidationWarning` | Are blocking validation errors and non-blocking warnings two concepts, and what is the code set of each? | Two classes with their own code sets; or one VocabularyValidationIssue with a severity |

Proposals and reasons: for D-001 and D-002 the glossary proposes "vocabulary" and "vocabulary version", because the process and nearly every story use them (the qualifier question is D-017). For D-003 it proposes "validation error" and "validation warning", because the API and the stories already keep them apart (SIMPL-18193/AC2 "blocking validation errors", SIMPL-18183 note on warnings); only the CDM merges them.

## Rule by rule

| Rule | Verdict | Evidence |
|---|---|---|
| DDD-01 one term per concept | not met | Vocabulary, version, validation report, latest version, content and several fields have two to five names (D-001, D-002, D-004, D-006, D-012, D-015, D-017, D-020; actors F-043; screens F-044). |
| DDD-02 one concept per term | not met | "validation warning" covers errors and warnings in the CDM (D-003); "latest version" (D-006); "description" on two levels (D-014); "vocabulary metadata" names the vocabulary level in stories and the version in the API (D-002). |
| DDD-03 glossary | partly met | None existed; [`glossary.md`](./glossary.md) drafts 34 terms (7 agreed, 20 proposed, 7 open). Every CDM class and every exposed business schema appears in it. |
| DDD-04 same value sets | partly met | Vocabulary and version status: ACTIVE, DEPRECATED in CDM value sets, API `VocabularyStatus` and the criteria (SIMPL-18170/AC1, 18175/AC1, 18191/AC1). Warning codes differ: CDM has 4, API examples show `deprecatedVocabularyReference` not in the CDM and no enum (D-003). |
| DDD-05 UI label mapping | not met | `Active`, `Deprecated` in SIMPL-18161/AC1, 18167/AC1, 18163/AC1 against `ACTIVE`, `DEPRECATED` elsewhere; "published" in BP02B.03. No mapping written (D-016). |
| DDD-06 business object = CDM class of same name | partly met | 5 of 11 business schemas match (`ExternalVocabulary`, `VocabularyValidationWarning`, the two search requests, the search response). The two main objects do not (D-001, D-002). The CDM points to this API (`openapi/vocabulary_openapi-tier1.yaml`). |
| DDD-07 wrappers add no concept | partly met | 11 of 13 wrappers are clean (payloads, pages, patch request, error base). `VocabularyValidationProblem` carries the validation report (D-004), `VocabularyVersionMetadataResponse` is a third version schema (D-002). |
| DDD-08 attribute = CDM attribute | partly met | Matched classes mostly align. Gaps: `createdBy` (D-013), `code` vs `warningCode`, `lastUpdated` vs `lastUpdateDate` (D-012), content (D-015), `version` vs `latestVersion` (D-001). |
| DDD-09 one name for identity | partly met | Version identity (`vocabularyName`, `version`) is stable. Vocabulary name is `vocabularyName` in paths, `name` elsewhere; external vocabulary is identified by namespace URI in the CDM and by `name` in the API (D-008). |
| DDD-10 value objects have no id | partly met | No value object gets an id in the API (warnings, search items). The CDM marks no value objects: warnings and the two searches are "owned entities" (D-011). |
| DDD-11 changes through the root | met | Version creation and version status change go through `/vocabularies/{vocabularyName}/versions...`; external vocabularies are their own root. |
| DDD-12 lifecycle defined once | partly met | States listed in CDM value sets; transitions only in prose, and the CDM ("the previous active version") differs from SIMPL-18191/AC1 ("all previous versions") (D-007; see F-001, F-015, F-038). |
| DDD-13 external concepts marked | partly met | Schema Manager is an external service in this CDM and matches the context CDM. GovernanceAuthority is an external actor with an attribute, defined the same way in both CDMs, with no owner named (D-018). |
| DDD-14 own language at the boundary | partly met | The API uses its own terms; no Schema Manager terms leak into schema names. But Schema Manager still has legacy `/vocabularies` endpoints, and SIMPL-393, 395, 28680 use catalogue or Schema Manager language (D-010). |
| DDD-15 context map | partly met | Pairwise traceability tables in both CDMs agree; the concept ownership index is not in the snapshot (D-019). |
| DATA-01 CDM exists | partly met | Exists; register says "In review", approved "No" (D-009). |
| DATA-04 business focus | partly met | Mostly business terms. Two read-only searches are modelled as owned entities; "Turtle", "rdfContent" lean on format (D-011, D-015). |
| DATA-09 UML classes with business nouns | partly met | Class diagram with noun names. `VocabularySearch` and `ExternalVocabularySearch` name operations, not things (D-011). |
| API-01 CDM with every API | met | One API, one CDM, linked both ways (CDM "API definition: openapi/vocabulary_openapi-tier1.yaml"). It covers all business entities, under other names (D-001, D-002). |

## Entities

| Business object | CDM class | Owned or external (right?) | Identity and its names | States | Operations and ACs that change state |
|---|---|---|---|---|---|
| Vocabulary | `InternalVocabulary` | owned (right) | name: `vocabularyName` (path), `name` (body, query, search) | ACTIVE, DEPRECATED | `POST /vocabularies` sets ACTIVE (SIMPL-18170/AC1, 18186/AC1, 18187/AC1); `PATCH /vocabularies/{vocabularyName}` ACTIVE <-> DEPRECATED (SIMPL-18170/AC2, 18174/AC1, 18175/AC1 to AC3); effect on versions open (F-001) |
| Vocabulary version | `InternalVocabularyVersion` | owned (right) | (vocabulary name, `version` integer); "version number" or "version identifier" in stories | ACTIVE, DEPRECATED | `POST /vocabularies` creates version 1 ACTIVE (SIMPL-18170/AC1); `POST .../versions` new ACTIVE, previous DEPRECATED (SIMPL-18172/AC2, 18173/AC1, 18189/AC1, 18190/AC1, 18191/AC1); `PATCH .../versions/{version}` no story (F-015) |
| External vocabulary | `ExternalVocabulary` | owned (right: the cached copy is this component's) | `name` in API paths; namespace URI plus aliases in the CDM (D-008) | none | `POST /external-vocabularies`, `DELETE /external-vocabularies/{name}`; no criteria (only SIMPL-18196 blanket) |
| Validation warning | `VocabularyValidationWarning` | owned; should be a value object (D-011) | none | none; code set (D-003) | created by `POST /vocabularies` and `POST .../versions` (SIMPL-18183 note, 18184/AC1) |
| Validation error and report | none | missing (D-003, D-004) | none | passed, failed (no field, F-033) | 400 of both create operations (SIMPL-18181/AC2, 18183/AC2, 18193/AC2, 18194/AC1) |
| Vocabulary search, external vocabulary search | `VocabularySearch`, `ExternalVocabularySearch` | owned entities; are queries (D-011) | none | none | read-only |
| Vocabulary reference | none | missing (D-005) | namespace in content (unclear) | none | checked during both create operations (SIMPL-18184/AC1 to AC3) |
| Governance Authority | `GovernanceAuthority` | external actor (right; owner not named, D-018) | `authorityIdentifier` (not used in the API) | none | none |
| Schema Manager | `SchemaManager` | external service (right) | none | none | none |

## Boundaries

- **Schema Manager** is the main consumer. It calls the vocabulary search, the external vocabulary search and the content download by namespace. Both CDMs agree on the direction and on Vocabulary Manager as the authoritative registry. The external classes match: this CDM's `SchemaManager` and the context CDM's `VocabularyManager` describe the same relation from both sides.
- **Shared ownership of "vocabulary".** Schema Manager keeps legacy `/vocabularies` endpoints "for backward compatibility" (context CDM). Until they are retired, two components expose the concept (D-010).
- **Different lifecycle words next door.** Schema Manager uses PUBLISHED and REVOKED for schemas and quality profiles; this component uses ACTIVE and DEPRECATED. That is allowed between contexts. BP02B borrows "publish" for the moment a version becomes ACTIVE; the glossary keeps it as the event name only (D-016).
- **Shared validation.** Schema Manager's semantic validation uses "the integrated semantic-validation SDK", and SIMPL-28680 says validation "is done using same functionality as Schema Manager". Schema Manager names its warning field `code`, as this API does, while this CDM says `warningCode` (D-012).
- **Catalogue and Schema Manager stories in this set.** SIMPL-395 talks about the catalogue's vocabulary calls and SHACL schemas, SIMPL-393 about ontologies, SIMPL-28680 about a Schema Manager endpoint (F-010, F-041, F-042, D-010).
- **Identity and roles.** GovernanceAuthority and the roles `GA_VOCABULARY_VIEWER` and `GA_VOCABULARY_ADMIN` belong to an identity or participant context that neither CDM names (D-018, F-002).

## 23a triage in numbers

34 hits: 15 real, 19 harmless.

| Rule | Hits | Real | Harmless | Notes |
|---|---|---|---|---|
| DATA-01 | 1 | 1 | 0 | D-009 |
| DDD-06 (schema without class) | 6 | 4 | 2 | real: `VocabularyCategory` (D-001), `VocabularyMetadata`, `VocabularySearchItem` (D-002), `VocabularyValidationIssue` (D-003); harmless: `InputValidationIssue` (generic error schema), `VocabularyStatus` (the enum of the CDM status value sets, which match) |
| DDD-06 (class not exposed) | 2 | 2 | 0 | `InternalVocabulary`, `InternalVocabularyVersion` are exposed, under other names (D-001, D-002) |
| DDD-07 | 13 | 2 | 11 | real: `VocabularyValidationProblem` (D-004), `VocabularyVersionMetadataResponse` (D-002); harmless: `CollectionPage` and the three list responses (paging), the three create payloads (requests for CDM classes; side points in D-014, D-015), `ExternalVocabularyContentRequest` (lookup key), `VocabularySearchItemRequest` (search criterion), `VocabularyStatusPatchRequest` (command), `InputValidationProblem` (generic error) |
| DDD-08 | 5 | 2 | 3 | real: `ExternalVocabulary.createdBy` (D-013), `VocabularyValidationWarning.code` (D-012); harmless: `ExternalVocabulary.href` (link), `ExternalVocabularySearchRequest.namespaceUri` and `.aliasUri` (single-value forms of the CDM's `namespaceUris`, `aliasUris`) |
| DDD-04 | 1 | 0 | 1 | the criteria do use ACTIVE and DEPRECATED (SIMPL-18170/AC1, 18175/AC1, 18191/AC1); the label question is D-016 |
| DDD-01 | 6 | 4 | 2 | real: `InternalVocabulary` (D-001, D-017), `InternalVocabularyVersion` (D-002), `VocabularySearch`, `ExternalVocabularySearch` (not business concepts, D-011); harmless: `ExternalVocabulary` (the process and stories say "external vocabularies", plural), `VocabularyValidationWarning` (stories say "warnings"; the meaning issue is D-003) |
| **Total** | **34** | **15** | **19** | |

Not found by 23a and added here: D-005, D-006, D-007, D-008 (identity part), D-010, D-014, D-016 (label part), D-018, D-019, D-020.
