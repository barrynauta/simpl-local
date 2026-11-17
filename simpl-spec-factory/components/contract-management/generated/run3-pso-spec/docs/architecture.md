# Architecture overview

## Component map

```
                 Provider party                                     Consumer party
  UI-P (nginx) --> UIBE-P --HTTP--> CM-P                    CM-C <--HTTP-- UIBE-C <-- UI-C (nginx)
                     ^               |  ^                     ^  |            ^
                     |  POST offers  |  | HTTP           HTTP |  |            | POST /contractOffers
                     |               v  |                     |  v            |
                     |        +----------------------------------------+      |
                     |        | stubs: connector (both sides) + C.2    |      |
                     |        | choreography, VC issuer and verifier,  |      |
                     |        | wallet, catalogue, directory, consent  |      |
                     |        +----------------------------------------+      |
                     |                                                        |
           CM-P <----+------------------ Kafka (one broker) ------------------+---> CM-C
                     simpl.contract.lifecycle.v1, simpl.contract.status.v1, notifications

  Keycloak: realm "provider" (UI-P, UIBE-P) and realm "consumer" (UI-C, UIBE-C)
  PostgreSQL: cm_provider, cm_consumer, uibe_provider, uibe_consumer
```

The same CM image runs at both parties; it knows its side from `CM_PARTICIPANT_ID` (D2). The same holds for
UIBE (`UIBE_PARTICIPANT_ID`, `UIBE_PARTY_ROLE`) and the UI (realm and party name from `/config.js`).

## The two parties

- **Provider** (`provider-01`): CM-P answers the existence check, validates the Consumer VC, signs, finalises
  (`PATCH .../status`). Its users suspend, reactivate, terminate, and manage consent.
- **Consumer** (`consumer-01`): CM-C prepares the offer (`/create`), sends it to UIBE-C, sends the email, signs
  after the user accepts, verifies the Provider VC (`PUT /verifications`). Its users accept or reject offers
  and request reactivation.
- Both: request revocation, answer a revocation request of the other party, see the overview and details.

## Inside CM

- `web`: API key filter (first check, D84 step 1), health, controllers. Path formats are checked in the
  controllers (step 2), body syntax and media type by Spring (step 3), then the service checks record, fields,
  party and state in that order (steps 4 to 7).
- `service/EstablishmentService`: the E and P transitions of D37 (BP07). Signing and validation run on a
  background pool after the HTTP response.
- `service/CredentialValidator`: the D27 algorithm, the same at both sides.
- `service/LifecycleService`: the L transitions, counterparty messages, consent, reads, audit counts.
- `service/ContractStore`: transactional primitives. Every transition writes one history entry and one status
  event; events leave after commit.
- `events`: Kafka publisher, topic creation and the lifecycle listener (group `cm-<participantId>`).
- `client`: HTTP clients for the stub and UIBE. Every stub call carries `X-Contract-Negotiation-Id` and
  `X-Correlation-Id`.

## Inside UIBE

- Two Spring Security chains: machine operations (`POST /contractOffers/{nid}`, `/admin/**`) need `x-api-key`;
  everything else except `/health` needs a JWT from the own realm with `contract_manager` and the own
  `participant_id`.
- Offer table (status CREATED, ACCEPTED, REJECTED, TERMINATED), offer decision, expiry run and scheduler.
- Read and lifecycle calls are passed to the own CM; CM errors come back with the same status code.

## Stub

Spring Boot, in-memory state. It implements Appendix C, signs VCs with the Appendix E keys, and drives the
negotiation choreography of C.2 on a thread per negotiation.

## UI

Vue 3 single page app with keycloak-js (authorization code with PKCE). Routes `/`, `/contracts/<nid>`,
`/contractOffers/<nid>`. All D72 test ids are present. The human-readable offer is sanitised with DOMPurify.
