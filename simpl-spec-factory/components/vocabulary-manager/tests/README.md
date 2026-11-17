# Black-box test suite

Written by the **test writer** from `specs/` and [`spec-digest.md`](../spec-digest.md) only. The code writer does not see this folder before the tests run (see the main README, "Approach").

The suite talks HTTP and drives a browser only, so it runs unchanged against any implementation: the factory build and upstream.

## Contents

| Folder | What |
|---|---|
| `features/api/` | 10 feature files, 74 scenarios, runnable |
| `features/ui/` | 4 feature files, 22 scenarios, runnable (Playwright, headless Chromium) |
| `features/steps/api_steps.py` | the HTTP step library; the UI scenarios reuse its Given steps for setup |
| `features/steps/ui_steps.py` | the browser step library: roles, labels and visible text only, no ids or CSS classes of one build |
| `features/environment.py` | target configuration (all environment variables are documented in its docstring), tokens, browser lifecycle |
| `reports/` | progress and JSON reports per target: `build1`, `upstream` (API) and `build1-ui`, `upstream-ui` (UI) |
| `fixtures/` | small Turtle files: valid, syntax errors, structure errors, references |

## Tags

- `@SIMPL-nnnnn`: the user story the scenario checks. All 32 in-scope stories are covered.
- `@Dnn`: the spec-digest decision the scenario depends on. A failure on such a scenario can mean "different decision", not "wrong behaviour".
- `@openapi-only`: behaviour that only the OpenAPI describes, no story.
- `@rbac`: role checks; upstream enforces these in a gateway, not in the service.
- `@ui`: browser scenarios. Only these start Chromium; each gets a fresh browser context.
- `@wip`: not runnable yet (none at the moment).

## Running

### API

```
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt

# against the factory build (docker compose up), tokens from its OIDC issuer (spec-digest D6)
API_URL=http://localhost:8086/v1 TOKEN_URL=<issuer token endpoint> .venv/bin/behave features/api

# against the local upstream stack (../simpl-vocabulary-manager), which only decodes tokens
AUTH_MODE=unsigned API_URL=http://localhost:8086/v1 .venv/bin/behave features/api
```

### UI

```
.venv/bin/pip install -r requirements.txt && .venv/bin/playwright install chromium

# factory build: the UI signs in through its identity provider (spec-digest D6 users)
UI_URL=http://localhost:4333 UI_AUTH=oidc \
API_URL=http://localhost:8096/v1 TOKEN_URL=http://localhost:8183/realms/simpl/protocol/openid-connect/token \
.venv/bin/behave features/ui

# local upstream stack: its UI has no sign-in, so the viewer and sign-in scenarios cannot pass there
UI_URL=http://localhost:4323 UI_AUTH=none AUTH_MODE=unsigned API_URL=http://localhost:8086/v1 .venv/bin/behave features/ui
```

- `UI_AUTH=oidc` signs in on the identity provider's login page as `ga-admin`, `ga-viewer` or `ga-norole` (password `password`). `UI_AUTH=none` only supports `admin`.
- Setup (vocabularies, versions, deprecation) goes through the API, so `API_URL` and the token settings must point at the same stack as `UI_URL`.
- "The API fails when ..." steps inject a 500 Problem Details response with Playwright request routing, so they work against any implementation without touching the stack.
- Labels a story quotes ("New Vocabulary +", "View vocabulary details", "Download vocabulary", "Update") must appear in the accessible name, any case. Where a story fixes no wording, the steps use a tolerant pattern with a comment naming the story.
- Messages count only when they appear after the user's action (a static hint like "Must be unique." does not count as an error), and uploaded file names are ignored.
- Debugging: `UI_HEADED=1` shows the browser, `UI_SCREENSHOTS=<dir>` saves a screenshot per failed scenario, `UI_DEBUG=1` prints the text that satisfied each message check.
- Reports: `-f progress -o reports/<target>-ui.txt -f json -o reports/<target>-ui.json`.

Every scenario creates its own vocabularies (name + a random run id), because internal vocabularies cannot be deleted. External vocabularies are deleted after each scenario.
