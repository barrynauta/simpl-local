# Inventory review (12b): schema-manager

Review of `inventory.json` (55 stories, 209 live criteria, 1 API with 18 operations, 28 extraction issues) against the files in `specs/`. Extraction and linking only; no judgement of the specs.

The business process is one file (`specs/functional/bp02c-manage-resource-description-schemas.md`). The script did not split it, so `inventory.json` has no `bp_requirements`. Links to the process use the id `BP02C#<section heading>`, one per process step heading (for example `BP02C#BP02C.04 Validate the schema`). **These BP02C ids are not in `inventory.json`.** The high-level requirements 2C.1 to 2C.8 are bold paragraphs inside the section "Requirement descriptions", not headings, so they have no id of their own here.

## Extraction issues

28 issues. 8 are parser errors, 20 are real properties of the spec (one of them also hides a parser merge).

| Issue | Parser error or real | What to change |
|---|---|---|
| SIMPL-14709/AC1: no Given clause | Parser error. The source has two numbered criteria, `1. Given ...` and `2. Given ...`, with a plain (not bold) Given. The script drops the first Given and its And, merges both criteria into one AC, and puts `2. Given ...` in other_text. | Accept a numbered list item `N. Given` (bold or not) as the start of a new criterion. Result: AC1 and AC2, both with Given. |
| SIMPL-14712/AC1: no Given clause | Parser error. Same pattern as SIMPL-14709: numbered criteria 1 and 2 merged into AC1, first Given lost. The negative scenario is parsed correctly as AC2. | Same fix. Result: three criteria instead of two. |
| SIMPL-14791/AC1: no Given clause | Parser error. Same pattern: numbered criteria 1 and 2 merged, first Given lost. | Same fix. Result: two criteria instead of one. |
| SIMPL-19992/AC4: no Given clause | Real. In the source the first three negative scenarios start with When. But the script also merges these three When/Then blocks into one AC4. | Keep the flag. Start a new criterion at a When that follows a Then or a Then's And. Result: three criteria (invalid URL, no event type, invalid event type). |
| SIMPL-32258/AC4: no Given clause | Parser error. The bold **When** is a sentence in the description above the user story, not a criterion. | Parse criteria only after the user story or after an "Acceptance Criteria" marker. Drop AC4. |
| SIMPL-32258/AC4: no Then clause | Parser error. Same cause as the row above. | Same fix. |
| SIMPL-33537/AC6: no Given clause | Parser error. The bold **When** sits inside a bullet of the "Implementation scope" section. The rest of that section ends up in other_text of AC6. | Parse criteria only after the "# Acceptance criteria" heading. Drop AC6. |
| SIMPL-33537/AC6: no Then clause | Parser error. Same cause. | Same fix. |
| schema_openapi:GET /schemas: no operationId | Real. The OpenAPI file has no operationId for this operation. | None. |
| schema_openapi:POST /schemas: no operationId | Real. | None. |
| schema_openapi:PATCH /schemas/{schemaName}: no operationId | Real. | None. |
| schema_openapi:GET /schemas/{schemaName}/versions: no operationId | Real. | None. |
| schema_openapi:POST /schemas/{schemaName}/versions: no operationId | Real. | None. |
| schema_openapi:GET /schemas/{schemaName}/{version}: no operationId | Real. | None. |
| schema_openapi:GET /vocabularies: no operationId | Real. | None. |
| schema_openapi:POST /vocabularies: no operationId | Real. | None. |
| schema_openapi:PATCH /vocabularies/{vocabularyName}: no operationId | Real. | None. |
| schema_openapi:GET /vocabularies/{vocabularyName}/versions: no operationId | Real. | None. |
| schema_openapi:POST /vocabularies/{vocabularyName}/versions: no operationId | Real. | None. |
| schema_openapi:GET /vocabularies/{vocabularyName}/versions/{version}: no operationId | Real. | None. |
| schema_openapi:PATCH /metadataQualityProfiles/{metadataQualityProfileName}: no operationId | Real. Only `deleteMetadataQualityProfile` and `getActiveQualityProfile` have one. | None. |
| schema_openapi:GET /webhooks: no operationId | Real. | None. |
| schema_openapi:POST /webhooks: no operationId | Real. | None. |
| schema_openapi:DELETE /webhooks/{webhookId}: no operationId | Real. | None. |
| bp02c file: single-file process, not parsed into requirements | Parser error (a limitation). The file has clear structure: seven step headings `### BP02C.01` to `### BP02C.07` and a table of high-level requirements 2C.1 to 2C.8 with detailed requirement ids (2C.1.1, 2C.2.1, ...). | Split a single-file process on `### BP02C.NN` headings, and read the "High-level requirements" table and "Requirement descriptions" items as requirements 2C.N and 2C.N.M. |
| SIMPL-20067: names GET /schemas/{schemaName}, but the OpenAPI has that path with another method | Real. The AC text names `GET /schemas/{schemaName}`; the OpenAPI has only PATCH on that path. | None. The check works. |
| SIMPL-31098: names GET /schemas/{schemaName}, but the OpenAPI has that path with another method | Real. Named in the "Validation hints" text, not in a clause. | None. |
| SIMPL-33537: names GET /schemas/{schemaName}, but the OpenAPI has that path with another method | Real. Named in the "Implementation scope" list. | None. |

