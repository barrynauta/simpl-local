# Findings: Contract Management

Fourth factory candidate, chosen as the non-common component with the most business logic: two parties negotiate, sign, validate, store and then manage usage contracts, around a signer, a wallet, a VC issuer and a connector that are not part of it.

## 2026-10-29: step 0, input set

36 live stories of the Jira component "Contract Management", collected locally (35 by the model, 1 plain fallback, all with a word match of 1.000), three OpenAPI files and BP07 and BP10. Details: [`specs/`](./specs/README.md).

- **No published event contract.** Offers and signed contracts pass between the parties over Kafka and the connector; the topic names exist only in code.
- **Replaced stories are still live** next to their replacements (three pairs), and two stories share one title.
- **Few stories are settled:** 5 of 36 are ready for implementation check.
- **The trust services are outside the input:** signing or the wallet is in the title of 11 stories, but no signer, wallet or VC issuer contract is published with them.

## 2026-10-29: step 1, spec digest

83 decisions ([`spec-digest.md`](./spec-digest.md)), as many as the Catalogue, plus five appendices: 11 operations no OpenAPI has, a stub contract, Kafka messages, test keys and a proof algorithm with a worked vector. 28 of 36 stories in.

The five biggest gaps, by impact on buildability:

1. **No event or callback contract** between the parties and the connector.
2. **No signer, VC issuer or wallet contract, and no format for the contract credential.**
3. **No lifecycle API and no state machine.** Suspend, reactivate, revoke, terminate and consent have UI stories but no API; three partial status lists disagree.
4. **The newer establishment stories contradict the OpenAPI**: which party runs `/create`, the name of `/issue`, methods and status codes, and whether the Provider credential is sent or fetched.
5. **No user identity or roles** on the UI backend: the OpenAPI declares only an API key.

## 2026-10-29: step 2, test writer

383 API scenarios and 38 UI scenarios (`@ui @wip`), from the specs and the digest only. Every state machine row has its own tag. Baseline on the local upstream stack (one party, no stub, no Kafka, no Keycloak; its `start.sh` had fast-forwarded the backend from the June clone to `63c6749`, 7 Aug 2026): 1 of 93 runnable scenarios passes, 290 skipped by tag. Kept failing as real differences: other problem type names (41), the operations the digest added (25), the existence check without an `exists` field (9), and smaller points. Details: [`tests/`](./tests/README.md).

## 2026-10-29: step 3, code writer (run 1)

Built from the specs and the digest only, in an isolated workspace: the contract manager backend and the contract UI backend (Java 21, Spring Boot 3), a Vue 3 UI per party, one stub service for connector, signer, VC issuer, wallet and catalogue, Keycloak with a realm per party, Kafka and PostgreSQL. Finish line met: clean start, 31 of 31 smoke checks (a whole negotiation to an active contract at both parties), `mvn verify` green (61 tests). The code writer listed 23 new spec gaps in `BUILD-LOG.md`, among them two digest decisions that contradict each other (D20 and D65 on 403; D27 and D28 on the stored result of a duplicate).

**Suite on the build: 339 of 345 API scenarios** (the 38 UI scenarios wait for step 4). Two test-writer bugs were fixed first: a Kafka client option that the installed library does not know (16 scenarios had never run against a live Kafka) and a placeholder used before it was set (1).

The 6 remaining failures:

| Scenarios | Reading |
|---|---|
| 1 | **Build deviation.** P1 says the Provider move to SIGNED is triggered by the connector; the build records `SYSTEM` |
| 2 | Digest ambiguity: D28 says a replay gets "result IGNORED" without saying whether that replaces the stored result. The build stores `IGNORED`, the tests expect the earlier result to stay |
| 1 | Digest ambiguity: D61 lists `CONNECTOR_CALLBACK` history entries without saying when they are written. The build writes one per callback, the test expects none after the existence check |
| 2 | Digest ambiguity: D60 lists `ownCredentialId` and `counterpartyCredentialId` as record fields; A.3, the detail response, does not list them. The build follows A.3 |

**Contamination check:** no class name is shared with the upstream code (62 build classes, 111 upstream). 18 of 226 string literals are shared; 17 are in the specs, the last is the column name `contract_negotiation_id`, derived from the spec field `contractNegotiationId`.

