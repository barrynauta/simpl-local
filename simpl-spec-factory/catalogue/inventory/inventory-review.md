# Spec inventory review: catalogue

Step 12b review of [`inventory.json`](./inventory.json) (step 12a), against `specs/` only. Links are in [`links.json`](./links.json). This step extracts and links. It does not judge the specs.

## Totals

| Item | Count |
|---|---|
| Extraction issues reported by the script | 22 |
| of which parser errors | 0 (1 partly: the message for SIMPL-31142) |
| of which real properties of the spec | 22 |
| Extra parser misreads found in this review | 12 |
| Links | 134 |
| Links named | 6 |
| Links inferred | 128 |

## Extraction issues

### Issues the script reported

| Issue | Parser error or real | What to change |
|---|---|---|
| SIMPL-29292/AC6 struck | Real. The whole scenario is struck in Jira (`-AC6-`, every line in `-...-`) | Nothing |
| SIMPL-29292/AC7 struck | Real, same form | Nothing |
| SIMPL-29293/AC6 struck | Real, same form | Nothing |
| SIMPL-29293/AC7 struck | Real, same form | Nothing |
| SIMPL-31106/AC1 no Given | Real. The criterion starts with **When** | Nothing |
| SIMPL-31111/AC1 no Given | Real. Starts with **When** | Nothing |
| SIMPL-31112/AC1 no Given | Real. Starts with **When** | Nothing |
| SIMPL-31113/AC1 no Given | Real. Starts with **When** | Nothing |
| SIMPL-31113/AC2 no Given | Real. Starts with **When** | Nothing |
| SIMPL-31113/AC3 no Given | Real. Starts with **When** | Nothing |
| SIMPL-31114/AC1 no Given | Real. Starts with **When** | Nothing |
| SIMPL-31149 no criteria | Real. Only a user story and a note that the scope is implemented in SIMPL-31153 | Nothing |
| SIMPL-31151 no criteria | Real. Only a user story and a note that it is covered by SIMPL-31150 | Nothing |
| SIMPL-31152 no criteria | Real. Same as SIMPL-31151 | Nothing |
| `query-mapper-adapter-be:GET /participants/resourceDescriptions` no operationId | Real. No `operationId` in `tier2-gateway/t2-adapter-openapi.yaml` | Nothing |
| `query-mapper-adapter-be:GET /participants/resourceDescriptions/{resourceDescriptionId}` no operationId | Real, same file | Nothing |
| `query-mapper-adapter-be:PATCH /participants/resourceDescriptions/{resourceDescriptionId}` no operationId | Real, same file | Nothing |
| `query-mapper-adapter-be:GET /participants/resourceDescriptions/{resourceDescriptionId}/version` no operationId | Real, same file | Nothing |
| bp610 no detailed requirements | Real. The file says "None published at the time of capture" (6.10, DOME, status Proposed) | Nothing. Optionally read "None published" as an explicit empty list |
| bp68 no detailed requirements | Real, same wording (6.8) | Same |
| bp69 no detailed requirements | Real, same wording (6.9) | Same |
| SIMPL-31142 names `POST /selfDescriptions`, OpenAPI has the path with another method | Real, but the message is partly off. The story names `catalogue-client-be GET /selfDescriptions` and `catalogue-client-be POST /selfDescriptions`. The catalogue-client-be entry file (v2) has neither. The v1 file has `GET /selfDescriptions` and `POST /selfDescriptions/advanced`. The script matched the path against `query-mapper-adapter-be` and ignored the API name in the text, and it did not flag the GET | Resolve a named API prefix ("catalogue-client-be GET ...") against that API only. Report both methods as "not in the entry file", and say when a non-entry file of the same API has the path |

### Misreads the script did not report

