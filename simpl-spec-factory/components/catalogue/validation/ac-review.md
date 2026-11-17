# AC review (step 20b): catalogue

Judgement checks on the acceptance criteria of the catalogue input set, from `specs/`, `inventory/` and the step 20a output only. Criteria are read as the story files state them; where `inventory.json` misread a clause (dropped "Or" lines, missed "* `And`" clauses), the story file was used. Findings are in [`ac-findings.json`](./ac-findings.json).

## Summary

64 findings on 77 live criteria in 33 stories. 17 blockers.

| Check | blocker | major | minor | Total |
|---|---|---|---|---|
| AC-C01 | 0 | 11 | 2 | 13 |
| AC-C02 | 0 | 5 | 1 | 6 |
| AC-C03 | 5 | 8 | 0 | 13 |
| AC-C04 | 6 | 4 | 3 | 13 |
| AC-C05 | 5 | 8 | 0 | 13 |
| AC-C06 | 1 | 2 | 3 | 6 |
| **Total** | 17 | 38 | 9 | 64 |

### The 5 findings with the most impact on a build

1. **F-044**: The federated detail: three stories need the schema in the response, the Trustor/Trusted stories struck it, and the adapter contract says no schema is returned. The detail UI cannot be built until this is settled.
2. **F-033**: Grouped results with per-group paging have no response shape: the federated search operation returns one flat list and leaves aggregation undefined.
3. **F-034**: Who consolidates own and federated results: the backend in one response (SIMPL-31140) or the UI with two calls (SIMPL-31142 design).
4. **F-055**: Schema management (create, version, SEARCH type) points to an API outside this input set; with F-054 and F-011 the four schema stories have no usable contract here.
5. **F-022**: The Trustor-side search operation (and with F-052 the Trusted-side detail operations) is in no entry file, so the federation half of search has no contract.

## Blockers

Step 30 has to close each of these with a decision row or accept the risk.

| Id | Target | Question |
|---|---|---|
| F-004 | cross: SIMPL-29292/AC2, SIMPL-29293/AC2 | Is the score a fraction from 0 to 1 or a percentage from 0 to 100, and what is the upper bound the UI accepts? |
| F-005 | SIMPL-29292/AC3 | For quick search, which profile supplies the classification list and the ordering: one profile per data space, the profile of each result's schema, or a union? |
| F-011 | cross: SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-30850/AC3, SIMPL-31817/AC1, SIMPL-31817/AC2, SIMPL-31817/AC3 | Where is the versioning operation defined (which component, which contract), and which fields does it take? |
| F-012 | cross: SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-30850/AC4 | Is schemaType required on the Create New Version form, or optional with RESOURCE as default? |
| F-013 | cross: SIMPL-30850/AC3, SIMPL-30850/AC5, SIMPL-31817/AC1, SIMPL-31817/AC3 | Does the 409 apply only when the published SEARCH schema has a different name than the one being versioned? |
| F-022 | cross: SIMPL-31106/AC1, SIMPL-31138/AC1, SIMPL-31138/AC2, SIMPL-31138/AC3, SIMPL-31139/AC1, SIMPL-31139/AC2, SIMPL-31139/AC3 | Which contract defines the Trustor-side search operations, and can it be added to the input set? |
| F-026 | cross: SIMPL-31111/AC1, SIMPL-31138/AC1 | Is the search string split on whitespace with OR between terms, or matched as one phrase? Is matching case-insensitive and by substring? |
| F-030 | cross: SIMPL-31113/AC2, SIMPL-31113/AC3, SIMPL-31140/AC3, SIMPL-31142/AC3 | Is the 502 internal (between the GAs, turned into an empty group for the Consumer) or the status the Consumer receives? |
| F-033 | cross: SIMPL-31140/AC1, SIMPL-31140/AC2, SIMPL-31142/AC1, SIMPL-31142/AC2 | What is the grouped response shape, and how does a client ask for the next page of one group? |
| F-034 | cross: SIMPL-31140/AC1, SIMPL-31142/AC1 | Does the backend merge own and federated results into one response, or does the UI combine two calls? |
| F-044 | cross: SIMPL-31148/AC1, SIMPL-31770/AC1, SIMPL-33885/AC1, SIMPL-31153/AC2, SIMPL-31150/AC4 | Is the schema part of the federated detail response, and if not, where does the UI get the Trustor's schema? |
| F-052 | cross: SIMPL-31153/AC1, SIMPL-31153/AC2, SIMPL-31153/AC3 | Where is the Trusted-side inbound operation defined, and is the Trustor-side operation the forwarding target? |
| F-054 | SIMPL-31815 | Is POST /schemas in SIMPL-31815 the catalogue-be operation, or the schema management API referred to as SIMPL-30422 / SIMPL-33537? |
| F-055 | cross: SIMPL-30850, SIMPL-31815, SIMPL-31816, SIMPL-31817 | Which component owns schema management for these four stories, and are they in scope for the catalogue build? |
| F-056 | cross: SIMPL-31815/AC3, SIMPL-31816/AC2 | Is the limit one SEARCH schema in any state, or one published SEARCH schema? May a new schema reuse a revoked schema's name? |
| F-057 | SIMPL-31816/AC1 | Does creating a federated search schema in the UI mean uploading a SHACL file, or editing properties in a form? |
| F-061 | cross: SIMPL-35874/AC2, SIMPL-31142/AC1 | In a mixed selection, is the local catalogue searched with the SEARCH schema fields too? |

