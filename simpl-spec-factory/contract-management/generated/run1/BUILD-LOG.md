# Build log

Build of Contract Management from `input/specs/` and `input/spec-digest.md` only, on 29 Oct 2026. Nothing outside the workspace was read. Libraries came from Maven Central and npm, images from Docker Hub and quay.io (Keycloak).

## Result at the end

- `docker compose -p cm-factory down -v` then `docker compose -p cm-factory up -d --build --wait`: all ten containers healthy, about 30 seconds with a warm Maven cache.
- `./smoke.sh`: 31 checks, all pass (full negotiation to ACTIVE at both parties with two VCs in each wallet; incorrectly signed offer rejected with `INVALID_CONSUMER_SIGNATURE`; suspend and reactivate seen at both parties; expiry run; 401 and 403).
- `mvn verify`: green. 45 unit tests (common, CM), 9 CM integration tests, 5 UIBE integration tests, 2 stub integration tests.
- Extra live checks done by hand: existence check finds an open contract (`EXISTING_CONTRACT`), `DIRECTORY:NO_CONTACT` gives `NOTIFICATION_SKIPPED`, `WALLET:CORRUPTED` at the Consumer gives FAILED `WALLET_CORRUPTED` with the story message and the Provider goes TERMINATED, status events and notifications read from Kafka, correlation id echoed, authorization code flow with PKCE against the `provider` realm via curl.
- One headless Chromium run (Playwright, in a scratch folder outside the build) logged in at both UIs and used the D72 hooks: offer page, accept, overview filter, empty overview with header row, Provider suspend through the reason dialog (submit disabled while blank), user without role sees only `access-denied`. All as expected. This script is not part of the build.

## Spec gaps found while building (beyond the digest)

Each is a place where I had to guess. The choice is in brackets.

