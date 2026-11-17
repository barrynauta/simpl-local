# Domain review (step 23b): schema-manager

Review of the domain language of the Schema Manager: the business process BP02C, 55 stories, the OpenAPI `schema_openapi.yaml` (ref f9e5205, 18 operations, 25 schemas) and the CDM `schema-manager-be.md`, with `vocabulary-manager-be.md` and `catalogue-be.md` as context and the default-properties flowchart as background. The CDM was judged against the API at entity level first: it explains business concepts and need not mirror every field, so attribute differences count as minor unless they hide another concept.

Outputs: [`glossary.md`](./glossary.md) (draft, 54 terms) and [`domain-findings.json`](./domain-findings.json) (20 findings). Step 20b findings are referred to as `F-` and not repeated.

## Summary

| Rule | Blocker | Major | Minor | Total |
|---|---|---|---|---|
| DDD-01 one term per concept | 0 | 1 | 2 | 3 |
| DDD-02 one concept per term | 0 | 3 | 2 | 5 |
| DDD-04 same value sets | 1 | 1 | 0 | 2 |
| DDD-06 business objects match the CDM | 0 | 1 | 0 | 1 |
| DDD-08 attribute names | 0 | 0 | 1 | 1 |
| DDD-09 one name for an identity | 2 | 0 | 0 | 2 |
| DDD-12 lifecycle defined once | 0 | 1 | 0 | 1 |
| DDD-13 external concepts not redefined | 0 | 1 | 0 | 1 |
| DDD-14 own language at boundaries | 0 | 0 | 1 | 1 |
| DDD-15 context map | 0 | 0 | 1 | 1 |
| API-01 CDM with the API | 0 | 1 | 0 | 1 |
| DATA-04 business focus | 0 | 0 | 1 | 1 |
| **Total** | **3** | **9** | **8** | **20** |

Glossary: 54 terms, 13 agreed, 16 proposed, 25 open. Ten of the open terms are the metadata quality profile classes, which no story or process step uses.

What already works well: the status values PUBLISHED and REVOKED, the schema types RESOURCE and SEARCH and the event types SchemaPublished and SchemaRevoked are the same in the CDM, the API and the criteria. The webhook subscription matches its CDM class field for field. The CDM marks the Vocabulary Manager, the Federated Catalogue and the Quality Scoring Service as external, and its copies of shared concepts agree with the catalogue CDM.

### The 5 findings with most impact

1. **D-001 (blocker, DDD-09).** The schema identity is `schemaName` in the path, the CDM and the create response, and `name` in every other body. The CDM does not record that the key is name plus schemaType (plus resourceType).
2. **D-002 (blocker, DDD-09).** The version identifier has five names (`version`, `schemaVersion`, `latestVersion`, `versionNumber`, `versionInfo`), plus `mqpVersion` and `profileVersion` for profiles.
3. **D-003 (blocker, DDD-04).** A replaced version is REVOKED in the process and the stories, "unavailable" in the CDM and the API, and has no status at all in SIMPL-20530.
4. **D-005 (major, DDD-02).** "Publish" means four things (new schema, new version, status change, event). The CDM raises no event for a new version, while the process and the notification stories treat a new version as published. The e-mail Provider notification has no CDM concept.
5. **D-009 (major, DDD-01).** Metadata quality profiles have three API operations and ten CDM classes but no story and no process step. The API uses four names for the concept.

## Blockers

| Id | Rule | Question | Related |
|---|---|---|---|
| D-001 | DDD-09 | Which single name does the schema identity carry in requests, responses and the CDM, and does the CDM record the full identity key of a schema? | F-007, F-047 |
| D-002 | DDD-09 | Which one name does the version identifier carry in the API and the CDM, for schemas and for metadata quality profiles? | F-001 |
| D-003 | DDD-04 | Is a replaced version REVOKED, a separate state such as SUPERSEDED, or not a state at all because only the schema carries a status? | F-004, F-024 |

D-003 and F-004 are one decision; D-001 and F-007 should be decided together (the key decides the name).

## Rule by rule

