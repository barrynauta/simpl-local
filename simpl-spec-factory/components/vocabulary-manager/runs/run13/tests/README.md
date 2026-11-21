# Vocabulary Manager: black-box API test suite

Written from the specifications and decisions of run 13 (`input/`), without the build. Python 3,
behave and requests (build profile `tests.harness`); Locust for the load test.

## Run

```
./run.sh api          # default API scenarios        -> reports/api.json (behave JSON)
./run.sh ui           # UI scenarios, headless Chromium -> reports/ui.json
./run.sh store-down   # backend without its store    -> reports/store-down.json
./run.sh load         # load test (step 70)          -> reports/load.json
./run.sh dry-run      # parse check, no system needed -> reports/dry-run.json
```

Each mode creates its own virtualenv on first use (`.venv`, `.venv-load`) and exits 0 when the run
itself worked, whether scenarios pass or fail. Extra arguments go to behave, for example
`./run.sh api features/vocabulary_status.feature` or `./run.sh api --tags=@D46`.
`./run.sh ui` uses its own virtualenv `.venv-ui` (`requirements.txt` plus `requirements-ui.txt`:
Playwright and axe) and installs Chromium on first use.

The suite never resets the store (E4). Every scenario makes its own names from a fresh token, so it
can run on a store that already holds data, and scenarios do not depend on each other.

## Environment variables

| Variable | Default (build profile) | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend with an unreachable store (D44), store-down mode |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | password grant |
| `TOKEN_CLIENT` | `vocabulary-tests` | client with the audience mapper |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | client without the audience mapper (E7, expect 401) |
| `TOKEN_URL_OTHER` | `http://localhost:8193/realms/simpl-other/protocol/openid-connect/token` | other realm (E7, expect 401) |
| `TOKEN_CLIENT_OTHER` | `vocabulary-tests` | client in the other realm |
| `UI_URL` | `http://localhost:4433` | UI (CSP scenario and `./run.sh ui`) |
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | contentBase of E2, for contentHref and internal references |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | E1 users |
| `ADMIN_EMAIL`, `VIEWER_EMAIL`, `NOROLE_EMAIL` | `<user>@example.com` | expected createdBy (D27) |
| `USER_PASSWORD` | `Password1!` | E1 password |
| `HTTP_TIMEOUT` | `75` | seconds per request |
| `LOAD_SEED_VOCABS`, `LOAD_SEED_VERSIONS` | `1000`, `20` | seed of D99 |
| `LOAD_DURATION`, `LOAD_WARMUP` | `300`, `30` | seconds |
| `LOAD_READ_RATE`, `LOAD_WRITE_RATE`, `LOAD_READ_USERS` | `20`, `1`, `20` | offered load of Q1 and Q4 |
| `LOAD_BIG_WRITES` | `5` | number of 5 MB writes (D71) |
| `LOAD_SKIP_SEED` | unset | `1` skips seeding |

## Layout

- `features/*.feature`: scenarios, one feature per area. `features/steps/api_steps.py` holds the
  API steps; the shared library is `vmtests/` (configuration, tokens, raw HTTP and multipart,
  names and Turtle content). `features/steps/ui_steps.py` holds the Playwright steps of the @ui scenarios.
- `fixtures/`: Turtle files. `{name}` is the vocabulary being created, `{tok}`, `{base}` and
  `{Alias}` as in the steps.
- `load/run_load.py`: the load test. `tools/coverage.py`: writes `COVERAGE.md`.

## Tags

| Tag | Meaning |
|---|---|
| `@AC_<story>_<ACn>` | acceptance criterion the scenario checks |
| `@D<n>`, `@E<n>`, `@Q<n>` | decision it depends on |
| `@OWASP-02`, `@OWT-A07`, `@API-46`, `@NFR-04`, `@BLG-err-problem`, ... | rule it checks (rule id as is) |
| `@negative` | error and abuse cases |
| `@ui` | UI scenario, run by `./run.sh ui` (Playwright) |
| `@manual` | UI scenario that cannot be automated without a missing decision; reason in COVERAGE.md |
| `@store_down` | needs the backend without its store; only `./run.sh store-down` runs it |
| `@wip` | cannot run against the API; never run |

## Writing conventions in the scenarios

- Placeholders in step texts, tables and docstrings: `{Alpha}` is the unique name of alias Alpha
  (`Alpha` plus the scenario token), `{tok}` the token, `{base}` the content base, `{admin_email}`
  the admin's email, `{$key}` a remembered value. Filters: `{tok:lower}`, `{Alpha:upper}`,
  `{$created:date}`, `{$created:plus1s}`, `{$created:plus1d}`, `{$created:nooffset}`,
  `{$created:offset1h}` (same instant with +01:00, URL encoded).
- Part tables (`| part | value |`) build multipart requests byte by byte. Value forms:
  `default-turtle`, `file:<fixture>`, `turtle-of-size:<bytes>`, `s4-errors:<count>`,
  `empty-file`, `binary-file`, `text-of-length:<n>`, `padded-text:<n>` (two blanks each side),
  `blank:<n>`, `empty-text`, `pad-request-to:<bytes>` (last row: pads the whole body to that size).
  A part named `vocabularyFile` or given a file form is sent as a `text/turtle` file part.
- JSON checks (`the response JSON has`) take `<absent>`, `<present>`, `<date-time>` (UTC, to the
  second, `Z`), `<empty list>`, `<list>`, `<not true>`, or the exact value.
- "Nothing stored" is proven through the list filtered on the scenario token or on a valid name,
  never through a read of the rejected name (brief, D49).

## UI scenarios (step 60)

The @ui scenarios use only roles, decided labels and decided texts (D75). Their steps are the
phrases implemented in `features/steps/ui_steps.py` with Playwright for Python. Every scenario starts in
a new browser context, without a stored session. The assumptions of the steps are listed at the top of
that file and in COVERAGE.md. "the API answers <METHOD> <path> with the
status <n>" means a route stub in the browser (D24); "... and the detail <d>" gives the stub a
problem+json body with that detail. The CSP scenario in `ui_access.feature` is not tagged @ui: it
uses HTTP steps against UI_URL and runs in `./run.sh api`.

## Known tool quirk

behave 1.3.3 marks a feature as "failed" in `--dry-run` when a Scenario Outline comes before a
plain Scenario, even with no failing or undefined step. The features start with a plain scenario
to avoid the noise; `./run.sh dry-run` fails only on undefined steps or errors.
