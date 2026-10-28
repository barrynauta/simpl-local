# Spec digest: Schema Manager

Second candidate of the spec-driven factory. This document is the single working reference for the test writer and the code writer, next to the frozen input set in [`specs/`](./specs/). It records what the specs say, where they are silent or disagree, and the decision taken for this run. Every decision is also a finding for the spec owners.

Input read: `specs/README.md`, the 55 stories in `specs/stories/`, `specs/api/schema_openapi.yaml` (1.1.0, commit `f9e5205`), `specs/functional/bp02c-manage-resource-description-schemas.md`, and [`findings.md`](./findings.md). Nothing else.

Date: 2026-10-28.

---

## 1. Source hierarchy

When sources disagree, the higher rank wins.

| Rank | Source | Wins on | Why |
|---|---|---|---|
| 1 | This digest (decisions D1 and up) | Every point listed in section 4 | The specs leave these open or contradict each other. Both writers must resolve them the same way. |
| 2 | Acceptance criteria (Given/When/Then) of in-scope stories | Behaviour: what happens, which status code, which state change | They are the testable acceptance contract. Within the stories: an [ENHANCE] story wins over the story it revisits; SIMPL-33537 (the API implementation story for `schemaType`, updated 2026-10-27) wins on everything about `schemaType`; otherwise the most recently updated story wins. An API story wins over a UI story on API behaviour. |
| 3 | `schema_openapi.yaml` | Wire format: paths, parameter names, multipart field names, JSON field names, enum values, problem `type` URNs, and any status code that no story fixes | It is the canonical contract that code generation and tests read. It ranks below the stories on behaviour because it has drifted: it carries the out-of-scope `/vocabularies` paths, lacks `GET /schemas/{schemaName}`, and still restricts PATCH to `REVOKED`. |
| 4 | Technical notes inside stories (non-AC text) | Details the AC leaves open | Useful, but often written as hints ("Hints for testing", "IMPORTANT") rather than requirements, and they reference unpublished pages. |
| 5 | BP02C | Intent and vocabulary | High level; every requirement still has status *Proposed*. Used only where nothing above speaks. |

Examples in stories (hostnames such as `api.simpl.space`, values such as `data-schema` or `"1.0.0"`) are illustrations. They never override a field format defined by rank 3 or a decision.

---

## 2. Scope

### 2.1 In scope

The Schema Manager backend (REST API) and its UI (the schema pages of the Governance Authority portal), including:

- schema concepts with two types: `RESOURCE` (resource description schemas) and `SEARCH` (the federated search schema);
- versions with server-assigned numbers, publication status and the one-published-per-slot rule;
- syntactic, default-property and semantic validation of SHACL (Turtle) content;
- retrieval of metadata, version history and raw content;
- webhook subscriptions and webhook delivery for `SchemaPublished` and `SchemaRevoked`;
- e-mail notification to Providers for `RESOURCE` schemas;
- role-based access control (Tier 1) on all schema operations.

### 2.2 Story scope table

54 of 55 stories are in scope. Four of them are in scope only in part.

| Story | Topic | Scope | Note |
|---|---|---|---|
| SIMPL-13343 | List schemas (API) | In | Extended by SIMPL-32258 |
| SIMPL-13345 | Versions of a schema (API) | In | Extended by SIMPL-32786 |
| SIMPL-13346 | Versions of a schema (UI) | In | Extended by SIMPL-32787 |
| SIMPL-13347 | Latest content (API) | In | Extended by SIMPL-32788; endpoint missing from OpenAPI (D9) |
| SIMPL-13353 | Create schema (API) | In | Extended by SIMPL-32802; version part replaced by SIMPL-20528 |
| SIMPL-13355 | Create schema (UI) | In | Extended by SIMPL-32800 |
| SIMPL-13357 | New version (API) | In | Replaced in part by SIMPL-32807 and SIMPL-20534 |
| SIMPL-13358 | New version (UI) | In | Replaced in part by SIMPL-32808 and SIMPL-20770 |
| SIMPL-13360 | Revoke (API) | In | Generalised by SIMPL-20529 |
| SIMPL-13361 | Revoke (UI) | In | Overlaps SIMPL-20530 (D51) |
| SIMPL-13364 | Version content (API) | In | Revisited by SIMPL-32798 (D10) |
| SIMPL-13365 | Version download (UI) | In | Extended by SIMPL-32797 |
| SIMPL-14354 | Retain revoked (API) | In | D21 |
| SIMPL-14701 | Retain revoked (UI) | Partly | The resource description side lives in the catalogue. In scope: the revoked version URI stays resolvable (D21) |
| SIMPL-14703 | Syntax and default properties | In | D29 to D32 |
| SIMPL-14709 | Notify on new schema | In | Restricted to RESOURCE by SIMPL-33694 |
| SIMPL-14712 | Notify on new version | In | Restricted to RESOURCE by SIMPL-33694 |
| SIMPL-14791 | Notify on revocation | In | Restricted to RESOURCE by SIMPL-33694 |
| SIMPL-14795 | Syntax error message (API) | In | D33 |
| SIMPL-14796 | Missing default property message (API) | In | D33 |
| SIMPL-14797 | Semantic error message (API) | In | D33 |
| SIMPL-15837 | Schema list (UI) | In | Extended by SIMPL-32790 |
| SIMPL-17066 | Access control Tier 1 | In | D5 |
| SIMPL-19992 | Webhook subscriptions | In | D41 to D46 |
| SIMPL-20067 | Download latest (UI) | In | D56 |
| SIMPL-20528 | Initial version number | In | D14 |
| SIMPL-20529 | Change status (API) | In | D18 |
| SIMPL-20530 | Change status (UI) | In | D52 |
| SIMPL-20534 | Next version number | In | D14 |
| SIMPL-20686 | Status on create | In | Extended by SIMPL-32801 |
| SIMPL-20770 | No version field in UI | In | D55 |
| SIMPL-22407 | Semantic validation | In | D34 to D37 |
| SIMPL-24172 | Syntax error message (UI) | In | |
| SIMPL-24173 | Missing default property message (UI) | In | |
| SIMPL-24174 | Semantic error message (UI) | In | |
| SIMPL-24175 | Access control Tier 2 (webhooks) | Partly | Tier 2 credentials and the gateway are a deployment concern outside this run. In scope: webhook endpoints require an authenticated caller (D7) |
| SIMPL-26105 | UUID and Version default properties | In | D30 |
| SIMPL-28815 | Provider adds billing schema to a resource description | **Out** | Provider wizard for resource descriptions (contract clauses). Belongs to the resource description / catalogue UI, not to schema management. Its own story says the Provider is the actor and the step is "Contract Clauses in the wizard" |
| SIMPL-31098 | Publish federated search schema | In | Its technical design points to SIMPL-33537. Federated search itself is out; managing the SEARCH schema is in |
| SIMPL-32258 | List schemas with schemaType | In | |
| SIMPL-32436 | Retain revoked, RESOURCE only | Partly | "Federated search is disabled" happens in another component. In scope: a revoked SEARCH schema shows status REVOKED and no SEARCH schema is PUBLISHED (D21, D22) |
| SIMPL-32497 | Retain revoked (UI) | Partly | Same split as SIMPL-32436 |
| SIMPL-32786 to SIMPL-32808 | 11 [ENHANCE] stories | In | Section 3 |
| SIMPL-33537 | API for federated search schema | In | Rank 2 lead story for `schemaType` |
| SIMPL-33694 | Notify only for RESOURCE | In | D47 |