| Rule | Verdict | Evidence |
|---|---|---|
| DDD-01 | not met | "schema concept" (10 stories) and "schema category" (API) for Schema (D-006); four prose names for each schema type (D-013); "Metadata Quality Profile", "Quality profile", "mqp", `simpl:QualityProfile` (D-009); "Webhook" and "subscription" (D-015); 12 of 13 CDM classes flagged by 23a never appear in the stories or BP02C. |
| DDD-02 | not met | "publish" for four actions (D-005); SchemaValidationIssue for errors and warnings (D-007); "default" and "mandatory" property mixed (D-011); "Governance Authority" for actor and system (D-016); "update" for new version, duplicate create and metadata change (D-019). |
| DDD-03 | not met before this step | No glossary existed. Drafted here: [`glossary.md`](./glossary.md), 54 terms, every CDM class and every exposed API business schema listed. |
| DDD-04 | partly met | Agreed: schema status, schema type, event type. Not agreed: replaced version (D-003), resource type values (D-012), "PUBLISH" in SIMPL-20530/AC2 (F-043), vocabulary ACTIVE and DEPRECATED absent from this CDM (D-008). |
| DDD-05 | partly met | Written mapping: "Latest Change (changelog)" in SIMPL-13346/AC1, status column shows the stored word. Missing: resource type labels `data`, `application`, `infrastructure` to stored values (D-012); success texts differ (F-029, F-043). |
| DDD-06 | partly met | Webhook matches. Schema is exposed as SchemaCategory (D-006); SchemaValidationWarning as SchemaValidationIssue (D-007); Vocabulary schemas have no class in this CDM (D-008); MetadataQualityProfile only through wrappers (D-009). |
| DDD-07 | partly met | 13 of 19 wrappers add no business concept. Six do: CreateSchemaResponsePayload (identity names, D-001, D-002), CreateMetadataQualityProfileResponsePayload (used for update, D-009), GetSchemaVersionsResponsePayload (external file, D-010), UpdateSchemaPayload and UpdateSchemaResponsePayload (no operation, D-019), VocabularyStatusUpdateRequest (reason, effectiveDate, D-008). |
| DDD-08 | partly met | Webhook fields match. Differences: lifecycleStatus and status, createdAt and created, rdfContent and schemaFile, latestVersion and graphName without CDM attribute, title and description placement (D-014). |
| DDD-09 | not met | Schema identity `schemaName` and `name` (D-001); version identifier five names (D-002); profile `metadataQualityProfileName` and `profileName` (D-009); vocabulary `vocabularyName` and `name` (D-008). `webhookId` is consistent. |
| DDD-10 | partly met | The API gives no id to SchemaValidationIssue (a value object) and gives `webhookId` to the Webhook entity. The CDM marks no class as a value object; ScoreRange and Threshold are `owned_entity` (D-017). |
| DDD-11 | met | All writes go through the root: versions under `/schemas/{schemaName}`, profile changes on `/metadataQualityProfiles/{metadataQualityProfileName}`, vocabulary versions under `/vocabularies/{vocabularyName}`. |
| DDD-12 | not met | No transition table in the CDM; transitions spread over SIMPL-20686, SIMPL-32801, SIMPL-20529, SIMPL-13357 and the API, and they conflict (D-004, D-003; F-002, F-003). Profiles: DELETE and PATCH make the same transition (D-009). Webhook "active" state undefined (D-015). |
| DDD-13 | partly met | External services marked (`external_service`, `external_actor`). The API re-exposes the Vocabulary Manager's vocabulary under other names without a CDM entry (D-008). |
| DDD-14 | partly met | Validation issue codes come from the Vocabulary Manager's warning codes (`externalReferenceSkipped`, D-007); "Billing Schema" from the contract clause context (D-018). |
| DDD-15 | partly met | The CDM lists referenced and referencing CDMs; the register's concept reference index is missing; the catalogue's external Schema lacks schemaType (D-020). |
| DATA-01 | partly met | The CDM exists; the register says "In review", approved "No" (D-010). |
| DATA-04 | partly met | RdfValidator "implemented with Apache Jena" and SchemaMetadata "read model" describe implementation (D-017). |
| DATA-09 | met | All classes are UML classes with noun names (Schema, SchemaVersion, Webhook, QualityRule, ...). |
| API-01 | partly met | A CDM accompanies the API, but its endpoint table lists four profile operations the frozen API lacks and two gateway APIs not in the specs (D-010). |

## Entities

