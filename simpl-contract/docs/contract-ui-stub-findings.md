# Assessment finding — `contract-ui` is a non-functional stub

**Date:** 2026-06-26
**Assessor:** Barry Nauta (DG Connect architecture)
**Component:** Contract-Billing → `contract-ui`
**Source:** `code.europa.eu/simpl/simpl-open/development/contract-billing/contract-ui`,
branch **`develop`**, commit `c39df8238ec48f154f61bb89f74570f5bb9fa40a`
(branch `main` is an empty GitLab placeholder).
**Context:** evidence for the recurring Contract-Billing architecture/quality
concerns tracked under **SPGRLOG-2191** (the SPGRLOG-1630 regression) and the
DoD "documentation/components per sprint" line.

## Summary

The Contract-Billing front-end exists as a repository with ~2,188 LOC across 39
TS/TSX files, but contains **no functioning contract UI**. It is generic
scaffolding copied from another team's front-end, with placeholder content and
**no integration to the `contract` backend**. Verified directly by running it in
the local stack (auth disabled): the app renders a header with a fake user name
and a single page of hardcoded placeholder text.

## Evidence (file:line)

1. **Copied from the Monitoring front-end, not purpose-built.**
   - `package.json` → `"name": "monitoring-reporting-fe"` (never renamed).
   - `src/shared/api/httpClient.ts:9` → sends the **Kibana** header
     `'kbn-xsrf': 'true'` — residue from the Monitoring/Elastic UI.
   - `src/auth/authentication.ts:37` → hardcodes a redirect to
     `https://catalogue-ui.consumer01.uat.simpl-europe.eu/` — residue from the
     Catalogue UI.

2. **No backend integration.**
   - `src/shared/api/httpClient.ts` defines an HTTP client that is **never
     imported or called** anywhere in the codebase.
   - There is **no contract API base URL** in the source or `.env`; the only
     configured backend is Keycloak.
   - `src/entities/contract/types.ts` models a contract as **`{ id: string }`**
     only — the domain is unmodelled.

3. **The single page is static placeholder content.**
   `src/pages/ContractViewPage/ContractViewPage.tsx` (20 lines) renders a title
   plus literal placeholder text, with the loading/error flags **hardcoded** and
   no data fetch:
   ```tsx
   <SIMPLTitle title="Contract Review" />
   <LoadingWrapper loadingTitle="Loading Contract data..." isLoading={false} isError={false}>
     Here contract informations
   </LoadingWrapper>
   ```
   - `ContractViewPage.tsx:15` → placeholder string **"Here contract
     informations"** (note the ungrammatical "informations").
   - No `useEffect`, no API call — the page never requests contract data.

4. **Fake hardcoded user.**
   - `src/app/layout/header/SIMPLMenuHeader.tsx:25` → the header prints a fixed
     **"Alexander WILLIAMS"**; it does not read the authenticated user.

5. **No documentation.**
   - `README.md` is still the **untouched default GitLab template** (even on
     `develop`).

6. **Cannot run without Keycloak (upstream).**
   - Every protected route redirects to a **remote** dev-sandbox Keycloak; there
     is no configuration to disable it, and the redirect URI is hardcoded to
     `http://localhost:3001/` (`src/auth/hooks/useAuth.ts`). (This local stack
     adds the missing `VITE_PUBLIC_AUTH_MODE` switch to run it offline — see
     `ui-overlay/`.)

## Significance

This is the same **"form without substance"** pattern previously documented for
the Notification Service changelogs and the D1.3.2 Architecture Specification: a
deliverable that exists and "passes" structurally (repo present, builds, renders)
while delivering no usable capability. For Contract-Billing specifically it means
the component's front-end is **scaffolding, not an implementation** — relevant to
acceptance (no demonstrable contract UI) and to the SC-3 handover.

## Reproduction

```
cd simpl-local/simpl-contract && ./start.sh
# UI served at http://localhost:4323 (auth disabled by default)
# Observe: header "Alexander WILLIAMS"; page "Contract Review" + "Here contract informations"; nothing else.
```