1. **403 on CM.** D65 says "CM never returns 403", D20 says `/create` returns 403 when the asset belongs to another provider. [D20 wins for `/create` only.]
2. **Duplicate detection versus the stored result** (closed by C1 in run 1.1). D28 detects a duplicate from the "last `validationResult`", D27 stores `IGNORED` on the record. If `IGNORED` is stored, a third trigger would no longer be a duplicate. [The record shows `IGNORED` and `DUPLICATE_OR_REPLAYED_SUBMISSION`; duplicate detection uses a hidden field with the last non-IGNORED result. A second trigger while a validation is still running is also treated as a duplicate.]
3. **History entry types.** D61 lists the entry types, but A.5 (consent change) and D33 (offer replaced, `APPROVED` or `CREATED` update) need an entry that is not a transition. [Added `CONSENT_STATUS_CHANGED` and `OFFER_UPDATE`, with `fromStatus` = `toStatus`.]
4. **Actor of accept and reject.** History needs the username (D61), but CM `PATCH /contractOffers/{nid}/{op}` has no body or field for it. [Optional header `X-Actor`, set by UIBE from `preferred_username`.]
5. **Existence check when neither id is the own id.** [400 `wrongParty`.]
6. **`/issue` on a record that is not REQUESTED and has no earlier result** (for example TERMINATED). [400 with type `invalidTransition`.]
7. **What makes `PUT /verifications` a 400 versus an asynchronous schema failure.** [400 only for a missing body, a non-object or an empty object. Everything else goes to validation and may end as `SCHEMA_OR_CONTENT_VALIDATION_FAILURE`.]
8. **Which VC fields are "required" for check (3).** [`@context`, `type`, `issuer`, `credentialSubject`, `validFrom`, `validUntil`, `proof`, and all eleven D55 subject fields as non-blank strings. `id` is not required.]
9. **`offerUrl` in the UIBE response** (B.5) needs the public UI URL, which is not a UIBE setting in D77. [Added `UI_PUBLIC_URL` to UIBE.]
10. **UIBE does not know its party role**, but B.1 maps `counterpartyName` by party. [UIBE filters per item, using the item's `partyRole`.]
11. **Stub choreography gaps.** C.2 does not say what happens when the step 1 search fails, when `/issue` does not return 201, or when the Provider VC cannot be fetched in step 5. [Step 1: `TERMINATED` `EXISTENCE_CHECK_FAILED`. Step 4: treated like a Provider rejection (Consumer `PUT .../update` REJECTED, `PROVIDER_REJECTED`). Step 5: Provider `PATCH .../status TERMINATED`, reason `PROVIDER_VC_MISSING`.]
12. **Order of 400, 404 and 409 on `POST /contracts/{nid}/lifecycleActions`.** [Body checks first (unknown action, missing or too long reason), then 404, then 409.]
13. **Problem type for 413 and for health 503.** Not in the D11 list. [413 uses `invalidDocument`; health 503 uses `internalServerError`, as in the UIBE OpenAPI example.]
14. **`reasonCode` on lifecycle transitions** (L1 to L7). [Not set; the reason text is in history and the message.]
15. **`actor` on the receiving side of a lifecycle message.** D.2 shows `actor: null` for `COUNTERPARTY`. [Null in history and event.]
16. **Expiry run without ids.** "skipped" is defined for "selected ids". [Without ids, only CREATED offers are selected; those not old enough are skipped.]
17. **Agreement file.** Trailing newline not specified. [None.]
18. **Optional reasons in the UI.** D71 opens a dialog for actions that need a reason. [REACTIVATE and ACCEPT_REVOCATION run directly, without a dialog and without a reason.]
19. **Signature date precision.** [VC `signedAt`, `validFrom`, `validUntil` and the signature dates are whole seconds.]
20. **Correlation id on stub calls.** C.2 sets `X-Correlation-Id` = nid. [So history entries for stub-driven steps carry the nid as correlation id.]
21. **Consent status on Consumer records.** D43 sets the initial value at creation; A.5 allows changes on Provider records only. [Consumer records also get `PENDING` or `NOT_DEFINED`, never changed.]
22. **Status event for a consent change** has no new status. [`previousStatus` = `newStatus`.]
23. **Stub catalogue defaults.** C.6 defines defaults only for `resourceDescriptionId` and `consentRequired`. [Also `resourceName` = asset id, `providerId` = `provider-01`, `providerName` from the directory, empty `description`, `policies`, `contractClauses`.]

## Problems met and how they were solved

1. **H2 and `byte[]`.** Hibernate mapped the clause document content to `BLOB`, which H2 in PostgreSQL mode rejects. Fixed with `columnDefinition = "bytea"` (works on both).
2. **Test helper named `status()`** hid the static MockMvc `status()` matcher. Renamed.
3. **Mockito stubbing while a background thread used the mock** gave `WrongTypeOfReturnValue`. Switched to `doReturn(...).when(...)` in those places.
4. **Shared test database**: a second provider test found the first test's open contract for the same asset (correct behaviour). Each test now uses its own asset id.
5. **Failsafe**: `*IT` classes did not run under `mvn verify` until the failsafe plugin was bound in the parent pom.
6. **UUID case.** A first version lower-cased path UUIDs; removed so ids are stored and echoed as given.
7. **Test tooling only**: Python's `http.cookiejar` does not keep cookies for `localhost`, so a scripted login failed with 400. The same flow with curl worked; not a defect of the build.

Fix iterations: about seven, all listed above. The compose stack and the smoke test passed on the first full run; no fix was needed on the running system.

## What does not work or is weak

- Integration tests use H2 and mocks, not Testcontainers. Postgres-specific and Kafka behaviour is only covered by `smoke.sh` and manual checks.
- No outbox: status events and lifecycle messages are published after commit. If Kafka is down at that moment, the message is lost (logged).
- The in-flight guard for duplicate validation is in memory, per CM instance.
- No frontend unit tests. The UI was checked once in a headless browser.
- The UI header (username, sign out) is still visible next to `access-denied` for a user without the role.
- The stub keeps all state in memory; restarting the `stubs` container loses wallets, faults and negotiations, while CM records stay.
- The scheduled expiry run at 01:00 UTC was not observed; only the manual trigger was tested.

## Run 1.1 (clarifications C1 to C6)

- **C1**: a duplicate trigger no longer writes `IGNORED` to the record. `validationResult`, `validationReasonCode` and `validatedAt` keep the last real result; the duplicate shows only as a `VALIDATION` history entry with `DUPLICATE_OR_REPLAYED_SUBMISSION` and the text "Validation result IGNORED". The hidden `effectiveValidationResult` field was removed (the old column stays in an existing database, unused).
- **C2**: `GET /contracts/{nid}` returns `ownCredentialId` and `counterpartyCredentialId`, null until set. UIBE passes them through unchanged.
- **C3**: no change; callback entries were already written after the callback.
- **C4**: the Provider REQUESTED to SIGNED (P1) and the FAILED outcomes of `/issue` (P2) and of `PUT /verifications` (E10) now have trigger `CONNECTOR` instead of `SYSTEM`. Consumer signing after the accept (E5, E6) keeps `SYSTEM`, because the D37 table names "system" as the actor there.
- **C5**: no change.
- **C6**: overview column headers and the matching filter labels changed to the story wording; the counterparty header depends on the party of the UI.
- Tests: `ProviderFlowIT` now checks that a duplicate leaves the stored result and `validatedAt` unchanged and writes the history entry, that P1 and the P2 failure carry `CONNECTOR`, and that both credential ids are present. `mvn verify` green (61 tests). Stack rebuilt with `up -d --build --wait`; `smoke.sh` passes. The C6 headers were checked only by a successful UI build, not in a browser.
- Unclear: C4 says "every transition the table attributes to the connector". I read the actor column literally, so E5 and E6 (actor "system") were left as `SYSTEM`.
