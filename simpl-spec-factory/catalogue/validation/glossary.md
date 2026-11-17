# Glossary (draft, step 23b): catalogue

Draft glossary for the catalogue bounded context (catalogue-be, query-mapper-adapter-be, catalogue-client-be), from `specs/`, the CDM snapshot in `../../cdm/` and the 23a checks. It proposes terms; it does not decide them. Findings are in [`domain-findings.json`](./domain-findings.json) (D-ids) and [`ac-findings.json`](./ac-findings.json) (F-ids).

Status: `agreed` when all sources already use one term, `proposed` when this draft chose among synonyms, `open` when the sources disagree on the meaning. Choice rule: the term the business process and stories use most, then the CDM, then the API.

CDM ids are short for `CDM:<file>:<Class>`: `fc` = catalogue-be.md, `qm` = query-mapper-adapter-be.md, `cc` = catalogue-client-be.md, `sm` = schema-manager-be.md (context), `dq` = data-quality-rules.md (context). API files: `fc` = catalogue-be, `qm` = query-mapper-adapter-be, `cc` = catalogue-client-be (v2 entry, v1 context).

## Terms

### Resource descriptions

| Term | Definition (from the specs) | CDM class | API schema and fields | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| resource description | Structured metadata describing a data, application or infrastructure offering, signed by a Provider and published in a data space catalogue (BP05B, BP06). | `fc:ResourceDescription` (owned); external in `qm`, `cc`, `dq` | fc `SelfDescription`, `SelfDescriptionResult`, paths `/self-descriptions`, `/selfDescriptions/*`; qm `/selfDescriptions`, `/participants/resourceDescriptions`, `/federated/resourceDescriptions`; cc `/selfDescriptions/{selfDescriptionId}` | all stories; BP05B, BP06 | self-description, SelfDescription, selfDescription (fc, qm, cc paths and schemas); Resource Description; RD (SIMPL-31148) | proposed (D-001) |
| resource description identifier | The identifier of a resource description, stable over its versions. | `fc:ResourceDescription.resourceIdentifier`, `dq:ResourceDescription.resourceIdentifier` | fc `SelfDescription.resourceKey`, `.id`, path `selfDescriptionId`, filter `ids`; qm path `resourceDescriptionId`; search items `claimsGraphUri`; cc path `selfDescriptionId` | SIMPL-31150, SIMPL-31153, SIMPL-31770 ("resource identifier") | resourceKey, id, selfDescriptionId, resourceDescriptionId, claimsGraphUri, "parent resource description id" (qm `/version`); sdHash, self_description_hash for the content hash | open (D-002, F-047) |
| resource description version | One version of a resource description; a new version is an update, as resource descriptions are immutable (BP05B overview). | `fc:ResourceDescriptionVersion` (versionNumber, publicationDate) | fc `SelfDescription.currentVersion`, `GET /self-descriptions/{selfDescriptionId}/versions`; qm `versionHistoryResponsePayload` `n.version`, path `/version` | BP5B.3, BP5B.5.3, BP5B.5.10 | currentVersion, version, versionNumber | proposed (D-021) |
| publish, published | The Governance Authority makes a validated resource description available in the data space catalogue (BP05B.07, BP5B.5.9). | `fc:ResourceDescription.lifecycleStatus` value `active` | fc `SelfDescriptionStatus` `active`; quick and advanced search "active Resource Descriptions" | SIMPL-27998/AC1 to AC3, SIMPL-31143, SIMPL-31769, SIMPL-31770 ("published properties") | active (CDM, API) | proposed (D-005) |
| revoke, revoked | The Governance Authority withdraws a resource description from the catalogue on the Provider's request; existing usage contracts continue (BP05B.14, BP05B assumptions). | `fc:ResourceDescription.lifecycleStatus` value `revoked` | fc `POST /self-descriptions/{selfDescriptionId}/revoke`, `SelfDescriptionStatus` `revoked`; qm `PATCH /participants/resourceDescriptions/{resourceDescriptionId}` | BP5B.7.1, BP5B.8.1, BP5B.5.10 | none | agreed |
| resource description result | A resource description's catalogue metadata together with its full content. | `fc:ResourceDescriptionResult` (content) | fc `SelfDescriptionResult` {meta, content} | none | SelfDescriptionResult | proposed (D-001, D-016) |
| general service properties (main properties) | Summary properties of an offering used for discovery and result display: name, description, language, offering type, access point. | `fc:GeneralServiceProperties` (owned); external in `qm`, `cc` | search items `n` (fc, qm), `i` (cc v1): `name`, `description`, `inLanguage`, `offeringType`, `serviceAccessPoint`; advanced search key `simpl:GeneralServiceProperties` | SIMPL-31141, SIMPL-31767, SIMPL-31768 ("main properties"); BP6.7.1, BP6.7.2 | main properties; serviceName, serviceDescription, serviceLanguage (fc CDM) | proposed (D-011, F-039) |
| resource type | The kind of offering a resource description or schema describes: data, application or infrastructure (BP05B.01). | `fc:GeneralServiceProperties.offeringType`; `sm:Schema.resourceType`; `fc:Schema.resourceType` | search items `offeringType`; cc `GET /schemas?resourceType=` | SIMPL-31815 hints, SIMPL-31817/AC2 | offering type, offeringType; values data / DATA / App / Application / DataOffering | open (D-008) |
| verification result | The outcome of checking a JSON-LD resource description against the catalogue's schema, with the lifecycle status it would get. | `fc:VerificationResult` | fc `POST /verification` `VerificationResult` | none | none | proposed (no story; D-016) |

