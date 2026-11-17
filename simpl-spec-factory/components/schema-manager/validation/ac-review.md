# AC review (step 20b): schema-manager

Judgement checks on the acceptance criteria of `specs/stories/` against `specs/api/schema_openapi.yaml` and `specs/functional/bp02c-manage-resource-description-schemas.md`, using the step 12 inventory, `links.json`, `inventory-review.md` and the step 20a hits. The story files were read directly; where `inventory.json` differs from a story file (merged or shifted criteria, see `inventory-review.md`), the story file was judged and the finding points to the nearest inventory id. Findings are in [`ac-findings.json`](./ac-findings.json).

## Summary

| Check | Name | Blocker | Major | Minor | Total |
|---|---|---|---|---|---|
| AC-C01 | Completeness | 1 | 3 | 3 | 7 |
| AC-C02 | Testability | 0 | 7 | 5 | 12 |
| AC-C03 | Ambiguity | 4 | 9 | 3 | 16 |
| AC-C04 | Consistency | 5 | 6 | 5 | 16 |
| AC-C05 | Correctness vs API and process | 3 | 10 | 3 | 16 |
| AC-C06 | Scope | 0 | 3 | 1 | 4 |
| **Total** | | **13** | **38** | **20** | **71** |

71 findings: 30 cross-story, 13 at story level, 28 at AC level. 55 stories, 209 live criteria in the inventory.

### The 5 findings with the most impact on a build

1. **F-001**: Version identifier: SemVer supplied by the client versus a server-generated integer. It changes the request body, the response, the 409 and 400 cases and the UI form.
1. **F-003**: Status lifecycle on creation: a new schema stored as REVOKED when one is already published, against BP02C.05 'published upon successful validation' and the notification stories.
1. **F-002**: Re-publication by PATCH with status PUBLISHED, which the OpenAPI forbids ('Only REVOKED is supported').
1. **F-006**: Default properties: Simpl-Open adds them (BP02C.04) or rejects schemas without them (SIMPL-14703, SIMPL-14796); the list itself is only in external tickets.
1. **F-007**: Uniqueness key of a schema name (name, name plus type, or name plus type plus resource type), which decides 409 on create and what PATCH and GET by name address.

## Blockers

Step 30 must close each of these with a decision row or mark it as accepted risk.

| Id | Target | Check | Question |
|---|---|---|---|
| F-001 | `cross` | AC-C04 | Is the version a server-generated integer string ("1", "2", ...) everywhere, and are the SemVer criteria (client-supplied version, 409 on a duplicate version, 400 on a bad version format, the SemVer field in the UI) withdrawn? |
| F-002 | `cross` | AC-C05 | Can a REVOKED schema be set back to PUBLISHED through PATCH /schemas/{schemaName}, and if so with which response code and body? |
| F-003 | `cross` | AC-C04 | When a new schema is created for a resource type that already has a PUBLISHED schema, is it stored as REVOKED (and are Providers then not notified), is it rejected with 409, or is it published and the other one revoked? |
| F-004 | `cross` | AC-C04 | Does each version carry its own status (previous version becomes REVOKED), or does a version inherit the status of its schema? What does a revoked schema's version list show? |
| F-005 | `cross` | AC-C05 | Is GET /schemas/{schemaName} (latest content, text/turtle, Content-Location header) part of this service's API? If yes, under which base path, with which authentication and roles, and will the OpenAPI be extended? |
| F-006 | `cross` | AC-C04 | Does Simpl-Open add missing default properties to a submitted schema, or reject the schema? Which exact properties (IRIs, constraints) are the default properties, including UUID and Version, per resource type? |
| F-007 | `cross` | AC-C04 | What is the uniqueness key of a schema: name, name plus schemaType, or name plus schemaType plus resourceType? Which resource is addressed when a request names only the schemaName? |
| F-008 | `cross` | AC-C03 | Does 409 apply when any SEARCH schema exists, or only when a PUBLISHED one exists? May a new SEARCH schema reuse a revoked SEARCH schema's name? After revocation, is a SEARCH schema published again by creating a new schema, a new version, or by PATCH? |
| F-009 | `cross` | AC-C03 | Who receives the notification (one configured mailbox, every Provider known to the data space, or webhook subscribers), through which channel, and how does a test observe it? |
| F-010 | `SIMPL-19992` | AC-C01 | What does the HTTP POST to the target URL contain, which actions raise SchemaPublished (new schema, new version, re-publication) and SchemaRevoked (revocation, implicit revocation of a previous version), and what happens when delivery fails (retries, timeout)? |
| F-011 | `SIMPL-32798/AC3` | AC-C03 | Does GET /schemas/{schemaName}/{version} return the Turtle content (as the OpenAPI says) or the version metadata as JSON? |
| F-012 | `SIMPL-32807/AC6` | AC-C03 | Is the case 'name exists as RESOURCE only, request with schemaType SEARCH, expect 404'? And is the 'version number that already exists' line still relevant now that versions are server-generated? |
| F-013 | `cross` | AC-C05 | Which resourceType values are valid (exact spelling and case), is the set closed (data, application, infrastructure) or free text, and does the API compare them case-sensitively? |

