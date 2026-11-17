# Domain review (step 23b): catalogue

Judgement of the domain language of the catalogue component: stories, the business processes BP05B and BP06, the three entry APIs (catalogue-be, query-mapper-adapter-be, catalogue-client-be) and their CDMs, with schema-manager-be.md and data-quality-rules.md as context. Input: step 23a [`domain-checks.md`](./domain-checks.md), step 20b [`ac-findings.json`](./ac-findings.json) (referred to by F-id, not repeated). Output: [`domain-findings.json`](./domain-findings.json) (D-001 to D-022) and the draft [`glossary.md`](./glossary.md). This review proposes terms; the spec owners decide.

The CDMs are judged against the APIs at entity level first. A CDM explains business concepts and need not mirror every field, so attribute differences are minor unless they hide a different concept. Where `inventory.json` misreads a clause, the story file was read instead (for example SIMPL-27998/AC2 "Or at least one dimension score", SIMPL-31817 `And` clauses).

## Summary

22 findings: 4 blockers, 10 major, 8 minor.

| Rule | Blocker | Major | Minor | Total | Findings |
|---|---|---|---|---|---|
| DDD-01 | 2 | 1 | 3 | 6 | D-001, D-003, D-014, D-018, D-019, D-020 |
| DDD-02 | 1 | 2 | 0 | 3 | D-004, D-006, D-009 |
| DDD-04 | 0 | 1 | 0 | 1 | D-008 |
| DDD-06 | 0 | 1 | 0 | 1 | D-011 |
| DDD-08 | 0 | 0 | 1 | 1 | D-021 |
| DDD-09 | 1 | 0 | 0 | 1 | D-002 |
| DDD-11 | 0 | 0 | 1 | 1 | D-022 |
| DDD-12 | 0 | 1 | 0 | 1 | D-005 |
| DDD-13 | 0 | 2 | 0 | 2 | D-007, D-010 |
| DDD-15 | 0 | 0 | 1 | 1 | D-017 |
| API-01 | 0 | 1 | 0 | 1 | D-012 |
| DATA-01 | 0 | 0 | 1 | 1 | D-015 |
| DATA-04 | 0 | 1 | 0 | 1 | D-013 |
| DATA-09 | 0 | 0 | 1 | 1 | D-016 |
| Total | 4 | 10 | 8 | 22 | |

Glossary: 49 terms, 11 agreed, 26 proposed, 12 open.

The five findings with most impact:

1. **D-002, the identity of a resource description (blocker).** Eight names (resourceIdentifier, resourceKey, id, selfDescriptionId, resourceDescriptionId, claimsGraphUri, sdHash, "parent resource description id"). A search result carries only claimsGraphUri; the detail call takes selfDescriptionId. Nothing says they are equal.
2. **D-001, resource description vs self-description (blocker).** The business and the CDMs say resource description; catalogue-be says SelfDescription in every schema and path; the adapter uses both. The federated operations still to be written need one name.
3. **D-003, overall score and classification names (blocker).** The criteria filter on overallCategory, which nothing has; the Consumer-facing API returns qualityScore and qualityClassification while catalogue-be and the adapter return overallScore and overallClassification.
4. **D-004, "federated catalogue" means two things (blocker).** In the stories and BP06 a catalogue of another data space; in the CDMs and API texts the catalogue-be service of the own data space.
5. **D-005, the lifecycle of a resource description (major).** Active and revoked in CDM and API; submitted, published, not stored in the criteria; a 202 "validation not finished" state nobody lists; no transition for a superseded version.

## Blockers and their questions

| Id | Target | Question | Proposal (not a decision) |
|---|---|---|---|
| D-001 | cross | Which name do new operations, schemas and events use, and is self-description kept only as a legacy wire name with a written mapping? | Resource description, as BP05B, BP06, the stories and all CDMs use it. Keep the existing SelfDescription schemas and paths as documented legacy names. |
| D-002 | cross | Which identifier names a resource description across its versions, which names one version, and is the claimsGraphUri of a search result the selfDescriptionId and resourceDescriptionId of the detail calls? | resourceIdentifier (the CDM name, today resourceKey) for the resource description; id for one version; search results expose the same identifier the detail call takes. See F-047. |
| D-003 | cross | Which field names does a search result and a detail carry for the overall score and classification, and is overallCategory meant as overallClassification? | Business terms overall quality score and overall quality classification; wire names overallScore and overallClassification, the names the stories (overallScore) and two of three APIs use. See F-004, F-005. |
| D-004 | cross | Which name does the catalogue-be service carry in business texts and CDMs, so that federated catalogue only means a catalogue of a Trustor data space? | Federated catalogue only for a Trustor data space's catalogue (BP06 wording); catalogue service for catalogue-be. |

