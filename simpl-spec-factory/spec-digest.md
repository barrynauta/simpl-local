# Spec digest: Vocabulary Manager (run 1)

Step 1 of the factory, written 2026-10-27 from the frozen input set in [`specs/`](./specs/README.md), before any code or test exists.

Purpose: read all specs once, list where they disagree or are silent, and make **one recorded decision per point**. The test writer and the code writer both work from `specs/` **plus this file**, so they resolve the same gaps the same way. Every decision here is also a finding: a place where the specs could be made more precise.

Tone: the specs are in good shape for a factory run. Most stories have testable Given/When/Then criteria, the OpenAPI is detailed, and the review comments show a team actively tightening the criteria. The points below are what is left to make the specs fully buildable.

---

## 1. Source hierarchy

When sources disagree, the higher one wins:

1. **OpenAPI** (`vocabulary_openapi-tier1.yaml`, Sep 2026): the API contract. It is the newest source, and the stories themselves defer status codes and payloads to "the API definition task", whose output this file is.
2. **User stories** (acceptance criteria): behaviour, UI and authorisation.
3. **R07.02 tech description** (May 2026): domain model, lifecycle and validation rules where the stories are silent. Its "API Design (Draft)" section is superseded by the OpenAPI.
4. **BP02B business process**: intent and vocabulary of the domain.

## 2. Scope

**In scope: 32 stories.** Paired API and UI stories for: list, filter and sort, details, version history, download, create, new version, automatic versioning, automatic publication, automatic deprecation, manual status change, syntax validation, semantic validation, validation report, deprecated-reference check, deprecation warnings, role-based access.

**Out of scope, 3 stories:**

| Story | Why |
|---|---|
| SIMPL-393 Provide CPS vocabulary (2024) | Federated Catalogue era; superseded by the Vocabulary Manager stories. CPSV can be used as seed data. |
| SIMPL-395 Provide UI for additional vocabulary (2024) | Same: describes a UI over catalogue calls, replaced by the 2026 stories. |
| SIMPL-28680 Metadata Quality Profile update | A Schema Manager story, matched by the search on "vocabulary". |

**Also out of scope:** the manual part of semantic review (BP02B.02), the sunset policy for deprecated versions (BP02B.05), and offering vocabularies to participants other than the Governance Authority (raised in a SIMPL-18559 comment; needs its own stories).

**In scope without stories (OpenAPI only):** external vocabularies (6 operations), the two search endpoints, version-level status change, health. These get tests derived from the OpenAPI only, marked as such in the traceability matrix.

## 3. Decisions

Each row: what the specs say, what we build, and the open question for the spec owners.

### API and access

| # | Topic | What the specs say | Decision for run 1 |
|---|---|---|---|
| D1 | API shape | R07.02 draft: `PATCH /vocabularies/{name}/status`, public `GET /vocabulary/{name}`, `versionLabel`, `/api` prefix. OpenAPI: `PATCH /vocabularies/{name}`, `GET /content/{name}`, integer `version`. | Follow the OpenAPI. R07.02's draft API is treated as superseded. |
| D2 | Public download | R07.02: anonymous users may use the public download endpoints. SIMPL-18196: **all** endpoints require a token. OpenAPI: global OAuth2, no exception for `/content`. | Everything requires a token except `/health`. Open question: should downloads be public, as R07.02 says? |
| D3 | Roles | Stories and R07.02: `GA_VOCABULARY_VIEWER` (read) and `GA_VOCABULARY_ADMIN` (read and write). OpenAPI: scopes `read` and `write`, no role names. | VIEWER may call every GET and the two search POSTs; ADMIN may call everything. |
| D4 | 401 and 403 | SIMPL-18162 and 18196 require 403 without the role and an authentication error without a token. The OpenAPI defines neither 401 nor 403 on any operation. | Return 401 (no or invalid token) and 403 (no role), as Problem Details. Add both to our copy of the contract; flag the OpenAPI gap. |
| D5 | Error format | R07.02: RFC 9457 Problem Details, Belgif guidance. Review comments point to the Simpl API Guidelines page for generic errors (not in our input). OpenAPI: `application/problem+json`, `Problem`, `issues[]`. | Follow the OpenAPI Problem schema. Open question: should the API Guidelines page be part of the input set? |
| D6 | Identity provider in `docker compose` | Upstream: Tier 1 authentication (Keycloak). | A local OIDC issuer in the compose file. Roles come from a `roles` claim (array) in the access token. Test harness contract: the issuer supports the password grant for client `vocabulary-tests` (public client) and has three users, password `password`: `ga-admin` (`GA_VOCABULARY_ADMIN`), `ga-viewer` (`GA_VOCABULARY_VIEWER`), `ga-norole` (no vocabulary role). Not a spec question, an environment choice. |

