# Business process review (step 22b): schema-manager

Review of BP02C "Manage resource description schemas" as a flow, against the inventory, the 22a coverage checks, the findings of 20b (F-), 21b (P-) and 23b (D-), and the step 30 decisions (D1 to D48, glossary "Schema lifecycle"). Findings are in `bp-findings.json` (B-001 to B-014). Nothing here repeats an earlier finding; related ids are under `see`.

The process is in good shape for coverage: every step has criteria and every step reaches an operation (22a: BP-01 0, BP-02 0, BP-04 0). The step 30 decisions settle almost every state transition. What remains is mostly the process text lagging behind those decisions, and the hand-overs to parties outside this component.

## Summary

| Severity | Count |
|---|---|
| blocker | 1 |
| major | 8 |
| minor | 5 |
| **total** | **14** |

The five findings with most impact:

1. **B-001 (blocker, FLOW).** BP02C.05 publishes every valid schema; the decided lifecycle (D7, D8, D12, D13) also stores a new schema as REVOKED, refuses a second published SEARCH schema with 409, and re-publishes a revoked schema by a new version. No decision says the decisions win over the process text.
2. **B-002 (major, MSV-03, OWA-API6).** The Governance Authority's write requests wait for every webhook subscriber (D24, 5 seconds each), and any authenticated participant may add any number of subscriptions (D28).
3. **B-003 (major, MSV-08).** Events and e-mails are sent at most once, with no retry and no outbox. A missed SchemaRevoked leaves a subscriber or Provider using a revoked schema, with no way to catch up.
4. **B-006 (major, FLOW).** The process outcome "available for Providers (BP 5B)" has no read path: every read needs a Governance Authority role (D27) and the service has no ingress.
5. **B-007 (major, FLOW).** Replacing a published schema by another schema forces revoke, then re-publish, with a gap where the scope has no published schema. For SEARCH that gap stops federated search.

## Blockers

### B-001: publication step contradicts the decided lifecycle

- **Evidence.** BP02C.05: "Upon successful validation, the new schema or new version ... is published". Overview: "Each resource type ... has one schema". D7: a new RESOURCE schema is stored as REVOKED when its type already has a published one (201, no event, no e-mail). D8 and D12: SEARCH conflicts give 409 after validation passed. D12: a new version re-publishes a REVOKED schema. SIMPL-20686/AC2 and SIMPL-32801/AC2 link to BP02C.01 but no step describes their outcome. D1 lets decisions win over stories and D44 over the OpenAPI; nothing covers BP02C.
- **Question for the process owner.** Is the D13 lifecycle the process? If so, align BP02C.03, .04 (see D19), .05 and the Overview. For the build: may step 30 add a decision that the decisions win over BP02C text wherever they differ?

## Flow table

Party names follow the decided glossary. "GA admin" is a Governance Authority user with GA_SCHEMA_ADMIN (D27).

| Step | Party | Input | Output | Hand-over | Failure outcome | State before, after | Findings |
|---|---|---|---|---|---|---|---|
| Trigger | GA admin | decision to create, version or revoke | one of .01, .02, .03 | none (human) | not applicable | any | B-011 |
| (Assumption) Providers propose changes | Provider, GA | proposal | manual review | outside the system, no channel named | not defined, manual | none | none (out of system) |
| (2C.1, no step) Retrieve schemas and versions | GA admin or viewer | GET /schemas, GET versions, GET content (D16, D17, D30) | lists, Turtle | synchronous API | 401, 403, 404 (D26, D47, D48) | no change | B-010 |
| BP02C.01 Create a new schema | GA admin | multipart CreateSchemaPayload (D15) | request to .04 | synchronous POST /schemas | 400 fields, 409 key (D4), 415; order against validation open | (none) | B-001, B-008, B-009 |
| BP02C.02 Create a new version | GA admin | schema selected via 2C.1; multipart new-version fields (D14) | request to .04 | synchronous POST /schemas/{schemaName}/versions | 400, 404 no schema (D14), 409 SEARCH (D12) | PUBLISHED or REVOKED | B-001, B-009, B-014 |
| BP02C.03 Revoke a schema | GA admin | PATCH {"status":"REVOKED"} (D9) | schema REVOKED, retained (D18) | synchronous PATCH, then event and e-mail | 400 other status, 404, idempotent 200 (D10) | PUBLISHED to REVOKED; REVOKED unchanged | B-007, B-011 |
| BP02C.04 Validate the schema | Simpl-Open (Schema Manager) | uploaded Turtle | pass, or issues | inside the same request; calls the vocabulary registry (D23, D42) | 400, nothing stored (D22); Vocabulary Manager down: not defined | no change on failure | B-004, B-009 |
| BP02C.05 Publish the schema | Simpl-Open | validated schema | stored version; status per D7, D8, D12; previous version shows REVOKED (D11) | 201 to GA; then webhook POSTs before the answer (D24) | store failure 500, no change (D37); 409 SEARCH | (none) or REVOKED to PUBLISHED; or stored REVOKED | B-001, B-002, B-007, B-008 |
| (no step) Schema events to subscribers | Schema Manager to subscribing participant | stored change | SchemaPublished or SchemaRevoked POST (D24) | webhook, synchronous inside the GA request | one attempt, logged, lost on crash | no change | B-002, B-003, B-005 |
| BP02C.06 Inform Providers | Schema Manager to Providers | stored change, RESOURCE only | one e-mail to NOTIFICATION_RECIPIENT (D25) | SMTP after store | logged, not retried | no change | B-003, B-013, B-014 |
| BP02C.07 Send report | Schema Manager to GA admin | validation issues | 400 problem with issues; warnings in 201 (D22, D23) | the API answer itself | not applicable | no change | B-012 |
| Outcome: available for Providers (BP 5B) | Provider or catalogue | published schema | resource descriptions built on it | not defined: reads need GA roles (D27), no ingress | not defined | not applicable | B-006 |

