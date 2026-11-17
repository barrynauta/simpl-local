# Compare with run 1 (step 30c): catalogue

This file compares the run 1 spec digest (committed 28 Oct 2026, decisions run1 D1 to run1 D83) with this run: the findings of step 20b (`F-nnn`, [`ac-findings.json`](../validation/ac-findings.json)), step 23b (`D-nnn`, [`domain-findings.json`](../validation/domain-findings.json)) and the decisions of step 30a ([`decisions.json`](./decisions.json), written here as `30a Dn`).

The run 1 digest has no decisions marked as added in a later run, so all 83 are counted as run 1.

## A note on the input set

The two runs did not read the same contracts. Run 1 names two catalogue-be roots (`fc_openapi.yaml` and `fc_openapi-itb.yaml`) and catalogue-client-be v1 and v2. This run's inventory has catalogue-be split into a tier 1 file (12 operations) and a tier 2 file (13 operations), no `-itb` file, and catalogue-client-be v2 tier 1 only (5 operations: no search, no catalogue list, no QAR). Some run 1 gaps (run1 D1, run1 D6, run1 D8) are shaped by that input. They are still counted, with a note.

## Summary

| Measure | Count |
|---|---|
| Run 1 decisions | 83 |
| Environment choices (left out of the count) | 10 |
| Scope statements (left out of the count) | 4 |
| Gap decisions compared | 69 |
| Found by the validators (finding ids cited) | 48 |
| of which agree with 30a | 21 |
| of which differ from 30a | 25 (one of them, run1 D30, is an `accepted risk` in 30a D21) |
| of which found but not decided in 30a | 2 (run1 D13, run1 D28) |
| Decided only (a 30a decision, no finding) | 7 (2 agree, 5 differ) |
| Missed (no finding, no 30a decision) | 14 |
| This run's blockers and majors | 21 blockers, 48 majors |
| matching a run 1 decision | 14 blockers, 41 majors |
| new, in run 1 scope | 3 blockers, 4 majors |
| new, inside stories run 1 put out of scope | 4 blockers, 3 majors |

Of the 30 "differ" (25 found plus 5 decided only), 11 come from one scope choice: 30a D4 leaves out most of the catalogue-be lifecycle that run 1 builds (shape store, list, delete, service-offering create, verification, webhook, recalculation, participant operations). They are run1 D5, D10, D12, D17, D18, D21, D25, D56, D58, D60, D67.

## Per decision

Class: `gap`, `environment` or `scope`. Result: `found`, `decided only`, `missed`. Compare: `agree`, `differ`, `open` (found, no 30a decision).