| Id | What the script did | What it should do |
|---|---|---|
| SIMPL-27998/AC2 | The line "Or at least one dimension score in the QAR is lower than its dimension threshold ..." went to `other_text`, so the When clause lost its second branch | Treat an unbolded line starting with "Or" (or "And") inside a criterion as a continuation clause with keyword `Or` |
| SIMPL-31150/AC2 | Same: "Or the request is received without the identity attributes of the Consumer ..." went to `other_text`; the Given lost its second branch | Same rule |
| SIMPL-29292/AC3, SIMPL-29293/AC3 | The note sentence "then selecting "Sufficient" as the minimum returns ..." became a second **Then** clause | Only take bold keywords (`**Then**`) as clauses; a lowercase "then" at line start inside a note is text. Or end the criterion at "*Note ...*" |
| SIMPL-31153/AC3 | The line "**When** xfsc-advsearch-be sends a federated resource-detail request, the adapter ... must:" from the "Technical description" section became a fifth clause | End a criterion at the next `#` heading ("# Resource-detail flow", "# Technical description") |
| SIMPL-31817/AC1, AC2, AC3 | The clauses written as list items with code keywords (`` * `And` no resourceType specified``, `` * `And` the schema is valid``, `` * `And` a resourceType for instance "DATA" is specified``) went to `other_text` | Accept `` `And` `` and `` * `And` `` as the And keyword |
| SIMPL-31815, SIMPL-31817 | "negative case:" sits in `other_text` of AC1, not as the group label of AC2 and AC3 | Treat a line ending in ":" before a Given as a group label, as for "Negative Scenarios:" |
| SIMPL-31153/AC2 | Partial strike in a clause: "... resource description, -along with the associated schema-" lost its closing dash; the Then keeps "-and associated schema-" as plain words. Neither is marked struck | Detect `-text-` inside a clause and store the struck part separately, or mark the clause partly struck |
| SIMPL-31143, SIMPL-31148, SIMPL-31769 | `user_story` is empty. The stories use a table form: `||As a|Consumer|`, `||I want|...|`, `||So that|...|` | Parse the `||As a|...|` table rows as user story parts |
| SIMPL-31767 | `user_story` has only "so that". The source reads "As a Consumer\ I* *want to consult ..." without bold | Accept unbolded "As a" and "I want", with stray `*` removed |
| SIMPL-31143/AC1, SIMPL-31769/AC1 | The scenario title row `||Scenario|Consultation of the details ...|` is not used as heading | Take a `||Scenario|...|` row as the criterion heading |
| SIMPL-31817 named operations | `refs.operations` lists `catalogue-be:GET /schemas`, `catalogue-be:POST /schemas`, `catalogue-client-be:GET /schemas`. The text names `POST /schemas/<schemaName>/versions`; the script cut the path at `<` and then matched all methods | Treat `<name>` as a path parameter like `{name}`; match method and full path; report "named but not in any entry file" when there is no match (no `/schemas/{schemaName}/versions` operation exists in the entry files) |
| Path refs from URLs | `refs.paths` holds URL fragments: `/blob/main/openapi/authenticationprovider-tier1-v2` (SIMPL-30848), `/blob/v2` (SIMPL-31138, SIMPL-31150), `/raw/main/documents/user-manual/imgs/resource-description-details` (SIMPL-31148) | Skip paths inside URLs and links |

Smaller points, no effect on ids: BP06 requirement texts start with ": " (left over from the bold title). The trailing text of the last criterion (technical details, mockup) goes to that criterion's `other_text`, for example SIMPL-29292/AC7. In SIMPL-33885/AC1 the "Expected Response:" bullets (HTTP 200 OK, schema returned) sit in `other_text`; in the source they follow the Then as bullets, not as clauses.

One fact the script does not check: the operationId `advancedSearchSelfDescriptions` is used twice in query-mapper-adapter-be, for `POST /governanceAuthority/selfDescriptions/advancedSearch` (tier 1) and `POST /selfDescriptions/advancedSearch` (tier 2).

## Spot check

Ten stories, drawn with Python `random.seed(12)`. Each criterion compared clause by clause with the source.

