# AC review (step 20b): contract-management

Judgement checks on the live acceptance criteria, made by a fresh agent from `specs/`, `inventory/` and the step 20a results only. Findings are in [`ac-findings.json`](./ac-findings.json). The findings say what a builder needs; they do not fix the criteria and they do not decide.

## Summary

57 findings on 138 live criteria in 36 stories: 14 blockers, 34 major, 9 minor.

| Check | Blocker | Major | Minor | Total |
|---|---|---|---|---|
| AC-C01 | 1 | 7 | 0 | 8 |
| AC-C02 | 2 | 6 | 4 | 12 |
| AC-C03 | 3 | 10 | 1 | 14 |
| AC-C04 | 4 | 1 | 3 | 8 |
| AC-C05 | 4 | 5 | 1 | 10 |
| AC-C06 | 0 | 5 | 0 | 5 |
| **Total** | 14 | 34 | 9 | 57 |

### The 5 findings with the most impact on a build

1. **F-003**: Validation and Provider signing are specified twice (async persisted state in SIMPL-19933/21948/19934 versus synchronous 201 or EDC callbacks in SIMPL-35584/35677). Every test of BP7.6 to BP7.8 depends on which set is normative.
1. **F-002**: The /create endpoint runs on the Provider side in SIMPL-19829/32511 and BP7.3.1, and on the Consumer side in SIMPL-35526. This decides where offers are created and what /create returns.
1. **F-004**: There is no single status set: stories use Requested, In progress, Accepted, Signed, Active, Finalized, pending, Suspended, Revoked, Terminated; the APIs use three other enums. The UI overview, filters, scheduler and lifecycle stories all depend on it.
1. **F-001**: The existence check has three interaction models (async ACK plus callback, synchronous boolean, /isPresent callback to EDC) and two sides.
1. **F-005**: The lifecycle stories (revoke, suspend, reactivate, terminate) have no API, and no story says how a status change reaches the other participant.

## Blockers

These have to be closed in step 30 (a decision row or accepted risk) before tests or code are written.

| Id | Target | Check | Question |
|---|---|---|---|
| F-001 | cross (19824/AC1, 19824/AC2, 32509/AC1, 35858/AC1) | AC-C04 | Which model is the target for the existence check: the existing async search endpoint with a callback, the new synchronous validateExistenceContract endpoint, or the /isPresent callback to EDC; and on which side (Provider or Consumer) does the Contract Manager run it? |
| F-002 | cross (19829/AC1, 32511/AC1, 35526/AC1) | AC-C04 | On which side does /create run, and what does it return: ACK plus callback to the Provider Connector (SIMPL-19829), the offer body with usageContract and usageContractHash (SIMPL-32511), or 201 with a push to the Contract UI backend and a user notification (SIMPL-35526)? |
| F-003 | cross (19933/AC10, 19934/AC1, 21948/AC10, 35584/AC1, 35677/AC1) | AC-C04 | Which stories define the target validation and signing flow on each side, and which ones are superseded? What does a test observe: the HTTP response, the persisted state, or the EDC callback? |
| F-004 | cross (30959/AC1, 21924/AC2, 32511/AC1, 32505/AC2, 32505/AC3, 35692/AC4, 31053/AC2, 31115/AC2, 31116/AC2, 19934/AC1) | AC-C04 | What is the one status set for a usage contract offer and a usage contract (including Suspended, Revocation requested, Revoked, Reactivation requested, Terminated), and how does each map to the API enums? |
| F-005 | cross (31053/AC3, 31076/AC4.1, 31076/AC4.2, 31115/AC3, 31116/AC3, 31117/AC3, 31118/AC3) | AC-C05 | Which backend owns the lifecycle state and operations (contract-manager-be or contract-ui-be), and through which channel (EDC, Kafka, other) is a status change passed to the other participant? |
| F-006 | cross (19927/AC2, 19927/AC3, 19927/AC4, 19938/AC2.1, 19938/AC2.2, 19938/AC3.1, 19938/AC3.2, 19938/AC4.1, 19938/AC4.2) | AC-C03 | When the wallet is unavailable, full or corrupted during the automatic signing flow, what happens: a user prompt for another location, or the step fails, the error is logged and EDC is told (for example /signed/{id}/false)? What does 'another secure location' mean, if anything? |
| F-007 | SIMPL-19938 | AC-C03 | Which version of AC1 to AC4 is the live one: the user-driven list items (AC1.1 to AC4.1) or the system-level headings (AC1.2 to AC4.2)? |
| F-008 | SIMPL-21924/AC2 | AC-C03 | Does the catalogue UI show the validation result (positive, access denied, contract exists), or only that the request was sent, with the result shown later in the contract manager UI? |
| F-009 | SIMPL-34633/AC1 | AC-C02 | What is the endpoint and response format of the audit data, which status set is counted, what is 'a given moment' (on request, scheduled), and what is the audit rule package? |
| F-010 | SIMPL-35660/AC1 | AC-C05 | Is the operation PATCH with 200 (as in the OpenAPI) or POST with 201 (as in the story)? |
| F-011 | SIMPL-35677/AC1 | AC-C05 | Which method and status code apply (PUT/200, POST/201 or POST/202), and does EDC still send the VC in the body or does the Contract Manager fetch it via /vcExchange? |
| F-012 | SIMPL-35584 | AC-C01 | What does validateAndIssue return and do when the consumer VC fails technical or business validation, when the VC cannot be fetched via EDC, and when the Signer, VC Issuer or wallet fails? |
| F-013 | SIMPL-28846/AC1 | AC-C05 | Which upload operation, supported MIME types and maximum file size apply, and which component exposes the upload? |
| F-014 | cross (19933/AC1, 19933/AC8, 21948/AC1, 21948/AC8) | AC-C02 | Which status does the negotiation or contract get after a successful validation, and after a technical error: FAILED or PENDING_RETRY (with retry)? |

