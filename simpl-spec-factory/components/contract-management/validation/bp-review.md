# Business process review (step 22b): contract-management

A fresh reviewer read the business processes BP07 and BP10, the inventory and links, the 22a checks, the findings of 20b (F-), 21b (P-) and 23b (D-), the step 30 decisions and glossary, and the engine rules MSV-03, MSV-06, MSV-08, OWT-A06, OWT-A10 and OWA-API6. The findings are in [`bp-findings.json`](./bp-findings.json). This review judges the flow; it does not decide. Earlier findings are referred to by id, not repeated.

## Summary

| Severity | Count |
|---|---|
| blocker | 1 |
| major | 12 |
| minor | 2 |
| **total** | **15** |

The decided R17 flow (D1 to D13) holds together well on the happy path: every step from the offer to the Consumer's confirmation has an operation, a side and a state. The gaps are in what happens between the two sides when something goes wrong, and in what the process promises that the build does not carry (terms, notifications, expiry).

The five findings with most impact:

1. **B-001 (blocker)**: the Provider can hold an Active usage contract that the Consumer has refused or failed to confirm. Active has no exit and the Provider is never told.
2. **B-002**: the Consumer's Signed state has no exit when the Provider rejects, fails or a callback is lost. A signed VC stays in the Consumer's wallet.
3. **B-008**: the Consumer reviews and signs an offer that holds only ids, no terms, no billing schema, no clause hash.
4. **B-004**: contract-ui-be and the Contract Manager can drift apart after a timeout or a 409, and the nightly timeout then retries for ever.
5. **B-006**: parallel requests for the same resource both pass the existence check, so duplicate usage contracts are possible.

## Blockers

### B-001 Provider Active while the Consumer closes (BP07.12, MSV-08)

D7 sets the Provider record to Active before the Consumer has confirmed (BP07.12). If D8 then ends REJECTED or FAILED, the Consumer record goes to Closed and the Connector gets /isValid false. No operation or callback reaches the Provider's Contract Manager, and D12 has no transition out of Active. The process outcome 'usage contract not validated: no usage contract established, both parties notified' cannot be reached on the Provider side, and BP10 would bill from the Provider's Active record. The dynamic view (step 5) has the Provider store its record only after both VCs are secured.

**Question for the process owner:** when the Consumer's confirmation fails after the Provider has signed, what happens to the Provider's usage contract, and how does the Provider's Contract Manager learn it?

Options: Provider waits in a pending state until a Consumer confirmation callback; a new Active to Closed transition triggered by the Connector; Consumer-side technical failures retried instead of closed.

## Flow table (BP07, decided R17 flow)

States are the D12 states of the usage contract record on each side (C = Consumer side, P = Provider side). "none" means no record exists yet.

