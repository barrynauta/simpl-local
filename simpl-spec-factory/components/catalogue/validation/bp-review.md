# Business process review (step 22b): catalogue

Processes: BP05B "Provider manages resource descriptions" and BP06 "Consumer searches resources". Inputs: the process files, the inventory and links, 22a (`bp-checks.json`), the findings of 20b (F-), 21b (P-) and 23b (D-), and the step 30 decisions (D1 to D42, E1 to E4). Findings are in [`bp-findings.json`](./bp-findings.json).

Scope note: D4 builds only part of the catalogue-be lifecycle (publication with quality gate, detail, QAR, revoke, versions, the two searches). Authoring, signing and the Provider's overview are not built in the catalogue. Gaps that D4 or another decision explains are triaged below as out of scope or covered, with the decision id. They are not findings.

## Summary

| Severity | Count |
|---|---|
| blocker | 1 |
| major | 9 |
| minor | 7 |
| **total** | **17** |

The 5 findings with most impact:

1. **B-001 (blocker)**: informing Consumers about a new version or a revocation has no party, no trigger and no receiver. Requirement titles say the Provider, 5B.8 says the Governance Authority. No decision defines an event.
2. **B-006 (major)**: semantic validation against the schema shapes is a named main-path check (BP05B.05), but no criterion or decision defines it.
3. **B-005 (major)**: publication touches the connector (asset) and the wallet (signed copy) before the catalogue can refuse. Nothing undoes or reuses that work.
4. **B-009 (major)**: the failure of the own catalogue during a search is undefined. Only a failing Trustor is decided (D25).
5. **B-010 (major)**: federated search fans out to other data spaces with no limit on catalogues per request or on request rate.

The process and the decisions agree well on BP06: the federated hand-overs (BP06.05 to BP06.10) map to D24, D25, D27, D28 and D34, and the Trustor failure path is fully decided. Most of the BP05B gaps come from the missing stories for revoke and versions (P-012) and from steps that other components perform.

## Blockers

### B-001: who informs Consumers, and on what signal

BP05B.12 and BP05B.15 are passive. 5B.6 and 5B.9 name the Provider as sender. The 5B.8 description names the Governance Authority. D18 and D35 define the state changes as synchronous calls with no event. The catalogue does not know which Consumers hold a contract (BP07).

**Question for the process owner:** which party informs the Consumers, what signal starts it (an event from catalogue-be after a new version and after a revocation, or an action elsewhere), and who resolves the Consumers with a contract? If both steps are outside the catalogue build, a step 30 addendum should say so.

## Flow table

States follow D18: none, published (stored `active`), revoked. "n/a" means the step does not change a resource description.

### BP05B