Federated search schema stories (SIMPL-31098, SIMPL-33537, SIMPL-32258 and the `SEARCH` paths of the [ENHANCE] stories): **in scope**. They describe this component's own API (a `schemaType` attribute, a one-published SEARCH slot). Only the consumer, federated search across data spaces, is another component.

### 2.3 OpenAPI operations

| Operation | Scope | Reason |
|---|---|---|
| `GET /schemas`, `POST /schemas` | In | Stories |
| `PATCH /schemas/{schemaName}` | In | Stories |
| `GET /schemas/{schemaName}/versions`, `POST /schemas/{schemaName}/versions` | In | Stories |
| `GET /schemas/{schemaName}/{version}` | In | Stories |
| `GET /schemas/{schemaName}` | In, **not in the OpenAPI** | Stories SIMPL-13347, SIMPL-32788, SIMPL-20067, SIMPL-33537 (D9) |
| `GET /webhooks`, `POST /webhooks`, `DELETE /webhooks/{webhookId}` | In | SIMPL-19992 |
| Six `/vocabularies` operations | Out | findings.md: the Schema Manager is a client of a separate Vocabulary Manager |
| `PATCH` and `DELETE /metadataQualityProfiles/{name}`, `GET /schemas/{schemaName}/qualityProfiles/active` | Out | No story in the input set, and the contract has no operation to create a profile, so the three operations cannot be exercised on their own. Finding F-MQP in D62 |
| 401, 406, 415 responses on all in-scope operations | In without a story for 406/415 | OpenAPI (406, 415) and stories (401) |

---

## 3. Enhancement stories

Eleven [ENHANCE] stories (all created 2026-09-08) revisit an earlier story to add `schemaType`. Common rule for all: an omitted `schemaType` means `RESOURCE` on every operation, and every existing RESOURCE behaviour keeps working (SIMPL-33537).

| Enhancement | Original | Relation | What the build follows |
|---|---|---|---|
| SIMPL-32786 versions list (API) | SIMPL-13345 | Extends | Original behaviour plus `schemaType` filter, default RESOURCE; `schemaType` in each item |
| SIMPL-32787 versions list (UI) | SIMPL-13346 | Extends | Original table plus SchemaType and ResourceType columns |
| SIMPL-32788 latest content (API) | SIMPL-13347 | Extends | Original behaviour plus `schemaType` query parameter (D9) |
| SIMPL-32790 schema list (UI) | SIMPL-15837 | Extends | Original plus SchemaType column and filter, default RESOURCE |
| SIMPL-32797 version download (UI) | SIMPL-13365 | Extends, small conflict | Download as in the original. The "selection box for schemaType" on the download page is replaced by the schema type of the page being viewed (D57) |
| SIMPL-32798 version details (API) | SIMPL-13364 | **Conflicts** | Original says the endpoint returns content, the enhancement says "schema meta datas". Build: content by default, metadata on `Accept: application/json` (D10) |
| SIMPL-32800 create schema (UI) | SIMPL-13355 | Extends | Original plus schemaType selector, resourceType disabled for SEARCH, three 409 paths. "409 CONFIG" is read as 409 Conflict |
| SIMPL-32801 status on create | SIMPL-20686 | Extends | Slot rule per resourceType for RESOURCE plus one global slot for SEARCH, 409 for a second published SEARCH (D16) |
| SIMPL-32802 create schema (API) | SIMPL-13353 | Extends and replaces | Replaces "1.0.0" by the server versioning algorithm (D14). Adds SEARCH paths. Its "same name and same resourceType" conflict rule conflicts with the original's "same name" rule: build follows D12 |
| SIMPL-32807 new version (API) | SIMPL-13357 | Replaces | Client-supplied version number and the "version already exists" 409 are gone (D14, D15). Adds `schemaType` and three 404 paths |
| SIMPL-32808 new version (UI) | SIMPL-13358 | Replaces | No version field (also SIMPL-20770). Adds schemaType. Its 409 example ("former version for the given schemaType does not exist") is a 404 per SIMPL-32807 |

Other stories that revisit earlier ones without the [ENHANCE] tag: SIMPL-32258 extends SIMPL-13343; SIMPL-26105 extends SIMPL-14703; SIMPL-20528 and SIMPL-20534 replace the version rules of SIMPL-13353 and SIMPL-13357; SIMPL-20770 replaces the version field of SIMPL-13358; SIMPL-20529 generalises SIMPL-13360; SIMPL-20530 overlaps SIMPL-13361; SIMPL-33694 narrows SIMPL-14709, SIMPL-14712 and SIMPL-14791; SIMPL-32436 and SIMPL-32497 narrow SIMPL-14354 and SIMPL-14701.

---

## 4. Decisions

Format: topic, what the sources say, decision for this run. "Finding" lines say what a spec owner could change.

### 4.1 API and access

**D1. Base path.**
Sources: SIMPL-13343, SIMPL-13353, SIMPL-32258, SIMPL-32802 use `/api/v1/schemas`. Other stories use `/schemas`. OpenAPI server URL ends in `/v1`, paths start at `/schemas`.
Decision: the API is served at `{SM_BASE_URL}/schemas...`. The reference value of `SM_BASE_URL` is `http://localhost:8080/v1` (OpenAPI wins on wire format). Tests never hard-code the prefix; they always build URLs from `SM_BASE_URL`.
Finding: align the stories on one prefix, matching the OpenAPI server entry.

**D2. Absolute URIs in headers.**
Sources: SIMPL-13347 wants absolute `Content-Location` URIs. OpenAPI `Location` examples are absolute but use a UUID path (`/schemas/{uuid}/version/1`) that matches no defined path.
Decision: `Location` and `Content-Location` are absolute, built as `{SM_PUBLIC_BASE_URL}/schemas/...` (config, defaults to `SM_BASE_URL`). Tests assert only the path suffix after the prefix. Location after create schema: `/schemas/{name}`; after new version: `/schemas/{name}/{version}`. For a SEARCH schema the query `?schemaType=SEARCH` is appended (D11).
Finding: replace the UUID-based Location example with the real path.

**D3. Authentication.**
Sources: SIMPL-13343 and SIMPL-32258 require 401 without a valid token. OpenAPI declares 401 only on the out-of-scope quality profile operations.
Decision: every in-scope operation returns 401 with `application/problem+json` (`type: urn:problem-type:simpl:unauthorized`) when the bearer token is missing, malformed, expired or not signed by the configured realm.
Finding: add 401 to every operation in the OpenAPI.