| Step | Party (decided) | Input | Output | Hand-over | Failure outcome | State before, after | Findings |
|---|---|---|---|---|---|---|---|
| BP07.01 Request offer (7.1.1) | Consumer user in the catalogue UI, Consumer Connector (not built, D2) | selected resource | EDC negotiation started | catalogue UI to Connector (consumption adapter POST /contracts, context only) | catalogue UI error (not built) | C: none, none | B-015 |
| BP07.02 Verify access policy (7.2.1) | Provider Connector (not built) | Consumer credentials, policy | allow or deny | Connector protocol | negotiation ends; no one notifies the Contract Manager or the users | C, P: none | B-005, B-009 |
| BP07.03 Verify existence (7.2.2) | Contract Manager, either side (D3); BP says Provider | search body from the Connector | 200 ACK, then /isPresent true or false | Connector to CM POST /agreements/{id}/search; CM to Connector /isPresent | 400, 401, 406, 415, 500; Connector decides on /isPresent true | C, P: none | B-006, B-007, B-009 |
| BP07.04 Provide offer (7.3.1, 7.3.2) | Provider Connector makes the EDC offer; Consumer CM prepares it (D4) | ContractAgreementCreate | record, humanReadable and hash, offer in contract-ui-be, notification | Connector to CM /create; CM to contract-ui-be POST /contractOffers; CM to Notification Service | contract-ui-be failure: 500, nothing stored; notification failure: logged | C: none to Requested | B-008, B-009 |
| BP07.05 Review (7.4.1, 7.4.2) | Consumer user through contract-ui-be (D14) | GET /contractOffers | accept or reject decision | contract-ui-be to CM PATCH /contractOffers/{id}/{operation} | CM failure: 502, offer stays CREATED; timeout after 7 days rejects (D13) | C: Requested | B-004, B-005, B-014 |
| BP07.05 reject branch (7.4.2) | Consumer CM (D5) | reject | /signed false | CM to Connector | callback failure logged, no retry | C: Requested to Closed | B-005 |
| BP07.06 Sign (7.5.1) | Consumer CM, triggered by accept (D5) | contract data | signature, VC | CM to Signer, VC Issuer | 500, stays Requested, user may retry; issued VC not withdrawn | C: Requested | B-003 |
| BP07.07 Store (7.5.2) | Consumer CM (D5, D6) | own VC | VC in wallet; /signed true | CM to Wallet; CM to Connector; Provider later pulls the VC via vcExchange and GET /verifications/{id}/vc (D10) | wallet error: 500, stays Requested; lost /signed true: no retry | C: Requested to Signed | B-002, B-003, B-012 |
| BP07.08 Validate (7.6.1) | Provider CM, automatic (D7, D9) | validateAndIssue from the Connector; Consumer VC via vcExchange | validation result | Connector to CM; CM to Connector GET /vcExchange | REJECTED: P Closed, /isValid false, notify Consumer, 422; FAILED: 500, P stays Requested; Consumer side not told | P: none to Requested | B-002, B-010 |
| BP07.09 Sign (7.7.1) | Provider CM, automatic (D7) | validated Consumer VC | Provider VC | CM to Signer, VC Issuer | 500, P stays Requested | P: Requested | B-003, B-010, B-011 |
| BP07.10 Store (7.7.2) | Provider CM (D7, D6) | Provider VC | VC in wallet | CM to Wallet | 500, P stays Requested | P: Requested to Active | B-001, B-003 |
| BP07.11 Notify Consumer (7.7.3) | Provider CM (D7) | Active record | /signed true; VC available through GET /verifications/{id}/vc (D10) | CM to Connector; Consumer pulls the VC | callback failure logged, no retry: Consumer stays Signed | P: Active | B-002, B-005 |
| BP07.12 Confirm (7.8.1) | Consumer CM (D8, D9) | PUT /verifications/{id}; Provider VC via vcExchange | validation result; /isValid true or false | Connector to CM; CM to Connector | REJECTED or FAILED: C Closed, /isValid false; Provider stays Active | C: Signed to Active or Closed | B-001, B-005, B-012 |

BP10 (billing) has no story in this component and no step is built. Its link to this flow is the usage contract it starts from (B-001: a Provider-side Active record the Consumer refused; B-008: no billing schema in the usage contract) and BP10.05, which hands enforcement to a BP11 that does not exist (B-013).

### Order and states

- The README order (Consumer signs, then Provider) matches D5, D7 and D8. The dynamic view has the Provider sign first (B-011).
- States no step reaches: an expired usage contract (B-007). States with no exit on a failure path: Signed on the Consumer side (B-002) and Active on the Provider side (B-001).
- SIMPL-21924/AC2 expects a record at request time (BP07.01); the decided flow creates it at BP07.04 (B-015).

### Parties and hand-overs

- Every Contract Manager hand-over is named: Connector calls in, Contract Manager calls back (/isPresent, /isValid, /signed), VCs are pulled through vcExchange and GET /verifications/{id}/vc. What the receiving Connector does with a callback, and whether the other side's Contract Manager hears of it, is not described (B-001, B-002, B-005).
- The process names the Provider for steps the decided flow gives to the Connector or the Consumer side (B-009).

## Triage of 22a

| 22a check | Findings | Out of scope or other process | Covered under another name or by a decision | Real gap (B- finding) |
|---|---|---|---|---|
| BP-01 | 1 | 0 | 1 (BA-001: delivery of the EDC offer is Connector work; receiving end is SIMPL-35526/AC1) | 0 |
| BP-02 | 2 | 0 | 2 (BA-002: validateAndIssue, D7; BA-003: GET /verifications/{id}/vc, D10; both new operations not yet in the OpenAPI) | 0 |
| BP-03 | 12 | 2 (BA-006 SIMPL-28846: BP05B; BA-014 SIMPL-34633: audit, cross-cutting) | 1 (BA-005 SIMPL-21966: failure path of BP07.08, covered by D7 and D11; link missing only) | 9 (BA-004 in B-012; BA-007 to BA-013 in B-013; BA-015 in B-014) |
| BP-04 | 0 | 0 | 0 | 0 |
| **total** | **15** | **2** | **4** | **9** |

Other notes from the triage: BP 7.2.1 (access policy) is covered only by SIMPL-21924 criteria that D2 leaves to the catalogue UI; the check itself is Connector work, so it is out of scope here. BP 7.3.1 is covered by SIMPL-19829 and 32511, whose Provider-side behaviour D4 does not build; this is part of B-009. The 17 process steps carry no direct links; they inherit the coverage of the detailed requirements as mapped in the flow table.
