# Contract Management: black-box test suite

Behave 1.2.6 suite for Simpl-Open Contract Management (contract manager backend
CM, contract UI backend UIBE, UI), written from `../specs/` and
`../spec-digest.md` only. It uses HTTP, Kafka and the stub control API of digest
Appendix C.6. It knows nothing about any implementation.

## Contents

| Path | What |
|---|---|
| `features/api/*.feature` | API and event scenarios (CM, UIBE, stub-driven flows, Kafka) |
| `features/ui/*.feature` | UI scenarios with the D72 hooks, tagged `@ui` (browser steps, Playwright) |
| `features/steps/param_types.py` | custom parse types: `Q` (quoted value), `Party`, `Method` |
| `features/steps/http_steps.py` | generic request and response steps |
| `features/steps/domain_steps.py` | test data, negotiations, contract state, stub faults, wallet, uploads, audit, expiry |
| `features/steps/event_steps.py` | Kafka listening, checks and lifecycle message injection |
| `features/steps/ui_steps.py` | browser steps (Playwright sync API, headless Chromium) |
| `features/environment.py` | hooks, configuration from environment variables, tag-based skipping |
| `cmtest/` | support library: config, HTTP, polling (D79), tokens, stub and VC helpers, Kafka watcher, browser lifecycle (`browser.py`) |
| `fixtures/documents/` | upload fixtures for SIMPL-28846 |
| `reports/` | run reports, one progress file and one JSON file per target |

Placeholders in feature files use `${name}`: `${nid}`, `${aid}`, `${did}`,
`${offerId}`, `${assetId}`, `${rdId}`, `${resourceName}`, `${providerId}`,
`${consumerId}`, `${consumerUiUrl}`, `${consentManagerUrl}`, and `${uuid}` (a
fresh UUID each time). Every scenario creates its own asset and ids (D80, D81)
and never assumes an empty database.

## Tags

| Tag | Meaning |
|---|---|
| `@SIMPL-nnnnn` | the story whose acceptance criterion the scenario checks |
| `@bp07` | a BP07 process step or outcome |
| `@openapi-only` | an OpenAPI or digest rule with no story behind it |
| `@Dnn` | the digest decisions the scenario depends on |
| `@E1` to `@E11`, `@P1` to `@P4`, `@L1` to `@L7` | the D37 state machine row it covers |
| `@rbac` | access control (API key, token, role) |
| `@oidc` | needs Keycloak tokens; skipped with `AUTH_MODE=none` |
| `@events` | needs Kafka; skipped with `SKIP_KAFKA=1` |
| `@stub` | needs the stub service; skipped with `SKIP_STUB=1` |
| `@two-party` | needs both parties; skipped with `SKIP_CONSUMER=1` |
| `@consumer` | needs the Consumer party; skipped with `SKIP_CONSUMER=1` |
| `@uibe` | needs UIBE; skipped with `SKIP_UIBE=1` |
| `@slow` | drives a whole negotiation or polls long |
| `@serial` | compares global counts; run it alone if counts drift |
| `@ui` | UI scenarios: a browser is started only for them. Run them with `behave features/ui`; exclude them with `--tags=~@ui` |
| `@wip` | excluded by default (`behave.ini`); no scenario carries it now |

## Environment variables

All have the defaults of the reference environment (compose project
`cm-factory`, D67, D76, D77). The full list with defaults is in the docstring
of `features/environment.py`. The main ones:

| Variable | Default |
|---|---|
| `CM_PROVIDER_URL`, `CM_CONSUMER_URL` | `http://localhost:19101/contract/v1`, `http://localhost:19201/contract/v1` |
| `UIBE_PROVIDER_URL`, `UIBE_CONSUMER_URL` | `http://localhost:19102/contract-ui/v1`, `http://localhost:19202/contract-ui/v1` |
| `CM_PROVIDER_API_KEY`, `CM_CONSUMER_API_KEY` | `test-cm-provider-key`, `test-cm-consumer-key` |
| `UIBE_PROVIDER_API_KEY`, `UIBE_CONSUMER_API_KEY` | `test-uibe-provider-key`, `test-uibe-consumer-key` |
| `STUB_URL`, `STUB_EDC_API_KEY` | `http://localhost:19090`, `test-edc-key` |
| `KAFKA_BOOTSTRAP` | `localhost:19092` |
| `KEYCLOAK_URL`, `PROVIDER_REALM`, `CONSUMER_REALM`, `KEYCLOAK_CLIENT_ID` | `http://localhost:19080`, `provider`, `consumer`, `contract-ui` |
| `PROVIDER_PARTICIPANT_ID`, `CONSUMER_PARTICIPANT_ID` | `provider-01`, `consumer-01` |
| `AUTH_MODE` | `oidc` (or `none`: no tokens, `@oidc` skipped) |
| `SKIP_STUB`, `SKIP_KAFKA`, `SKIP_CONSUMER`, `SKIP_UIBE` | unset (set to `1` to skip) |
| `POLL_INTERVAL`, `POLL_TIMEOUT`, `QUIET_PERIOD` | `0.25`, `20`, `2` seconds |