**D4. Role names.**
Sources: most stories and the OpenAPI use `GA_SCHEMA_ADMIN` and `GA_SCHEMA_VIEWER`. SIMPL-13343 (one negative scenario), SIMPL-13358 and SIMPL-32808 use `governance-admin` and `governance-viewer`.
Decision: roles are `GA_SCHEMA_ADMIN` and `GA_SCHEMA_VIEWER`, read from the top-level `roles` array claim of the access token. `GA_SCHEMA_ADMIN` implies every viewer right (SIMPL-17066).
Finding: replace the `governance-*` names in three stories.

**D5. Access matrix.**
Sources: SIMPL-17066 lists admin and viewer actions. Content retrieval stories (SIMPL-13347, SIMPL-13364) say only "authenticated GA user".
Decision:

| Operation | Allowed roles | Otherwise |
|---|---|---|
| `GET /schemas`, `GET /schemas/{name}`, `GET /schemas/{name}/versions`, `GET /schemas/{name}/{version}` | GA_SCHEMA_VIEWER or GA_SCHEMA_ADMIN | 403 |
| `POST /schemas`, `POST /schemas/{name}/versions`, `PATCH /schemas/{name}` | GA_SCHEMA_ADMIN | 403 |
| `GET`, `POST /webhooks`, `DELETE /webhooks/{id}` | any authenticated caller | 401 only (D7) |

Authorisation is checked before any validation or existence check, so a viewer sending a malformed create gets 403, not 400.

**D6. "Public" latest-content URI.**
Sources: SIMPL-13347 calls the latest-content URI "stable, public, unversioned", but its AC starts with "I am an authenticated GA user".
Decision: authenticated, viewer or admin (D5). No anonymous access.
Finding: say whether "public" means unauthenticated (for example for catalogue resolvers) or only "stable".

**D7. Webhook endpoint access.**
Sources: SIMPL-24175: Tier 2 credentials via the Tier 2 gateway, no attribute check, "enables all participants". OpenAPI lists 403 on webhook operations.
Decision: webhook operations need a valid token from the test realm and no role. `ga-norole` may create, list and delete subscriptions. Tier 2 and the gateway are out of this run. 403 is never returned by these operations here.
Finding: describe how a Tier 2 caller is identified at the service (header, claim) so it can be tested without the gateway.

**D8. Content negotiation and media types.**
Sources: SIMPL-20529 wants 406 for an `Accept` other than JSON or problem JSON. OpenAPI lists 406 and 415 on most operations.
Decision:
- JSON operations: 406 when `Accept` is present and admits neither `application/json`, `application/problem+json` nor `*/*`.
- Content operations (D9, D10): `text/turtle` and `*/*` give content; `application/json` gives metadata (D10 only); anything else gives 406.
- `POST /schemas` and `POST /schemas/{name}/versions` require `multipart/form-data`, otherwise 415. `PATCH` and `POST /webhooks` require `application/json`, otherwise 415.
- All errors use `application/problem+json` with the OpenAPI `Problem` shape.

**D9. Latest content: `GET /schemas/{schemaName}`.**
Sources: SIMPL-13347, SIMPL-32788, SIMPL-20067, SIMPL-33537 (AC4 and the filter list) all use it. The OpenAPI has no GET on this path. SIMPL-31098's validation hint says this GET returns a JSON result set with a `status` field.
Decision: implement `GET /schemas/{schemaName}?schemaType=&resourceType=`. 200 with `Content-Type: text/turtle`, body is the stored content of the highest version, `Content-Location` is the absolute versioned URI (D2). Works for REVOKED concepts too (D21). 404 when no concept matches. Status is read from `GET /schemas` (list), not from this endpoint.
Finding: add the operation to the OpenAPI; correct the SIMPL-31098 hint.

**D10. Version-specific endpoint: content or metadata.**
Sources: SIMPL-13364 and SIMPL-33537 AC4: full raw content. SIMPL-32798: "schema meta datas". OpenAPI: `text/turtle` body plus a `Location` header.
Decision: `GET /schemas/{schemaName}/{version}` returns `text/turtle` content by default, with `Content-Location` (and `Location`, per OpenAPI) set to its own absolute URI. With `Accept: application/json` it returns that version's metadata item (same shape as one item of the versions list, D25). A version that is not a positive integer string, or does not exist, gives 404 (OpenAPI example uses `9.9.9`).
Finding: state in SIMPL-32798 that metadata is the JSON representation of the same resource.

**D11. Schema identity and name collisions.**
Sources: SIMPL-13353: one schema per name (409). SIMPL-32802: 409 when "same name and same resourceType". SIMPL-32798 hint: one name may exist three times for RESOURCE, once per resource type. OpenAPI (version GET): the same name may exist for RESOURCE and SEARCH. Content-Location is described as the named graph, keyed only by name and version.
Decision: a concept is identified by **(schemaType, name)**. A name is unique within a schema type, whatever the resourceType. The same name may exist once as RESOURCE and once as SEARCH. Every by-name operation selects the concept with `schemaType` (query on GET and PATCH, form field on POST version), default RESOURCE. If `resourceType` is also given for a RESOURCE schema and differs from the stored value, the result is 404. The SEARCH variant of a URI carries `?schemaType=SEARCH` so the two named graphs never collide.
Finding: this is the largest open point (Q1). Name the identity key, and if it includes resourceType, say how the by-name UI routes and URIs disambiguate.

**D12. Name rules and reuse.**
Sources: OpenAPI and SIMPL-13345: PascalCase, `^[A-Z][a-zA-Z0-9]*$`, 3 to 64 characters. OpenAPI examples ("Application Shape") and SIMPL-13346 ("Application resource schema") break the rule. SIMPL-32801 and SIMPL-32802 ask the owners to "define whether a new schema may reuse a revoked schema's name".
Decision: the pattern applies to `name` on create and to `{schemaName}` in every path (400 before any 404). Names are case-sensitive. A revoked schema keeps its name: creating a schema with a name that exists in the same schema type returns 409, revoked or not.
Finding: fix the examples; confirm the no-reuse rule.

**D13. resourceType values.**
Sources: UI stories: `data`, `infrastructure`, `application`. SIMPL-13343: `data-schema`. SIMPL-32802: `APP, INFRA, DATA`. OpenAPI: free string, PascalCase pattern, examples `Application`, `LegalPerson`, `Data Offering`. BP02C: three resource types.
Decision: `resourceType` is a free PascalCase string (OpenAPI pattern), not an enum. Lowercase values give 400. The UI offers `Data`, `Application`, `Infrastructure` (labels "Data", "Application", "Infrastructure"). For RESOURCE it is required on create (400 if missing). For SEARCH it must be absent on create and on every query (400, SIMPL-33537).
Finding: settle one value set, and whether it is closed.

