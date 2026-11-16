# Build log

Build of Contract Management from `input/` only (digest, OpenAPI, stories, BP07, PSO scenarios). Date: 16 Nov 2026.

## Result at the end

| Finish line item | Result |
|---|---|
| `docker compose -p cm-factory-pso up -d --build --wait` from a clean state (`down -v` first) | Succeeds, all 11 services healthy |
| `smoke.sh` | 33 of 33 checks pass (also on a second run against the same data) |
| `pso-check.sh` | 8 of 8 PSO scenarios pass |
| `mvn verify` | Green: 63 tests (52 CM, 6 UIBE, 5 stub) |
| Stack left running | Yes |

## PSO scenarios versus the digest

I found no disagreement. All 8 scenarios match the revised rows (D11, D12, D13, D25, D65, D84, D85). The 401
comes from the CM API key filter, before any other check, with a problem body that conforms to
`pso/schemas/error-response.json`. `instance` is the request path.

## Spec gaps where I had to choose (new findings beyond the digest)

1. **`/issue` on a record that is not REQUESTED and has no earlier validation** (for example TERMINATED). No
   row says what happens. I answer 400 `invalidTransition` (step 7 of D84; 400 because it is an OpenAPI operation).
2. **`PUT /contractOffers/{nid}/update` with `REJECTED` on a record that is not REQUESTED, ACCEPTED or SIGNED.**
   D33 lists only the allowed sources. I answer 400 `invalidTransition`.
3. **History entry type for non-transition events.** D61 has no entry type for "offer status APPROVED or
   CREATED" (D33) or for a consent change (A.5, "writes a history entry"). I use `TRANSITION` with
   `fromStatus` = `toStatus` and reason codes `OFFER_STATUS_<value>` and `CONSENT_STATUS_CHANGED`. The consent
   change uses trigger `SYSTEM`.
4. **Who accepted the offer.** CM `PATCH /contractOffers/{nid}/{op}` has no body, so CM cannot learn the user
   for the history actor. UIBE sends an extra optional header `X-Actor` with `preferred_username`. Without it
   the actor is `null`.
5. **UIBE needs its party role and the UI URL.** B.1 maps `counterpartyName` by party, and B.5 returns an
   `offerUrl`, but D77 gives UIBE neither. I added `UIBE_PARTY_ROLE` and `UI_PUBLIC_URL` to UIBE.
6. **Required VC fields in D27 check 3.** "A required field of D53 or D55" is not a list. I require
   `@context`, `type`, `id`, `issuer`, `validFrom`, `validUntil`, `credentialSubject`, `proof`, all 11 subject
   fields of D55 as non-blank strings, and parseable dates (`validFrom`, `validUntil`, `signedAt`).
7. **`PUT /verifications/{nid}` with body `{}`.** The OpenAPI says the VC is required. I accept `{}` as "no VC"
   and the validation gives REJECTED `MISSING_CONTRACT_DATA` (D27 check 2). A non-object body is 400. The stub
   sends `{}` when `providerVcOverride` is `{"notFound": true}`, because C.2 step 5 does not say what to send then.
8. **A second trigger while a validation is still running.** D28 covers only finished results. I treat it as a
   duplicate (IGNORED).
9. **Stub choreography failures not in C.2.** When the existence check call itself fails, or a participant is
   unknown, the stub ends with `TERMINATED` and reasons `SEARCH_FAILED` or `UNKNOWN_PARTICIPANT`. When
   `/issue` does not answer 201, the stub treats it like a Provider rejection (step 4 reject path).
10. **vcExchange for a nid the stub did not start.** C.1 says the stub calls "the counterparty CM". Without a
    negotiation the stub takes the other of `provider-01` and `consumer-01`.
11. **`GET /agreements/search`.** It also matches `GET /agreements/{aid}` with aid `search`. I answer 405,
    because `/agreements/search` is its own resource (POST only).
12. **Accept and reject buttons on the overview** (30959 AC1). D71 and D72 put the action hooks on the details
    page; one `action-accept` per row would make the test ids ambiguous. The overview has a details link per
    row; accept and reject are on the details page only.
13. **Actions with an optional reason** (REACTIVATE, ACCEPT_REVOCATION) run at once in the UI, without the
    reason dialog. D71 says the dialog opens for "an action that needs a reason".
14. **`reason` query parameter on CM PATCH** other than `EXPIRED`, or with `accept`: 400 `argumentTypeMismatch`.
15. **Directory or catalogue down during `/create`.** Names become `null` when the directory fails; a
    catalogue error other than 404 gives 500.
16. **Expiry run without `contractNegotiationIds`.** B.4 does not say whether `skipped` then lists every
    ineligible offer. It does.
17. **Actor on the receiving side of a lifecycle message.** History and status event use `actor: null` (as the
    D.2 example), the reason is kept.
18. **Lifecycle message publish failure.** No source covers it. The local transition stays; the error is only
    logged, so the parties can disagree. No retry or outbox.

## Problems met and how I solved them

1. **PATCH from Java clients returned 502.** `HttpURLConnection` (Spring `SimpleClientHttpRequestFactory`)
   does not support PATCH, so UIBE could not call CM `PATCH /contractOffers` and the stub could not call
   `PATCH .../status`. Switched all three clients to the JDK `HttpClient` (`JdkClientHttpRequestFactory`).
2. **UI proxy reached the wrong container.** After a rebuild, nginx kept the old IP of `uibe-consumer`, which
   by then belonged to another Tomcat, so `/contract-ui/v1/...` gave a Tomcat 404. The nginx template now uses
   the Docker resolver with a variable upstream, so the name is resolved again.
3. **Test compile error** from a helper named like the static `status()` matcher. Renamed.
4. **Keycloak realm import.** I removed references to `default-roles-<realm>` from the users to avoid an
   import order problem; tokens carry only the D67 roles. `firstName` and `lastName` are set so that the
   password grant does not hit the "account not fully set up" required action.

Fix iterations after the first full start: 4 (the four items above). The stack started healthy at the first
`up`; the Appendix E vector reproduced at the first run.

## What does not work or is weak

- **UI tests:** only a small unit test of the labels (`vitest`). The UI was checked once by hand in a browser
  as `consumer-admin`: sign-in with PKCE, overview with the D72 hooks, details with the offer HTML, accept
  (status moved to Signed, buttons gone). The reason dialog, the Provider UI and `access-denied` were not
  checked in a browser.
- **Stub state is in memory.** Restarting `stubs` loses negotiations, faults, overrides and wallets; a
  negotiation waiting for the user at step 3 is gone after a restart.
- **Kafka listener:** no retry or dead letter topic; a message that throws is logged and skipped.
- **Schema management:** Hibernate `ddl-auto: update`, no migrations.
- **Third-party images** (PostgreSQL, Kafka, Keycloak) run with their own users; only the four images of this
  build were made non-root (Java services as uid 10001, nginx as uid 101).
- **D79 timings** were not measured formally; in the smoke run every asynchronous step finished in well under
  a second.
- Nothing was cut: contract clause upload (28846) and the consent link (31047) are implemented.