## Rule by rule

| Rule | Verdict | Evidence |
|---|---|---|
| DDD-01 one term per concept | not met | Resource description / self-description (D-001); overallScore / qualityScore / metadata quality score and overallCategory / overallClassification / qualityClassification (D-003); catalogueId / catalogueIds / dataspace/catalogue identifier (D-014); searchTerm / searchString / q / keyword (D-018); own / local catalogue (D-019); five names of the federated search schema (D-020). Actor and role names are in F-064 and F-059. |
| DDD-02 one concept per term | not met | "Federated catalogue" (D-004); "active" for a resource description state, the resolved profile and a catalogue's federation flag (D-006); "schema" for the legacy catalogue store and the Schema Manager schema (D-009). |
| DDD-03 glossary per component | not met before this step | No glossary in `specs/`. Drafted in [`glossary.md`](./glossary.md). |
| DDD-04 same value sets | partly met | Equal: SchemaEvent types (SchemaPublished, SchemaRevoked), schema status (PUBLISHED, REVOKED), threshold status (PASSED, FAILED), schemaType (RESOURCE, SEARCH). Different: resource description status, criteria say published, API says active (D-005); resource type in four spellings (D-008); classification example `good` lower case against `Good` (glossary). |
| DDD-05 written UI label mapping | not met | No mapping for published / active, "Ressource" / RESOURCE (SIMPL-30850), local / own catalogue. Classification labels are dynamic; SIMPL-29292/AC3 writes the ordering rule, which is the only written mapping. |
| DDD-06 exposed objects map to CDM classes | partly met | Matched: QualityAssessmentRecord, VerificationResult, CatalogueInfo to Catalogue. Not matched: the SelfDescription family (D-001), the quality parts (D-007), the anonymous search node (D-011), session, query and legacy schema schemas (D-012, D-009). |
| DDD-07 wrappers add no concept | partly met | Pages, problems and envelopes are clean. SearchResponsePayload / searchResponsePayload carry the general service properties summary under an anonymous key (D-011); advancedSearchRequestPayload adds catalogueIds, a concept the CDM lacks (D-014). |
| DDD-08 attribute names | partly met | Concepts match; names differ for the schema event, resource description metadata, version and verification result (D-021), general service properties (D-011) and the recalculation (D-013). |
| DDD-09 one identity name | not met | Resource description (D-002); schema: schemaId, schemaName, name, uri (D-009); catalogue: catalogueId, CatalogueInfo.id (D-014). |
| DDD-10 value objects without id | met | GeneralServiceProperties, QualityFilter, NumericRangeFilter and the quality parts have no id of their own in the APIs. SelfDescriptionQualityRuleResult.id refers to the rule, not to the outcome. |
| DDD-11 changes through the root | partly met | Revoke and QAR go through the resource description path; DELETE by content hash and new versions without the root in the path do not (D-022). |
| DDD-12 lifecycle defined once | not met | Resource description states and transitions are not listed (D-005); the recalculation has one state and a contradictory trigger (D-013). Schema and profile lifecycles are defined in the Schema Manager CDM and reused correctly. |
| DDD-13 external concepts marked, not redefined | partly met | All CDMs mark external classes with stereotypes, and the external attribute lists match their owners (Schema, SchemaVersion, MetadataQualityProfile, QAR in `qm` and `dq`). But catalogue-be redefines the QAR parts (D-007), identity attributes, mapping and discovery policy have no class anywhere (D-010), and the client CDM names an actor as owner (D-017). |
| DDD-14 own language, one translation place | partly met | The Query Mapper is the stated translation layer between the catalogue client and catalogue-be. But catalogue-be speaks the upstream XFSC catalogue language (SelfDescription, sdHash, claimsGraphUri, OPENCYPHER /query) instead of its CDM's (D-001, D-012); identity mapping sits in the gateway per the adapter contract and in the adapter per SIMPL-31150 (F-050). |
| DDD-15 context map | partly met | Each CDM has reference tables, no context map; ownership statements differ (D-017). |
| DATA-01 CDM exists | met, not approved | A CDM exists for each API; all five are In review, not approved (D-015). |
| DATA-04 domain, not transfer | partly met | The client CDM removed envelopes on purpose. ResourceDescriptionResult, VerificationResult and RecalcQualityScoreJob describe API results (D-013, D-016). |
| DATA-09 business nouns | partly met | Services modelled as classes and an abbreviated class name (D-016). |
| API-01 CDM delivered with each API | partly met | Each API has a CDM; the alignment tables do not match the entry files (D-012). |