### 4.2 Lifecycle and versioning

**D14. Version numbers.**
Sources: SIMPL-13353: first version "1.0.0". SIMPL-13357 and SIMPL-13358: client supplies a SemVer. SIMPL-13347 examples: "1.2.0". SIMPL-20528, SIMPL-20534, SIMPL-20770 and the OpenAPI: integers "1", "2", "3", server-assigned. SIMPL-20534 sketches a versioning library with `changeType` PATCH/MINOR/MAJOR returning "1.2.0".
Decision: versions are positive integers serialised as strings ("1", "2", ...). The first version is "1". Each new version is the highest existing number plus one. Clients cannot set a version: a `version` form field, if sent, is ignored. No SemVer and no `changeType`.
Finding: remove the SemVer examples and the `changeType` sketch, or reopen the integer rule.

**D15. 409 on version number.**
Sources: SIMPL-13357 and the OpenAPI: 409 when the version already exists. With D14 that case cannot happen.
Decision: `POST /schemas/{name}/versions` never returns 409. Tests do not cover the case.
Finding: drop the 409 from the new-version operation.

**D16. Status model and the slot rule.**
Sources: SIMPL-20686 and SIMPL-32801: on create, PUBLISHED if no PUBLISHED schema of the same resourceType (RESOURCE) or of type SEARCH exists, else REVOKED for RESOURCE and 409 for SEARCH. SIMPL-20529: only one PUBLISHED per resource type. SIMPL-20530: "a new version does not have its own status, it inherits the status of the schema concept". SIMPL-13357 and SIMPL-32807: after a new version, the previous version changes from PUBLISHED to REVOKED. BP02C.05: a new version is published and the previous revoked. SIMPL-33537 AC1: with no published SEARCH schema, a new SEARCH schema or a new version of one is published.
Decision:
- Status (`PUBLISHED` or `REVOKED`) belongs to the concept.
- A **slot** is the set of concepts that compete for publication: for RESOURCE, all RESOURCE concepts with the same resourceType; for SEARCH, all SEARCH concepts.
- Create: the new concept is PUBLISHED if no other concept in its slot is PUBLISHED. Otherwise RESOURCE gets REVOKED (201) and SEARCH gets 409 (D17).
- New version: if the concept is PUBLISHED it stays PUBLISHED. If it is REVOKED and no other concept in the slot is PUBLISHED, it becomes PUBLISHED. Otherwise it stays REVOKED (201).
- Version status is derived: the highest version has the concept's status, all lower versions are REVOKED. This satisfies both SIMPL-20530 and SIMPL-13357.
- All slot checks and transitions are atomic: two concurrent requests can never leave two PUBLISHED concepts in one slot.
Finding: state the status model explicitly, including a new version of a revoked schema.

**D17. Second SEARCH schema.**
Sources: SIMPL-31098, SIMPL-32801, SIMPL-32802, SIMPL-33537 AC2, SIMPL-32800: 409 when a PUBLISHED SEARCH schema exists. SIMPL-33537: a revoked one does not count.
Decision: `POST /schemas` with `schemaType=SEARCH` returns 409 when any SEARCH concept is PUBLISHED, with detail `A published federated search schema already exists.` A name clash inside SEARCH also gives 409 (D12), detail `A schema with that name already exists.` When both apply, the name clash is reported.

**D18. PATCH status transitions.**
Sources: SIMPL-13360 and OpenAPI `SchemaStatusUpdateRequest`: only `REVOKED`. SIMPL-20529 and SIMPL-20530: `REVOKED` and `PUBLISHED`, 409 if already in that state, 409 if another concept of the slot is PUBLISHED, 400 for other values. SIMPL-31098: PATCH to PUBLISHED returns 201.
Decision: `PATCH /schemas/{name}` accepts `{"status": "REVOKED"}` and `{"status": "PUBLISHED"}`. 200 with a `SchemaCategory` body on success (also for PUBLISHED; the 201 in SIMPL-31098 is not followed). 400 for any other value or a missing field. 409 when the concept is already in the requested state. 409 when publishing and another concept in the slot is PUBLISHED, detail `Another schema is in PUBLISHED state for the resource type.` (for SEARCH: `A published federated search schema already exists.`). 404 when no concept matches (D11).
Finding: widen the OpenAPI enum to PUBLISHED; correct the 201 in SIMPL-31098.

**D19. Initial changelog.**
Sources: SIMPL-13353: default changelog like "Initial version.". `CreateSchemaPayload` has no changelog field.
Decision: version 1 always gets changelog `Initial version.`

**D20. Required fields.**
Sources: OpenAPI `CreateSchemaPayload` requires `schemaFile`, `name`, `description`, `title`. SIMPL-31098 lists name, title and content only. `CreateNewSchemaVersionPayload` requires `schemaFile` and `changelog`.
Decision: follow the OpenAPI. Missing or blank required fields give 400. `resourceType` per D13. On a new version, `resourceType`, if sent, must equal the stored value, otherwise 400 (the type of a concept never changes).

**D21. Retention of revoked schemas.**
Sources: SIMPL-14354, SIMPL-14701, SIMPL-32436, SIMPL-32497, BP02C assumptions. SIMPL-32436 and SIMPL-32497: "no retaining functionality" for SEARCH. SIMPL-33537 AC5: after revocation the GET on the SEARCH schema shows it as revoked.
Decision: nothing is ever deleted. Revoked concepts and all their versions remain listed and retrievable through every GET, for both schema types. "No retaining for SEARCH" is read as: no consumer may rely on a revoked SEARCH schema; the API still shows it.
Finding: clarify "no retaining functionality" for SEARCH.

**D22. Federated search disabled after revocation.**
Sources: SIMPL-32436 and SIMPL-32497: revoking the SEARCH schema disables federated search.
Decision: out of this component. Observable here: after revocation `GET /schemas?schemaType=SEARCH&status=PUBLISHED` is empty and a `SchemaRevoked` webhook is delivered (D44).

**D23. Request processing order.**
Sources: not specified; stories test single failures.
Decision, for create and new version: 401, 403, 415, metadata and parameter validation (400), concept lookup (404, new version only), conflicts (409), then content validation (400, D29 to D36). The tests only combine failures in this order.

### 4.3 Listing, pagination and payloads

**D24. Schema list item.**
Sources: OpenAPI list item: name, description, title, resourceType, schemaType, version, changelog, status. SIMPL-15837 and SIMPL-32790 show Creation Date and Creator. `SchemaCategory` has `created` and `modified` but no creator.
Decision: one item per concept. Fields from the OpenAPI, where `version` and `changelog` are those of the highest version, plus three additive fields: `created` (concept creation, ISO 8601 UTC), `modified`, `createdBy` (the `preferred_username` claim of the creator, else `sub`). `resourceType` is `null` for SEARCH.
Finding: add creator and dates to the list item.

