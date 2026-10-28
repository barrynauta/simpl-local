# Spec digest: Simpl-Open Catalogue

Input set: `specs/` collected 2026-10-28 (33 stories, three OpenAPI folders, BP05b, BP06). This digest is the shared basis for the test writer and the code writer. Where the specs are silent or disagree, the decision here is binding for this run. Every decision is also a finding for the spec owners.

Notation: `cat` = catalogue-be (federated catalogue), `qma` = query mapper adapter, `cbe` = catalogue client backend (`xfsc-advsearch-be`), `ui` = search UI. Suffix `-a`, `-b`, `-c` names the data space of an instance.

---

## 1. Source hierarchy

The earlier rule (OpenAPI > stories > business process) holds only partly. The core of the catalogue has no stories, several new features have stories newer than the contract, and two OpenAPI roots of catalogue-be disagree. For this run:

1. **Composed OpenAPI roots** win on the wire: paths, parameter names, status codes, schemas. For catalogue-be that is `api/catalogue-be/fc_openapi.yaml` with its tier and `common/` files, not `fc_openapi-itb.yaml` (D1). For the adapter, `adapter_openapi.yaml` (1.2.0) plus the tier-1 advanced-search draft (D6). For cbe, both v1 and v2 (D8).
2. **Stories with Given/When/Then** win on behaviour the OpenAPI does not define: aggregation, grouping, partial failure, quality gating, UI behaviour. Struck text in a story (`-text-` or `{-}text{-}`) is deleted, not a requirement.
3. **Linkage stories** ("covered with the implementation of ... only for linkage") keep their acceptance criteria as requirements, because the implementing tickets are not in the input set.
4. **The `-itb` files** are used only for examples and for the inline review notes (`#!!`, section 4.1), never for path or parameter names.
5. **Business processes** BP05b and BP06 define scope and the core behaviour where neither the OpenAPI nor a story says anything (for example: revoking the previous version, single-hop federation).
6. **This digest** fills the rest. Where a story and the OpenAPI disagree on a wire detail, the OpenAPI wins and the row says so.

Public standards used as tie-breakers: W3C VC Data Model 1.1, JSON-LD 1.1, SHACL Core, RFC 9457 (problem details), RFC 8785 (JCS), RFC 8037 (EdDSA in JOSE), the did:jwk method.

---

## 2. Component map

```
  Browser (ui-a)            tests
      |                       |
      v                       v
   cbe-a (/v1, /v2) ----> qma-a (/v1) ----> cat-a (/)         data space A (Trusted GA)
      |                     |   \
      |  QAR only           |    \ federation token + User-Attributes + Correlation-Id
      +-----------------> cat-a   \
                                   +----> qma-b (/v1) ----> cat-b (/)   data space B (Trustor GA)
                                   +----> qma-c (/v1)                    data space C (does not trust A)
                                   +----> schema-stub /stub/slow         "slow" catalogue
   cbe-a ----> schema-stub (Schema Manager stand-in)
   cbe-a, cat-a, cat-b ----> quality-stub (Quality Scoring stand-in)
   all ----> keycloak (realms dataspace-a, dataspace-b, dataspace-c)
```

| Component | Owns | Calls |
|---|---|---|
| cat | Storage of resource descriptions (RDs), publication pipeline (syntax, signature, ownership, schema, SHACL, versioning, quality gate), revoke, delete, versions, QAR storage, local quick and advanced search with discovery-policy and quality filtering, validation shapes store, verification, schema webhook, recalc acknowledgement | quality-stub |
| qma | The Governance Authority (GA) search and lookup entry point. Local: quick and advanced search, participant RD operations. Trusted side: search and detail across selected catalogues, forwarding to Trustors. Trustor side: inbound federated search and detail, GA authentication, identity attribute mapping. The gateway duties (authentication of the calling GA, attribute mapping, discovery ABAC) are folded into qma (D36) | cat, other qma instances |
| cbe | Consumer-facing backend for the UI: grouped search over own and federated catalogues, catalogue list, detail, QAR, schema list and content, active quality profile | qma, cat (QAR only), schema-stub, quality-stub |
| ui | Quick search, advanced search, catalogue selection, quality filters, results per catalogue tab, detail page | cbe only (through its own same-origin proxy) |

Overlaps settled:
- `/selfDescriptions/advancedSearch` exists in cat and qma. cat executes the query on its store. qma is the GA entry point: it validates the request, resolves the caller's identity attributes, and calls cat. Nothing but qma calls cat search (D4).
- Schema paths exist in cat (`/schemas`, validation shapes) and cbe (`/schemas`, the Schema Manager view used to build the advanced form). They are separate stores loaded from the same seed files (D5, D58, D61).

---

## 3. Scope

### 3.1 Stories (33): 29 in, 4 out

| In | Notes |
|---|---|
| SIMPL-27998 | Quality gate at publication, with the quality stub |
| SIMPL-29292, SIMPL-29293 | Quality filters in quick and advanced search, UI and API |
| SIMPL-30848 | Partly: the Trustor search endpoint is static configuration (D46). The configuration API belongs to the authentication provider and is out |
| SIMPL-31102, SIMPL-31103 | Catalogue selection in the UI |
| SIMPL-31106, SIMPL-31111, SIMPL-31112, SIMPL-31113, SIMPL-31114, SIMPL-31138, SIMPL-31139 | Local and federated search, forwarding, mapping |
| SIMPL-31140, SIMPL-31141, SIMPL-31142 | Consolidated, grouped result; result tabs |
| SIMPL-31143, SIMPL-31148, SIMPL-31769, SIMPL-31770, SIMPL-33885 | Detail, own and federated |
| SIMPL-31149, SIMPL-31150, SIMPL-31151, SIMPL-31152, SIMPL-31153 | Federated detail, both sides |
| SIMPL-31767, SIMPL-31768 | Overview via API |
| SIMPL-35874 | Advanced form uses the SEARCH schema when a federated catalogue is selected |

| Out | Why |
|---|---|
| SIMPL-31815, SIMPL-31817 | Create and version a SEARCH schema: Schema Manager API. Only the result (a `schemaType` attribute, one SEARCH schema) is consumed, from the stub |
| SIMPL-31816, SIMPL-30850 | Schema management UI: Schema Manager UI |

### 3.2 Operations: 45 in the OpenAPI files, 37 in, 8 out, plus 3 defined here

catalogue-be (25 operations): 18 in.
- In: `GET/POST /self-descriptions`, `POST /self-descriptions/resource`, `GET /self-descriptions/{selfDescriptionId}`, `GET/POST /self-descriptions/{selfDescriptionId}/qualityAssessmentRecord`, `POST /self-descriptions/{selfDescriptionId}/revoke`, `GET /self-descriptions/{selfDescriptionId}/versions`, `DELETE /self-descriptions/{self_description_hash}`, `GET /selfDescriptions/quickSearch`, `POST /selfDescriptions/advancedSearch`, `GET/POST /schemas`, `GET/DELETE /schemas/{schemaId}`, `POST /schemas/{schemaName}/recalcQualityScore`, `POST /verification`, `POST /webhooks/schema-events`.
- Out (respond `501` with a Problem, D10): `GET/POST /query` (needs an openCypher engine; quick and advanced search are the query surface), `GET/DELETE /session` (review notes call it SSO-dependent), `GET /verification` (HTML page), `GET /schemas/latest` and `PUT /schemas/{schemaId}` (only SHACL shapes are stored in this run).

query-mapper-adapter (9 operations): 9 in, including the tier-1 draft `POST /governanceAuthority/selfDescriptions/advancedSearch` that the root does not compose (D6).

catalogue-client-be (11 operations, v1 and v2): 10 in. Out: `GET /contractClauses/documents/{documentId}` (contract component; `501`).

Defined in this digest because stories need them and no OpenAPI has them (D7, section 6): qma `GET /federated/selfDescriptions`, qma `POST /federated/selfDescriptions/advancedSearch`, qma `GET /governanceAuthority/resourceDescriptions/{resourceDescriptionId}`. Plus `GET /health` on every service.

### 3.3 Business process steps

- BP05b (15 steps): 13 in, 2 out. In through the API: 01 to 03 (the test composes the content), 04 (the test signs, cat verifies), 05, 06 (as error responses), 07, 08 to 10, 11, 13, 14. Out: 12 and 15 (inform Consumers; no consumer registry, belongs to contract and usage). The provider UI for 01 to 03 and 13 is SD tooling, out.
- BP06 (12 steps): 11 in (01 to 11), 1 out (12, DOME). HLRs 6.8 to 6.10 (DOME) out.
- Detailed requirements out: 6.1.2 "search all federated catalogues without selecting them" (D45), the sorting part of 6.7.1 and 6.7.2 (D33), 5B.6, 5B.9 notifications.

### 3.4 In scope without a story

The whole RD lifecycle in cat (publish, new version, revoke previous, revoke, delete, list, get, versions, verification, schema webhook, shape store, recalc acknowledgement, QAR store), local quick and advanced search in qma and cat, the participant operations in qma (5B.1), and the cbe schema and quality profile proxies.

### 3.5 Other components: stubs or static configuration

| Component | Treatment |
|---|---|
| Schema Manager | `schema-stub`, read-only, from seed files (section 6.4) |
| Quality Scoring service and MQP store | `quality-stub` with a control API (section 6.5) |
| Identity provider and IAA | Keycloak with fixed realms (section 5) |
| Authentication provider federation, IAA attribute mapping | Static JSON configuration per qma (section 6.3) |
| Tier 1 and Tier 2 gateways | None. Each backend validates JWTs and roles itself (D68) |
| DOME, contract and usage, signer and wallet | Out. Tests sign with fixed keys (D22, D23) |

