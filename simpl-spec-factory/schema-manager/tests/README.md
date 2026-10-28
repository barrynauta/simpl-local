# Schema Manager black-box test suite

Written by the test writer of the spec-driven factory, from `../specs/`, `../spec-digest.md` and `../findings.md` only. No implementation source was read. The API suite talks HTTP only (plus its own webhook receiver, the Vocabulary Manager stub seeding and the Mailpit API of D60); the UI suite drives a headless Chromium with Playwright and looks only at what a user sees (DOM, accessible roles, the requests the browser sends). Both run unchanged against any implementation.

## Contents

| Path | What |
|---|---|
| `features/api/*.feature` | API scenarios for every in-scope API story, the API behaviour of the UI stories and the OpenAPI-only operations |
| `features/ui/*.feature` | 36 UI scenarios (`@ui`), with the `data-testid` values and texts of D45 to D57 |
| `features/steps/api_steps.py` | The shared step library, with the custom parse types `Str` (double-quoted value), `SQ` (single-quoted value), `Int`, `SchemaType`, `User` |
| `features/steps/ui_steps.py` | Browser steps (Playwright sync API). Locator order: story labels, then the D46 test ids and D45 to D57 texts, then a tolerant pattern naming the story; no CSS classes or ids |
| `features/smlib.py` | Configuration, tokens, webhook receiver, Mailpit client, stub seeding |
| `features/environment.py` | Hooks and the browser lifecycle (one browser per run, one context per `@ui` scenario); the docstring documents every environment variable |
| `fixtures/` | Turtle fixtures: valid RESOURCE and SEARCH schemas with the D31 default properties, syntax errors, each structure, default property and semantic error, references to stub vocabularies, non-Turtle files |
| `reports/` | Progress, plain and JSON reports per target |

### Aliases in the feature files

`$A`, `$B`, `$S` ... are schema names, `$RT...` resource types, `$VOC...` vocabulary names. Each becomes a unique value per run and per scenario (D61): `T<run><seq><alias>`, `Rt<run><seq>...`, `V<run><seq>...`. `$NS` is `VOCABULARY_NAMESPACE_BASE`, `$DP` the default property namespace of D31. Fixtures use the same tokens.

## Tags

| Tag | Meaning |
|---|---|
| `@SIMPL-nnnnn` | Story the scenario checks |
| `@Dnn` | Digest decision the scenario depends on |
| `@openapi-only` | Behaviour defined by the OpenAPI only (406, 415, 409 on duplicate webhooks, warnings, out-of-scope paths) |
| `@rbac` | Role checks (D4, D5, D7) |
| `@mail` | Needs mail capture (`MAIL_API_URL`) |
| `@webhook` | Needs the webhook receiver to be reachable from the service |
| `@search-serial` | Touches the single global SEARCH slot. Run these one after another, never in parallel (D61) |
| `@vocab` | Needs the Vocabulary Manager stub (`VOCABULARY_MANAGER_URL`) |
| `@needs-idp` | Needs a validating identity provider (`AUTH_MODE=oidc`) |
| `@vm-down` | Only in a dedicated run with the Vocabulary Manager unreachable (D36, `SM_VM_DOWN_RUN=true`) |
| `@ui` | UI scenarios (browser). Run them with `behave features/ui`; leave them out with `behave features/api` or `--tags=~@ui` |
| `@wip` | Excluded by default (`behave.ini`); no scenario carries it at the moment |

Scenarios whose requirement is not met are skipped automatically by `environment.py`.

## Environment variables

See the docstring of `features/environment.py`. The main ones:

| Variable | Default (factory build) | Upstream local stack |
|---|---|---|
| `SM_BASE_URL` | `http://localhost:8098/v1` | `http://localhost:8085` |
| `AUTH_MODE` | `oidc` | `none` |
| `KEYCLOAK_URL` | `http://localhost:8185` | (unused) |
| `CLIENT_ID` | `schema-tests` | (unused) |
| `VOCABULARY_MANAGER_URL` | `http://localhost:8099` | empty (no stub) |
| `VOCABULARY_NAMESPACE_BASE` | `https://vocabulary.simpl.test/content/` | same |
| `MAIL_API_URL` | `http://localhost:8027` | empty (no capture) |
| `PROVIDER_NOTIFICATION_EMAILS` | empty (recipients not checked) | empty |
| `WEBHOOK_RECEIVER_URL` | `http://host.docker.internal:8299` | same |
| `WEBHOOK_LISTEN_PORT` | `8299` | same |
| `SM_VM_DOWN_RUN` | `false` | `false` |