| Business object | CDM class | Owned or external (and right?) | Identity and its names | States | Operations and ACs that change state |
|---|---|---|---|---|---|
| Schema | `Schema` | owned, right | `schemaName` (path, CDM, create response), `name` (bodies); key also needs schemaType and resourceType (D-001, F-007) | PUBLISHED, REVOKED | POST /schemas (SIMPL-20686/AC1 to AC3, SIMPL-32801/AC1 to AC6, SIMPL-31098/AC1, SIMPL-33537/AC1, AC2); PATCH /schemas/{schemaName} to REVOKED (SIMPL-13360/AC1, SIMPL-20529/AC2, SIMPL-13361/AC1, SIMPL-20530/AC1, SIMPL-33537/AC5); to PUBLISHED only in stories (SIMPL-20529/AC1, SIMPL-20530/AC2, SIMPL-31098/AC2; F-002) |
| Schema version | `SchemaVersion` | owned, right | schema identity plus `version` / `schemaVersion` / `versionNumber` (D-002) | PUBLISHED, REVOKED, "unavailable" (D-003) | POST /schemas/{schemaName}/versions (SIMPL-13357/AC1, SIMPL-32807/AC1 to AC3: previous version to REVOKED); no operation changes a single version's status |
| Schema metadata | `SchemaMetadata` | owned; a view, not an entity (D-017) | none of its own | mirrors Schema and version status | none (read only: GET /schemas) |
| Validation issue | `SchemaValidationWarning` | owned, right; value object | none (no id, right) | none | created by POST /schemas and POST /schemas/{schemaName}/versions (SIMPL-22407/AC3, AC4) |
| Webhook subscription | `Webhook` | owned, right | `webhookId` everywhere | none in CDM and API; "active" in SIMPL-19992/AC2 | POST /webhooks (SIMPL-19992/AC1), DELETE /webhooks/{webhookId} (SIMPL-19992/AC3) |
| Schema event | `SchemaEvent` | owned, right | none | none (eventType SchemaPublished, SchemaRevoked) | raised by publish and revoke; which actions is open (D-005, F-010) |
| Metadata quality profile | `MetadataQualityProfile` | owned, right; consumed by the catalogue and quality scoring | `metadataQualityProfileName` (API), `profileName` (CDM) | PUBLISHED, REVOKED | PATCH and DELETE /metadataQualityProfiles/{metadataQualityProfileName}; no AC |
| Metadata quality profile version | `MetadataQualityProfileVersion` | owned, right | `mqpVersion`, `profileVersion`, `versionNumber` | PUBLISHED, REVOKED | none in the frozen API (D-010) |
| Quality dimension, rule (existence, is-in, custom SHACL), threshold, score range, classification | eight classes | owned, right; parts of the profile aggregate | none exposed | none | none (inside the JSON-LD graph) |
| Vocabulary and vocabulary version | none here; `InternalVocabulary`, `InternalVocabularyVersion` in the Vocabulary Manager CDM | external; the API redefines it (D-008) | `vocabularyName` (path), `name` (bodies), `version` | ACTIVE, DEPRECATED | POST /vocabularies, PATCH /vocabularies/{vocabularyName}, POST /vocabularies/{vocabularyName}/versions; no AC |
| Default property | none (flowchart model only) | owned by Simpl-Open per the BP; not modelled (D-011) | none | none | validated in POST /schemas and versions (SIMPL-14703, SIMPL-26105) |
| Provider notification | none | not modelled (D-005) | none | none | sent after publish, new version and revoke (SIMPL-14709, SIMPL-14712, SIMPL-14791, SIMPL-33694) |
| Governance Authority | `GovernanceAuthority` | external actor, right | `authorityIdentifier` (not in the API; identity comes from the token) | none | none |

## Boundaries

| Other context | Concepts | How this component uses them | Language check |
|---|---|---|---|
| Vocabulary Manager | InternalVocabulary, InternalVocabularyVersion, ExternalVocabulary, VocabularyValidationWarning | Semantic validation calls it (CDM, SIMPL-22407). The legacy `/vocabularies` operations re-expose its vocabulary. | Not met for the legacy operations: other names (Vocabulary, VocabularyListItem) and extra fields (reason, effectiveDate) that the owner's CDM does not have (D-008). Issue codes are the owner's warning codes (D-007). |
| Federated Catalogue | ResourceDescription; external copies of Schema, SchemaVersion, SchemaEvent, MetadataQualityProfile, MetadataQualityProfileVersion | Consumes published schemas and receives schema events by webhook. | Its copies agree with this CDM (schemaName, resourceType, lifecycleStatus; PUBLISHED, REVOKED; SchemaPublished, SchemaRevoked). Its Schema lacks schemaType (D-020). Retention and federated search outcomes live there (F-025). |
| Quality Scoring Service | consumes MetadataQualityProfile | Reads the active profile (GET /schemas/{schemaName}/qualityProfiles/active). | CDM not in this component's context list; the JSON-LD type `simpl:QualityProfile` may be its name or a third one (D-009). |
| Resource description tooling and contract clauses | resource description, offeringType, contract clauses, "Billing Schema" | Default properties of every schema; SIMPL-28815 belongs there. | "offeringType" may be the same set as resourceType (D-012); "Billing Schema" reuses the word schema (D-018). |
| Identity and access (Tier 1, Tier 2 gateways) | roles, Tier2 credentials, Participant | Access control on all operations (SIMPL-17066, SIMPL-24175). | Role names differ (F-015); "Governance Authority" used for the receiving system (D-016). |
| Notification (e-mail) | Provider notification | Informs Providers (BP02C.06). | No CDM concept; recipients and channel open (D-005, F-009). |

