# PSO run: suite changes

The suite was written from `input/spec-digest-run1.1.md`. This run updates it to
`input/spec-digest.md`, where the PSO scenarios are the highest source (digest
section 1.1 and section 8). Every changed or added scenario carries `@pso-spec`
and its decision tags. The PSO scenarios were not copied; the scenarios below
check the decisions they caused.

Run only these scenarios: `behave --tags=@pso-spec`.

## Changed scenarios

| Feature file | Scenario | Decision | Before | After |
|---|---|---|---|---|
| `api/health_and_conventions.feature` | The provider CM health check without an API key is refused with 401 (was: answers without authentication) | D13, D65, D11 | no key: 200, status UP | no key: 401 `unauthorized`, problem body per D11 |
| `api/health_and_conventions.feature` | The consumer CM health check without an API key is refused with 401 (was: answers without authentication) | D13, D65, D11 | no key: 200, status UP | no key: 401 `unauthorized` |
| `api/agreements.feature` | A status change for an unknown agreement gives 404 (was: is refused with 400) | D12, D32 | 400 `validationFailure` | 404 `notFound`, detail "Unknown contractAgreementId" |
| `api/contract_offers_cm.feature` | A new presentation for an unknown negotiation gives 404 (was: is refused with 400) | D12, D33 | 400 `validationFailure` | 404 `notFound`, detail "Unknown contractNegotiationId" |
| `api/contract_offers_cm.feature` | A status update for an unknown negotiation gives 404 (was: is refused with 400) | D12, D33 | 400 `validationFailure` | 404 `notFound`, detail "Unknown contractNegotiationId" |
| `api/consumer_verification_direct.feature` | Verification for an unknown negotiation id gives 404 (was: is refused with 400) | D12, D31 | 400 `validationFailure` | 404 `notFound`, detail "Unknown contractNegotiationId" |
| `api/offer_decision.feature` | CM answers 404 for a decision on an unknown negotiation id (was: CM refuses ... with 400) | D12, D23, D84 | 400 `validationFailure` | 404 `notFound`, detail "Unknown contractNegotiationId" |
| `api/provider_issue_direct.feature` | validate-and-sign without a body and without a Provider record gives 404 (was: is refused with 400) | D25, D12 | 400 `validationFailure` | 404 `notFound`, detail "Unknown contractAgreementId", no record created |

## Added scenarios

| Feature file | Scenario | Decision | Before | After |
|---|---|---|---|---|
| `api/health_and_conventions.feature` | The provider CM health check with a wrong API key is refused with 401 | D13, D65 | no scenario | 401 `unauthorized` |
| `api/health_and_conventions.feature` | The provider CM health check answers with the API key | D13 | no scenario (health was checked without a key) | 200, actuator content type, status UP |
| `api/health_and_conventions.feature` | The consumer CM health check answers with the API key | D13 | no scenario | 200, actuator content type, status UP |
| `api/health_and_conventions.feature` | The 404 for an unknown agreement follows the error rules | D11, D12, D34 | no scenario for the explicit D11 points | 404 `notFound`; type an absolute URI, status equals HTTP status, instance absent, null or the request path |
| `api/health_and_conventions.feature` | The 401 of a POST without an API key carries a full problem body | D11, D65, D25 | no scenario for the explicit D11 points | 401 `unauthorized` with a problem body per D11 |
| `api/auth_cm.feature` | A bearer token without an API key is refused with 401 | D65, D85 | no scenario | 401: CM does not read `Authorization` |
| `api/auth_cm.feature` | With the API key an extra Authorization header changes nothing | D65, D85, D12 | no scenario | 404 `notFound` for an unknown aid, not 401 |
| `api/provider_issue_direct.feature` | validate-and-sign with an empty JSON object and without a Provider record gives 404 | D25, D12, D84 | no scenario | `{}` counts as no body: 404 `notFound`, no record created |
| `api/provider_validation.feature` | validate-and-sign with an empty JSON object uses the Provider record of the existence check | D25 | no scenario | `{}` behaves as no body: 201 `INITIATED`, record reaches SIGNED |
| `api/check_order.feature` (new) | A create call at the Provider with an empty body and no API key gives 401, not 400 | D84, D65, D20, D11 | no scenario | 401 before `wrongParty` and body checks |
| `api/check_order.feature` | A validate-and-sign call for unknown ids gives 401 without a key and 404 with it | D84, D65, D25, D12 | no scenario | 401, then 404 for the same ids with the key |
| `api/check_order.feature` | An existence check with an empty body gives 401 without a key and 400 with it | D84, D65, D19, D11 | no scenario | 401, then 400 `validationFailure` with the key, no record |
| `api/check_order.feature` | A path id that is not a UUID gives 401 when the API key is missing | D84, D65, D16 | no scenario | 401 before `argumentTypeMismatch` |
| `api/check_order.feature` | A status change with an empty body for an unknown agreement gives 404, not 400 | D84, D12, D32 | no scenario | 404 before body fields |
| `api/check_order.feature` | A verification with an empty body for an unknown negotiation at the Provider gives 404, not 400 | D84, D12, D31 | no scenario | 404 before body fields and party |
| `api/check_order.feature` | A status update with an empty body for an unknown negotiation at the Provider gives 404, not 400 | D84, D12, D33 | no scenario | 404 before body fields and party |

## Support code

- `features/steps/http_steps.py`: new step `the problem body follows the error rules of D11`; new step `I send ... to the ... CM at ... without an API key and with header ... set to ...`.
- `features/steps/domain_steps.py`: new step `the connector asks the ... CM to validate and sign with an empty JSON object`.
- Feature descriptions of `auth_cm`, `health_and_conventions`, `provider_issue_direct` and `consumer_verification_direct` updated to the new rules.

## Not changed, on purpose

- UIBE health stays open (D13 reach: CM only).
- Scenarios that already agreed with the confirmed rows (D19, D20, D34, the 404 reads of D12) stay as they were.
- Scenarios that check 405, 406 or 415 already send a valid key (D84).

## Totals

| | Scenarios |
|---|---|
| Changed | 8 |
| Added | 16 |
| Unchanged | 375 |
| Total now | 399 (383 before) |

Dry run (`behave --dry-run`): 0 undefined steps.