## Per story

Story-level findings are listed on the row "(story)". A cross finding is listed on every criterion it names.

### SIMPL-27998

| AC | Findings | Severity |
|---|---|---|
| (story) | F-003 | major |
| AC1 | none | |
| AC2 | F-001 | major |
| AC3 | F-002 | major |

### SIMPL-29292

| AC | Findings | Severity |
|---|---|---|
| (story) | F-007, F-064 | major |
| AC1 | none | |
| AC2 | F-004 | blocker |
| AC3 | F-005 | blocker |
| AC4 | none | |
| AC5 | none | |
| AC6 (struck) | not judged | |
| AC7 (struck) | not judged | |

### SIMPL-29293

| AC | Findings | Severity |
|---|---|---|
| (story) | F-007, F-064 | major |
| AC1 | none | |
| AC2 | F-004 | blocker |
| AC3 | F-006 | major |
| AC4 | none | |
| AC5 | F-008 | minor |
| AC6 (struck) | not judged | |
| AC7 (struck) | not judged | |

### SIMPL-30848

| AC | Findings | Severity |
|---|---|---|
| (story) | F-009 | major |
| AC1 | F-010 | major |

### SIMPL-30850

| AC | Findings | Severity |
|---|---|---|
| (story) | F-014, F-015, F-055 | blocker |
| AC1 | F-011, F-012, F-059 | blocker |
| AC2 | F-011, F-012, F-059 | blocker |
| AC3 | F-011, F-013 | blocker |
| AC4 | F-012 | blocker |
| AC5 | F-013 | blocker |

### SIMPL-31102

| AC | Findings | Severity |
|---|---|---|
| (story) | F-021 | minor |
| AC1 | F-018, F-019, F-020, F-059 | major |
| AC2 | F-018 | major |

### SIMPL-31103

| AC | Findings | Severity |
|---|---|---|
| (story) | F-021 | minor |
| AC1 | F-018, F-019, F-020, F-059 | major |
| AC2 | F-018 | major |

### SIMPL-31106

| AC | Findings | Severity |
|---|---|---|
| (story) | F-025 | minor |
| AC1 | F-022 | blocker |

### SIMPL-31111

| AC | Findings | Severity |
|---|---|---|
| (story) | F-029 | major |
| AC1 | F-026, F-028 | blocker |

### SIMPL-31112

| AC | Findings | Severity |
|---|---|---|
| (story) | F-029 | major |
| AC1 | F-027 | major |

### SIMPL-31113

| AC | Findings | Severity |
|---|---|---|
| (story) | F-032 | major |
| AC1 | none | |
| AC2 | F-023, F-030 | blocker |
| AC3 | F-030, F-031 | blocker |

