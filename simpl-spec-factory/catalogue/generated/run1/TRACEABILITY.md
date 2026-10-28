# Traceability

Test abbreviations (all JUnit unless stated):

| Short | Test |
|---|---|
| CIT | `backend/catalogue-be/src/test/.../CatalogueIT` (Spring Boot, H2, mocked quality stub, mocked JWT) |
| QIT | `backend/query-mapper-adapter/src/test/.../QmaIT` (mocked cat, Trustor and token endpoint) |
| BIT | `backend/catalogue-client-be/src/test/.../CbeIT` (mocked qma and stubs) |
| CT | `CryptoTest` (keys, JCS, signatures) |
| SQT | `ShaclAndQualityTest` (SHACL, JSON-LD, profile, thresholds, QAR) |
| AQT, APT, FCT, PQT | `AdvancedQueryTest`, `AccessPolicyTest`, `FederationConfigTest`, `PagingAndQualityParamsTest` (common) |
| SMOKE | `smoke.sh` against the running stack (not JUnit) |

The UI has no automated tests of its own in this build (frontend tests were optional). It was checked
by hand with a headless browser walkthrough during the build; that script is not part of the delivery.

## Stories (29 in scope)

| Story | Implemented in | Own tests |
|---|---|---|
| SIMPL-27998 quality gate | cat `service/QualityGate`, `quality/*`, `PublicationService` step 8 | SQT thresholds and QAR, CIT `qualityGateRejectsBelowThresholdAndStoresNothing` |
| SIMPL-29292 quick search quality filter (UI) | `ui/src/components/QualityFilters.vue`, `SearchView.vue`; backend filter in cat `SearchService.qualityFilter`, cbe `QualityLabels` | CIT (minimumClassification), BIT `validationAndRoles`, PQT |
| SIMPL-29293 advanced search quality filter (UI) | same components in the advanced tab; body fields in cbe `V1Controller.advanced` | BIT `advancedSearchSendsQualityOnlyToTheOwnCatalogue` |
| SIMPL-30848 Trustor search endpoint (partly) | static `env/federation/federation-a.json`, cbe `/v1/catalogues` | BIT `cataloguesAndSchemas`. Configuration API: not implemented (out, D46) |
| SIMPL-31102 catalogue selection, quick | `CatalogueSelector.vue` | none automated |
| SIMPL-31103 catalogue selection, advanced | `CatalogueSelector.vue` in the advanced tab | none automated |
| SIMPL-31106 Trustor provides results | qma `FederatedController.quick`, `.advanced` | QIT `trustorSideChecksTrustMapsAttributesAndEchoesTheCorrelationId` |
| SIMPL-31111 own quick search with policy | qma `LocalSearchController.quick`, cat `SearchService.quick`, `AccessPolicy` | QIT `localQuickSearch...`, CIT `publishSearchAndReadAnRd`, APT, SMOKE |
| SIMPL-31112 own advanced search with policy | qma `LocalSearchController.advanced`, cat `SearchService.advanced`, `AdvancedQuery` | QIT `localAdvancedSearch...`, CIT, AQT, SMOKE |
| SIMPL-31113 Trusted forwards search | qma `GovernanceAuthorityController.quick`, `.advanced`, `FederationClient` | QIT `governanceAuthoritySearch...`, SMOKE (federated) |
| SIMPL-31114 attribute mapping (search) | `FederationConfig.mapAttributes`, `FederatedController.inbound` | FCT, QIT trustor test |
| SIMPL-31138 Trustor quick search | qma `FederatedController.quick` (AC2: untrusted is 403, AC3: empty list) | QIT trustor test |
| SIMPL-31139 Trustor advanced search | qma `FederatedController.advanced` | covered by the same code path as 31138; no separate test |
| SIMPL-31140 consolidated grouped result | cbe `GroupedSearch` (6.2), qma `combine` | BIT `groupedSearchKeepsCataloguesApart...`, QIT, SMOKE |
| SIMPL-31141 overview of own results (UI) | `ResultsTabs.vue` | none automated |
| SIMPL-31142 overview of federated results (UI) | `ResultsTabs.vue`, per-tab paging in `SearchView.vue` | none automated |
| SIMPL-31143 detail of own RD (UI) | `DetailView.vue`, `flatten.ts` | none automated |
| SIMPL-31148 detail of federated RD (UI) | `DetailView.vue` (no schema, D42) | none automated |
| SIMPL-31149 forward detail request | qma `GovernanceAuthorityController.detail` (federated branch) | QIT `detailThroughTheGovernanceAuthority` |
| SIMPL-31150 Trustor handles detail request | qma `FederatedController.detail` | QIT trustor test (policy denied), QIT detail test |
| SIMPL-31151 attribute mapping (detail) | `FederatedController.inbound` | FCT |
| SIMPL-31152 Trustor provides detail | `FederatedController.detail` | QIT detail test |
| SIMPL-31153 Trusted forwards and receives detail | `GovernanceAuthorityController.detail`, `FederationClient.call(detail=true)` | QIT detail test |
| SIMPL-31767 overview via API, own | cbe `V1Controller.search` (own group), main properties `RdProjection` | BIT, CIT, SMOKE |
| SIMPL-31768 overview via API, federated | cbe federated groups | BIT, SMOKE |
| SIMPL-31769 detail via API, own, with quality | cbe `Detail`, qma detail own branch | BIT `detailIsTheVcPlusQuality...`, QIT |
| SIMPL-31770 detail via API, federated | cbe `V2Controller.detail`, qma federated branch | BIT, QIT |
| SIMPL-33885 detail with catalogue id | cbe `V2Controller.detail` (`catalogueId` required, pattern, `400`, `404`, `500`) | BIT |
| SIMPL-35874 SEARCH schema for federated advanced search | `AdvancedForm.vue`, cbe `/v2/schemas?schemaType=SEARCH` | BIT `cataloguesAndSchemas` (API part only) |

