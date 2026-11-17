# AC review (step 20b): Vocabulary Manager

Judgement checks on the 91 live acceptance criteria in 35 stories, from `specs/`, `inventory/` and the step 20a results in `ac-checks.*`. Findings are in `ac-findings.json`. This review states what is open; it does not decide.

Process requirement ids `BP02B#...` follow `inventory-review.md` and are not in `inventory.json`.

## Summary

| Check | Blocker | Major | Minor | Total |
|---|---|---|---|---|
| AC-C01 | 0 | 8 | 0 | 8 |
| AC-C02 | 1 | 4 | 3 | 8 |
| AC-C03 | 4 | 11 | 4 | 19 |
| AC-C04 | 0 | 0 | 4 | 4 |
| AC-C05 | 4 | 7 | 0 | 11 |
| AC-C06 | 1 | 2 | 0 | 3 |
| **Total** | **10** | **32** | **11** | **53** |

Five findings with the most impact on a build:

1. **F-001** (AC-C03, blocker, `cross`): The specs use two status levels, vocabulary status and version status, but no criterion says how they relate. A builder cannot tell what deprecating or reactivating a vocabulary does to its versions, which status the list shows, or whether a deprecated vocabulary can get a new version.
1. **F-002** (AC-C03, blocker, `cross`): The role model is named five different ways and the criteria do not say which role may read and which may write. With a read-only role and an admin role in the notes, the blanket 403 rule can be read two ways.
1. **F-003** (AC-C05, blocker, `SIMPL-18184/AC2`): The criterion blocks a new version that references a deprecated vocabulary version, while the OpenAPI creates the version and returns a warning for exactly that case, and the process says deprecated vocabularies can still be referenced.
1. **F-005** (AC-C02, blocker, `cross`): No spec lists the syntax, structure and semantic rules, so a tester cannot build a passing and a failing vocabulary for each criterion. The UI story also names a 'structure' check that the API stories do not have.
1. **F-006** (AC-C05, blocker, `SIMPL-18180/AC1`): Validation must run when a file is uploaded, before submission, but the API offers no validate-only operation and the API story states there is no separate validation endpoint.

## Blockers

Step 30 has to close each of these with a decision row or accept the risk.

| Id | Target | Check | Question |
|---|---|---|---|
| F-001 | `cross` | AC-C03 | When the vocabulary status is set to DEPRECATED or back to ACTIVE, what happens to the status of each version, which status does GET /vocabularies show, and can a new version be created for a DEPRECATED vocabulary (and if so, does the vocabulary become ACTIVE)? |
| F-002 | `cross` | AC-C03 | Which roles exist for this component, which operations and screens does each role allow, and is 'the vocabulary management role' in the criteria GA_VOCABULARY_ADMIN only, or either of the two roles? Is the check done by the gateway (Tier 1) or by the service? |
| F-003 | `SIMPL-18184/AC2` | AC-C05 | Does a reference to a DEPRECATED internal vocabulary version block creation (400) or create the version with a warning (201 with validationWarnings)? |
| F-004 | `SIMPL-18183/AC2` | AC-C03 | Do semantic warnings (for example an unresolved external namespace) stop creation, or is creation refused only when there is at least one semantic error? |
| F-005 | `cross` | AC-C02 | Which rules make up structure validation and semantic validation (with one example per rule), and is 'structure' a separate step from 'semantic' or the same thing under another name? |
| F-006 | `SIMPL-18180/AC1` | AC-C05 | Where does validation on upload run: in the browser, through a new validate-only endpoint, or not at all (validation only on submit)? |
| F-007 | `SIMPL-18169/AC1` | AC-C05 | What changelog value does the UI send when creating a vocabulary, or should changelog be optional on POST /vocabularies? |
| F-008 | `SIMPL-18172/AC2` | AC-C03 | Must the user upload a new file for every new version, or may the UI resend the current content when only the description changes? What does 'pre-filled with ... content' show? |
| F-009 | `SIMPL-18163/AC1` | AC-C05 | Is the name filter a case-insensitive 'contains' search or an exact name match? |
| F-010 | `SIMPL-28680` | AC-C06 | Is SIMPL-28680 part of the Vocabulary Manager build, or only of Schema Manager? If the MQP vocabulary must exist in Vocabulary Manager, is that seed data for this component? |