**D25. Version list item.**
Sources: OpenAPI references `./common/common-components.yaml#/components/schemas/GetSchemaVersionsResponsePayload`, a file that is not part of the published contract. Example items: name, graphName (null), version, changelog, created, schemaType. SIMPL-13346 and SIMPL-32787 show Status, Creator, ResourceType.
Decision: the versions response has the same paging envelope as the list (D26). Each item: `name`, `version`, `changelog`, `created`, `createdBy`, `schemaType`, `resourceType`, `status` (derived, D16), `graphName` (the absolute versioned URI, never null).
Finding: inline the referenced component; the input set cannot resolve it.

**D26. Pagination and order.**
Sources: SIMPL-13343: 20 per page, Belgif guidelines. OpenAPI: `page` from 1, `pageSize` default 20, envelope `page`, `totalPages`, `total`, `pageSize`, `next`, `metadata`. OpenAPI versions: "ordering is deterministic by version-subject URI, an empty page returns 404".
Decision: `page` minimum 1, `pageSize` 1 to 100; out of range or non-integer gives 400. `next` is a relative link starting with `/schemas`, keeping all filters, `null` on the last page. Schema list order: `name` ascending, then `schemaType`. Versions order: numeric version ascending (a lexical URI order would put "10" before "2"). Schema list: a page past the end returns 200 with an empty `metadata`. Versions list: a page past the end returns 404, as in the OpenAPI. No server-side sort parameter (UI sorts the loaded page, D53).

**D27. Filters.**
Sources: SIMPL-13343, SIMPL-32258, SIMPL-33537, OpenAPI.
Decision: `GET /schemas` filters `status`, `resourceType`, `schemaType` (default RESOURCE), combined with AND. Invalid enum value or pattern gives 400 with problem JSON. `resourceType` together with `schemaType=SEARCH` gives 400 on every operation that accepts both (SIMPL-33537 wins over the versions description in the OpenAPI, which says such a filter "will not match"). Unknown query parameters are ignored.

**D28. Create and new-version responses.**
Sources: OpenAPI `CreateSchemaResponsePayload` (schemaName, schemaVersion, validationWarnings) and `CreateNewVersionSchemaResponsePayload` (full metadata plus warnings). SIMPL-22407: "the API returns a success response with the created schema details".
Decision: follow the OpenAPI shapes. The create response also carries `status`, `schemaType` and `resourceType` (additive). `validationWarnings` is always present, possibly empty.

### 4.4 Validation

**D29. Multipart layout.**
Sources: SIMPL-13353 and SIMPL-32802: ".ttl file and JSON metadata as multipart". SIMPL-13357 and SIMPL-32807: a `metadata` part and a `file` part. OpenAPI: flat form fields `schemaFile`, `name`, `title`, `description`, `resourceType`, `schemaType` (create) and `schemaFile`, `changelog`, `resourceType`, `schemaType` (new version).
Decision: the OpenAPI flat form fields are the contract. The file part is `schemaFile`. Its filename and part content type are not checked; the bytes are parsed as Turtle (UTF-8).
Finding: align the stories with the flat field names.

**D30. Syntax and structure checks.**
Sources: SIMPL-14703, SIMPL-14795, SIMPL-13353 ("syntax and integrity"), BP02C.04.
Decision: the content must parse as Turtle and must contain at least one `sh:NodeShape`. Failure gives 400 with detail starting `Syntax Error: ` followed by the parser message or `no sh:NodeShape found`.

**D31. Default properties: which ones.**
Sources: SIMPL-14703 points to SIMPL-16971, SIMPL-26105 to SIMPL-26100; neither is in the input set. SIMPL-26105 names UUID and Version. BP02C lists contract clauses (SLA, licence, billing schema) as default properties imposed by Simpl-Open. SIMPL-33537: "default properties applicable to SEARCH", not listed.
Decision for this run:
- RESOURCE: the default property set is `simpl:uuid` and `simpl:version` with `@prefix simpl: <https://simpl-programme.ec.europa.eu/ns/default#>`. The set is configuration (`SM_DEFAULT_PROPERTIES`, comma-separated IRIs); the tests use the default above.
- SEARCH: no default properties are required.
Finding (Q2): publish the default property list with IRIs, per schema type.

**D32. Default properties: how they are checked.**
Sources: SIMPL-14703 and SIMPL-26105: "verifies the presence". BP02C.04: "Simpl-Open enriches the schema with default properties, then validates".
Decision: presence check, no enrichment. A RESOURCE schema passes when at least one `sh:NodeShape` has, for every default property IRI, a `sh:property` node whose `sh:path` is that IRI. Stored content is exactly the uploaded bytes. Failure gives 400 with detail starting `Default Properties Error: ` and one `issues` entry per missing property (`code: missingDefaultProperty`, `location` = the property IRI).
Finding: settle "enrich" versus "verify presence".

**D33. Error detail prefixes.**
Sources: SIMPL-14795 and SIMPL-24172: `Syntax Error: `. SIMPL-14796: `Mandatory properties error: `. SIMPL-24173: `Default Properties Error: `. SIMPL-14797 and SIMPL-24174: `Semantic Validation Error: `. OpenAPI example: `Syntax validation errors detected in provided file.`
Decision: three exact, case-sensitive prefixes: `Syntax Error: `, `Default Properties Error: `, `Semantic Validation Error: `. The newer SIMPL-24173 wins over SIMPL-14796 and matches the BP term "default properties". Stages run in the order syntax, default properties, semantic; the first failing stage stops processing.
Finding: align SIMPL-14796 and the OpenAPI example.

**D34. Semantic validation: which references are local.**
Sources: SIMPL-22407: validate only against self-managed vocabularies; warn for external vocabularies that cannot be resolved; return all errors. OpenAPI issue codes `localReferenceUnavailable` and `externalReferenceSkipped`, with a local namespace example `https://authority.example/vocabulary-manager/content/UnknownVocabulary`.
Decision: a namespace is **local** when it starts with `VOCABULARY_NAMESPACE_BASE` (config). The vocabulary name is the next path segment, up to `#` or `/`. A namespace is **well known** when it is RDF, RDFS, XSD, OWL, SHACL, Dublin Core terms, SKOS or the default property namespace (D31). Any other namespace is **external**.

**D35. Semantic validation: the checks.**
Sources: SIMPL-22407 lists existence of vocabularies and terms, allowed terms, relationships, dependencies and constraints, without a precise rule set.
Decision: the semantic stage checks exactly these, collecting all errors before answering:
1. Every local IRI used as object of `sh:path`, `sh:class`, `sh:targetClass` or `sh:datatype`, or as a member of an `sh:in` list: its vocabulary exists in the Vocabulary Manager. Else error `localReferenceUnavailable`.
2. Same IRIs: the term exists, meaning it appears as a subject in the vocabulary content. Else error `unknownVocabularyTerm`.
3. Every `sh:node` and `sh:qualifiedValueShape` object is a shape defined in the same document. Else error `unresolvedShapeReference` (the "relationships and dependencies" check).
4. Every prefix declared with an external namespace yields a warning `externalReferenceSkipped`, `location` `prefix '<p>' -> '<namespace>'`.
Errors give 400 with detail starting `Semantic Validation Error: ` and every error in `issues`. Each issue has `code`, `description` and `location`; `location` names the property path and the IRI involved. Warnings never block; they are returned in `validationWarnings`.
Finding (Q3): publish the semantic rule set and the issue code list.