| Run 1 | Topic | Class | Result | Findings | 30a | Compare | Why |
|---|---|---|---|---|---|---|---|
| run1 D1 | Two catalogue-be roots | gap | missed | | | | No finding on two roots; this run's inventory has no `-itb` file, so the conflict may not be in the input. |
| run1 D2 | Error body: Error versus Problem | gap | missed | | | | No finding. 30a itself uses an Error body code in 30a D17 and a problem type in 30a D31. |
| run1 D3 | Base paths, `/health` | environment | | | | | |
| run1 D4 | Who calls catalogue-be search | gap | decided only | | 30a D20, D24, D27 | differ | Run 1: the adapter resolves the token's attributes and calls catalogue-be. 30a: tier 2 reads `User-Attributes` set by a gateway that is not built. |
| run1 D5 | Two `/schemas` stores | gap | found | D-009 | 30a D4 | differ | Run 1 builds catalogue-be `/schemas` as the SHACL shape store. 30a does not build it. |
| run1 D6 | Tier 1 advanced search draft, operationId clash | gap | missed | | | | The tier 1 file is now an entry file, but `advancedSearchSelfDescriptions` still appears in tier 1 and tier 2 (inventory). No finding on the clash. |
| run1 D7 | Missing GA-to-GA and client-to-adapter contracts | gap | found | F-022, F-052 | 30a D28, D30 | differ | Both add the same three operations. Run 1 names the Trustor search `/federated/selfDescriptions`, 30a `/federated/resourceDescriptions`. |
| run1 D8 | Client backend v1 and v2 | gap | found | F-018, F-041, F-047 | 30a D7, D23, D32 | differ | Run 1 serves v1 and v2 side by side. 30a has only v2 as entry and copies the catalogue list and QAR operations from v1 into v2. |
| run1 D9 | Content negotiation 406, 415 | gap | missed | | | | No finding. |
| run1 D10 | Out-of-scope operations answer 501 | gap | found | D-012 | 30a D4 | differ | Run 1: the operations exist and answer 501. 30a: not built, no status stated. |
| run1 D11 | Identity of a resource description | gap | found | D-002, F-047 | 30a D7 | differ | Both: id is the credential subject id. Run 1: each version has its own id, `sdHash` by JCS. 30a: one id across versions (detail returns the latest), `sdHash` and `resourceKey` not used. |
| run1 D12 | Two create operations | gap | decided only | | 30a D4 | differ | Run 1: one pipeline for both. 30a: only `POST /self-descriptions/resource` is built. |
| run1 D13 | Version model in the payload | gap | found | D-022, D-005 | 30a D18 | open | Both revoke the previous version. Run 1 defines `simpl:version` and `simpl:previousVersion`. D-022 asks how a new version names its root; no 30a decision closes it. |
| run1 D14 | Publication pipeline order and codes | gap | missed | | | | Only the quality step is found (F-001, under run1 D51). Syntax, signature, ownership, schema and SHACL steps and their codes have no finding. |
| run1 D15 | Asynchronous 202 | gap | found | D-005 | 30a D18 | agree | Both: synchronous, 202 never returned. |
| run1 D16 | Revoke | gap | found | D-005 | 30a D18 | agree | Both: idempotent 200, revoked left out of search. 30a does not build the adapter PATCH and does not state the owner check. |
| run1 D17 | Delete | gap | found | D-022 | 30a D4, D18 | differ | Run 1: hard delete by the owner, the test clean-up path. 30a: not built. |
| run1 D18 | List defaults | gap | decided only | | 30a D4 | differ | Run 1 sets defaults, filters and order. 30a: not built. |
| run1 D19 | Version history response | gap | missed | | | | No finding on the `string` schema of catalogue-be versions versus the adapter payload. 30a D7 uses the operation without a shape. |
| run1 D20 | Notifications out | scope | | | | | |
| run1 D21 | Participant operations order | gap | decided only | | 30a D4 | differ | Run 1 sets the order. 30a: adapter `/participants` not built. |
| run1 D22 | Proof format | gap | missed | | | | No finding on how a credential is signed or verified. |
| run1 D23 | Trust anchors, key registry | gap | missed | | | | No finding on how catalogue-be knows a Provider's keys or binds token and signer. |
| run1 D24 | JSON-LD contexts | gap | missed | | | | No finding. |
| run1 D25 | `POST /verification` | gap | decided only | | 30a D4 | differ | Run 1 builds it with flags. 30a: not built. |
| run1 D26 | Validators always empty | gap | missed | | | | No finding. |
| run1 D27 | Quick search matching | gap | found | F-026 | 30a D19 | agree | Both: split on whitespace, OR, case-insensitive substring of any literal. Run 1 also excludes `@context`, types and the policy subtree. |
| run1 D28 | Search parameter names | gap | found | D-018 (minor) | | open | Found as a minor naming point; 30a gives no decision. |
| run1 D29 | Advanced search body | gap | found | F-027 | 30a D19 | agree | Same comparisons and AND. The mapping of root keys to nested nodes is in run 1 only. |
| run1 D30 | Discovery access policy | gap | found | D-010, F-024 | 30a D20, D21 | differ | Run 1: parse the ODRL policy, no policy means not discoverable. 30a D21 (accepted risk): a list of attribute codes, empty means everyone may discover. |
| run1 D31 | Paging | gap | missed | | | | No finding on page base, defaults and caps. |
| run1 D32 | Catalogue ids in local search | gap | found | F-028, F-035 | 30a D22 | agree | Both: one own catalogue, searched only when selected. Run 1 adds the alias `local`, 30a the id `ownCatalogue` (E3). |
| run1 D33 | Sorting | gap | found | F-040 | 30a D26 | agree | Both: not built. |
| run1 D34 | Main properties | gap | found | F-039, D-011 | 30a D13 | agree | Same five properties plus overall quality. Field names differ (see D-003 below). |
| run1 D35 | Test topology | environment | | | | | |
| run1 D36 | GA-to-GA authentication | gap | found | F-023, F-050 | 30a D25, D27 | differ | Run 1: no mTLS, client-credentials token checked inside the adapter. 30a: mTLS and the federation check in a gateway that is not built. |
| run1 D37 | Attribute forwarding and mapping | gap | found | F-024, D-010 | 30a D20, D27 | differ | Both: missing header 400, unmapped codes dropped. Run 1 maps in the Trustor adapter; 30a in the gateway (not built). |
| run1 D38 | Grouped result | gap | found | F-033, F-034 | 30a D24 | agree | Both: one response, a group per catalogue in request order, page a group by repeating with that id. |
| run1 D39 | Partial failure | gap | found | F-030, F-032, F-036 | 30a D25 | differ | Both: 200 with a failed group. Run 1: statuses `UNAUTHENTICATED` or `UNAVAILABLE`, adapter 502 or 504 on a single id. 30a: status `unavailable` for all. |
| run1 D40 | Unknown and inactive catalogue ids | gap | found | F-031 | 30a D25 | agree | Both: unknown id 400, inactive not called and marked unavailable. |
| run1 D41 | Quality in federated results | gap | found | F-037 | 30a D26 | agree | Both: null quality fields, filter on the own group only. |
| run1 D42 | Schema in the federated detail | gap | found | F-044 | 30a D29 | agree | Both: no schema, generic rendering. |
| run1 D43 | Detail errors and visibility | gap | found | F-046, F-049, F-051, F-053, F-042 | 30a D31 | differ | Run 1: policy denial 403, Trustor failure 502, timeout 504. 30a: denial hidden as 404, all Trustor failures 500. |
| run1 D44 | Single hop | gap | decided only | | 30a D28 | agree | Both: the Trustor searches only its own data space. |
| run1 D45 | Search all without selection | gap | found | F-020 | 30a D5 | agree | Both: out, selection is explicit. |
| run1 D46 | Catalogue list | gap | found | F-018, F-009, F-010 | 30a D3, D23 | agree | Both: Trustor catalogues from static data, own catalogue not in the list. |
| run1 D47 | Timeouts | gap | found | F-032 | 30a D25 | differ | Run 1: 3 s per Trustor call. 30a: 10 s (2 s in tests). |
| run1 D48 | Correlation-Id | gap | decided only | | 30a D30 | agree | Both: the Trusted side creates a UUID per forwarded call. |
| run1 D49 | Score scale | gap | found | F-004 | 30a D14 | differ | Run 1: 0 to 100 on the wire. 30a: 0 to 1 on the wire, percent in the UI. |
| run1 D50 | Quality Scoring stand-in | environment | | | | | |
| run1 D51 | Threshold evaluation | gap | found | F-001, F-003 | 30a D17 | differ | Both 422. Run 1: Problem `qualityBelowThreshold`, story text kept with "treshold". 30a: Error code `QUALITY_THRESHOLD_NOT_MET`, "threshold", and a separate dimension message. |
| run1 D52 | Quality failure modes | gap | found | F-002, F-003 | 30a D17 | differ | Run 1: 502 for scoring failures, 500 for configuration. 30a: 500 for all. |
| run1 D53 | Classification source and order | gap | found | F-005, F-006 | 30a D15 | differ | Run 1: one default profile. 30a: the union of active profiles, evaluated per result's profile. |
| run1 D54 | Quality filters exclude unscored | gap | found | F-007 | 30a D16 | agree | Both exclude descriptions without a score when a filter is set. |
| run1 D55 | QAR validity and storage | gap | missed | | | | No finding on what a valid QAR is. D-007 and 30a D11 settle names and representation only. |
| run1 D56 | Recalculation | gap | found | D-013 | 30a D4 | differ | Both: acknowledge only. Run 1 builds the endpoint; 30a does not. |
| run1 D57 | Quality in the detail | gap | found | F-041 | 30a D32 | differ | Run 1: a `qualityAssessment` member in the detail body. 30a: a separate QAR call, 204 shows a message. |
| run1 D58 | Shape store in catalogue-be | gap | found | D-009 | 30a D4 | differ | Run 1 builds it. 30a: not built. |
| run1 D59 | Resource description to schema link | gap | missed | | | | No finding on `dct:conformsTo`. |
| run1 D60 | Schema webhook | gap | found | D-013 | 30a D4 | differ | Run 1 builds status handling. 30a: not built. |
| run1 D61 | Schema list with `schemaType` | gap | found | F-060 | 30a D29 | agree | Both add `schemaType` (RESOURCE or SEARCH) to the client backend list. |
| run1 D62 | SEARCH schema management out | gap | found | F-016, F-054, F-055 | 30a D1, D2 | agree | Both: Schema Manager, out of the catalogue build. |
| run1 D63 | SHACL dialect | gap | missed | | | | No finding. 30a has no SHACL validation step. |
| run1 D64 | Identity provider contract | environment | | | | | |
| run1 D65 | Token claims | environment | | | | | |
| run1 D66 | Roles per operation | gap | found | F-029, F-059 | 30a D20 | differ | Run 1: any of four searcher roles. 30a: `CATALOGUE_SEARCHER` only. |
| run1 D67 | Roles "N/A" | gap | found | F-054 | 30a D1, D4 | differ | Run 1: a valid token, no role. 30a: the operations are not built. |
| run1 D68 | Token validation without gateways | gap | found | F-050 | 30a D27, E1 | differ | Run 1: every backend checks the JWT. 30a: tier 1 checks the JWT, tier 2 trusts headers. |
| run1 D69 | `User-Attributes` format | gap | found | F-024, D-010 | 30a D20 | agree | Both: comma-separated attribute codes. |
| run1 D70 | UI test selectors | environment | | | | | |
| run1 D71 | Catalogue selection in the UI | gap | found | F-018, F-019, F-021 | 30a D22, D23 | differ | Both: own catalogue first and preselected, own still usable on failure. Run 1 shows inactive catalogues disabled; 30a hides them. |
| run1 D72 | Quality filter controls | gap | found | F-007 | 30a D16 | agree | Both: 0 to 100 inline check, search disabled while invalid. |
| run1 D73 | Result tabs | gap | found | F-036, F-038 | 30a D24, D26 | agree | Both: one tab per group, labelled with the catalogue name. |
| run1 D74 | Advanced form from the schema | gap | found | F-061, F-062 | 30a D24, D29 | agree | Both use the SEARCH schema when a Trustor catalogue is selected. How a shape becomes a form is in run 1 only. |
| run1 D75 | Detail page | gap | found | F-044, F-045 | 30a D29, D31 | differ | Run 1: one generic page for own and federated. 30a: generic for federated, schema-driven for own. |
| run1 D76 | Schema management UI out | scope | | | | | |
| run1 D77 | Provider UI out | scope | | | | | |
| run1 D78 | Ports and names | environment | | | | | |
| run1 D79 | Stubs | environment | | | | | |
| run1 D80 | Test data | environment | | | | | |
| run1 D81 | Seed files | environment | | | | | |
| run1 D82 | Logging not asserted | gap | found | F-002 | 30a D17 | agree | Both log the reason and do not test the log. |
| run1 D83 | DOME out | scope | | | | | |

