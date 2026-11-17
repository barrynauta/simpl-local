# API review (step 21b): Vocabulary Manager

Input: `specs/api/vocabulary_openapi-tier1.yaml` (Vocabulary Management API 1.0.0, 19 operations), `validation/api-checks.json` (21a), `ac-findings.json` (20b), `domain-findings.json` (23b), `decisions/decisions.json` (step 30), `inventory/inventory.json`, `inventory/links.json`, `guidelines/rules.json`. Findings are in [`api-findings.json`](api-findings.json). Rule `BUILD` marks a builder point that no single guideline rule covers.

The API is in good shape on the basics: problem+json on every error except health, realistic examples on most bodies, enums on status and sort, paging links on every list. Most open points are about the file catching up with the step 30 decisions.

## Summary

12 findings: 1 blocker, 3 major, 8 minor.

| Rule | Blocker | Major | Minor | Findings |
|---|---|---|---|---|
| COV-01 | 1 | 1 | 0 | P-001, P-004 |
| BUILD | 0 | 1 | 2 | P-002, P-006, P-012 |
| SEC-07 | 0 | 1 | 0 | P-003 |
| API-45 | 0 | 0 | 1 | P-005 |
| API-06 (with API-10, API-28) | 0 | 0 | 1 | P-007 |
| API-52 (with API-50, API-54) | 0 | 0 | 1 | P-008 |
| API-21 | 0 | 0 | 1 | P-009 |
| API-49 | 0 | 0 | 1 | P-010 |
| API-11 | 0 | 0 | 1 | P-011 |

The five findings with most impact:

1. **P-001 (blocker).** The two search operations have no story and no decision; paging, request limits, order and the oneOf edge case are open.
2. **P-002 (major).** The OpenAPI file does not yet carry the step 30 decisions: 401 and 403 (D17), the relaxed name filter (D19), pageSize maximum (D22), createdBy on versions (D27), validationWarnings required with a closed code set (D10), field lengths (D37), the forbidden version field (D7), latestVersionHref always filled (D5), unknown query parameters (D21).
3. **P-003 (major).** The security scheme declares read and write scopes on a placeholder OAuth2 server; D16 decides on two Keycloak roles and says scopes are not checked.
4. **P-004 (major).** Six external vocabulary operations and the manual version status change have no acceptance criteria apart from the blanket access rules of SIMPL-18196.
5. **P-005 (minor).** External vocabulary operations reuse internal error bodies: 404 names `vocabularyName`, 400 shows a semantic error that D30 rules out.

## Blockers

**P-001. Search operations: paging, limits, order.** `POST /vocabularies/searches` and `POST /external-vocabulary/searches` return CollectionPage fields (page, pageSize, total, totalPages) but take no page parameters, and their request arrays have no maxItems. Only SIMPL-18196 links to them, and D16 and D22 do not cover them.

Question: are the search results paged, how many references may one request hold, in which order are results returned, and is a body that fits two oneOf branches (for example `namespaceUri` plus `namespaceUris`) rejected?

Options:
- No paging: all matches in one response (page 1, totalPages 1); at most 100 references per request, 400 above; request order, duplicates removed; two branches return 400.
- Paging as on the GET lists: page and pageSize on the POST, default 20, maximum 100; ordered by name then version; duplicates removed; two branches return 400.

## Review rules: vocabulary-manager-be

The rules with check `review` that apply to `api-spec`: 19 `API-` rules and SEC-07. No `OWASP-` rule applies to `api-spec`.

