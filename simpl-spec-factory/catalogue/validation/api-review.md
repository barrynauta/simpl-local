# API review (step 21b): catalogue

Reviewer: fresh agent, 17 Nov 2026. Input: the five OpenAPI entry files under `specs/api/` (catalogue-be t1 and t2, query-mapper-adapter-be t1 and t2, catalogue-client-be v2), their references, `api-checks.json` (21a), `ac-findings.json` (20b), `domain-findings.json` (23b), `decisions.json` (step 30), `inventory.json`, `links.json` and `guidelines/rules.json`. Findings are in [`api-findings.json`](api-findings.json).

## Summary

23 findings: **3 blocker, 10 major, 10 minor**.

| Rule | Blocker | Major | Minor |
|---|---|---|---|
| COV-02 | 1 | | |
| API-60 | 1 | 1 | |
| BUILDER (no single rule) | 1 | 3 | 1 |
| SEC-07 | | 1 | |
| API-22 | | 1 | |
| API-45 | | 1 | |
| API-52 | | 1 | |
| COV-01 | | 1 | 1 |
| API-24 | | 1 | |
| API-06, API-14, API-15, API-49, API-21, API-03, API-28, API-54 | | | 8 |

The five findings with most impact:

1. **P-001 (blocker):** the Consumer-facing catalogue-client-be v2 has no search operation at all, so the UI has nothing to call for D24.
2. **P-002 (blocker):** nothing says how the Consumer's identity attributes become `User-Attributes` on tier 2 calls when no gateway is built, so policy filtering in an end-to-end search cannot be built the same way twice.
3. **P-003 (blocker):** the publication operation, the main path of the quality gate, has a `type: string` body with no fields for schema, version or owner, and a 201 body that differs from the decided "publication response".
4. **P-004 (major):** security schemes are broken or placeholders, and the Tier 1 operations lack the 401 and 403 that D20 decides.
5. **P-006 (major):** the contracts still accept and show the 0 to 100 score scale that D14 rejected.

## Blockers and their questions

**P-001: Consumer-facing search operations missing (catalogue-client-be).**
The v2 entry file has only schema operations and the detail. D24 sends the UI to catalogue-client-be, and D23 and D32 move two v1 operations into v2, but no decision moves or defines the search operations.
Question: which catalogue-client-be operations does the UI call for quick and advanced search, own-only and mixed, and do they return the D24 grouped response in both cases?
Options: take v1 `GET /selfDescriptions` and `POST /selfDescriptions/advanced` into v2 with `{groups}` always; the same with a flat list for own-only; or `POST /selfDescriptions` as SIMPL-31142 names it.

**P-002: Identity attributes on the search path.**
Tier 2 searches take an optional `User-Attributes` header. Tier 1 operations have none. D30 has the Trusted adapter forward `User-Attributes` without a source. D20 defines a missing header only for Trustor operations.
Question: which JWT claim carries the identity attribute codes, which service sets `User-Attributes` on tier 2 calls without a gateway, and what does a local search return when the header is missing?

**P-003: Publication request and response.**
`POST /self-descriptions/resource` takes `type: string` with no example. It validates "schema version, resource version number, and provider ownership" without naming a field. Its 201 is `SelfDescription`, while `SelfDescriptionPublicationResponse` (with `qualityAssessment`) belongs to an operation D4 does not build.
Question: what is the request (schema reference, resource description id and version number, provider identity), which checks give 400, 403, 409 and 422, and which schema does the 201 body use?

## Rule-by-rule review

Rules with check `review` that apply to `api-spec` (API, SEC, OWASP; no OWASP rule of this kind applies to `api-spec`). Verdicts: met, partly, not met, n/a.