## Missed (14)

The validators check acceptance criteria (20b) and names against the CDMs (23b). Neither has a check that reads the OpenAPI contracts on their own for wire behaviour, and the resource description lifecycle in catalogue-be has no story. That explains most misses.

| Run 1 | Why the validators may have missed it |
|---|---|
| run1 D1 | The `-itb` root is not in this run's inventory, so there was no second root to compare. |
| run1 D2 | Error formats are a contract consistency question; no step compares error bodies across operations. |
| run1 D6 | The operationId clash spans two entry files; no step checks operationIds across files. |
| run1 D9 | 406 and 415 are contract details with no criterion behind them. |
| run1 D14 | The validation steps of publication come from the business process and the contract, not from a story; only the quality step has a story (SIMPL-27998). |
| run1 D19 | A response schema of type `string` is a contract defect; no story covers version history. |
| run1 D22 | No story or criterion mentions signatures, so the AC check had nothing to test; the domain check looks at names. |
| run1 D23 | Same as run1 D22: key registry and signer binding are outside every criterion. |
| run1 D24 | JSON-LD context handling is an implementation detail no source states. |
| run1 D26 | `validatorDids` is a contract field without a story. |
| run1 D31 | Paging defaults sit in the contracts only; no criterion asserts them. |
| run1 D55 | The QAR `POST` has no story; F-003 asks only whether an invalid QAR counts as a technical failure. |
| run1 D59 | The `dct:conformsTo` link appears only in a contract example and the process. |
| run1 D63 | SHACL validation at publication is not in any criterion; 30a also left the shape store out (30a D4). |