## Per story

Story-level findings are shown in the row `(story)`. Cross-story findings are listed on each criterion they name. Criteria without findings are left out; a story with no row has no finding.

### SIMPL-19824: [Replaced][API] - API to verify existing usage contract

| AC | Findings | Severity |
|---|---|---|
| (story) | F-027 | major |
| AC1 | F-001 | blocker |
| AC2 | F-001, F-018 | blocker, major |

### SIMPL-19829: [Replaced] [API] - API to create usage contract offer

| AC | Findings | Severity |
|---|---|---|
| (story) | F-028 | major |
| AC1 | F-002 | blocker |

### SIMPL-19927: Consumer stores signed usage contract securely in digital wallet

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-024 | major |
| AC2 | F-006 | blocker |
| AC3 | F-006 | blocker |
| AC4 | F-006 | blocker |

### SIMPL-19933: Provider Contract Manager to validate the usage contract offer signed by the Consumer

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-014, F-016 | blocker, major |
| AC2 | F-015 | major |
| AC4 | F-016 | major |
| AC6 | F-015 | major |
| AC7 | F-017 | major |
| AC8 | F-014 | blocker |
| AC9 | F-015 | major |
| AC10 | F-003 | blocker |

### SIMPL-19934: Provider signs the received contract previously signed by the Consumer

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-003, F-004, F-038, F-025 | blocker, major, minor |

### SIMPL-19938: Provider stores signed usage contract securely in digital wallet

| AC | Findings | Severity |
|---|---|---|
| (story) | F-007 | blocker |
| AC1.1 | F-024 | major |
| AC1.2 | F-024 | major |
| AC2.1 | F-006 | blocker |
| AC2.2 | F-006 | blocker |
| AC3.1 | F-006 | blocker |
| AC3.2 | F-006 | blocker |
| AC4.1 | F-006 | blocker |
| AC4.2 | F-006 | blocker |

### SIMPL-21924: [UI] Consumer requests a contract offer for using a resource via UI

| AC | Findings | Severity |
|---|---|---|
| (story) | F-050 | major |
| AC1 | F-055 | minor |
| AC2 | F-004, F-008 | blocker |
| AC4 | F-018 | major |

### SIMPL-21928: Consumer signs the usage contract offer received from a Provider

| AC | Findings | Severity |
|---|---|---|
| (story) | F-040 | major |
| AC1 | F-019 | major |
| AC2 | F-025 | minor |

### SIMPL-21948: Consumer validates the usage contract signed by the Provider

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-014, F-016 | blocker, major |
| AC2 | F-015 | major |
| AC4 | F-016 | major |
| AC7 | F-017 | major |
| AC8 | F-014 | blocker |
| AC9 | F-015 | major |
| AC10 | F-003 | blocker |

### SIMPL-21963: Consumer delivers the signed usage contract offer to the Provider

| AC | Findings | Severity |
|---|---|---|
| (story) | F-039 | major |

### SIMPL-21966: Provider notifies Consumer about incorrectly signed usage contract offer

| AC | Findings | Severity |
|---|---|---|
| (story) | F-041 | major |
| AC6 | F-057 | minor |

### SIMPL-22690: [Replaced] A Consumer accepts a usage contract offer via UI

