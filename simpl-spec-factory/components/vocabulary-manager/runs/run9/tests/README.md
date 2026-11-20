# Vocabulary Manager: black-box API suite (step 40, run 9)

Python 3, behave and requests. HTTP only. The suite is written from the specs and the decisions of
step 30 and runs unchanged against any implementation of the component.

## How to run

```
./run.sh api          # default API scenarios        -> reports/api.json
./run.sh store-down   # backend without its store     -> reports/store-down.json
./run.sh ui           # browser scenarios (@ui)        -> reports/ui.json, reports/a11y/
./run.sh load         # Q1 load test (Locust)         -> reports/load.json
./run.sh dry-run      # parse check, no system needed -> reports/dry-run.json
```

Each mode creates `.venv` on first use (`requirements.txt`; the load mode adds `requirements-load.txt`).
`run.sh` exits 0 when the run itself worked, whether scenarios pass or fail; it exits non-zero only
when no report could be written. Reports are behave JSON (load: its own JSON).

Mode `api` runs every scenario except `@ui`, `@wip` and `@store_down`. Mode `store-down` runs only
`@store_down`, against `STORE_DOWN_API_URL`. Mode `ui` runs `@ui` (not `@wip`, not `@manual`) in headless Chromium; it installs `requirements-ui.txt`
and the Playwright Chromium on first use. Every scenario starts a new browser context without stored
sessions and signs in on the Keycloak page. `UI_URL` (default `http://localhost:4433`), `UI_TIMEOUT_S`
(15) and `UI_HEADED` (set to show the browser) apply.

## Environment variables

| Variable | Default (build profile) | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend whose store is unreachable (D44) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | password grant |
| `TOKEN_CLIENT` | `vocabulary-tests` | public test client with the audience mapper |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | client without audience (E7, expect 401) |
| `TOKEN_URL_OTHER` | `.../realms/simpl-other/...` | other issuer (E7, expect 401) |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | users of E1 |
| `USER_PASSWORD` | `Password1!` | password of the test users |
| `ADMIN_EMAIL` | `ga-admin@example.com` | expected `createdBy` (D27) |
| `CONTENT_BASE_URL` | (unset) | contentBase of E2; when unset it is learnt from the first `contentHref` |
| `REQUEST_TIMEOUT` | `90` | seconds per request |

Load test only: `SEED_VOCABULARIES` (1000), `SEED_VERSIONS` (20), `SEED_FILE_BYTES` (10240),
`SEED_WORKERS` (4), `LOAD_SEED` (1; 0 skips seeding), `LOAD_SEED_PREFIX` (`Loadseed`),
`LOAD_DURATION` (`5m`), `LOAD_READS_PER_SECOND` (20), `LOAD_WRITES_PER_SECOND` (1),
`LOAD_READ_USERS` (20), `LOAD_WRITE_USERS` (4), `LOAD_WRITE_FILE_BYTES` (10240),
`LOAD_LARGE_WRITE_BYTES` (1 MB, the 1 MB share of the writes), `LOAD_WARMUP_S` (30, left out of the
statistics), `LOAD_BIG_WRITES` (3).

## Data and isolation

The store is never reset (E4). Every scenario gets a run id; a vocabulary alias `Health` becomes
`Health<runid>`, external namespaces are `https://ext.example.org/<runid>/...`. Lists are checked
through the name filter on the run id. 'Nothing stored' is proved by that filter, or by a search on
a namespace, never by a read on a rejected name. Set-up steps run as `ga-admin`; actions run as the
user the scenario names (default `ga-admin`).

Placeholders in steps and fixtures: `${Alias}` vocabulary name, `${ext:Alias}` external name,
`${run}`, `${contentBase}`, `${extns:X}`, `${adminEmail}`, `${repeat:c:N}`, `${pad:N}` (run-unique
name of N characters), `${upper:Alias}`, `${remembered:key:field}`, `${day:key:field}`. Multipart tables also take `@content-of:Alias:n` and
`@description-of:Alias:n` (the uploaded bytes and description of version n), and `@omit` (leave the part out).

## Layout

- `features/*.feature`: scenarios; `features/steps/vm_steps.py`: the one shared step library;
  `features/environment.py`: per-scenario state. `lib/vmtest/`: HTTP, tokens, names, Turtle generators.
- `fixtures/`: Turtle files (syntax error at line 3, rules S1 to S6, relative IRIs, CPSV-like, exact bytes).
- `load/locustfile.py`, `load/run_load.py`: the Q1 load test.
- `features/steps/ui_steps.py`: browser steps ('the user ...', 'the UI ...'), Playwright and axe-core.
  See COVERAGE.md section 5 for how elements are found and the assumptions.

## Tags

- `@AC_<story>_<ACn>`: the criterion a scenario checks, for example `@AC_SIMPL-18170_AC1`.
- `@D12`, `@E7`, `@Q2`: the decisions it depends on.
- Rule ids as is: `@OWASP-02`, `@API-46`, `@NFR-04`, `@BLG-err-problem`, ...
- `@negative`: error and abuse cases. `@ui`: browser scenarios (`./run.sh ui`). `@wip`: cannot run
  against an API. `@store_down`: needs the backend without its store.

Examples: `.venv/bin/behave --tags="@D46" features`, `.venv/bin/behave --tags="@AC_SIMPL-18173_AC1" features`.

## Load test

`./run.sh load` seeds 1000 vocabularies with 20 versions of about 10 KB (D99, Q4; already seeded ones
are kept), runs Locust headless for 5 minutes at an offered 20 reads/s and 1 write/s, then times
3 creates with a file at the 5 MB limit (D71). `reports/load.json` holds, per operation, p50, p95,
p99, error rate and achieved rate, the targets of Q1 and the verdicts (`q1_met`). Seeding 20 000
versions takes a while; use `LOAD_SEED=0` when the store is already seeded.

## Known limits of the suite

- Dry-run: 0 undefined steps. behave 1.3.3 marks two features 'failed' in dry-run when an outline
  is followed by a plain scenario; no step or scenario fails, it is a status artefact of dry-run.
- A 500 (SIMPL-18162/AC4) cannot be caused from outside; that scenario is `@wip`.
- The 5 MB scenarios and the parallel-upload scenario send large bodies; allow a few minutes.