### SIMPL-31114

| AC | Findings | Severity |
|---|---|---|
| (story) | F-024 | major |
| AC1 | F-064 | minor |

### SIMPL-31138

| AC | Findings | Severity |
|---|---|---|
| (story) | F-024 | major |
| AC1 | F-022, F-026 | blocker |
| AC2 | F-022, F-023 | blocker |
| AC3 | F-022 | blocker |

### SIMPL-31139

| AC | Findings | Severity |
|---|---|---|
| (story) | F-024 | major |
| AC1 | F-022, F-027 | blocker |
| AC2 | F-022, F-023 | blocker |
| AC3 | F-022 | blocker |

### SIMPL-31140

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-033, F-034, F-035 | blocker |
| AC2 | F-033 | blocker |
| AC3 | F-030, F-036 | blocker |
| AC4 | F-037 | major |

### SIMPL-31141

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-028, F-039, F-040 | major |

### SIMPL-31142

| AC | Findings | Severity |
|---|---|---|
| (story) | F-038 | major |
| AC1 | F-033, F-034, F-061 | blocker |
| AC2 | F-033 | blocker |
| AC3 | F-030 | blocker |
| AC4 | none | |

### SIMPL-31143

| AC | Findings | Severity |
|---|---|---|
| (story) | F-042 | major |
| AC1 | F-041 | major |

### SIMPL-31148

| AC | Findings | Severity |
|---|---|---|
| (story) | F-045 | minor |
| AC1 | F-044 | blocker |
| AC2 | none | |
| AC3 | none | |

### SIMPL-31149

| AC | Findings | Severity |
|---|---|---|
| (story) | F-063 | minor |

### SIMPL-31150

| AC | Findings | Severity |
|---|---|---|
| (story) | F-050, F-051 | major |
| AC1 | none | |
| AC2 | F-049 | major |
| AC3 | F-049 | major |
| AC4 | F-044 | blocker |

### SIMPL-31151

| AC | Findings | Severity |
|---|---|---|
| (story) | F-063 | minor |

### SIMPL-31152

| AC | Findings | Severity |
|---|---|---|
| (story) | F-063 | minor |

### SIMPL-31153

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-052 | blocker |
| AC2 | F-044, F-052 | blocker |
| AC3 | F-052, F-053 | blocker |

### SIMPL-31767

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-028, F-039, F-040 | major |

### SIMPL-31768

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-039 | major |

### SIMPL-31769

| AC | Findings | Severity |
|---|---|---|
| (story) | F-042, F-043 | major |
| AC1 | F-041 | major |

### SIMPL-31770

| AC | Findings | Severity |
|---|---|---|
| (story) | F-048 | minor |
| AC1 | F-044, F-047 | blocker |
| AC4 | F-046 | major |
| AC5 | F-047 | major |

### SIMPL-31815

| AC | Findings | Severity |
|---|---|---|
| (story) | F-017, F-054, F-055 | blocker |
| AC1 | F-059 | major |
| AC2 | none | |
| AC3 | F-056 | blocker |

### SIMPL-31816

| AC | Findings | Severity |
|---|---|---|
| (story) | F-055, F-058 | blocker |
| AC1 | F-057, F-059 | blocker |
| AC2 | F-056 | blocker |

### SIMPL-31817

| AC | Findings | Severity |
|---|---|---|
| (story) | F-017, F-055 | blocker |
| AC1 | F-011, F-013 | blocker |
| AC2 | F-011 | blocker |
| AC3 | F-011, F-013, F-016 | blocker |

### SIMPL-33885

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-044, F-047 | blocker |
| AC2 | F-047 | major |
| AC3 | none | |
| AC4 | F-046 | major |

### SIMPL-35874

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-019 | major |
| AC2 | F-061, F-062 | blocker |
| AC3 | F-060 | major |

## Cross-story consistency (AC-C04)

