# Business process review (step 22b): vocabulary-manager

Process: BP02B - Manage vocabularies (`specs/functional/bp02b-manage-vocabularies.md`). Read with the 22a coverage (`bp-checks.json`), the findings of 20b (F-), 21b (P-) and 23b (D-), and the decisions of step 30 (`decisions/decisions.json`, `decisions/glossary.md`). Findings are in `bp-findings.json`, ids B-001 to B-009.

## Summary

| Severity | Findings |
|---|---|
| blocker | 0 |
| major | 6 |
| minor | 3 |
| total | 9 |

The process is short and the decisions of step 30 already settle its main path: create or new version, automatic validation, automatic publication as ACTIVE with the older ACTIVE versions deprecated in the same transaction, or a validation report and nothing stored (D4, D6, D8, D14). All of this runs in one service with one store, so no cross-service saga is needed (MSV-08 met by design). No main-path step has an open order, party or hand-over that would make two builders build different flows, so there is no blocker. What is open sits around the main path: steps the process does not have, concurrent and dependency failures, the hand-over to consumers, and a limit on a flow that costs other parties.

The 5 findings with most impact:

1. **B-003 (major, OWT-A10).** Two version creations for the same vocabulary at the same time have no defined outcome: same number, two ACTIVE versions, or a refusal.
2. **B-001 (major, FLOW).** External vocabulary registration and removal have no process step, while the outcome of validation depends on registering first, and removal does not check who still references the vocabulary.
3. **B-006 (major, OWA-API6).** A new version deprecates every older ACTIVE version at once, with no limit, no identical-version check and no confirmation.
4. **B-005 (major, MSV-03).** It is not said how Schema Manager and the providers learn that a version was published or deprecated (pull at use time, or an event).
5. **B-002 (major, FLOW).** Reactivation of vocabularies and versions, and manual deprecation of one version, are decided states that no process step reaches.

## Blockers

None. The questions for the process owner are in the major findings:

| Id | Question |
|---|---|
| B-001 | Should BP02B get steps to register and remove external vocabularies, before BP02B.01 and BP02B.04, and may a referenced external vocabulary be removed? |
| B-002 | Is reactivating a vocabulary or a version part of BP02B, and may a single version be deprecated or reactivated from the UI? |
| B-003 | Are overlapping changes to one vocabulary serialised, or does the second request fail and store nothing? |
| B-004 | Which timeout, retry and status apply when the store or the identity provider is slow or down, and does token validation fail closed? |
| B-005 | Do consumers read at use time only, or does Vocabulary Manager publish events on publication and deprecation? Who tells providers? |
| B-006 | Should version creation have a limit, an identical-version check and a UI confirmation? |

## Flow table

Party "GA admin" is a Governance Authority user with GA_VOCABULARY_ADMIN, "GA viewer" one with either role (D16). "System" is Vocabulary Manager. States: V = vocabulary status, v = version status (glossary state tables).