## Per story

Each table lists the inventory AC ids with the findings that name them (AC-level findings, story-level findings and cross-story findings that list the id in their evidence). Severity is the highest of those findings. A story row `(story)` holds story-level findings.

### SIMPL-13343

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-016, F-030, F-031 | major |
| AC2 | F-013 | blocker |
| AC3 | F-014 | major |
| AC4 | F-015 | major |
| AC5 | none | |

### SIMPL-13345

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-016 | major |
| AC2 | none | |
| AC3 | none | |

### SIMPL-13346

| AC | Findings | Severity |
|---|---|---|
| (story) | F-068 | minor |
| AC1 | F-004, F-017, F-018, F-068 | blocker |

### SIMPL-13347

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-005, F-023 | blocker |
| AC2 | F-005 | blocker |

### SIMPL-13353

| AC | Findings | Severity |
|---|---|---|
| (story) | F-071 | minor |
| AC1 | F-001, F-013, F-019, F-023, F-071 | blocker |
| AC2 | F-007 | blocker |
| AC3 | none | |

### SIMPL-13355

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-013 | blocker |
| AC2 | F-013, F-032 | blocker |

### SIMPL-13357

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-004, F-020, F-021 | blocker |
| AC2 | none | |
| AC3 | F-001 | blocker |
| AC4 | none | |
| AC5 | F-001 | blocker |
| AC6 | F-052, F-057 | minor |

### SIMPL-13358

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-015, F-030, F-056 | blocker |
| AC2 | F-001 | blocker |
| AC3 | F-001, F-028 | blocker |

### SIMPL-13360

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-007, F-022, F-057 | blocker |
| AC2 | none | |
| AC3 | none | |
| AC4 | none | |

### SIMPL-13361

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-029 | major |
| AC2 | F-059 | minor |
| AC3 | none | |
| AC4 | none | |
| AC5 | none | |
| AC6 | none | |

### SIMPL-13364

| AC | Findings | Severity |
|---|---|---|
| (story) | F-033 | major |
| AC3 | F-001, F-023, F-033 | blocker |
| AC1 | F-024, F-033 | major |
| AC2 | F-024, F-033 | major |

### SIMPL-13365

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-058 | blocker |
| AC2 | F-058 | minor |

### SIMPL-14354

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-024, F-034 | major |

### SIMPL-14701

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-025 | major |

### SIMPL-14703

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-006, F-054 | blocker |
| AC2 | F-006, F-054 | blocker |
| AC3 | F-006, F-054 | blocker |
| AC4 | F-006, F-054 | blocker |

### SIMPL-14709

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-003, F-009, F-026 | blocker |

### SIMPL-14712

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-009, F-026 | blocker |
| AC2 | F-009, F-066 | blocker |

### SIMPL-14791

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-009, F-026 | blocker |

### SIMPL-14795

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-027 | major |
| AC2 | F-027 | major |

### SIMPL-14796

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-006, F-027 | blocker |
| AC2 | F-027 | major |

### SIMPL-14797

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-027, F-035 | major |
| AC2 | F-027 | major |
| AC3 | none | |
| AC4 | F-027 | major |

