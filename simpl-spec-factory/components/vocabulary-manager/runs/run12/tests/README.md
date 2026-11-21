# Vocabulary Manager: black-box API test suite (step 40)

Written from the stories, the OpenAPI and the decisions of step 30 only. It talks HTTP to
whatever implementation runs at `API_URL`; nothing in it depends on a build.

## How to run

```
./run.sh api          # default API scenarios            -> reports/api.json
./run.sh store-down   # backend without its store (D44)   -> reports/store-down.json
./run.sh load         # Q1 load test (Locust)             -> reports/load.json
./run.sh ui           # @ui scenarios in Chromium         -> reports/ui.json, reports/axe/
```

Each mode creates its virtualenv on first use (`.venv`, `.venv-load`) and exits 0 when the run
itself worked, whether scenarios pass or fail. Exit 2: virtualenv setup failed; 3: no report.
`./run.sh ui` uses its own virtualenv (`.venv-ui`, `requirements-ui.txt`: behave, Playwright for
Python, axe-playwright-python) and installs Chromium. Every scenario gets a new browser context,
so no stored session is reused; the user signs in through the Keycloak login form (E1).
`UI_HEADED=1` shows the browser; `UI_TIMEOUT_MS` (default 15000) sets the wait for elements.

Check without a running system:

```
.venv/bin/behave --dry-run          # parses every feature, reports undefined steps
.venv/bin/python tools/coverage.py  # regenerates COVERAGE.md from the tags
```

## Environment variables

| Variable | Default (build profile) | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base; paths in scenarios are relative to it |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend with an unreachable store (`@store_down`) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | password grant |
| `TOKEN_CLIENT` | `vocabulary-tests` | client with the audience mapper |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | E7: token without audience, expect 401 |
| `TOKEN_URL_OTHER` | `http://localhost:8193/realms/simpl-other/protocol/openid-connect/token` | E7: wrong issuer, expect 401 |
| `TOKEN_CLIENT_OTHER` | `vocabulary-tests` | client in the other realm |
| `USER_ADMIN`, `USER_VIEWER`, `USER_NOROLE` | `ga-admin`, `ga-viewer`, `ga-norole` | E1 users |
| `USER_PASSWORD` | `Password1!` | E1 password |
| `USER_ADMIN_EMAIL` | `ga-admin@example.com` | expected `createdBy` (D27) |
| `CONTENT_BASE_URL` | learned from the first `contentHref` | contentBase of D12 and E2 |
| `HTTP_TIMEOUT`, `UPLOAD_TIMEOUT` | `60`, `90` seconds | client timeouts |
| `UI_URL` | `http://localhost:4433` | for step 60 |

Load test only: `LOAD_SEED_ID` (`Q4`), `LOAD_SEED_VOCABULARIES` (`1000`), `LOAD_SEED_VERSIONS` (`20`),
`LOAD_SEED_THREADS` (`4`, at most 4 by D92), `LOAD_SKIP_SEED` (`0`), `LOAD_DURATION` (`300` s), `LOAD_WARMUP` (`30` s),
`LOAD_READ_RATE` (`20`/s), `LOAD_WRITE_RATE` (`1`/s), `LOAD_BIG_WRITES` (`3`).

## Data

The store is never reset (E4). Every scenario gets a run id of 10 letters and digits and builds
its names from it: `{{Health}}` in a step means `Health<runid>`. Lists and "nothing stored" checks
filter on the run id (`GET /vocabularies?name=<runid>`), so other data in the store never matters.
External vocabularies use namespaces that contain the run id; "nothing registered" is checked with
the external search on that namespace.

Placeholders: `{{Alias}}`, `{{upper:Alias}}`, `{{run}}`, `{{contentBase}}`, `{{adminEmail}}`,
`{{today}}`, `{{tomorrow}}`, `{{Long64}}`, `{{Long65}}`, and any value saved with
`I remember the response field ... as "key"`. They work in step text, tables, doc strings and
the Turtle files in `fixtures/`.

## Layout

| Path | Content |
|---|---|
| `features/*.feature` | Gherkin scenarios |
| `features/steps/api_steps.py` | the one shared step library for the API |
| `features/steps/ui_steps.py` | Playwright steps for the @ui scenarios (decided texts and names only) |
| `features/environment.py` | per-scenario run id and state; `@store_down` uses `STORE_DOWN_API_URL` |
| `lib/vmclient.py` | configuration, tokens, raw HTTP, generated Turtle of an exact size |
| `fixtures/` | Turtle templates |
| `load/run_load.py` | Locust load test (seed, throughput mix, 5 MB writes) |
| `tools/coverage.py` | writes `COVERAGE.md` |

## Tags

- `@AC_<story>_<ACn>`: the criterion checked, for example `@AC_SIMPL-18170_AC1`.
- `@D12`, `@E5`, `@Q2`: decisions the scenario depends on. Every scenario without a criterion tag has one.
- Rule ids as given in `rules.json`: `@OWASP-02`, `@NFR-04`, `@API-46`, `@BLG-err-problem`, ...
- `@negative`: error and abuse cases. `@ui`: UI scenarios (`./run.sh ui`). `@a11y`: axe WCAG 2.1 AA check of a page.
  `@wip`: cannot run against the API. `@store_down`: needs the store-less backend.

Default API run: `--tags=~@ui --tags=~@wip --tags=~@store_down`.

## Load test

`./run.sh load` seeds 1000 vocabularies with 20 versions of about 10 KB (D99; skipped for names
that already exist, so a second run only tops up), then offers 20 reads/s and 1 write/s for
5 minutes after a 30 s warm-up, then runs three creates with a 5 MB file one at a time (D71).
`reports/load.json` holds per operation: requests, failures, error rate, p50, p95, p99, achieved
rate, the p95 target and whether it is met; plus the achieved throughput against the offered rate
(met at 95 % with no errors). Seeding 20 000 versions takes a while; use `LOAD_SKIP_SEED=1` when
the seed is already in place.

## Assumptions made while writing the suite

- Every 400 problem has type `urn:problem-type:belgif:badRequest` (all OpenAPI 400 examples; D48 keeps belgif for 400).
- Missing or invalid form fields and JSON properties are issues `in: body` named after the field (D55: tests check `in` and `name`).
- S1 locations are checked with a language-tagged literal only; whether a plain literal is written with `^^xsd:string` is left open (D140 says "when present").
- The case-insensitive duplicate name (D37) is checked on status and type only; D15 does not say which spelling the detail uses.
- A file one byte over the limit (5 242 881 bytes) is "above"; 5 242 880 bytes is "at" the limit. 6 MB is read as 6 291 456 bytes; the over-6-MB test sends about 6.4 MB.
- URL-too-long and headers-too-large are tested with 200 000 characters, assuming the raised server limits of D158 stay below that.
- The syntax error location is checked for the line only (`line 3` with an optional column, D60, D65).
