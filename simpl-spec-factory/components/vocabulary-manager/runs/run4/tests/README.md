# Vocabulary Manager: black-box API test suite

Written from the specs and decisions of run 4 only (step 40). HTTP only: the suite never
reads the code and runs unchanged against any build of the component.

## Run

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/behave                       # API scenarios (default tags, see below)
.venv/bin/behave --tags=@D46           # one decision
.venv/bin/behave --tags=@AC_SIMPL-18173_AC1
.venv/bin/behave --dry-run --tags="@ui or not @ui"   # parse check, every scenario
```

`behave.ini` excludes `@wip`, `@ui` and `@store_down` by default. Pass `--tags` to override.

The store is never reset. Every scenario makes its own run id; vocabulary names are
`V<runid><Alias>`, external names `E<runid><Alias>`, so a list filtered on `name=V<runid>` holds
only the scenario's own data.

## UI run (step 60)

```bash
.venv/bin/pip install -r requirements.txt
.venv/bin/python -m playwright install chromium
.venv/bin/behave --tags=@ui -f json -o reports/ui.json -f plain -o reports/ui-plain.txt
```

Extra variables: `UI_URL` (default `http://localhost:4433`), `UI_HEADLESS` (`0` shows the browser),
`UI_TIMEOUT_MS` (default 10000). Each scenario gets a fresh browser context and signs in on the
Keycloak page. Stubbed API answers (`the API is stubbed to answer ...`) are served by Playwright
in the browser only; the running API is not touched.

## Environment variables

| Variable | Default | Meaning |
|---|---|---|
| `API_URL` | `http://localhost:8196/v1` | API base URL (the `/v1` base of E2) |
| `TOKEN_URL` | `http://localhost:8193/realms/simpl/protocol/openid-connect/token` | Keycloak token endpoint |
| `TOKEN_CLIENT` | `vocabulary-tests` | public client with the password grant |
| `TOKEN_CLIENT_SECRET` | empty | only if the client is confidential |
| `TEST_PASSWORD` | `Password1!` | password of the three test users |
| `ADMIN_USER`, `VIEWER_USER`, `NOROLE_USER` | `ga-admin`, `ga-viewer`, `ga-norole` | users of E1 |
| `ADMIN_EMAIL`, `VIEWER_EMAIL`, `NOROLE_EMAIL` | `<user>@example.com` | expected `createdBy` (D27) |
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | contentBase of E2: used for `contentHref` checks and internal references (D12) |
| `HTTP_TIMEOUT` | `30` | seconds per request |

## Tags

| Tag | Meaning |
|---|---|
| `@AC_<story>_<ACn>` | acceptance criterion checked, for example `@AC_SIMPL-18170_AC1` |
| `@D<n>`, `@E<n>` | decision the scenario depends on |
| `@API-..`, `@OWASP-..`, `@OWA-..`, `@BLG-..`, `@SEC-..`, `@ARCH-..`, `@MACH-..` | rule from `rules.json` that the scenario checks |
| `@negative` | error and abuse cases |
| `@ui` | UI scenario, run in a browser (Playwright, step 60). Run with `behave --tags=@ui` |
| `@a11y` | axe-core check (WCAG 2.1 A and AA) of one main page; violations go to `reports/a11y-<page>.json` |
| `@store_down` | needs a deployment whose store URL points at a port where nothing listens (D44). Run it alone: `behave --tags=@store_down` |

## Layout

- `features/*.feature`: scenarios. API features first, `ui_*.feature` for the UI.
- `features/steps/vm_steps.py`: the shared API step library.
- `features/steps/ui_steps.py`: browser steps of the `@ui` scenarios (step 60), by role, label and text only.
- `lib/uidriver.py`: browser start, sign-in through the Keycloak page, API stubs (`page.route` on `/v1/`), row and version lookups.
- `lib/vmclient.py`: configuration, tokens, date and JSON helpers. `lib/world.py`: scenario state and API actions.
- `fixtures/`: Turtle files. `${own}`, `${content:Alias}` and similar placeholders are filled per scenario.
- `tools/tag_report.py`: scenarios per tag, used for `COVERAGE.md`.

## Notes for whoever runs it

- Upload checks: the 5 MB limit is tested at 5 242 880 bytes (accepted) and one byte more (413).
- D43 is tested with real parallel requests (five version creations, two creations of one name, two identical PATCHes).
- `behave --dry-run` (behave 1.3.3) reports `access_control.feature` as "failed" although no step is undefined or failing. It is a status quirk of behave's dry-run for a feature with a Background, a Scenario Outline and a plain Scenario; a three-line toy feature shows the same.
