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

A separate agent built everything from `specs/` and `spec-digest.md` only, in its own workspace: three Spring Boot backends in one Maven reactor (about 5,100 lines of Java, against about 19,000 for upstream's catalogue backend alone), a Vue 3 UI, two Node stubs (Schema Manager and quality scoring), Keycloak with three realms, 11 containers. About 45 minutes, four fix iterations, no compile errors; the stack came up healthy on the first start, smoke test and `mvn verify` (37 tests) green.

- **26 further spec gaps** in its build log. Notable: the quality profile has no field for the threshold value; no problem type for a revoke or delete by a non-owner; 400 or 422 for a missing `dct:conformsTo`; whether `@id` values are searchable; the group order; paging fields for a search over several catalogues.
- **Black-box suite: 458 of 459 API scenarios pass.** The one failure is a real deviation: for a signature whose verification method is not the issuer's, the build checks the key registry first and answers 403 `providerMismatch`, where D14 puts the signature check first (422 `invalidSignature`).
- **Test-writer bugs found by the first run:** the result lookup did not look inside the `meta` object of list items, and the header check treated `Location` as a comma list. Fixed; no check was weakened. The upstream baseline was run before these fixes.
- **Not done yet:** the contamination check against upstream, the UI browser steps (step 4), swap tests.

### How to read 458 of 459

As with the earlier candidates, both writers used the same decision list, here 83 decisions. The result shows that a component with three backends and federation across data spaces is buildable from the published specs **plus a long written decision list**. It does not show that the published specs are: the core of the catalogue has no stories at all, the contract between Governance Authorities is not published, and the digest had to define three operations itself.
