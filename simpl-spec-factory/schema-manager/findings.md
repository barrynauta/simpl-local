# Findings: Schema Manager

Second candidate of the factory, after the Vocabulary Manager. Input: published or to-be-published material only (stories without review comments, the OpenAPI, the business process). See [`specs/`](./specs/).

## 2026-10-28: step 0, input set

### The canonical OpenAPI contains the Vocabulary Manager API, in an outdated version

- `schema_openapi.yaml` (schema-manager-be, main, commit `f9e5205`, 2026-10-21) has 12 paths. Four of them are `/vocabularies` paths, with six operations under the tag "Vocabulary management". Their descriptions say the vocabularies are "stored in simpl-schema-manager".
- The service does not implement them. It has no endpoint for `/vocabularies`. It is a **client** of the Vocabulary Manager: semantic validation resolves vocabulary references by calling the Vocabulary Manager over HTTP (`SemanticValidationConfig`, `vocabulary-manager.base-url`). So the design is sound; the contract is not.
- The operations were added on 2026-02-25 under SIMPL-18201 ("Vocabulary Management API - Governance Authority"), most likely before the Vocabulary Manager had its own repository, and were not removed afterwards.
- They have drifted: the Vocabulary Manager's own OpenAPI now has 13 paths (search, content download, external vocabularies, version status). The copy here has 6 operations.
- The openapi README says `schema_openapi.yaml` is the "active" spec, used by ITB, code generation and deployments, and must have "the same effective contract" as the split layout. The split root and the tier 1 and tier 2 files contain no `/vocabularies` paths. So the two layouts disagree, and anything generated from the full file advertises six endpoints that answer 404.
- **For this run:** the `/vocabularies` paths are out of scope. The code writer builds the Schema Manager as a client of a Vocabulary Manager, not a second one.
- **Suggestion for the spec owners:** remove the vocabulary paths from `schema_openapi.yaml` and refer to the Vocabulary Manager's OpenAPI for the calls the Schema Manager makes.

## 2026-10-28: step 1, spec digest

Written by a fresh agent from the public input only ([`spec-digest.md`](./spec-digest.md)).

- **63 decisions** (Vocabulary Manager: 30), from 55 stories (Vocabulary Manager: 35). The Schema Manager specs leave about twice as much open per story.
- **Scope:** 54 of 55 stories, 4 of them partly (parts belong to the catalogue, Tier 2 or federated search). Out: the billing schema story SIMPL-28815 (the Provider wizard for resource descriptions), the `/vocabularies` operations and the three metadata quality profile operations (no story and no operation to create a profile).
- **Enhancement stories:** 8 of 11 add `schemaType` on top of the original. SIMPL-32807 and SIMPL-32808 replace a client-supplied SemVer with a server-assigned integer. SIMPL-32798 conflicts with its original (metadata against content).
- **Main gaps, by impact on buildability:**
  1. Schema identity: stories use "name", "name and resourceType" and "name and schemaType". Every path and route depends on it.
  2. The default property list is referenced (SIMPL-16971, SIMPL-26100) but not in the published input.
  3. Semantic validation rules and the call to the Vocabulary Manager are not defined.
  4. The status model contradicts itself: per schema or per version, and SIMPL-20529 allows PATCH to PUBLISHED where the OpenAPI does not.
  5. Webhook payloads, triggers and delivery rules, and the Provider e-mail, are not specified.
- **Contract gaps found on the way:** `GET /schemas/{schemaName}` is not in the OpenAPI (only PATCH on that path). The "full, inline" `schema_openapi.yaml` still points to `./common/common-components.yaml` for the versions payload, so it is not self-contained, contrary to its README.
- **Hard to test:** names can never be reused and the publication slots are global, so tests share state. Every test needs unique names and its own resourceType, and the SEARCH tests must run one after another.

## 2026-10-28: step 2, test writer

Written by a fresh agent from the specs and digest only ([`tests/`](./tests/)).

