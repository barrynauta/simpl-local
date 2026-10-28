# Step 4: UI browser steps (Schema Manager)

Given to a fresh general-purpose agent. Same method as the first candidate ([`../../pipeline/04-test-writer-ui.md`](../../pipeline/04-test-writer-ui.md)), adapted. `{SM}` is this `schema-manager` folder, `{UPSTREAM}` the local upstream stack (`~/src/simpl-local/simpl-schema-manager`).

---

You are the **test writer** of a spec-driven software factory experiment. Your job: make the 36 UI scenarios in `{SM}/tests/features/ui/*.feature` runnable with browser steps (Python, behave 1.2.6 + Playwright sync API, headless Chromium), then run them against two implementations of the Simpl-Open **Schema Manager** and report.

## Black-box rule (most important)

The tests must check what the **specs and the digest** say, not what one build happens to do.
- You MAY read: `{SM}/specs/`, `{SM}/spec-digest.md`, `{SM}/findings.md`, and everything under `{SM}/tests/`. You may read `{UPSTREAM}/README.md` and `{UPSTREAM}/docs/` to operate the upstream stack.
- You MUST NOT read: `{SM}/generated/`, `~/src/spec-factory-build-sm/`, `~/src/gitlab/`, or the source under `{UPSTREAM}/repos/`. Do not grep them either.
- You MAY look at the rendered pages in the browser (DOM, accessibility tree, screenshots) and talk HTTP to the APIs: that is what a user sees.
- **Locators, in this order:** (1) accessible roles, labels and visible text taken from the story wording; where a story quotes a label, match it case-insensitive and whitespace tolerant. (2) Where no story fixes it, the texts and `data-testid` values that the digest defines (D45 to D57): they are part of the shared decision list, not of one build. Tag such scenarios or steps with the decision, so a failure reads as "different decision". (3) Where neither fixes it, a tolerant pattern with a short comment naming the story. No CSS classes or ids.
- Never weaken an assertion to make one implementation pass. A deviation from a story or decision is a finding, not a test bug. Fix only genuine test-writer bugs. If you change any .feature text, list each change and why.

## Setup

- Work in `{SM}/tests`. Existing venv `.venv`. Add `playwright` to `requirements.txt`, install it and `playwright install chromium`.
- Read `features/environment.py`, `features/smlib.py`, `features/steps/api_steps.py` and `features/steps/ui_steps.py` first. Reuse the API Given steps and the alias mechanism for setup; do not duplicate them. Browser steps go in `features/steps/ui_steps.py`, in the style of the API steps. Browser lifecycle in environment.py, only for scenarios tagged @ui: the 291 API scenarios must stay as they are and keep passing.
- New configuration, documented in the environment.py docstring: `SM_UI_URL` (D60 name), `UI_AUTH` = `oidc` (sign in on the identity provider's login page as `ga-admin`, `ga-viewer`, `ga-norole`, password `password`) or `none` (the UI has no sign-in). Generic login-page locators.
- "The API fails" steps: fault injection with Playwright `page.route`, returning a Problem Details error for the matching request. Works against any implementation.
- Downloads: `page.expect_download`, compare the bytes with the API's content of that version.
- Remove `@wip` from runnable scenarios; keep `@ui`.

## Targets

1. Factory build (running now, do not stop, restart or modify it): `SM_UI_URL=http://localhost:4334 UI_AUTH=oidc SM_BASE_URL=http://localhost:8098/v1 KEYCLOAK_URL=http://localhost:8185 VOCABULARY_MANAGER_URL=http://localhost:8099 MAIL_API_URL=http://localhost:8027`.
2. Local upstream (May 2026 version, Keycloak bypassed, UI at `http://localhost:4322`, API at `http://localhost:8085`): start it with `{UPSTREAM}/start.sh`, run, and stop it with `{UPSTREAM}/stop.sh` at the end. Run with `UI_AUTH=none AUTH_MODE=none`. Expect sign-in and viewer scenarios to fail there (environment), and expect setup through the API to fail where upstream needs the resource type declared in the schema file (see findings, step 2): classify, do not adapt the fixtures to upstream.

Run `behave features/ui` against both and save `reports/build1-ui-progress.txt`, `reports/build1-ui.json`, `reports/upstream-ui-progress.txt`, `reports/upstream-ui.json`. At the end, rerun `behave features/api` against the build once (with `PROVIDER_NOTIFICATION_EMAILS=providers@simpl.test`) to confirm 289 passed, 2 skipped. Keep iterations bounded: if a step stays ambiguous after a few attempts, leave it failing with a clear message.

Update `tests/README.md` (UI running instructions, tags). Do not touch findings.md. Do not commit. Writing rule: no em-dashes and no en-dashes anywhere.

## Report back (under 400 words)

- pass counts per target (UI), and the API rerun count;
- per failing scenario: target, scenario, what the browser showed against what the story or decision says, and a classification: implementation deviation / different decision / environment / spec gap;
- test bugs you fixed, and any .feature text changes;
- story wordings too vague for a strict locator (spec findings).
