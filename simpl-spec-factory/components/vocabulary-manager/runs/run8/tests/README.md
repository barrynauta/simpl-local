# Vocabulary Manager: black-box API test suite

Python 3, behave and requests (build profile `build.yaml`). The suite talks
HTTP only. It reads every address and user from environment variables, so it
runs unchanged against any implementation of the component. It never resets
the store: every scenario creates its own data with a unique id (decision E4).

## Run

```
./run.sh api          # default API scenarios      -> reports/api.json
./run.sh store-down   # backend without its store  -> reports/store-down.json
./run.sh load         # Locust load test (Q1, Q4)  -> reports/load.json
./run.sh dry-run      # parse check, no system     -> reports/dry-run.json
```

Each mode creates its own virtualenv on first use (`.venv`, `.venv-load`) and
exits 0 when the run itself worked, whether scenarios pass or fail. It exits
non-zero only when the virtualenv cannot be built or no report was written.
Extra arguments go to behave or Locust, for example
`./run.sh api features/search.feature` or `./run.sh api --tags=@D38`.

`./run.sh ui` runs the `@ui` scenarios in a headless Chromium (Playwright for
Python, own virtualenv `.venv-ui` from `requirements-ui.txt`), writes
`reports/ui.json` and the axe results per page in `reports/a11y/`. Each
scenario starts a fresh browser context without a stored session.

## Environment variables

| Variable | Default (build profile run 8) | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend whose store is unreachable (D44) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | password grant |
| `TOKEN_CLIENT` | `vocabulary-tests` | client with the audience mapper |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | E7: token without audience, expect 401 |
| `TOKEN_URL_OTHER` | `http://localhost:8193/realms/simpl-other/protocol/openid-connect/token` | E7: wrong issuer, expect 401 |
| `TOKEN_CLIENT_OTHER` | value of `TOKEN_CLIENT` | client in the other realm |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | E1 users |
| `ADMIN_EMAIL`, `VIEWER_EMAIL`, `NOROLE_EMAIL` | `<user>@example.com` | expected `createdBy` (D27) |
| `USER_PASSWORD` | `Password1!` | E1 password |
| `CONTENT_BASE_URL` | empty: learnt from the `contentHref` of a probe vocabulary | contentBase of E2 |
| `HTTP_TIMEOUT` | `60` | seconds per request |
| `RUN_ID` | random | prefix of the unique ids |
| `UI_URL` | `http://localhost:4433` | UI of the build (`./run.sh ui`) |
| `UI_HEADLESS` | `1` | `0` shows the browser |
| `UI_TIMEOUT_MS` | `10000` | wait per UI assertion |

Load test only: `LOAD_USERS` (20), `LOAD_SPAWN_RATE` (5), `LOAD_DURATION` (5m),
`LOAD_SEED_VOCABULARIES` (1000), `LOAD_SEED_VERSIONS` (20),
`LOAD_READS_PER_SECOND` (20), `LOAD_WRITES_PER_SECOND` (1), `LOAD_BIG_EVERY` (30).

## Layout

- `features/*.feature`: API scenarios; `features/store_down.feature` (tag
  `@store_down`); `features/ui/*.feature`: UI scenarios (`@ui`, browser steps).
- `features/steps/api_steps.py`: the one shared step library.
  `features/steps/ui_steps.py`: Playwright browser steps; `vmtest/ui.py`: browser and stub helpers.
- `vmtest/`: configuration, tokens, HTTP client, placeholder expansion.
- `fixtures/`: Turtle files. `${uid}` in a fixture becomes the scenario id.
- `load/locustfile.py`: load test. `tools/coverage.py`: writes `COVERAGE.md`.

## Unique data and placeholders

Aliases in quotes (`"Health"`) and `${Health}` in paths, tables and doc
strings become `Health<uid>`, unique per scenario. `${Health.upper}` gives the
name in upper case. `${uid}`, `${contentBase}`, `${email:ga-admin}`,
`${saved:key}`, `{{repeat:x:1000}}` and `{{name:65}}` are explained in
`vmtest/resolve.py`. Counts and lists are checked through the name filter on
the scenario's own uid. "Nothing stored" is proven through the list with a
name filter (internal) or a search by namespace (external), never through a
read of the rejected name.

## Tags

- `@AC_<story>_<ACn>`: the acceptance criterion a scenario checks.
- `@D<n>`, `@E<n>`, `@Q<n>`: the decisions it depends on.
- Rule ids as is: `@OWASP-02`, `@OWASP-08`, `@OWT-A02`, `@OWT-A05`, `@OWT-A07`,
  `@OWT-A10`, `@OWA-API3`, `@API-12`, `@API-36` to `@API-38`, `@API-44` to
  `@API-47`, `@BLG-err-problem`, `@BLG-prb-type`, `@BLG-req-valid`,
  `@BLG-stat-codes`, `@BLG-col-paging`, `@BLG-col-sort`, `@SEC-05`, `@MACH-08`,
  `@NFR-04`, `@NFR-07`, `@NFR-10`.
- `@negative`: error and abuse cases. `@ui`: UI scenarios. `@store_down`: needs
  the backend without its store. `@wip`: cannot run against the API; excluded
  from every mode.

API-46 (problem type `urn:problem-type:simpl:...`) is checked only on 401, 403,
406, 409, 413 and 415; 400, 404, 500 and 503 keep the belgif types (D48).

## Load test

`./run.sh load` seeds the Q4 data set if it is not there yet (1000 vocabularies
named `LoadSeed0000` to `LoadSeed0999`, one vocabulary with 20 versions, one
external vocabulary; seed data is reused across runs), then runs 19 reader
users at 20 reads per second in total and 1 writer at 1 write per second for
`LOAD_DURATION`. It writes p50, p95, p99, error rate and throughput per
operation, with the Q1 target and whether it was met, to `reports/load.json`.

## Regenerate COVERAGE.md

```
python3 tools/coverage.py ../input
```