## Entities

| Business object | CDM class | Owned or external, and is that right | Identity and its names | States | Operations and ACs that change state |
|---|---|---|---|---|---|
| Resource description | `catalogue-be:ResourceDescription` | Owned by catalogue-be; external in the other four CDMs. Right for the published stage; the CDM itself leaves the split with SD Tooling open (D-017). | resourceIdentifier (CDM); resourceKey, id, sdHash (fc); selfDescriptionId, self_description_hash (fc paths); resourceDescriptionId (qm); claimsGraphUri (search items); selfDescriptionId + catalogueId (cc) (D-002) | active, revoked (CDM, API); submitted, published, not stored (ACs); pending implied by 202 (D-005) | fc POST /self-descriptions, POST /self-descriptions/resource (publish, SIMPL-27998/AC1 to AC3); fc POST .../revoke, qm PATCH /participants/resourceDescriptions/{id} (revoke, BP5B.8.1); fc DELETE /self-descriptions/{self_description_hash} (no AC) |
| Resource description version | `catalogue-be:ResourceDescriptionVersion` | Owned; right | versionNumber (CDM); currentVersion (fc), version (qm) | none; BP5B.5.10 revokes the previous version, no transition defined (D-005) | fc POST /self-descriptions/resource (no AC) |
| General service properties | `catalogue-be:GeneralServiceProperties` | Owned (value object of the resource description); right | none (value object) | none | none (part of the resource description) |
| Quality Assessment Record | `data-quality-rules:QualityAssessmentRecord` | External (Quality Scoring), persisted by catalogue-be; right, but catalogue-be's API redefines its parts (D-007) | via the resource description path; no own id in the API | none (thresholdStatus PASSED, FAILED on its parts) | fc POST .../qualityAssessmentRecord (upsert by Quality Scoring, no AC); publication with QAR (SIMPL-27998/AC1) |
| Metadata Quality Profile | `schema-manager-be:MetadataQualityProfile` | External (Schema Manager); right | profileId, profileVersion (fc recalculation body) | PUBLISHED, REVOKED (sm); "active" by resolution (D-006) | none in this component |
| Schema, schema version | `schema-manager-be:Schema`, `SchemaVersion` | External in catalogue-be and catalogue-client-be CDMs; right. catalogue-be's legacy /schemas operations contradict it (D-009) | schemaName (sm); schemaId, name, uri (APIs) | PUBLISHED, REVOKED; SEARCH uniqueness (F-056) | SIMPL-30850, SIMPL-31815 to SIMPL-31817 name schema create and version operations not in the entry files (F-011, F-054, F-055); fc POST /webhooks/schema-events receives the changes |
| Schema event | `schema-manager-be:SchemaEvent` | External; right | eventId (fc) | SchemaPublished, SchemaRevoked | fc POST /webhooks/schema-events (no AC) |
| Quality score recalculation | `catalogue-be:RecalcQualityScoreJob` | Owned; doubtful, the API treats it as an acknowledgement (D-013) | none in the API | ACCEPTED | fc POST /schemas/{schemaName}/recalcQualityScore (no AC) |
| Verification result | `catalogue-be:VerificationResult` | Owned; a computed result, not a stored entity (D-016) | none | lifecycleStatus is the would-be status of the checked description | fc POST /verification (no AC) |
| Search request | `catalogue-client-be:SearchQuery`, `SearchCriteria`, `QualityFilter`, `NumericRangeFilter` | Owned by catalogue-client-be, external in qm and fc; right | none (transient) | none | search operations; SIMPL-29292, SIMPL-29293, SIMPL-31111 to SIMPL-31142 |
| Catalogue | `catalogue-client-be:Catalogue` | External, owner Trustor Governance Authority; right | CatalogueInfo.id, catalogueId, catalogueIds, "dataspace/catalogue identifier" (D-014) | active true/false = federation status (D-006) | none in this component (configuration in SIMPL-30848, outside the entry files, F-009) |
| Search result group | none | Missing (F-033) | catalogue identifier and Governance Authority | none | SIMPL-31140, SIMPL-31142 |