| Step | Party | Input | Output | Hand-over | Failure outcome | State before, after | Findings |
|---|---|---|---|---|---|---|---|
| BP05B.01 Create | Provider (SD tooling) | resource type, common metadata | draft | none (outside the catalogue, D4, D1 for schemas) | not in scope | none, none | |
| BP05B.02 Type metadata | Provider (SD tooling) | type-specific metadata | draft | none | not in scope | none, none | |
| BP05B.03 Policy and contract metadata | Provider (SD tooling) | policies, contract clauses | draft | none | not in scope | none, none | |
| BP05B.04 Sign and submit | Provider | signed credential | submission | POST /self-descriptions/resource with publisher token (D35, D37); dynamic view also registers a connector asset and stores a wallet copy first | refusal leaves connector asset and wallet copy undefined | none, none | B-005, B-011 |
| BP05B.05 Validate | Governance Authority (catalogue service) | submission | pass or refusal | in-process, then synchronous call to the Quality Scoring Service (D17) | 400, 403, 409 (D35), 422, 500 (D17); semantic validation undefined | none, none | B-006, B-007, B-004 |
| BP05B.06 Remediate | Provider | refusal problem body | resubmission | synchronous error response (D17, D36) | unlimited loop | none, none | B-014 |
| BP05B.07 Publish | Governance Authority (catalogue service) | validated submission | published version, QAR | 201 with Location (D35) | nothing stored on refusal (D17) | none, published | B-004, B-012 |
| BP05B.08 New version | Provider (SD tooling) | published description | draft of version n+1 | none | not in scope | published, published | B-014 |
| BP05B.09 Update type metadata | Provider (SD tooling) | metadata | draft | none | not in scope | published, published | |
| BP05B.10 Update policies | Provider (SD tooling) | policies, clauses | draft, then BP05B.04 | none | not in scope | published, published | |
| BP05B.11 Revoke previous version | Governance Authority (catalogue service) | successful publication of n+1 | version n revoked | same call as BP05B.07 (D18) | atomicity with BP05B.07 not stated; failed n+1 keeps n published (D35) | published (n), revoked (n) | B-002, B-012 |
| BP05B.12 Inform about new version | open (Provider or Governance Authority) | new version | notice to Consumers | none defined | none defined | n/a | B-001, B-002 |
| BP05B.13 Request revocation | Provider | published description | revoke request | POST .../revoke (D37) | 401, 403, 404; revoked again is 200 (D18) | published, published | B-003 |
| BP05B.14 Revoke | Governance Authority (catalogue service, implicit) | request | revoked description | same call, immediate (D18) | as BP05B.13 | published, revoked | B-003 |
| BP05B.15 Inform about revocation | open | revocation | notice to Consumers | none defined | none defined | n/a | B-001, B-002 |
| 5B.1 Consult own descriptions | Provider | id | detail, versions | GET detail and versions (D40); overview not built (D4) | 404 also when own policy not met | n/a | B-013 |

### BP06

| Step | Party | Input | Output | Hand-over | Failure outcome | State before, after | Findings |
|---|---|---|---|---|---|---|---|
| BP06.01 Search | Consumer (search UI) | type, scope (catalogueIds), terms, quality filter | search request | catalogue-client-be GET /selfDescriptions or POST /selfDescriptions/advanced (D33) | 400, 401, 403 (D33, D37); invalid filter 400 (D14, D16) | n/a | B-016, B-017 |
| BP06.02 Policy filter | Governance Authority (adapter, catalogue-be) | User-Attributes from token claim | filtered query | header User-Attributes (D34) | missing header 400; empty means no attributes, fails closed (D21, D34) | n/a | |
| BP06.03 Execute query | Governance Authority (catalogue-be) | query | own results | adapter tier 2 to catalogue-be (D33) | own-catalogue failure undefined | n/a | B-009 |
| BP06.04 Quality rating, consult results | Consumer | results | displayed results | UI | as BP06.11 | n/a | B-008 |
| BP06.05 Forward to Trustors | Trusted Governance Authority (adapter) | selected Trustor catalogues | forwarded requests | HTTP to Trustor GET /federated/resourceDescriptions or POST .../advancedSearch with User-Attributes, Correlation-Id (D28, D34) | Trustor failure gives group unavailable, 10 s timeout (D25) | n/a | B-010 |
| BP06.06 Handle and map | Trustor Governance Authority (gateway) | forwarded request | mapped attributes | gateway in front of the Trustor adapter (D27) | unmapped attribute counts as absent (D20); refusal reaches the Trusted side as failed call (D25) | n/a | |
| BP06.07 Policy filter with mapped attributes | Trustor Governance Authority (adapter) | mapped User-Attributes | filtered query | in-process (D21) | missing header 400 (D28) | n/a | |
| BP06.08 Execute query for trusted Consumer | Trustor Governance Authority | query | Trustor results, quality null | to its catalogue-be (D28) | 500 (D28) | n/a | B-010 |
| BP06.09 Forward results | Trustor Governance Authority | results | response | synchronous HTTP response (D28) | as BP06.05 | n/a | |
| BP06.10 Receive results | Trusted Governance Authority | response | group | synchronous | as BP06.05 | n/a | |
| BP06.11 Collect and provide | Governance Authority (adapter, client backend) | groups | grouped response {groups} | 200 to catalogue-client-be and UI (D24) | Trustor groups decided (D25); own group undefined | n/a | B-008, B-009 |
| BP06.12 Forward to DOME | Governance Authority | query | DOME offerings | none: out of build per glossary (D5) | not defined | n/a | B-017 |
| (no step) Federated detail | Trusted and Trustor Governance Authority | id, catalogueId | details | adapter tier 1 to Trustor GET /federated/resourceDescriptions/{id} (D30) | 404, 400, 500 mapped (D31) | n/a | B-015 |

