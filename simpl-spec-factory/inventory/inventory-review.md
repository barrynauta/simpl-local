# Spec inventory review: Vocabulary Manager (step 12b)

Review of `inventory.json` and `inventory.md` against `specs/`. This step extracts and links. It does not judge the quality of the specs.

## Note on process ids

The business process is one file, `specs/functional/bp02b-manage-vocabularies.md`. The script does not split single-file processes, so `inventory.json` has no `bp_requirements`. The links in `links.json` therefore use ids of the form `BP02B#<section heading>`, built from the six requirement headings under "Requirement descriptions" (for example `BP02B#2B.4 - The Governance Authority validates a new vocabulary or a new version of a vocabulary`). **These ids are not in `inventory.json`.** Each link reason starts with the detailed requirement number it matches (for example `2B.4.1`), so coverage can also be read at that level.

## Extraction issues

The six issues the script flagged:

| Issue | Parser error or real | What to change |
|---|---|---|
| SIMPL-393: no acceptance criteria found | Real | The story has an "AC:" block with two plain bullets ("The onthology is visible while executing endpoint to fetch all onthologies", "We are able to fetch details of stored onthology"). There is no Given/When/Then. Optional: record non-Gherkin "AC:" bullets as free-text criteria, so they are not lost. |
| SIMPL-395: no acceptance criteria found | Real | The description says criteria will be added later. No criteria in the text. |
| SIMPL-18162/AC2: no When clause | Real | The source uses "And I request the metadata ..." after the Given, not "When". Nothing to change in the script. |
| SIMPL-18162/AC3: no When clause | Real | Same pattern as AC2. |
| SIMPL-18162/AC4: no When clause | Real | Same pattern as AC2. |
| bp02b-manage-vocabularies.md: single-file process, not parsed | Parser limitation | The file does hold numbered detailed requirements: 14 bullets "2B.1.1" to "2B.6.1" under "Detailed requirements:", grouped by six "2B.n" headings. The script should parse single-file processes too and emit ids such as `BP2B.4.1`. |

Further parser observations, not flagged by the script:

| Where | Parser error or real | What to change |
|---|---|---|
| SIMPL-18183/AC2 | Parser error | The Note bullet "- when doing semantic validation don't fail on first issue ..." became a `When` clause at the end of AC2. Match keywords only in their bold form (`**When**`), not a plain "when" at the start of a bullet. |
| SIMPL-18162/AC1 to AC4 | Parser error | The scenario titles are table rows (`\|Scenario\|<title>\|`). The first title is lost, and each later title sits in `other_text` of the previous criterion. Read a `\|Scenario\|...\|` row as the heading of the next criterion. |
| SIMPL-18173/AC1 | Parser error (minor) | The bold line "Happy Path" is not kept as the heading of AC1, while "Negative Scenarios" is kept as a group. Treat a bold line before a Given as a heading. |
| SIMPL-18169/AC1, AC2; SIMPL-18167/AC1; SIMPL-18180/AC4 to AC7 | Parser error (split) | Bullet lists that complete a clause ("the following required fields are available:", "Then the system:") go to `other_text`, apart from the clause. The words are kept. Attach such bullets to the clause that ends with a colon. |
| SIMPL-18166 | Parser error (over-match) | The note names the path `/vocabularies/{vocabularyName}/versions/{version}` without a method. The script maps it to both GET and PATCH. The story is about retrieval. Keep path-only matches as path-level, or mark them as ambiguous. |
| SIMPL-28680 refs | Parser error | `refs.paths` holds two fragments of a GitLab URL (`/blob/.../t1-schema-openapi`, `/blob/develop/openapi/schema_openapi`). Do not read paths inside URLs. |
| SIMPL-28680/AC2 | Real | The source has two Given/When/Then blocks under the heading "AC1" and no heading "AC2". The script gave the second block the derived id AC2, which fills the gap. If the owner adds an "AC2" heading later, the ids stay the same; if the owner inserts a block, they shift. |

## Spot check

Ten stories picked at random (`random.seed(12)` over the 33 stories with criteria). Each criterion compared clause by clause with the source.

| Story | Result |
|---|---|
| SIMPL-18161 | OK. Two criteria, headings "Positive Scenario" and "Negative Scenario" kept, words unchanged. |
| SIMPL-18165 | OK. One criterion, five clauses, words unchanged. |
| SIMPL-18173 | OK on clauses and words for all six criteria. The heading "Happy Path" of AC1 is not kept (see above). |
| SIMPL-18174 | OK. Three criteria, words unchanged. |
| SIMPL-18180 | Clauses complete and words unchanged. In AC4 to AC7 the Then clause is only "the system:"; its three bullet points sit in `other_text`, split from the clause. |
| SIMPL-18181 | OK. Two criteria, all nine and eleven clauses present, words unchanged. |
| SIMPL-18186 | OK. One criterion, italic heading "AC1 - Automatic publication after successful validation" kept. |
| SIMPL-18194 | OK. Two criteria, words unchanged. |
| SIMPL-18452 | OK. Two criteria, words unchanged. |
| SIMPL-28680 | Words unchanged. AC2 is derived from a second block under the heading "AC1" (see above). Nothing missing or merged. |

