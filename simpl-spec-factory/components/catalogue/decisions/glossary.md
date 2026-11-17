# Glossary (final, step 30a): catalogue

Final glossary for the catalogue build (catalogue-be, query-mapper-adapter-be, catalogue-client-be, search UI). It decides every `proposed` and `open` term of the draft in [`../validation/glossary.md`](../validation/glossary.md). The first column keeps the draft term; the second is the term the writers use in code, tests and UI texts. Decision ids refer to [`decisions.json`](./decisions.json).

CDM ids: `fc` = catalogue-be.md, `qm` = query-mapper-adapter-be.md, `cc` = catalogue-client-be.md, `sm` = schema-manager-be.md, `dq` = data-quality-rules.md. "Legacy wire name" means an existing API name kept unchanged and mapped here (D6).

## Resource descriptions

| Draft term | Term in build and tests | Definition | CDM class | API names | Retired synonyms | Decision |
|---|---|---|---|---|---|---|
| resource description | resource description | Structured metadata about a data, application or infrastructure offering, signed by a Provider and published in a data space catalogue. | `fc:ResourceDescription` | legacy wire names `SelfDescription`, `/self-descriptions`, `/selfDescriptions`; new paths use `resourceDescriptions` | self-description, SelfDescription, RD | D6 |
| resource description identifier | resource description identifier | The credential subject id (a DID) of a resource description, stable over its versions. | `fc:ResourceDescription.resourceIdentifier` | `claimsGraphUri[0]` (search items), `selfDescriptionId` (catalogue-be, catalogue-client-be paths), `resourceDescriptionId` (adapter paths) | resourceKey, sdHash for identity, "parent resource description id" | D7 |
| resource description version | resource description version | One version of a resource description, addressed by version number. | `fc:ResourceDescriptionVersion` | `GET /self-descriptions/{selfDescriptionId}/versions` | currentVersion as identifier | D7, D18 |
| publish, published | published | State of a resource description that passed validation and the quality gate and can be found. | `fc:ResourceDescription.lifecycleStatus` | stored value `active` (enum `SelfDescriptionStatus`) | active (in texts), stored | D18 |
| revoke, revoked | revoked | State of a withdrawn or superseded resource description. | `fc:ResourceDescription.lifecycleStatus` | `revoked` | none | agreed |
| resource description result | resource description with metadata | Catalogue metadata of a resource description together with its content. Not used by the build. | `fc:ResourceDescriptionResult` | legacy wire name `SelfDescriptionResult` | SelfDescriptionResult | D4, D6 |
| general service properties (main properties) | main properties (resource description summary) | name, description, offeringType, inLanguage, serviceAccessPoint, plus overallScore and overallClassification for own-catalogue results. | `fc:GeneralServiceProperties` | search item `n` (`i` in v1) | serviceName, serviceDescription, serviceLanguage (CDM attribute names, owner to align) | D13 |
| resource type | offering type (on a resource description); resource type (on a schema) | Offering type: closed set data, application, infrastructure. Resource type: Schema Manager open value on a schema, passed through. | `fc:GeneralServiceProperties.offeringType`; `sm:Schema.resourceType` | `offeringType`; `resourceType` (cc `GET /schemas`) | DATA, App, Infra, Data as offering type values | D12 |
| verification result | verification result | Outcome of checking a JSON-LD document. Not built. | `fc:VerificationResult` | `POST /verification` (not built) | none | D4 |

## Metadata quality

| Draft term | Term in build and tests | Definition | CDM class | API names | Retired synonyms | Decision |
|---|---|---|---|---|---|---|
| Quality Assessment Record (QAR) | Quality Assessment Record | The JSON-LD graph from the Quality Scoring Service for one resource description; stored and returned unchanged. | `dq:QualityAssessmentRecord` (external) | `QualityAssessmentRecord`; `qualityAssessment` is a projection | none | D11 |
| Metadata Quality Profile (MQP) | Metadata Quality Profile | Schema Manager template of dimensions, rules, thresholds and score ranges with classifications. | `sm:MetadataQualityProfile` (external) | `profileVersion`, `/qualityProfiles/active` | metadata quality definition, Minimum Quality Profile | D11 |
| active Metadata Quality Profile | active Metadata Quality Profile | The profile resolved for the latest published version of a schema. | `sm:SchemaVersion` "resolves active profile" | `GET /schemas/{schemaName}/qualityProfiles/active` | none | D10, D15 |
| threshold | threshold | Minimum score, overall or per dimension, for publication. Fraction 0 to 1. | `sm:Threshold` | not exposed | none | agreed, D14 |
| overall quality score | overall quality score | Weighted overall score of a resource description, fraction 0 to 1; UI shows percent. | `dq:QualityAssessmentRecord.qualityScore` | `overallScore` (search items, all APIs) | qualityScore (v1 wire), overall score, metadata quality score | D8, D14 |
| overall quality classification | overall quality classification | The profile label of the range the overall score falls in. | `dq:QualityAssessmentRecord.qualityClassification` | `overallClassification` | overallCategory, qualityClassification (v1 wire) | D8, D15 |
| dimension rating | dimension rating | Score, classification and threshold status of one quality dimension. | `dq:DimensionRating` | `SelfDescriptionQualityDimensionResult` (catalogue read model) | dimension score, DimensionScore | D11 |
| rule outcome | rule outcome | Result of one quality rule. | `dq:RuleOutcome` | `SelfDescriptionQualityRuleResult` (catalogue read model) | RuleEvaluation, RuleResult | D11 |
| quality filter: minimum score, minimum classification | minimum score, minimum classification | Optional search filters; results at or above them are returned. | `cc:QualityFilter` | `minimumScore` (0 to 1), `minimumClassification` | none | agreed, D14 to D16 |
| quality score recalculation | quality score recalculation | An acknowledged request to rescore a schema's resource descriptions. Not built. | `fc:RecalcQualityScoreJob` (to become a request, owner) | `POST /schemas/{schemaName}/recalcQualityScore` (not built) | RecalcQualityScoreJob as a record | D4 |