## Per story

Cross-story findings are listed against every criterion they involve. "story" marks a finding on the story as a whole.

### SIMPL-393: Provide CPS (Core Public Services) vocabulary

| AC | Findings | Severity |
|---|---|---|
| story | F-041 | major |

### SIMPL-395: Provide UI for additional vocabulary

| AC | Findings | Severity |
|---|---|---|
| story | F-042 | major |

### SIMPL-18161: View All vocabularies and their definitions via UI

| AC | Findings | Severity |
|---|---|---|
| story | F-020 | major |
| AC1 | F-001, F-002, F-019, F-020, F-044, F-045 | blocker, major, minor |
| AC2 | - | - |

### SIMPL-18162: Retrieve metadata of all vocabularies via API

| AC | Findings | Severity |
|---|---|---|
| story | F-018 | major |
| AC1 | F-045 | minor |
| AC2 | F-018 | major |
| AC3 | F-002, F-011 | blocker, major |
| AC4 | - | - |

### SIMPL-18163: Sort and filter vocabulary list via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-009, F-020 | blocker, major |
| AC2 | F-021 | major |

### SIMPL-18164: Apply filters and sorting via vocabulary list API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-009, F-016, F-045 | blocker, major, minor |
| AC2 | - | - |
| AC3 | F-018 | major |
| AC4 | F-017, F-046 | major, minor |

### SIMPL-18165: Download and view full details of the newest vocabulary version in the UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-022, F-044 | major, minor |

### SIMPL-18166: Retrieve full details of a vocabulary version via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-024 | major |
| AC2 | F-046 | minor |

### SIMPL-18167: View version history of a vocabulary in the UI

| AC | Findings | Severity |
|---|---|---|
| story | F-026 | major |
| AC1 | F-025, F-026, F-044, F-045 | major, minor |

### SIMPL-18168: Retrieve version history of a vocabulary via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-027 | major |
| AC2 | - | - |

### SIMPL-18169: Create a new vocabulary via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-007, F-043 | blocker, minor |
| AC2 | F-013, F-035 | major |

### SIMPL-18170: Create a new vocabulary via API

| AC | Findings | Severity |
|---|---|---|
| story | F-036 | major |
| AC1 | - | - |
| AC2 | F-001 | blocker |
| AC3 | F-013, F-036, F-046 | major, minor |

### SIMPL-18172: Create a new version of a vocabulary via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-008 | blocker |
| AC2 | F-008, F-013, F-015, F-045 | blocker, major, minor |
| AC3 | - | - |

### SIMPL-18173: Create a new version of a vocabulary via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-014, F-015 | major |
| AC2 | F-013, F-036 | major |
| AC3 | F-046 | minor |
| AC4 | F-011 | major |
| AC5 | F-011 | major |
| AC6 | F-013, F-037 | major |

### SIMPL-18174: Update vocabulary status via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-038 | blocker, major |
| AC2 | F-049 | minor |
| AC3 | - | - |

### SIMPL-18175: Update vocabulary status via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-038 | blocker, major |
| AC2 | - | - |
| AC3 | F-046 | minor |

### SIMPL-18176: Access deprecated vocabulary versions in UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-002, F-044 | blocker, minor |
| AC2 | F-028 | major |
| AC3 | F-029 | major |

### SIMPL-18178: Warning on deprecated vocabularies in Vocabulary Management UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-030 | major |

### SIMPL-18180: Validate vocabulary syntax and structure via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-006, F-013 | blocker, major |
| AC2 | F-006 | blocker |
| AC3 | F-006 | blocker |
| AC4 | F-005 | blocker |
| AC5 | - | - |
| AC6 | F-005 | blocker |
| AC7 | - | - |
| AC8 | F-048 | minor |

### SIMPL-18181: Validate vocabulary syntax via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-005, F-033, F-043 | blocker, major, minor |
| AC2 | F-034 | major |

### SIMPL-18182: Validate vocabulary semantics via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-050 | minor |
| AC2 | F-005 | blocker |
| AC3 | F-005 | blocker |

