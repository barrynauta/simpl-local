# Contract Management: run 1 decisions compared with this run

Run 1: the spec digest of late October 2026, decisions run1 D1 to run1 D83 with appendices A to E. Section 7 of that digest (C1 to C6, "clarifications after run 1", 29 Oct 2026) is a later addition and is counted separately.

This run: findings of step 20b (`validation/ac-findings.json`, F-001 to F-057) and step 23b (`validation/domain-findings.json`, D-001 to D-028), and the decisions of step 30a (`decisions/decisions.json`, 30a D1 to 30a D25 and 30a E1 to 30a E8).

Classes used:

- `found`: the gap is in a 20b or 23b finding. The result then says whether the 30a decision `agrees`, `differs`, is an `accepted risk`, or is `open` (found, but 30a decides nothing on it).
- `decided only`: no finding names the gap, but a 30a decision settles it.
- `missed`: neither a finding nor a 30a decision covers it.
- `environment` or `scope`: a run 1 choice of test environment, stub design or build scope, not a gap in the specs. Left out of the found and missed counts.

## Summary

| Measure | Count |
|---|---|
| Run 1 decisions (run1 D1 to run1 D83) | 83 |
| of which `environment` | 17 |
| of which `scope` | 1 |
| Counted gaps | 65 |
| `found` | 50 (77 %) |
| `decided only` | 4 (6 %) |
| `missed` | 11 (17 %) |
| Found: 30a agrees | 9 |
| Found: 30a differs | 33 |
| Found: 30a accepted risk | 7 |
| Found: open (no 30a decision) | 1 |
| Decided only: agrees / differs | 0 / 4 |
| Blockers and majors of this run | 67 (20 blockers, 47 majors) |
| of which match a run 1 decision or scope statement | 59 |
| of which `new` | 8 (2 blockers, 6 majors) |
| Later additions in the digest (run 1.1, C1 to C6) | 6, see the last section |

The validators found about three quarters of the run 1 gaps on their own. Where both runs decided, they mostly decided differently (37 differ against 9 agree, counting found and decided only). Much of the difference comes from one scope choice: run 1 builds the UI, Keycloak users, the post-Active lifecycle, a Kafka channel, consent and clause upload; 30a builds two backends with API keys and puts the lifecycle, consent and upload out as accepted risks (30a D2, D15, D16, D18).

## Per decision

