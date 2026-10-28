# Findings: Catalogue

Third candidate of the factory, after the Vocabulary Manager and the Schema Manager. Scope: the whole catalogue (catalogue-be, query mapper adapter, catalogue client backend, UI). Input: published or to-be-published material only. See [`specs/`](./specs/README.md).

## 2026-10-28: step 0, input set

- **The core catalogue has no stories.** Publishing, versioning and revoking a resource description, quick search and advanced search exist only as API operations and as business processes (BP05b, BP06). The 33 live stories are all from 2026 and cover new features: federated search across data spaces, the federated search schema, quality filters and the quality assessment record.
- **Many stories are not settled:** 10 are "FOR SC TO RESPOND", 6 "PBI Defined", 4 in review; 8 have no Given/When/Then criteria.
- **Three backends with overlapping paths** (`/selfDescriptions/advancedSearch` in catalogue-be and the adapter, schema paths in catalogue-be and the client backend).
- **Review notes inside the contract:** the active catalogue-be OpenAPI still carries 13 inline notes from an early design review, for example a field marked "NOT FURTHER SPECIFIED".

## 2026-10-28: step 1, spec digest

Written by a fresh agent from the public input only ([`spec-digest.md`](./spec-digest.md)).

- **83 decisions** (Vocabulary Manager 30, Schema Manager 63), plus one row per inline review note (N1 to N13).
- **Scope:** 29 of 33 stories (the four federated search schema stories belong to the Schema Manager); 37 of 45 operations (the other 8 answer 501); 3 operations defined by the digest because the stories need them and no OpenAPI has them (inbound search on the trustor side, quick and advanced, and a detail lookup through the consumer's Governance Authority).
- **Main gaps, by impact on buildability:**
  1. The contract between Governance Authorities is not published: the inbound search APIs, the mTLS through a gateway, the mapping of identity attributes. The digest uses OAuth2 client credentials and a static mapping instead.
  2. Signing has no defined profile or trust anchors. The digest picks Ed25519 over canonical JSON with `did:jwk` issuers and fixed test keys.
  3. The consolidated federated result has no defined shape, and the stories disagree on partial failure (502 or an empty group).
  4. The stories disagree on whether a federated detail returns a schema.
  5. The quality scoring service has no contract, not even its scale.
- **Test topology:** three data spaces (A trusted, B trustor, C not trusted by A), a slow and an inactive catalogue, in one compose project.

## 2026-10-28: step 2, test writer

Written by a fresh agent from the specs and digest only ([`tests/`](./tests/)).

- **485 scenarios:** 459 API in 12 features, 26 UI (Gherkin only, browser steps come in step 4). All 29 in-scope stories and 65 decisions are tagged.
- **Baseline on the local upstream stack** (May 2026 version of the earlier repositories, one data space, no identity provider, no stubs, no client backend; federation, client backend, quality, stub and UI scenarios skipped): 55 passed, 150 failed, 280 skipped. About 110 failures are decisions upstream does not share (no Problem JSON, no syntax, signature or SHACL checks so broken descriptions get 201, no versioning, out-of-scope operations answer 200, other paging and matching rules), 26 are missing pieces of the environment, about 20 are newer stories. Details: [`tests/reports/upstream-classification.txt`](./tests/reports/upstream-classification.txt).
- **Digest still too vague for a test:** the health path of the `/v1` services, the body of `GET /self-descriptions/{id}`, where the client backend takes the catalogue ids for advanced search, the group status on a timeout, the participant list payload.

## 2026-10-28: step 3, code writer (run 1)

A separate agent built everything from `specs/` and `spec-digest.md` only, in its own workspace (output: [`generated/run1/`](./generated/run1/)): three Spring Boot backends in one Maven reactor (about 5,100 lines of Java, against about 19,000 for upstream's catalogue backend alone), a Vue 3 UI, two Node stubs (Schema Manager and quality scoring), Keycloak with three realms, 11 containers. About 45 minutes, four fix iterations, no compile errors; the stack came up healthy on the first start, smoke test and `mvn verify` (37 tests) green.

- **26 further spec gaps** in its build log. Notable: the quality profile has no field for the threshold value; no problem type for a revoke or delete by a non-owner; 400 or 422 for a missing `dct:conformsTo`; whether `@id` values are searchable; the group order; paging fields for a search over several catalogues.
- **Black-box suite: 458 of 459 API scenarios pass.** The one failure is a real deviation: for a signature whose verification method is not the issuer's, the build checks the key registry first and answers 403 `providerMismatch`, where D14 puts the signature check first (422 `invalidSignature`).
- **Test-writer bugs found by the first run:** the result lookup did not look inside the `meta` object of list items, and the header check treated `Location` as a comma list. Fixed; no check was weakened. The upstream baseline was run before these fixes.
- **Isolation:** no file access outside its workspace, no web search for Simpl or XFSC code.
- **Contamination:** 6 of 64 class names are shared with upstream's 299 (the May clones plus the current canonical repositories), all generic or from the OpenAPI (`HealthController`, `SchemaController`, `SearchController`, `SearchService`, `SignatureVerifier`, `VerificationController`); different package (`eu.simpl.catalogue` against `eu.xfsc.fc`); `SignatureVerifier` is 79 lines against 15. Of the build's 257 longer string literals, 42 also occur in upstream code, and every one of them is in the specs or the digest (role names, problem titles, JSON-LD terms). No sign that the build reproduces upstream code.

### How to read 458 of 459

As with the earlier candidates, both writers used the same decision list, here 83 decisions. The result shows that a component with three backends and federation across data spaces is buildable from the published specs **plus a long written decision list**. It does not show that the published specs are: the core of the catalogue has no stories at all, the contract between Governance Authorities is not published, and the digest had to define three operations itself.

## 2026-10-28: step 4, UI browser steps

The 26 UI scenarios made runnable with Playwright by a fresh agent, which saw the rendered pages only, not the code. Locators: story wording first, then the selectors of digest section 7, then tolerant patterns.

- **Factory build: 26 of 26.** API rerun after the change: 458 passed, 1 failed (the known D14 order deviation).
- **Local upstream: 0 of 26.** This says little about the upstream UI:
  - about 18 fail on the selectors of digest section 7 (a different decision, now tagged `@D70`); upstream quick search exists but calls `/api/selfDescriptions`, not `/api/v1`;
  - newer stories: no catalogue selector (SIMPL-31102, SIMPL-31103), no "minimum score" or "minimum classification" control (SIMPL-29292, SIMPL-29293), no detail route the digest's error cases can use (D75);
  - environment: no data space B, no client backend, no quality stub (6 scenarios); the advanced tab needs an SD Tooling service that the local stack does not have.
- **Setup by publishing the digest's signed descriptions to upstream worked**: upstream accepts them (it checks no signatures).
- **Test-writer changes:** a tab must be opened before its page indicator is checked; the schema choice compares as a set, since neither SIMPL-35874 nor D74 fixes order or label form; the "empty quality filters" scenario now checks that no filter parameter is sent; waits for option lists.
- **Story wording too vague for a strict locator:** the stories quote almost no labels (only "Quick Search Tab", "Advanced Search", "the Search button", "minimum score", "minimum classification"); the digest gives no route for the detail page; "every property" (D75) does not say how JSON-LD paths are shown.

## 2026-10-28: step 5, swap experiments

Each joint crossed on its own, configuration only. Details and commands: [`experiments/swap/`](./experiments/swap/README.md).

- **Quick search, advanced search and the resource description itself cross every joint**: the build's adapter on upstream's catalogue backend, upstream's adapter on the build's catalogue backend, the build's client backend on upstream's adapter, the build UI on upstream's backends and the upstream UI on the build's.
- **Suite over the crossed combinations:** build adapter on upstream catalogue 39 of 63; upstream adapter on build catalogue 20 of 63 (on its own catalogue 11 of 46); build client backend on upstream 55 of 68; build UI on upstream 21 of 26. The upstream UI on the build does the same calls as on its own stack, with the same results.
- **One contract point:** the OpenAPI enum for the status is `active` and `revoked`; the build follows it, upstream's catalogue only accepts upper case and answers 400. This alone is 17 of the 24 failures of the first combination.
- **The rest is newer stories and error handling:** quality scores and filters and the detail lookup through the Governance Authority are not in the May code; upstream answers with `iana:` problem types or plain JSON errors, and its adapter does not pass the token on every call.
- **Lesson:** as for the Schema Manager, the API contract holds better than the suite numbers suggest. Where the build and upstream differ, the build follows the current OpenAPI and upstream follows its own earlier version.