---

## 4. Decisions

### 4.1 Inline review notes in the catalogue-be contract

The 13 `#!!` notes are in `fc_openapi-itb.yaml` only. Per note:

| # | Location | Note (short) | What this run takes |
|---|---|---|---|
| N1 | `VerificationResult.lifecycleStatus` | Not known for external RDs; answer: compute it | Keep the name `lifecycleStatus`, compute it (D25) |
| N2 | `VerificationResult.issuer` | Is this the participant? Answer: yes | `issuer` is the VC `issuer` DID |
| N3 | `VerificationResult.issuer` type comment | "NOT FURTHER SPECIFIED" | No lookup of an issuer RD. Plain VC issuer DID |
| N4 | `required: lifecycleStatus` | Remove from required? Answer: compute | Stays required |
| N5 | `required: issuer` | Required if only "if available"? Answer: required | Required. A VC without `issuer` is rejected with `400` |
| N6 | `Role` | Changed to a plain string | Roles are strings everywhere |
| N7 | `Session.status` | What is a status? | Session is out of scope |
| N8 | `Session.seed` (commented out) | Seed for pagination | No seed. Deterministic ordering instead (D31) |
| N9 | `SelfDescription.id` | Added id / credentialSubject | `id` = `credentialSubject.@id` (D11) |
| N10 | `GET /selfDescriptions` parameters | Filters for all metadata fields | Implement the filter set of the composed root, with its names (D18) |
| N11 | `/query` | Singular, one query | Path is `/query`; operation out of scope |
| N12 | `/session` | Singular | Out of scope |
| N13 | `DELETE /session` | Logout is SSO-dependent | Out of scope |

### 4.2 Component map and API

**D1 Two catalogue-be roots.** `fc_openapi.yaml` uses `/self-descriptions/...`, query names `issuers`, `validators`, `statuses`, `ids`, `hashes`, path `{self_description_hash}`, `/webhooks/schema-events`, server `.../fc`. `fc_openapi-itb.yaml` uses `/selfDescriptions/...`, `issuer`, `validatorDids`, `status`, `id`, `sdHash`, `{selfDescriptionHash}`, `/webhooks/schemaEvents`, server `.../fc/v1`. The layout README calls both "active root specs". Decision: the composed root wins on every path and name. Search paths stay `/selfDescriptions/quickSearch` and `/selfDescriptions/advancedSearch` (camel case in both). No aliases.

**D2 Error body format.** Composed `common` maps most responses to `Error {code, message}` with `application/json`; QAR responses, search 400s, the itb file and the adapter and cbe use RFC 9457 `Problem` with `application/problem+json`. Decision: every error response of every backend is a `Problem` with `application/problem+json`, with at least `type`, `title`, `status`, `detail`. Problem types are listed in section 6.6.

**D3 Base paths.** Servers: cat `.../fc` (gateway prefix), qma `/v1`, cbe `/v1` and `/v2`. Decision: cat serves at the root of its port (no `/fc`), qma under `/v1`, cbe under `/v1` and `/v2`. Every service also answers `GET /health` with `200 {"status":"UP"}` without authentication.

**D4 Who calls cat search.** BP06 dynamic view: the query mapper translates the query and adds access-policy filters; the catalogue executes. Both cat and qma expose quick and advanced search. Decision: qma validates and resolves the caller's identity attributes, then calls cat search with `User-Attributes`; cat applies the discovery-policy filter and the quality filters (D30, D54). cbe and the UI never call cat search directly.

**D5 Schema paths.** cat `/schemas` (shapes, roles `GA_SCHEMA_*`) and cbe `/schemas` (list with `resourceType`, content, versions) overlap in name only. Decision: cat `/schemas` is the store of SHACL shapes used for validation at publication. cbe `/schemas` proxies the Schema Manager stub. Both are loaded from the same seed files.

**D6 Tier-1 advanced search draft.** `t1-adapter-openapi.yaml` defines `POST /governanceAuthority/selfDescriptions/advancedSearch`; the README says it is not composed into the root, and its `operationId` `advancedSearchSelfDescriptions` collides with the tier-2 local operation. Stories SIMPL-31139, SIMPL-31113 need federated advanced search. Decision: in scope, contract as in the tier-1 file, `operationId` renamed `advancedSearchGovernanceAuthoritySelfDescriptions`.

**D7 Missing GA-to-GA and cbe-to-qma contracts.** SIMPL-31138 and SIMPL-31139 point to APIs (Trustor inbound quick and advanced search) that are not in the input set; the adapter README says delegation "depends on the separate SIMPL-31880 API". SIMPL-31153 asks for a cbe-to-Trusted-adapter detail request, not specified. Decision: three operations defined in section 6.1. They reuse the existing schemas (`searchResponsePayload`, `advancedSearchRequestPayload`, `federatedResourceDescriptionPayload`).

**D8 cbe v1 and v2.** v1 has search, QAR, catalogues, detail (without `catalogueId`), contract documents. v2 has schemas, quality profile, detail with required `catalogueId`. No search in v2. Decision: both versions served side by side. v1 detail means own catalogue; v2 detail routes by `catalogueId`. The UI uses v1 for search, catalogues and QAR, v2 for schemas, profile and detail.

**D9 Content negotiation.** 406 and 415 appear in many operations. Decision: if an `Accept` header is present and accepts neither the operation's media type nor `*/*`, answer `406`. If a request body has a `Content-Type` other than the listed ones, answer `415`. A missing `Accept` is fine.

**D10 Out-of-scope operations.** Decision: they exist and answer `501` with Problem type `urn:problem-type:simpl:notImplemented`. Tests only check that they are not `2xx`.

### 4.3 Resource description lifecycle

**D11 Identity.** The tags text says RDs are referenced by sha256 hash; paths use `selfDescriptionId`; N9 adds `id` = credentialSubject. Decision: `selfDescriptionId` = `credentialSubject.@id` (URL-encoded in paths). `sdHash` = lowercase hex SHA-256 of the RFC 8785 (JCS) serialisation of the full submitted VC including `proof`. `issuer` = VC issuer. `validatorDids` = `[]` always (D26).

**D12 Two create operations.** `POST /self-descriptions` ("Service-Offering", response `SelfDescriptionPublicationResponse` with `qualityAssessment`) and `POST /self-descriptions/resource` ("Resource or new version", response `SelfDescription`, 422 listed). Decision: one pipeline for both, same rules, same error codes. `POST /self-descriptions` returns `SelfDescriptionPublicationResponse`; `POST /self-descriptions/resource` returns `SelfDescription` and also includes `qualityAssessment` (extra member allowed). Both set `Location: /self-descriptions/{urlencoded id}` and include `resourceKey` and `currentVersion`.

**D13 Version model.** BP05b: RDs are immutable; a new version is created and the previous one is revoked (BP05B.11, 5B.5.10). `addResource` validates "resource version number". `SelfDescription` has `resourceKey` and `currentVersion`. No payload field for the version is specified. Decision: every version has its own `credentialSubject.@id`. `credentialSubject` carries `simpl:version` (integer, default 1 if absent) and, for version > 1, `simpl:previousVersion` `{"@id": "<id of the previous version>"}`. Rules: version 1 must not have `previousVersion`. Version n > 1 must point to the currently active version of a resource owned by the same participant, and n must be that version + 1. Otherwise `409` `versionConflict`. On success the previous version becomes `revoked` with a new `statusDatetime`. `resourceKey` = the id of version 1. `currentVersion` = the version number of that RD.

**D14 Publication pipeline order and codes.** Specs list 400, 401, 403, 409, 422, 500 without mapping. Decision, checked in this order, first failure wins:
1. Token and role (`401`, `403`).
2. Syntax: body is JSON, is a VC (`@context` first entry `https://www.w3.org/2018/credentials/v1`, `type` contains `VerifiableCredential`, `issuer`, `issuanceDate`, `credentialSubject.@id`, `proof` present), JSON-LD expands with bundled contexts only (D24). Else `400` `badRequest`.
3. Signature (D22, D23). Else `422` `invalidSignature`.
4. Ownership: `credentialSubject.simpl:providerInformation.simpl:providedBy` equals the token `participant_id`, and the signing key is registered for that participant. Else `403` `providerMismatch`.
5. Schema: `credentialSubject.dct:conformsTo.dct:schemaName` names a stored shape, its status is not `REVOKED`, and `dct:hasVersion` equals the stored version. Else `422` `schemaNotSupported`.
6. SHACL validation of the RD graph against that one shape graph only. Else `422` `shaclViolation`, `detail` lists the violated paths.
7. Duplicate `credentialSubject.@id`: `409` `alreadyExists`. Versioning (D13): `409` `versionConflict`.
8. Quality gate (D50 to D52).
9. Store, return `201`.

**D15 Asynchronous 202.** `POST /self-descriptions` lists `202` with `retry-after`. Decision: publication is synchronous; `202` is never returned.

**D16 Revoke.** cat `POST /{id}/revoke` (200 with `SelfDescription`); qma `PATCH /participants/resourceDescriptions/{id}` (204, idempotent). BP: revoked RDs stay inspectable for existing contracts. Decision: owner only (`403` otherwise), `404` if unknown. Idempotent in both: cat returns `200` with `status: revoked`, qma returns `204`. Revoked RDs remain readable by `GET /self-descriptions/{id}` and listable, are excluded from all searches and from Consumer detail (D43), and appear in version history.

**D17 Delete.** `DELETE /self-descriptions/{self_description_hash}`, publisher roles. Decision: owner only. Hard delete of the RD and its QAR. Works for active and revoked RDs. It does not reactivate a previous version. This is the test clean-up path.