No criterion was missing, merged or had changed words in the sample. The one split is the bullet lists in SIMPL-18180.

## Coverage from the links

91 live criteria, 19 operations, 6 process requirements (14 detailed requirements). Links go from criterion to operation or requirement; the reverse view is the same set.

**Criteria with no operation and no requirement (9):**
SIMPL-18195/AC1, AC2, AC3; SIMPL-28680/AC1, AC2, AC3, AC4, AC5, AC6.

**Criteria with a requirement but no operation (14):**
SIMPL-18178/AC1; SIMPL-18180/AC1, AC4, AC5, AC6, AC7, AC8; SIMPL-18182/AC1, AC2, AC3; SIMPL-18184/AC4; SIMPL-18189/AC3; SIMPL-18193/AC1, AC2.

**Criteria with an operation but no requirement (4):**
SIMPL-18196/AC1, AC2, AC3, AC4.

**Operations with no criterion (1):**
`vocabulary_openapi-tier1:GET /health`.

**Operations covered only by the blanket access-control criteria of SIMPL-18196 ("any Vocabulary Management API endpoint") (9):**
`POST /vocabularies/searches`, `PATCH /vocabularies/{vocabularyName}/versions/{version}`, `GET /external-vocabularies`, `POST /external-vocabularies`, `POST /external-vocabulary/searches`, `POST /external-vocabularies/content`, `GET /external-vocabularies/{name}/content`, `GET /external-vocabularies/{name}`, `DELETE /external-vocabularies/{name}`.

**Requirements with no criterion:** none. All six `BP02B#2B.n` headings and all 14 detailed requirements (2B.1.1 to 2B.6.1) have at least one criterion. The thinnest: 2B.5.3 (3 criteria), 2B.4.3 and 2B.5.1 (4 each).

## Vocabulary

### Actors and roles

| Term | Where |
|---|---|
| Governance Authority | most stories (SIMPL-18161 to SIMPL-18196, SIMPL-18452), BP02B Actors |
| Governance Authority user | SIMPL-18169, 18172, 18176, 18183, 18184, 18186, 18189, 18190, 18191, 18193, 18194, 18195, 18559 |
| governance authority agent | SIMPL-393, SIMPL-395 |
| Governance Authority End User | SIMPL-28680 |
| user with the Vocabulary Management role | SIMPL-18181 |
| vocabulary management role | SIMPL-18162, 18181, 18183, 18190, 18195, 18196 |
| vocabulary management permissions / rights | SIMPL-18165, 18169, 18172, 18180, 18189, 18191, 18452 |
| vocabulary management admin rights | SIMPL-18184 |
| role authorizing read access to the vocabulary history | SIMPL-18176/AC1 |
| "with any role" | SIMPL-18559 |
| `GA_VOCABULARY_VIEWER`, `GA_VOCABULARY_ADMIN`, Tier1 Role | SIMPL-18196 (notes, AC4) |
| OAuth2 scopes `read`, `write` | OpenAPI `OAuth2Auth` |
| authorized to manage metadata quality governance | SIMPL-28680 |
| Providers (propose changes by email) | BP02B Assumptions |
| Simpl-Open, the system, the API, Schema Manager (acting systems) | throughout; Schema Manager in SIMPL-28680 |

### Statuses

| Term | Where |
|---|---|
| `ACTIVE`, `DEPRECATED` (vocabulary status) | SIMPL-18161 (`Active`, `Deprecated`), 18163, 18170, 18174, 18175, 18178; OpenAPI `VocabularyStatus`, `PATCH /vocabularies/{vocabularyName}` |
| `ACTIVE`, `DEPRECATED` (version status) | SIMPL-18170, 18172, 18173, 18176, 18181, 18184, 18186, 18191, 18559; OpenAPI `PATCH /vocabularies/{vocabularyName}/versions/{version}` |
| published, deprecated, "Full Vocabulary Deprecation" | BP02B steps BP02B.03, BP02B.05; 2B.5, 2B.6 |
| latest (`latest: true`) | SIMPL-18170/AC1; OpenAPI `VocabularySearchItem` |
| validation passed / failed | SIMPL-18180, 18181, 18182, 18183, 18184, 18193, 18194 |
| health `UP`, `DEGRADED`, `DOWN` | OpenAPI `GET /health` |
| Proposed (requirement status) | BP02B high-level requirements note |