Other extraction effects seen while reviewing, not in the issue list:

- **Concrete paths not checked.** SIMPL-13347 and SIMPL-32788 send GET to concrete paths such as `/schemas/ApplicationAsset`. These are instances of `GET /schemas/{schemaName}` too, but the script did not flag them because it only checks method plus template. Suggestion: match concrete paths against path templates.
- **Headings shift by one.** When a criterion's heading is a line such as `### 2. New schema version published` or `PATH: ...`, the script puts it in other_text of the previous criterion. Seen in SIMPL-33694, SIMPL-32802, SIMPL-32807, SIMPL-32800, SIMPL-32788, SIMPL-20529 and SIMPL-20530.
- **"OR" alternatives lost.** An untagged `- OR` or `Or ...` line between clauses goes to other_text. The two When clauses then read as consecutive steps. Seen in SIMPL-15837/AC4, AC5, SIMPL-32790/AC4, AC5, SIMPL-14703/AC3, AC4 and SIMPL-26105/AC3, AC4.
- **Bullet lines under a clause.** Bullets that complete a clause ("When I complete the form by: ...", "each error includes, where applicable: ...") go to other_text. In SIMPL-13355/AC2 the only expected outcome of the negative scenario (UI stays on the form and shows a message on 400 or 409) is such a bullet, so the parsed Then only says the UI calls `POST /schemas`.
- **Labels follow scenario numbers.** "Positive Scenario 1" becomes AC1 and "Negative Scenario 1" also becomes AC1 when the story has no numbered positive scenario. In SIMPL-13364 and SIMPL-32798 the unnumbered positive scenarios become AC3 and AC4 and come before AC1 and AC2.

## Spot check

10 stories drawn at random (seed 20261117). Clause by clause against the source.

| Story | Result |
|---|---|
| SIMPL-13345 | OK. 3 criteria, all clauses match. The dash separator line lands in other_text of AC2. |
| SIMPL-13361 | OK. 6 criteria, all clauses match. The `|Scenario||...|` table rows are not kept; they are not clauses. |
| SIMPL-14354 | OK. The Given keeps the Jira markup `{color:#172b4d}...{color}`, as in the source. |
| SIMPL-14701 | OK. The Given keeps the markup `[color:#172b4d]`, as in the source. |
| SIMPL-14703 | Difference. In AC3 and AC4 the source When continues with the untagged line "Or Simpl does not verify the presence of the default properties". The inventory puts this line in other_text, so the When clause lacks the second condition. |
| SIMPL-14796 | OK. 2 criteria, all clauses match. |
| SIMPL-15837 | Difference. In AC4 and AC5 the source has two When clauses joined by "- OR". The inventory keeps both When clauses in the criterion but moves "OR" to other_text. |
| SIMPL-17066 | OK. 12 criteria, all clauses match. The `---` separators are in other_text. |
| SIMPL-32801 | OK. 6 criteria with their scenario headings; all clauses match. |
| SIMPL-33694 | Difference in headings only. AC1 has no heading; the headings "2. New schema version published" to "5. SEARCH schema change" each sit in other_text of the criterion before. All clauses match. |

Result: 7 OK, 3 with a difference. No clause has changed words. No criterion is missing or split. The differences are a lost "Or" condition (SIMPL-14703), a lost "OR" between two When clauses (SIMPL-15837), and shifted headings (SIMPL-33694).

## Coverage from the links

From `links.json`: 342 links from 192 of the 209 live criteria.