| Run 1 | Topic | Class | Findings | 30a | Result | Why |
|---|---|---|---|---|---|---|
| run1 D1 | Same paths on two backends | found | D-015 | 30a D21, D14 | agree | Both: UIBE calls CM's PATCH, CM is the root, UIBE holds a view. Run 1 also names the connector as caller of CM `POST /contractOffers`; 30a is silent on that. |
| run1 D2 | Which party a call runs at | found | F-034 | 30a E3 | differ | Run 1 derives the side per negotiation from the body and the own id; 30a fixes the side per instance (`PARTICIPANT_SIDE`). Both return 400 on the wrong side. |
| run1 D3 | Orchestrator and Kafka between connector and CM | found | D-014, F-029 | 30a D2 | agree | Both: no orchestrator, connector calls CM over HTTP, CM calls connector callbacks. |
| run1 D4 | Story path or method differs from OpenAPI | found | F-010, F-011, F-020, F-038, F-001 | 30a D5, D8, D3, D7 | differ | Agree on PATCH 200, PUT 200 and on not building `validateExistenceContract`. Run 1 builds 35584 on `POST .../issue`; 30a adds a new `POST .../validateAndIssue`. |
| run1 D5 | `GET /verifications/{nid}/vc` | found | F-020, F-036 | 30a D10 | agree | Both add it on both sides: own VC, 404 if unknown or not signed, 401. |
| run1 D6 | Read model and lifecycle API on CM | found | F-044, F-005 | 30a D14, D15 | differ | Run 1 adds `GET /contracts`, `GET /contracts/{nid}`, `POST .../lifecycleActions`; 30a adds no read model (overview from UIBE, details from `GET /agreements`) and no lifecycle API. |
| run1 D7 | UIBE operations for the UI | found | F-044, F-046 | 30a D14, D2 | differ | Run 1 adds four UIBE operations; 30a extends `ContractOffer` with ids and `createdAt` and adds no operation. |
| run1 D8 | `GET /contractOffers` object or list | found | F-051, F-044 | 30a D14 | agree | Both: JSON array, empty array, never 404. |
| run1 D9 | Offer field names in UIBE | found | D-017 | 30a D21, D14 | differ | Run 1: requests `humanReadable`/`hash`, responses `humanReadableData`/`hashData`, PATCH body an object. 30a: `humanReadable`/`hash` everywhere, PATCH body the status as a JSON string. |
| run1 D10 | Offer status vocabularies | found | D-006, F-037, F-004 | 30a D12, D13 | differ | Run 1 adds UIBE `TERMINATED` for expiry and keeps CM `APPROVED`. 30a adds UIBE `EXPIRED` and retires `APPROVED` for `ACCEPTED`. |
| run1 D11 | Error format and types | found | F-030, F-042 | 30a E5, D3 | differ | Both RFC 9457 with `urn:problem-type:simpl:<type>`. Type names and codes differ (`validationFailure` vs `invalidRequest`; run 1 adds `wrongParty`, `offerNotPending`; 30a adds `conflict` 409, `usageContractValidationFailed` 422, `badGateway` 502). |
| run1 D12 | Unknown id where no 404 is documented | decided only | (none) | 30a D5, D8, D14 | differ | Run 1 returns 400 where the OpenAPI lists no 404; 30a returns 404. No finding raises the undocumented code. |
| run1 D13 | Health endpoint | missed | (none) | 30a E2 (auth only) | | Run 1 fixes content type, `status: UP`, 503 when the store is down. |
| run1 D14 | Correlation id | found | F-029 | 30a E7, D11 | differ | Run 1: `X-Correlation-Id` header, generated, echoed, propagated. 30a: the nid is the correlation id, in logs only. |
| run1 D15 | Date-time format | missed | (none) | (none) | | OpenAPI examples have no offset; run 1 fixes UTC rules. |
| run1 D16 | Id formats | missed | (none) | (none; 30a D3 refers to "its pattern") | | `contractAgreementId` UUID versus the nid pattern and length. |
| run1 D17 | Who starts the negotiation, how a test drives it | scope | F-050, F-008 | 30a D2 | agree | Both leave BP07.01 and the catalogue UI out. Run 1 adds a stub that drives the whole flow; 30a tests call CM directly. |
| run1 D18 | One negotiation id for both parties | found | D-004, D-002 | 30a D20, D19 | agree | Both: nid and aid are shared and come from the connector; CM never mints its own. |
| run1 D19 | Existence check | found | F-001, F-018, F-030, F-031, F-027 | 30a D3 | differ | Same model (search, ACK, `/isPresent`). Run 1 counts all open states including Requested and Signed, needs only `assetId` and `consumerId`, returns `exists`, and creates the Provider record. 30a counts Active and reserved states only, needs all five fields, creates no record. |
| run1 D20 | `/create` | found | F-002, F-032, F-033, F-028 | 30a D4 | differ | Both: Consumer side, 201 `CREATED`, 500 and no record if UIBE fails. Run 1: catalogue lookup (404, 403), 32511 response fields, 400 `alreadyExists`. 30a: no catalogue call, no 32511 fields, 409 on a repeat. |
| run1 D21 | Human-readable offer and its hash | decided only | (none; D-017 is names only) | 30a E8 | differ | Both SHA-256 lower-case hex. Run 1 renders resource, parties, policies and clauses, HTML-escaped; 30a lists the seven ids. No finding raises the MD5-length example hash. |
| run1 D22 | Consumer notified that an offer is ready | found | F-033 | 30a D11, D4 | differ | Run 1: `EmailNotification` on Kafka to the directory email. 30a: a notification request to an HTTP stub, which resolves the address. |
| run1 D23 | Accept or reject | found | F-010, F-034, F-045, F-029, D-006 | 30a D5, D14 | differ | Run 1: 400 `offerNotPending`, async signing, reject to Rejected, UIBE returns CM's code. 30a: 409, signing inside the call, reject to Closed, UIBE returns 502. |
| run1 D24 | Consumer signing | found | F-019, F-040, F-025, F-006 | 30a D5, D6 | differ | Run 1: async; failure sets Failed with a reason and calls `/signed false`. 30a: sync; failure is 500, record stays Requested, no callback, user may retry. |
| run1 D25 | Provider validate-and-sign trigger | found | F-003, F-012, F-038, F-020 | 30a D7 | differ | Run 1: `/issue`, async, 201 `INITIATED`. 30a: `validateAndIssue`, sync, 201 `FINALIZED` or 422 or 500. |
| run1 D26 | How the Provider gets the Consumer VC | found | F-012, F-039 | 30a D7 | agree | Both via `/vcExchange`; 404 gives Rejected with `MISSING_CONTRACT_DATA`. |
| run1 D27 | Validation checks, order, reason codes | found | F-014, F-015, F-016 | 30a D9 | differ | Same codes from 21948, first failure wins. Order differs, and run 1 adds an issuer DID check against the directory. |
| run1 D28 | Duplicates and technical errors | found | F-017, F-014 | 30a D9 | differ | Run 1: duplicate after Success or Rejected is `IGNORED` with the normal response; Failed keeps the status and allows a retry. 30a: duplicate on Active or Closed is 409; Consumer-side Failed closes the record. |
| run1 D29 | Provider outcome after validation | found | F-012, F-039 | 30a D7 | differ | Run 1: `/isValid true`, Signed, later Active through `PATCH .../status`. 30a: straight to Active, no `/isValid true`. |
| run1 D30 | Notification of an incorrectly signed offer | found | F-041 | 30a D11 | differ | Both: only on Rejected, not on Failed, no retry. Channel differs as in run1 D22. |
| run1 D31 | Consumer validation of the Provider VC | found | F-011, F-035 | 30a D8 | differ | Opposite readings. Run 1: VC in the PUT body, no `/vcExchange`. 30a: body ignored, CM fetches via `/vcExchange`. |
| run1 D32 | Finalisation at the Provider (`PATCH /agreements/{aid}/definitions/{did}/status`) | missed | (F-039 names the operation only as a possible oracle) | (none) | | 30a never says what this OpenAPI operation does. |
| run1 D33 | CM `POST /contractOffers/{nid}` and `PUT .../update` | found | D-015, D-023 | (none) | open | D-015 cites both operations; no 30a decision gives their behaviour. |
| run1 D34 | Agreement reads (`GET /agreements/{aid}`, `/file`, `POST /agreements/search`) | missed | (none) | (30a D5, D14 use `GET /agreements` as a read) | | File format and search semantics are not raised. |
| run1 D35 | Contract validity and expiry | found | F-016 | 30a D9 | differ | Both use the VC `validFrom`/`validUntil`. Run 1 sets `validUntil` to signing time plus P365D; 30a: absent means no limit. |
| run1 D36 | Both parties notified of the outcome (BP07) | missed | (none) | (none) | | Both runs send only two notifications; only run 1 records the BP07 gap. |
| run1 D37 | The state machine | found | F-004, D-005, D-006, D-007, D-009, F-018 | 30a D12 | differ | Run 1: 11 states with lifecycle. 30a: four built states (Requested, Signed, Active, Closed), the lifecycle states reserved. |
| run1 D38 | Business status versus OpenAPI wire status | found | F-004, D-007, F-032, D-023 | 30a D12 | agree | Same mapping: Requested = CREATED, Signed = FINALIZING, Active = FINALIZED, terminal = TERMINATED. Run 1 also maps Accepted to INITIATED; 30a keeps INITIATED for acknowledgements. |
| run1 D39 | Reasons and justifications | found | F-023 | 30a D15 | accepted risk | Not built in 30a; its proposal matches run 1 (mandatory, at most 1000 characters). |
| run1 D40 | Wrong party, wrong state, missing role | found | F-031, F-052 | 30a D2, E2, D15 | differ | Run 1: 401, 403 for a missing role, 409 for a wrong state. 30a: no user roles in the backends, so no 403; lifecycle not built. |
| run1 D41 | Who may answer a revocation | found | F-005, F-054 | 30a D15 | accepted risk | Not built; 30a's proposal gives the transitions but not who may answer. |
| run1 D42 | From which states the Provider may terminate | found | F-049 | 30a D15 | accepted risk | Not built; 30a's proposal matches run 1 (all established states). |
| run1 D43 | Contract consent status | found | F-047, D-025 | 30a D16 | accepted risk | Run 1 builds four values and an endpoint; 30a builds nothing. |
| run1 D44 | Expiry of unsigned offers | found | F-037, D-008 | 30a D13 | differ | Both: CM first, retry next run. Run 1: UIBE `TERMINATED`, CM `reason=EXPIRED` to Terminated. 30a: UIBE `EXPIRED`, plain reject to Closed. |
| run1 D45 | Effect of lifecycle on resource access | found | F-022 | 30a D15 | agree | Both: enforcement is outside the component, no connector call. |
| run1 D46 | Transport for lifecycle between parties | found | F-005 | 30a D15 | accepted risk | Run 1 uses a shared Kafka broker; 30a builds no channel. |
| run1 D47 | Lifecycle message | found | F-005 | 30a D15 | accepted risk | Follows from run1 D46. |
| run1 D48 | Counterparty message that does not fit | missed | (none) | (none) | | Arises only once a cross-party channel exists. |
| run1 D49 | Status event | environment | | | | Observability design. |
| run1 D50 | Ordering and delivery | environment | | | | Kafka settings. |
| run1 D51 | How tests observe events | environment | | | | Test setup. |
| run1 D52 | How the trust services are stubbed | environment | (F-025 touches the unsigned VC) | 30a E4 | | Stub design. |
| run1 D53 | VC format and proof | missed | (D-012 is about CDM ownership) | (none) | | OpenAPI example uses VC 1.1 and `Ed25519Signature2020`; no finding asks which data model version or proof suite. |
| run1 D54 | Test keys, DIDs, trust | environment | | | | |
| run1 D55 | Credential subject | decided only | (none) | 30a E8 | differ | Run 1 adds `resourceDescriptionId`, `usageContractHash`, `signerRole`, `signedAt`; 30a adds `contractOfferId`, `offerHash`, `signatureValue`. |
| run1 D56 | How a test produces an incorrectly signed VC | environment | | | | Test injection. |
| run1 D57 | Wallet errors | found | F-006, F-007, F-024 | 30a D6 | differ | Both: no user prompt, story messages. Run 1: contract to Failed, 500 means corrupted, other errors unavailable. 30a: 500 response, record not moved, other errors mean corrupted. |
| run1 D58 | What each wallet holds | found | F-024 | 30a D6, D10, D8 | differ | Run 1: both VCs in each wallet. 30a: own VC in the wallet; the Provider VC is kept with the Consumer record. |
| run1 D59 | Signer and issuer failures | found | F-012, F-034 | 30a D5, D7 | differ | Run 1: async, Failed with `SIGNING_FAILED`. 30a: HTTP 500, record unchanged. |
| run1 D60 | Contract record fields | found | D-016, D-018 | 30a D24 | differ | Run 1 lists 28 fields; 30a gives no field list and keeps external data as ids. |
| run1 D61 | Audit trail | found | F-029 | 30a E7, D1 | differ | Run 1: append-only history per record, shown in the API. 30a: logs only, "no separate audit store". |
| run1 D62 | Aggregated audit data | found | F-009 | 30a D17 | differ | Both add a count endpoint, package out. Run 1 counts 11 business states with `total` and `participantId`; 30a counts four wire values. |
| run1 D63 | Clause document upload | found | F-013, F-042, F-043, F-056 | 30a D18 | accepted risk | Not built; 30a defaults match run 1 (SHA-256, new id per upload, 413, 415). |
| run1 D64 | "Traceable in backend logs" | decided only | (none) | 30a E7 | differ | Run 1: review only, tests use the history. 30a: logging rules, no history. |
| run1 D65 | CM authentication | found | F-031 | 30a E2 | agree | API key, 401, no 403. |
| run1 D66 | UIBE authentication | found | F-052 | 30a E2, D2 | differ | Run 1: user JWT on user operations. 30a: API key only, users checked at the gateway. |
| run1 D67 | Identity provider contract | environment | | | | Keycloak realms and users. |
| run1 D68 | Roles per operation | found | F-024, F-052, F-047 | 30a D2 | differ | Run 1: `contract_manager`, `consent_manager`. 30a: no roles in the backends. |
| run1 D69 | Token from the other party | missed | (none) | (none) | | Arises only with per-party user tokens, which 30a does not have. |
| run1 D70 | Connector callback authentication | environment | (F-021) | 30a E4 | | Stub contract. |
| run1 D71 | Screens | found | F-044, F-046, F-051 | 30a D2, D14 | differ | Run 1 builds a UI; 30a builds no frontend and tests at UIBE. |
| run1 D72 | Test hooks | environment | | | | |
| run1 D73 | Buttons for actions that are not allowed | missed | (none) | (none) | | 32505 AC3 and 31118 AC1 "cannot click" not raised. |
| run1 D74 | User without the role | found | F-052 | 30a D2 | differ | Run 1: `access-denied` view. 30a: gateway concern, not tested. |
| run1 D75 | Rendering the human-readable HTML | missed | (none) | (none) | | HTML from another party shown in the UI; sanitising not raised. |
| run1 D76 | Topology, names, ports | environment | | 30a E1 | | Run 1 uses host ports 19101 and up; 30a the OpenAPI ports 8085, 8086. |
| run1 D77 | Keys and configuration | environment | | 30a E2, E6 | | |
| run1 D78 | Who builds stubs and realm files | environment | | | | |
| run1 D79 | Asynchronous timing | environment | | | | |
| run1 D80 | Fault scoping | environment | | | | |
| run1 D81 | Test data | environment | | | | |
| run1 D82 | Time-based rules without waiting | environment | | 30a E6 | | Run 1: admin trigger with `asOf`; 30a: short timeout and cron in tests. |
| run1 D83 | Seed data | environment | | | | |