With `AUTH_MODE=none` the suite sends a self-made HS256 token with `preferred_username`, `roles` and `realm_access.roles`, so a stack that decodes roles without validating the token still sees the right roles.

## Setup

```bash
cd tests
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/playwright install chromium     # for the UI scenarios
```

## Run against the factory build

Start the build first (`docker compose up -d --wait` in the build folder). Then:

```bash
.venv/bin/behave \
  -f progress -o reports/factory-progress.txt \
  -f json.pretty -o reports/factory.json \
  -f plain -o reports/factory-plain.txt
```

All defaults point at the reference environment. Set `PROVIDER_NOTIFICATION_EMAILS` to the value configured in the build to also check the recipients.

Dedicated D36 run: make the Vocabulary Manager unreachable from the service (for example `docker compose stop` on the stub), then

```bash
SM_VM_DOWN_RUN=true .venv/bin/behave --tags=@vm-down
```

## Run against upstream (baseline)

```bash
../../../simpl-schema-manager/start.sh        # path from this folder: ~/src/simpl-local/simpl-schema-manager
SM_BASE_URL=http://localhost:8085 AUTH_MODE=none VOCABULARY_MANAGER_URL= MAIL_API_URL= \
  .venv/bin/behave \
  -f progress -o reports/upstream-progress.txt \
  -f json.pretty -o reports/upstream.json \
  -f plain -o reports/upstream-plain.txt
~/src/simpl-local/simpl-schema-manager/stop.sh
```

Upstream has no Keycloak, no stub and no mail capture, and it is a May 2026 version, older than the enhancement stories. Expect many failures; each one is a test-writer bug (fixed), an environment difference (a missing piece) or a real difference on a decision or a newer story (kept failing).

## UI scenarios

The browser steps need the UI and the API of the target, plus the identity provider when the UI signs in.

| Variable | Factory build | Upstream local stack |
|---|---|---|
| `SM_UI_URL` | `http://localhost:4334` (default) | `http://localhost:4322` |
| `UI_AUTH` | `oidc` (default): sign in on the login page as `ga-admin`, `ga-viewer` or `ga-norole` | `none` (the UI has no sign-in) |
| `SM_UI_API_URL` | not needed: the default recognises `SM_BASE_URL` and the same path on the UI origin (`http://localhost:4334/v1`) | `http://localhost:4322/v1` (the UI calls the API through its nginx proxy) |
| `UI_HEADLESS` | `true` (default); `false` shows the browser | same |
| `UI_SCREENSHOT_DIR` | optional: a screenshot of every failed UI scenario | same |

Factory build:

```bash
SM_UI_URL=http://localhost:4334 UI_AUTH=oidc SM_BASE_URL=http://localhost:8098/v1 \
  .venv/bin/behave features/ui \
  -f progress -o reports/build1-ui-progress.txt \
  -f json.pretty -o reports/build1-ui.json
```

Upstream (start it first, stop it afterwards, as above):

```bash
SM_UI_URL=http://localhost:4322 UI_AUTH=none AUTH_MODE=none SM_BASE_URL=http://localhost:8085 \
  SM_UI_API_URL=http://localhost:4322/v1 VOCABULARY_MANAGER_URL= MAIL_API_URL= \
  .venv/bin/behave features/ui \
  -f progress -o reports/upstream-ui-progress.txt \
  -f json.pretty -o reports/upstream-ui.json
```

How the steps work:

- Setup goes through the API Given steps and the aliases (`$A`, `$RT`), as in the API features.
- "the UI sent the request ..." compares method, path and query parameters (order free) of the requests the browser sent to the API. `page` and `pageSize` are compared only when the scenario names them; `schemaType=RESOURCE` counts as absent when the scenario does not name `schemaType` (the default selector, D11, D27).
- "the UI sent no request to the API" counts from the moment the page was loaded (or a list row was brought on screen).
- The schemas list is paged by 20 and ordered by name (D26, D53), and the store keeps all earlier test data. When a step names a row or a row action of a schema, the steps page through the list with `pagination-next` and `pagination-prev` until the row is shown, as a user would.
- Downloads are caught with Playwright's download event; the file name is checked and the bytes are compared with what the API returns for that version.
- `Given the API answers "<METHOD> <path>" with status <code>` injects a Problem Details error for that request with `page.route`, for load-failure scenarios. No scenario uses it yet.
- `SM_RUN_ID` fixes the run id for debugging (never reuse one: names are never reusable, D12).

## Dry run

```bash
.venv/bin/behave --dry-run -f progress
```

Every step is defined; the dry run reports 0 undefined steps.