| Story | Result |
|---|---|
| SIMPL-27998 | AC1, AC3 OK. AC2: the "Or at least one dimension score ..." line is missing from the When (in `other_text`) |
| SIMPL-30850 | Words OK. The first heading "Positive Scenario: Successfully creating a new version of a resource schema" holds two Given/When/Then blocks; the script split them into AC1 and AC2, and AC2 has no heading. A split is fair; AC2 could inherit the heading |
| SIMPL-31114 | OK |
| SIMPL-31138 | OK |
| SIMPL-31142 | OK |
| SIMPL-31143 | AC1 clauses OK. User story missing (table form); scenario title row not used as heading |
| SIMPL-31151 | OK (no criteria in the source) |
| SIMPL-31770 | OK. AC2 and AC3 do not exist in the source, so the ids jump from AC1 to AC4. The stray " -" after "returned result" in AC1 is dropped |
| SIMPL-31817 | Clauses missing: AC1 lacks "And no resourceType specified" and "And the schema is valid"; AC2 and AC3 lack "And a resourceType for instance "DATA" is specified" |
| SIMPL-35874 | OK |

Result: 6 of 10 OK, 4 with a difference. The full read of all 33 stories found the further misreads in the table above.

## Coverage from the links

77 live criteria, 39 operations, 48 requirements.

**Criteria with no operation and no requirement (15):** SIMPL-30850/AC1, AC2, AC3, AC4, AC5; SIMPL-31102/AC2; SIMPL-31103/AC2; SIMPL-31138/AC2; SIMPL-31139/AC2; SIMPL-31816/AC1, AC2; SIMPL-31817/AC1, AC2, AC3; SIMPL-35874/AC2.

For reference: 28 criteria have no operation, 37 have no requirement (lists can be derived from `links.json`). Schema versioning (`/schemas/{schemaName}/versions`, SIMPL-30850, SIMPL-31817) has no operation in the entry files. The Trustor-side search operation (SIMPL-31106, SIMPL-31138, SIMPL-31139) is described in the adapter as "the separate SIMPL-31880 API" and is not in the entry files.

**Operations with no criterion (25):**

- catalogue-be: `GET /schemas`, `GET /schemas/{schemaId}`, `PUT /schemas/{schemaId}`, `DELETE /schemas/{schemaId}`, `GET /schemas/latest`, `POST /schemas/{schemaName}/recalcQualityScore`, `GET /verification`, `POST /verification`, `GET /session`, `DELETE /session`, `POST /webhooks/schema-events`, `GET /self-descriptions`, `POST /self-descriptions`, `GET /self-descriptions/{selfDescriptionId}`, `GET /self-descriptions/{selfDescriptionId}/versions`, `DELETE /self-descriptions/{self_description_hash}`, `POST /self-descriptions/{selfDescriptionId}/revoke`, `GET /query`, `POST /query`
- catalogue-client-be: `GET /schemas/{schemaId}/content`, `GET /schemas/{schemaId}/{version}`
- query-mapper-adapter-be: `GET /participants/resourceDescriptions`, `GET /participants/resourceDescriptions/{resourceDescriptionId}`, `PATCH /participants/resourceDescriptions/{resourceDescriptionId}`, `GET /participants/resourceDescriptions/{resourceDescriptionId}/version`

**Requirements with no criterion (28):** BP5B.1.1, BP5B.1.2, BP5B.1.3, BP5B.2.1 to BP5B.2.6, BP5B.3.1 to BP5B.3.5, BP5B.4.1, BP5B.5.1 to BP5B.5.8, BP5B.5.10, BP5B.6.1, BP5B.7.1, BP5B.8.1, BP5B.9.1. Every BP06 requirement has at least one criterion. Processes 6.8, 6.9 and 6.10 have no requirements, so they do not appear.

## Vocabulary

### Actors and roles