Unnumbered run 1 scope statements (section 3) also have findings: the replaced pairs 19824/32509, 19829/32511, 22690/32505 (F-027, F-028, F-029, both runs keep the replacement and carry over error cases), the connector stories 35858, 35863, 35888 as client side only and 35889 out (F-021, agree), 21924 out except its CM part (F-050, F-008, agree). They differ on 31047 and 28846: run 1 builds them in part, 30a leaves them out (30a D16, D18).

## Missed

| Run 1 | Gap | Why the validators may have missed it |
|---|---|---|
| run1 D13 | Health response and failure code | The OpenAPI documents it and no story mentions it; there is no criterion to check. |
| run1 D15 | Date-time format without offset | A wire format detail inside OpenAPI examples; the 20b checks read criteria, not example values. |
| run1 D16 | Id formats (UUID versus pattern) | Schema-level inconsistency across operations; no rule compares parameter formats. |
| run1 D32 | `PATCH /agreements/{aid}/definitions/{did}/status` (finalisation) | No live story drives this operation; checks start from criteria, so an OpenAPI operation without a story stays unseen. |
| run1 D34 | Agreement file format and `POST /agreements/search` | Same reason: OpenAPI operations no criterion uses. |
| run1 D36 | BP07 "both parties are notified" | A business process outcome; 20b checks story criteria and 23b checks terms, neither checks BP outcomes against stories. |
| run1 D48 | Counterparty message that does not fit | Only exists inside run 1's own channel design; the validators stopped at the missing channel (F-005). |
| run1 D53 | VC data model version and proof suite | A standards question inside an OpenAPI example; D-012 looks at the CDM class, not the credential format. |
| run1 D69 | Token from the other party | Only exists with per-party user tokens, a run 1 design choice. |
| run1 D73 | Disallowed buttons absent or disabled | UI rendering detail; the criteria read as clear to the checks. |
| run1 D75 | Sanitising the human-readable HTML | A security concern no criterion states; the SEC rules are applied in 30a, not in 20b or 23b. |