### Metadata quality

| Term | Definition (from the specs) | CDM class | API schema and fields | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| Quality Assessment Record (QAR) | The quality scoring result of one resource description against a Metadata Quality Profile version: rule outcomes, dimension ratings and an overall rating. | `dq:QualityAssessmentRecord` (owner); external in `fc`, `qm`, `cc` | fc `QualityAssessmentRecord` (JSON-LD), `GET` and `POST /self-descriptions/{selfDescriptionId}/qualityAssessmentRecord`; cc v1 `GET /selfDescriptions/{selfDescriptionId}/qualityAssessmentRecord` | SIMPL-27998/AC1 to AC2 | QAR; qualityAssessment (fc `SelfDescriptionPublicationResponse`) | agreed (representation open, D-007) |
| Metadata Quality Profile (MQP) | Governance template of quality dimensions, rules, weights, thresholds and score ranges with classifications, owned by Schema Manager. | `sm:MetadataQualityProfile` (owner); external in `fc`, `dq`, `cc` | fc recalculation body `profileVersion`, `profileId`; cc `GET /schemas/{schemaName}/qualityProfiles/active` | SIMPL-27998, SIMPL-29292/AC3, SIMPL-29293/AC3 | metadata quality definition (SIMPL-27998/AC1, AC2), Minimum Quality Profile (cc v1 `GET /selfDescriptions`) | proposed (D-007) |
| active Metadata Quality Profile | The profile resolved for the latest published version of a schema; it supplies the classification labels and their order. | `sm:SchemaVersion` "resolves active profile" | cc `GET /schemas/{schemaName}/qualityProfiles/active` | SIMPL-29292/AC3, SIMPL-29293/AC3 | none (but "active" has other meanings, D-006) | proposed (D-006, F-005) |
| threshold | A minimum score set in the profile, for the overall rating or for one dimension, that gates publication. | `sm:Threshold` (classificationScope overall, dimension) | not exposed | SIMPL-27998/AC1, AC2 ("overall threshold", "dimension threshold") | none | agreed |
| overall quality score | The weighted overall score of a resource description in its QAR. | `dq:QualityAssessmentRecord.qualityScore`, `dq:OverallQualityRating.weightedScore`, `qm:QualityAssessmentRecord.qualityScore` | fc and qm search items `overallScore`; cc v1 search items `qualityScore`; fc `SelfDescriptionQualityOverallResult.score` | SIMPL-27998/AC1, SIMPL-29292/AC2, SIMPL-29293/AC2, SIMPL-31140/AC4, SIMPL-31143, SIMPL-31769 | overall score, overallScore, qualityScore, metadata quality score, score, weightedScore, SelfDescriptionScore | proposed (D-003, scale F-004) |
| overall quality classification | The profile-defined label (for example Poor, Sufficient, Good) that the overall score falls in. | `dq:QualityAssessmentRecord.qualityClassification`, `sm:Classification` | fc and qm search items `overallClassification`; cc v1 `qualityClassification`; fc `SelfDescriptionQualityOverallResult.classification` | SIMPL-29292/AC3, SIMPL-29293/AC3, SIMPL-31143, SIMPL-31769; BP6.1.1 | overallCategory (ACs), overallClassification, qualityClassification, quality classification, classification | proposed (D-003) |
| dimension rating | The score, classification and threshold status of one quality dimension in a QAR. | `dq:DimensionRating` | fc `SelfDescriptionQualityDimensionResult` {name, score, classification, thresholdStatus}; cc QAR `DimensionScore` | SIMPL-27998/AC1, AC2 ("dimension score"), SIMPL-31143, SIMPL-31769 | dimension score, DimensionScore, SelfDescriptionQualityDimensionResult | proposed (D-007) |
| rule outcome | The result of one quality rule in a QAR. | `dq:RuleOutcome` (outcome, ruleType) | fc `SelfDescriptionQualityRuleResult` {id, name, status}; cc QAR `RuleEvaluation` | none | RuleResult, RuleEvaluation | proposed (D-007) |
| quality filter: minimum score, minimum classification | Optional thresholds a Consumer adds to a search; only results at or above them are returned. | `cc:QualityFilter` (minimumScore, minimumClassification) | `minimumScore`, `minimumClassification` in fc, qm and cc search operations | SIMPL-29292, SIMPL-29293 AC1 to AC4; BP6.1.1 | none | agreed |
| quality score recalculation | Rescoring all resource descriptions of a schema after its Metadata Quality Profile changes. | `fc:RecalcQualityScoreJob` | fc `POST /schemas/{schemaName}/recalcQualityScore` 202 {status, schemaName, profileVersion, estimatedResourceCount} | none | RecalcQualityScoreJob, triggerQualityScoreRecalculation | open (D-013) |