### SIMPL-18183: Validate vocabulary semantics via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-005 | blocker |
| AC2 | F-004, F-005 | blocker |

### SIMPL-18184: Automatic check for deprecated vocabularies during new version creation in UI

| AC | Findings | Severity |
|---|---|---|
| story | F-032 | major |
| AC1 | F-002, F-032 | blocker, major |
| AC2 | F-003, F-032 | blocker, major |
| AC3 | F-032 | major |
| AC4 | F-003, F-031 | blocker, major |

### SIMPL-18186: Automatic publication of vocabularies UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-012 | blocker, major |

### SIMPL-18187: Publish vocabulary via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-012, F-051 | major, minor |
| AC2 | F-012, F-051 | major, minor |

### SIMPL-18189: Automatic versioning of vocabularies via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-014 | major |
| AC2 | - | - |
| AC3 | - | - |

### SIMPL-18190: Automatic versioning of vocabularies via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-014 | major |
| AC2 | F-046 | minor |

### SIMPL-18191: Automatic deprecation of previous vocabulary version UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-015 | major |

### SIMPL-18193: Receive vocabulary validation report via UI

| AC | Findings | Severity |
|---|---|---|
| story | F-040 | major |
| AC1 | F-040 | major |
| AC2 | F-040 | major |

### SIMPL-18194: Receive vocabulary validation report via API

| AC | Findings | Severity |
|---|---|---|
| AC1 | - | - |
| AC2 | - | - |

### SIMPL-18195: Role-based access control for vocabulary management UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-002 | blocker |
| AC2 | F-039 | major |
| AC3 | - | - |

### SIMPL-18196: Role-based access control for vocabulary management API

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-002, F-052 | blocker, minor |
| AC2 | F-002, F-011 | blocker, major |
| AC3 | F-011, F-052 | major, minor |
| AC4 | F-002 | blocker |

### SIMPL-18452: Download and view full details of all vocabulary versions in the UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-023 | major |
| AC2 | F-022 | major |

### SIMPL-18559: Warning on deprecated vocabulary versions in Vocabulary History UI

| AC | Findings | Severity |
|---|---|---|
| story | F-047 | minor |
| AC1 | F-002, F-025, F-044, F-047 | blocker, major, minor |
| AC2 | F-001, F-047 | blocker, minor |

### SIMPL-28680: [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile

| AC | Findings | Severity |
|---|---|---|
| story | F-010, F-053 | blocker, minor |
| AC1 | F-010, F-043 | blocker, minor |
| AC2 | F-010, F-053 | blocker, minor |
| AC3 | F-010 | blocker |
| AC4 | F-010 | blocker |
| AC5 | F-010 | blocker |
| AC6 | F-010, F-053 | blocker, minor |

## Cross-story consistency (AC-C04)

- **F-043** (minor): The same actor appears under several names. Evidence: 'Governance Authority' (most stories), 'Governance Authority user' (SIMPL-18169, 18172, 18189, 18191), 'governance authority agent' (SIMPL-393, 395), 'Governance Authority End User' (SIMPL-28680), 'user with the _Vocabulary Management_ role' (SIMPL-18181).
- **F-044** (minor): Screen names vary: management page, management screen, details page, history page, history interface. Evidence: SIMPL-18161/AC1: 'Vocabulary Management screen'; SIMPL-18165/AC1: 'Vocabulary Management page'; SIMPL-18167/AC1: 'vocabulary details page'; SIMPL-18559/AC1: 'vocabulary history page'; SIMPL-18176/AC1: 'vocabulary management or history interface'.
- **F-045** (minor): Field names differ between stories and the API. Evidence: SIMPL-18164/AC1 filter '`lastUpdated`' vs SIMPL-18162/AC1 and OpenAPI field 'lastUpdateDate'; SIMPL-18172/AC2 'change summary', SIMPL-18167/AC1 'Change log', OpenAPI 'changelog'; SIMPL-18161/AC1 'creator', OpenAPI 'createdBy'.
- **F-051** (minor): 'vocabulary repository' and 'vocabulary registry' are used for the store, and 'an identifier indicating the vocabulary is available' does not name the field. Evidence: SIMPL-18187/AC1: 'persisted in the vocabulary repository', 'the response includes an identifier indicating the vocabulary is available in the vocabulary registry'; BP Assumptions: 'centrally stored in the Vocabulary Hub'; OpenAPI 201: Location header, href, contentHref.