**D18 List.** `GET /self-descriptions`: filters, `withMeta` (default true), `withContent` (default false), `offset`, `limit`. No default for `statuses`. Decision: default `statuses=active`. Time ranges are `start/end` ISO 8601, inclusive start, exclusive end; malformed is `400`. All array filters are comma-separated (`explode: false`), OR within a filter, AND across filters. `validators` matches nothing (D26). Order: `uploadDatetime` descending, then `id` ascending. Each `meta` also carries `qualityAssessment` (schema `SelfDescriptionQualityAssessment`) when the RD has a QAR. `content` is the stored VC as a JSON string. No discovery-policy filter on this internal operation.

**D19 Version history.** cat `GET /{id}/versions` "returns all versions except the requested"; response schema `string`, itb example uses the search payload. qma `GET /participants/resourceDescriptions/{id}/version` uses `versionHistoryResponsePayload` with `version`. Decision: cat returns `{"totalCount", "items": [{"n": {claimsGraphUri, offeringType, name, inLanguage, description, serviceAccessPoint, version, status}}]}` for all other versions of the same `resourceKey`, version descending, paged by `page` and `pageSize`. qma returns all versions including the requested one, same order. `404` if the id is unknown. qma: owner only (`403`).

**D20 Notifications.** BP05B.12, BP05B.15, 5B.6, 5B.9. Decision: out. The publication and revoke responses are the only "notification" to the Provider.

**D21 Participant operations in qma.** Owned list ordered by `orderBy` (`publicationDate` default, or `resourceType`), direction unspecified. Decision: `publicationDate` descending; `resourceType` ascending then `publicationDate` descending. Lists active and revoked RDs of the caller (`providedBy == participant_id`). Detail of an RD owned by someone else is `403`, unknown is `404`. Detail returns the stored VC.

### 4.4 Verification and signatures

**D22 Proof format.** The specs show `JsonWebSignature2020` proofs with a detached JWS header `{"alg":...,"b64":false,"crit":["b64"]}` and `did:jwk` or `did:web` verification methods. No canonicalisation is stated. Decision, so tests can sign without a wallet:
- `proof.type` `JsonWebSignature2020`, `proofPurpose` `assertionMethod`, `created` ISO 8601, `verificationMethod` = `<issuer DID>#0`.
- Key: Ed25519. `issuer` is a `did:jwk` (base64url of the UTF-8 JSON of `{"crv":"Ed25519","kty":"OKP","x":"..."}`).
- `proof.jws` = `base64url(header) + ".." + base64url(signature)`, header exactly `{"alg":"EdDSA","b64":false,"crit":["b64"]}`.
- Signing input = ASCII of `base64url(header)` + `.` + UTF-8 bytes of the JCS (RFC 8785) serialisation of the VC without its `proof` member.
- Verification: decode the did:jwk from `verificationMethod`, check it equals `issuer`, check the signature. Any change to the VC after signing must fail with `422`.
This is a simplification of Data Integrity canonicalisation. It is a finding.

**D23 Trust anchors.** Specs say the Provider signs with its credentials; ownership must match. No key registry is defined. Decision: each cat has a static participant key registry (`participant_id` to a list of Ed25519 public keys `x`). Keys come from fixed seeds: seed = SHA-256 of the ASCII string `simpl-catalogue-factory/<name>`.

| name | registered in | seed (hex) | public `x` (base64url) |
|---|---|---|---|
| provider-a | cat-a | 88351eb4a482417bdcfe19014adf65d5ceafc2bc7ee7f417ab00d54d862bd9c7 | MSC615MULPOxkuLnq2RlmNcxMU8fvDfq_Iy6vXVhEPY |
| provider-a2 | cat-a | ea61d9ebcb20fb085ed8c329bcf89fd5c7196b0a01f42f89118dd1bd60ef7f8c | fDG5arCWrkwpREKH_CfxIxNgyCM1FZ97GPkheLg35Ls |
| provider-b | cat-b | d9e7a79c454d1da78d3f5cae10d483c348fa805f9f02d69a1a9e98367d2894a4 | 7Hu7c6wpBAUBnCvhn_UVU9XTPQqPUuIDi0NdBLpi81U |
| unregistered | nowhere | 71873c3159a0a9227d881b26b70c0bf7738c07eb5e6f52f158b4b8ee61960583 | WtN5__UEhdz_e6ANP8dYx_lKAxxxEkynOtWbTFGczm8 |

Compare keys by `x`, not by the did:jwk string. A valid signature with a key not registered for the token's participant is `403` `providerMismatch`.

**D24 JSON-LD contexts.** Examples reference `https://www.w3.org/2018/credentials/v1` and `https://w3id.org/security/suites/jws-2020/v1`. Decision: cat bundles these two contexts and never fetches remote contexts. Any other remote context is `400`. `credentialSubject` carries an inline `@context` with the prefixes `simpl` (`http://w3id.org/gaia-x/simpl#`), `rdf`, `xsd`, `dct` (`http://purl.org/dc/terms/`).

**D25 POST /verification.** Flags `verifySemantics`, `verifySchema`, `verifyVPSignature`, `verifyVCSignature` (all default true). Decision: input is a VC (a VP is `400`). Runs steps 2, 3, 5, 6 of D14 as enabled by the flags (`verifySemantics` = JSON-LD expansion, `verifySchema` = steps 5 and 6, `verifyVCSignature` = step 3). `verifyVPSignature` is ignored. No ownership or quality check, nothing stored. Success: `200` `VerificationResult` with `verificationTimestamp`, `lifecycleStatus` (`active`, or `revoked` if an RD with the same id is stored as revoked), `issuer`, `issuedDateTime` (= `issuanceDate`), `validatorDids: []`. Failure: `400` for syntax, `422` for signature, schema or SHACL. Authenticated, no role.

**D26 Validators.** `validatorDids` and the `validators` filter exist, nothing defines validation by third parties. Decision: always empty.

### 4.5 Search

**D27 Quick search matching.** BP06 overview and 6.1.1: terms matched against any field. SIMPL-31111: at least one term found in at least one property. Decision: split the term on whitespace. An active RD matches if any term is a case-insensitive substring of any string literal value under `credentialSubject`, at any depth, excluding `@context`, `@type`, `rdf:type` and the `simpl:servicePolicy` subtree. Numbers are compared as their decimal text.

**D28 Parameter names.** cat `searchTerm` (required), qma `searchString` (required, pattern and length 1 to 255 in the contract), cbe `q` (required). Decision: each backend keeps its own name. qma enforces its pattern (`400`). cbe passes `q` on as `searchString` and propagates a `400`.

**D29 Advanced search body.** cat body is a JSON string; qma `advancedSearchRequestPayload`: root keys are "resource description node names", values are objects of property names and values; numeric ranges with `op1`, `value1`, `op2`, `value2`; root fields `catalogueIds`, `minimumScore`, `minimumClassification`. Examples use `simpl:GeneralServiceProperties` while RDs use the property `simpl:generalServiceProperties` with `rdf:type simpl:GeneralServiceProperties`. Decision:
- A root key (other than the three reserved ones) selects the nested node under `credentialSubject` whose `rdf:type.@id` equals the key (compact form, `simpl:` prefix).
- Inner keys are property local names (`name` matches `simpl:name`). Keys starting with `@` are ignored.
- String values: case-insensitive substring of the literal. Number or range: compare the numeric literal (`@value` if typed). Operators allowed: `=`, `>`, `>=`, `<`, `<=`. `op2` and `value2` are optional; `op1` and `value1` are required in a range object.
- All criteria are combined with AND. An RD without the node or property does not match.
- Errors (`400`): empty node object (detail as in the contract example), a range object with keys other than `op1`, `op2`, `value1`, `value2` (detail as in the contract example), unknown operator, non-numeric range value, body not a JSON object.
- cat receives the same body without `catalogueIds`.

**D30 Discovery access policy.** BP06.02, BP06.07, SIMPL-31111, SIMPL-31138: filter on RDs whose discovery access policies permit the Consumer. The only policy material is the ODRL JSON string in `simpl:servicePolicy.simpl:access-policy` with action `http://simpl.eu/odrl/actions/search` and `assignee.uid` such as `CONSUMER`. Decision: an RD is discoverable by a caller with attribute set S if its access policy parses as JSON and has at least one `permission` whose `action` list contains `http://simpl.eu/odrl/actions/search` and whose `assignee.uid` is in S. `constraint` is ignored. A missing or unparsable policy means not discoverable. The same rule applies in cat search and in qma detail. Owners get no exception in search.

**D31 Paging.** qma payload has `totalCount`, `page`, `pageSize`, `total`, `totalPages`, `next`; cat payload has only `totalCount`, `items`. Decision: `page` 1-based default 1, `pageSize` default 20, values above 100 capped to 100, `page` or `pageSize` below 1 or non-integer is `400`. qma: `totalCount == total`; `next` is a relative URL without `/v1` (as in the example), same query with `page+1`, or `null` on the last page. cat returns `totalCount` (all matches) and the page. Order of search results: `uploadDatetime` descending, then `id` ascending.

**D32 Catalogue ids in local search.** qma local `GET /selfDescriptions` has no `catalogueIds`; local advanced requires `catalogueIds` and says "if no ids, no catalogue will be searched"; cbe v1 `catalogueIds` optional. SIMPL-30848: one catalogue per data space. Decision: qma local advanced searches only if `catalogueIds` contains the own catalogue id, else `200` with an empty page. Other ids are ignored there. cbe: missing `catalogueIds` means own catalogue only; the reserved id `local` is an alias of the own catalogue id.

