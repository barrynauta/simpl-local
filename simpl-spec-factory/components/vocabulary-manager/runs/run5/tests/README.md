# Vocabulary Manager: black-box API suite (run 5, step 40)

Behave scenarios for the Vocabulary Manager, written from the specs and the decisions of
step 30 only. The suite talks HTTP to the API and to the token endpoint. It never resets
the store: every scenario creates its own data with a unique run id (decision E4).

## Run

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/behave                       # API scenarios; @ui, @wip and @store-down are skipped
.venv/bin/behave --tags=~@negative     # positive paths only
.venv/bin/behave --tags=@AC_SIMPL-18173_AC1
.venv/bin/behave --dry-run             # parse check: no undefined steps
.venv/bin/python tools/coverage.py     # regenerate COVERAGE.md
```

UI scenarios (step 60, Playwright; the UI and Keycloak must be running):

```bash
.venv/bin/python -m playwright install chromium
UI_STEPS=1 .venv/bin/behave --tags=@ui -f json -o reports/ui.json -f progress
UI_STEPS=1 .venv/bin/behave --tags=@a11y     # axe WCAG 2.1 AA, results in reports/axe/
```

Failed UI scenarios leave a screenshot in `reports/ui-screenshots/`.

JUnit output for a report: `.venv/bin/behave --junit --junit-directory reports`.

## Environment variables

| Variable | Default (build profile) | Meaning |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base; paths in the features are relative to it |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | Keycloak token endpoint (password grant) |
| `TOKEN_CLIENT` | `vocabulary-tests` | public client used for the password grant |
| `TOKEN_CLIENT_SECRET` | none | only if the test client is confidential |
| `TOKEN_SCOPE` | `openid` | no scope beyond openid is requested (D40) |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | users of E1 holding GA_VOCABULARY_ADMIN, GA_VOCABULARY_VIEWER, no role |
| `USER_PASSWORD` | `Password1!` | password of the three users; `ADMIN_PASSWORD`, `VIEWER_PASSWORD`, `NOROLE_PASSWORD` override per user |
| `VOCABULARY_CONTENT_BASE_URL` | derived | contentBase of E2; when unset, taken from the first `contentHref` the API returns |
| `STORE_DOWN_API_URL` | none | API base of a second backend started with its store URL on a port where nothing listens (D44); enables `@store-down` |
| `UI_STEPS` | none | set to 1 to run the `@ui` scenarios in the browser (steps added in step 60) |
| `UI_URL` | `http://localhost:4433` | the UI under test |
| `UI_API_PREFIX` | `/v1` | path under which the UI calls the API; used by the browser stubs of D24 |
| `UI_TIMEOUT` | `10` | seconds a UI check waits |
| `UI_HEADED` | none | 1 shows the browser |
| `HTTP_TIMEOUT` | `30` | seconds per request |

`createdBy` is checked against the token's `email` claim, else `preferred_username` (D27),
so the suite does not hard-code e-mail addresses.

## Layout

- `features/*.feature`: one feature per area (access control, create, validation,
  deprecated references, versions, status, list, version history and content, searches,
  external vocabularies, errors and health, and five `ui_*` features).
- `features/steps/vocabulary_steps.py`: the shared step library for the API.
- `features/steps/ui_steps.py`: the UI steps in Playwright (step 60), by role, label and text.
- `features/ui_accessibility.feature`: one axe check per main page (`@a11y`).
- `features/steps/_types.py`: the custom parse type `Q` (a quoted value without quotes).
- `features/environment.py`: per-scenario state and the skip rules.
- `vmtest/client.py`: tokens, HTTP, multipart, Turtle builders, placeholder expansion.
- `fixtures/turtle/`: Turtle files for the validation rules (placeholders expanded at run time).
- `tools/coverage.py`: writes `COVERAGE.md`.

## Placeholders in feature text

`{Alpha}` becomes `Alpha` plus the run id, so names are unique per scenario. Others:
`{run}`, `{upper:Alpha}`, `{lower:Alpha}`, `{content:Alpha}` and `{content:Alpha:2}`
(content URLs, D12), `{ns:Ext}`, `{chars:N}`, `{blank}`, `{today}`, `{saved:key}`,
`{creator:ROLE}`. Form tables take `vocabularyFile` values `default`, `fixture:<file>`,
`size:<bytes>`, `empty`, `text:<turtle>` and `version:<n>` (the stored bytes of version n).

## Tags

- `@AC_<story>_<ACn>`: the acceptance criterion a scenario checks.
- `@D<n>`, `@E<n>`: the decisions it depends on.
- Rule ids from `input/rules.json` as they are, for example `@OWASP-02`, `@API-46`, `@BLG-err-problem`, `@OWA-API3`.
- `@negative`: error and abuse cases.
- `@ui`: UI scenarios (browser; skipped unless `UI_STEPS` is set).
- `@a11y`: axe WCAG 2.1 AA check of a main page.
- `@manual`: not automated (always skipped; reason in `COVERAGE.md`). None at present.
- `@wip`: cannot run against the API from outside (always skipped).
- `@store-down`: needs `STORE_DOWN_API_URL` (D44).

## Notes for whoever runs it

- Scenarios that sort by date wait about one second between creations, because times
  are kept to the second (D58).
- The 5 MB boundary scenarios upload 5 242 880 and 5 242 881 bytes (D15).
- With behave 1.3.3 a dry run of a feature whose first scenario is an outline under a
  Background reports the feature as failed although no step fails; every feature here
  starts with a plain scenario to avoid that.