**Criteria with no operation and no process step (17):**
SIMPL-13347/AC1, SIMPL-13347/AC2, SIMPL-17066/AC5, SIMPL-17066/AC11, SIMPL-20067/AC1, SIMPL-20067/AC2, SIMPL-28815/AC1, SIMPL-28815/AC2, SIMPL-28815/AC3, SIMPL-28815/AC4, SIMPL-28815/AC5, SIMPL-28815/AC6, SIMPL-32788/AC1, SIMPL-32788/AC2, SIMPL-32788/AC3, SIMPL-32788/AC4, SIMPL-33537/AC6.

Facts behind this list: SIMPL-13347, SIMPL-20067 and SIMPL-32788 retrieve the latest content with GET on `/schemas/{schemaName}`, which the OpenAPI does not have. SIMPL-17066/AC5 and AC11 say "list schema details", which matches no single operation. SIMPL-28815 is about a Provider adding a billing contract clause to a resource description. SIMPL-33537/AC6 is the parser artefact from the extraction issues.

Also: 37 criteria have no operation, and 79 have no process step. Most criteria without a process step are about retrieval (lists, versions, content, details) and webhooks. Retrieval is high-level requirement 2C.1, which has no process step heading.

**Operations with no criterion (9):**
- schema_openapi:GET /vocabularies
- schema_openapi:POST /vocabularies
- schema_openapi:PATCH /vocabularies/{vocabularyName}
- schema_openapi:GET /vocabularies/{vocabularyName}/versions
- schema_openapi:POST /vocabularies/{vocabularyName}/versions
- schema_openapi:GET /vocabularies/{vocabularyName}/versions/{version}
- schema_openapi:PATCH /metadataQualityProfiles/{metadataQualityProfileName}
- schema_openapi:DELETE /metadataQualityProfiles/{metadataQualityProfileName}
- schema_openapi:GET /schemas/{schemaName}/qualityProfiles/active

**Process step sections with no criterion:** none of the seven steps (BP02C.01 to BP02C.07). BP02C.06 has the fewest (9 links). The section "Trigger resource description schema management" was not linked; it has no step id. High-level requirement 2C.1 (retrieving schemas and versions) has no step section, so retrieval criteria could not be linked to the process.

Links per target: POST /schemas 46, POST /schemas/{schemaName}/versions 41, PATCH /schemas/{schemaName} 32, GET /schemas 21, GET /schemas/{schemaName}/versions 14, GET /schemas/{schemaName}/{version} 12, POST /webhooks 5, DELETE /webhooks/{webhookId} 2, GET /webhooks 1; BP02C.04 38, BP02C.01 35, BP02C.02 30, BP02C.03 21, BP02C.05 20, BP02C.07 15, BP02C.06 9.

## Vocabulary

### Actors and roles

| Term | Where |
|---|---|
| Governance Authority / Governance Authority user | most stories, for example SIMPL-13343, SIMPL-14703, SIMPL-17066, SIMPL-33537; BP02C Actors |
| Governance Authority administrator | SIMPL-13355 (user story), SIMPL-14795, SIMPL-13361 (storycard) |
| GA user / GA client | SIMPL-13347, SIMPL-32788 |
| API client | SIMPL-13343, SIMPL-13345, SIMPL-13353, SIMPL-13357, SIMPL-13360, SIMPL-20528, SIMPL-20534, SIMPL-32258, SIMPL-32786, SIMPL-32802, SIMPL-32807 |
| governance authority service (a subscribing service) | SIMPL-19992 (user story) |
| Provider / Provider end user | SIMPL-14709, SIMPL-14712, SIMPL-14791, SIMPL-33694, SIMPL-28815; BP02C.06, BP02C Assumptions |
| Participant (with Tier2 credentials) | SIMPL-24175 |
| End-User | BP02C Prerequisites |
| Role `GA_SCHEMA_ADMIN` | SIMPL-13343, SIMPL-13353, SIMPL-13357, SIMPL-13360, SIMPL-17066, SIMPL-20529, SIMPL-32800, SIMPL-32802, SIMPL-32807 and others |
| Role `GA_SCHEMA_VIEWER` | SIMPL-13343, SIMPL-13345, SIMPL-13365, SIMPL-15837, SIMPL-17066, SIMPL-20067, SIMPL-32258 |
| Role `governance-admin` / `governance-viewer` | SIMPL-13343/AC4, SIMPL-13358/AC1, SIMPL-32808/AC2 |
| "role authorising schema management" / "role authorising schema viewing" | SIMPL-14354, SIMPL-14703, SIMPL-22407, SIMPL-33537, SIMPL-33694 and others |
| System side: Simpl / Simpl-Open / the service / Schema Management Service (SMS) / the system | Simpl: SIMPL-13364, SIMPL-14703, SIMPL-17066; Simpl-Open: SIMPL-33537, SIMPL-33694, BP02C; SMS: SIMPL-13353, SIMPL-32802; the system: SIMPL-19992 |
| UI: governance management portal / schema management portal / schema management UI / UI of the Governance Authority / governance management UI | SIMPL-13346, SIMPL-13355, SIMPL-20067 / SIMPL-15837, SIMPL-32790 / SIMPL-32800 / SIMPL-13361, SIMPL-20530 / SIMPL-20770 |