| Term | Where |
|---|---|
| Governance Authority (GA) | SIMPL-27998, SIMPL-31106, SIMPL-31111, SIMPL-31112, SIMPL-31114, SIMPL-31138 to SIMPL-31140, SIMPL-31149 to SIMPL-31153, SIMPL-31815 to SIMPL-31817; BP5B.5.x, BP5B.8.1, BP6.2.1, BP6.3.x, BP6.6.1, BP6.7.6 to BP6.7.8 |
| Trusted Governance Authority (Trusted GA) | SIMPL-30848, SIMPL-31106, SIMPL-31113, SIMPL-31138, SIMPL-31139, SIMPL-31150, SIMPL-31153; BP6.3.x, BP6.4.1, BP6.5.1, BP6.7.5 to BP6.7.7; `query-mapper-adapter-be:GET /governanceAuthority/selfDescriptions`, `GET /federated/resourceDescriptions/{resourceDescriptionId}` |
| Trustor Governance Authority (Trustor GA, "trustor authorities") | SIMPL-30848, SIMPL-31102, SIMPL-31103, SIMPL-31113, SIMPL-31138 to SIMPL-31140, SIMPL-31142, SIMPL-31150, SIMPL-31153; BP6.4.1, BP6.5.1, BP6.7.5, BP6.7.6, BP6.7.8 |
| Governance Authority user | SIMPL-30850, SIMPL-31815 to SIMPL-31817 |
| Consumer | most search stories (SIMPL-31102 to SIMPL-31153, SIMPL-31767 to SIMPL-31770, SIMPL-33885, SIMPL-35874); BP6.x |
| Customer | SIMPL-31114/AC1 ("the Customer's public identity attributes") |
| Participant | SIMPL-29292, SIMPL-29293 (user story) |
| Provider | SIMPL-29292, SIMPL-29293 (title only); BP5B.x |
| "someone who is not a Trusted GA" | SIMPL-31138/AC2, SIMPL-31139/AC2 |
| Roles: `GA_SCHEMA_ADMIN`, `GA_SCHEMA_VIEWER` | SIMPL-31815, SIMPL-31817; `catalogue-be:GET /schemas`, `GET /schemas/{schemaId}`, `GET /schemas/latest` |
| Role: `governance-admin` | SIMPL-30850/AC2 |
| "role authorising schema management", "role authorising searching", "role authorising federated search configuration" | SIMPL-30850, SIMPL-31816; SIMPL-31102, SIMPL-31103; SIMPL-30848 |
| ABAC roles: `CATALOGUE_SEARCHER`, `DATA_PROVIDER_PUBLISHER`, `APP_PROVIDER_PUBLISHER`, `INFRA_PROVIDER_PUBLISHER` | catalogue-be tier 2 operations (`GET /selfDescriptions/quickSearch`, `POST /self-descriptions/resource`, ...) |
| Schema Manager (as caller) | `catalogue-be:POST /webhooks/schema-events`; SIMPL-31150 (struck lines) |

### Statuses

| Status | Of what | Where |
|---|---|---|
| valid / submitted | resource description | SIMPL-27998/AC1 to AC3 |
| published / not published | resource description | SIMPL-27998/AC1 to AC3, SIMPL-31143/AC1, SIMPL-31769/AC1, SIMPL-31770/AC1; BP5B.5.9 |
| stored / not stored | resource description | SIMPL-27998/AC2 |
| active | resource description | `catalogue-be:GET /selfDescriptions/quickSearch`, `POST /selfDescriptions/advancedSearch` |
| revoked | resource description | `catalogue-be:POST /self-descriptions/{selfDescriptionId}/revoke`, `query-mapper-adapter-be:PATCH /participants/resourceDescriptions/{resourceDescriptionId}`; BP5B.5.10, BP5B.8.1 |
| published / revoked | schema | SIMPL-30850/AC5, SIMPL-31815/AC3, SIMPL-31817/AC3; `catalogue-be:POST /webhooks/schema-events` |
| inactive | schema | BP5B.5.2 |
| active | Metadata Quality Profile | SIMPL-29292/AC3, SIMPL-29293/AC3; `catalogue-client-be:GET /schemas/{schemaName}/qualityProfiles/active` |
| trusted / no longer trusted, federation "still enabled" | Governance Authority relationship | SIMPL-31113/AC2, SIMPL-31138/AC2, SIMPL-31150/AC1 to AC3 |
| authenticated / unauthenticated | Governance Authority | SIMPL-31150/AC1, AC2 |

### Main data objects