### SIMPL-15837

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-017, F-018 | major |
| AC2 | F-013 | blocker |
| AC3 | none | |
| AC4 | F-014, F-055 | major |
| AC5 | none | |

### SIMPL-17066

| AC | Findings | Severity |
|---|---|---|
| (story) | F-040 | major |
| AC1 | none | |
| AC2 | none | |
| AC3 | none | |
| AC4 | F-038 | major |
| AC5 | F-039 | major |
| AC6 | none | |
| AC7 | F-040 | major |
| AC8 | F-040 | major |
| AC9 | F-040 | major |
| AC10 | F-038, F-040 | major |
| AC11 | F-039, F-040 | major |
| AC12 | F-040 | major |

### SIMPL-19992

| AC | Findings | Severity |
|---|---|---|
| (story) | F-010, F-061 | blocker |
| AC1 | F-010, F-041, F-061 | blocker |
| AC2 | none | |
| AC3 | F-060 | minor |
| AC4 | F-042 | major |
| AC5 | none | |

### SIMPL-20067

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-005, F-069 | blocker |
| AC2 | F-005, F-056 | blocker |

### SIMPL-20528

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001 | blocker |
| AC2 | F-052 | minor |

### SIMPL-20529

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-002, F-053 | blocker |
| AC2 | F-053 | minor |
| AC3 | none | |
| AC4 | none | |
| AC5 | none | |
| AC6 | none | |
| AC7 | F-022 | major |
| AC8 | F-002, F-003, F-022 | blocker |
| AC9 | F-052 | minor |

### SIMPL-20530

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-004, F-029 | blocker |
| AC2 | F-002, F-043 | blocker |
| AC3 | F-002, F-043 | blocker |
| AC4 | F-002, F-043, F-056 | blocker |

### SIMPL-20534

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001 | blocker |
| AC2 | F-052 | minor |

### SIMPL-20686

| AC | Findings | Severity |
|---|---|---|
| (story) | F-067 | minor |
| AC1 | F-003, F-067 | blocker |
| AC2 | F-003, F-067 | blocker |
| AC3 | F-003, F-067 | blocker |

### SIMPL-20770

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-030 | major |
| AC2 | F-001, F-056 | blocker |
| AC3 | F-028, F-056 | major |

### SIMPL-22407

| AC | Findings | Severity |
|---|---|---|
| (story) | F-035 | major |
| AC1 | F-035 | major |
| AC2 | F-035 | major |
| AC3 | F-035, F-036 | major |
| AC4 | F-035, F-036 | major |

### SIMPL-24172

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-027, F-028 | major |
| AC2 | F-028 | major |
| AC3 | none | |
| AC4 | F-027 | major |

### SIMPL-24173

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-006, F-027, F-028 | blocker |
| AC2 | F-027 | major |

### SIMPL-24174

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-027, F-028, F-035 | major |
| AC2 | F-028 | major |
| AC3 | none | |
| AC4 | F-027 | major |

### SIMPL-24175

| AC | Findings | Severity |
|---|---|---|
| (story) | F-037 | major |
| AC1 | F-037 | major |
| AC2 | F-037 | major |
| AC3 | F-014, F-037 | major |

### SIMPL-26105

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-006, F-054 | blocker |
| AC2 | F-006, F-054 | blocker |
| AC3 | F-006, F-054 | blocker |
| AC4 | F-006, F-054 | blocker |

### SIMPL-28815

| AC | Findings | Severity |
|---|---|---|
| (story) | F-044 | major |
| AC1 | F-044 | major |
| AC2 | F-044 | major |
| AC3 | F-044 | major |
| AC4 | F-044 | major |
| AC5 | F-044 | major |
| AC6 | F-044 | major |

### SIMPL-31098

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-030, F-062 | major |
| AC2 | F-002 | blocker |
| AC3 | F-005, F-008 | blocker |

### SIMPL-32258

| AC | Findings | Severity |
|---|---|---|
| (story) | F-070 | minor |
| AC4 | none | |
| AC1 | F-016, F-030, F-031, F-070 | major |
| AC2 | F-016 | major |
| AC3 | F-013 | blocker |
| AC5 | F-014 | major |
| AC6 | none | |
| AC7 | F-008 | blocker |

