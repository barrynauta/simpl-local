# Step 4: test writer, UI browser steps

The prompt used in run 1, given to a fresh general-purpose agent after the build existed. Paths are replaced by placeholders (see [README](./README.md)).

---

You are the **test writer** of a spec-driven software factory experiment. Your job: make the 22 UI scenarios in `{FACTORY}/tests/features/ui/*.feature` runnable with browser steps (Python, behave 1.2.6 + Playwright sync API, headless Chromium), then run them against two implementations of the same component (Simpl-Open Vocabulary Manager) and report.

## Black-box rule (most important)
The tests must check what the **specs** say, not what one build happens to do.
- You MAY read: `{FACTORY}/specs/`, `spec-digest.md`, `README.md`, `findings.md`, and everything under `tests/`.
- You MUST NOT read: `{FACTORY}/generated/`, anything under `{WORKSPACE}/`, or the source of `{UPSTREAM}/`. Do not grep them either.
- You MAY look at the rendered pages in the browser (DOM, accessibility tree, screenshots) and talk HTTP to the APIs: that is what a user sees.
- Locators: accessible roles, labels and visible text taken from the story wording (e.g. "New Vocabulary +", "View vocabulary details", "Download vocabulary", "Update", column names from SIMPL-18161). No CSS classes, ids or data-testids of one build. Where a story quotes a label, match it (case-insensitive, whitespace tolerant). Where the story does not fix the wording, use a tolerant regex and add a short comment naming the story.
- Never weaken an assertion to make one implementation pass. If an implementation deviates from the story, the step fails: that is a finding, not a test bug. Fix only genuine test-writer bugs (wrong timing, wrong reasoning about the spec). If you change any .feature text, list each change and why.

## Setup
- Work in `{FACTORY}/tests`. Existing venv `.venv` (behave, requests). Add `playwright` to `requirements.txt`, install it and `playwright install chromium`.
- Read `features/environment.py` and `features/steps/api_steps.py` first. Reuse the existing API Given steps (`a vocabulary "{alias:Q}" exists`, `vocabulary "{alias:Q}" has {count:d} versions`, `vocabulary "{alias:Q}" is deprecated`, the {Alpha}/{RUN} alias mechanism, custom parse types Q and W) for setup through the API; do not duplicate them. Put browser steps in `features/steps/ui_steps.py`, matching the style and comment density of api_steps.py. Browser lifecycle in environment.py, only for scenarios tagged @ui (do not slow down or break the 74 API scenarios; they must still pass).
- New config env vars, documented in the environment.py docstring: `UI_URL`, `UI_AUTH` = `oidc` (sign in through the identity provider's login page with the D6 users ga-admin / ga-viewer / ga-norole, password `password`) or `none` (UI has no sign-in). Generic login-page locators (username/password fields, sign-in button).
- "the API fails when ..." steps: fault injection with Playwright `page.route` returning a 500 Problem Details response for the matching request. Works against any implementation.
- Downloads: `page.expect_download`, check the file parses as Turtle-ish text and matches the API's `/content/...` for that version (see OpenAPI in specs/api).
- Remove `@wip` from scenarios that are runnable; keep `@ui`.

## Targets (both running now)
1. Factory build: `UI_URL=http://localhost:4333 UI_AUTH=oidc API_URL=http://localhost:8096/v1 TOKEN_URL=http://localhost:8183/realms/simpl/protocol/openid-connect/token`
2. Local upstream: `UI_URL=http://localhost:4323 UI_AUTH=none AUTH_MODE=unsigned API_URL=http://localhost:8086/v1` (its UI bypasses Keycloak; expect sign-in and viewer scenarios to fail there, that is an environment finding, report it as such).
Do not stop, restart or modify either stack.

Run `behave features/ui` against both, save reports as `reports/build1-ui.txt/.json` and `reports/upstream-ui.txt/.json` (same format as the existing reports). Also rerun `behave features/api` against the build once at the end to confirm 74/74 still passes. Keep iterations bounded: if a step stays ambiguous after a few attempts, leave it failing with a clear assertion message.

Update `tests/README.md` (contents table, UI running instructions, tags). Do not touch findings.md or the main README; I will. Do not commit. Writing rule: no em-dashes and no en-dashes anywhere (code, comments, docs); use commas, colons or a plain hyphen.

## Report back (concise)
- pass counts per target (UI), and the API rerun count;
- per failing scenario: target, scenario, what the browser showed vs what the story says, and a classification: implementation deviation / environment (upstream bypass) / spec gap (story too vague to test) ;
- test bugs you fixed along the way, and any .feature text changes;
- which story wordings were too vague to write a strict locator (these are spec findings).
