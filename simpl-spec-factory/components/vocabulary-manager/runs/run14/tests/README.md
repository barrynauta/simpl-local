# Vocabulary Manager test suite (run 14, steps 40 and 60)

Black-box suite for the Vocabulary Manager, written from the stories, the OpenAPI and the decisions of
step 30. HTTP only: it never reads the build, never resets the store, and every scenario creates its own
data under a unique run id (E4).

## Run

```
./run.sh api          # default API scenarios        -> reports/api.json (behave JSON)
./run.sh store-down   # needs the store-down backend -> reports/store-down.json
./run.sh load         # Q1 load test (Locust)        -> reports/load.json
./run.sh ui           # @ui scenarios in Chromium    -> reports/ui.json, axe details in reports/axe/
./run.sh dry-run      # parse check, no system       -> reports/dry-run.txt
```

`run.sh` creates `.venv` on first use (Python 3, behave 1.3.3, requests; Locust for `load`). It exits 0
when the run itself worked, whether scenarios pass or fail, and 2 when it could not run. Extra arguments
go to behave, for example `./run.sh api -i vocabulary_create -n "duplicate"`.

`api` runs `not @ui and not @wip and not @store_down`. `store-down` runs `@store_down` against
`STORE_DOWN_API_URL`. `ui` (step 60) installs Playwright and Chromium from `requirements-ui.txt` and runs
`@ui and not @manual and not @wip` against `UI_URL`. Every UI scenario starts in a new browser context, so no
stored session is reused; it logs in on the Keycloak page. `UI_HEADLESS=0` shows the browser,
`UI_TIMEOUT_MS` sets the wait per action (default 15000).

## Environment variables

| Variable | Default (build.yaml) | Use |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base |
| `STORE_DOWN_API_URL` | `http://localhost:8199/v1` | backend without its store (D44) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | password grant (E1, D40) |
| `TOKEN_CLIENT` | `vocabulary-tests` | public test client |
| `TOKEN_CLIENT_NOAUD` | `vocabulary-tests-noaud` | token without the audience, expect 401 (E7) |
| `TOKEN_CLIENT_SHORT` | `vocabulary-tests-short` | 10 s tokens, expect 401 after expiry (E8) |
| `TOKEN_URL_OTHER` | `http://localhost:8193/realms/simpl-other/protocol/openid-connect/token` | wrong issuer, expect 401 (E7) |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | test users (E1) |
| `USER_PASSWORD` | `Password1!` | password of the test users |
| `ADMIN_EMAIL` | `ga-admin@example.com` | expected `createdBy` (D27); the features use the E1 value |
| `CONTENT_BASE` | empty | contentBase (E2); when empty it is read from a `contentHref` |
| `HTTP_TIMEOUT` | `60` | seconds per request |
| `UI_URL` | `http://localhost:4433` | UI base for `./run.sh ui` |

## Layout

- `features/*.feature`: API scenarios; `features/ui/*.feature`: UI scenarios (`@ui`).
- `features/steps/api_steps.py`: API steps; `features/steps/ui_steps.py`: browser steps (step 60);
  `features/environment.py`: run id per scenario and a fresh browser context per `@ui` scenario.
- `vmtest/ui.py`: the shared browser process and the per-scenario session.
- `vmtest/`: configuration, HTTP and token helpers, Turtle generators, placeholders.
- `fixtures/`: static files (`binary.png`). Turtle content is inline in the scenarios or generated.
- `load/`: Locust load test for Q1. `tools/`: dry-run check and the coverage generator.

### Placeholders in the feature files

`$Alpha` is a name unique to the scenario (`Alpha` + run id); `$Alpha!upper` and `$Alpha!lower` change its
case; `$RUN` is the run id (used as a name filter to see only the scenario's own vocabularies); `$CB` is
contentBase; `$EXT` is an external namespace base unique to the scenario; `$N64`, `$N65` are names of 64 and
65 characters; `$N3` is an unused 3-character name; `${key}` is a value saved earlier in the scenario.
Multipart tables accept `@valid`, `@file`, `@size:<n>`, `@repeat:<c>:<n>`, `@padded:<c>:<n>`, `@empty`.

## Tags

| Tag | Meaning |
|---|---|
| `@AC_<story>_<ACn>` | acceptance criterion checked, for example `@AC_SIMPL-18170_AC1` |
| `@D12`, `@E1`, `@Q2` | decision the scenario depends on |
| `@OWASP-02`, `@API-46`, `@BLG-err-problem`, `@NFR-04` ... | rule checked (rule id as in rules.json) |
| `@negative` | error or abuse case |
| `@ui` | UI scenario (step 60) |
| `@a11y` | axe check, WCAG 2.1 AA (Q6, NFR-10, D123) |
| `@manual` | UI scenario that cannot be automated; reason in COVERAGE.md (none in run 14) |
| `@wip` | cannot run against the API (SIMPL-18162/AC4, D179) |
| `@store_down` | needs the backend without its store |
| `@slow` | takes more than 10 s (expired token waits about 80 s, 5 MB uploads) |

## Load test

`./run.sh load` runs `load/run_load.py`:

1. Seed (D99): tops up 1000 vocabularies `Load00000` to `Load00999` with 20 versions of about 10 KB each.
   Idempotent; `LOAD_SEED=0` skips it. The first seed takes a while (20 000 uploads, 4 in parallel, D92).
2. Throughput mix: 20 reads/s and 1 write/s offered (D99), 30 s warm-up then 5 minutes measured. One Locust
   task per operation Q1 names: getVocabularies, getVocabulary, getVocabularyVersions, getVocabularyVersion,
   searchVocabularies, downloadVocabulary, downloadVocabularyVersion, getExternalVocabularies (reads) and
   createVocabularyVersion with 10 KB, 100 KB and 1 MB files (write, 3 writers at 1/3 write/s each so a slow write does not lower the offered rate). No retries; a 503 is an error (D183).
3. 5 MB writes apart (D71, D99): 3 new versions at the 5 MB limit, one after the other.

`reports/load.json` holds per operation `p50`, `p95`, `p99` (ms), `error_rate`, `requests` and `rps`, the
achieved rates, and one verdict per Q1 target (read p95 <= 300 ms per operation, write p95 <= 3 s, 5 MB
write <= 30 s, throughput >= 95 % of the offered rate with no errors). Settings: `LOAD_DURATION`,
`LOAD_WARMUP`, `LOAD_VOCABULARIES`, `LOAD_VERSIONS`, `LOAD_READS_PER_SECOND`, `LOAD_WRITES_PER_SECOND`,
`LOAD_READ_USERS`, `LOAD_WRITE_USERS`, `LOAD_BIG_WRITES`, `LOAD_SEED`, `LOAD_SEED_THREADS`, `LOAD_PREFIX`.

## Coverage

`python3 tools/coverage.py` regenerates `COVERAGE.md` from the tags.
