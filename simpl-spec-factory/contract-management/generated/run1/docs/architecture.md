# Architecture

## Component map

```
                 Provider party (provider-01)                      Consumer party (consumer-01)
  ui-provider --> uibe-provider --HTTP--> cm-provider        cm-consumer <--HTTP-- uibe-consumer <-- ui-consumer
   (nginx,          (Spring Boot,          (Spring Boot,      (same image)          (same image)       (same image)
    Vue 3)           JWT + API key)         API key)
                          ^                  |   ^                 |   ^               ^
                          | POST             |   | HTTP            |   |               | POST /contractOffers/{nid}
                          | /contractOffers  v   |                 v   |               |
                          |            +------------------------------------+         |
                          +------------| stubs: connector (both sides),     |---------+
                                       | signer + VC issuer, verifier,      |
                                       | wallets, catalogue, directory,     |
                                       | consent page, control endpoints    |
                                       +------------------------------------+
              cm-provider <------------- kafka (one broker, KRaft) ------------> cm-consumer
                         simpl.contract.lifecycle.v1, simpl.contract.status.v1, notifications

  keycloak: realms provider and consumer      postgres: cm_provider, cm_consumer, uibe_provider, uibe_consumer
```

| Concern | Where |
|---|---|
| Contract record, state machine, validation, signing orchestration, history, audit counts, clause upload | CM (`contract-manager/`) |
| Offer table, offer expiry scheduler, user API with token and role checks | UIBE (`contract-ui-backend/`) |
| Screens | UI (`ui/`), calls only its own UIBE through nginx |
| Connector, signer, VC issuer, verifier, wallet, catalogue, directory | Stub (`stubs/`) |
| Proof algorithm, JCS, did:key, problem JSON | `common/` |

## Inside CM

- `web/CmController`: all OpenAPI operations and the added operations of Appendix A. JSON bodies are read by hand (`common/.../JsonBody`) so that 400 and 415 follow D11 exactly, also for the optional body of `/issue`.
- `web/ApiKeyFilter`: `X-Api-Key` on everything except `/health` (D65).
- `service/EstablishmentService`: the synchronous part of BP07 (search, create, accept or reject, issue, verifications, agreement status, offer transfer and update).
- `service/SigningService`: the asynchronous part: Consumer signing (D24), Provider validation and signing (D26 to D29), Consumer verification (D31), duplicates (D28), wallet errors (D57).
- `service/CredentialValidator`: the one validation algorithm of D27 for both sides. It has no Spring dependency; the verifier and the directory are passed in as ports.
- `service/LifecycleService`: L1 to L7, lifecycle messages out and in (D47, D48), consent status (A.5), `allowedActions`.
- `service/ContractService`: every record change goes through here; it writes the history entry (D61) and publishes the status event after commit (D49).
- `domain/`: `ContractRecord` (D60, with optimistic locking), `HistoryEntry`, `LifecycleAction` (the L table as data), `ContractStatus` (D37 sets and the D38 mapping).
- `events/`: Kafka topics (created at startup), publisher, lifecycle listener (group `cm-<participantId>`).
- `clients/`: `StubClient` and `UibeClient`. Every call to the stub carries `X-Contract-Negotiation-Id` (D80) and `X-Correlation-Id` (D14).

Background work runs on virtual threads after the database transaction commits, so a callback never reaches the stub before the state it reports is visible.

## Inside UIBE

- `SecurityConfig`: OAuth2 resource server. Tokens are checked against the JWKS of the own realm and must carry the own realm issuer (`http://localhost:19080/realms/<realm>`), else 401.
- `ApiKeyFilter`: `x-api-key` on `POST /contractOffers/{nid}` and `/admin/**`.
- `UserContext`: `participant_id` must equal the own participant id, and `contract_manager` must be in `realm_access.roles`, else 403.
- `OfferService`: the offer table, accept and reject (D23), expiry (D44, B.4). `ExpiryScheduler` runs it on `OFFER_EXPIRY_CRON`.
- `UibeController`: passes the contract reads and lifecycle actions to CM, adds the offer fields and the consent link, and removes `UPDATE_CONSENT` for users without `consent_manager`.

## The UI

One image, configured per party by environment (`UI_REALM`, `UI_PARTY`, `UIBE_UPSTREAM`, `KEYCLOAK_URL`). nginx renders `/config.json` from these at start. Login is the authorization code flow with PKCE through `oidc-client-ts`, client `contract-ui`. Routes: `/` (overview), `/contracts/<nid>` and `/contractOffers/<nid>` (details). The human-readable offer is sanitised with DOMPurify before it is rendered.

## The stub

In-memory state. It implements C.1 (connector callbacks, `vcExchange`), C.2 (one virtual thread per negotiation, acting as both connectors), C.3 (issue and verify with the keys of Appendix E.1), C.4 (wallet), C.5 (catalogue, directory, consent page) and C.6 (control endpoints). Faults and overrides are kept per nid.

## Establishment flow (happy path)

1. Test: `POST /_control/negotiations`.
2. Stub to CM-P `POST /agreements/{nid}/search`. CM-P creates its record (REQUESTED) and calls `/isPresent/{nid}/false`.
3. Stub to CM-C `POST .../create`. CM-C reads the catalogue and directory, builds the HTML and hash, posts the offer to UIBE-C, stores REQUESTED, publishes the "offer available" email.
4. User accepts in UI-C. UIBE-C to CM-C `PATCH /contractOffers/{nid}/accept`: ACCEPTED. CM-C issues its VC, stores it in its wallet, goes to SIGNED, calls `/signed/{nid}/true`.
5. Stub to CM-P `POST .../issue`. CM-P fetches the Consumer VC through `/vcExchange`, validates it (D27), calls `/isValid/true`, issues its own VC, stores both VCs, goes to SIGNED, calls `/signed/true`.
6. Stub fetches the Provider VC from CM-P and sends it to CM-C `PUT /verifications/{nid}`. CM-C validates, stores the Provider VC, goes to ACTIVE, calls `/isValid/true`.
7. Stub to CM-P `PATCH /agreements/{aid}/definitions/{did}/status` `FINALIZED`: CM-P goes to ACTIVE.

## Lifecycle after establishment

A user action at one party goes UI, UIBE, CM (`POST /contracts/{nid}/lifecycleActions`). CM checks the L table, changes its record, and publishes a lifecycle message to `simpl.contract.lifecycle.v1` with the counterparty as recipient. The other CM applies the same transition with trigger `COUNTERPARTY`, or writes `COUNTERPARTY_MESSAGE_REJECTED` when it does not fit.