- **327 scenarios:** 291 API in 16 features, 36 UI (Gherkin only, browser steps come in step 4). All 54 in-scope stories and all 63 decisions are tagged at least once. Side effects are tested through a webhook receiver, the Vocabulary Manager stub and the mail capture of D60.
- **Baseline on the local upstream stack** (May 2026 version, no identity provider, no stub, no mail capture): 5 passed, 257 failed, 65 skipped. The baseline says little, for one reason:
- **Upstream applies a structural rule that no spec states.** It rejects the valid test schema with "only one resource type expected in file. Got: []": a schema file must declare its resource type inside the Turtle. No story and no decision mentions this. About 160 scenarios fail in setup because of it. This is the most important finding of the step: a real implementation needs a rule that a reader of the specs cannot know.
- **Other upstream differences on decisions:** problem types `belgif:` and `iana:` instead of one prefix (D63, about 70 scenarios); "Syntax error:" where SIMPL-14795 says "Syntax Error: "; no maximum page size (D26); 400 instead of 406 for an unknown webhook event and instead of 404 for a malformed webhook id (D41); 500 instead of 415 for a JSON body on create (D8).
- **Newer stories:** upstream ignores `schemaType` (SIMPL-33537, the enhancement stories), as expected for a May 2026 version.
- **Digest still too vague for a test:** what `Accept: application/json` returns on a schema (D8), whether the name-clash text applies to RESOURCE schemas (D49), whether missing default properties are reported next to other errors (D32), and whether webhook retries count as one delivery (D44).

## 2026-10-28: step 3, code writer (run 1)

A separate agent built backend (Spring Boot 3.5, Java 21, PostgreSQL with Flyway, Jena for Turtle and SHACL), UI (Vue 3, Vite, TypeScript, keycloak-js) and the environment (Keycloak realm, PostgreSQL, Mailpit, a Vocabulary Manager stub) from `specs/` and `spec-digest.md` only, in its own workspace. About 27 minutes, five fix iterations. Output: [`generated/run1/`](./generated/run1/), with its own `TRACEABILITY.md` and `BUILD-LOG.md`.

- **Isolation:** no file access outside its workspace, no web access.
- **Contamination:** 8 of 28 class names are shared with upstream's 104, all generic or from the OpenAPI (`SchemaController`, `SchemaService`, `SchemaType`, `Webhook`); different package; backend about a quarter of upstream's size (2,500 against 9,300 lines). No sign that the build reproduces upstream code.
- **Black-box suite: 291 of 291 API scenarios pass** (289 in the normal run, 2 in the dedicated run with the Vocabulary Manager down). This includes the Provider e-mails seen in the mail capture and the webhook deliveries seen by the receiver.
- **Test-writer bug found by the first run:** every scenario got the same "unique" names, because the scenario counter was kept on the behave context, which resets per scenario. 222 scenarios failed on "a schema with that name already exists". The upstream baseline could not show it, because upstream rejected every create. Fixed in `features/environment.py`; no check was changed.
- **38 further spec gaps** in its build log. Notable: there is no metadata read for one schema, so the details page has no source for its title; the list columns of D54 and the per-row download of D57 do not fit together; D11 and D20 give different status codes for a resourceType mismatch; how other 4xx answers of the Vocabulary Manager are treated.

### How to read 291 of 291

As with the first candidate, both writers used the same 63 decisions. The result shows that these specs **plus a long written decision list** are buildable. It does not show that the published specs are: they needed twice as many decisions as the Vocabulary Manager, and upstream itself applies at least one rule that no spec states (the resource type declared inside the schema file, see step 2). The decision list is the real deliverable for the spec owners.

## 2026-10-28: step 4, UI browser steps

The 36 UI scenarios made runnable with Playwright by a fresh agent, which saw the rendered pages only, not the code. Locators: story wording first, then the texts and `data-testid` values of the digest (D45 to D57), then tolerant patterns.

- **Factory build: 36 of 36.** API rerun after the change: 289 passed, 2 skipped (the Vocabulary-Manager-down pair).
- **Local upstream: 0 of 36.** This says little about the upstream UI:
  - 23 fail in API setup: 20 on the unstated resource type rule (step 2), 3 because upstream has no `schemaType` yet (SIMPL-33537 is newer).
  - 9 create-form scenarios: upstream has no `/schemas/new` route (it opens a schema called "new") and no `data-testid` values. Different decision (D45, D46).
  - 2 filter scenarios: no schema type filter, a newer story (SIMPL-32790).
  - 1 implementation deviation: the column header reads "Name"; SIMPL-15837 and SIMPL-32790 say "Schema Name".
  - 1 environment: with no sign-in there is no state for a user without a role.
- **Test-writer bugs fixed:** the request matcher of the fault-injection step was too literal (parameter order, default values); the build UI calls the API on its own address; the list is paged and the store keeps all earlier data, so the steps page through the list as a user would. One feature changed: "No match for the filters" assumed no revoked SEARCH schema exists; it now makes sure no SEARCH schema is published and runs serially.
- **Story wording too vague for a strict check:** D54 lists 7 columns while D57 puts a Download action on each row; what a click on a column header does to the sort direction; pagination "disabled or hidden" (SIMPL-32790) against hidden (D53); the format of the creation date; how a "read-only" schema type is shown (D50); error texts given only as examples ("e.g.").