The four `decided only` rows (run1 D12, D21, D55, D64) show the same pattern: 30a filled them as environment choices or side notes, without a finding.

## New in this run

Blockers and majors that match no run 1 decision or scope statement.

| Finding | Severity | What it raises | 30a |
|---|---|---|---|
| F-007 | blocker | SIMPL-19938 holds two live versions of AC1 to AC4 with different actors. | 30a D6: the system-level headings count. |
| D-001 | blocker | The main object has four names (usage contract, contract agreement, contract, usage contract agreement). | 30a D19: "usage contract" as the term, agreement names stay on the wire. |
| F-048 | major | No way for the Provider to decline a reactivation request; the contract can stay in Reactivation requested. Run 1's lifecycle table has the same hole. | 30a D15: proposal adds Reactivation requested back to Suspended. |
| D-009 | major | "Signed usage contract" used for both the one-signature offer and the two-signature contract. | 30a D12: Signed (offer) versus Active (contract). |
| D-010 | major | "validate" and "verify" used for the same check, and "verify" also for BP 7.2. | 30a D22: "validation" for the signed offer check. |
| D-012 | major | CM CDM models `VerifiableCredential` as its own class, unlike the VC Issuer and Wallet CDMs. | 30a D24: external, owner VC Issuer. |
| D-013 | major | External CDM classes name no owner and use other names than their owners. | 30a D24. |
| D-018 | major | All three CDMs are in review, thin, and point to old repositories. | 30a D24: owner work, does not block. |

Five of the eight are terminology or CDM findings from 23b. Run 1 had no domain check, so it named things for the build but did not raise the naming itself.

## Later additions in the digest (run 1.1, counted separately)

| Id | Topic | Findings | 30a | Note |
|---|---|---|---|---|
| C1 | A duplicate does not overwrite the stored result | F-017 | 30a D9 | 30a: the duplicate changes nothing and gets 409. |
| C2 | Credential ids in `GET /contracts/{nid}` | (none) | (none) | Depends on run 1's read model. |
| C3 | Timing of connector callback history entries | (none) | (none) | Depends on run 1's history. |
| C4 | Trigger `CONNECTOR` for async work | (none) | (none) | Depends on run 1's history. |
| C5 | After accept, any later status satisfies 32505 AC2 | F-045, F-019 | 30a D5, D14 | 30a signs inside the call, so the offer reads ACCEPTED and the record Signed. |
| C6 | Overview column labels in story wording | F-044 | 30a D14 | 30a maps the story columns to `ContractOffer` fields. |