**D33 Sorting.** 6.7.1 and 6.7.2 ask for sorting on main and quality properties. No contract has a sort parameter. Decision: out; fixed order of D31.

**D34 Main properties.** SIMPL-31141, SIMPL-31767: "main properties". Decision: `claimsGraphUri` (`[credentialSubject.@id]`), and from `simpl:generalServiceProperties`: `offeringType`, `name`, `inLanguage`, `description`, `serviceAccessPoint` (the `@value` if typed), plus `overallScore` and `overallClassification` in cat and qma, `qualityScore`, `qualityClassification` and `catalogueId` in cbe (wrapper `i`, as in the cbe example).

### 4.6 Federated search across data spaces

**D35 Test topology.** Needs more than one data space. Decision:
- Data space A: Trusted GA. cat-a, qma-a, cbe-a, ui-a. Own catalogue `catalogue-a`.
- Data space B: Trustor GA that trusts A. cat-b, qma-b. Own catalogue `catalogue-b`.
- Data space C: qma-c only, trusts nobody. Catalogue `catalogue-c`. Never reaches a catalogue.
- `catalogue-slow`: listed by A as active, its URL points at `schema-stub` `/stub/slow`, which answers after 10 s.
- `catalogue-d`: listed by A with `active: false`.
Configuration in section 6.3. Tests seed A by publishing to cat-a with provider-a, and B by publishing to cat-b with provider-b.

**D36 GA-to-GA authentication.** SIMPL-31113, SIMPL-31150, SIMPL-31153: mutual TLS between adapters; adapter README: the gateway authenticates, maps attributes and enforces ABAC, and the federated detail operation has `security: []`. No gateway and no PKI are in the input set. Decision: no mTLS in this run. The Trusted qma gets an OAuth2 client-credentials token from its own realm (`qma-a-federation`) and sends it as `Authorization: Bearer`. The Trustor qma accepts a federated request only if the token is valid, its `iss` and `azp` match an entry of its `trustedAuthorities`. No or invalid token: `401`. Valid token of an untrusted GA (or a user token): `403`. The gateway duties run inside qma. This is a finding: the contract leaves authentication to a gateway that is not specified.

**D37 Attribute forwarding and mapping.** SIMPL-31113: include the Consumer's public identity attributes. SIMPL-31114, SIMPL-31138: map at the Trustor side; replace codes matching `remoteAuthorityAttribute` by `localAuthorityAttribute`, add `additionalIdentityAttributes`. The IAA interface is out. Decision: all values of the token claim `identity_attributes` are public. The Trusted qma sends them as `User-Attributes` (comma-separated, D69). The Trustor maps with its static `attributeMapping` for that caller; unmapped codes are dropped; `additionalIdentityAttributes` are added. The result is passed to the Trustor's cat as `User-Attributes`. Missing `User-Attributes` header: `400` (SIMPL-31150 AC2 "received without the identity attributes"). Header present but empty: allowed, the set is then only the additional attributes.

**D38 Consolidated, grouped result.** SIMPL-31140 and SIMPL-31142: one response, grouped per catalogue, each group naming its catalogue and GA, not merged, paging per group. cbe v1 response schema is `type: object` with a flat example (`items[].i.catalogueId`). Decision: cbe v1 `GET /selfDescriptions` and `POST /selfDescriptions/advanced` return the shape of section 6.2: the flat `totalCount` and `items` kept for compatibility, plus `groups`. `page` and `pageSize` apply per group. To page one group, the client repeats the request with `catalogueIds=<that id>` and the new `page`; other groups are not affected. Groups follow the order of `catalogueIds` as sent, own catalogue first when present.

**D39 Partial failure.** SIMPL-31113 AC2: reject the call to a Trustor that no longer trusts with `502`. SIMPL-31140 scenario 3: return an empty group for that catalogue, others unaffected. Decision: both hold at different levels. qma `GET /governanceAuthority/selfDescriptions` with a single federated id returns `502` (`federationUnauthenticated` when the Trustor answers 401 or 403; `federationUnavailable` for connection errors, 5xx, inactive catalogue; `504` `federationTimeout` after 3 s). cbe calls qma once per selected catalogue and turns any such error into a group with `status` `UNAUTHENTICATED` or `UNAVAILABLE`, empty `items`, `total: 0`, and an `error` object; the overall cbe response stays `200`. With several ids in one qma call, qma concatenates the pages of the reachable catalogues, sums `total`, skips failed ones and names them in the response header `Simpl-Failed-Catalogues` (comma-separated).

**D40 Unknown and inactive catalogue ids.** SIMPL-31113 AC3: a catalogue not in a federation is rejected with `502`. Tier-1 contract: invalid catalogue selection is `400`. Decision: the wire contract wins. An id not in the configuration (and not `local`) is `400` at qma and cbe. A configured id with `active: false` is not called and gives `502` `federationUnavailable` (cbe group `UNAVAILABLE`).

**D41 Quality in federated results.** SIMPL-31140 scenario 4 and BP06.04: quality only for own catalogues. Tier-1 quick search accepts `minimumScore` and `minimumClassification`. Decision: the Trustor returns `overallScore` and `overallClassification` as `null` on federated requests. The Trusted side does not forward quality filters. cbe applies quality filters only to the own-catalogue group; federated groups are unfiltered and carry `qualityScore: null`, `qualityClassification: null`.

**D42 Schema in the federated detail.** SIMPL-31148 (UI built from the schema returned by the backend), SIMPL-31770 AC1 (schema included) and SIMPL-33885 (details "together with the related schema") ask for a schema. SIMPL-31150 and SIMPL-31153 strike the schema; the adapter README and `federatedResourceDescriptionPayload` say "without a separate schema". Decision: no schema in any detail response. The UI renders federated details generically from the JSON-LD (D75). Strongest finding of the federated part.

**D43 Detail errors and visibility.** SIMPL-31150 lists positive, not-found, unauthorised, missing attributes, policy-denied, federation revoked, downstream error. SIMPL-31770 adds forbidden and timeout. SIMPL-33885: 400, 404, 500. Decision for `GET /governanceAuthority/resourceDescriptions/{id}` (qma) and cbe v2 detail:

| Case | qma (Trusted) | cbe |
|---|---|---|
| Missing or malformed `catalogueId` (pattern `^[A-Za-z0-9._-]{1,64}$`) or unknown id | 400 | 400 |
| RD unknown, or revoked (Consumers see active RDs only) | 404 | 404 |
| Discovery policy not satisfied (D30) | 403 `discoveryPolicyDenied` | 403 |
| Trustor rejects the GA (401 or 403 from Trustor) | 502 `federationUnauthenticated` | 502 |
| Trustor unreachable, 5xx, inactive catalogue | 502 `federationUnavailable` | 502 |
| Trustor slower than 3 s | 504 `federationTimeout` | 504 |
| Anything else | 500 | 500 |

The Trustor side (`GET /federated/resourceDescriptions/{id}`) answers 401 or 403 (D36), 400 (missing `User-Attributes` or `Correlation-Id`), 404, 403 `discoveryPolicyDenied`, 500. Trustor errors are passed on without internal details.

**D44 Single hop.** BP06.06: a Trustor never forwards. Decision: the `/federated/...` operations search and read only the own catalogue; `catalogueIds` other than the own id give an empty result.

**D45 Search all federated catalogues without selection.** 6.1.2 bullet two. No story, no contract. Decision: out. Selection is explicit.

**D46 Catalogue list.** cbe `GET /catalogues`: "all catalogues from any Trustor GA", items `id`, `name`, `authorityURL`, `active`. SIMPL-30848: configuration is covered by the authentication provider. Decision: cbe reads qma-a's federation configuration (same file, section 6.3) and lists `federatedCatalogues` in file order with paging. The own catalogue is not listed.

**D47 Timeouts.** Not specified. Decision: 3 s per Trusted-to-Trustor call; 10 s for any other backend call.

**D48 Correlation-Id.** Required header on the federated detail, echoed in the response header only. Decision: the Trusted qma generates a UUID v4 per forwarded request and sends it on all `/federated/...` calls; the Trustor echoes it as `Correlation-Id` in every response, including errors when it was sent.

### 4.7 Quality

**D49 Score scale.** cat and qma examples use 0.92; cbe uses 0 to 100 (`maximum: 100`, 87.5); the MQP and the stories use percent ranges (0 to 40 %, 70.0). Decision: 0 to 100 everywhere, as a number. Score from a QAR = 100 x `score` / `maxScore` of the node. `minimumScore` must be >= 0 (all) and <= 100 (cbe).

**D50 Quality Scoring stand-in.** SIMPL-27998: a synchronous call from publication to "the score-validation endpoint", returning a QAR; the remark allows a stub. No contract. Decision: `quality-stub` (section 6.5). cat calls `POST /v1/score-validation` with the VC, and `GET /v1/profiles/active?schemaName=<name>` for the Metadata Quality Profile (MQP).

**D51 Threshold evaluation.** SIMPL-27998 AC1, AC2: overall score >= overall threshold and each dimension score >= its threshold; error "Quality Score X is below treshold Y."; MQP example lists four thresholds but `includesThreshold` names two. Decision: apply the thresholds named in the profile's `includesThreshold`. `classificationScope: "overall"` compares the overall score; a scope `{"@id": <dimension>}` compares that dimension's score. Comparison is >=. Overall is checked first, then dimensions in `includesDimension` order; the first failure is reported. Rejection: `422` `qualityBelowThreshold`, `detail` exactly `Quality Score X is below treshold Y.` (spelling as in the story), numbers with at most 2 decimals and no trailing zeros (for example `65`, `72.5`). Nothing is stored. On pass the QAR is stored with the RD and the computed `qualityAssessment` is returned (rule `status` from `RuleEvaluation.result`, dimension `classification` from score ranges, `thresholdStatus` where a threshold applies).