### Main data objects

| Term | Where |
|---|---|
| vocabulary | all stories, BP02B, OpenAPI |
| vocabulary version | SIMPL-18166, 18168, 18172, 18173, 18176, 18184, 18187, 18189 to 18191, 18452, 18559; OpenAPI `/versions` paths |
| vocabulary metadata (name, version, description, status, creationDate, lastUpdateDate, creator) | SIMPL-18161, 18162, 18170, 18176; OpenAPI `VocabularyMetadata`, `VocabularyCategory` |
| vocabulary content / file (Turtle, ttl, RDF Schema, OWL) | SIMPL-18169, 18170, 18172, 18180, 18452; SIMPL-395; BP02B.01; OpenAPI `vocabularyFile`, `/content` paths |
| version identifier / version number | SIMPL-18173, 18176, 18189, 18190; OpenAPI `version` |
| change log / changelog / change summary | SIMPL-18167, 18169, 18172, 18176; OpenAPI `changelog` |
| validation report / structured report / error response / validation feedback | SIMPL-18173, 18180, 18181, 18182, 18183, 18193, 18194; BP02B.06; OpenAPI `VocabularyValidationProblem`, `ProblemIssue` |
| validation warning | SIMPL-18183 (notes), 18184/AC4; OpenAPI `VocabularyValidationWarning`, `validationWarnings` |
| referenced vocabulary version | SIMPL-18184; OpenAPI `POST /vocabularies/searches` |
| external vocabulary / cached external vocabulary | SIMPL-18183, 18184 (notes); BP02B Overview and Assumptions; OpenAPI `/external-vocabularies` paths |
| Vocabulary Hub / vocabulary repository / vocabulary registry | BP02B Assumptions; SIMPL-395; SIMPL-18187 |
| ontology / onthology / schema / SHACL constraints | SIMPL-393, SIMPL-395, SIMPL-18163/AC1 |
| Metadata Quality Profile (MQP), profile version | SIMPL-28680 |

### Pairs that may mean the same

| Word A | Word B | Where |
|---|---|---|
| Governance Authority | Governance Authority user / governance authority agent / Governance Authority End User | see Actors |
| vocabulary management role | vocabulary management permissions / admin rights / `GA_VOCABULARY_ADMIN` | SIMPL-18162, 18165, 18184, 18196 |
| version identifier | version number | SIMPL-18189 vs SIMPL-18190; OpenAPI `version` |
| change log / changelog | change summary | SIMPL-18167, OpenAPI vs SIMPL-18172, 18176 |
| published | ACTIVE / "automatically become available" / "persisted and made available" | BP02B.03, 2B.5 vs SIMPL-18186, 18187 |
| vocabulary repository | vocabulary registry / Vocabulary Hub | SIMPL-18187/AC1 vs BP02B Assumptions |
| validation report | structured report / error response / validation feedback / notification | SIMPL-18193, 18194, 18181, 18173 vs BP02B.06, 2B.4.4 |
| vocabulary status | version status | SIMPL-18174, 18175 vs SIMPL-18191; two PATCH operations |
| deprecate a vocabulary | deprecate a vocabulary version | 2B.6 vs BP02B.05, 2B.5.3 |
| `lastUpdated` | `lastUpdateDate` / "last updated" | SIMPL-18164 and OpenAPI query vs SIMPL-18162 and OpenAPI response; SIMPL-18161 |
| `creationDate` | "date created" | SIMPL-18162, 18164 vs SIMPL-18161, 18167 |
| creator | `createdBy` | SIMPL-18161, 18176 vs OpenAPI |
| vocabulary management page / screen | vocabulary details page / history page | SIMPL-18161, 18165, 18172 vs SIMPL-18167, 18559 |
| structure validation | semantic validation | SIMPL-18180 vs SIMPL-18182, 18183; 2B.4.2 |
| authentication token / Bearer token | OAuth2 | SIMPL-18162, 18196 vs OpenAPI `OAuth2Auth` |
| ontology | vocabulary | SIMPL-393 vs the rest |

## Totals

| Item | Count |
|---|---|
| Links | 229 |
| Named | 2 (SIMPL-18166/AC1 and AC2 to `GET /vocabularies/{vocabularyName}/versions/{version}`, from the path in the story note) |
| Inferred | 227 (72 of them are the SIMPL-18196 "any endpoint" links, 4 criteria times 18 secured operations) |
| Criterion to operation | 145 |
| Criterion to `BP02B#` requirement | 84 |