With `SKIP_UIBE=1` the Consumer decision and lifecycle actions are sent to CM
directly (Appendix A.4) instead of through UIBE.

## Install

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

## Run against the factory build

Start the build's compose project `cm-factory`, then:

```bash
.venv/bin/behave -f progress -o reports/factory-progress.txt \
                 -f json.pretty -o reports/factory.json
```

Everything runs with the defaults. A dry run (`.venv/bin/behave --dry-run`)
checks that every step is defined.

## Run against the local upstream stack

The upstream stack (`~/src/simpl-local/simpl-contract`, see its README) has one
party in Provider mode, no Keycloak, no stub, no UIBE, and a Kafka with SASL on
port 9095. Its CM listens on port 8086 and needs its own API key, which the
stack passes to the container as `API_KEY`.

```bash
export CM_PROVIDER_URL=http://localhost:8086/contract/v1
export CM_PROVIDER_API_KEY=<the API key of the upstream contract container>
export AUTH_MODE=none SKIP_STUB=1 SKIP_KAFKA=1 SKIP_CONSUMER=1 SKIP_UIBE=1
export POLL_TIMEOUT=5 QUIET_PERIOD=1
.venv/bin/behave -f progress -o reports/upstream-progress.txt \
                 -f json.pretty -o reports/upstream.json
```

Only Provider CM scenarios that need no stub, no Kafka and no token run there.
The shorter poll timeout only shortens the run; it does not change any check.

## UI scenarios (browser)

`features/ui` has 38 scenarios for the UI stories (SIMPL-30959, 30960, 32505,
31047, 31053, 31076, 31115 to 31118, 35692 AC4). They use Playwright (sync API,
headless Chromium), one fresh browser context per login. Setup reuses the API
and stub Given steps (a negotiation driven through the stub to the state the
scenario needs); only the user's part happens in the browser.

Install once:

```bash
.venv/bin/pip install -r requirements.txt
.venv/bin/python -m playwright install chromium
```

Locators, in this order: story wording (case-insensitive, whitespace tolerant:
the column names of 30959 and the status labels), then the D72 `data-testid`
values and the D71 routes (`/`, `/contracts/<nid>`, `/contractOffers/<nid>`),
then a tolerant pattern commented at the step. No CSS classes or ids. Buttons
that are not allowed (D73) are checked as absent from the DOM, only after the
screen has loaded.

| Variable | Default |
|---|---|
| `UI_PROVIDER_URL`, `UI_CONSUMER_URL` | `http://localhost:19103`, `http://localhost:19203` (D76) |
| `UI_AUTH` | `oidc`: sign in on the identity provider's login page with the D67 users (generic "user name"/"password"/"sign in" locators); `none`: the UI has no sign-in |
| `UI_TIMEOUT` | `20` seconds per browser expectation |
| `UI_HEADED` | unset (set to `1` to watch the browser) |
| `UI_SCREENSHOT_DIR` | `reports/ui-screenshots` (a screenshot per failed scenario; empty to switch off) |

The step `the browser request to "<path>" fails` injects a Problem Details 500
for matching browser requests (`page.route`); it works against any build.

Against the factory build (defaults):

```bash
.venv/bin/behave features/ui -f progress -o reports/build1-ui-progress.txt \
                 -f json.pretty -o reports/build1-ui.json
```

Against another UI, for example one without sign-in:

```bash
export UI_PROVIDER_URL=http://localhost:4323 UI_AUTH=none AUTH_MODE=none
.venv/bin/behave features/ui -f progress -o reports/upstream-ui-progress.txt \
                 -f json.pretty -o reports/upstream-ui.json
```

`AUTH_MODE=none` skips the `@oidc` scenarios (users without the role). The API
suite alone: `.venv/bin/behave --tags=~@ui`.

## Writing new scenarios

- Use your own asset (`Given a fresh asset`) and ids. Filter lists by your own
  nid or resource name.
- Check only what the specs and the digest say: status codes, problem types,
  states and named fields.
- Asynchronous results are polled (`... reaches status`, `... has:`), never
  read once.
