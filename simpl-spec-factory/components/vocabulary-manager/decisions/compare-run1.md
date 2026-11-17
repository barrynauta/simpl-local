# Compare with run 1: Vocabulary Manager (step 30c)

Run 1: the spec digest (`spec-digest.md`, 27 Oct 2026), decisions run1 D1 to run1 D30. The run1 D18 clarification of 28 Oct 2026 counts as part of run 1. No other later additions are marked in the digest.

This run: findings F-nnn (`validation/ac-findings.json`, step 20b) and D-nnn (`validation/domain-findings.json`, step 23b), decisions 30a D1 to 30a D37 and 30a E1 to 30a E3 (`decisions/decisions.json`, step 30a).

Classes used below:

- **found**: the gap is in this run's findings (finding ids cited).
- **decided only**: a 30a decision settles it, but no finding names the gap.
- **missed**: neither a finding nor a 30a decision covers it.
- **environment** or **scope**: not a gap; left out of the found and missed counts.

## Summary

| Measure | Count |
|---|---|
| Run 1 decisions | 30 |
| Environment | 2 (run1 D6, run1 D27) |
| Scope | 1 (run1 D21) |
| Gaps counted | 27 |
| Found | 22 (one of them partial: run1 D2) |
| Decided only | 2 (run1 D8, run1 D19) |
| Missed | 3 (run1 D1, run1 D5, run1 D22) |
| Found or decided only, decisions agree | 13 |
| Found or decided only, decisions differ | 11 |
| Found, 30a still open (accepted risk) | 0 |
| This run, blockers | 13: 8 match run 1, 5 new |
| This run, majors | 39: 21 match run 1, 18 new |

"Match run 1" includes 5 findings (F-010, F-012 in part, F-041, F-042, D-010) that match the unnumbered scope section of the digest rather than a numbered decision.

## Per decision

