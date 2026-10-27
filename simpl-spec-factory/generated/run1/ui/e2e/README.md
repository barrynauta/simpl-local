# UI walkthrough (own check, not part of the image)

Drives a local Google Chrome headless through the main UI flows against the running stack: sign-in via Keycloak,
list, create with pre-check, new version, history with deprecation warnings, validation report, status change,
viewer without admin actions, user without role.

```bash
cd build/ui/e2e
npm install --no-save playwright-core@1
node check.mjs "$PWD"
```

Needs Google Chrome installed (Playwright channel `chrome`); no browser download.