### Search

| Term | Definition (from the specs) | CDM class | API schema and fields | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| search request (search query) | A Consumer's search: a search type, a search scope, search terms and optional quality filter. | `cc:SearchQuery` (owner); external in `qm`, `fc` | request parameters and bodies of the search operations | SIMPL-31106 to SIMPL-31142, SIMPL-31767, SIMPL-31768; BP06 | search query, search request, federated search request | proposed |
| quick search | Search with one or more search terms matched against any property of a resource description (BP06 overview). | `cc:SearchQuery` without SearchCriteria | fc `GET /selfDescriptions/quickSearch`; qm `GET /selfDescriptions`, `GET /governanceAuthority/selfDescriptions`; cc v1 `GET /selfDescriptions` (`getSimpleSearch`) | SIMPL-29292, SIMPL-31102, SIMPL-31111, SIMPL-31138; BP6.1.1 | simple search, simple keyword search (cc CDM, cc v1) | proposed |
| advanced search | Search with values for one or more attributes of a resource description (BP06 overview). | `cc:SearchQuery` with `cc:SearchCriteria` | fc `POST /selfDescriptions/advancedSearch`; qm `POST /selfDescriptions/advancedSearch`, `POST /governanceAuthority/selfDescriptions/advancedSearch`; cc v1 `POST /selfDescriptions/advanced` | SIMPL-29293, SIMPL-31103, SIMPL-31112, SIMPL-31139, SIMPL-35874 | advanced structured search; path `/advanced` vs `/advancedSearch` | agreed (term), paths differ |
| search terms | The words a Consumer enters for a quick search. | `cc:SearchQuery.keyword` | fc `searchTerm`, qm `searchString`, cc v1 `q` | SIMPL-31111, SIMPL-31138; BP6.1.1 | keyword, searchTerm, searchString, q | proposed (D-018, F-026) |
| search criteria | Structured attribute filters of an advanced search, each targeting a resource description node. | `cc:SearchCriteria` (nodeName), `cc:NumericRangeFilter` | advanced search body keys such as `simpl:GeneralServiceProperties`, `simpl:OfferingPrice.price` {op1, value1, op2, value2} | SIMPL-31106 ("search criteria") | none | agreed |
| search scope (catalogue selection) | The catalogue or catalogues in which a search is performed: own, federated or DOME Marketplace (BP06 overview). | none | qm `catalogueIds`; cc v1 `catalogueIds`; `advancedSearchRequestPayload.catalogueIds` | SIMPL-31102, SIMPL-31103, SIMPL-35874; BP6.1.1, BP6.1.2 | catalogue selection, selected catalogues | proposed (D-014) |
| search result group | The results of one catalogue in a search over several catalogues, naming its catalogue and Governance Authority, with its own paging. | none | not exposed (qm returns a flat list) | SIMPL-31140, SIMPL-31142 Scenarios 1 to 3 | tab per catalogue (SIMPL-31142) | open (F-033) |
| discovery access policy | The policy on a resource description that decides which Consumers may discover it. | none (`qm:AccessPolicyFilter` is the filter, not the policy) | qm "access policy values", "discovery ABAC" | SIMPL-31111, SIMPL-31112, SIMPL-31138, SIMPL-31139, SIMPL-31150; BP06.02, BP06.07, BP6.2.1 | access policy values, discovery ABAC, AccessPolicyFilter | proposed (D-010) |
| identity attributes | The Consumer's identity attributes sent with a search or detail request and used to evaluate discovery access policies. | none | header `User-Attributes` (fc, qm) | SIMPL-31106, SIMPL-31113, SIMPL-31114, SIMPL-31138, SIMPL-31150; BP6.3.1, BP6.7.6 | public identity attributes, Consumer's attributes, User-Attributes | proposed (D-010) |
| attribute mapping | The federation-defined mapping from a Trusted Governance Authority's identity attributes to the Trustor's own. | none (external: IAA) | not exposed | SIMPL-31114, SIMPL-31138, SIMPL-31150; BP6.3.1, BP6.7.6 | federation-defined attribute mappings, mapped attributes | proposed (D-010) |
| correlation information | Data that ties a forwarded detail request to its response. | none | qm header `Correlation-Id` | SIMPL-31150/AC4, SIMPL-31153 | Correlation-Id | proposed |