| Rule | catalogue-be | query-mapper-adapter-be | catalogue-client-be (v2) |
|---|---|---|---|
| API-01 CDM per API | partly: CDM exists, alignment stale (D-012) | partly: CDM lacks GA and federated operations (D-012) | partly: CDM lists operations not in v1 or v2 (D-012) |
| API-02 reference the CDM | not met: QAR parts and search item redefined locally (D-007, D-011) | not met: `searchResponsePayload.n` local (D-011) | partly: untyped `object` bodies, nothing redefined, nothing referenced |
| API-03 hand-written first | met: README, code generated from root spec | partly: hand-edited, but "Generated server url" left (P-021) | partly: springdoc artefacts (P-021) |
| API-10 plural collections | partly: `/session`, `/self-descriptions/resource` | partly: `/participants/.../version` | met |
| API-11 method fits action | met for built operations; POST for advanced search is a justified read | met (PATCH revoke acceptable; POST advanced search justified) | met |
| API-15 American spelling | met for properties (`Unprocessible` is a component name, P-018) | not met: `catalogueIds` (P-018) | partly: parameter `catalogueId`; D23 and D24 add more (P-018) |
| API-19 business meaning | partly: `claimsGraphUri`, `sdHash`, node `n` (D-011) | partly: `n`, `claimsGraphUri` | met |
| API-22 realistic examples | partly: `simpl:format: test`, `contact: test@test.com`, `minimumScore: 75` (P-006, P-020) | partly: `catalogueId1`, `jws: illustrative-signature`, 75 and `good` | partly: 0 to 100 profile ranges, `SCHEMA_VERSION_ID`, TTL `@prefix ex:` |
| API-28 document, collection, controller | met | met | partly: `/schemas/{schemaId}/{version}` overlaps `/content` (P-022) |
| API-30 detail path | met: `/self-descriptions/{selfDescriptionId}` | met: `/federated/resourceDescriptions/{resourceDescriptionId}` | partly: schema detail only under `/content` and `/{version}` |
| API-49 business purpose | not met: t1 none, t2 generator remarks (P-019) | partly: t1 business, t2 one technical line | met: business context, actors, flow |
| API-50 own id is `id` | partly: `SelfDescription.id`, but search item uses `claimsGraphUri` array (P-023) | partly: `claimsGraphUri` | n/a in v2 (D23 `CatalogueInfo.id` will meet it) |
| API-51 relation names | partly: `issuer` fine, `validatorDids` names the identifier | n/a | n/a |
| API-54 identifier types | not met: no identifier schemas (P-023) | not met | not met |
| API-55 standard identifiers | met: DIDs as credential subject ids | met | met |
| API-58 enums | met: `type`, `QueryLanguage`, `SelfDescriptionStatus`; `minimumClassification` is profile-defined, no enum possible | met: `orderBy` | met: `resourceType` open by D12; D29 `schemaType` needs an enum when added |
| API-60 required flags | partly: `User-Attributes`, `Authorization` optional on operations that need them (P-002, P-004) | partly: local search `User-Attributes` optional; `catalogueIds` lacks minItems (P-007) | met |
| API-62 AsyncAPI for Kafka | n/a: no Kafka producer or consumer in the contracts; schema events arrive by HTTP webhook (not built, D4) | n/a | n/a |
| API-79 topic owner | n/a | n/a | n/a |
| SEC-07 Tier 1 OIDC via Keycloak | not met in t1 (undefined `OAuth2Auth`); n/a for tier 2 operations | partly: oauth2 with `auth.example.com` placeholders, scopes not roles (P-004) | partly: plain bearer JWT, no OIDC, no roles (P-004) |

Not met: API-02 (2 APIs), API-15 (1), API-49 (1), API-54 (3), SEC-07 (1, catalogue-be t1).

## Coverage

COV-01: 25 operations no criterion covers. Classes: **retire 18, internal plumbing 5, story missing 2**.