## Search

| Draft term | Term in build and tests | Definition | CDM class | API names | Retired synonyms | Decision |
|---|---|---|---|---|---|---|
| search request (search query) | search request | A Consumer's quick or advanced search with a search scope, search terms or criteria, and optional quality filter. | `cc:SearchQuery` | request parameters and bodies | search query, federated search request | D24 |
| quick search | quick search | Search with terms matched against any literal value of a resource description. | `cc:SearchQuery` | `GET /selfDescriptions/quickSearch` (fc), `GET /selfDescriptions`, `GET /governanceAuthority/selfDescriptions`, `GET /federated/resourceDescriptions` (qm) | simple search, simple keyword search | D19 |
| advanced search | advanced search | Search with values for named properties, all of which must match. | `cc:SearchCriteria` | `.../advancedSearch` | advanced structured search | agreed, D19 |
| search terms | search terms | The words of a quick search, split on whitespace. | `cc:SearchQuery.keyword` | `searchTerm` (fc), `searchString` (qm), legacy wire names | keyword, q | D19 |
| search criteria | search criteria | Property filters of an advanced search. | `cc:SearchCriteria` | advanced search body | none | agreed |
| search scope (catalogue selection) | search scope | The list of selected catalogues of a search. | `cc:SearchQuery` (owner to add) | `catalogueIds` | catalogue selection, selected catalogues | D13, D22 |
| search result group | search result group | The results of one catalogue in a federated search, with its own paging and a status. | none (owner to add) | `groups[]` {catalogueId, catalogueName, authorityUrl, status, totalCount, items, page, pageSize, totalPages} | tab (UI only) | D24, D25 |
| discovery access policy | discovery access policy | The list of identity attribute codes allowed to discover a resource description (build assumption). | part of `fc:ResourceDescription` (owner to model) | not exposed | access policy values, discovery ABAC, AccessPolicyFilter | D20, D21 |
| identity attributes | identity attributes | The Consumer's identity attribute codes sent with a request. | external (IAA) | header `User-Attributes`, comma separated | public identity attributes, Consumer's attributes | D20 |
| attribute mapping | attribute mapping | Federation-defined mapping of a Trusted GA's attributes to the Trustor's; done by the gateway. | external (IAA, gateway) | not exposed | mapped attributes | D27 |
| correlation information | correlation id | UUID tying a forwarded detail or search to its response. | none | header `Correlation-Id` | correlation information | D28, D30 |

## Catalogues and parties

| Draft term | Term in build and tests | Definition | CDM class | API names | Retired synonyms | Decision |
|---|---|---|---|---|---|---|
| catalogue | catalogue | A catalogue of resource descriptions. | `cc:Catalogue` | `CatalogueInfo` {id, name, authorityURL, active}, `GET /catalogues` | none | D23 |
| catalogue identifier | catalogue identifier | Identifier of one catalogue. | `cc:Catalogue` | `catalogueId`, `catalogueIds`, `CatalogueInfo.id` | dataspace/catalogue identifier | D7, D13 |
| own catalogue | own catalogue | The single catalogue of the Consumer's own data space (id `ownCatalogue` in tests). | none | local search operations | local catalogue, catalogues of my own data space | D9, D22 |
| federated catalogue | federated catalogue | A catalogue of a Trustor data space. | `cc:Catalogue` | `/governanceAuthority/*`, `/federated/*` | Trustor catalogue, catalogue of a federated data space | D9 |
| catalogue service (catalogue-be) | catalogue service | The catalogue-be service storing and searching resource descriptions of the own data space. | `qm:FederatedCatalogue` (owner to rename) | catalogue-be | Federated Catalogue (for the service), simpl-fc-service | D9 |
| federation enabled | federation enabled / federation disabled | Whether the trust relation between a Trusted and a Trustor GA is in force. | prose of `cc:Catalogue` | `CatalogueInfo.active` true / false | trusted, no longer trusted, still enabled | D10 |
| Governance Authority (GA) | Governance Authority | Authority that governs a data space and its catalogue. | `sm:GovernanceAuthority` | none | GA | agreed |
| Trusted Governance Authority | Trusted Governance Authority | GA that forwards its Consumers' requests to Trustor GAs. | none | none | Trusted GA | agreed |
| Trustor Governance Authority | Trustor Governance Authority | GA that answers forwarded requests. | `cc:TrustorGovernanceAuthority` | `authorityURL` | Trustor GA, trustor authorities | agreed |
| Consumer | Consumer | Participant that searches and consults resource descriptions. | `cc:Consumer` | none | Participant, Provider (as searcher), Customer | D6 |
| Provider | Provider | Participant that publishes and revokes resource descriptions. | `fc:Provider` | `*_PROVIDER_PUBLISHER` | none | agreed |
| DOME Marketplace offering | DOME Marketplace offering | Offering in the DOME search scope. Out of the build (no story). | none | none | none | D5 |

