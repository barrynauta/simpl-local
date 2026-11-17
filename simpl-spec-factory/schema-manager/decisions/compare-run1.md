# Run 1 versus this run: schema-manager

Run 1: the spec digest of 28 Oct 2026, decisions run1 D1 to run1 D63, before the later PSO additions. The digest has no decisions marked as added in a later run, so all 63 count as run 1.

This run: findings F-001 to F-071 (step 20b, `ac-findings.json`), D-001 to D-020 (step 23b, `domain-findings.json`), decisions 30a D1 to 30a D43 (`decisions.json`). Inventory: 55 stories, 210 live criteria.

Classes used:

- `found`: the gap is in this run's findings (finding ids cited).
- `decided only`: no finding, but a 30a decision settles it.
- `missed`: neither a finding nor a 30a decision.
- `environment` and `scope`: not a gap; left out of the found and missed count.
- `agree` / `differ`: on the main question the run 1 decision answers. A difference in detail that a test would see is noted in the line, and makes it `differ` only when it touches the main question.

## 1. Summary

| Measure | Count |
|---|---|
| Run 1 decisions | 63 |
| Environment | 7 (run1 D38, D45, D46, D58, D59, D60, D61) |
| Scope | 1 (run1 D62) |
| Gaps to compare | 55 |
| Found | 50 (91%) |
| Decided only | 2 (run1 D37, D63) |
| Missed | 3 (run1 D19, D23, D36) |
| Found and agree | 32 |
| Found and differ | 18 |
| Found but still `accepted risk` in 30a | 0 (the one 30a accepted risk, 30a D20, agrees with run1 D31 on SEARCH) |
| Decided only: agree | 2 |
| This run's blockers and majors | 63 (16 blockers, 47 majors) |
| New: match no run 1 decision | 7 (3 blockers, 4 majors) |
| Match run 1 text outside the Dn list (source hierarchy, scope tables, section 3) | 4 majors |

Partial finds worth knowing: run1 D8 (content negotiation) is found only for 415 on create (F-071); the 406 rules have no finding and no 30a decision. Run1 D12 is found for name reuse (F-008), but its rule that the name pattern applies to `{schemaName}` in every path (400 before 404) has no counterpart.

## 2. Per decision