### Lifecycle and versioning

| # | Topic | What the specs say | Decision for run 1 |
|---|---|---|---|
| D7 | Version numbers | SIMPL-18173 and 18190: "same versioning library as schema management" (not in input). OpenAPI: integer `version`. | Integers, starting at 1, incremented by 1. |
| D8 | Manual version in a request | SIMPL-18190: reject. OpenAPI: "must not provide a `version` form field", no code given. | 400 with an input validation issue on `version`. |
| D9 | New version activates | SIMPL-18173 and 18191: new version ACTIVE, **all** previous versions DEPRECATED. BP02B.03: the previous version is deprecated. | As the stories: all previous versions DEPRECATED, in the same transaction as the creation. |
| D10 | Vocabulary vs version status | SIMPL-18175: manual vocabulary status ACTIVE/DEPRECATED, reversible. BP02B.05: full deprecation means all versions deprecated. OpenAPI also has a version-level PATCH with no story. | Deprecating a vocabulary sets all its versions to DEPRECATED. Re-activating it sets only the latest version back to ACTIVE. Version-level PATCH follows the OpenAPI, but a version cannot become ACTIVE while another version of the same vocabulary is ACTIVE (409). Open question: is this the intended relation between the two statuses? |
| D11 | New version on a deprecated vocabulary | Not specified. | Rejected with 409: re-activate the vocabulary first. Open question. |
| D12 | Changelog on creation | OpenAPI: `changelog` required on create. SIMPL-18169 (UI): no change log field on the creation form. A review comment suggests "Initial version". | The UI sends `changelog = "Initial version"` automatically. |
| D13 | Duplicate name | SIMPL-18169 (UI): the name must be unique. API stories: silent. OpenAPI: 409 on create. | 409 Conflict on a duplicate name, shown as a field error in the UI. |
| D14 | "Published" vs "active" | SIMPL-18186 and 18187 speak of publication and a "registry"; a review comment asks to choose one term. | There is no separate publish step or registry: a stored, valid version is ACTIVE and available. One term in UI and docs: **Active**. |

### Validation

| # | Topic | What the specs say | Decision for run 1 |
|---|---|---|---|
| D15 | File format | OpenAPI: "Vocabulary file in Turtle format"; downloads are `text/turtle`. BP02B: RDF Schema or OWL. | Turtle only; anything else 415. RDFS and OWL content in Turtle are both accepted. |
| D16 | Syntax and structure rules | SIMPL-18180 and 18181: syntax and "structure rules", all errors with exact location, do not stop at the first error. "Structure rules" are not defined anywhere. | Syntax = the file parses as Turtle (location: line and column). Structure = the graph is not empty and declares at least one `owl:Ontology` or `skos:ConceptScheme`. Open question: what are the intended structure rules? |
| D17 | Semantic rules | SIMPL-18182: terms "unique, consistent, and logically coherent". R07.02: "minimum consistency checks", internal and external reference resolution. SIMPL-18183 notes: only self-managed vocabularies are validated; unresolvable external references give warnings; report all issues. | Errors: (a) a term used from an internal vocabulary does not exist in the referenced version; (b) the referenced internal version is DEPRECATED (SIMPL-18184); (c) the referenced internal vocabulary or version does not exist; (d) one IRI declared both as a class and as a property. Warnings: a namespace that is neither internal nor a registered external vocabulary. Open question: this is the largest gap. The checks need a written list before two teams can build them the same way. |
| D18 | How a vocabulary references a version of another | Needed for D17 and SIMPL-18184, but not described. A tester's comment on SIMPL-18184 notes the UI form has no way to reference a version. | By namespace IRI in the Turtle: `{base}/{Name}/{n}#`, where `{base}` is configurable with default `https://vocabulary.simpl.local/vocabularies`. `{base}/{Name}#` (no version) refers to the latest version. A vocabulary's own namespace (its own name, with or without a version number) is where it defines its terms, not a reference: only the namespaces of other vocabularies count as references. (Clarified 28 Oct 2026 after the Rust run, see findings.) Open question. |
| D19 | Status code for a missing reference | A review answer on SIMPL-18184 says 404. The OpenAPI says semantic problems are 400. | 400 with a `VocabularyValidationIssue`: the request itself is found, its content is invalid. |
| D20 | Warnings on success | R07.02: warnings do not block. OpenAPI: `validationWarnings` on the version response. SIMPL-18194: success "without separate error details". | 201 with `validationWarnings` filled when there are warnings. The UI shows them on the confirmation. |

