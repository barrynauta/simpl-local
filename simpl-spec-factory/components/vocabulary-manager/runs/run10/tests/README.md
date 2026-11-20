# Vocabulary Manager API test suite

Black-box tests for the Vocabulary Manager, written from the stories, the OpenAPI and the decisions
of step 30 (decisions win where they differ). HTTP only: the suite never looks inside the component.
Harness from the build profile: Python 3, behave, requests; Locust for the load test.

## Run

```
./run.sh api          # default API scenarios            -> reports/api.json (behave JSON)
./run.sh ui           # @ui scenarios in Chromium (Playwright) -> reports/ui.json, reports/a11y.json
./run.sh store-down   # backend without its store (D44)  -> reports/store-down.json
./run.sh load         # load test of Q1 (D99), not part of the default run -> reports/load.json
./run.sh dry-run      # parse check, needs no running system -> reports/dry-run.json
```

Each mode creates `.venv` on first use and installs `requirements.txt` (`requirements-ui.txt` and the Playwright
Chromium for `ui`, `requirements-load.txt` for `load`). The UI mode opens a new browser context per scenario, so no
run reuses a stored sign-in; screenshots of failed UI scenarios go to `reports/ui-artifacts/`.
The exit code is 0 when the run itself worked, whether scenarios pass or fail; 2 when it broke.
Extra arguments go to behave, for example `./run.sh api --tags=@D43` or `./run.sh api features/search.feature`.

## Environment variables

| Variable | Default (build profile) | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend with an unreachable store (store-down mode) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | password grant |
| `TOKEN_CLIENT` | `vocabulary-tests` | client with the audience mapper |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | E7: token without audience, expect 401 |
| `TOKEN_URL_OTHER` | `http://localhost:8193/realms/simpl-other/protocol/openid-connect/token` | E7: wrong issuer, expect 401 |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | E1 users |
| `USER_PASSWORD` | `Password1!` | E1 password |
| `ADMIN_EMAIL`, `VIEWER_EMAIL`, `NOROLE_EMAIL` | `<user>@example.com` | expected `createdBy` (D27) |
| `CONTENT_BASE_URL` | discovered | contentBase of E2; when unset, the suite reads it from the `contentHref` of a probe vocabulary |
| `RUN_ID` | random 6 characters | part of every name the suite creates |
| `HTTP_TIMEOUT` | `90` | seconds per request (5 MB writes may take up to 30 s, D71) |
| `UI_URL` | `http://localhost:4433` | UI base for `./run.sh ui` |
| `UI_TIMEOUT` | `15` | seconds a UI step waits for an element or a condition |
| `UI_HEADED` | unset | `1` shows the browser |

## Test data

The store is shared and never reset (E4). Every scenario gets its own prefix `Vt<run id>S<nnnn>X`; an alias
such as `"A"` in a step means the vocabulary `<prefix>A`. External namespaces are
`https://ext.example.org/vt/<prefix>/<key>`. "Nothing was stored" is proven through the list with a name
filter (or the version list or status of a valid name), never through a read of a rejected name (D49).
Turtle templates are in `fixtures/ttl/`; placeholders: `${own}` (own namespace), `${marker}` (unique text),
`${base}` (contentBase), `${A}` (name of alias A), `${var:ref}` (a file variable set by a step).

## Tags

| Tag | Meaning |
|---|---|
| `@AC_<story>_<ACn>` | acceptance criterion checked, for example `@AC_SIMPL-18173_AC2` |
| `@D12`, `@E7`, `@Q2` | decision the scenario depends on |
| `@OWASP-02`, `@NFR-04`, `@API-46`, `@BLG-err-problem`, ... | rule checked (rule id as in rules.json) |
| `@negative` | error or abuse case |
| `@ui` | UI scenario, run by `./run.sh ui`; excluded from API runs |
| `@a11y` | axe WCAG 2.1 AA check of a main page (list, details, both forms) |
| `@manual` | cannot be automated without an undecided UI detail; skipped, listed in `COVERAGE.md` |
| `@wip` | cannot run against the API; never run |
| `@store_down` | needs the backend without its store; only in `./run.sh store-down` |
| `@access`, `@create`, `@versions`, `@validation`, `@status`, `@list`, `@search`, `@external`, `@content`, `@protocol` | feature area |

## Load test

`./run.sh load` seeds the store as D99 asks (1000 vocabularies with 20 versions of about 10 KB; names
`Ld<tag>N0001`..., a rerun only tops up), then runs Locust headless for 5 minutes at the offered rate of
20 reads per second (20 users, 1 request per second each, one task per read operation Q1 names) and
1 write per second (3 writer users at 1/3 per second each, so the offered rate holds while a write takes up to 3 s; create and new version alternating, files 10 KB to 1 MB). After it, a separate
test sends five 5 MB creations one after the other (D71). `reports/load.json` holds p50, p95, p99, error rate
and achieved rate per operation, and a summary against the Q1 targets (reads p95 300 ms, writes p95 3 s,
5 MB writes p95 30 s, at least 95 % of the offered rate with no errors).

Settings: `LOAD_DURATION` (5m), `LOAD_SEED_VOCABULARIES` (1000), `LOAD_SEED_VERSIONS` (20),
`LOAD_SEED_FILE_BYTES` (10240), `LOAD_SEED_TAG` (Seed1), `LOAD_SEED_WORKERS` (4, D92), `LOAD_SKIP_SEED` (0),
`LOAD_BIG_WRITES` (5), `LOAD_WRITE_SIZES` (10240,102400,524288,1048576).

## Layout

```
features/*.feature          scenarios
features/steps/api_steps.py shared step library (HTTP)
features/steps/ui_steps.py  browser steps (Playwright for Python, axe-playwright-python)
features/support/vmclient.py tokens, multipart encoding, configuration
features/environment.py     per-scenario prefix and state
fixtures/ttl/               Turtle templates
load/                       Locust file and load runner
tools/coverage.py           regenerates COVERAGE.md: .venv/bin/python tools/coverage.py ../input > COVERAGE.md
```
