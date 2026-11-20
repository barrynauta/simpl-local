# Vocabulary Manager: black-box API test suite

Written from the specifications and decisions of run 11 (stories, OpenAPI `vocabulary_openapi-tier1.yaml`,
decisions D1 to D141, E1 to E7, Q1 to Q6). It talks HTTP only and runs unchanged against any
implementation of the component.

## Run

```
./run.sh api          # default API scenarios            -> reports/api.json (behave JSON)
./run.sh store-down   # backend without its store         -> reports/store-down.json
./run.sh ui           # @ui scenarios in headless Chromium  -> reports/ui.json, reports/a11y/
./run.sh load         # Q1 load test (Locust), not default -> reports/load.json
./run.sh dry-run      # parse, check undefined and ambiguous steps and tags; needs no system
```

Each mode creates its own virtualenv on first use (`.venv` for behave and requests, `.venv-load` for
Locust). `run.sh` exits 0 when the run itself worked, whether scenarios pass or fail; it exits 2 when no
report could be written. `./run.sh ui` uses its own virtualenv `.venv-ui` (behave, Playwright, axe-playwright-python) and installs
Chromium for Playwright on first use. Screenshots of failed UI scenarios go to `reports/ui-screens/`.

## Environment variables

| Variable | Default | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base; paths in the features are relative to it. A path starting with `//` is relative to the host (used for `/actuator`). |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | Backend whose store is unreachable (`./run.sh store-down`). |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | Password grant for the test users. |
| `TOKEN_CLIENT` | `vocabulary-tests` | Client with the audience mapper. |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | Same realm, no audience: expect 401 (E7). |
| `TOKEN_URL_OTHER` | `http://localhost:8193/realms/simpl-other/protocol/openid-connect/token` | Other issuer: expect 401 (E7). |
| `USER_ADMIN`, `USER_VIEWER`, `USER_NOROLE` | `ga-admin`, `ga-viewer`, `ga-norole` | Test users of E1. |
| `USER_PASSWORD` | `Password1!` | Their password. |
| `USER_ADMIN_EMAIL` (and `_VIEWER_`, `_NOROLE_`) | `ga-admin@example.com` ... | Expected `createdBy` (D27). |
| `CONTENT_BASE` | learnt from `contentHref` | Content base of E2 / D12; set it only to skip the probe vocabulary. |
| `RUN_ID` | random | Prefix of all generated names. |
| `HTTP_TIMEOUT` | `120` | Seconds per request. |
| `UI_URL` | `http://localhost:4433` | For the `@ui` scenarios. |
| `UI_WAIT` | `15` | Seconds a UI step waits for the page to reach the expected state. |
| `UI_HEADED` | unset | `1` shows the browser window. |

Load test only: `LOAD_RUN_TIME` (default `5m`), `LOAD_READ_USERS` (20), `LOAD_READS_PER_SECOND` (20),
`LOAD_WRITES_PER_SECOND` (1), `LOAD_BIG_WRITES` (3), `SEED_VOCABULARIES` (1000), `SEED_VERSIONS` (20),
`SEED_FILE_BYTES` (10240), `SEED_WORKERS` (4), `SEED_EXTERNALS` (10).

## Data

The store is never reset (E4). Every scenario gets a unique id (`RUN_ID` plus a counter) and builds
its names from it: alias `A` becomes `V<id>A`. Lists are checked through the name filter on the
scenario id. "Nothing stored" is proved through the list with a name filter, or for external
vocabularies through a GET by a valid name (404).

Placeholders in feature text: `${A}` name of alias A, `${upper:A}` and `${lower:A}`, `${NS:A}` own
namespace used in A's Turtle, `${URI:X}` a unique external URI, `${CB}` content base, `${SID}`
scenario id, `${TODAY}`, `${EMAIL:admin}`, `${R:key}` a remembered value with optional
`:+1s`, `:-1s`, `:noz`, `:date`, `:offset+01:00`. A `\"` inside a quoted step argument is a quote.

## Tags

- `@AC_<story>_<ACn>`: the acceptance criterion a scenario checks.
- `@D<n>`, `@E<n>`, `@Q<n>`: decisions and quality targets the scenario depends on.
- Rule ids as they are (`@OWASP-02`, `@API-46`, `@NFR-04`, `@BLG-err-problem`, ...).
- `@negative`: error and abuse cases. `@ui`: UI scenarios (`./run.sh ui`). `@a11y`: axe check of a main page.
  `@manual`: cannot be automated (none at present).
  `@store_down`: needs the backend without store. `@wip`: cannot run against an API (none at present).

Run a subset with behave tags, for example
`.venv/bin/behave --tags='@D38' --tags='~@ui' features` (behave 1.2.6 uses the `~@tag` form for 'not').

## Layout

```
features/*.feature        scenarios (ui_*.feature are @ui)
features/steps/library.py the shared API step library
features/steps/ui.py      browser steps of the @ui scenarios (Playwright)
support/ui_hooks.py       browser lifecycle and API stubs for the @ui scenarios
features/environment.py   per-scenario state
support/vm.py             configuration, tokens, HTTP, names, Turtle generation
fixtures/                 Turtle files used by scenarios
load/                     Locust load test (seed.py, locustfile.py, big_writes.py, report.py)
tools/check_suite.py      static check (dry-run mode)
tools/coverage.py         writes COVERAGE.md from the tags
```

## Load test

`./run.sh load` seeds the design load of Q4 / D99 when it is not there yet (1000 vocabularies with 20
versions of about 10 KB, 10 external vocabularies; the seed is recorded in `reports/load-seed.json`
and reused), then runs Locust headless: 20 read users offering 20 reads per second over every read
operation Q1 names, and one write user offering 1 write per second (create or new version, files up to
1 MB), for 5 minutes. Then it times `LOAD_BIG_WRITES` creates with a 5 MB file apart from the mix
(D71). `reports/load.json` holds per operation the request count, failures, error rate, p50, p95,
p99 and rate, plus the verdicts of Q1: read p95 at most 300 ms per operation, write p95 at most 3 s,
5 MB writes at most 30 s, achieved rate at least 95 % of the offered rate, no errors.
Seeding 20 000 versions takes a while (4 parallel uploads, D92).