### Catalogues and parties

| Term | Definition (from the specs) | CDM class | API schema and fields | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| catalogue | A catalogue of resource descriptions, registered with a Governance Authority. | `cc:Catalogue` (external, owner Trustor GA) | cc v1 `CatalogueInfo` {id, name, authorityURL, active}, `GET /catalogues` | SIMPL-31102, SIMPL-31103, SIMPL-31140 | CatalogueInfo | proposed |
| catalogue identifier | The identifier of a catalogue in a search scope or detail request. | none (prose of `cc:Catalogue`) | `catalogueId` (cc v2), `catalogueIds` (qm, cc v1), `CatalogueInfo.id` | SIMPL-33885/AC1 to AC3 | dataspace/catalogue identifier, catalogueIds | open (D-014, F-047) |
| own catalogue | A catalogue of the Consumer's own data space. | none | cc v1 `GET /selfDescriptions` (local) | SIMPL-31141, SIMPL-31143, SIMPL-31767, SIMPL-31769; BP06 | local catalogue, catalogues of my own data space, own data space catalogue | proposed (D-019) |
| federated catalogue | A catalogue of another data space whose Trustor Governance Authority trusts the Consumer's Governance Authority. | none | qm `/governanceAuthority/selfDescriptions`, `/federated/resourceDescriptions` | SIMPL-31102, SIMPL-31148, SIMPL-31768, SIMPL-31770; BP6.1.2, BP6.7.2 | catalogue of a federated data space, Trustor catalogue; the same words also name the catalogue-be service | open (D-004) |
| catalogue service (catalogue-be) | The service that stores resource descriptions of the own data space and evaluates search requests. | `qm:FederatedCatalogue`, `cc:FederatedCatalogue`, `sm:FederatedCatalogue` (external service) | catalogue-be | none | Federated Catalogue, simpl-fc-service | open (D-004) |
| federation enabled | The trust relation between a Trusted and a Trustor Governance Authority is in force. | prose of `cc:Catalogue` ("active federation status") | cc v1 `CatalogueInfo.active` | SIMPL-31113/AC2, SIMPL-31150/AC1, AC2 | trusted, no longer trusted, still enabled, active | open (D-006, F-030) |
| Governance Authority (GA) | The authority that governs a data space and its catalogue. | `sm:GovernanceAuthority` (external actor) | none | most stories; BP05B, BP06 | GA, Governance Authority user | agreed |
| Trusted Governance Authority | The Governance Authority that forwards its Consumers' searches and detail requests to other data spaces. | none | qm t1 description | SIMPL-30848, SIMPL-31113, SIMPL-31153; BP6.5, BP6.7.5 | Trusted GA | agreed |
| Trustor Governance Authority | The Governance Authority that trusts a Trusted Governance Authority and answers its forwarded requests. | `cc:TrustorGovernanceAuthority` (external) | `CatalogueInfo.authorityURL` | SIMPL-31106, SIMPL-31138, SIMPL-31139, SIMPL-31150; BP6.3, BP6.4 | Trustor GA, trustor authorities | agreed |
| Consumer | The participant that searches catalogues and consults resource descriptions. | `cc:Consumer` (external actor) | none | most search stories; BP06 | Participant (SIMPL-29292, SIMPL-29293), Provider (their titles), Customer (SIMPL-31114) | proposed (F-064) |
| Provider | The participant that creates, signs, submits and revokes resource descriptions. | `fc:Provider` (external actor) | ABAC roles `*_PROVIDER_PUBLISHER` | BP05B | none | agreed |
| DOME Marketplace offering | An offering found in the DOME Marketplace search scope, without quality rating. | none | none | none (BP6.8 to BP6.10 only) | none | open (no story, no CDM) |