| AC | Findings | Severity |
|---|---|---|
| (story) | F-029 | major |

### SIMPL-28846: Store uploaded Contract Clauses documents and generate hash metadata

| AC | Findings | Severity |
|---|---|---|
| (story) | F-042 | major |
| AC1 | F-013 | blocker |
| AC2 | F-056 | minor |
| AC6 | F-043 | major |

### SIMPL-30959: A Consumer consults an overview of their usage contracts via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-004, F-044, F-045 | blocker, major |
| AC2 | F-051 | minor |
| AC3 | F-052 | minor |

### SIMPL-30960: A Consumer consults the details of a usage contract via UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-046 | major |

### SIMPL-31047: A Provider chooses the action to update the contract consent assertion status of a selected usage contract in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| (story) | F-047 | major |

### SIMPL-31053: A Provider or Consumer requests the revocation of a usage contract in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| AC2 | F-004, F-023 | blocker, major |
| AC3 | F-005 | blocker |

### SIMPL-31076: The responding Participant responds to a revocation request of a usage contract in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| (story) | F-054 | minor |
| AC2 | F-022 | major |
| AC3 | F-023 | major |
| AC4.1 | F-005 | blocker |
| AC4.2 | F-005 | blocker |

### SIMPL-31115: A Provider suspends a usage contract by providing a reason in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| AC2 | F-004, F-022, F-023 | blocker, major |
| AC3 | F-005 | blocker |

### SIMPL-31116: A Consumer requests the reactivation of a suspended usage contract in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| AC2 | F-004, F-023, F-053 | blocker, major, minor |
| AC3 | F-005 | blocker |

### SIMPL-31117: A Provider reactivates a suspended usage contract in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| (story) | F-048 | major |
| AC2 | F-022 | major |
| AC3 | F-005 | blocker |

### SIMPL-31118: A Provider terminates a usage contract by providing a justification related to the termination conditions in their contract manager UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-049 | major |
| AC2 | F-022, F-023 | major |
| AC3 | F-005 | blocker |

### SIMPL-32505: A Consumer accepts a usage contract offer via UI

| AC | Findings | Severity |
|---|---|---|
| AC2 | F-004, F-019 | blocker, major |
| AC3 | F-004 | blocker |

### SIMPL-32509: [API] - API to verify existing usage contract

| AC | Findings | Severity |
|---|---|---|
| (story) | F-031 | major |
| AC1 | F-001, F-018 | blocker, major |
| AC4 | F-030 | major |

### SIMPL-32511: [API] - API to create usage contract offer

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-002, F-004, F-032 | blocker, major |

### SIMPL-34633: [API] A Participant generates aggregated usage contract data for auditing purposes

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-009 | blocker |

### SIMPL-35440: [API] Contract Manager returns VC for contract for given negotiation ID

| AC | Findings | Severity |
|---|---|---|
| (story) | F-036 | major |
| AC1 | F-020 | major |

### SIMPL-35526: Contract Manager prepares contract data for contract UI

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-002, F-033 | blocker, major |

### SIMPL-35584: [API] Contract Manager validates and signs contract agreement

| AC | Findings | Severity |
|---|---|---|
| (story) | F-012 | blocker |
| AC1 | F-003, F-020 | blocker, major |

### SIMPL-35660: Contract Manager signs contract on consumer side

| AC | Findings | Severity |
|---|---|---|
| (story) | F-034 | major |
| AC1 | F-010, F-019 | blocker, major |

### SIMPL-35677: [API] Contract Manager verify contract on consumer side

| AC | Findings | Severity |
|---|---|---|
| (story) | F-035 | major |
| AC1 | F-003, F-011 | blocker |

### SIMPL-35692: Contract UI backend will terminate unsigned contract offers after X days

| AC | Findings | Severity |
|---|---|---|
| (story) | F-037 | major |
| AC4 | F-004 | blocker |

### SIMPL-35858: [API] Create API for contract existence verification

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-001, F-021, F-026 | blocker, major, minor |

### SIMPL-35863: [API] Create API for contract validation result

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-021, F-026 | major, minor |

### SIMPL-35888: [API] Create API for VC exchange

| AC | Findings | Severity |
|---|---|---|
| AC1 | F-021 | major |

### SIMPL-35889: [API Definition] Create API for VC exchange

| AC | Findings | Severity |
|---|---|---|
| (story) | F-021 | major |

## Cross-story

Consistency findings (AC-C04) and other findings that span stories.