## 23a triage

61 hits: 34 real, 27 harmless.

| Rule | Hits | Real | Harmless |
|---|---|---|---|
| DATA-01 | 1 | 1 | 0 |
| DDD-01 | 13 | 12 | 1 |
| DDD-04 | 7 | 6 | 1 |
| DDD-06 | 21 | 9 | 12 |
| DDD-07 | 19 | 6 | 13 |
| **Total** | **61** | **34** | **27** |

### Real

| Hit | Rule | Finding |
|---|---|---|
| schema-manager-be (CDM not approved) | DATA-01 | D-010 |
| SchemaValidationWarning, MetadataQualityProfile, MetadataQualityProfileVersion, QualityDimension, QualityRule, ExistenceRule, IsInRule, CustomShaclRule, Threshold, ScoreRange, Classification, SchemaEvent not in stories or BP | DDD-01 (12) | D-007, D-009, D-005 |
| VocabularyStatusUpdateRequest.status, VocabularyListItem.status, VocabularyVersionMetadata.status (each: not in a CDM value set; ACs never use) | DDD-04 (6) | D-008 |
| SchemaCategory, SchemaValidationIssue, VocabularyListItem, VocabularyVersionMetadata (API without CDM class) | DDD-06 (4) | D-006, D-007, D-008 |
| Schema, SchemaValidationWarning, MetadataQualityProfile, MetadataQualityProfileVersion, SchemaEvent (CDM class not exposed under its name) | DDD-06 (5) | D-006, D-007, D-009, D-010, D-005 |
| CreateSchemaResponsePayload, CreateMetadataQualityProfileResponsePayload, GetSchemaVersionsResponsePayload, UpdateSchemaPayload, UpdateSchemaResponsePayload, VocabularyStatusUpdateRequest | DDD-07 (6) | D-001, D-002, D-009, D-010, D-019, D-008 |

### Harmless

| Hit | Rule | Reason |
|---|---|---|
| RdfValidator not in stories or BP | DDD-01 | Internal validation capability, not a business term; the stories say "Simpl validates" (see D-017 for DATA-04). |
| stories: ARCHIVED | DDD-04 | Invalid example value in a negative criterion (SIMPL-13360/AC4), expected to give 400. |
| MetadataQualityProfileStatus | DDD-06 | An enum type for `MetadataQualityProfile.lifecycleStatus`, same values; not a business object. |
| SchemaVersion not exposed | DDD-06 | Exposed through version response wrappers (CreateNewVersionSchemaResponsePayload, versions list); mapping in the glossary. |
| SchemaMetadata not exposed | DDD-06 | Exposed as the inline item of GetAllSchemasResponsePayload.metadata. |
| RdfValidator not exposed | DDD-06 | Internal service, not a business object. |
| QualityDimension, QualityRule, ExistenceRule, IsInRule, CustomShaclRule, Threshold, ScoreRange, Classification not exposed (8) | DDD-06 | Carried inside the profile's RDF graph (ActiveMetadataQualityProfileResponsePayload `@graph`), not as API schemas; the API treats the profile content as opaque. |
| ActiveMetadataQualityProfileResponsePayload | DDD-07 | Serialisation of the profile graph; no new concept (its type name is covered in D-009). |
| CreateNewSchemaVersionPayload, CreateSchemaPayload, CreateWebhookRequest, SchemaStatusUpdateRequest, MetadataQualityProfileStatusUpdateRequest | DDD-07 | Request wrappers with fields of existing concepts (enum subset of SchemaStatusUpdateRequest is F-002). |
| CreateNewVersionSchemaResponsePayload | DDD-07 | Representation of a schema version. |
| GetAllSchemasResponsePayload, GetVocabulariesResponsePayload, GetAllVocabulariesResponsePayload | DDD-07 | Page wrappers. |
| CreateVocabularyPayload, CreateNewVocabularyVersionPayload, VocabularyVersionDetailResponsePayload | DDD-07 | Wrappers of the vocabulary concept; the boundary question is D-008. |