| Rule | Level | Verdict | Evidence | Finding |
|---|---|---|---|---|
| API-01 | MUST | met | `sources.yaml` names the CDM `vocabulary-manager-be.md` for this API; 23b reviewed it (its approval is open, D-009, decided in D2). | |
| API-02 | SHOULD | partly met | Schemas redefine the CDM classes under other names (VocabularyCategory, VocabularyMetadata); raised in D-001 and D-002, decided in D25 and D26. | refer D-001, D-002 |
| API-03 | SHOULD | met | The file is committed source under `openapi/` (sources.yaml), with curated descriptions and examples; the build itself is outside this step's input. | |
| API-10 | SHOULD | partly met | `/external-vocabulary/searches` is singular next to `/external-vocabularies`. | P-007 |
| API-11 | SHOULD | partly met | The two search POSTs are read-only with a stated reason (batch body). `POST /external-vocabularies/content` is a read by POST, marked legacy. | P-011 |
| API-15 | SHOULD | met | Property names use American spelling (normalized, authorization). | |
| API-19 | SHOULD | met | Names carry business meaning: contentHref, latestVersionHref, namespaceUri, validationWarnings. | |
| API-22 | SHOULD | met | Examples are realistic: HealthcareVocabulary, Dublin Core terms, real Turtle snippets. | |
| API-28 | SHOULD | partly met | Searches and the namespace content lookup are controllers at the same level as `{vocabularyName}` and `{name}`. | P-007 |
| API-30 | SHOULD | met | Details at `/vocabularies/{vocabularyName}`, `/vocabularies/{vocabularyName}/versions/{version}`, `/external-vocabularies/{name}`. | |
| API-49 | SHOULD | partly met | info.description names internal vocabularies only. | P-010 |
| API-50 | SHOULD | not met | The identifier property is `name`, not `id` or `code`; D30 makes the name the identity, so this is a deliberate natural key. | P-008 |
| API-51 | SHOULD | met | References are named after the relation: contentHref, latestVersionHref. | |
| API-54 | SHOULD | not met | No identifier schema type; the name pattern is repeated in six places. | P-008 |
| API-55 | SHOULD | met | namespaceUri and aliasUris keep their standard names. | |
| API-58 | SHOULD | partly met | status and sort use enum; VocabularyValidationWarning.code is a closed set (D10) without enum. | P-002 |
| API-60 | SHOULD | met | All path parameters and request bodies are marked required; required body fields are listed. | |
| API-62 | MUST | not applicable | No story, decision or input file names Kafka events for this component. | |
| API-79 | SHOULD | not applicable | No topics. | |
| SEC-07 | SHOULD | partly met | An oauth2 scheme is declared, but with placeholder URLs and scopes, not OpenID Connect through Keycloak. | P-003 |

## Coverage

21a reported no COV-01 and no COV-02 hit. Reading `links.json` per operation shows why: SIMPL-18196/AC1 to AC4 ("invoke any Vocabulary Management API endpoint") are linked by inference to every secured operation, so each operation counts as covered. The table classes each operation whose only links come from SIMPL-18196.

| Operation | Links outside SIMPL-18196 | Class | Where handled |
|---|---|---|---|
| GET /health | 0 (left out by 21a) | internal plumbing | F-052, D17 |
| POST /vocabularies/searches | 0 | internal plumbing (reference resolution for Schema Manager), contract open | P-001 |
| POST /external-vocabulary/searches | 0 | internal plumbing, contract open | P-001 |
| POST /external-vocabularies/content | 0 | internal plumbing marked legacy; retire candidate | P-011 |
| PATCH /vocabularies/{vocabularyName}/versions/{version} | 0 (SIMPL-18166 names the path, links go to the GET) | story missing; behaviour decided | F-015, D4, P-004 |
| GET /external-vocabularies | 0 | story missing; behaviour decided | D-020, D30, P-004 |
| POST /external-vocabularies | 0 | story missing; behaviour decided | D-020, D30, P-004 |
| GET /external-vocabularies/{name} | 0 | story missing; behaviour decided | D-020, D30, P-004 |
| GET /external-vocabularies/{name}/content | 0 | story missing; behaviour decided | D-020, D30, P-004 |
| DELETE /external-vocabularies/{name} | 0 | story missing; behaviour decided | D-020, D30, P-004 |

The other 9 operations each have links from their own stories.

COV-02 (a story names an operation the API lacks):

| Story | Named | Class | Where handled |
|---|---|---|---|
| SIMPL-28680 | POST `/metadataQualityProfiles/{metadataQualityProfileName}/versions` | out of scope: Schema Manager | F-010, D1 |
| SIMPL-18166 | `/vocabularies/{vocabularyName}/versions/{version}` | present in the API | none needed |

21a did not flag SIMPL-28680 because the inventory holds its path without a method (`refs.paths`, empty `method_paths`), although AC1 names the POST. Already decided, so no new finding.

## 21a triage

5 findings in 18 places: 3 real (16 places), 2 harmless (2 places).

| Id | Rule | Places | Verdict | Reason or finding |
|---|---|---|---|---|
| A-001 | API-63 | 1 | harmless | No input names Kafka or any event for this component, so no AsyncAPI file is due (API-62 not applicable). |
| A-002 | API-06 | 5 | real | P-007 |
| A-003 | API-21 | 6 | real | P-009 |
| A-004 | API-45 | 1 | harmless | GET /health 503 returns the same HealthResponse document as 200 so that probes can read the status; it is a status report, not an error of the request. |
| A-005 | API-52 | 5 | real | P-008 |