- **F-001** (AC-C04, blocker), cross (19824/AC1, 19824/AC2, 32509/AC1, 35858/AC1): Three stories describe the existence check with three different interaction models: an async ACK plus a later callback, a synchronous 200 with a boolean, and a Contract Manager call to an EDC endpoint. The live criteria also place the check on different sides.
- **F-002** (AC-C04, blocker), cross (19829/AC1, 32511/AC1, 35526/AC1): The create endpoint is assigned to the Provider in two stories and the process, and moved to the Consumer side in a third. The response also differs: an ACK with a later callback, a full offer body, or a 201 that triggers the Contract UI backend.
- **F-003** (AC-C04, blocker), cross (19933/AC10, 19934/AC1, 21948/AC10, 35584/AC1, 35677/AC1): Validation and Provider signing are specified twice with different observable outcomes. The older stories say validation is asynchronous and checked through persisted state and logs; the newer ones return 201 after validating and signing, or call EDC /isValid. SIMPL-19934 expects 200 with 'Finalized' from a signing call.
- **F-004** (AC-C04, blocker), cross (30959/AC1, 21924/AC2, 32511/AC1, 32505/AC2, 32505/AC3, 35692/AC4, 31053/AC2, 31115/AC2, 31116/AC2, 19934/AC1): Each story uses its own set of status names for the same offer or contract, and the two APIs use yet other enums. A UI overview, a filter and the scheduler cannot be built without one agreed set and a mapping.
- **F-005** (AC-C05, blocker), cross (31053/AC3, 31076/AC4.1, 31076/AC4.2, 31115/AC3, 31116/AC3, 31117/AC3, 31118/AC3): The lifecycle stories (revoke, respond, suspend, reactivate, terminate) need operations that no OpenAPI offers, and each has a criterion where the other party sees the new status. No story says how the change reaches the other participant.
- **F-006** (AC-C03, blocker), cross (19927/AC2, 19927/AC3, 19927/AC4, 19938/AC2.1, 19938/AC2.2, 19938/AC3.1, 19938/AC3.2, 19938/AC4.1, 19938/AC4.2): The wallet failure criteria ask the user to name another secure location, but storage happens inside an automatic backend flow with no user in the loop. A builder can read this as a UI prompt or as a failed step with an error.
- **F-014** (AC-C02, blocker), cross (19933/AC1, 19933/AC8, 21948/AC1, 21948/AC8): The success outcome of both validation stories is 'updated according to the agreed lifecycle' without naming the status, and the technical-error case allows two end states. There is no pass condition on the main path.
- **F-015** (AC-C03, major), cross (19933/AC2, 19933/AC6, 19933/AC9, 21948/AC2, 21948/AC9): The failure criteria say 'rejected or failed' without saying which, and only the Consumer-side story maps cases to reason codes. The Provider side has no codes.
- **F-016** (AC-C03, major), cross (19933/AC1, 19933/AC4, 21948/AC1, 21948/AC4): Both validation stories check a validity period, but no API schema or story defines where an offer or contract carries its validity dates.
- **F-017** (AC-C03, major), cross (19933/AC7, 21948/AC7): Duplicate or replayed submissions must be detected, but the key is not named. Two builders will pick different keys (negotiation id, signature, payload hash).
- **F-018** (AC-C03, major), cross (32509/AC1, 21924/AC4, 19824/AC2): 'Active usage contract' decides whether a new offer may be created, but the lifecycle stories add Suspended, Revocation requested and Reactivation requested. It is not said whether these count as active.
- **F-019** (AC-C04, major), cross (21928/AC1, 35660/AC1, 32505/AC2): Consumer signing is described in three places with different triggers: a generic 'signing process is triggered', the accept operation on the Contract Manager, and the UI accept action. It is unclear whether SIMPL-21928 adds behaviour beyond SIMPL-35660.
- **F-020** (AC-C05, major), cross (35440/AC1, 35584/AC1): Two Contract Manager endpoints in the criteria are not in the OpenAPI, so request body, response body and error codes are unknown.
- **F-021** (AC-C06, major), cross (35858/AC1, 35863/AC1, 35888/AC1, 35889): Four stories specify endpoints of the EDC connector, not of the Contract Management component. For this component they only define the client side of a call.
- **F-022** (AC-C03, major), cross (31115/AC2, 31117/AC2, 31118/AC2, 31076/AC2): The lifecycle stories say why (remove or restore access) but no criterion says what happens to resource access, or who enforces it, when a contract becomes Suspended, Revoked, Terminated or Active again.
- **F-023** (AC-C01, major), cross (31053/AC2, 31076/AC3, 31115/AC2, 31116/AC2, 31118/AC2): Reasons and justifications are asked for in five actions, but no criterion says whether they are mandatory, what limits apply, or whether the other party sees them.
- **F-024** (AC-C02, major), cross (19927/AC1, 19938/AC1.1, 19938/AC1.2): The 'securely stored' outcomes have no oracle beyond the wallet call, and 'access is restricted to authorized roles' names no role.
- **F-025** (AC-C04, minor), cross (21928/AC2, 19934/AC1): One story forbids an 'unsigned VC' as an intermediate artefact while another describes one as a step. Signer naming also differs.
- **F-026** (AC-C02, minor), cross (35858/AC1, 35863/AC1): The EDC callback criteria do not name the response code for an accepted result.
- **F-054** (AC-C04, minor), SIMPL-31076: Two criteria share the label AC4, and the same decision is called 'deny' in AC3 and 'Reject' in AC4.2. SIMPL-31053/AC3 is headed 'Accept case' but only shows the request.
- **F-055** (AC-C04, minor), SIMPL-21924/AC1: Headings of AC1 to AC4 name the Provider, while the clauses are written for the Consumer end user.