### Schemas

| Term | Definition (from the specs) | CDM class | API schema and fields | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| schema | A SHACL shape that defines the structure of resource descriptions of one resource type, or the common federated search fields; versioned, owned by Schema Manager. | `sm:Schema` (owner); external in `fc`, `cc`, `dq` | cc v2 `/schemas`, `/schemas/{schemaId}/content`; fc webhook `EventDataSchema`; fc t1 `/schemas` (legacy store, OntologySchema) | SIMPL-30850, SIMPL-31815 to SIMPL-31817, SIMPL-35874 | the same word for the legacy catalogue store of ontologies, shapes and vocabularies | open (D-009) |
| schema name | The identifier of a schema. | `sm:Schema.schemaName` | `schemaName` (fc recalculation, cc quality profile), `schemaId` (cc, fc t1), `name` (cc list), `uri` (fc event) | SIMPL-30850, SIMPL-31817 (`/schemas/{schemaName}/versions`) | schemaId, name, uri | open (D-009) |
| schema version | An immutable published version of a schema. | `sm:SchemaVersion` | cc `GET /schemas/{schemaId}/{version}`; fc `EventDataVersion` | SIMPL-30850, SIMPL-31817 | version | agreed |
| federated search schema | The single schema of type SEARCH holding the attributes common to all federated catalogues. | `sm:Schema` with schemaType SEARCH | not in entry files (F-011, F-054) | SIMPL-30850, SIMPL-31815 to SIMPL-31817, SIMPL-35874 | search schema, schema of type SEARCH, SEARCH schema, federated Search Schema | proposed (D-020) |
| resource schema | A schema of type RESOURCE for one resource type. | `sm:Schema` with schemaType RESOURCE | cc v2 `/schemas` | SIMPL-30850 | resource definition schema, Ressource | proposed (D-020) |
| schema event | A signal from Schema Manager that a schema was published or revoked. | `sm:SchemaEvent`, `fc:SchemaEvent` (external) | fc `POST /webhooks/schema-events` `SchemaChangeEvent` {eventId, eventType, timestamp, data} | none | SchemaChangeEvent, webhook event | proposed (D-021) |
| schema administrator role | The role that allows a Governance Authority user to manage schemas. | none | `GA_SCHEMA_ADMIN`, `GA_SCHEMA_VIEWER` in fc t1 | SIMPL-30850, SIMPL-31815 to SIMPL-31817 | governance-admin, "role authorising schema management" | open (F-059) |