| Object | Where |
|---|---|
| resource description / Resource Description / self-description / selfDescription / resourceDescription | everywhere; paths `/self-descriptions`, `/selfDescriptions`, `/resourceDescriptions` |
| Quality Assessment Record (QAR) | SIMPL-27998; `catalogue-be:GET` and `POST /self-descriptions/{selfDescriptionId}/qualityAssessmentRecord` |
| metadata quality definition / Metadata Quality Profile (MQP) | SIMPL-27998/AC1 to AC3, SIMPL-29292/AC3, SIMPL-29293/AC3; `catalogue-client-be:GET /schemas/{schemaName}/qualityProfiles/active` |
| overall score, dimension score, overallScore, minimum score, minimumScore, metadata quality score | SIMPL-27998, SIMPL-29292, SIMPL-29293, SIMPL-31140/AC4, SIMPL-31143, SIMPL-31769; quick and advanced search operations |
| classification, quality classification, overallCategory, minimumClassification | SIMPL-29292/AC3, SIMPL-29293/AC3, SIMPL-31143, SIMPL-31769; quick and advanced search operations |
| schema, schemaType (RESOURCE, SEARCH), resourceType (App, Infra, Data) | SIMPL-30850, SIMPL-31815 to SIMPL-31817, SIMPL-35874; `catalogue-client-be:GET /schemas` (parameter `resourceType`) |
| federated search schema / search schema / schema of type SEARCH | SIMPL-30850, SIMPL-31815 to SIMPL-31817, SIMPL-35874 |
| catalogue, own catalogue, local catalogue, federated catalogue | SIMPL-31102 to SIMPL-31148, SIMPL-35874; BP6.x |
| quick search request, advanced search request, federated search request | SIMPL-29292, SIMPL-29293, SIMPL-31106 to SIMPL-31142, SIMPL-31767, SIMPL-31768; BP6.1.1 to BP6.3.2 |
| search terms / searchTerm / searchString | SIMPL-31111, SIMPL-31112, SIMPL-31138, SIMPL-31139; `catalogue-be:GET /selfDescriptions/quickSearch`, `query-mapper-adapter-be:GET /selfDescriptions` |
| identity attributes, public identity attributes, mapped attributes, User-Attributes | SIMPL-31106, SIMPL-31111 to SIMPL-31114, SIMPL-31138, SIMPL-31139, SIMPL-31150, SIMPL-31153; BP6.3.1, BP6.5.1, BP6.7.5, BP6.7.6; header `User-Attributes` in adapter and catalogue-be search operations |
| discovery access policies | SIMPL-31111, SIMPL-31112, SIMPL-31114, SIMPL-31138, SIMPL-31139, SIMPL-31150; BP6.2.1, BP6.3.2 |
| correlation information / Correlation-Id | SIMPL-31150/AC4; `query-mapper-adapter-be:GET /federated/resourceDescriptions/{resourceDescriptionId}` |
| catalogue identifier: dataspace/catalogue identifier, catalogueId, catalogueIds | SIMPL-33885/AC1 to AC3; `catalogue-client-be:GET /selfDescriptions/{selfDescriptionId}`, `query-mapper-adapter-be:GET /governanceAuthority/selfDescriptions` |
| endpoint configuration (of a Trustor GA) | SIMPL-30848/AC1 |
| search result group, paging information | SIMPL-31140/AC1 to AC3, SIMPL-31142/AC1 to AC3 |

### Words that may mean the same

