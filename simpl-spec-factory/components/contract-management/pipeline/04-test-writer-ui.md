# Step 4: UI browser steps (Contract Management)

Given to a fresh general-purpose agent. Same method as the earlier candidates ([`../../catalogue/pipeline/04-test-writer-ui.md`](../../catalogue/pipeline/04-test-writer-ui.md)), adapted to the Contract Management digest. `{CM}` is this `contract-management` folder, `{UPSTREAM}` the local upstream stack (`~/src/simpl-local/simpl-contract`).

---

You are the **test writer** of a spec-driven software factory experiment. Your job: make the 38 UI scenarios in `{CM}/tests/features/ui/*.feature` runnable with browser steps (Python, behave 1.2.6 + Playwright sync API, headless Chromium), then run them against two implementations of Simpl-Open **Contract Management** and report.

## Black-box rule (most important)

The tests must check what the **specs and the digest** say, not what one build happens to do.
- You MAY read: `{CM}/specs/`, `{CM}/spec-digest.md`, `{CM}/findings.md`, and everything under `{CM}/tests/`. You may read `{UPSTREAM}/README.md` and `{UPSTREAM}/docs/` to operate the upstream stack.
- You MUST NOT read: `~/src/spec-factory-build-cm/`, `{CM}/generated/`, any other `~/src/spec-factory-build*`, `~/src/gitlab/`, or the source under `{UPSTREAM}/repos/` and `{UPSTREAM}/ui-overlay/`. Do not grep them either.
- You MAY look at the rendered pages in the browser (DOM, accessibility tree, screenshots) and talk HTTP to the APIs: that is what a user sees.
- **Locators, in this order:** (1) accessible roles, labels and visible text taken from the story wording; where a story quotes a label, match it case-insensitive and whitespace tolerant. (2) Where no story fixes it, the routes, texts and `data-testid` values that the digest defines (D71 to D75): they are part of the shared decision list, not of one build. Tag such scenarios or steps with the decision, so a failure reads as "different decision". (3) Where neither fixes it, a tolerant pattern with a short comment naming the story. No CSS classes or ids.
- Never weaken an assertion to make one implementation pass. A deviation from a story or decision is a finding, not a test bug. Fix only genuine test-writer bugs. If you change any .feature text, list each change and why.

## Setup

- Work in `{CM}/tests`. Existing venv `.venv`. Add `playwright` to `requirements.txt`, install it and `playwright install chromium`.
- Read `features/environment.py`, `cmtest/*.py` and `features/steps/*.py` (including the existing `ui_steps.py`) first. Reuse the API and stub Given steps for setup (a negotiation driven through the stub to the state a scenario needs); do not duplicate them. Browser steps go in `features/steps/ui_steps.py`, in the style of the API steps. Browser lifecycle in environment.py, only for scenarios tagged @ui: the 345 API scenarios must stay as they are.
- New configuration, documented in the environment.py docstring: the UI base URL per party (defaults `http://localhost:19103` Provider, `http://localhost:19203` Consumer, D76), `UI_AUTH` = `oidc` (sign in on the identity provider's login page with the users of D67) or `none` (the UI has no sign-in). Generic login-page locators.
- "The API fails" steps: fault injection with Playwright `page.route`, returning a Problem Details error for the matching request. Works against any implementation.
- Remove `@wip` from runnable scenarios; keep `@ui`.

## Targets

1. Factory build (running now, compose project `cm-factory`; do not stop, restart or modify it): the default URLs of D76, `UI_AUTH=oidc`.
2. Local upstream (June 2026 version of the earlier repositories, one party in Provider mode, no Keycloak, no UI backend, no stub; UI at `http://localhost:4323`, backend at `http://localhost:8086`): start it with `{UPSTREAM}/start.sh` as its README says, run, and stop it with `{UPSTREAM}/stop.sh` at the end. Its README says the upstream UI is a shell that renders a read path only. Do not change the upstream repositories (no git pull, reset or checkout); if `start.sh` would update them or the stack does not start, say so and report the upstream target as not run. Run with `UI_AUTH=none AUTH_MODE=none` and the upstream URLs. Expect most scenarios to fail or be skipped there: classify, do not adapt the tests to upstream.

Run `behave features/ui` against both and save `reports/build1-ui-progress.txt`, `reports/build1-ui.json`, `reports/upstream-ui-progress.txt`, `reports/upstream-ui.json`. At the end, rerun `behave --tags=~@ui` against the build once to confirm 339 passed and 6 failed (the known deviations listed in findings.md, step 3). Keep iterations bounded: if a step stays ambiguous after a few attempts, leave it failing with a clear message.

Update `tests/README.md` (UI running instructions, tags). Do not touch findings.md. Do not commit. Only the upstream contract stack may be started and stopped; ports 4323, 5433, 8086, 9002 and 9095 belong to it. Writing rule: no em-dashes and no en-dashes anywhere.

## Report back (under 400 words)

- pass counts per target (UI), and the API rerun count;
- per failing scenario: target, scenario, what the browser showed against what the story or decision says, and a classification: implementation deviation / different decision / environment / spec gap;
- test bugs you fixed, and any .feature text changes;
- story wordings too vague for a strict locator (spec findings).