Word pairs from `inventory-review.md` that show up as real conflicts: "Accepted" / APPROVED / ACCEPTED and "Requested" / "In progress" / "pending" / CREATED / INITIATED (F-004); "Digital Signature Service" / "Signer Service" (F-025); "deny" / "Reject" (F-054); "Contract Backend" / "Contract Manager" (F-029). The other pairs did not change behaviour in any criterion read.

## 20a triage

| Code | What | Hits | Real | Harmless | Example of a harmless hit |
|---|---|---|---|---|---|
| AC-S03 | no Then | 1 | 1 | 0 | - |
| AC-S04 | more than one action in When | 18 | 1 | 17 | `SIMPL-32509/AC1`: "verifies" and "invokes the POST ... API" are one action stated twice; in the others the And is a condition, not a second action |
| AC-S05 | more than 5 outcomes | 2 | 2 | 0 | - |
| AC-S07 | AC number used twice | 5 | 5 | 0 | - |
| AC-S08 | story without ACs | 1 | 1 | 0 | - |
| AC-S10 | text outside the clauses | 26 | 7 | 19 | `SIMPL-30959/AC1`: the bullet list is the field list of the Then itself; elsewhere a Technical Info block after the last criterion |
| AC-S11 | replaced story still live | 3 | 3 | 0 | - |
| AC-S12 | near-duplicate AC | 9 | 0 | 9 | `SIMPL-19927/AC3`: same frame as AC2 but a different wallet condition and message; all 9 pairs test different cases |
| AC-W01 | vague word | 10 | 10 | 0 | - |
| AC-W05 | limit or time without a value | 11 | 5 | 6 | `SIMPL-35692/AC2`: "timeout" has a value: default 7 days, configurable (AC1 Given and Technical Description); SIMPL-19824/AC6 "timeout" is an example error type |
| **Total** | | 86 | 35 | 51 | |

Real hits and the findings that carry them: AC-S03 in F-053; AC-S04 (SIMPL-22690/AC2) in F-029; AC-S05 in F-001; AC-S07 in F-007 and F-054; AC-S08 in F-021; AC-S10 in F-002, F-009, F-011, F-013, F-015, F-029, F-039; AC-S11 in F-027 to F-029; AC-W01 in F-024, F-029, F-030, F-039, F-042; AC-W05 in F-013, F-016, F-041.

## Coverage

Most findings: SIMPL-19933 (5), SIMPL-21924 (5), SIMPL-21948 (5), SIMPL-30959 (5), SIMPL-19934 (4), SIMPL-28846 (4).

Fewest findings (0 or 1): SIMPL-21963 (1), SIMPL-22690 (1), SIMPL-30960 (1), SIMPL-31047 (1), SIMPL-34633 (1), SIMPL-35888 (1), SIMPL-35889 (1). A low count here mostly means a short story, not a complete one.

Not covered by any criterion (from `links.json`, as a reminder):

- Operations in scope of this component: `contract-manager-be:GET /agreements/{contractAgreementId}/file`, `contract-manager-be:POST /agreements/search`, `contract-manager-be:PUT /contractOffers/{contractNegotiationId}/update`, and both `GET /health`. The consumption adapter operations other than `POST /contracts` are context only.
- Process requirement `BP7.3.2` (A Provider provides the usage contract offer to a Consumer).
- The other way round, several criteria need operations that no OpenAPI has: the lifecycle and consent stories (F-005, F-047), the audit endpoint (F-009), the upload (F-013), validateExistenceContract (F-001), validateAndIssue and GET /verifications/{contractNegotiationId}/vc (F-020).