| Run 1 | Topic | Class | This run: findings | This run: decision | Agree or differ | Why |
|---|---|---|---|---|---|---|
| run1 D1 | API shape (R07.02 draft vs OpenAPI) | missed | none | none (30a source hierarchy does not name R07.02) | n/a | No finding compares the R07.02 draft API with the OpenAPI. |
| run1 D2 | Public download | found (partial) | F-052 | 30a D17 | agree | Both: every operation needs a token except /health. The R07.02 claim that downloads are public is not raised; only the /health exception is. |
| run1 D3 | Roles | found | F-002, D-018 | 30a D16 | differ | Run 1: viewer may call every GET and the two search POSTs. 30a: viewer also may call POST /external-vocabularies/content (three read-only POSTs). |
| run1 D4 | 401 and 403 | found | F-011, F-002, F-052 | 30a D17 | agree | Both: 401 without or with an invalid token, 403 without the role, as problem+json. 30a adds problem types and says the service checks, not only the gateway. |
| run1 D5 | Error format (Problem Details, API Guidelines page) | missed | none | none | n/a | No finding on the error format or on the missing API Guidelines page. 30a decisions use problem+json throughout, without a finding. |
| run1 D6 | Identity provider in compose | environment | none | 30a E1 | differs (not counted) | Run 1: a local OIDC issuer, `roles` claim, client `vocabulary-tests` with password grant, password `password`. 30a: Keycloak realm `simpl`, `realm_access.roles`, clients `vocabulary-manager-ui` and `vocabulary-manager-be`, password `Password1!`. Same three users. |
| run1 D7 | Version numbers | found | F-014 | 30a D7 | agree | Integers, version 1 at creation, plus 1 each time. |
| run1 D8 | Manual version in a request | decided only | none (F-014 is about the number format, not the form field) | 30a D7 | agree | Both: 400 with an input validation issue on `version`. |
| run1 D9 | New version activates | found | F-015, D-007 | 30a D4 | agree | Both: new version ACTIVE, older versions DEPRECATED, same transaction. 30a writes it as "every older ACTIVE version". |
| run1 D10 | Vocabulary status vs version status | found | F-001, F-015, D-007, D-006 | 30a D3, 30a D4 | differ | Run 1: deprecating a vocabulary sets all versions DEPRECATED; reactivating sets the latest back to ACTIVE; a version cannot become ACTIVE while another is ACTIVE (409). 30a: two separate statuses, a vocabulary status change never writes to versions ("effectively deprecated" instead); more than one ACTIVE version is allowed after a manual reactivation. |
| run1 D11 | New version on a deprecated vocabulary | found | F-001 | 30a D3 | differ | Run 1: 409, reactivate first. 30a: allowed, the vocabulary stays DEPRECATED. |
| run1 D12 | Changelog on creation | found | F-007 | 30a D32 | agree | The UI sends a fixed changelog. Run 1 "Initial version", 30a "Initial version." (with a full stop). |
| run1 D13 | Duplicate name | found | F-036, F-035 | 30a D15, 30a D37 | differ | Both: 409. Run 1 shows it as a field error in the UI; 30a shows it in a message above the form and makes uniqueness case-insensitive. |
| run1 D14 | "Published" vs "active" | found | F-012, D-016, F-051 | 30a D6, 30a D31 | agree | No publish step or registry; a stored valid version is ACTIVE; UI label "Active". |
| run1 D15 | File format | found | F-013, F-037, F-036 | 30a D15 | differ | Both: Turtle only. Run 1: anything else 415. 30a: 415 only for a wrong request content type; a file part that is not valid Turtle is 400. |
| run1 D16 | Syntax and structure rules | found | F-005, F-048 | 30a D9 | differ | Run 1: structure is its own check (graph not empty and declares owl:Ontology or skos:ConceptScheme); report all errors, line and column. 30a: "structure" is the UI word for the semantic step; empty file is semantic rule S6; no Ontology or ConceptScheme rule; only the first syntax error is reported. |
| run1 D17 | Semantic rules | found | F-005, F-003, D-003 | 30a D9, 30a D10, 30a D11, 30a D12 | differ | Run 1 errors: unknown term in a referenced version, deprecated referenced version, missing referenced vocabulary or version, IRI both class and property. 30a errors: S1 to S7 (literal on object property, resource on datatype property, property both object and datatype, duplicate prefLabel per language, disjoint classes, empty file, missing reference). A deprecated reference is a warning in 30a, an error in run 1. Unknown namespace is a warning in both. |
| run1 D18 | How a vocabulary references a version | found | F-032, D-005, F-031 | 30a D12 | differ | Same idea in both: a namespace IRI, no version means latest, own namespace is not a reference. Different form: run 1 `{base}/{Name}/{n}#`, base `https://vocabulary.simpl.local/vocabularies`; 30a a declared prefix starting `<contentBase>/content/<Name>[/versions/<n>]`, contentBase `https://authority.example/vocabulary-manager`. 30a also lists W3C namespaces that never warn. |
| run1 D19 | Status code for a missing reference | decided only | none (no finding names the 404 vs 400 review answer) | 30a D12 (S7) | agree | Both: 400 with a validation issue. |
| run1 D20 | Warnings on success | found | F-004, F-040, D-003 | 30a D10, 30a D14 | agree | 201 with validationWarnings; the UI shows them on the confirmation. 30a adds a closed set of warning codes and an empty array when there are none. |
| run1 D21 | Visual design (Figma not in input) | scope | (F-047, F-030 touch the wording) | 30a D36 asks for the Figma wording as an owner question | n/a | A statement about the input set, not a gap. |
| run1 D22 | List columns (no file format column) | missed | none | none | n/a | No finding on the file format column. |
| run1 D23 | Name filter | found | F-009 | 30a D19 | differ | Both: case-insensitive "contains". Run 1 keeps the OpenAPI pattern and the UI capitalises the first letter. 30a relaxes the pattern to `^[A-Za-z0-9]+$` and the UI sends the text as typed. |
| run1 D24 | "Full details" download | found | F-022, F-024, D-015 | 30a D29 | agree | The download is the Turtle content only; metadata is shown on screen. |
| run1 D25 | Where the deprecation warning shows | found | F-030, F-047, F-031 | 30a D36, 30a D14 | differ | Both: list, details and version history with a link to the latest active version. Run 1 also warns when a reference is selected in the new-version form. 30a: no picker; the reference warning appears in the warnings panel after a successful submit. |
| run1 D26 | Hidden or disabled actions | found | F-039 | 30a D18 | agree | Hidden for users without the admin role; direct calls get 403. 30a adds a message for a direct URL. |
| run1 D27 | Persistence | environment | none (source is a story note) | 30a E3 | agrees (not counted) | Apache Jena Fuseki in both. |
| run1 D28 | Validation notification | found | D-004, F-033 | 30a D14 | agree | The synchronous response is the report; nothing separate is sent or stored. |
| run1 D29 | Page size | found | F-018 | 30a D22 | agree | Maximum 100, above that 400. 30a adds the rule for a page past the last. |
| run1 D30 | Upload limit | found | F-013 | 30a D15 | differ | Run 1: 10 MB, configurable (digest says the OpenAPI gives no value). 30a: 5 MB (F-013 reads a 5 MB limit in the API). |