### Statuses

| Object | Values | Where |
|---|---|---|
| Schema | PUBLISHED, REVOKED | SIMPL-13357, SIMPL-13360, SIMPL-20529, SIMPL-20530, SIMPL-20686, SIMPL-32801, SIMPL-31098; OpenAPI enums |
| Schema (invalid example) | ARCHIVED, INVALID_STATUS | SIMPL-13360/AC4; SIMPL-13343/AC5, SIMPL-32258/AC7 |
| Schema status change request | REVOKED only ("Only REVOKED is supported for this endpoint") | OpenAPI `SchemaStatusUpdateRequest` on PATCH /schemas/{schemaName} |
| Schema version | PUBLISHED, REVOKED | SIMPL-13357/AC1, SIMPL-32807/AC1 to AC3; a note in SIMPL-20530 says a version inherits the status of the schema concept |
| Published state written as PUBLISH | PUBLISH | SIMPL-20530/AC2 to AC4 |
| Vocabulary | ACTIVE, DEPRECATED | OpenAPI only |
| Metadata Quality Profile | PUBLISHED, REVOKED | OpenAPI only |
| Webhook event types | SchemaPublished, SchemaRevoked (invalid example: SchemaExploded) | SIMPL-19992; OpenAPI `Webhook` |
| Webhook subscription | "active" | SIMPL-19992/AC2 |
| BP02C high-level requirements | Proposed | BP02C, status note |

### Main data objects

| Object | Where |
|---|---|
| Schema / schema concept | SIMPL-13347, SIMPL-13353, SIMPL-15837, SIMPL-32790, SIMPL-32802 (concept); everywhere (schema) |
| Schema version, version number, changelog / "Latest Change" | SIMPL-13345, SIMPL-13346, SIMPL-13357, SIMPL-20528, SIMPL-20534, SIMPL-20770, SIMPL-32807 |
| Schema metadata: name / schemaName, title, description, resourceType, schemaType, status, creation date, creator, latest version | SIMPL-13353, SIMPL-15837, SIMPL-32790, SIMPL-32800, SIMPL-32802 |
| schemaType: RESOURCE, SEARCH | SIMPL-31098, SIMPL-32258, SIMPL-32786 to SIMPL-32808, SIMPL-33537, SIMPL-33694 |
| resourceType values | `data`, `infrastructure`, `application` (SIMPL-13355, SIMPL-32800); `data-schema` (SIMPL-13343/AC2); APP, INFRA, DATA (SIMPL-32802 technical info); `Application` PascalCase (OpenAPI) |
| SHACL definition / `.ttl` file / schema definition file / raw SHACL content | SIMPL-13347, SIMPL-13353, SIMPL-13357, SIMPL-13358, SIMPL-32788, SIMPL-32802, SIMPL-32807 |
| Default properties, mandatory properties, UUID and Version default properties | SIMPL-14703, SIMPL-14796, SIMPL-24173, SIMPL-26105, SIMPL-33537; BP02C.04 |
| Validation rules, relationships, dependencies; referenced vocabularies | SIMPL-14797, SIMPL-22407, SIMPL-24174; BP02C.04 |
| Error outputs: error message / problem details JSON object / structured error response / report | SIMPL-14795, SIMPL-14796 / SIMPL-13343, SIMPL-13357, SIMPL-20529 / SIMPL-22407 / BP02C.07 |
| Location and Content-Location headers, named graph | SIMPL-13347, SIMPL-13353, SIMPL-13357, SIMPL-32788, SIMPL-32802, SIMPL-32807 |
| Notification / e-mail to Providers | SIMPL-14709, SIMPL-14712, SIMPL-14791, SIMPL-33694; BP02C.06 |
| Webhook subscription: webhookId, targetUrl, events | SIMPL-19992, SIMPL-24175 |
| Resource description, contract clause, Billing Schema | SIMPL-14701, SIMPL-32436, SIMPL-32497 (resource description); SIMPL-28815 (contract clause, Billing Schema); BP02C Assumptions ("billing schema") |
| Tier2 credentials | SIMPL-24175 |
| Vocabulary, Metadata Quality Profile | OpenAPI only |