Cross-story findings under other checks that are also about consistency between stories, API and process: F-001 (AC-C03, blocker), F-002 (AC-C03, blocker), F-005 (AC-C02, blocker), F-011 (AC-C05, major), F-012 (AC-C05, major), F-013 (AC-C03, major), F-014 (AC-C03, major), F-015 (AC-C01, major), F-022 (AC-C03, major), F-038 (AC-C01, major), F-046 (AC-C02, minor).

## 20a triage

| Code | Hits | Real | Harmless | Example of a harmless hit |
|---|---|---|---|---|
| AC-S02 | 3 | 0 | 3 | SIMPL-18162/AC2: "And I request the metadata ..." plays the role of the When; the request is clear. |
| AC-S04 | 9 | 1 | 8 | SIMPL-18169/AC1: "navigate ... And click New Vocabulary +" is one UI path to the form. Real: SIMPL-18172/AC2 (F-008). |
| AC-S05 | 2 | 0 | 2 | SIMPL-18181/AC2: eight outcomes describe one rejection from several sides. |
| AC-S08 | 2 | 2 | 0 | none; both (SIMPL-393, SIMPL-395) are in F-041, F-042. |
| AC-S09 | 58 | 1 | 57 | SIMPL-18164/AC1: ids are stable as long as the frozen input set is used. Real: SIMPL-28680/AC2 (F-053). |
| AC-S10 | 36 | 7 | 29 | SIMPL-18165/AC1: the Figma link line. Real: SIMPL-18169/AC1, 18173/AC6, 18183/AC2, 18184/AC4, 18190/AC2, 18196/AC4, 28680/AC6. |
| AC-S12 | 4 | 0 | 4 | SIMPL-18180/AC5 vs AC4: success and syntax errors are different outcomes. |
| AC-W01 | 11 | 5 | 6 | SIMPL-18162/AC1 "relevant": the field list follows in brackets. Real: SIMPL-18169/AC2, 18178/AC1, 18183/AC1, 18559/AC1, 18559/AC2. |
| AC-W03 | 2 | 1 | 1 | SIMPL-18180/AC7 "syntax and/or structure errors": both lead to the same outcome. Real: SIMPL-18172/AC2. |
| AC-W05 | 1 | 0 | 1 | SIMPL-18172/AC3 "advised to retry": advice text, not a limit. |
| **Total** | **128** | **17** | **111** | |

## Coverage

Most findings (counting each finding once per story it involves): SIMPL-18173 (7), SIMPL-18161 (6), SIMPL-18164 (6), SIMPL-18559 (5), SIMPL-18162 (4), SIMPL-18167 (4).

Fewest findings: SIMPL-18194 (0), SIMPL-18168 (1), SIMPL-18178 (1), SIMPL-18189 (1), SIMPL-18191 (1), SIMPL-18193 (1), SIMPL-393 (1), SIMPL-395 (1).

Reminder from the links (`inventory-review.md`), not repeated in detail:

- Operation with no criterion: `GET /health`.
- Operations covered only by the blanket access rules of SIMPL-18196: `POST /vocabularies/searches`, `PATCH /vocabularies/{vocabularyName}/versions/{version}` (see F-015), and the seven external vocabulary operations (`/external-vocabularies...`, `/external-vocabulary/searches`), including `DELETE /external-vocabularies/{name}`. No story gives their behaviour.
- Process requirements: every `BP02B#2B.n` has at least one criterion. Thinnest: 2B.5.3 (deprecate previous version), 2B.4.3 (status of referenced vocabularies, see F-003, F-032) and 2B.5.1 (publish, see F-012).
- Criteria with no operation and no requirement: SIMPL-18195/AC1 to AC3 (UI access) and SIMPL-28680/AC1 to AC6 (see F-010).