| Run 1 | Topic | Class | This run: findings | 30a | Verdict | Why |
|---|---|---|---|---|---|---|
| D1 | Base path | found | F-019 | D15, D38 | agree | Both use `/v1` from the OpenAPI server, not `/api/v1`. |
| D2 | Absolute URIs in Location / Content-Location | found | F-019 | D15, D16 | differ | Run 1: Location after create is the concept `/schemas/{name}`, after new version `/schemas/{name}/{version}`, `?schemaType=SEARCH` only for SEARCH. 30a: always the version URI, with `?schemaType=` always and `&resourceType=` for RESOURCE. |
| D3 | 401 on every operation | found | F-014 | D26 | agree | Same 401 rule and problem type `urn:problem-type:simpl:unauthorized`. |
| D4 | Role names | found | F-015 | D27 | agree | Both keep `GA_SCHEMA_ADMIN` and `GA_SCHEMA_VIEWER`, admin implies viewer. The token claim differs (see run1 D59). |
| D5 | Access matrix | found | F-038, F-039, F-040 | D27, D28 | agree | Same matrix. Run 1 adds "authorisation before validation" (403 before 400); 30a is silent on that order. |
| D6 | "Public" latest-content URI | found | F-005 | D16 | agree | Both: authenticated, viewer or admin, not public. |
| D7 | Webhook endpoint access | found | F-037 | D28 | agree | Both: valid token, no role, Tier 2 gateway out of the build. |
| D8 | Content negotiation, 406 and 415 | found (partly) | F-071 | D15 | agree on the overlap | Both give 415 for a non-multipart create. Run 1 also fixes 406 on `Accept`, `text/turtle` versus `application/json`, and 415 on PATCH and POST /webhooks; no finding or 30a decision covers these. |
| D9 | `GET /schemas/{schemaName}` latest content | found | F-005, F-064 | D16 | agree | Both add it: `text/turtle`, Content-Location, works for revoked, 404 when no match. |
| D10 | Version endpoint: content or metadata | found | F-011, F-033 | D17 | differ | Run 1: content by default, metadata on `Accept: application/json`. 30a: content only, "schema meta datas" read as a slip. |
| D11 | Schema identity | found | F-007, D-001 | D4 | differ | Run 1: key (schemaType, name), name unique within a type. 30a: key (name, schemaType, resourceType), up to three RESOURCE schemas per name, 400 when resourceType is needed to choose. |
| D12 | Name rules and reuse | found | F-008, F-007 | D4, D8, D15 | agree | Both: no reuse of a revoked name within the key (409). Run 1 also applies the name pattern to the path parameter; 30a only on create. |
| D13 | resourceType values | found | F-013, D-012, F-048 | D6, D4 | differ | Run 1: open PascalCase string, UI offers Data, Application, Infrastructure. 30a: closed set of those three, anything else 400. Both: required for RESOURCE, 400 with SEARCH. |
| D14 | Version numbers | found | F-001, D-002 | D3 | agree | Integer strings from "1", server-assigned, client field ignored. |
| D15 | 409 on version number | found | F-001 | D3 | agree | Both withdraw the 409 and the SemVer 400. |
| D16 | Status model and slot rule | found | F-003, F-004, F-021, D-003, D-004 | D7, D11, D12, D13 | agree | Status on the schema, derived version status, new version re-publishes a revoked schema when the slot is free. Edge: new version of a revoked SEARCH schema with another SEARCH published is 201 REVOKED in run 1, 409 in 30a. Run 1 also requires atomic slot checks; 30a is silent. |
| D17 | Second SEARCH schema | found | F-008, F-062 | D8 | agree | 409 only when a PUBLISHED SEARCH exists, same detail text. |
| D18 | PATCH transitions | found | F-002, F-022, F-038, F-043, F-053 | D9, D10 | differ | Run 1: PATCH accepts PUBLISHED and REVOKED, 409 when already in that state. 30a: REVOKED only (PUBLISHED gives 400), revoking a revoked schema gives 200. |
| D19 | Initial changelog "Initial version." | missed | none | none | | See section 3. |
| D20 | Required fields | found | F-062, F-071, F-048 | D15, D14 | agree | OpenAPI required fields, 400 when missing. On a new version run 1 gives 400 for a different resourceType; 30a uses resourceType as a selector, so a mismatch gives 404. |
| D21 | Retention of revoked schemas | found | F-024, F-025, F-034 | D17, D18 | agree | Nothing deleted, revoked schemas and versions stay readable with 200. |
| D22 | Federated search disabled | found | F-025 | D18 | agree | Both leave it to another component. Run 1 names the observable part (empty PUBLISHED SEARCH list, SchemaRevoked event). |
| D23 | Request processing order | missed | none | D22 (stage order only) | | See section 3. |
| D24 | Schema list item | found | F-016, F-017, F-031, F-070 | D30 | differ | Run 1 adds `created`, `modified`, `createdBy`, resourceType null for SEARCH. 30a adds `created` only, no creator, resourceType absent for SEARCH. |
| D25 | Version list item | found | F-016, F-017, D-010 | D30 | differ | Same envelope and fields, derived status, graphName as version URI. Run 1 adds `createdBy`; 30a has no creator. |
| D26 | Pagination and order | found | F-018 | D31 | agree | Same order rules, numeric version order, 200 empty past the end for schemas, 404 for versions, no sort parameter. Run 1 also bounds pageSize 1 to 100 (400 otherwise); 30a does not. |
| D27 | Filters | found | F-031, F-045 | D4, D30 | agree | schemaType default RESOURCE, resourceType with SEARCH gives 400. |
| D28 | Create and new-version responses | found | D-002 | D3, D7 | differ | Run 1 adds status, schemaType, resourceType to the create response. 30a: OpenAPI shape only, status read from the list. |
| D29 | Multipart layout | found | F-019, F-020, F-047 | D14, D15 | agree | Flat OpenAPI form fields, file part `schemaFile`. |
| D30 | Syntax and structure checks | found | F-054 | D22 | agree | Both: parses as Turtle and has at least one `sh:NodeShape`, prefix `Syntax Error: `. |
| D31 | Default properties: which ones | found | F-006, F-051, D-011 | D19, D20 | differ | Run 1: `simpl:uuid` and `simpl:version` in an assumed namespace, configurable. 30a: ten named properties matched by local name plus extras per resourceType. Both: none for SEARCH. |
| D32 | Default properties: how checked | found | F-006, F-023 | D19, D21 | agree | Presence check, no enrichment, stored bytes unchanged. Details differ: full IRI versus local name match; issue code `missingDefaultProperty` versus `defaultPropertyMissing`. |
| D33 | Error detail prefixes | found | F-027 | D22 | agree | Same three prefixes, same stage order, first failing stage stops. |
| D34 | Local versus external references | found | F-035 | D23 | agree | Configured namespace base marks local, a well-known list is skipped, the rest is external. The lists differ slightly (run 1 counts `sh:in` members; 30a skips dcat, odrl, schema.org). |
| D35 | Semantic checks | found | F-035, F-036 | D23, D22 | differ | Run 1: four checks, including `unresolvedShapeReference` for `sh:node` and `sh:qualifiedValueShape`. 30a: two error checks and one warning, no shape-reference check. |
| D36 | Vocabulary Manager outage | missed | none | none | | See section 3. |
| D37 | Semantic validation on create | decided only | none | D22 | agree | Both run all three stages on create and on new version. No finding names the "we don't handle semantic validation here" note. |
| D38 | No event bus | environment | | D24, D25 | agree | Both use webhooks and e-mail only. |
| D39 | Provider e-mail recipients | found | F-009 | D25, D41 | agree | Configured address, asynchronous, a failure is logged. Run 1 allows a comma-separated list; 30a one address. |
| D40 | Provider e-mail: when and what | found | F-009, F-026, F-066 | D25 | differ | Same triggers and RESOURCE-only rule. Exact subjects and body labels differ (for example `Schema new version published: {name} (version {v})` versus `Schema version published: {name} version {v}`). |
| D41 | Webhook subscription contract | found | F-041, F-042, F-060, F-061, D-015 | D29 | agree | Syntax-only URL check, 406 for an unknown event, 409 duplicate, 204 delete. |
| D42 | Webhook triggers | found | F-010, D-005 | D24 | agree | Same triggers, both schema types, no event for a superseded version. |
| D43 | Webhook payload | found | F-010 | D24 | differ | Run 1: `schemaName`, `schemaUri`, header `X-Simpl-Event`. 30a: `name`, no URI, no header. |
| D44 | Webhook delivery | found | F-010 | D24 | differ | Run 1: asynchronous after commit, two retries. 30a: before the API answers, one attempt, no retry. |
| D45 | UI identity and routes | environment | | D38 | | 30a fixes the UI port only, no client id or routes. |
| D46 | UI test hooks (`data-testid`) | environment | | none | | 30a has no test hooks. |
| D47 | Notifications only for RESOURCE | found | F-009 | D25, D1 | agree | SIMPL-33694 followed as written. |
| D48 | Role-dependent UI | found | F-040, F-059 | D26 | agree | Viewers do not see write actions. Run 1 adds a fixed no-role page text. |
| D49 | Create Schema form | found | F-028, F-032, F-045, F-047 | D32, D34, D5 | agree | Same fields, inline validation, disabled submit, same two 409 texts, redirect to details. |
| D50 | Create New Version form | found | F-049, F-050, F-056 | D14, D32 | agree | Schema type read-only from the page, no version field, back to the details page. |
| D51 | Revoke from the list | found | F-029 | D33 | agree | Confirmation popup, `Schema "{name}" revoked successfully.` |
| D52 | Publish from the list | found | F-043, F-002 | D9 | differ | Run 1 has a Publish action; 30a has no Publish button. |
| D53 | Schema list page | found | F-017, F-018, F-045, F-055 | D30, D31, D32, D34 | differ | Run 1 keeps a Creator column and fixed empty and error texts; 30a drops Creator and fixes no empty-state text. |
| D54 | Details page | found | F-017, F-018, F-068 | D30, D31 | differ | Run 1 keeps a Creator column and default order version descending; 30a drops Creator. |
| D55 | No version field in the UI | found | F-001 | D3 | agree | |
| D56 | Download latest | found | F-005, F-058 | D16, D35 | differ | File name `{name}-{version}.ttl` versus `{name}-v{version}.ttl`; fixed error text versus the Problem detail. |
| D57 | Download a version | found | F-046, F-058 | D35 | differ | Same endpoint and no selection box. File name and success text differ (`Schema version {version} downloaded successfully.` versus `Schema downloaded.`). |
| D58 | Persistence | environment | | D40 | differ | Run 1: tests never reset, shared state. 30a: a fresh empty instance per scenario. |
| D59 | Identity provider | environment | | D39 | differ | Run 1: Keycloak realm, top-level `roles` claim. 30a: `realm_access.roles`, tokens may be minted with a local key. |
| D60 | Test doubles | environment | | D41, D42, D43 | differ | Run 1: Vocabulary Manager HTTP stub. 30a: a vocabulary fixture file. Mailpit versus MailHog API. |
| D61 | Test data conventions | environment | F-052 (500 paths) | D37, D40 | differ | Run 1: unique names, own resourceType per test, 500 paths not written. 30a: fresh instance, 500 paths in component tests with an injected fault. |
| D62 | Quality profile operations | scope | D-009, D-010 | D2 | agree | Both: not built, 404. |
| D63 | Problem `type` URNs | decided only | none | D22, D26 | agree | 30a uses `urn:problem-type:simpl:` values without a finding on the mixed `iana` and `simpl` prefixes in the OpenAPI. |