**D52 Quality failure modes.** SIMPL-27998 AC3 and technical details: invalid QAR, endpoint failure, configuration error must give an explicit technical error, nothing published, reason logged. Decision: scoring endpoint error, timeout (10 s) or non-200: `502` `qualityAssessmentFailed`. QAR fails D55 structure checks: `502` `qualityAssessmentFailed`. No active profile (stub `404`) or a profile without applicable thresholds: `500` `qualityConfigurationError`. Each case writes one log line with the RD id and the reason. Tests do not assert log content (D82).

**D53 Classification.** SIMPL-29292 note: labels are dynamic per MQP; order by the lower bound of the score range. MQP example: `ScoreRange` with `minScore`, `maxScore`, `rangeClassification` IRI; the label in results is `Green`. Decision: label = local name of the `rangeClassification` IRI (after the last `/`, `#` or `:`). A score falls in the range with `minScore <= score < maxScore`; the range with the highest `maxScore` also includes its `maxScore`. Labels are compared case-insensitively. One profile per data space in this run (the stub serves the same default profile for every schema except the two test schemas of D58); cat uses the profile of the RD's schema at publication and the default profile to order labels for `minimumClassification`.

**D54 Quality filters.** cat, qma and cbe: `minimumScore`, `minimumClassification`, both optional; AND when both. SIMPL-29292 AC6 (exclude RDs without score) is struck. Decision: when any quality filter is set, RDs without a score are excluded (a null cannot satisfy >=). Unknown classification label: `400` (detail as in the contracts). Empty values mean not set. Filters apply only to own-catalogue results (D41).

**D55 QAR storage and retrieval.** `GET` returns the stored QAR, `204` if the RD exists without one; `POST` creates or replaces (roles N/A), `400` for an invalid graph. Decision: a valid QAR has a non-empty `@graph`, every node has `@id` and `@type`, exactly one node of type `QualityAssessmentRecord` with `computedOn` equal to the RD id, and exactly one `SelfDescriptionScore` with numeric `score` and `maxScore > 0`. `POST` replaces the QAR and recomputes the stored summary (`overallScore`, `overallClassification`, dimensions) without threshold gating; the RD stays published. `404` if the RD is unknown. `204` is unreachable in this run (every published RD has a QAR); tests do not cover it.

**D56 Recalculation.** `POST /schemas/{schemaName}/recalcQualityScore` acknowledges with `202`. Decision: acknowledge only, no rescoring. `estimatedResourceCount` = active RDs whose `dct:schemaName` equals `schemaName`. `400` for the name pattern, `404` `schemaNotFound` if cat has no such shape, `404` `activeQualityProfileNotFound` if the stub has no profile. Authenticated, no role.

**D57 Quality in the detail.** SIMPL-31143, SIMPL-31769: overall score and classification and per-dimension score and classification "returned via API". cbe detail examples return a bare VC. Decision: the cbe detail body is the stored VC with one extra top-level member `qualityAssessment` (schema `SelfDescriptionQualityAssessment` of cat) for own-catalogue RDs. Federated RDs have no such member. qma's detail returns `{resourceDescription, catalogueId, qualityAssessment?}` (section 6.1) and cbe flattens it.

### 4.8 Schemas

**D58 Shape store in cat.** `POST /schemas` accepts OWL, SHACL or SKOS; identifiers unspecified. Decision: only SHACL (Turtle or JSON-LD). `schemaId` = local name of the top-level `sh:NodeShape` whose `sh:targetClass` is `simpl:DataOffering`, `simpl:ApplicationOffering` or `simpl:InfrastructureOffering`; none found is `400`; an existing `schemaId` is `409`. New shapes get version `"1"` and status `PUBLISHED`. `GET /schemas` returns `{"ontologies": [], "shapes": [<schemaId>...], "vocabularies": []}`. `GET /schemas/{schemaId}` returns the Turtle. `DELETE` removes it (`404` if unknown). At startup cat loads all files of `seed/schemas/` and `seed/catalogue-only-schemas/` (section 6.4). Tests never delete seed shapes; they create their own with unique names.

**D59 RD to schema link.** BP: creation always uses the latest schema; `addResource` rejects unsupported schema versions; the federated example carries `dct:conformsTo` with `dct:schemaName` and `dct:hasVersion`. Decision: `credentialSubject.dct:conformsTo` is required with `dct:schemaName` and `dct:hasVersion` (string). Rules in D14 step 5. Validation uses only that shape graph.

**D60 Schema webhook.** `POST /webhooks/schema-events`, no security, `SchemaPublished` or `SchemaRevoked`. `data.schema.name` is optional in the schema. Decision: `data.schema.name` is required (`400` otherwise). `SchemaPublished` sets the stored shape of that name to `PUBLISHED` and, if `data.version.version` is present, its version. `SchemaRevoked` sets it to `REVOKED`; publications against it then fail with `422` `schemaNotSupported` (5B.5.2). An unknown name is accepted (`200`) and remembered, so a later `POST /schemas` of that name takes the remembered status. Content is never fetched from the event URIs.

**D61 Schema list for the UI.** SIMPL-35874: call the schema retrieval API with `schemaType=SEARCH`. cbe v2 `GET /schemas` has only `resourceType`. SIMPL-31815 and SIMPL-31817: one SEARCH schema, `resourceType` null. Decision: cbe v2 `GET /schemas` gains `schemaType` (`RESOURCE` or `SEARCH`, invalid is `400`). No `schemaType` means `RESOURCE` only (keeps current behaviour). Each item also has `schemaType`. Data come from schema-stub. `GET /schemas/{schemaId}/content` and `GET /schemas/{schemaId}/{version}` return Turtle from the stub. `GET /schemas/{schemaName}/qualityProfiles/active` returns the stub profile (`400` for the PascalCase pattern, `404` if none).

**D62 SEARCH schema management.** SIMPL-31815, SIMPL-31816, SIMPL-31817, SIMPL-30850 (uniqueness `409`, `resourceType` forbidden `400`, multipart versions, UI dialogs). Decision: out, Schema Manager. The stub serves exactly one SEARCH schema. Finding: SIMPL-31817 negative case 2 says `409 Config` with a `resourceType` set, which mixes the two negative cases.

**D63 SHACL dialect.** Decision: SHACL Core only, open shapes (no `sh:closed`). Datatypes from JSON-LD typed literals; JSON numbers map to `xsd:integer` or `xsd:double` as in JSON-LD 1.1, so shapes use `xsd:decimal` only for typed literals (`{"@type":"xsd:decimal","@value":...}`).

### 4.9 Access and identity

**D64 Identity provider contract.** Stories: "a role authorising searching", `GA_SCHEMA_ADMIN`, `governance-admin`. OpenAPI: ABAC and RBAC role names, OAuth2 authorization code with example URLs. Decision: Keycloak, realms, clients and users of section 5. All user passwords `Passw0rd!`.

**D65 Token claims.** Not specified. Decision: roles in `realm_access.roles`; `participant_id` (string) and `identity_attributes` (JSON array of strings) as top-level claims, from user attributes, on every client of the realm. Service-account tokens have no `participant_id`.

**D66 Roles per operation.** OpenAPI lists roles for cat; qma and cbe list none. Decision: table in section 5.4. Role checks are "any one of".

**D67 Roles "N/A".** cat `POST /schemas`, `DELETE /schemas/{id}`, recalc, `POST /verification`, `POST` QAR say "Required roles: N/A"; the webhook has `security: []`. Decision: "N/A" means a valid token of the service's realm, no role. The webhook needs no token. Finding: schema writes without a role are open to any authenticated user.

**D68 Token validation without gateways.** cat says it relies on the Tier 2 gateway. Decision: every backend validates the bearer JWT itself: signature from the realm JWKS, `exp`, and `iss`. Browser and tests reach Keycloak at `http://localhost:18080`, containers at `http://keycloak:8080`. Keycloak runs with hostname `http://localhost:18080`, so `iss` is always `http://localhost:18080/realms/<realm>`; containers fetch JWKS from `http://keycloak:8080/realms/<realm>/protocol/openid-connect/certs`. cat-a, qma-a, cbe-a accept realm `dataspace-a` only; cat-b and qma-b accept `dataspace-b` for local calls; `/federated/...` accept the issuers of their `trustedAuthorities`.

**D69 User-Attributes header.** Optional header on cat and qma search, required on the federated detail, format unspecified (example `CONSUMER`). Decision: comma-separated attribute codes, no spaces, case-sensitive. On cat: if the header is present it is the attribute set (trusted because cat is only reachable from qma in production; finding); if absent, cat uses the token's `identity_attributes`. qma local search ignores any incoming `User-Attributes` and always sends the caller's token attributes. A Trustor qma calls its cat with its own service token (`qma-b-service`) and the mapped attributes.

### 4.10 UI

**D70 Test selectors.** Not specified; needed so the two writers meet. Decision: the `data-testid` values of section 7. The UI is in English. Message texts are free unless stated.

**D71 Catalogue selection.** SIMPL-31102, SIMPL-31103: select one or more catalogues of all Trustor authorities; if the list API fails, the selector stays empty and an error is shown. Decision: the UI loads `GET /api/v1/catalogues` when a search tab opens. The selector shows an always-present own-catalogue option (`local`, checked by default) and one option per listed catalogue; options with `active: false` are shown disabled. On failure only the own option shows and `catalogue-error` is visible. At least one catalogue must be selected to search.

