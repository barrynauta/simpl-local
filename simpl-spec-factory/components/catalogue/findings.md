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

## 2026-10-29: step 6, PSO tests

The API tests of the PSO test team, run unchanged through a small stand-in for the participant gateway. Details and commands: [`experiments/pso/`](./experiments/pso/README.md).

- **Build: 32 of 52. Upstream (May stack): 17 of 52.**
- **10 scenarios need live signing** through SD Tooling and the signer, on both targets: environment.
- **None of the other 10 build failures is a build defect:** 5 hit operations the factory left out (D10) or never saw (`/query/search`, in no spec), 4 decisions the digest took differently (D2 Problem JSON against the older `message` field, D66 roles against the IAA role `T2IAA_M`), 1 seed data (the Gaia-X Core ontology, missing on both).
- **PSO partly tests the older contract:** the error shape with `message` and a role the catalogue OpenAPI does not name. As for the Schema Manager, where PSO and the factory differ, PSO follows the running system.
- **Upstream's 35 failures are mostly the local stack:** 12 because it runs without Keycloak, 11 because the May stack has no client backend.

## 2026-11-04: clarification round, run 1.2 (PSO tests as a source)

Step 2 of [`../tests-as-spec.md`](../../tests-as-spec.md). The PSO suite became a source of the digest, below the specifications (digest section 1, with the precedence rule). Changes:

- **D10 revised (rule 3).** Run 1 left out `GET/POST /query`, `GET /verification` and `GET /schemas/latest`, although the OpenAPI has them. That was the factory's own scope choice, so it was corrected. New rows D84 (openCypher query), D85 (claims graph mapping and lifecycle, marked "to validate"), D86 (latest schemas), D87 (verification page).
- **D88 (rule 1).** `POST /query/search` is in no specification; PSO TC_FC_09 shows 400 for an empty body. The build gives it the contract of `POST /query`.
- **D89 (rule 2 with a partial way out).** Every Problem also carries `message`; verification failures also carry `code` `verification_error`. A first version added `code` to every Problem. PSO then failed two scenarios that passed before (TCFC_SV03, TCFC_SV08): PSO validates error bodies with a JSON schema that is `oneOf` Problem or `Error`, and a body with both shapes matches both branches. So one body cannot fully meet both shapes. The error shape stays a question for the spec owners.

How `/query` works now: each catalogue (cat-a, cat-b) has its own Neo4j Community 5.26 in the compose file (no authentication, no host port, no volume). On publish, catalogue-be expands the `credentialSubject` to RDF (the same graph SHACL validates) and writes it as a property graph: one `Resource` node per IRI or blank node, scoped to the RD, with `uri` and `claimsGraphUri`; one extra label per `rdf:type` (local name); literals as properties named after the predicate's local name; links as relationships named the same way. Revoke, delete and a new version remove the claims, so the graph holds active RDs only, as search does. PostgreSQL stays the record of truth, and the graph is rebuilt from it at startup. `POST /query` runs one statement in a read-only transaction with the `timeout` of the request and returns `{totalCount, items}`, one item per row. The mapping is not in any specification and is marked "to validate" in D85 and in `tests-as-spec.md`.

As before, the digest came first. Then the test-writer side added `cat_query.feature` (28 scenarios) and dropped the four D10 rows that are now in scope. The code-writer side changed the build (`generated/run1/BUILD-LOG.md`, "Run 1.2"). In this round both sides were done by one agent, from the digest rows only, so the separation of the two writers is weaker than in run 1.

| Suite | Before | After |
|---|---|---|
| Own API scenarios of run 1 (455 after dropping four D10 rows; 459 before) | 454 of 455 (458 of 459 recorded) | 454 of 455 |
| Own API scenarios, new (`cat_query.feature`) | 1 of 28 on the old build | 28 of 28 |
| Own UI scenarios | 26 of 26 | 26 of 26 |
| `mvn verify` | green | green (one test updated for D10, one added) |
| PSO, with the stand-ins | 39 of 52 | **46 of 52** |

The one own failure is the known D14 order deviation of run 1 (403 `providerMismatch` before 422 `invalidSignature`).

Newly passing in PSO: TC_FC_03, TC_FC_08, TC_FC_09, TC_FC_10, TCC02, TCC09, TCCAT_SV12. Nothing that passed before fails now. The 6 that remain:

- TCC11: the `code` is right now, but PSO expects the exact message "Semantic Error: no proper CredentialSubject found". The build rejects the same body one step earlier, at the signature check (D14 order), with another message. Not taken over.
- 2 times TCC06: PSO sends `simpl:offeringType` "DATA", the seed shape allows lowercase values (question 15).
- TC_FC_06 and TCFC_SV04: the IAA role `T2IAA_M` (question 14).
- TCC03: the Gaia-X Core ontology. The build stores no ontologies (D58, D86); this did not fall out of the graph work, because the graph holds RD claims, not schemas (question 16).

Result file: `experiments/pso/results/pso-on-build-run1.2.json`.