## Boundaries

| Concept | Owner | How this component uses it | Remark |
|---|---|---|---|
| Schema, schema version, schema event, Metadata Quality Profile, classification, threshold | Schema Manager | Referenced, marked external in catalogue-be and catalogue-client-be CDMs, attributes match the owner | catalogue-be still exposes a legacy schema store under the same name (D-009). The schema stories (SIMPL-30850, SIMPL-31815 to SIMPL-31817) describe Schema Manager behaviour (F-055). |
| Quality Assessment Record and its parts | Quality Scoring Service | Persisted and returned by catalogue-be | catalogue-be's API renames the parts (D-007). |
| Identity attributes, attribute mapping, federation trust | IAA / authentication provider | Carried in User-Attributes, mapped by the gateway or the adapter | Not modelled in any CDM of the input set (D-010); SIMPL-30848 is implemented in the authentication provider (F-009). |
| Search request, search criteria, quality filter | catalogue-client-be | Passed through by the Query Mapper, evaluated by catalogue-be | Consistent across the three CDMs. |
| Resource description | catalogue-be (published), SD Tooling (authoring) | Owned here | Split stated as an open question in catalogue-be.md (D-017). |
| Offering price, provider information, data properties, asset properties | SD Tooling BE | Read-only sub-components in the client CDM | Not in the context CDMs of this run; not checked. |
| EDC connector endpoint | connector context | A sub-component `EdcConnector` of the resource description in the client CDM | Owner given as "Provider" (D-017); the name comes from the connector vendor, not from the catalogue's language. |
| DOME Marketplace offering | DOME (external entity) | Search scope in BP6.8 to BP6.10 | No story, CDM class or API. |

The external classes in the own CDMs agree with the context CDMs for Schema, SchemaVersion, SchemaEvent, MetadataQualityProfile (subset of attributes) and QualityAssessmentRecord. They disagree for GeneralServiceProperties between catalogue-be.md (owner) and catalogue-client-be.md (D-011, D-017).

## 23a triage

75 hits: 34 real, 41 harmless. Every real hit is part of a D-finding. 23a cites the `-itb` copies as evidence for most catalogue-be schemas; the same schemas are in the entry files (`common/common-components.yaml`, referenced from `t1-fc-openapi.yaml` and `t2-fc-openapi.yaml`), so the hits stand.