**D72 Quality filter controls.** SIMPL-29292, SIMPL-29293: controls in the Quick and Advanced tabs (not the header bar); AC6 and AC7 struck, but the technical details still ask for validation and inline errors. Decision: technical details apply. `min-score-input` accepts a number from 0 to 100; anything else shows `min-score-error` and disables `search-button`. `min-classification-select` options are the classification labels of the default profile (from `GET /api/v2/schemas/DataOfferingShape/qualityProfiles/active`), in ascending order, plus an empty option. Empty fields are not sent.

**D73 Results.** SIMPL-31141, SIMPL-31142: tabbed tables, one tab per catalogue, main properties. Decision: one tab per group in the response order, labelled with `catalogueName`. Each tab shows its rows, its own paging controls, and a status message instead of rows when the group is empty (`No results`) or not `OK` (a message naming the catalogue). Paging a tab re-requests only that catalogue and replaces only that tab. Own-catalogue rows show score and classification; federated rows do not. A row opens the detail page.

**D74 Advanced form.** SIMPL-35874: only the own catalogue selected, show the RESOURCE schema list and let the user pick; at least one federated catalogue selected, use the SEARCH schema without a choice. The form is "defined by the schema". Decision: the UI fetches the schema Turtle through cbe v2 and builds sections from the top-level shape's `sh:property` entries that have `sh:node`: one section per referenced node shape, keyed by that node shape's `sh:targetClass` (compact `simpl:` form), one field per `sh:property` path local name. Numeric datatypes (`xsd:decimal`, `xsd:integer`, `xsd:double`) get operator and value inputs (`op1`, `value1`, `op2`, `value2`), other datatypes a text input. Only filled fields are sent, using the body format of D29. Switching the catalogue selection rebuilds the form.

**D75 Detail page.** SIMPL-31143, SIMPL-31148: all published properties; for own RDs the overall and per-dimension quality; the federated case looks the same; errors show a message and no details. Decision: one detail page for both. It lists every property of `credentialSubject` as a path and value pair (nested objects flattened with `/`, for example `simpl:generalServiceProperties/simpl:name`), excluding `@context`. Own RDs also show overall score, overall classification and one line per dimension. On any error status `detail-error` is shown and no property rows. No schema is used (D42).

**D76 Schema management UI.** SIMPL-31816, SIMPL-30850. Decision: out (Schema Manager).

**D77 Provider UI.** BP05b UI for creation, versioning, revocation (SD tooling). Decision: out. Provider flows are API-only in this run.

### 4.11 Environment and testability

**D78 Ports and names.** Decision: section 5.1. Compose project name `simpl-catalogue-factory`. Every service listens on port 8000 inside its container. Only the listed host ports are published; databases have no host port.

**D79 Stubs.** Decision: `schema-stub` and `quality-stub` as in sections 6.4 and 6.5. They are part of the reference environment built by the code writer; tests use their control endpoints.

**D80 Test data.** Decision: each test makes its own RDs with `credentialSubject.@id` = `did:web:test.simpl.local:<Type>:<uuid4>` and puts a run token (`t` + 10 lowercase hex characters, fresh per test) in `simpl:name` and `simpl:description`, and searches by that token so parallel tests do not see each other. Clean-up: `DELETE /self-descriptions/{sdHash}` by the owner, also for revoked versions; test shapes by `DELETE /schemas/{schemaId}`; quality-stub entries by `DELETE /stub/scores/{id}`. Cannot be cleaned up: schema statuses remembered by the webhook for unknown names (use unique names), log lines, Keycloak users and clients (fixed), seed shapes and seed schemas (never touch). Tests must not depend on an empty catalogue.

**D81 Seed files.** Decision: content in section 6.4, mounted read-only into cat-a, cat-b and schema-stub.

**D82 Logging.** SIMPL-27998 AC3 "the failure reason is logged". Decision: implemented (D52), not asserted by tests. Finding: a log requirement is not checkable through the API.

**D83 DOME.** BP06.12, HLR 6.8 to 6.10, the DOME prerequisite. Decision: out. No DOME scope option in the UI.

---

## 5. Identity and environment contract

### 5.1 Services, host ports, names

| Service (compose name) | Host port | Internal URL | Notes |
|---|---|---|---|
| keycloak | 18080 | http://keycloak:8080 | `start-dev --import-realm`, hostname `http://localhost:18080` |
| cat-a | 18101 | http://cat-a:8000 | catalogue-be, data space A |
| qma-a | 18102 | http://qma-a:8000/v1 | adapter A (Trusted GA) |
| cbe-a | 18103 | http://cbe-a:8000/v1, /v2 | client backend A |
| ui-a | 18104 | http://ui-a:8000 | UI; proxies `/api/v1/*` and `/api/v2/*` to cbe-a |
| cat-b | 18201 | http://cat-b:8000 | catalogue-be, data space B |
| qma-b | 18202 | http://qma-b:8000/v1 | adapter B (Trustor GA) |
| qma-c | 18302 | http://qma-c:8000/v1 | adapter C, trusts nobody |
| schema-stub | 18401 | http://schema-stub:8000 | Schema Manager stand-in, `/stub/slow` |
| quality-stub | 18402 | http://quality-stub:8000 | Quality Scoring and MQP stand-in |

The stack is started with `docker compose -p simpl-catalogue-factory up -d --wait`; every service has a health check on `/health` (Keycloak on its own readiness endpoint). Tests read base URLs from environment variables with the host URLs above as defaults.

### 5.2 Realms and clients

| Realm | Client | Type | Secret | Use |
|---|---|---|---|---|
| dataspace-a | `catalogue-ui` | public, standard flow, PKCE S256, redirect `http://localhost:18104/*`, web origin `http://localhost:18104` | none | UI login |
| dataspace-a, dataspace-b, dataspace-c | `test-client` | public, direct access grants on | none | tests get user tokens by password grant |
| dataspace-a | `qma-a-federation` | confidential, service account | `qma-a-federation-secret` | qma-a calling Trustors |
| dataspace-b | `qma-b-service` | confidential, service account, realm role `CATALOGUE_SEARCHER` | `qma-b-service-secret` | qma-b calling cat-b |
| dataspace-c | `qma-c-federation` | confidential, service account | `qma-c-federation-secret` | tests: an untrusted GA calling qma-b |

Token endpoint (tests): `http://localhost:18080/realms/<realm>/protocol/openid-connect/token`. Access token lifespan 600 s. Realm roles: `CATALOGUE_SEARCHER`, `DATA_PROVIDER_PUBLISHER`, `APP_PROVIDER_PUBLISHER`, `INFRA_PROVIDER_PUBLISHER`, `GA_SCHEMA_VIEWER`, `GA_SCHEMA_ADMIN` in realms a and b. Claims per D65 through a default client scope.

### 5.3 Users (password `Passw0rd!` for all)

| Realm | User | Realm roles | `participant_id` | `identity_attributes` |
|---|---|---|---|---|
| dataspace-a | consumer-a | CATALOGUE_SEARCHER | consumer-a | CONSUMER, RESEARCHER |
| dataspace-a | provider-a | DATA_, APP_, INFRA_PROVIDER_PUBLISHER, CATALOGUE_SEARCHER | provider-a | PROVIDER |
| dataspace-a | provider-a2 | DATA_PROVIDER_PUBLISHER | provider-a2 | PROVIDER |
| dataspace-a | schema-viewer-a | GA_SCHEMA_VIEWER | schema-viewer-a | (none) |
| dataspace-a | schema-admin-a | GA_SCHEMA_ADMIN | schema-admin-a | (none) |
| dataspace-a | nobody-a | (none) | nobody-a | CONSUMER |
| dataspace-b | consumer-b | CATALOGUE_SEARCHER | consumer-b | CONSUMER |
| dataspace-b | provider-b | DATA_, APP_, INFRA_PROVIDER_PUBLISHER, CATALOGUE_SEARCHER | provider-b | PROVIDER |
| dataspace-b | schema-admin-b | GA_SCHEMA_ADMIN | schema-admin-b | (none) |

### 5.4 Roles per operation

"Searcher set" = any of `CATALOGUE_SEARCHER`, `DATA_PROVIDER_PUBLISHER`, `APP_PROVIDER_PUBLISHER`, `INFRA_PROVIDER_PUBLISHER`. "Publisher set" = any of the three publisher roles.

| Operations | Rule |
|---|---|
| cat search, `GET /self-descriptions`, `GET /{id}`, `GET /{id}/versions`, `GET` QAR | searcher set |
| cat `POST /self-descriptions`, `POST /resource`, revoke, delete | publisher set, plus ownership |
| cat `GET /schemas`, `GET /schemas/{id}` | `GA_SCHEMA_VIEWER` or `GA_SCHEMA_ADMIN` |
| cat `POST /schemas`, `DELETE /schemas/{id}`, recalc, `POST /verification`, `POST` QAR | valid token, no role (D67) |
| cat webhook | no token |
| qma local search, `/governanceAuthority/...` | searcher set |
| qma `/participants/...` | publisher set, plus ownership |
| qma `/federated/...` | federation token of a trusted GA (D36) |
| cbe, all operations | searcher set |
| schema-stub, quality-stub | no token |

Missing or invalid token: `401`. Valid token without the role: `403`.

---

## 6. Contracts defined here

### 6.1 New qma operations

All under `/v1`, responses `application/json`, errors Problem.

**`GET /federated/selfDescriptions`** (Trustor inbound quick search). Query: `catalogueIds` (required, repeated, as tier-1), `searchString` (as local), `page`, `pageSize`. Headers: `Authorization` (federation token), `User-Attributes` (required, may be empty), `Correlation-Id` (required). Response `200` `searchResponsePayload`, scores `null`. Errors `400`, `401`, `403`, `500`. Correlation-Id echoed.