## New in this run (blockers and majors matching no run 1 decision)

### In run 1 scope (3 blockers, 4 majors)

| Finding | Severity | What it adds |
|---|---|---|
| D-001 | blocker | Resource description versus self-description as the business name (30a D6). Run 1 kept each wire name without a naming rule. |
| D-003 | blocker | Four names for the overall score and classification, and `overallCategory` in the criteria, which no API has (30a D8). Run 1 (run1 D34) kept the names per backend and did not note `overallCategory`. |
| D-004 | blocker | "Federated catalogue" means a Trustor catalogue in the stories and the own catalogue-be service in the CDMs (30a D9). |
| F-062 | major | No published SEARCH schema, or a failed retrieval: what the form shows (30a D29). Run 1's stub always serves one. |
| D-006 | major | "Active" has three meanings: lifecycle, quality profile, federation flag (30a D10). |
| D-007 | major | Three sets of names for the parts of a QAR (30a D11). |
| D-008 | major | Resource type and offering type: one concept or two, and their spelling (30a D12). |

### Inside stories run 1 put out of scope (4 blockers, 3 majors)

Run 1 put SIMPL-30850, SIMPL-31815, SIMPL-31816 and SIMPL-31817 out (Schema Manager) and examined only the `409 Config` case (run1 D62). This run checked the criteria of those stories and found:

| Finding | Severity | What it adds |
|---|---|---|
| F-012 | blocker | `schemaType` both required and defaulted on the version form. |
| F-013 | blocker | The 409 rule would block versioning of the one SEARCH schema. |
| F-056 | blocker | Uniqueness of the SEARCH schema differs between stories. |
| F-057 | blocker | Schema creation as a form editor or a file upload. |
| F-014 | major | No criterion for a user without the schema role. |
| F-017 | major | No statuses for invalid SHACL, missing fields, missing role. |
| F-058 | major | No error criteria on the create form. |

30a closes all seven with 30a D1 (out of the catalogue build) and 30a D2 (rules for the stub and owner feedback).