## 3. Missed

| Run 1 | What run 1 decided | Why the validators may have missed it |
|---|---|---|
| D19 | Version 1 always gets changelog `Initial version.` | The default sits in a technical note of SIMPL-13353 ("like ..."), not in a criterion, and `CreateSchemaPayload` has no changelog field. The AC checks look at criteria and the domain checks at terms, so a field with no request input and no AC falls between them. |
| D23 | Order of checks: 401, 403, 415, 400, 404, 409, then content validation | Every criterion tests one failure at a time, so no single criterion is ambiguous. The gap only shows when two failures combine, which no check rule looks for. 30a D22 fixes the order of the three validation stages only. |
| D36 | Vocabulary Manager outage gives 500, nothing stored | The specs are silent on the dependency failing, and silence is not a contradiction. The AC checks test what criteria say; no rule asks "what if a called service is down". 30a then replaced the call with a fixture file (30a D42), so the case cannot occur in its build. |

Partial misses inside found decisions: the 406 rules of run1 D8, the path-parameter name pattern of run1 D12, the pageSize bounds of run1 D26 and the atomic slot check of run1 D16. All four are cross-cutting wire or concurrency rules that no single criterion states.

## 4. New

Blockers and majors of this run that match no run 1 decision.

| Finding | Severity | What it says | Note |
|---|---|---|---|
| F-012 | blocker | SIMPL-32807/AC6 contradicts itself: the schema exists as SEARCH, SEARCH is requested, yet 404 is expected. | Run 1 lists the 404 paths of SIMPL-32807 without noting this. 30a D14 reads it by its heading. |
| D-001 | blocker | The identity has two wire names, `name` and `schemaName`, and the CDM says `schemaName` identifies the schema. | Run 1 decided the key (run1 D11), not the name. 30a D5. |
| D-002 | blocker | The version has five names: `version`, `schemaVersion`, `latestVersion`, `versionNumber`, `versionInfo`. | Run 1 decided the value format (run1 D14), not the names. 30a D3. |
| F-032 | major | SIMPL-13355/AC2 has the same Given and When as the positive case, so it never fails. | 30a D32: triggered by an existing name or an invalid file. |
| D-006 | major | The aggregate is `Schema` (CDM), `SchemaCategory` (API), "schema concept" (stories). | Run 1 used "concept" without choosing a term. 30a D36. |
| D-007 | major | One object, `SchemaValidationIssue`, carries errors and warnings; the CDM calls it a warning. | 30a D22: "validation issue", error in `issues`, warning in `validationWarnings`. |
| D-010 | major | The CDM describes another API revision and is not approved. | Run 1 did not read the CDM. 30a has no decision on it (the finding asks for owner action). |

Matched by run 1 text outside the Dn list (not counted as new):

| Finding | Severity | Run 1 place |
|---|---|---|
| F-030 | major | Section 1 (an [ENHANCE] story wins) and section 3 (the eleven pairs). 30a D1. |
| F-044 | major | Section 2.2: SIMPL-28815 out of scope. 30a D2. |
| F-050 | major | Section 3: the 409 example of SIMPL-32808 is a 404. 30a D14. |
| D-008 | major | Section 2.3: `/vocabularies` out, Schema Manager is a client. 30a D2. |

The new findings come mostly from the domain checks (four of seven): naming and CDM alignment, which run 1 did not read for. The other three are single criteria that contradict or cannot fail, which a criterion-by-criterion check catches and a digest-level read passes over.