| API | Operation | Class | Basis |
|---|---|---|---|
| catalogue-be | GET /schemas | retire | legacy schema store, D4, D1 |
| catalogue-be | GET /schemas/{schemaId} | retire | D4 |
| catalogue-be | PUT /schemas/{schemaId} | retire | D4; no role (P-014) |
| catalogue-be | DELETE /schemas/{schemaId} | retire | D4; no role (P-014) |
| catalogue-be | GET /schemas/latest | retire | D4 |
| catalogue-be | POST /schemas/{schemaName}/recalcQualityScore | internal plumbing | triggered by profile changes, not built (D4, D-013) |
| catalogue-be | GET /verification | retire | legacy HTML page, D4 |
| catalogue-be | POST /verification | retire | D4 |
| catalogue-be | GET /session | retire | D4, CDM excludes sessions |
| catalogue-be | DELETE /session | retire | D4 |
| catalogue-be | POST /webhooks/schema-events | internal plumbing | Schema Manager callback, not built (D4); `security: []` (P-014) |
| catalogue-be | GET /self-descriptions | retire | legacy list, D4 |
| catalogue-be | POST /self-descriptions | retire | superseded by `/resource`, D4, D18 (no 202) |
| catalogue-be | GET /self-descriptions/{selfDescriptionId} | internal plumbing | backs the client own detail (D31) |
| catalogue-be | GET /self-descriptions/{selfDescriptionId}/versions | story missing | D7, D18 use it (P-012) |
| catalogue-be | DELETE /self-descriptions/{self_description_hash} | retire | D4, D18 |
| catalogue-be | POST /self-descriptions/{selfDescriptionId}/revoke | story missing | D18 lifecycle, BP05B revoke (P-012) |
| catalogue-be | GET /query | retire | D4 |
| catalogue-be | POST /query | retire | D4 |
| catalogue-client-be | GET /schemas/{schemaId}/content | internal plumbing | UI schema-driven rendering (D29); identifier open (P-011) |
| catalogue-client-be | GET /schemas/{schemaId}/{version} | internal plumbing | same (P-011) |
| query-mapper-adapter-be | GET /participants/resourceDescriptions | retire | Provider view, not built (D4) |
| query-mapper-adapter-be | GET /participants/resourceDescriptions/{resourceDescriptionId} | retire | D4 |
| query-mapper-adapter-be | PATCH /participants/resourceDescriptions/{resourceDescriptionId} | retire | D4; second revoke path |
| query-mapper-adapter-be | GET /participants/resourceDescriptions/{resourceDescriptionId}/version | retire | D4 |

COV-02: 3 operations a story names that no entry file has. **New 1, decided 1, false positive 1.**

| Story | Operation | Class |
|---|---|---|
| SIMPL-31142 | catalogue-client-be POST /selfDescriptions | new: P-001 (shape decided in D24, operation not defined) |
| SIMPL-31770 | xfsc-advsearch-be GET /selfDescriptions/{id} | false positive: exists as catalogue-client-be GET /selfDescriptions/{selfDescriptionId} (D7, D30) |
| SIMPL-31817 | POST /schemas/{schemaName}/versions | decided: out of scope, Schema Manager (F-011, D1) |

## 21a triage

22 findings in 131 places: **18 real, 4 harmless**.

| Id | Verdict | Where it went |
|---|---|---|
| A-001 | real | P-016 |
| A-002 | harmless | `@context` and `@graph` are JSON-LD keywords the standard requires |
| A-003 | harmless | `SchemaChangeEvent.data` is the usual event envelope member, and the webhook is not built (D4); listed in P-014 |
| A-004 | real | P-020, P-003, P-010; most of the 26 places are on operations D4 does not build |
| A-005 | real | P-016 |
| A-006 | real | P-013 |
| A-007 | real | P-004 |
| A-008 | real | P-014 (operation not built, but generated from the root) |
| A-009 | real | P-019 |
| A-010 | real | P-019 |
| A-011 | real | P-019 |
| A-012 | real | P-016 |
| A-013 | real | P-020, P-004 |
| A-014 | real | coverage table, P-012, P-014 |
| A-015 | real | P-017 |
| A-016 | real | P-016, P-011 |
| A-017 | real | coverage table (internal plumbing), P-011 |
| A-018 | harmless | no Kafka in the input set; schema events use an HTTP webhook (API-62 n/a) |
| A-019 | harmless | `security: []` on the federated detail is by design: the gateway does mutual TLS (D27) |
| A-020 | real | P-016 |
| A-021 | real | coverage table, P-014 |
| A-022 | real | 1 place new (P-001), 1 decided (D1), 1 false positive (SIMPL-31770) |

21a gap: API-45 was checked but not reported for catalogue-be, whose shared error responses are `application/json` with `Error`; this is P-009.