| Id | Severity | Criteria | Finding |
|---|---|---|---|
| F-008 | minor | SIMPL-29293/AC5 | The advanced-search story says "Quick Search behavior remains unchanged", a copy from the quick-search twin. |
| F-012 | blocker | SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-30850/AC4 | AC1 says schemaType may be left empty and defaults to RESOURCE; AC2 lists schemaType as a required field; AC4 names only the changelog and the file as validated fields. The UI cannot both require the field and default it. |
| F-023 | major | SIMPL-31138/AC2, SIMPL-31139/AC2, SIMPL-31113/AC2 | The Trustor rejects a non-trusted caller with 403, while the Trusted side expects a failed mTLS handshake (502) for the same broken trust. A failed handshake never reaches HTTP, so a 403 can only happen for a caller with a valid certificate. The two tests need different set-ups. |
| F-030 | blocker | SIMPL-31113/AC2, SIMPL-31113/AC3, SIMPL-31140/AC3, SIMPL-31142/AC3 | For a Trustor that no longer trusts, SIMPL-31113 rejects the call with 502, while SIMPL-31140 and SIMPL-31142 return an empty group for that catalogue and leave the others unaffected. A builder cannot tell whether the Consumer gets 502 or 200 with an empty group. |
| F-034 | blocker | SIMPL-31140/AC1, SIMPL-31142/AC1 | SIMPL-31140/AC1 returns own and federated results "in a single response", while the SIMPL-31142 technical design has the UI call the local search API and the federated search API separately and build the tabs itself. These are two different builds. |
| F-044 | blocker | SIMPL-31148/AC1, SIMPL-31770/AC1, SIMPL-33885/AC1, SIMPL-31153/AC2, SIMPL-31150/AC4 | Three stories require the schema to come back with a federated detail, while the Trustor and Trusted stories have the schema struck and the adapter contract says it returns no separate schema. The UI cannot render the detail without knowing where the schema comes from. |
| F-047 | major | SIMPL-33885/AC1, SIMPL-33885/AC2, SIMPL-31770/AC1, SIMPL-31770/AC5 | SIMPL-33885 adds a "dataspace/catalogue identifier" (one value or two?), while SIMPL-31770 says the API accepts "the resource identifier returned by the federated search" and keeps the own-catalogue path unchanged. The contract makes catalogueId required for every call, own catalogue included. |
| F-048 | minor | SIMPL-31770 | The ids jump from AC1 to AC4; AC2 and AC3 do not exist. Harmless for the build, but tickets may refer to missing ids. |
| F-053 | major | SIMPL-31153/AC3 | AC3 forwards the Trustor's error to the Consumer, while the technical description asks to translate errors without exposing federation details. These give different responses for the same error. |
| F-056 | blocker | SIMPL-31815/AC3, SIMPL-31816/AC2 | The uniqueness rule differs: SIMPL-31815 rejects a second SEARCH schema only when another is published, SIMPL-31816 when one "already exists", and the technical notes say "only one". The hints add open questions on revoked schemas and name reuse. |
| F-059 | major | SIMPL-30850/AC1, SIMPL-30850/AC2, SIMPL-31815/AC1, SIMPL-31816/AC1, SIMPL-31102/AC1, SIMPL-31103/AC1 | One permission has four names: a role "authorising schema management", governance-admin, GA_SCHEMA_ADMIN, and for search a role "authorising searching" against the API's ABAC roles. A tester cannot set up the user. |
| F-061 | blocker | SIMPL-35874/AC2, SIMPL-31142/AC1 | With local and federated catalogues selected together, SIMPL-35874 shows only the SEARCH schema fields, while the SIMPL-31142 design searches the local catalogue with the filters of the selected resource schema. The local search in a mixed selection is built differently under each. |
| F-064 | minor | SIMPL-29292, SIMPL-29293, SIMPL-31114/AC1 | The searching actor has several names: Provider (title), Participant (user story), Consumer (other search stories), Customer (SIMPL-31114). |

## 20a triage

