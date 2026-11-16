# PSO test scenarios as a specification (visible half)

These files come from the API test suite of the PSO test team (code.europa.eu `simpl/pso/pso-test/pso-test-automation-unified`, commit `ec7a546`, 27 Oct 2026). They are written from the same stories by people who never saw the factory. In this run they are a **specification**, and the highest one for the behaviour they check (see the precedence rule in the digest, section 1.1).

Only part of the PSO scenarios for Contract Management is here: 8 of 14. That is deliberate. Do not try to guess or find the others.

## Files

| Path | What |
|---|---|
| `features/contract-billing.feature` | 4 functional scenarios (`@TC_CB_nn`) |
| `features/contract-billing-schema-validation.feature` | 4 schema scenarios (`@TCCB_SVnn`) |
| `schemas/error-response.json` | the JSON schema the step "the contract billing response body conforms to the error response schema" validates the body against |

PSO calls the component "Contract Billing". It is the contract manager backend (CM) of the Provider.

## What each step sends

All requests go to the Provider's contract manager, base path `/contract/v1`, through the participant gateway (see "Environment" below). Ids written as `<uuid>` are fresh random UUIDs that exist nowhere.

| Step text | Request |
|---|---|
| a user with role "SD_PUBLISHER" logs in based on the "DATA_PROVIDER" | gets a user token from the Provider's Keycloak; the requests below carry `Authorization: Bearer <token>` |
| a user with role "T1UAR_M" logs in to "data provider" | the same, another user of the Provider |
| a user without authorization is based on "DATA_PROVIDER" | the requests below carry no `Authorization` header |
| a participant with participant type "DATA_PROVIDER" is onboarded with the following data | environment: the gateway reports the Provider as already onboarded, so nothing is sent to the contract manager |
| the user gets CB agreement by random id | `GET /contract/v1/agreements/<uuid>` |
| the user retrieves a contract agreement by random id for schema validation | `GET /contract/v1/agreements/<uuid>` |
| the user gets CB agreement file by random id | `GET /contract/v1/agreements/<uuid>/file` |
| the user checks CB health endpoint | `GET /contract/v1/health` |
| the user issues a CB credential with random ids | `POST /contract/v1/credentials/agreements/<uuid>/definitions/<uuid>/issue`, `Content-Type: application/json`, body `{}` |
| the user issues a verifiable credential for schema validation | `POST /contract/v1/credentials/agreements/<uuid>/definitions/test-def/issue`, body `{}` |
| the user creates contract data for schema validation | `POST /contract/v1/credentials/agreements/<uuid>/definitions/test-def/create`, body `{}` |
| the user searches contract agreements for schema validation | `POST /contract/v1/agreements/test-negotiation/search`, body `{}` |
| the response status code is N | the HTTP status must be exactly N |
| the contract billing response body conforms to the error response schema | the body must validate against `schemas/error-response.json` |

## Environment

In a Simpl data space the PSO suite reaches the contract manager through the Provider's participant gateway. The factory environment has a stand-in for that gateway, with these rules. They are fixed; build to them.

- The gateway forwards `/contract/v1/...` to the Provider's contract manager.
- When a request has an `Authorization` header, the gateway removes it and adds the contract manager's API key (`X-Api-Key`, D65, D77). It does not check the token.
- When a request has no `Authorization` header, the gateway forwards it **without** an API key.

So for the contract manager: "a user with a role" arrives as a request with the right API key, and "a user without authorization" arrives as a request with no API key.