## Order and states

- BP05B.05 to BP05B.07 (validate, then publish) match D17 and D18. The dynamic view's publish-then-validate order does not (B-004).
- BP05B.11 matches D18 (previous version revoked on success). The single revoked state cannot carry the different follow-ups of BP05B.12 and BP05B.15 (B-002).
- BP05B.13 and BP05B.14 are one call in the decisions (B-003).
- Every decided state is reached by a step: published by BP05B.07, revoked by BP05B.11 and BP05B.14. The decided 409 for republishing a revoked description (D35) has no process step, which is consistent: the process has no "republish" path.
- BP06.04 sits before the federated steps it depends on (B-008).

## 22a triage in numbers

| 22a check | Total | Out of scope | Covered by another name or by a decision | Real gap |
|---|---|---|---|---|
| BP-01 | 28 | 12 | 12 | 4 |
| BP-02 | 4 | 0 | 4 | 0 |
| BP-03 | 5 | 4 | 1 | 0 |
| **total** | **37** | **16** | **17** | **4** |

**BP-01, out of scope (12):**
- BA-001 (5B.1.1 overview): D4, GET /self-descriptions is not built.
- BA-004 to BA-014 (5B.2.x authoring, 5B.3.x version authoring): done in SD tooling on the Provider side (dynamic view participants). Resource types come from the Schema Manager (D1, D12). D4 does not build POST /self-descriptions.

**BP-01, covered by a decision (12):**
- BA-002, BA-003 (own detail, version history): GET detail and GET versions, built by D4 and shaped by D40. There is no story (P-012). B-013 adds the owner-view point.
- BA-015 (sign and submit): the submission is POST /self-descriptions/resource (D35). Signing is the signer side. B-011 adds signature verification.
- BA-016, BA-017 (schema validation, inactive schema): D35 400 for an unpublished schema version. B-007 adds the latest-version question.
- BA-018, BA-019 (versioning validation): replaced by the server-assigned version number, the ownership check and 409 (D35).
- BA-020, BA-021 (syntax): D35 400 invalidInput for the body and required fields.
- BA-024 (revoke previous version): D18, D35.
- BA-026, BA-027 (request revocation, revoke): POST .../revoke, D4, D18, D37. B-003 adds the hand-over point.

**BP-01, real gaps (4):** BA-022 and BA-023 (semantic validation, B-006); BA-025 and BA-028 (notify Consumers, B-001).

**BP-02 (4), covered by a decision:** BA-029 (6.1.2) by D33 and D24. These are Consumer search operations missing from the v2 contract, already raised as P-001. BA-030 to BA-032 (6.3.1, 6.3.2, 6.4.1) by D27 (mapping in the gateway) and D28 (Trustor operations missing from the contract, already raised as F-022). The second bullet of 6.1.2 (all federated catalogues without selection) is out of scope by D5.

**BP-03 (5):** BA-033 to BA-036 (SIMPL-30850, SIMPL-31815, SIMPL-31816, SIMPL-31817) are schema management, out of scope by D1. They belong to BP02 configuration, not to BP05B or BP06. BA-037 (SIMPL-35874) belongs to BP06.01 and 6.1.2. Only the link is missing, and the process text lags (B-016).

Outside 22a: 6.8 to 6.10 (DOME) have no detailed requirements, so 22a does not list them. They are out of the build per the decided glossary, but no decision text says so (B-017).
