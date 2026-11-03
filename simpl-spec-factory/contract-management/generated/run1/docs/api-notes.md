# API notes

The OpenAPI files in `../input/specs/api/` describe the documented operations. This page lists how they were built, with the digest amendments, and the operations that were added.

## Common rules

- Errors: always `application/problem+json` with `type`, `title`, `status`, `detail`, `instance` (D11). The body is written directly to the response, so it also arrives when `Accept` excludes JSON (406).
- `X-Correlation-Id`: accepted, generated when absent, echoed on every response, sent on outbound calls, put in Kafka headers (D14).
- Dates: OpenAPI-documented responses use UTC local date-time without offset (`2026-10-29T10:00:00.123`); added operations, events and VCs use RFC 3339 with `Z` (D15).
- Ids: `contractAgreementId` in paths must be a UUID (else 400 `argumentTypeMismatch`); other ids follow `^[A-Za-z0-9:-]+$`, 1 to 100 characters (D16).
- Unknown ids on operations without a documented 404 give 400 `validationFailure` with detail `Unknown contractNegotiationId` or `Unknown contractAgreementId` (D12).

## CM (`/contract/v1`, header `X-Api-Key`)

| Operation | Party | Response | Notes |
|---|---|---|---|
| `GET /health` | any | 200 actuator JSON, 503 problem | no auth |
| `POST /agreements/{nid}/search` | any (stub calls Provider) | 200 `{contractAgreementId, contractDefinitionId, exists, contractSearchTO}` | creates the Provider record; then `/isPresent` callback |
| `POST /credentials/agreements/{aid}/definitions/{did}/create` | Consumer | 201 with the 32511 fields | 403 asset of another provider, 404 asset unknown, 400 `alreadyExists`, 500 if UIBE fails |
| `PATCH /contractOffers/{nid}/{accept or reject}` | Consumer | 200 `{contractNegotiationId, status: INITIATED}` | query `reason=EXPIRED` (reject only) gives TERMINATED `OFFER_EXPIRED`; optional header `X-Actor` (set by UIBE) |
| `POST /credentials/agreements/{aid}/definitions/{did}/issue` | Provider | 201 `INITIATED` | body optional; validation and signing are asynchronous |
| `PUT /verifications/{nid}` | Consumer | 200 `INITIATED` | body is the Provider VC; record must be SIGNED |
| `PATCH /agreements/{aid}/definitions/{did}/status` | Provider | 200 with technical status | `FINALIZED` from SIGNED, `TERMINATED` from REQUESTED or SIGNED |
| `GET /agreements/{aid}` | any | 200 `ContractAgreementData` | technical status (D38) |
| `GET /agreements/{aid}/file` | any | 200 `text/plain`, seven lines | 404 until both signature dates exist |
| `POST /agreements/search` | any | 200 `{items: [{contractAgreementId, consumerId}]}` | established contracts only |
| `POST /contractOffers/{nid}` | Consumer | 200 `INITIATED` | replaces the offer HTML and hash while REQUESTED, forwards to UIBE |
| `PUT /contractOffers/{nid}/update` | Consumer | 200 `INITIATED` | `REJECTED` gives FAILED `PROVIDER_REJECTED`; `SIGNED` and others are 400 |
| `GET /verifications/{nid}/vc` (A.1) | any | 200 own VC, 404 | |
| `GET /contracts` (A.2) | any | 200 `{items}` | filters `contractNegotiationId`, `resourceName`, `providerName`, `consumerName`, `status` (comma list) |
| `GET /contracts/{nid}` (A.3) | any | 200 details with `allowedActions` and `history` | |
| `POST /contracts/{nid}/lifecycleActions` (A.4) | any | 200 details | 400 bad action or reason, 404, 409 `invalidTransition` |
| `PUT /contracts/{nid}/consentStatus` (A.5) | Provider | 200 details | 409 when not a Provider record, terminal or `NOT_DEFINED` |
| `GET /audit/contractStatusCounts` (A.6) | any | 200 counts per status | |
| `POST /contractClauseDocuments` (A.7) | any | 201 metadata | multipart part `file`; 400 empty, 413 too large, 415 media type |

## UIBE (`/contract-ui/v1`)

| Operation | Auth | Notes |
|---|---|---|
| `GET /health` | none | |
| `POST /contractOffers/{nid}` | `x-api-key` | 201 `{offerStatus, offerUrl}`; creates or replaces (status kept) |
| `POST /admin/offerExpiry/run` | `x-api-key` | B.4 |
| `GET /contractOffers` | Bearer, `contract_manager` | JSON array, D9 names |
| `GET /contractOffers/{nid}` | Bearer, `contract_manager` | D9 names, 404 |
| `PATCH /contractOffers/{nid}/{accept or reject}` | Bearer, `contract_manager` | D23; 200 body is the offer object |
| `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions` | Bearer, `contract_manager` | passed to CM; `counterpartyName` filter; offer fields and `consentManagerUrl` added |

Token rules: missing or invalid token, wrong issuer: 401. Token of the right realm but another `participant_id`, or without `contract_manager`: 403.

## Stub (`http://localhost:19090`)

Exactly Appendix C. Additions that do not change the contract: `GET /health`; `GET /_control/negotiations/{nid}` also returns the four ids; `POST /_control/catalogue/assets` also defaults `resourceName` (to the asset id), `providerId` (`provider-01`), `providerName` (from the directory), `description`, `policies` and `contractClauses` (empty). `PUT .../overrides` accepts `null` to remove an override.

## Kafka

Topics `simpl.contract.lifecycle.v1`, `simpl.contract.status.v1`, `notifications`, created by CM at startup with one partition. Message shapes are those of Appendix D. Key = nid. Headers `correlationId` and `contractNegotiationId`.