| Code | Hits | Real | Harmless | Example of a harmless hit |
|---|---|---|---|---|
| AC-S01 | 7 | 0 | 7 | SIMPL-31111/AC1: the context is in the When of a linkage-only story |
| AC-S04 | 23 | 0 | 23 | SIMPL-31815/AC1: "And no resourceType specified" is a condition, not a second action |
| AC-S08 | 3 | 3 | 0 | none: all three are real (F-063) |
| AC-S09 | 23 | 0 | 23 | SIMPL-31816/AC1: the id is stable while the input set is frozen |
| AC-S10 | 31 | 12 | 19 | SIMPL-27998/AC2: the "Or ..." line is a clause in the source, a parser misread |
| AC-W01 | 12 | 8 | 4 | SIMPL-29292/AC3: "Sufficient" is a classification label |
| AC-W03 | 6 | 1 | 5 | SIMPL-30850/AC1: "should be redirected" is a UI step meant as "shall" |
| AC-W05 | 19 | 3 | 16 | SIMPL-29292/AC1: "minimum score" is a field name, not a limit |
| **Total** | 124 | 27 | 97 | |

Real hits by criterion:

- SIMPL-27998/AC2 AC-W05: no status code
- SIMPL-29292/AC2 AC-W05: score scale open
- SIMPL-29292/AC3 AC-S10: note defines the ordering; part of the profile finding
- SIMPL-29293/AC2 AC-W05: score scale open
- SIMPL-29293/AC3 AC-S10: same
- SIMPL-30848/AC1 AC-S10: note moves scope to another component
- SIMPL-30850/AC2 AC-W03: part of the required-or-default schemaType contradiction
- SIMPL-30850/AC4 AC-W01: message text open
- SIMPL-30850/AC5 AC-S10: technical details carry the "only one" rule
- SIMPL-30850/AC5 AC-W01: message text open
- SIMPL-31102/AC2 AC-W01: message text open
- SIMPL-31103/AC2 AC-W01: message text open
- SIMPL-31140/AC1 AC-W01: "if relevant" is open
- SIMPL-31142/AC1 AC-W01: same
- SIMPL-31148/AC2 AC-W01: message text open
- SIMPL-31148/AC3 AC-S10: technical design carries the schema source
- SIMPL-31148/AC3 AC-W01: message text open
- SIMPL-31149 AC-S08: part of F (linkage-only stories)
- SIMPL-31150/AC4 AC-S10: technical description conflicts with the adapter contract
- SIMPL-31151 AC-S08: part of F (linkage-only stories)
- SIMPL-31152 AC-S08: part of F (linkage-only stories)
- SIMPL-31153/AC3 AC-S10: technical description conflicts with AC3
- SIMPL-31769/AC1 AC-S10: note links to federated stories
- SIMPL-31770/AC5 AC-S10: technical description asks for statuses the contract lacks
- SIMPL-31815/AC3 AC-S10: hints carry open uniqueness questions
- SIMPL-31816/AC2 AC-S10: technical details carry the "only one" rule
- SIMPL-31817/AC3 AC-S10: missed And clause and details feed the precedence finding

## Coverage

Most findings: SIMPL-30850 (7), SIMPL-31140 (6), SIMPL-31817 (5), SIMPL-31816 (5), SIMPL-31815 (5), SIMPL-31142 (5).

Fewest findings: SIMPL-31149 (1), SIMPL-31151 (1), SIMPL-31152 (1), SIMPL-31768 (1), SIMPL-30848 (2), SIMPL-31106 (2).

Reminders from the links (see `inventory-review.md` for the full lists, not repeated here):

- 25 operations have no criterion, among them the whole resource-description lifecycle in catalogue-be (`POST /self-descriptions`, `.../revoke`, `.../versions`, `DELETE /self-descriptions/{self_description_hash}`), most schema operations, and the four participant operations of the adapter.
- 28 BP05b requirements have no criterion: publishing, versioning and revoking resource descriptions (BP5B.1 to BP5B.9) are only covered by SIMPL-27998 for the quality gate.
- Two BP06 points have a criterion link but no criterion text: searching all federated catalogues without selecting them (BP6.1.2, see F-020) and sorting the overview (BP6.7.1, see F-040).
- Operations named in the stories but absent from the entry files: `/schemas/{schemaName}/versions` (F-011), the Trustor-side search (F-022), the Trusted-side detail forwarding (F-052), the catalogue list for selection (F-018).