**D36. Vocabulary Manager outage.**
Sources: silent.
Decision: if the Vocabulary Manager does not answer, answers 5xx or times out (5 s), the request fails with 500, detail `Semantic validation unavailable: vocabulary manager unreachable`, and nothing is stored. A document with no local references does not call the Vocabulary Manager.

**D37. Semantic validation on create.**
Sources: SIMPL-13353 and SIMPL-32802 notes: "we don't specifically handle semantic validation in this story". SIMPL-22407: semantic validation on create and on new version.
Decision: all three stages run on create and on new version, for both schema types.

### 4.5 Notifications and webhooks

**D38. Event bus.**
Sources: no story mentions Kafka or any bus. Notifications go by e-mail (SIMPL-14712, SIMPL-33694) and by webhooks (SIMPL-19992).
Decision: no Kafka in this run. Two observable channels: SMTP e-mail and HTTP webhooks.

**D39. Provider e-mail: recipients and transport.**
Sources: SIMPL-14712 and SIMPL-14791: "provide the email to which to send these notifications via a configuration key". SIMPL-33694: "notify all Providers". BP02C.06: "e.g. via email".
Decision: one e-mail per event, sent over SMTP (`SMTP_HOST`, `SMTP_PORT`, no auth in tests) from `NOTIFICATION_FROM` to every address in `PROVIDER_NOTIFICATION_EMAILS` (comma-separated). Sending is asynchronous and never fails the API call; a send failure is logged.
Finding: say who "all Providers" are and where their addresses come from.

**D40. Provider e-mail: when and what.**
Sources: SIMPL-14709 (name, title, version, description, resource type, links), SIMPL-14712 (description of change, identifier, version, timestamp, links), SIMPL-14791 (identifier, timestamp, follow-up actions), SIMPL-33694 (RESOURCE only, nothing on failure, nothing for SEARCH).
Decision, RESOURCE concepts only:

| Trigger | Subject (exact) |
|---|---|
| Create that results in PUBLISHED, or PATCH to PUBLISHED | `Schema published: {name} (version {version})` |
| New version that results in PUBLISHED | `Schema new version published: {name} (version {version})` |
| PATCH to REVOKED | `Schema revoked: {name}` |

Plain-text body, one `Label: value` line each: `Name`, `Title`, `Description`, `Resource type`, `Version`, `Change description` (the changelog), `Timestamp` (ISO 8601 UTC), `Instructions` (value of `PROVIDER_INSTRUCTIONS_URL`). No e-mail when: the request fails for any reason, the result is REVOKED on create or new version, the previous version is superseded by a new one, or the concept is SEARCH.

**D41. Webhook subscription contract.**
Sources: SIMPL-19992 and the OpenAPI.
Decision: `POST /webhooks` with `{"targetUrl", "events"}` gives 201 and a `Webhook` body with a server-generated UUID `webhookId`. `targetUrl` must be an absolute `http` or `https` URL with a host, else 400 (`url`, `not-a-url`, `http://`, `ftp://invalid.com` all fail). Missing or empty `events` gives 400. An event name outside `SchemaPublished`, `SchemaRevoked` gives **406** with detail `'<value>' is not a valid event type. Valid event types are: SchemaPublished, SchemaRevoked` (OpenAPI; the story only says "an error"). Same `targetUrl` and same event set as an existing subscription gives 409. `GET /webhooks` returns all subscriptions as a plain array (not paged, not filtered by caller). `DELETE /webhooks/{id}` gives 204, or 404 when unknown. An id that is not a UUID gives 404.
Finding: 406 for an unknown event value is unusual (400 is the usual code); confirm.

**D42. Webhook event triggers.**
Sources: SIMPL-19992 names two event types, nothing more.
Decision: `SchemaPublished` fires on every transition into PUBLISHED and on every new version that results in PUBLISHED (the same triggers as the "published" and "new version" e-mails, D40). `SchemaRevoked` fires on PATCH to REVOKED. Both schema types fire webhooks (SIMPL-33694 limits only Provider notifications; a federated search consumer needs SEARCH events). A superseded previous version does not fire `SchemaRevoked`.

**D43. Webhook payload.**
Sources: silent.
Decision: HTTP POST, `Content-Type: application/json`, header `X-Simpl-Event: <eventType>`, body:
```json
{
  "eventId": "<uuid>",
  "eventType": "SchemaPublished",
  "occurredAt": "2026-10-28T10:00:00Z",
  "schemaName": "DataAsset",
  "schemaType": "RESOURCE",
  "resourceType": "Data",
  "version": "2",
  "status": "PUBLISHED",
  "schemaUri": "<absolute versioned URI>"
}
```
`resourceType` is `null` for SEARCH.
Finding (Q5): publish the event payload and delivery rules.

**D44. Webhook delivery.**
Sources: silent.
Decision: asynchronous, after the change is committed. Every subscription whose `events` contains the event type gets one delivery. A non-2xx answer or a 5 s timeout is retried twice (after 1 s and 5 s), then dropped and logged. No signature. Tests wait up to 10 s for a delivery and 3 s to confirm the absence of one.

### 4.6 UI

**D45. UI identity and routes.**
Sources: stories speak of the "governance management portal" and name pages ("Schemas list", "Create New Schema", details page with versions). Figma designs are not in the input set.
Decision: a single-page application at `SM_UI_URL`, logging in through the realm (D59) with the public client `schema-ui` (authorization code with PKCE). Routes: `/schemas` (list, also the landing page), `/schemas/new`, `/schemas/{name}?schemaType=` (details with the versions table), `/schemas/{name}/versions/new?schemaType=`. Error texts shown to the user come from the problem `detail` unless a fixed text is given below.

**D46. Test hooks in the UI.**
Sources: silent.
Decision: these `data-testid` values are fixed: `schema-list-table`, `schema-row-{name}`, `filter-status`, `filter-resource-type`, `filter-schema-type`, `pagination-prev`, `pagination-next`, `empty-state`, `error-state`, `retry-button`, `create-schema-button`, `action-download-{name}`, `action-revoke-{name}`, `action-publish-{name}`, `action-new-version-{name}`, `confirm-dialog`, `confirm-button`, `cancel-button`, `notification-success`, `notification-error`, `field-name`, `field-title`, `field-description`, `field-schema-type`, `field-resource-type`, `field-file`, `field-changelog`, `submit-button`, `versions-table`, `version-row-{version}`, `action-download-version-{version}`. Column headers use the exact texts of D53.