### Pairs of words that may mean the same

- "schema concept" (SIMPL-13347, SIMPL-13353, SIMPL-32802) and "schema" (most stories).
- "resource description schema" (BP02C, OpenAPI summaries), "resource schema" (SIMPL-32258/AC1, SIMPL-13346), "Resource Definition Schema" (SIMPL-32802, SIMPL-32807 other_text) and "RESOURCE schema" (SIMPL-33694).
- "Federated Search schema" (SIMPL-33537), "federated search schema" (SIMPL-31098, SIMPL-32436) and "SEARCH schema" (SIMPL-33694).
- "Resource description schema" / "Federated search schema" (SIMPL-32258/AC4) and `RESOURCE` / `SEARCH` (SIMPL-33537).
- `GA_SCHEMA_ADMIN` (most stories), `governance-admin` (SIMPL-13358/AC1, SIMPL-32808/AC2) and "role authorising schema management" (SIMPL-14703, SIMPL-33694).
- `GA_SCHEMA_VIEWER` (SIMPL-13343/AC1), `governance-viewer` (SIMPL-13343/AC4) and "role authorising schema viewing" (SIMPL-33537/AC3).
- "Simpl" (SIMPL-14703, SIMPL-17066), "Simpl-Open" (SIMPL-33537, SIMPL-33694), "Schema Management Service (SMS)" (SIMPL-13353) and "the service" (SIMPL-13343).
- "governance management portal" (SIMPL-13355), "schema management portal" (SIMPL-15837), "schema management UI" (SIMPL-32800), "UI of the Governance Authority" (SIMPL-13361) and "governance management UI" (SIMPL-20770).
- "PUBLISH" (SIMPL-20530/AC2) and "PUBLISHED" (SIMPL-20529/AC1).
- SemVer version "1.0.0" / "1.3.0" (SIMPL-13353, SIMPL-13357, SIMPL-13358, SIMPL-32788) and integer version "1, 2, 3" (SIMPL-20528, SIMPL-20534 technical details; OpenAPI `version` parameter).
- "versionInfo" (SIMPL-13353/AC1), "version" (SIMPL-13357) and "schemaVersion" (OpenAPI `CreateSchemaResponsePayload`).
- "name" (SIMPL-13353, SIMPL-13355) and "schemaName" (SIMPL-32800, SIMPL-32807).
- resourceType values `data` / `DATA` / `data-schema` / `Data`, and `application` / `APP` / `Application` (see data objects).
- "error message" (SIMPL-14796), "problem details JSON object" (SIMPL-13357), "structured error response" (SIMPL-22407), "feedback" (BP02C 2C.5) and "report" (BP02C.07).
- "409 Conflict" (SIMPL-13353, SIMPL-32802/AC4) and "409 Config" / "409 CONFIG" (SIMPL-32800/AC3 to AC5, SIMPL-32802/AC6).
- "404 Not Found" (SIMPL-13345) and "404 Resource Not Found" (SIMPL-20529/AC5).
- "Resolver Interface endpoint" (SIMPL-13347/AC2, SIMPL-32788/AC4) and "the endpoint for the schema concept" (SIMPL-13347/AC1).
- "schema versions page" (SIMPL-13346), "schema versions list page" (SIMPL-13365), "details page for a specific schema" (SIMPL-13358) and "list of schema's page" (SIMPL-20770).
- "notify Providers" by e-mail (SIMPL-14712, SIMPL-33694) and "event notifications" by webhook subscription (SIMPL-19992).
- "billing schema" as a contract clause schema (BP02C Assumptions) and contract clause of type "Billing Schema" (SIMPL-28815).
- ADR-008 (SIMPL-31098, SIMPL-32802, SIMPL-32808) and ADR-009 (SIMPL-32436, SIMPL-32497, SIMPL-32801), both cited for "only one federated search schema".
- "default properties" (SIMPL-14703), "default or mandatory properties" (SIMPL-14796) and "UUID and Version default properties" (SIMPL-26105).

## Totals

- Links: 342. Named: 82. Inferred: 260.
- To operations: 174 (82 named, 92 inferred). To process steps: 168 (all inferred; no criterion names BP02C).
- Criteria linked: 192 of 209. Operations linked: 9 of 18. Process steps linked: 7 of 7.
