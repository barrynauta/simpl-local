# API notes

All CM paths are under `/contract/v1`, all UIBE paths under `/contract-ui/v1`. Errors are always
`application/problem+json` with `type`, `title`, `status`, `detail` and `instance` (the request path).

## CM

Every CM request, `/health` included, needs `X-Api-Key`. Without it or with a wrong one: 401 with a problem
body, before any other check (D65, D84). CM never reads `Authorization`.

| Operation | Party | Success | Notes |
|---|---|---|---|
| `GET /health` | both | 200 `application/vnd.spring-boot.actuator.v3+json` | 503 when the database is down |
| `POST /agreements/{nid}/search` | any (stub calls the Provider) | 200 `{contractAgreementId, contractDefinitionId, exists, contractSearchTO}` | `assetId`, `consumerId` required. Creates the Provider record when nothing open exists. Calls `isPresent` after the response |
| `POST /credentials/agreements/{aid}/definitions/{did}/create` | Consumer | 201, D20 fields | 400 `wrongParty` at the Provider, 400 `alreadyExists`, 404 or 403 for the asset, 500 when UIBE fails |
| `PATCH /contractOffers/{nid}/{accept or reject}` | Consumer | 200 `{nid, INITIATED}` | optional `?reason=EXPIRED` (reject only) gives TERMINATED `OFFER_EXPIRED`. Optional header `X-Actor` (set by UIBE) is the history actor |
| `POST /credentials/agreements/{aid}/definitions/{did}/issue` | Provider | 201 `{aid, did, INITIATED}` | `{}` is no body: record found by aid, else 404. Validation and signing run afterwards |
| `PUT /verifications/{nid}` | Consumer | 200 `{nid, INITIATED}` | body = Provider VC; `{}` counts as "no VC" (`MISSING_CONTRACT_DATA`) |
| `GET /verifications/{nid}/vc` | both | 200 own VC | 404 until signed |
| `PATCH /agreements/{aid}/definitions/{did}/status` | Provider | 200 `{aid, did, FINALIZED or TERMINATED}` | 400 `invalidTransition` from a wrong state |
| `POST /contractOffers/{nid}` | Consumer | 200 `{nid, INITIATED}` | replaces the offer while REQUESTED and forwards it to UIBE |
| `PUT /contractOffers/{nid}/update` | Consumer | 200 `{nid, INITIATED}` | `REJECTED` moves REQUESTED, ACCEPTED or SIGNED to FAILED `PROVIDER_REJECTED` |
| `GET /agreements/{aid}` | both | 200 `ContractAgreementData` | technical status (D38), dates without offset |
| `GET /agreements/{aid}/file` | both | 200 text/plain, 7 lines | 404 until both signature dates exist |
| `POST /agreements/search` | both | 200 `{items}` | established contracts only |
| `GET /contracts`, `GET /contracts/{nid}` | both | A.2, A.3 | `status` filter takes comma separated D37 values |
| `POST /contracts/{nid}/lifecycleActions` | both | A.3 body | 400 for a missing reason, 409 `invalidTransition` |
| `PUT /contracts/{nid}/consentStatus` | Provider | A.3 body | 409 for a Consumer, terminal or `NOT_DEFINED` record |
| `GET /audit/contractStatusCounts` | both | A.6 | all 11 statuses, zeros included |
| `POST /contractClauseDocuments` | both | 201 metadata | multipart part `file`; 400 empty, 413 too large, 415 type |

Unknown path: 404. Wrong method: 405 (also `GET /agreements/search`). `Accept` without JSON: 406. Wrong body
type: 415. Bad path id: 400 `argumentTypeMismatch`. Bad body: 400 `validationFailure`.

## UIBE

| Operation | Auth | Notes |
|---|---|---|
| `GET /health` | none | 200 |
| `POST /contractOffers/{nid}` | `x-api-key` | 201 `{offerStatus, offerUrl}`; creates CREATED or replaces content |
| `GET /contractOffers` | JWT | JSON array, D9 field names |
| `GET /contractOffers/{nid}` | JWT | 404 when unknown |
| `PATCH /contractOffers/{nid}/{op}` | JWT | 400 `offerNotPending` unless CREATED; CM errors keep CREATED and are returned with the same status |
| `GET /contracts`, `GET /contracts/{nid}` | JWT | B.1, B.2; `counterpartyName` maps by party |
| `POST /contracts/{nid}/lifecycleActions` | JWT | B.3; actor = `preferred_username` |
| `POST /admin/offerExpiry/run` | `x-api-key` | B.4 |

JWT rules: issuer must be the own realm (else 401), `contract_manager` realm role (else 403),
`participant_id` equal to the own id (else 403).

## Stub

Appendix C exactly, plus `GET /health`. Control endpoints have no authentication.