| Pair | Where |
|---|---|
| resource description / self-description | stories use "resource description"; catalogue-be paths and summaries use "Self-Description"; adapter uses both (`/selfDescriptions`, `/resourceDescriptions`) |
| metadata quality definition / Metadata Quality Profile (MQP) | SIMPL-27998/AC1 ("metadata quality definition"), SIMPL-27998/AC2 ("metadata quality definition (MQP)"), SIMPL-29292/AC3 ("Metadata Quality Profile (MQP)") |
| Metadata Quality Profile / Minimum Quality Profile | SIMPL-29292/AC3 vs catalogue-client-be v1 `GET /selfDescriptions` description (not an entry file) |
| overallCategory / classification / quality classification | SIMPL-29292/AC3, SIMPL-29293/AC3 vs SIMPL-31143/AC1, BP6.7.3 |
| overallScore / overall score / metadata quality score | SIMPL-29292/AC2 vs SIMPL-27998/AC1 vs SIMPL-31140/AC4 |
| federated search schema / search schema / schema of type SEARCH | SIMPL-31815/AC3 ("published search schema" and "federated search schema"), SIMPL-35874/AC2 |
| resource schema / resource definition schema / schemaType RESOURCE | SIMPL-30850/AC1 heading and Given, SIMPL-31816 technical details |
| Trusted GA / Trusted Governance Authority; Trustor GA / Trustor Governance Authority / trustor authorities | SIMPL-31138, SIMPL-31139, SIMPL-31102/AC1 |
| Consumer / Customer | SIMPL-31114/AC1 ("Customer's") vs all other stories |
| Consumer / Participant / Provider (for the searching user) | SIMPL-29292 and SIMPL-29293: title says Provider, user story says Participant; similar search stories say Consumer |
| `governance-admin` / `GA_SCHEMA_ADMIN` / "role authorising schema management" | SIMPL-30850/AC2, SIMPL-31815/AC1, SIMPL-30850/AC1 |
| own catalogue / local catalogue / catalogues of my own data space | SIMPL-31143, SIMPL-35874/AC1, SIMPL-31767/AC1 |
| federated catalogue / catalogue of a federated data space / Trustor catalogue | SIMPL-31148, SIMPL-31768, `query-mapper-adapter-be:GET /governanceAuthority/selfDescriptions` |
| data space / dataspace | SIMPL-31140 vs SIMPL-31148, SIMPL-33885 |
| search terms / searchTerm / searchString / keyword | SIMPL-31111 vs `catalogue-be:GET /selfDescriptions/quickSearch` vs `query-mapper-adapter-be:GET /selfDescriptions` |
| dataspace/catalogue identifier / catalogueId / catalogueIds | SIMPL-33885 vs `catalogue-client-be:GET /selfDescriptions/{selfDescriptionId}` vs `query-mapper-adapter-be:GET /governanceAuthority/selfDescriptions` |
| identity attributes / User-Attributes / "Consumer's attributes" | SIMPL-31113, SIMPL-31111/AC1, adapter header `User-Attributes` |
| discovery access policies / access policy values / discovery ABAC | SIMPL-31111/AC1 vs `query-mapper-adapter-be:GET /selfDescriptions` vs `GET /federated/resourceDescriptions/{resourceDescriptionId}` |
| published / active (resource description) | SIMPL-27998/AC1 vs `catalogue-be:GET /selfDescriptions/quickSearch` |
| trusted / federation enabled | SIMPL-31113/AC2 vs SIMPL-31150/AC1 |
| xfsc-adv-search / xfsc-advsearch-be / xsfc-adv-search-be / catalogue-client-be | SIMPL-33885 title, SIMPL-31770, SIMPL-31148, `specs/README.md` |
| query-mapper / query-mapper-adapter / catalogue-query-mapper-adapter / QueryHandler | SIMPL-31111, SIMPL-33885, SIMPL-31150, SIMPL-31143 |
| `409 Conflict` / `409 Config` | SIMPL-31815/AC3 vs SIMPL-31817/AC3 |

## Links

`links.json` holds 134 links: 6 named, 128 inferred.

- Named: SIMPL-31770/AC1, AC4, AC5 to `catalogue-client-be:GET /selfDescriptions/{selfDescriptionId}` (the story names "GET /selfDescriptions/{id}"); SIMPL-31815/AC1 to AC3 to `catalogue-be:POST /schemas` (the criteria name "POST /schemas").
- The script's named operations for SIMPL-31817 are not used (see the misreads above). SIMPL-30850 and SIMPL-31817 name `/schemas/{schemaName}/versions`, which no entry file has, so they get no operation link.
- No story names a BP requirement id, and no BP requirement names a story key in this set, so all requirement links are inferred.