Not numbered in run 1, for reference: the scope section (SIMPL-393, SIMPL-395 and SIMPL-28680 out of scope, manual semantic review out of scope) matches F-010, F-041, F-042, D-010 and 30a D1, 30a D6, and both runs agree. Run 1 open question 6 (external vocabularies have no story) has no numbered decision; this run raises it as D-008 and D-020 and decides it in 30a D30.

## Missed

| Run 1 | Why the validators may have missed it |
|---|---|
| run1 D1 API shape | The conflict is between the R07.02 tech description and the OpenAPI. In this run R07.02 is only a link inside the stories (it is not an inventoried source), so there was nothing to compare against. |
| run1 D5 Error format | The stories and the OpenAPI agree on problem+json; the tension in run 1 came from R07.02 (Belgif guidance) and review comments pointing to an API Guidelines page outside the input. Without those, no conflict shows. |
| run1 D22 List columns | The only trigger is a review comment saying Figma has a file format column. The story's own column list is complete and consistent, so a check on the criteria finds nothing to flag. |

Partly missed: run1 D2. The /health exception is found (F-052), but the R07.02 statement that downloads are public is not, for the same reason as run1 D1.

## New in this run

Blockers and majors that match no run 1 decision.

| Id | Severity | Gap | 30a decision |
|---|---|---|---|
| F-006 | blocker | Validation on file upload before submit, with no validate-only endpoint. | 30a D13 |
| F-008 | blocker | Whether a new version may change only the description, and what "pre-filled with content" shows. | 30a D33 |
| D-001 | blocker | VocabularyCategory: is it the vocabulary or a view of its latest version, and which name. | 30a D25 |
| D-002 | blocker | The vocabulary version has four names; VocabularyMetadata clashes with the stories. | 30a D26 |
| D-003 | blocker | The CDM uses "validation warning" for blocking errors too (syntaxError, semanticError as warning codes). | 30a D10 |
| F-016 | major | What a date filter matches. | 30a D20 |
| F-017 | major | Unknown query parameters: 400 or ignored. | 30a D21 |
| F-019 | major | List "date created" and "creator": the vocabulary or its latest version. | 30a D25 |
| F-020 | major | UI paging and empty list. | 30a D34 |
| F-021 | major | Sortable columns, default order, direction. | 30a D23 |
| F-023 | major | What selecting a version shows. | 30a D35 |
| F-025 | major | History order; details page and history page the same screen. | 30a D23, 30a D35 |
| F-026 | major | History page on load failure or not found. | 30a D35 |
| F-027 | major | "All versions" against a paginated API. | 30a D35 |
| F-028 | major | Search or filter over versions. | 30a D35 |
| F-029 | major | Creator on version metadata (field missing in the API). | 30a D27 |
| F-033 | major | Indicator that syntax validation passed. | 30a D14 |
| F-034 | major | Nothing stored after a failed creation. | 30a D8 |
| F-035 | major | Field format rules and error messages on the create form. | 30a D37 |
| F-038 | major | Status change to the current status (409) and UI offering only the other status. | 30a D4 |
| D-006 | major | "Latest version": highest number or latest ACTIVE. | 30a D5 |
| D-008 | major | External vocabulary identity: name or namespace URI. | 30a D30 |
| D-009 | major | The CDM snapshot is not approved. | 30a D2 |

Notes on the counts: D-003 overlaps run1 D20 on warnings being non-blocking, but its gap (the CDM code set) is new; the CDM was not a run 1 source. F-018 matches run1 D29 on the maximum page size; its page-past-the-last part is new but is counted as a match.