### SIMPL-32436

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-025 | major |
| AC2 | F-025 | major |

### SIMPL-32497

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-025 | major |
| AC2 | F-025 | major |

### SIMPL-32786

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-016 | major |
| AC2 | F-016 | major |
| AC3 | none | |
| AC4 | none | |

### SIMPL-32787

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-004, F-017, F-018, F-068 | blocker |

### SIMPL-32788

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-005, F-023 | blocker |
| AC2 | F-005 | blocker |
| AC3 | F-005, F-064 | blocker |
| AC4 | F-005 | blocker |

### SIMPL-32790

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-017, F-018 | major |
| AC2 | F-013, F-045 | blocker |
| AC3 | none | |
| AC4 | F-014, F-055 | major |
| AC5 | none | |

### SIMPL-32797

| AC | Findings | Severity |
|---|---|---|
| (story) | F-046 | major |
| AC1 | F-046, F-058 | major |
| AC2 | F-046, F-058 | major |

### SIMPL-32798

| AC | Findings | Severity |
|---|---|---|
| AC3 | F-001, F-007, F-011, F-023 | blocker |
| AC4 | F-007, F-011 | blocker |
| AC1 | F-024 | major |
| AC2 | F-024 | major |

### SIMPL-32800

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-013, F-047 | blocker |
| AC2 | none | |
| AC3 | F-007, F-013, F-047, F-057 | blocker |
| AC4 | F-007, F-057 | blocker |
| AC5 | F-008, F-057 | blocker |

### SIMPL-32801

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-003, F-067 | blocker |
| AC2 | F-003 | blocker |
| AC3 | F-003 | blocker |
| AC4 | F-008 | blocker |
| AC5 | F-008 | blocker |
| AC6 | F-008, F-063 | blocker |

### SIMPL-32802

| AC | Findings | Severity |
|---|---|---|
| (story) | F-048 | major |
| AC1 | F-001, F-013, F-019, F-023, F-048, F-071 | blocker |
| AC2 | F-001, F-019 | blocker |
| AC3 | none | |
| AC4 | F-007 | blocker |
| AC5 | F-007 | blocker |
| AC6 | F-008, F-057 | blocker |
| AC7 | none | |

### SIMPL-32807

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-004, F-020, F-021 | blocker |
| AC2 | F-004, F-020 | blocker |
| AC3 | F-004, F-020 | blocker |
| AC4 | F-050 | major |
| AC5 | F-050 | major |
| AC6 | F-012 | blocker |
| AC7 | none | |
| AC8 | none | |
| AC9 | F-052 | minor |

### SIMPL-32808

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-030, F-049, F-056 | major |
| AC2 | F-015, F-049 | major |
| AC3 | F-049 | major |
| AC4 | none | |
| AC5 | F-050 | major |

### SIMPL-33537

| AC | Findings | Severity |
|---|---|---|
| AC6 | none | |
| AC1 | F-006, F-021, F-051 | blocker |
| AC2 | F-008 | blocker |
| AC3 | none | |
| AC4 | F-005 | blocker |
| AC5 | F-002, F-008 | blocker |

### SIMPL-33694

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-003, F-009 | blocker |
| AC2 | F-009, F-026 | blocker |
| AC3 | F-009, F-026 | blocker |
| AC4 | F-009 | blocker |
| AC5 | F-009, F-065 | blocker |

## Cross-story

Consistency findings (AC-C04), across stories and against the OpenAPI or the process wording.

