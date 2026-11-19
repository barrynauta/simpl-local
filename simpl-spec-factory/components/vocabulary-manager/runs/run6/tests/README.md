# Vocabulary Manager: black-box API test suite

Gherkin scenarios (behave) and a shared Python step library (requests). They test the Vocabulary Manager through HTTP only, from its stories, the OpenAPI file and the decisions of step 30. The suite never reads the build, never resets the store and runs unchanged against any implementation: every value comes from an environment variable.

## Run

```
./run.sh api          # default API scenarios            -> reports/api.json
./run.sh store-down   # backend without its store (D44)  -> reports/store-down.json
./run.sh ui           # @ui browser scenarios (Playwright) -> reports/ui.json, axe results in reports/a11y/
./run.sh load         # load test: none in this build    -> reports/load.json
./run.sh dry-run      # parse all features, fail on undefined steps (no system needed)
```

The first run creates `.venv` and installs `requirements.txt` (behave 1.3.3, requests 2.34.2, playwright 1.65.2, axe-playwright-python 0.1.8). Extra arguments go to behave, for example `./run.sh api --tags=@D43` or `./run.sh api -n "duplicate"`. `run.sh` exits 0 when the run worked, whether the scenarios pass or fail. It exits 2 when behave writes no valid report.

Mode `api` runs `not ui and not wip and not store_down`. Mode `store-down` runs `store_down` against `STORE_DOWN_API_URL`. That is a second backend whose store URL points at a port where nothing listens (`stack.sh store-down-up`). Mode `ui` runs `ui and not wip and not manual` against `UI_URL` (default http://localhost:4433). It installs Chromium for Playwright if needed and starts every scenario in a new browser context, so no stored session, cookie or storage is reused; each scenario signs in on the Keycloak login page. Setup data is created through the API as ga-admin.

## Environment variables

| Variable | Default (build profile, E1) | Meaning |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base; paths and `href` values are relative to it (E2) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | Keycloak token endpoint (password grant, scope `openid`, D40) |
| `TOKEN_CLIENT` | `vocabulary-tests` | public client with the password grant |
| `TOKEN_CLIENT_SECRET` | empty | only if the client is confidential |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend without a store (D44) |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | test users of E1 |
| `ADMIN_EMAIL`, `VIEWER_EMAIL`, `NOROLE_EMAIL` | `<user>@example.com` | expected `createdBy` (D27) |
| `TEST_PASSWORD` | `Password1!` | password of the test users |
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | fallback contentBase; the suite learns the real one from the first `contentHref` |
| `HTTP_TIMEOUT` | `60` | seconds per request |

## Tags

| Tag | Meaning |
|---|---|
| `@AC_<story>_<ACn>` | the acceptance criterion the scenario checks, for example `@AC_SIMPL-18173_AC1` |
| `@D12`, `@E4` | the decisions the scenario depends on; a scenario without a criterion tag always has one |
| `@OWASP-02`, `@BLG-err-problem`, `@API-46`, `@NFR-04` | rule ids from `rules.json`, as they are |
| `@negative` | error and abuse cases |
| `@ui` | UI scenarios, Playwright steps in `features/steps/ui_steps.py`; run with `./run.sh ui` |
| `@a11y` | axe check of one main page for WCAG 2.1 AA (NFR-10); writes `reports/a11y/<scenario>.json` |
| `@manual` | cannot be automated; reason in `COVERAGE.md`; excluded from every run |
| `@wip` | cannot run against the API (an internal error that cannot be caused from outside) |
| `@store_down` | needs the backend without its store; run with `./run.sh store-down` |

## How the suite works

- **Own data per scenario.** Every scenario gets a run id such as `R6b2463fd60`. The logical name `"Health"` in a step is stored as `HealthR6b2463fd60`. Lists and counts are always filtered on the scenario's own names (E4).
- **Templates.** Strings in steps can hold `{{run}}`, `{{runupper}}`, `{{runlower}}`, `{{name:Health}}` (stored name), `{{base}}` (contentBase), `{{ext:Terms}}` (an external namespace of this run), `{{var:x}}` and `{{datepart:x}}` (remembered values), and `{{email:ga-admin}}`. A logical name written as `raw:<text>` is used as is, for example for invalid or other-case names.
- **Setup as admin.** `Given` steps that create data always run as `ga-admin`, whatever user the scenario acts as. Reads that prove the stored state also run as `ga-admin`.
- **Nothing stored.** After a refused request the suite proves that nothing was stored through the list with a name filter, or a read by a valid name. It never uses a GET on the rejected name (D49).
- **Tables for multipart requests.** `| field | value |` changes the default form fields. The first row for a field replaces its default; further rows add parts (for `aliasUris`). Values: `(omitted)`, `(empty)`, `(spaces)`, `file:<fixture>`, `turtle:<bytes>` (valid Turtle of that exact size), `chars:<n>`, or text.
- **Fixtures.** Turtle files in `fixtures/turtle/`, made by `tools/make_fixtures.py`. Default contents are generated per scenario in `vmtest/turtle.py`: a SKOS scheme in an own namespace, so they cause no validation error or warning.
- **Code layout.** `vmtest/` holds configuration, tokens, the scenario state (`world.py`) and a valid request for every operation (`operations.py`). `features/steps/vocabulary_steps.py` is the shared step library. `features/environment.py` builds a fresh state per scenario.

## Load test

No `Q` decision gives a response time or throughput target, so there is no load test (see `COVERAGE.md`). `./run.sh load` writes `reports/load.json` with `"skipped": true` and the reason. If a later decision round adds `Q` targets, add a Locust file under `load/`: one task per operation with the stated load, writing p50, p95, p99 and the error rate per operation to `reports/load.json`.

## Coverage

`COVERAGE.md` is generated: `python3 tools/coverage.py`. It lists every live criterion and every decision with its scenarios, and gives the reason for each one without a scenario.

## Known quirk

In a dry run, behave 1.3 may report a feature as "failed" when that feature mixes scenario outlines and plain scenarios, even though no step ran. `./run.sh dry-run` therefore checks only the scenario and step counts for undefined or failed steps.