Totals: 49 terms; 11 agreed, 26 proposed, 12 open.

## Value sets

### Resource description: lifecycle status

| State | CDM (`fc:ResourceDescription.lifecycleStatus`) | API enum (fc `SelfDescriptionStatus`) | ACs and process | UI |
|---|---|---|---|---|
| submitted, awaiting validation | none | none (fc `POST /self-descriptions` 202 "validation is not finished yet") | "submitted" (SIMPL-27998/AC1 to AC3), BP05B.04 | not defined |
| published | `active` | `active` | "published" (SIMPL-27998/AC1, BP05B.07, BP5B.5.9) | not defined |
| rejected (not stored) | none | none (error response) | "not published", "not stored" (SIMPL-27998/AC2, AC3) | error message |
| revoked | `revoked` | `revoked` | "revoked" (BP5B.8.1, BP5B.5.10 for the previous version) | not defined |
| deleted | "removes it entirely" (text) | `DELETE /self-descriptions/{self_description_hash}` | none | none |

Transitions are not listed anywhere (D-005).

### Schema and schema version: lifecycle status (external, Schema Manager)

| State | CDM (`sm`, `fc`) | API enum (fc `SchemaCategoryStatus`) | ACs and process | UI |
|---|---|---|---|---|
| published | `PUBLISHED` | `PUBLISHED` | "published" (SIMPL-31815/AC3, SIMPL-31817/AC3) | not defined |
| revoked | `REVOKED` | `REVOKED` | "revoked schemas remain addressable as history" (SIMPL-31815 hints) | not defined |
| superseded version | "implicit unavailable status" (`sm` text) | none | "inactive schema" (BP5B.5.2) | not defined |

### Schema type

| Value | CDM (`sm:Schema.schemaType`) | API | ACs | UI |
|---|---|---|---|---|
| resource schema | `RESOURCE` | not in entry files | `RESOURCE` (SIMPL-30850 AC1, AC2) | "Ressource" (SIMPL-30850 technical details) |
| federated search schema | `SEARCH` | not in entry files; cc `GET /schemas` has no schemaType parameter (F-060) | `SEARCH` (SIMPL-31815, SIMPL-31817, SIMPL-35874) | not defined |

### Resource type / offering type (open, D-008)

| Value | CDM | API | ACs and process | UI |
|---|---|---|---|---|
| data | `data` (`fc`, `qm` offeringType); `DataOffering` example (`sm` resourceType) | `data` (search items), `DATA` (cc `/schemas` example), `data` (cc parameter example) | "data" (BP05B.01), "Data" (SIMPL-31815 hints), "DATA" (SIMPL-31817/AC2) | not defined |
| application | `application`; `Application` (`sm`) | `application`, `APPLICATION` (cc) | "application" (BP05B.01), "App" (SIMPL-31815 hints) | not defined |
| infrastructure | `infrastructure` | `infrastructure` (search items) | "infrastructure" (BP05B.01), "Infra" (SIMPL-31815 hints) | not defined |