## Schemas

| Draft term | Term in build and tests | Definition | CDM class | API names | Retired synonyms | Decision |
|---|---|---|---|---|---|---|
| schema | schema | A Schema Manager SHACL schema; the catalogue reads it only. The catalogue-be legacy store is not built. | `sm:Schema` (external) | cc v2 `/schemas`, `/schemas/{schemaId}/content` | schema for the legacy ontology store | D1, D4 |
| schema name | schema name | Identifier of a schema. | `sm:Schema.schemaName` | `schemaName`; `schemaId` and `name` in cc carry the same value | schemaId (as a separate concept), uri | D7, D29 |
| schema version | schema version | An immutable published version of a schema. | `sm:SchemaVersion` | `GET /schemas/{schemaId}/{version}` | none | agreed |
| federated search schema | federated search schema | The single published schema of type SEARCH, used for advanced search over Trustor catalogues. | `sm:Schema` (schemaType SEARCH) | `GET /schemas?schemaType=SEARCH` | search schema, SEARCH schema, schema of type SEARCH | D2, D29 |
| resource schema | resource schema | A schema of type RESOURCE. | `sm:Schema` (schemaType RESOURCE) | `GET /schemas?schemaType=RESOURCE` | resource definition schema, Ressource | D2 |
| schema event | schema event | Schema Manager signal that a schema was published or revoked. Not consumed by the build. | `sm:SchemaEvent` | `POST /webhooks/schema-events` (not built) | SchemaChangeEvent | D4 |
| schema administrator role | schema administrator role | Role to manage schemas; Schema Manager scope. | none | `GA_SCHEMA_ADMIN` | governance-admin, "role authorising schema management" | D1 |
| search role | search role | Role a Consumer needs to search. | none | `CATALOGUE_SEARCHER` | "role authorising searching" | D20 |

## State and value tables

### Resource description lifecycle (D18)

| State | Stored value (CDM, API enum) | ACs and process | UI |
|---|---|---|---|
| published | `active` | published (SIMPL-27998, BP05B.07) | not shown (only published ones appear) |
| revoked | `revoked` | revoked (BP5B.8.1, previous version BP5B.5.10) | not shown |

Transitions: publication (201) creates a published version and revokes the previous published version; revoke moves published to revoked (idempotent). A rejected publication (422 or 500) stores nothing, so "not stored" is an outcome, not a state. No pending state, no deletion.

### Federated search group status (D25)

| Status | Meaning | UI |
|---|---|---|
| `complete` | The catalogue was searched; items may be empty | tab with results or "No results." |
| `unavailable` | The Trustor could not be reached, refused, failed, timed out, or federation is disabled | tab with "This catalogue is not available right now." |

### Federation status of a catalogue (D10)

| State | Wire (`CatalogueInfo.active`) | Stories |
|---|---|---|
| federation enabled | `true` | trusted, still enabled |
| federation disabled | `false` | no longer trusted |

### Quality score display (D14)

| Wire (0 to 1) | UI (percent) |
|---|---|
| `0.75` | 75 % |
| `minimumScore` sent | UI input / 100 |

### Quality classification (D15)

Labels come from the active Metadata Quality Profile. Order by lower bound of the overall score range; lower bound inclusive, upper bound exclusive except the top range. Test profile: Poor [0, 0.4), Sufficient [0.4, 0.7), Good [0.7, 1]. Labels compare case-insensitively.

### Threshold status and rule outcome

`PASSED`, `FAILED` (CDM and API agree, agreed).

### Schema type (D2, Schema Manager)

| Value | Meaning |
|---|---|
| `RESOURCE` | resource schema, default when not given |
| `SEARCH` | federated search schema, at most one published |

### Offering type (D12)

`data`, `application`, `infrastructure` (lower case).

### Search scope (D22, D24, D5)

| Value | In build |
|---|---|
| own catalogue | yes, preselected, id `ownCatalogue` |
| federated catalogue | yes, selected explicitly |
| all federated catalogues | no |
| DOME Marketplace | no |