**`POST /federated/selfDescriptions/advancedSearch`** (Trustor inbound advanced search). Body `advancedSearchRequestPayload` (quality fields ignored). Headers, paging and responses as above, plus `415`.

**`GET /governanceAuthority/resourceDescriptions/{resourceDescriptionId}`** (detail through the Consumer's GA). Query: `catalogueId` (required; own id, `local`, or a federated id). User token, searcher set. Own catalogue: read from cat, check status active and discovery policy (D30) with the token's attributes. Federated: call `GET /federated/resourceDescriptions/{id}` on the Trustor with federation token, `User-Attributes`, `Correlation-Id`. Response `200`:
```json
{ "catalogueId": "catalogue-a", "resourceDescription": { "...": "full VC" }, "qualityAssessment": { "rules": [], "dimensions": [], "overall": {} } }
```
`qualityAssessment` only for the own catalogue. Errors per D43.

The existing `GET /federated/resourceDescriptions/{resourceDescriptionId}` additionally requires the federation token (D36) and applies mapping and policy itself.

### 6.2 cbe grouped search response

For v1 `GET /selfDescriptions` and `POST /selfDescriptions/advanced` (`page` and `pageSize` per group):
```json
{
  "totalCount": 3,
  "items": [ { "i": { "claimsGraphUri": ["..."], "offeringType": "data", "name": "...", "description": "...",
                       "inLanguage": "en", "serviceAccessPoint": "...", "qualityScore": 87.5,
                       "qualityClassification": "Green", "catalogueId": "catalogue-a" } } ],
  "groups": [
    { "catalogueId": "catalogue-a", "catalogueName": "Data space A catalogue", "governanceAuthorityId": "ga-a",
      "authorityURL": null, "local": true, "status": "OK", "error": null,
      "page": 1, "pageSize": 20, "total": 3, "totalPages": 1, "items": [ { "i": { } } ] },
    { "catalogueId": "catalogue-c", "catalogueName": "Data space C catalogue", "governanceAuthorityId": "ga-c",
      "authorityURL": "http://qma-c:8000/v1", "local": false, "status": "UNAUTHENTICATED",
      "error": { "type": "urn:problem-type:simpl:catalogue:federationUnauthenticated", "detail": "..." },
      "page": 1, "pageSize": 20, "total": 0, "totalPages": 0, "items": [] }
  ]
}
```
`status`: `OK`, `UNAUTHENTICATED`, `UNAVAILABLE`. Top-level `totalCount` = sum of group `total`; top-level `items` = concatenation of the group pages. A `400` from validation (unknown id, bad `q`, bad quality values) fails the whole request.

### 6.3 Federation configuration (static files)

`env/federation/federation-a.json` (read by qma-a and cbe-a):
```json
{
  "gaId": "ga-a",
  "ownCatalogue": { "id": "catalogue-a", "name": "Data space A catalogue" },
  "outboundClient": { "tokenUrl": "http://keycloak:8080/realms/dataspace-a/protocol/openid-connect/token",
                      "clientId": "qma-a-federation", "clientSecret": "qma-a-federation-secret" },
  "federatedCatalogues": [
    { "id": "catalogue-b", "name": "Data space B catalogue", "gaId": "ga-b", "authorityURL": "http://qma-b:8000/v1", "active": true },
    { "id": "catalogue-c", "name": "Data space C catalogue", "gaId": "ga-c", "authorityURL": "http://qma-c:8000/v1", "active": true },
    { "id": "catalogue-slow", "name": "Slow catalogue", "gaId": "ga-slow", "authorityURL": "http://schema-stub:8000/stub/slow/v1", "active": true },
    { "id": "catalogue-d", "name": "Data space D catalogue", "gaId": "ga-d", "authorityURL": "http://qma-d.invalid:8000/v1", "active": false }
  ],
  "trustedAuthorities": []
}
```
`env/federation/federation-b.json` (qma-b): `gaId` `ga-b`, `ownCatalogue` `{"id":"catalogue-b","name":"Data space B catalogue"}`, `serviceClient` (`qma-b-service`), `federatedCatalogues: []`, and
```json
"trustedAuthorities": [
  { "gaId": "ga-a", "issuer": "http://localhost:18080/realms/dataspace-a", "clientId": "qma-a-federation",
    "attributeMapping": [ { "remoteAuthorityAttribute": "CONSUMER", "localAuthorityAttribute": "PARTNER_CONSUMER" } ],
    "additionalIdentityAttributes": [ "FEDERATED" ] }
]
```
So consumer-a (CONSUMER, RESEARCHER) is seen at B as {PARTNER_CONSUMER, FEDERATED}. `env/federation/federation-c.json` (qma-c): `gaId` `ga-c`, own catalogue `catalogue-c`, `trustedAuthorities: []`.

### 6.4 Seed schemas and schema-stub

`seed/schemas/` (loaded by cat-a, cat-b and schema-stub): `DataOfferingShape.ttl`, `ApplicationOfferingShape.ttl`, `InfrastructureOfferingShape.ttl`, `FederatedSearchShape.ttl`. `seed/catalogue-only-schemas/` (cat only): `NoThresholdShape.ttl`, `NoProfileShape.ttl` (copies of `DataOfferingShape.ttl` with the top-level shape renamed).

`DataOfferingShape.ttl`:
```turtle
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix simpl: <http://w3id.org/gaia-x/simpl#> .

simpl:DataOfferingShape a sh:NodeShape ;
  sh:targetClass simpl:DataOffering ;
  sh:property [ sh:path simpl:generalServiceProperties ; sh:node simpl:GeneralServicePropertiesShape ; sh:minCount 1 ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:providerInformation ; sh:node simpl:ProviderInformationShape ; sh:minCount 1 ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:offeringPrice ; sh:node simpl:OfferingPriceShape ; sh:maxCount 1 ] .

simpl:GeneralServicePropertiesShape a sh:NodeShape ;
  sh:targetClass simpl:GeneralServiceProperties ;
  sh:property [ sh:path simpl:name ; sh:datatype xsd:string ; sh:minCount 1 ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:description ; sh:datatype xsd:string ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:inLanguage ; sh:datatype xsd:string ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:offeringType ; sh:in ( "data" "application" "infrastructure" ) ; sh:minCount 1 ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:serviceAccessPoint ; sh:datatype xsd:anyURI ; sh:maxCount 1 ] .

simpl:ProviderInformationShape a sh:NodeShape ;
  sh:targetClass simpl:ProviderInformation ;
  sh:property [ sh:path simpl:providedBy ; sh:datatype xsd:string ; sh:minCount 1 ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:contact ; sh:datatype xsd:string ; sh:maxCount 1 ] .

simpl:OfferingPriceShape a sh:NodeShape ;
  sh:targetClass simpl:OfferingPrice ;
  sh:property [ sh:path simpl:price ; sh:datatype xsd:decimal ; sh:maxCount 1 ] ;
  sh:property [ sh:path simpl:currency ; sh:datatype xsd:string ; sh:maxCount 1 ] .
```
`ApplicationOfferingShape` and `InfrastructureOfferingShape`: the same file with the top-level shape renamed and `sh:targetClass` `simpl:ApplicationOffering` or `simpl:InfrastructureOffering`. `FederatedSearchShape`: top-level `simpl:FederatedSearchShape a sh:NodeShape` without target class, with the `generalServiceProperties` and `offeringPrice` properties only; its `GeneralServicePropertiesShape` keeps `name`, `description`, `offeringType`.

schema-stub endpoints (no auth):
- `GET /schemas?resourceType=&schemaType=` returns `{"schemas":[{"schemaId","name","title","description","resourceType","schemaType","version"}]}`. Seed entries: `DataOfferingShape` (DATA, RESOURCE), `ApplicationOfferingShape` (APPLICATION, RESOURCE), `InfrastructureOfferingShape` (INFRASTRUCTURE, RESOURCE), `FederatedSearchShape` (`resourceType` null, SEARCH). `schemaId` = `name` = file name without extension; `version` `"1"`; `title` = name with spaces before capitals.
- `GET /schemas/{schemaId}/content`, `GET /schemas/{schemaId}/{version}`: Turtle, `404` Problem if unknown.
- `ANY /stub/slow/...`: waits 10 s, then `503`.

### 6.5 quality-stub

- `POST /v1/score-validation`, body the VC (`application/json` or `application/ld+json`). Returns `200 application/ld+json` QAR whose nodes are: the RD node (`@id` = RD id), `MetadataQualityProfile` (`urn:simpl:mqp:default`, `profileVersion` `1.0.0`), three `QualityDimension` (`urn:simpl:quality:dimension:completeness`, `...:accuracy`, `...:validity`, `name` `Completeness`, `Accuracy`, `Validity`), three `DimensionScore` (`dimension`, `score`, `maxScore` 100), one `SelfDescriptionScore` (`computedOn`, `score`, `maxScore` 100, `dimensionScores`), one `RuleEvaluation` (`result` `passed`), one `QualityAssessmentRecord` (`computedOn`, `evaluatedAgainst`, `profileVersion`, `evaluationId`, `correlationId`, `assessmentDate`, `selfDescriptionScore`, `ruleEvaluations`). Default scores: overall 100, every dimension 100.
- `PUT /stub/scores/{urlencoded RD id}` with `{"overall": 65, "dimensions": {"Completeness": 95, "Accuracy": 80, "Validity": 70}}` sets the scores for that RD id; missing dimensions default to 100. With `{"mode": "invalid"}` the stub returns a QAR without a `QualityAssessmentRecord` node; `{"mode": "error"}` returns `500`; `{"mode": "timeout"}` waits 15 s. `DELETE /stub/scores/{id}` removes the entry.
- `GET /v1/profiles/active?schemaName=<name>`: `404` Problem for `NoProfileShape`; for `NoThresholdShape` the default profile with an empty `includesThreshold`; otherwise (and without `schemaName`) the default profile:
  - classifications `urn:simpl:quality:classification:Red`, `...:Yellow`, `...:Green`;
  - `ScoreRange` Red 0 to 60, Yellow 60 to 80, Green 80 to 100 (`minScore`, `maxScore` as plain numbers, `rangeClassification`);
  - `Threshold` `urn:simpl:quality:threshold:overall` 70 with `classificationScope` `"overall"`, and `urn:simpl:quality:threshold:completeness` 90 with scope `{"@id":"urn:simpl:quality:dimension:completeness"}`;
  - `includesDimension` the three dimensions, `includesThreshold` both thresholds.
  Parsers accept numbers either plain or as `{"@value": "...", "@type": "xsd:decimal"}`.

### 6.6 Problem types

`urn:problem-type:simpl:badRequest` (400 syntax), `urn:problem-type:simpl:catalogue:invalidInput` (400 parameters), `urn:problem-type:simpl:unauthorized` (401), `urn:problem-type:simpl:forbidden` (403 role), `urn:problem-type:simpl:resourceNotFound` (404 cat), `urn:problem-type:simpl:notFound` (404 qma, cbe), `urn:problem-type:simpl:notAcceptable` (406), `urn:problem-type:simpl:unsupportedMediaType` (415), `urn:problem-type:simpl:internalServerError` (500), `urn:problem-type:simpl:notImplemented` (501), and under `urn:problem-type:simpl:catalogue:`: `providerMismatch` (403), `discoveryPolicyDenied` (403), `alreadyExists` (409), `versionConflict` (409), `invalidSignature` (422), `schemaNotSupported` (422), `shaclViolation` (422), `qualityBelowThreshold` (422), `qualityConfigurationError` (500), `qualityAssessmentFailed` (502), `federationUnauthenticated` (502), `federationUnavailable` (502), `federationTimeout` (504), `schemaNotFound` (404), `activeQualityProfileNotFound` (404).

### 6.7 Minimal valid RD (before signing)

```json
{
  "@context": ["https://www.w3.org/2018/credentials/v1", "https://w3id.org/security/suites/jws-2020/v1"],
  "type": "VerifiableCredential",
  "issuer": "did:jwk:<provider key>",
  "issuanceDate": "2026-10-28T10:00:00Z",
  "credentialSubject": {
    "@context": { "simpl": "http://w3id.org/gaia-x/simpl#", "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                  "xsd": "http://www.w3.org/2001/XMLSchema#", "dct": "http://purl.org/dc/terms/" },
    "@id": "did:web:test.simpl.local:DataOffering:<uuid4>",
    "rdf:type": { "@id": "simpl:DataOffering" },
    "dct:conformsTo": { "@id": "urn:simpl:schema:DataOfferingShape:1", "dct:schemaName": "DataOfferingShape", "dct:hasVersion": "1" },
    "simpl:version": 1,
    "simpl:generalServiceProperties": { "rdf:type": { "@id": "simpl:GeneralServiceProperties" },
      "simpl:name": "<token> dataset", "simpl:description": "<token> description", "simpl:inLanguage": "en",
      "simpl:offeringType": "data",
      "simpl:serviceAccessPoint": { "@type": "xsd:anyURI", "@value": "https://provider.example.org/data" } },
    "simpl:providerInformation": { "rdf:type": { "@id": "simpl:ProviderInformation" },
      "simpl:providedBy": "provider-a", "simpl:contact": "provider-a@example.org" },
    "simpl:offeringPrice": { "rdf:type": { "@id": "simpl:OfferingPrice" },
      "simpl:price": { "@type": "xsd:decimal", "@value": 100 }, "simpl:currency": "EUR" },
    "simpl:servicePolicy": { "rdf:type": { "@id": "simpl:ServicePolicy" },
      "simpl:access-policy": "{\"@context\":\"http://www.w3.org/ns/odrl.jsonld\",\"@type\":\"Set\",\"uid\":\"<uuid4>\",\"permission\":[{\"action\":[\"http://simpl.eu/odrl/actions/search\"],\"assignee\":{\"uid\":\"CONSUMER\"},\"constraint\":[]}]}" }
  }
}
```
Then add `proof` per D22.

---

## 7. UI test selectors (`data-testid`)

Login: standard Keycloak form (fields `username`, `password`, button `kc-login`). App: `quick-search-tab`, `advanced-search-tab`, `search-input` (quick), `search-button`, `catalogue-option-local`, `catalogue-option-<catalogueId>` (checkboxes), `catalogue-error`, `min-score-input`, `min-score-error`, `min-classification-select`, `schema-select` (advanced, own catalogue only), `adv-field-<NodeLocalName>-<property>` for text fields (for example `adv-field-GeneralServiceProperties-name`), `adv-field-<NodeLocalName>-<property>-op1`, `-value1`, `-op2`, `-value2` for numeric fields, `results-tab-<catalogueId>`, `results-panel-<catalogueId>`, `result-row` (attribute `data-sd-id` = RD id), `result-score`, `result-classification`, `group-status-<catalogueId>`, `page-next-<catalogueId>`, `page-prev-<catalogueId>`, `page-indicator-<catalogueId>` (text `Page n of m`), `detail-property` (attributes `data-path`, text = value), `detail-quality-overall-score`, `detail-quality-overall-classification`, `detail-quality-dimension-<name>`, `detail-error`. The own catalogue tab uses `catalogueId` `catalogue-a`. UI tests may force the catalogue list failure by intercepting the browser request to `/api/v1/catalogues`.

---

## 8. Open questions for the spec owners (by impact on buildability)

1. **GA-to-GA contract.** The Trustor inbound search APIs (referenced by SIMPL-31138, SIMPL-31139, the adapter README) are not published; the authentication between GAs (mTLS through a gateway) and the gateway's mapping and ABAC duties have no contract. Without them, federated search cannot be built or tested end to end (D7, D36, D37).
2. **Signature profile and trust anchors.** Which canonicalisation and suite, which key types, how a catalogue knows a Provider's keys, and how the token participant is bound to the signer (D22, D23).
3. **Consolidated result shape and partial failure.** SIMPL-31140 groups per catalogue with independent paging; the cbe schema is an untyped object with a flat example. SIMPL-31113 says `502`, SIMPL-31140 says empty group (D38, D39, D40).
4. **Schema in the federated detail.** SIMPL-31148, SIMPL-31770 and SIMPL-33885 expect it; SIMPL-31150, SIMPL-31153 and the adapter contract exclude it (D42).
5. **Quality Scoring contract and scale.** The score-validation endpoint, the QAR content, the 0 to 1 versus 0 to 100 scale, and which MQP thresholds apply (`includesThreshold` versus all thresholds) (D49 to D53).
6. **Two catalogue-be roots.** Paths, parameter names, error bodies and server paths differ between `fc_openapi.yaml` and `fc_openapi-itb.yaml`, and 13 review notes remain in the contract text (D1, D2, section 4.1).
7. **Version fields in the RD payload.** How a new version names its predecessor and its number (D13).
8. **Discovery access policy rule.** Which ODRL subset, constraints, and the default for RDs without a policy (D30).
9. **Identity attributes.** Which token claim carries them, which are "public", and the source of the mapping (IAA) (D37, D65, D69).
10. **Advanced search mapping.** How request node names relate to RD properties, and how a SHACL shape drives the form when shapes are flat and RDs are nested (D29, D74).
11. **Roles.** "N/A" roles on write operations of cat; no roles at all on qma and cbe (D66, D67).
12. **Smaller gaps.** Default `statuses` on the list (D18), sorting (D33), "search all federated catalogues" (D45), linkage stories with their own criteria (section 1), log requirements (D82), the `409 Config` case of SIMPL-31817 (D62).

---

## 9. Next steps

**Both writers read:** this digest in full; the composed OpenAPI files (`api/catalogue-be/fc_openapi.yaml` with tier and common files, `api/query-mapper-adapter-be/adapter_openapi.yaml` with tier files including the tier-1 advanced draft, `api/catalogue-client-be/` v1 and v2); the 29 in-scope stories; BP05b and BP06 READMEs. Not the `-itb` files, except for examples.

**Test writer:** builds API tests per backend (cat, qma-a, qma-b, cbe-a), federation tests across A, B, C, slow and inactive catalogues, quality-gate tests driven through quality-stub, and UI tests on ui-a with the selectors of section 7. Signs RDs with the fixed keys (D22, D23). Follows D80 for isolation and clean-up.

**Code writer:** builds cat, qma, cbe, ui, both stubs, the Keycloak realm imports, the federation files and the seed files, wired into one compose project with the ports of section 5.1.

**Where they are most likely to diverge** (check these first):
1. The signing input and the did:jwk handling (D22).
2. The grouped cbe response and per-group paging (D38, 6.2).
3. Error status codes and Problem `type` values in the publication pipeline and the federated paths (D14, D39, D43, 6.6).
4. The score scale and the threshold message text (D49, D51).
5. Version fields and the revoke-previous behaviour (D13).
6. Token claims, `User-Attributes` format and the mapping at B (D65, D69, 6.3).
7. The discovery-policy rule, which applies in cat search and in qma detail (D30).
8. Advanced search key mapping and the form built from shapes (D29, D74).
9. Catalogue ids: `local`, unknown versus inactive (D32, D40).
10. UI selectors and when quality data is shown (section 7, D41, D57).