- **F-001** (blocker, `cross`): The stories use two different version identifiers. Older stories expect a SemVer string supplied by the client or set to 1.0.0; newer stories and the OpenAPI expect a server-generated integer. Tests and code built from different stories will disagree on the version value, the request body and the 409 and 400 cases.
- **F-003** (blocker, `cross`): The stories disagree on what status a newly created schema gets. Some stories store a new schema as REVOKED when another schema of the same resource type is published; the business process publishes every schema that passes validation, and the notification stories notify on every new schema.
- **F-004** (blocker, `cross`): It is not clear whether status belongs to a schema version or to the schema concept. Two stories set the previous version to REVOKED when a new version is created; another story says versions have no status of their own. The UI stories show a status per version, but the versions response in the OpenAPI example has no status field.
- **F-006** (blocker, `cross`): The business process says Simpl-Open adds the default properties to a schema before validating it. The stories instead reject a schema that does not already contain them. The list of default properties is only in external tickets, so neither reading can be tested from the specs.
- **F-007** (blocker, `cross`): The stories do not agree on what makes a schema name unique. This decides when POST returns 409 and which schema PATCH /schemas/{schemaName} and GET by name address.
- **F-015** (major, `cross`): Role names differ between stories. Most use GA_SCHEMA_ADMIN and GA_SCHEMA_VIEWER; three criteria use governance-admin and governance-viewer. A builder must know which role names to check in the token.
- **F-027** (major, `cross`): The expected error 'detail' texts differ between stories and from the OpenAPI. The same failure (missing default properties) has two different prefixes, and no API criterion names the status code.
- **F-029** (major, `cross`): Two UI stories describe revocation differently: one with a confirmation popup and one success text, the other without a popup and with another success text.
- **F-031** (major, `cross`): SIMPL-13343 asks for a list of all schemas, but since the schema type was introduced, GET /schemas without a filter returns only RESOURCE schemas. A test written from SIMPL-13343 may expect SEARCH schemas in the list.
- **F-043** (major, `SIMPL-20530/AC2`): After a successful Publish the criterion expects the status to become REVOKED, sends 'PUBLISH' instead of 'PUBLISHED', and gives a success text that differs from the technical details.
- **F-050** (major, `SIMPL-32808/AC5`): This criterion expects 409 when no former version exists for the chosen schemaType; the API story expects 404 for the same case.
- **F-056** (minor, `cross`): UI stories place errors and redirects in different places for similar flows, and some refer to a form where there is none.
- **F-057** (minor, `cross`): Status code names are misspelled in some criteria ('409 CONFIG', '409 Config', '500 Error', '{schemaName{}}').
- **F-063** (minor, `SIMPL-32801/AC6`): The NOTE says one SEARCH schema exists 'for each resourceType' and in the same sentence that resourceType is empty for SEARCH.
- **F-064** (minor, `SIMPL-32788/AC3`): The SEARCH path uses the example URL of the RESOURCE schema.
- **F-070** (minor, `SIMPL-32258`): The testing hint says GET /schemas retrieves 'all schemas and all versions of them', but the endpoint returns one entry per schema.

Other cross-story findings (not AC-C04): F-002 (AC-C05, blocker), F-005 (AC-C05, blocker), F-008 (AC-C03, blocker), F-009 (AC-C03, blocker), F-013 (AC-C05, blocker), F-014 (AC-C05, major), F-016 (AC-C02, major), F-017 (AC-C05, major), F-018 (AC-C03, major), F-019 (AC-C05, major), F-020 (AC-C05, major), F-021 (AC-C01, major), F-023 (AC-C03, major), F-024 (AC-C03, major), F-025 (AC-C06, major), F-026 (AC-C02, major), F-028 (AC-C02, major), F-030 (AC-C06, major), F-052 (AC-C02, minor).

## 20a triage

Per 20a code: hits that are real (a finding of their own or part of one in this review) and hits that are harmless.