**D47. Notifications only for RESOURCE.** See D40. SIMPL-33694 is followed as written.

**D48. Role-dependent UI.**
Sources: SIMPL-20530: only admins change status. Other stories are silent on hiding actions.
Decision: for a viewer, the create button, Revoke, Publish and New Version actions are not rendered. Downloads are shown to both roles. A user with neither role sees `You do not have permission to view this page.` in `error-state`, and no table.

**D49. Create Schema form.**
Sources: SIMPL-13355, SIMPL-32800.
Decision: fields name, title, description, schema type (select, `RESOURCE` default, options `RESOURCE` and `SEARCH`), resource type (select `Data`, `Application`, `Infrastructure`; required for RESOURCE; disabled and cleared for SEARCH), file. Inline validation: required fields, name pattern (D12). `submit-button` is disabled until the form is valid. On 201: success notification `Schema "{name}" created successfully.` and redirect to the details page. On error: stay on the form, show the problem `detail` in `notification-error`. Fixed texts for 409: `A schema with that name already exists.` (name clash) and `A published federated search schema already exists.` (SEARCH slot), picked by the API `detail` (D17).

**D50. Create New Version form.**
Sources: SIMPL-13358 (version field, SemVer), SIMPL-20770 (no version field), SIMPL-32808 (changelog, file, schemaType selector).
Decision: fields changelog and file. Schema type is shown, filled from the schema being viewed, and read-only (a selector would let a user target a different concept, D11). No version field. `submit-button` disabled while changelog is blank or no file is chosen; inline messages next to the fields. On 201: redirect to the details page with the new version visible (SIMPL-13358 and SIMPL-32808 win over SIMPL-20770's "list of schemas"). On error: stay on the form, `detail` at the top in `notification-error`.

**D51. Revoke from the list.**
Sources: SIMPL-13361: confirmation popup, success text `Schema "{schema_name}" revoked successfully.`. SIMPL-20530: no popup, success text `Schema state changed.`.
Decision: Revoke (only on PUBLISHED rows) opens `confirm-dialog`; Cancel closes it and sends nothing; Confirm sends the PATCH. Success: `Schema "{name}" revoked successfully.` and the row shows REVOKED after the list reloads. Error: `notification-error` with the problem `detail`; the row keeps its status.
Finding: align the success texts of SIMPL-13361 and SIMPL-20530.

**D52. Publish from the list.**
Sources: SIMPL-20530: success texts `Schema state changed.` (AC) and `Schema {schema_name} published successfully!` (technical details); the AC also says the status becomes REVOKED after publishing (a slip); 409 text `Another schema is in PUBLISHED state for the resource type.`
Decision: Publish (only on REVOKED rows), no popup. Success: `Schema {name} published successfully!`, row shows PUBLISHED. 409: `notification-error` with the problem `detail` (D18 gives the exact text). Other errors: `detail`, row stays REVOKED.

**D53. Schema list page.**
Sources: SIMPL-15837, SIMPL-32790.
Decision: columns, in order, with these header texts: `Schema Name`, `Title`, `Description`, `Schema Type`, `Resource Type`, `Latest Version`, `Status`, `Creation Date`, `Creator`, `Actions`. Filters: status, resource type, schema type (default `RESOURCE`; resource type disabled when `SEARCH`). Changing a filter reloads page 1 from the API with the matching query. Pagination controls use `page` and `pageSize=20`. Sorting by Schema Name and Title is done in the browser on the loaded page. Clicking a name opens the details page. Empty results: header row stays, `empty-state` shows `No schemas found.` without filters or `No schemas match the selected filters.` with any filter, and pagination is hidden. Load failure: `error-state` shows `Could not load schemas. Please try again later.` and `retry-button`.

**D54. Details page (versions).**
Sources: SIMPL-13346, SIMPL-32787.
Decision: header shows name, title, schema type, resource type and status. Table columns: `Version`, `Status`, `Schema Type`, `Resource Type`, `Creation Date`, `Creator`, `Latest Change`. Default order version descending; sortable by Version and Creation Date in the browser. A `New Version` button (admin only, D48) opens D50.

**D55. Version number field.** Removed from the UI, see D50 (SIMPL-20770).

**D56. Download latest from the list.**
Sources: SIMPL-20067: download is the first action, calls `GET /schemas/{name}`, error text `An error occurred while downloading the schema. Please update the page and try again.`
Decision: as written. Saved file name `{name}-{version}.ttl`, version taken from `Content-Location`. No success text is required here.

**D57. Download a version.**
Sources: SIMPL-13365 and SIMPL-32797: success message on the versions page; SIMPL-32797 adds a schemaType selection box.
Decision: each row has a download action calling `GET /schemas/{name}/{version}?schemaType=<page type>`, saved as `{name}-{version}.ttl`. Success: `Schema version {version} downloaded successfully.` Error: `An error occurred while downloading the schema. Please update the page and try again.` No selection box; the page's schema type is used.

### 4.7 Environment

**D58. Persistence.**
Sources: SIMPL-13353 and SIMPL-32802 notes mention Fuseki2 as the persistence layer and the versioned URI as named graph.
Decision: any durable store. The reference environment offers PostgreSQL; a triple store is optional. The stored content is returned byte-for-byte. Tests never reset the store, assume nothing about existing data and create everything they need (D61).

**D59. Identity provider for tests.**
Same convention as the first candidate. Keycloak, realm `simpl`.
- Public client `schema-tests`, direct access grants (password grant) enabled, for API tests.
- Public client `schema-ui`, standard flow with PKCE, redirect URI `{SM_UI_URL}/*`, web origin `{SM_UI_URL}`, for the UI.
- Realm roles `GA_SCHEMA_ADMIN` and `GA_SCHEMA_VIEWER`.
- Users, password `password`: `ga-admin` (GA_SCHEMA_ADMIN), `ga-viewer` (GA_SCHEMA_VIEWER), `ga-norole` (no schema role).
- A mapper on a default client scope puts realm roles in a top-level `roles` array claim of the access token for both clients.
- The service validates tokens against `{KEYCLOAK_URL}/realms/simpl` (issuer and JWKS) and reads only the `roles` claim. The audience is not checked.

**D60. Test doubles and configuration.**
- **Vocabulary Manager stub** (built with the tests, contract shared by both writers):
  - `GET {VOCABULARY_MANAGER_URL}/vocabularies/{name}/content`: 200 `text/turtle` with the current content, or 404. This is the only call the Schema Manager makes.
  - `PUT {VOCABULARY_MANAGER_URL}/vocabularies/{name}/content` (body `text/turtle`): seeds or replaces a vocabulary, 204. Test-only.
  - No authentication. The Schema Manager sends no token to it.
  - Local namespace of vocabulary `Foo`: `{VOCABULARY_NAMESPACE_BASE}Foo#` or `{VOCABULARY_NAMESPACE_BASE}Foo/`. Reference value of the base: `https://vocabulary.simpl.test/content/`.
- **Mail capture:** an SMTP server that exposes received messages over HTTP (reference: Mailpit, API at `MAIL_API_URL`). Tests filter messages by the unique schema name in the subject.
- **Webhook receiver:** a small HTTP server run by the tests that records every POST (headers and body) and lets the tests read them back. Its URL (`WEBHOOK_RECEIVER_URL`) must be reachable from the service. Each test subscribes with a unique path (for example `{WEBHOOK_RECEIVER_URL}/hooks/{uuid}`) and deletes the subscription at the end.
- **Configuration names** shared by service, UI and tests: `SM_BASE_URL`, `SM_PUBLIC_BASE_URL`, `SM_UI_URL`, `KEYCLOAK_URL`, `VOCABULARY_MANAGER_URL`, `VOCABULARY_NAMESPACE_BASE`, `SM_DEFAULT_PROPERTIES`, `SMTP_HOST`, `SMTP_PORT`, `NOTIFICATION_FROM`, `PROVIDER_NOTIFICATION_EMAILS`, `PROVIDER_INSTRUCTIONS_URL`, `MAIL_API_URL`, `WEBHOOK_RECEIVER_URL`, `CORS_ALLOWED_ORIGINS` (must include `SM_UI_URL`).

**D61. Test data conventions.**
Sources: silent. Names are never reusable (D12) and slots are global, so tests share state.
Decision:
- Schema names: `T` + an alphanumeric suffix (letters and digits only, total 3 to 64), unique per test run and per test.
- RESOURCE tests that create schemas use their own random resourceType (`Rt` + suffix, PascalCase), so each test owns its slots. Only UI tests use `Data`, `Application`, `Infrastructure`.
- Vocabularies seeded in the stub use unique names (`V` + suffix).
- SEARCH has one global slot. All tests that create, publish or revoke SEARCH schemas run serially, in one group. Before a SEARCH test needs a free slot, it revokes any PUBLISHED SEARCH concept (as `ga-admin`).
- Valid fixture: a Turtle file with one `sh:NodeShape` holding `sh:property` entries for `simpl:uuid` and `simpl:version` (D31) and no external prefixes.
- Assertions on 500 paths (SIMPL-13357, SIMPL-20528, SIMPL-20534, SIMPL-20529) are not written: a black-box test cannot force a server fault. D36 (Vocabulary Manager down) is the only 500 the tests trigger, by stopping or pointing away from the stub only in a dedicated run.

**D62. Quality profile operations.**
Sources: three operations in the OpenAPI (tag "Quality profile"), no story, no create operation.
Decision: not built, not tested. Like the `/vocabularies` paths, they answer 404.
Finding (F-MQP): either add the stories and a create operation, or move these operations to the contract of the component that owns quality profiles.

**D63. Problem `type` values.**
Sources: OpenAPI uses both `urn:problem-type:iana:notFound` and `urn:problem-type:simpl:notFound`.
Decision: `urn:problem-type:simpl:` + `badRequest`, `unauthorized`, `forbidden`, `notFound`, `notAcceptable`, `conflict`, `unsupportedMediaType`, `internalServerError`. `status` in the body equals the HTTP status. Tests assert `status` and the detail prefixes and fixed texts given in this digest, not other `detail` wording.

---

## 5. Open questions for the spec owners

Ordered by impact on buildability.

1. **Schema identity (D11).** Is a schema identified by name, by name and schema type, or by name, schema type and resource type? Three sources say three things, and every endpoint, URI and UI route depends on it.
2. **Default property list (D31, D32).** Which IRIs are default properties, per schema type, and is the step "enrich" or "verify presence"? SIMPL-16971 and SIMPL-26100 are referenced but not published.
3. **Semantic validation rules (D34 to D36).** Which checks, which issue codes, how a local vocabulary namespace is recognised, and which Vocabulary Manager operation is called.
4. **Status model (D16, D18).** Status per concept or per version; what a new version of a revoked schema does; whether PATCH may set PUBLISHED (the OpenAPI says no, SIMPL-20529 says yes).
5. **Webhook events (D42 to D44).** Payload, triggers (does a new version fire `SchemaPublished`; do SEARCH changes fire), retries, signing, and Tier 2 identification.
6. **Missing and drifted contract parts (D9, D10, D25).** `GET /schemas/{schemaName}` is absent; the versions payload points to a file outside the published contract; SIMPL-32798 asks for metadata where the OpenAPI returns Turtle.
7. **Version format (D14).** Integers in the OpenAPI and three stories, SemVer in four others and in the versioning library sketch.
8. **Provider recipients (D39, D40).** Who receives the e-mail and what it must contain.
9. **Multipart layout (D29).** Flat form fields or `metadata` plus `file` parts.
10. **resourceType values (D13).** One value set, closed or open.
11. **Base path (D1)** and **Location header format (D2).**
12. **Error detail prefixes (D33)** and the 406 code for an unknown webhook event (D41).
13. **Name reuse after revocation (D12).** The stories ask the question themselves.
14. **UI texts (D51, D52)** and the "public" URI (D6).
15. **Quality profile operations (D62)**: owner and missing create operation.

---

## 6. Next steps

### Test writer

1. Build the environment pieces of D59 and D60: realm export, Vocabulary Manager stub, mail capture, webhook receiver.
2. Write API tests per story, following the decisions where a story is vague or superseded. Use D61 for names and slot isolation, and group SEARCH tests serially.
3. Cover per operation: success, 401, 403 per role (D5), 400 for each validation rule (D12, D13, D20, D26, D27, D30 to D35), 404, 409 (D16 to D18, D41), 406 and 415 (D8).
4. Cover the slot rule (D16) as a state table: create, new version and PATCH, for RESOURCE and SEARCH.
5. Observe side effects: e-mail subjects and body lines (D40) and webhook deliveries (D42 to D44), including their absence on failure, on REVOKED results and for SEARCH e-mail.
6. Write UI tests with the `data-testid` values and texts of D46 to D57, logging in as `ga-admin`, `ga-viewer` and `ga-norole`.
7. Map every test to its story key and decision number so a failure points back to a source.

### Code writer

1. Implement the in-scope operations of section 2.3 with the wire format of the OpenAPI as amended by D1 to D28; do not implement `/vocabularies` or quality profiles.
2. Implement the concept and version model and the atomic slot rule (D11, D14, D16 to D18).
3. Implement the validation pipeline in the order of D23 and D33, with a Vocabulary Manager client against the stub contract of D60.
4. Implement Provider e-mail (D39, D40) and webhook subscriptions and delivery (D41 to D44), both asynchronous after commit.
5. Read every setting from the configuration names in D60; validate tokens per D59.
6. Build the UI per D45 to D57.
7. Where this digest is still silent, choose the reading closest to the source hierarchy in section 1 and record the choice as a new finding.