### How to read 339 of 345

As for the earlier candidates, both writers used the same decision list. The result shows that the component is buildable from the published specs **plus 83 written decisions and five appendices**, most of them about what lies between the parties. The 5 ambiguous scenarios show where even a long decision list still leaves room: two writers read the same sentence differently. Tightening D28, D60 and D61 would close them.

## 2026-10-29: step 4, UI browser steps

The 38 UI scenarios made runnable with Playwright by a fresh agent, which saw the rendered pages only, not the code. Locators: story wording first, then the routes and `data-testid` values of the digest (D71 to D75), then tolerant patterns.

- **Factory build: 36 of 38.** API rerun after the change: 340 passed, 5 failed. The D61 history scenario of step 3 passes some of the time: the build writes the connector callback entry asynchronously, so its count depends on timing.
- **Failures:** after "accept", the page shows "Signed", not "Accepted" (32505): signing starts at once (D24), so ACCEPTED lasts a moment and neither story nor digest says what the page shows. The empty overview names its columns "Negotiation id ... Counterparty" where 30959 says "Contract negotiation ID ... Provider name": an implementation deviation from the story wording, which D71 did not fix.
- **Local upstream: not run.** Its backend did not boot under `start.sh` (a setting the August version needs), and every UI scenario sets up its contract through a stub and a UI backend that upstream does not have.
- **Story wording too vague for a strict locator:** no UI story quotes a button, field or dialog label; "cannot open the contract manager UI" and "redirected to the consent manager" needed tolerant checks; "no external resources" in the rendered contract (D75) has no definition.

## 2026-10-29: step 5, swap experiments

Three joints crossed, configuration only. Details and commands: [`experiments/swap/`](./experiments/swap/README.md).

- **API walk, 20 calls over the OpenAPI operations: 13 give the same status**, and an agreement has the same 10 fields on both. The differences: an OpenAPI operation upstream has not built (`POST /agreements/search`), rules the contract does not state (status change, verification without a body), and undocumented codes (upstream answers 404 on the offer operations, the digest chose 400 because the OpenAPI lists none).
- **The upstream UI read page works on the build**: a contract the build negotiated end to end shows with all its fields.
- **The events between the parties share nothing.** Upstream runs establishment over Kafka request and response topics; the digest, with no event contract to follow, runs it over HTTP with the connector and uses Kafka for lifecycle and status only. A build party and an upstream party could not negotiate with each other.
- **Lesson:** where the OpenAPI speaks, the two are interchangeable; where it is silent, they drift; and the unpublished event contract is the real interoperability gap.

## 2026-10-29: step 6, PSO tests

The API tests of the PSO test team, run unchanged through a stand-in for the participant gateway. Details and commands: [`experiments/pso/`](./experiments/pso/README.md).

- **Build: 7 of 14. Upstream: 7 of 14, the same 7.** Build and upstream agree on every scenario.
- **None of the 7 failures is a build defect.** 3 expect 404 on bodies the OpenAPI calls invalid (both answer 400), 1 expects health behind the key (the OpenAPI says so, the digest chose open, upstream is open too), 1 tests a `GET` the OpenAPI does not have, 2 need IAA onboarding.
- **PSO's expectations differ from the running system here too.** As for the other candidates, the useful signals are for the spec owners: list the 404s PSO and the stories expect, and say whether health is behind the key.

## 2026-10-29: clarification round, run 1.1

The 8 failures of run 1 (6 API, 2 UI) each pointed at one unclear decision or one deviation. Six clarifications were added to the digest (section 7, C1 to C6): a replay keeps the stored validation result (C1); the detail response carries both credential ids (C2); connector callback history entries may come after the response, so tests do not count them (C3); a transition the state table attributes to the connector is recorded with that trigger (C4); after the accept the page may already show a later status (C5); the overview columns use the story wording (C6).

The code writer applied C1, C2, C4 and C6 in its isolated workspace (`BUILD-LOG.md`, "Run 1.1"); the tests took C3 and C5 (two .feature lines and one step, tagged `@C3` and `@C5`).

**Run 1.1: 383 of 383** (345 API, 38 UI). As for the Vocabulary Manager's D18, a small number of written clarifications closed every remaining gap between two writers who never saw each other's work. The build in `generated/run1/` is the run 1.1 state.