### Quality classification (profile-defined)

| Value | CDM | API | ACs | UI |
|---|---|---|---|---|
| labels from the active profile | `sm:Classification.classificationName`, dynamic: Green/Yellow/Red (dimension), Poor/Medium/Good (overall) in the default profile | free string: `overallClassification` example `Green` (fc, qm), `qualityClassification` `Good`, `Medium` (cc v1), `minimumClassification` example `good` (lower case) | "Poor", "Sufficient", "Good" (SIMPL-29292/AC3, SIMPL-29293/AC3) | labels from the profile; order by score range (SIMPL-29292/AC3 note) |

The label sets in the CDM example and the criteria differ (Medium vs Sufficient), which is allowed for a dynamic set; case sensitivity of `minimumClassification` is not stated (F-005, F-006).

### Threshold status and rule outcome

| Value | CDM (`dq:DimensionRating.thresholdStatus`, `OverallQualityRating.thresholdStatus`, `RuleOutcome.outcome`) | API enum (fc `SelfDescriptionQualityStatus`) | ACs | UI |
|---|---|---|---|---|
| passed | `PASSED` | `PASSED` | "greater or equal than ... threshold" (SIMPL-27998/AC1) | not defined |
| failed | `FAILED` | `FAILED` | "lower than ... threshold" (SIMPL-27998/AC2) | error "Quality Score X is below treshold Y." (SIMPL-27998) |

Rule type (`dq:RuleOutcome.ruleType`): `EXISTENCE`, `IS_IN`, `CUSTOM_SHACL`; not exposed by the catalogue API.

### Metadata Quality Profile status (external)

| State | CDM (`sm:MetadataQualityProfile.lifecycleStatus`) | API | ACs | UI |
|---|---|---|---|---|
| published | `PUBLISHED` | none | none | none |
| revoked | `REVOKED` | none | none | none |
| active (resolved for the latest published schema version) | relation "resolves active profile", not a status | path `/qualityProfiles/active` | "active Metadata Quality Profile" (SIMPL-29292/AC3) | none |

### Schema event type

| Value | CDM (`sm:SchemaEvent.eventType`, `fc:SchemaEvent.eventType`) | API enum (fc `WebhookEventType`) | ACs | UI |
|---|---|---|---|---|
| schema published | `SchemaPublished` | `SchemaPublished` | none | none |
| schema revoked | `SchemaRevoked` | `SchemaRevoked` | none | none |

### Quality score recalculation status

| Value | CDM (`fc:RecalcQualityScoreJob.status`) | API | ACs | UI |
|---|---|---|---|---|
| accepted | `ACCEPTED` | `status` free string, example `ACCEPTED` (202) | none | none |

### Federation (trust) status of a catalogue (open, D-006)

| State | CDM | API | ACs | UI |
|---|---|---|---|---|
| enabled | "active federation status" (`cc:Catalogue` text) | `CatalogueInfo.active: true` | "trusted", "federation ... still enabled" (SIMPL-31113, SIMPL-31150/AC1) | not defined |
| disabled | same | `active: false` | "no longer trusted", "no-longer-trusted" (SIMPL-31113/AC2, SIMPL-31150/AC2) | not defined |

### Search scope (open, D-014)

| Value | CDM | API | ACs and process | UI |
|---|---|---|---|---|
| own catalogue | none | cc v1 local search, `catalogueIds` optional | "own catalogue" (BP06, SIMPL-31143), "local catalogue" (SIMPL-35874) | tab per catalogue (SIMPL-31142) |
| federated catalogue | none | qm t1 `catalogueIds` required | BP6.1.2, SIMPL-31102, SIMPL-31103 | tab per catalogue |
| all federated catalogues | none | not possible (`minItems: 1`) | BP6.1.2 "without explicitely selecting them" | none (F-020) |
| DOME Marketplace | none | none | BP6.8 to BP6.10 | none |