| Step | Party | Input | Output | Hand-over | Failure outcome | State before, after | Findings |
|---|---|---|---|---|---|---|---|
| Trigger vocabulary management | GA admin (off-line, possibly from a provider email) | business need or provider proposal | decision to create or change | off-line (email, Assumptions) | not in the system | none | B-008, B-009 |
| (no step) Register external vocabulary | GA admin | Turtle file, name, namespace, aliases | external vocabulary (cached copy) | POST /external-vocabularies (D30, D41) | 400 syntax, 409 duplicate, 413, 415; nothing stored | none, registered (no states) | B-001 |
| BP02B.01 Create new vocabulary (2B.2) | GA admin | name, description, Turtle file | request to the system | UI form or POST /vocabularies, multipart (D15, D37) | 401, 403 before body checks (D17); 400, 409, 413, 415; nothing stored (D8) | none | B-003, B-004 |
| BP02B.02 Validate vocabulary (2B.4.1 to 2B.4.3) | System (manual review off-line before upload, D6) | uploaded content | pass, with or without warnings; or errors | inside the same request; UI pre-check of syntax in the browser (D13) | 400 VocabularyValidationProblem, nothing stored (D8, D9, D10) | none | B-007 |
| BP02B.03 Publish vocabulary (2B.5.1, 2B.5.2) | System, automatic (D6) | validated content | stored version n, ACTIVE; 201 with validationWarnings | 201 response, Location header (D26, D31); consumers pull via search and content (no event) | store failure: 500, nothing stored (D8); timeout and retry open | V: none to ACTIVE (new vocabulary only); v: none to ACTIVE | B-003, B-004, B-005, B-007 |
| BP02B.04 Create new version (2B.3) | GA admin | latest version pre-filled, changelog, file (D33) | request to the system, then BP02B.02 and BP02B.03 | UI form or POST .../versions | 404 unknown name; 400 on a version field (D7); as BP02B.01 | V unchanged, also when DEPRECATED (D3) | B-003, B-006, B-007 |
| BP02B.05 scenario 1, automatic deprecation (2B.5.3) | System, same transaction as BP02B.03 (D4) | new version stored | every older ACTIVE version DEPRECATED | inside the request; consumers not told | rolled back with the creation (D8) | v: ACTIVE to DEPRECATED (older versions) | B-005, B-006 |
| BP02B.05 scenario 2, full vocabulary deprecation (2B.6) | GA admin | status DEPRECATED | V DEPRECATED, versions unchanged, effectively deprecated (D3) | UI status action or PATCH /vocabularies/{vocabularyName} | 404, 400, 409 same status (D4, D24); no change | V: ACTIVE to DEPRECATED | B-005, B-007 |
| BP02B.05 single version deprecation | GA admin, API only (D4) | status DEPRECATED | v DEPRECATED | PATCH .../versions/{version} | 404, 400, 409; no change | v: ACTIVE to DEPRECATED | B-002 |
| (no step) Reactivate vocabulary or version | GA admin | status ACTIVE | V or v ACTIVE; two ACTIVE versions possible | PATCH, vocabulary in UI and API, version in API only | 404, 400, 409; no change | V or v: DEPRECATED to ACTIVE | B-002 |
| BP02B.06 Send validation notification (2B.4.4) | System | validation errors | validation report (transient) | synchronous 400 response; UI report panel (D14) | not applicable | none (nothing stored) | B-007 |
| (no step) Consult vocabularies (2B.1) | GA viewer | filters, sort, paging | list, details, history, content | GET operations and read-only searches (D16, D22, D35) | 400, 404 | none | B-008 |

No decided state is skipped by a step. The states that no step reaches are the DEPRECATED to ACTIVE transitions of both levels (B-002). An open point already raised in D3: a new version of a DEPRECATED vocabulary is created ACTIVE but is effectively deprecated from the start.

## Rules checked

| Rule | Result | Findings |
|---|---|---|
| MSV-03 events for state changes | not met for the publication and deprecation that consumers depend on; open whether pull is enough | B-005 |
| MSV-06 dependencies | not met: store and identity provider have no timeout, retry or caller outcome | B-004 |
| MSV-08 sagas and compensation | met by design: one service, one store, creation and deprecation in one transaction (D4, D8) | none |
| OWT-A06 abuse cases | not met: no abuse case written for version creation | B-006 |
| OWT-A10 exceptional conditions | partly met: failed requests leave nothing (D8); concurrent changes undefined | B-003 |
| OWA-API6 sensitive business flows | not met: no limit or check on version creation that deprecates versions in use | B-006 |

## 22a triage in numbers

| Check | 22a findings | Out of scope | Covered under another name | Cross-cutting or outside the process | Real gap |
|---|---|---|---|---|---|
| BP-01 | 0 | 0 | 0 | 0 | 0 |
| BP-02 | 0 | 0 | 0 | 0 | 0 |
| BP-03 | 3 | 1 | 0 | 2 | 0 |
| BP-04 | 0 | 0 | 0 | 0 | 0 |

- **BA-001 (SIMPL-18195) and BA-002 (SIMPL-18196):** role-based access in the UI and the API. Cross-cutting: it applies to every step, as settled in D16, D17 and D18. Not a process gap.
- **BA-003 (SIMPL-28680):** the Metadata Quality Profile update belongs to Schema Manager and is outside this build (D1, F-010). Not a process gap.
- **Steps 0 of 6 covered:** the links use the requirement level, and every step maps to a covered requirement (mapping in B-008). Covered under the requirement names, so not counted as a BP-01 gap.

The coverage gaps this step found are not visible to 22a, because they are operations and transitions without a process step rather than steps without criteria: external vocabulary registration (B-001) and reactivation (B-002).