| Code | Hits | Real | Harmless | Why harmless (typical) | Example of a harmless hit |
|---|---|---|---|---|---|
| AC-S01 | 6 | 0 | 6 | parser merges and artefacts; the source has a Given or the line is not a criterion | SIMPL-14709/AC1: the source has '1. Given ...' and '2. Given ...'; the parser merged them |
| AC-S03 | 2 | 0 | 2 | parser artefacts, not criteria | SIMPL-32258/AC4: a sentence in the description, not a criterion |
| AC-S04 | 49 | 9 | 40 | UI flows with several user steps, preconditions written as When, or merged criteria; each step is observable | SIMPL-13361/AC1: click Revoke, confirm in the popup; a normal UI sequence |
| AC-S05 | 7 | 0 | 7 | many outcomes, but each one is observable on the main path | SIMPL-13357/AC1: six outcomes, all checkable in the response and the stored state |
| AC-S09 | 189 | 0 | 189 | ids are derived from order; stable while the input set is frozen | SIMPL-13343/AC3: unnumbered 'Negative Scenario' that keeps its position |
| AC-S10 | 120 | 39 | 81 | technical notes, separators and shifted headings that do not change a clause | SIMPL-17066/AC1: only the '---' separator lines |
| AC-S12 | 30 | 0 | 30 | near-duplicates that differ in the case they test (status code, new schema or new version) | SIMPL-13361/AC3 vs AC2: same flow, 400 instead of 403 |
| AC-W01 | 28 | 3 | 25 | the vague word comes with an example text, a rule, or is part of a status name | SIMPL-20529/AC6: 'acceptable' is part of '406 Not Acceptable' |
| AC-W03 | 25 | 2 | 23 | 'should' or 'either' in a Given or in an outcome that is concrete | SIMPL-13343/AC5: 'should contain a problem details JSON object', the OpenAPI defines it |
| AC-W05 | 4 | 2 | 2 | no limit is implied | SIMPL-15837/AC5: 'no results were found' has no time or limit |
| **Total** | **460** | **55** | **405** | | |

Real hits by code: AC-W01: SIMPL-13343/AC1, SIMPL-14712/AC1, SIMPL-33694/AC2; AC-W03: SIMPL-15837/AC4, SIMPL-32790/AC4; AC-W05: SIMPL-15837/AC4, SIMPL-32790/AC4; AC-S04: SIMPL-14703/AC1, SIMPL-14703/AC2, SIMPL-20528/AC2, SIMPL-20529/AC1, SIMPL-20529/AC2, SIMPL-20534/AC2, SIMPL-26105/AC1, SIMPL-26105/AC2, SIMPL-33537/AC1; AC-S10: SIMPL-13343/AC2, SIMPL-13347/AC2, SIMPL-13355/AC2, SIMPL-13357/AC1, SIMPL-13364/AC2, SIMPL-14354/AC1, SIMPL-14701/AC1, SIMPL-14703/AC3, SIMPL-14703/AC4, SIMPL-14712/AC2, SIMPL-14795/AC2, SIMPL-14796/AC2, SIMPL-14797/AC4, SIMPL-15837/AC4, SIMPL-20528/AC2, SIMPL-20530/AC4, SIMPL-20534/AC2, SIMPL-22407/AC3, SIMPL-22407/AC4, SIMPL-24172/AC4, SIMPL-24173/AC2, SIMPL-24174/AC4, SIMPL-24175/AC3, SIMPL-26105/AC3, SIMPL-26105/AC4, SIMPL-31098/AC3, SIMPL-32258/AC3, SIMPL-32258/AC7, SIMPL-32436/AC2, SIMPL-32497/AC2, SIMPL-32790/AC4, SIMPL-32797/AC2, SIMPL-32798/AC2, SIMPL-32801/AC6, SIMPL-32802/AC7, SIMPL-32807/AC1, SIMPL-32807/AC2, SIMPL-32807/AC3, SIMPL-32807/AC6.

## Coverage

Stories with the most findings (direct or through a cross-story finding): SIMPL-32802 (9), SIMPL-32258 (7), SIMPL-13357 (6), SIMPL-13353 (6), SIMPL-33537 (6), SIMPL-32807 (6), SIMPL-32790 (6), SIMPL-13343 (6).

Stories with the fewest: one finding: SIMPL-13345, SIMPL-14701, SIMPL-14795, SIMPL-28815, SIMPL-32436, SIMPL-32497, SIMPL-32786.

Reminder from the links (not repeated from step 12b): no criterion covers the six vocabulary operations (`/vocabularies...`), the two Metadata Quality Profile operations on `/metadataQualityProfiles/{metadataQualityProfileName}` and `GET /schemas/{schemaName}/qualityProfiles/active`. Semantic validation (SIMPL-22407) depends on internal vocabularies, but no criterion manages them through this API. Process requirement 2C.1 (retrieval) has no step section, and 'Trigger resource description schema management' has no criterion; BP02C.06 (inform Providers) has the fewest links, and its recipients are open (F-009). The webhook event delivery (F-010) has neither a criterion nor an operation.