States: the decided lifecycle (glossary "Schema lifecycle", D13) has transitions that no step reaches: create to REVOKED (D7), REVOKED to PUBLISHED by a new version (D12), and the SEARCH 409 after valid input (D8, D12). All are in B-001. No step reaches a state the decisions do not have.

Parties: the process names only the Governance Authority as actor. The flow also has Providers (receivers of .06), subscribing participants (webhooks, SIMPL-19992, SIMPL-24175, D28) and the Vocabulary Manager (dependency of .04). The roles in the stories and the API match the process for every step the process has (GA_SCHEMA_ADMIN for writes, D27).

## 22a triage

| 22a check | Count | Out of scope | Covered under another name | Real gap | Finding |
|---|---|---|---|---|---|
| BP-01 step or requirement with no criterion | 0 | 0 | 0 | 0 | none |
| BP-02 covered, no operation | 0 | 0 | 0 | 0 | none |
| BP-03 story with no process link | 18 | 1 | 15 | 2 | B-010, B-005 |
| BP-04 unknown process id | 0 | 0 | 0 | 0 | none |

- **Covered under another name (15):** BA-001 to BA-007, BA-009, BA-012 to BA-018. Retrieval stories; they belong to requirement BP2C.1 and detailed requirement BP2C.1.1, which have no step heading. Grouped in B-010 (minor).
- **Real gap (2):** BA-008 (SIMPL-19992, webhook subscriptions) and BA-010 (SIMPL-24175, Tier 2 access to subscriptions). The process has no step or actor for schema events. B-005 (major).
- **Out of scope (1):** BA-011 (SIMPL-28815, billing contract clause in the resource description wizard). Another component (D2, already F-044). No finding.
- **Requirement level.** The eight requirements and twelve detailed requirements show 0 criteria because links use step ids; they inherit the criteria of their steps. Only 2C.1 has no step behind it (B-010).
- **Withdrawn criteria in coverage.** Six of the 20 links to BP02C.05 come from criteria withdrawn by D9 (SIMPL-20529/AC1, AC8, SIMPL-20530/AC2 to AC4, SIMPL-31098/AC2). BP02C.05 stays covered by 14 other links (SIMPL-13357, SIMPL-17066, SIMPL-20686, SIMPL-31098/AC1, SIMPL-32801, SIMPL-32807, SIMPL-33537).

## Rules checked

| Rule | Result | Findings |
|---|---|---|
| MSV-03 events for state changes | Events exist for publish and revoke (D24), but the publisher waits for every consumer | B-002 |
| MSV-06 dependency slow or down | Defined for webhooks and e-mail (D24, D25); not for the Vocabulary Manager | B-004 |
| MSV-08 sagas and compensation | Single-service writes are atomic (D22, D37). The hand-over to subscribers and Providers is at most once with no recovery | B-003 |
| OWT-A06 abuse cases | Revoking the last published schema of a scope has no check beyond a UI popup | B-007 |
| OWT-A10 exceptional conditions | Validation and store failures leave no change (D22, D37). Concurrent writes and check order are open | B-008, B-009 |
| OWA-API6 sensitive business flows | No limit on subscriptions (cost for the GA request) or on Provider notifications | B-002, B-014 |