### UI

| # | Topic | What the specs say | Decision for run 1 |
|---|---|---|---|
| D21 | Visual design | Every UI story links to a Figma design that is not in our input. | Build from the stories only, with a plain component library. The assessment states that the design was not used. |
| D22 | List columns | SIMPL-18161: actions, name, description, version, status, created, last updated, creator. A comment notes Figma also has a file format column. | The story's columns; no file format column (always Turtle, D15). |
| D23 | Name filter | SIMPL-18163 (UI): "Name contains 'shacl'". OpenAPI: `name` query must match `^[A-Z][a-zA-Z0-9]*$`, which rejects lowercase input. | API: case-insensitive "contains" on the name; the UI capitalises the first letter before sending. Open question: the pattern and the UI example do not fit together. |
| D24 | "Full details" download | SIMPL-18165 and 18452: download the "full details" of a version. A tester asked what that means. | The Turtle content of that version (`/content/...`). Metadata is shown on screen, not in the file. |
| D25 | Where the deprecation warning shows | SIMPL-18178: when a deprecated vocabulary is visible. A tester notes the epic says: when one is referenced or selected. | Everywhere: in the list, on the details and history pages (with a link to the latest active version, SIMPL-18559), and when selecting a reference in the new-version form (SIMPL-18184). |
| D26 | Hidden or disabled actions | SIMPL-18195: restricted actions "hidden or disabled". | Hidden for VIEWER. Direct calls still get 403 from the API. |

### Other

| # | Topic | What the specs say | Decision for run 1 |
|---|---|---|---|
| D27 | Persistence | A story note: "use same persistence layer as Schema Management - Fuseki DB". | Apache Jena Fuseki, in the compose file. |
| D28 | Validation notification | BP02B.06: Simpl-Open sends a report to the Governance Authority. | The synchronous API response is the report; no separate notification. |
| D29 | Page size | OpenAPI: `page` from 1, `pageSize` default 20, minimum 1, no maximum. | Maximum 100; above that 400. Open question: what maximum is intended? |
| D30 | Upload limit | OpenAPI: 413 above "the configured upload limit"; no value. | 10 MB, configurable. |

## 4. Open questions for the spec owners

In order of impact on buildability:

1. **Semantic and structure rules (D16, D17):** a written list of checks, with an example of each.
2. **Referencing other vocabulary versions (D18):** the mechanism inside the Turtle file.
3. **Vocabulary status vs version status (D10, D11).**
4. **401 and 403 in the OpenAPI (D4)**, and whether downloads are public (D2).
5. **Name filter (D23)**, page size (D29), upload limit (D30).
6. **External vocabularies:** six operations in the OpenAPI, one line in R07.02, no story. Who uses them, and with which roles?

## 5. What this means for the next steps

- **Test writer:** Gherkin features from the 32 stories plus D1 to D30; OpenAPI-only features marked `@openapi-only`; every scenario tagged with its story key or decision number.
- **Code writer:** Spring Boot backend and Vue 3 UI from the same inputs, without seeing the tests.
- **findings.md:** starts with this digest. Each run adds what the factory got right, what needed correction, and which decisions turned out to matter.