Out of scope (not implemented, by digest 3.1): SIMPL-31815, SIMPL-31817 (Schema Manager API),
SIMPL-31816, SIMPL-30850 (Schema Manager UI).

## BP05b steps

| Step | Implemented in | Own tests |
|---|---|---|
| 01 to 03 create, type metadata, policies | composed by the client (`tools/sign-rd.mjs`, test helpers) | SMOKE, CIT |
| 04 sign and submit | signature check `SignatureVerifier` | CT, CIT |
| 05 validate | `VcChecks` (syntax, JSON-LD, schema, SHACL), `QualityGate` | SQT, CIT `pipelineErrorsInTheDigestOrder` |
| 06 remediate | error Problems of D14 | CIT |
| 07 publish | `PublicationService` store | CIT, SMOKE |
| 08 to 10 new version | `PublicationService` version chain (D13) | CIT `newVersionRevokesThePreviousOne...` |
| 11 revoke previous version | same transaction | CIT |
| 12 inform about new version | not implemented (out, D20) | |
| 13 request revocation | cat `POST /{id}/revoke`, qma `PATCH /participants/...` | CIT |
| 14 revoke | `RdService.revoke` | CIT |
| 15 inform about revocation | not implemented (out, D20) | |
| 5B.1 provider consults own RDs | qma `ParticipantController` | QIT `participantOperationsAreOwnerOnly` |

## BP06 steps

| Step | Implemented in | Own tests |
|---|---|---|
| 01 search, scope, quality filter | UI `SearchView.vue`, cbe `V1Controller` | BIT, SMOKE |
| 02 access policy filter | cat `SearchService` with `AccessPolicy` | CIT, APT |
| 03 execute query | cat `SearchService.quick`, `.advanced` | CIT |
| 04 quality rating, own only | cat summary fields; cbe nulls federated quality | BIT, SMOKE |
| 05 forward to Trustors | qma `GovernanceAuthorityController`, `FederationClient` | QIT |
| 06 handle search from trusted data space, single hop | qma `FederatedController` (never forwards) | QIT |
| 07 policy filter with mapped attributes | `FederationConfig.mapAttributes`, cat with `User-Attributes` | QIT, FCT |
| 08 execute query for the trusted Consumer | cat-b through qma-b | SMOKE |
| 09 forward results | qma-b response | QIT |
| 10 receive results | qma-a `combine` | QIT |
| 11 collect and provide | cbe `GroupedSearch` | BIT, SMOKE |
| 12 DOME | not implemented (out, D83) | |

## Decisions

How each decision is built is in [docs/decisions.md](docs/decisions.md). Tests per decision:

| Decisions | Own tests |
|---|---|
| D1, D3, D8, D10, D12 | CIT (paths, Location, both POSTs, 501), BIT (v1/v2, 501) |
| D2, D9 | CIT (Problem content type), QIT (406, 415), all error assertions |
| D4, D69 | QIT `localQuickSearch...` (headers sent to cat), CIT (`User-Attributes` override) |
| D5, D58, D60, D61 | CIT `verificationSchemasWebhookAndOutOfScope`, SQT (shape detection), BIT (schemaType) |
| D6, D7 | QIT GA advanced path not tested separately; quick, federated and detail paths tested |
| D11, D22, D23, D24 | CT, SQT (`remoteContextsAreRefused`), CIT (tampered, unregistered, mismatch) |
| D13, D16, D17, D19 | CIT `newVersionRevokesThePreviousOneAndLifecycleIsOwnerOnly` |
| D14 | CIT `pipelineErrorsInTheDigestOrder` |
| D15, D20, D33, D45, D62, D76, D77, D83 | nothing to test (not built by decision) |
| D18 | CIT (ids and statuses filters) |
| D21 | QIT `participantOperationsAreOwnerOnly` |
| D25, D26 | CIT verification |
| D27, D28 | CIT quick search, QIT (`searchString` pattern) |
| D29 | AQT, CIT advanced search |
| D30 | APT, CIT (provider without CONSUMER sees nothing), QIT (policy denied) |
| D31 | PQT, QIT (`next`, `totalPages`) |
| D32, D34 | QIT local advanced, BIT (own only, `local`), CIT main properties |
| D35, D47, D68, D78, D79, D81 | SMOKE (whole environment) |
| D36, D37, D44, D48 | QIT trustor test, FCT |
| D38, D39, D40, D41 | BIT grouped search, QIT GA search |
| D42, D43, D57 | QIT detail, BIT detail |
| D46 | BIT catalogues |
| D49, D51, D53 | SQT |
| D50, D52 | CIT quality gate (below threshold, no profile) |
| D54 | CIT minimumClassification, BIT label validation, PQT |
| D55 | SQT invalid QAR |
| D56 | CIT recalc |
| D59, D63 | CIT schema version mismatch and SHACL violation, SQT |
| D64 to D67 | CIT and QIT role checks with mocked JWT claims; realm files checked by SMOKE |
| D70 to D75 | no automated UI test (manual headless walkthrough) |
| D80, D82 | SMOKE follows D80; D82 log lines not asserted, as decided |