| 23a hit | Rule | Verdict | Reason or finding |
|---|---|---|---|
| `catalogue-be` | DATA-01 | real | D-015 |
| `catalogue-be#AnnotatedStatement` | DDD-06 | harmless | generic graph-query mechanics of /query, not a business object; the operation itself is in D-012 |
| `catalogue-be#EventDataSchema` | DDD-06 | real | D-021 (name differs from CDM SchemaEvent, Schema, SchemaVersion; values match) |
| `catalogue-be#EventDataVersion` | DDD-06 | real | D-021 (name differs from CDM SchemaEvent, Schema, SchemaVersion; values match) |
| `catalogue-be#OntologySchema` | DDD-06 | real | D-009 |
| `catalogue-be#Parameters` | DDD-06 | harmless | generic graph-query mechanics of /query, not a business object; the operation itself is in D-012 |
| `catalogue-be#QueryLanguage` | DDD-06 | harmless | generic graph-query mechanics of /query, not a business object; the operation itself is in D-012 |
| `catalogue-be#Role` | DDD-06 | real | D-012 (CDM excludes session endpoints that the entry file keeps) |
| `catalogue-be#SchemaCategoryStatus` | DDD-06 | real | D-021 (name differs from CDM SchemaEvent, Schema, SchemaVersion; values match) |
| `catalogue-be#SchemaChangeEvent` | DDD-06 | real | D-021 (name differs from CDM SchemaEvent, Schema, SchemaVersion; values match) |
| `catalogue-be#SelfDescription` | DDD-06 | real | D-001 |
| `catalogue-be#SelfDescriptionQualityAssessment` | DDD-06 | real | D-007 |
| `catalogue-be#SelfDescriptionQualityStatus` | DDD-06 | real | D-007 |
| `catalogue-be#SelfDescriptionStatus` | DDD-06 | real | D-001 |
| `catalogue-be#SelfDescriptions` | DDD-06 | real | D-001 |
| `catalogue-be#Session` | DDD-06 | real | D-012 (CDM excludes session endpoints that the entry file keeps) |
| `catalogue-be#Statement` | DDD-06 | harmless | generic graph-query mechanics of /query, not a business object; the operation itself is in D-012 |
| `catalogue-be#WebhookEventType` | DDD-06 | real | D-021 (name differs from CDM SchemaEvent, Schema, SchemaVersion; values match) |
| `catalogue-be#EventData` | DDD-07 | real | D-021 |
| `catalogue-be#Result` | DDD-07 | harmless | page or query result envelope |
| `catalogue-be#ResultPage` | DDD-07 | harmless | page or query result envelope |
| `catalogue-be#Results` | DDD-07 | harmless | page or query result envelope |
| `catalogue-be#SearchResponsePayload` | DDD-07 | real | D-011 (anonymous summary node) |
| `catalogue-be#SelfDescriptionPublicationResponse` | DDD-07 | harmless | wrapper that combines SelfDescription and the quality assessment; adds no new concept (the naming is D-001, D-007) |
| `catalogue-be#SelfDescriptionQualityDimensionResult` | DDD-07 | real | D-007 (redefines external DimensionRating, OverallQualityRating, RuleOutcome) |
| `catalogue-be#SelfDescriptionQualityOverallResult` | DDD-07 | real | D-007 (redefines external DimensionRating, OverallQualityRating, RuleOutcome) |
| `catalogue-be#SelfDescriptionQualityRuleResult` | DDD-07 | real | D-007 (redefines external DimensionRating, OverallQualityRating, RuleOutcome) |
| `catalogue-be#SelfDescriptionResult` | DDD-07 | real | D-001 (it is CDM ResourceDescriptionResult under the other name) |
| `catalogue-be.md#ResourceDescription` | DDD-06 | real | D-001 (exposed as SelfDescription, SelfDescriptionResult) |
| `catalogue-be.md#ResourceDescriptionResult` | DDD-06 | real | D-001 (exposed as SelfDescription, SelfDescriptionResult) |
| `catalogue-be.md#GeneralServiceProperties` | DDD-06 | real | D-011 (exposed as anonymous node n) |
| `catalogue-be.md#ResourceDescriptionVersion` | DDD-06 | real | D-021 (versions operation returns an untyped string; adapter calls it version) |
| `catalogue-be.md#RecalcQualityScoreJob` | DDD-06 | real | D-013 |
| `catalogue-be#QualityAssessmentRecord.@context` | DDD-08 | harmless | JSON-LD envelope keys of an external record, not attributes |
| `catalogue-be#QualityAssessmentRecord.@graph` | DDD-08 | harmless | JSON-LD envelope keys of an external record, not attributes |
| `catalogue-be#VerificationResult.verificationTimestamp` | DDD-08 | harmless | the CDM lists these attributes in its text; only the diagram lacks them (noted in D-021) |
| `catalogue-be#VerificationResult.lifecycleStatus` | DDD-08 | harmless | the CDM lists these attributes in its text; only the diagram lacks them (noted in D-021) |
| `catalogue-be#VerificationResult.issuer` | DDD-08 | harmless | the CDM lists these attributes in its text; only the diagram lacks them (noted in D-021) |
| `catalogue-be#VerificationResult.issuedDateTime` | DDD-08 | harmless | the CDM lists these attributes in its text; only the diagram lacks them (noted in D-021) |
| `catalogue-be#VerificationResult.validatorDids` | DDD-08 | harmless | the CDM lists these attributes in its text; only the diagram lacks them (noted in D-021) |
| `catalogue-be` | DDD-09 | real | D-002 (the hash is one of eight identity names) |
| `catalogue-be#QueryLanguage.(enum)` | DDD-04 | harmless | query-language switch of the generic /query operation, not a business value set |
| `catalogue-be#SelfDescriptionQualityStatus.(enum)` | DDD-04 | harmless | PASSED and FAILED are in the context CDM (data-quality-rules.md thresholdStatus, RuleOutcome.outcome); the names are D-007 |
| `catalogue-be#SelfDescriptionQualityStatus.(enum)` | DDD-04 | harmless | the criteria describe pass and fail in words ("greater or equal than its threshold"); no value to align |
| `catalogue-be#SelfDescriptionStatus.(enum)` | DDD-04 | real | D-005 (criteria say published, API says active) |
| `catalogue-be#SchemaCategoryStatus.(enum)` | DDD-04 | harmless | the criteria say "published ... schema" in lower case (SIMPL-31815/AC3); same values, case only |
| `catalogue-be.md#ResourceDescriptionResult` | DDD-01 | harmless | an API view; stories need not name it (DATA-09 point in D-016) |
| `catalogue-be.md#GeneralServiceProperties` | DDD-01 | real | D-011 (stories say main properties) |
| `catalogue-be.md#VerificationResult` | DDD-01 | harmless | no story covers verification; nothing to align |
| `catalogue-be.md#RecalcQualityScoreJob` | DDD-01 | harmless | no story covers recalculation (the class itself is D-013) |
| `query-mapper-adapter-be` | DATA-01 | real | D-015 |
| `query-mapper-adapter-be#advancedSearchRequestPayload` | DDD-07 | real | D-014 (carries catalogueIds, a concept the CDM lacks; the rest is SearchQuery, SearchCriteria, QualityFilter) |
| `query-mapper-adapter-be#federatedResourceDescriptionPayload` | DDD-07 | harmless | wrapper around one ResourceDescription |
| `query-mapper-adapter-be#searchResponsePayload` | DDD-07 | real | D-011 |
| `query-mapper-adapter-be#versionHistoryResponsePayload` | DDD-07 | harmless | page of ResourceDescriptionVersion entries (attribute name in D-021) |
| `query-mapper-adapter-be.md#AccessPolicyFilter` | DDD-06 | harmless | interaction concept carried by the User-Attributes header, not a schema (the term is D-010) |
| `query-mapper-adapter-be.md#AccessPolicyFilter` | DDD-01 | real | D-010 |
| `catalogue-client-be` | DATA-01 | real | D-015 |
| `catalogue-client-be#InputObject` | DDD-06 | harmless | parts of the problem detail for errors |
| `catalogue-client-be#InputValidationIssue` | DDD-06 | harmless | parts of the problem detail for errors |
| `catalogue-client-be#BadRequestProblem` | DDD-07 | harmless | error envelope |
| `catalogue-client-be#CatalogueSearchResult` | DDD-07 | harmless | page envelope of Catalogue (the CDM says so) |
| `catalogue-client-be#InternalServerErrorProblem` | DDD-07 | harmless | error envelope |
| `catalogue-client-be#NotAcceptableProblem` | DDD-07 | harmless | error envelope |
| `catalogue-client-be#NotFoundProblem` | DDD-07 | harmless | error envelope |
| `catalogue-client-be#UnauthorizedProblem` | DDD-07 | harmless | error envelope |
| `catalogue-client-be.md#SearchQuery` | DDD-06 | harmless | exposed as query parameters and request body in v1, not as named schemas; v2 entry file has no search |
| `catalogue-client-be.md#SearchCriteria` | DDD-06 | harmless | exposed as query parameters and request body in v1, not as named schemas; v2 entry file has no search |
| `catalogue-client-be.md#NumericRangeFilter` | DDD-06 | harmless | exposed as query parameters and request body in v1, not as named schemas; v2 entry file has no search |
| `catalogue-client-be.md#QualityFilter` | DDD-06 | harmless | exposed as query parameters and request body in v1, not as named schemas; v2 entry file has no search |
| `catalogue-client-be#CatalogueInfo.id` | DDD-08 | harmless | the CDM lists these attributes in its text (attribute typing removed on purpose) |
| `catalogue-client-be#CatalogueInfo.name` | DDD-08 | harmless | the CDM lists these attributes in its text (attribute typing removed on purpose) |
| `catalogue-client-be#CatalogueInfo.authorityURL` | DDD-08 | harmless | the CDM lists these attributes in its text (attribute typing removed on purpose) |
| `catalogue-client-be#CatalogueInfo.active` | DDD-08 | real | D-006 (active as federation status) |
| `catalogue-client-be.md#NumericRangeFilter` | DDD-01 | harmless | below the level of the stories; advanced-search detail |

| Rule | Hits | Real | Harmless |
|---|---|---|---|
| DATA-01 | 3 | 3 | 0 |
| DDD-01 | 6 | 2 | 4 |
| DDD-04 | 5 | 1 | 4 |
| DDD-06 | 29 | 18 | 11 |
| DDD-07 | 20 | 8 | 12 |
| DDD-08 | 11 | 1 | 10 |
| DDD-09 | 1 | 1 | 0 |
| Total | 75 | 34 | 41 |
