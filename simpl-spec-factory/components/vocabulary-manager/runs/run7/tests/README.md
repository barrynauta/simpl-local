# Vocabulary Manager: black-box API test suite

Written from the stories, the OpenAPI file and the decisions of step 30 only. HTTP, black box.
Python 3, behave and requests; Locust for the load test (build profile `build.yaml`).

## How to run

```
./run.sh api          # default API scenarios      -> reports/api.json (behave JSON)
./run.sh store-down   # backend without its store  -> reports/store-down.json
./run.sh load         # load test, Q1 at Q4 load   -> reports/load.json
./run.sh ui           # @ui scenarios in Chromium (Playwright)  -> reports/ui.json, reports/a11y/
./run.sh dry-run      # parse check, no system needed
```

`run.sh` creates `.venv` on first use and installs `requirements.txt`. It exits 0 when the run itself
worked (a valid report was written), whether scenarios pass or fail, and 2 when it could not run.
The component must already be running; the suite never starts it and never resets the store.

## Environment variables

| Variable | Default | Meaning |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base, paths are appended to it |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | API base of the backend whose store is unreachable (D44); used by `@store_down` scenarios |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | Keycloak token endpoint |
| `TOKEN_CLIENT` | `vocabulary-tests` | Public client with the password grant |
| `TOKEN_CLIENT_SECRET` | empty | Only if the client is confidential |
| `TEST_PASSWORD` | `Password1!` | Password of the test users (E1) |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | Test users (E1) |
| `ADMIN_EMAIL`, `VIEWER_EMAIL`, `NOROLE_EMAIL` | `<user>@example.com` | Expected `createdBy` (D27) |
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | contentBase of E2, used in `contentHref` checks and internal references (D12) |
| `UI_URL` | `http://localhost:4433` | UI base for the `@ui` scenarios |
| `UI_TIMEOUT` | `15` | Seconds a UI step waits for an element or text |
| `HEADED` | empty | Set to any value to watch the browser |
| `REQUEST_TIMEOUT` | `90` | Seconds per HTTP request |
| `LOAD_USERS`, `LOAD_SPAWN_RATE`, `LOAD_DURATION` | `20`, `5`, `5m` | Load test shape |
| `LOAD_SEED_ID`, `LOAD_SEED_VOCABULARIES`, `LOAD_SEED_VERSIONS`, `LOAD_SEED_PARALLEL` | `q4seed`, `1000`, `20`, `16` | Seed of the Q4 volume, reused across runs |
| `LOAD_5MB_WRITES`, `LOAD_5MB_INTERVAL` | `3`, `60` | 5 MB writes and the seconds between them |
| `MAX_EXT_PAGES` | `1000` | Pages the external-list checks read before they fail |

## Layout

- `features/*.feature`: scenarios. `features/steps/api_steps.py` is the one shared step library;
  `features/steps/ui_steps.py` holds the Playwright browser steps (step 60). Each `@ui` scenario opens a
  new browser context, so no stored session is reused; it signs in on the Keycloak login page (E1).
  Failed UI scenarios leave a screenshot in `reports/ui-failures/`; axe results go to `reports/a11y/`.
- `vmlib/client.py`: configuration, tokens (password grant, cached), HTTP calls, Turtle generators.
- `fixtures/*.ttl`: Turtle files; `${...}` placeholders are filled per scenario.
- `load/locustfile.py`: load test. `tools/coverage.py`: regenerates the tables of `COVERAGE.md`.

Every scenario gets a run id (10 hex characters). Vocabulary names are `V<runid><Alias>`, external names
`E<runid><Alias>`, so a list filtered on the run id shows only the scenario's own data (E4). "Nothing was
stored" is proven with the list and a name filter, never with a read on a rejected name (D49).

Placeholders in feature text: `${V:Alpha}`, `${VU:Alpha}` (upper case), `${E:Dc}`, `${NS:Dc}` (external
namespace), `${CB}` (content base), `${rid}`, `${RID}`, `${R:x:1000}` (repeat), `${NAME:64}` (name of that
length), `${S:key}` and `${SD:key}` (saved value and its date part), `${EMAIL:ga-admin}`.
In expected issue and warning tables a cell starting `^` is a prefix match, `~` a regular expression, and an
empty cell is not checked.

## Tags

| Tag | Meaning |
|---|---|
| `@AC_<story>_<ACn>` | Acceptance criterion the scenario checks, for example `@AC_SIMPL-18170_AC1` |
| `@D12`, `@E1`, `@Q2` | Decision or quality target it depends on |
| `@OWASP-02`, `@API-46`, `@BLG-err-problem`, `@NFR-04` ... | Rule it checks, id as in `rules.json` |
| `@negative` | Error or abuse case |
| `@ui` | UI scenario, runs with `./run.sh ui` |
| `@a11y` | UI scenario with an axe WCAG 2.1 AA check (Q6) |
| `@manual` | UI scenario that cannot be automated; left out of `./run.sh ui` (none at present) |
| `@wip` | Cannot run against the API (kept for coverage) |
| `@store_down` | Needs the backend without its store (`./run.sh store-down`) |
| `@slow` | Uploads a 5 MB file |

Run a subset: `.venv/bin/behave features --tags="@D46 and not @ui"`.

## Load test

`./run.sh load` seeds the Q4 volume (1000 vocabularies with 20 versions, reused between runs), warms up,
resets the statistics and then runs for `LOAD_DURATION`: 18 reader users at about 20 requests per second,
one writer that starts one write per second with an open arrival rate (1 MB files), and one user for the
three 5 MB versions on its own vocabulary. `reports/load.json` holds p50, p95, p99, the error rate and the request rate per
operation, the Q1 target per operation and whether it was met, and the overall throughput. It is not part
of the default run.

## Notes

- behave 1.3.3 marks a feature as failed in a dry run when its first scenario is an outline followed by a
  plain scenario; each feature therefore opens with a plain scenario.
- Assumptions taken where the decisions leave room are listed in `COVERAGE.md`.
