# How each digest decision is implemented

Code paths are relative to `backend/` unless stated. `cat`, `qma`, `cbe` as in the digest.

## Inline review notes N1 to N13

| Note | Implementation |
|---|---|
| N1, N4 | `VerificationResult.lifecycleStatus` is computed: `revoked` if an RD with the same id is stored as revoked, else `active` (`VerificationController`) |
| N2, N3, N5 | `issuer` is the VC issuer DID (string or `{id}`); a VC without issuer is `400` (`VcChecks.structure`) |
| N6 | Roles are plain strings from `realm_access.roles` (`Caller`) |
| N7, N12, N13 | `/session` answers `501` (`OutOfScopeController`) |
| N8 | No seed; fixed ordering `uploadDatetime` desc, `id` asc (`RdRepository`) |
| N9 | `id` = `credentialSubject.@id` (`RdRecord.meta`) |
| N10 | Filter set and names of the composed root (`SelfDescriptionController.list`) |
| N11 | `/query` exists and answers `501` |

## Decisions

| # | Implementation |
|---|---|
| D1 | Paths and names of `fc_openapi.yaml` only: `/self-descriptions/...`, `issuers`, `validators`, `statuses`, `ids`, `hashes`, `providedBy`, `/webhooks/schema-events`; search stays `/selfDescriptions/quickSearch` and `/advancedSearch`. No aliases |
| D2 | `common/ProblemAdvice`, `Problems`, `ProblemTypes`; the security entry point writes Problems too. The content type is preset so `Accept` never blocks an error body |
| D3 | cat at the root, qma under `/v1`, cbe under `/v1` and `/v2`; `common/HealthController` |
| D4 | Only qma calls cat search (`qma/client/CatClient`); cat filters by policy and quality (`SearchService`) |
| D5 | cat `/schemas` is the SHACL store (`ShapeService`); cbe `/v2/schemas` proxies schema-stub (`V2Controller`). Both read `env/seed/schemas` |
| D6 | `POST /v1/governanceAuthority/selfDescriptions/advancedSearch` in `GovernanceAuthorityController.advanced` |
| D7 | The three operations of 6.1 in `FederatedController` and `GovernanceAuthorityController.detail` |
| D8 | `V1Controller` and `V2Controller` side by side; v1 detail = own catalogue |
| D9 | `produces` and `consumes` on every mapping; Spring raises 406 or 415, mapped to Problems |
| D10 | `OutOfScopeController` (cat) and `V1Controller.document` (cbe) answer `501 notImplemented` |
| D11 | `selfDescriptionId` = `credentialSubject.@id`; `sdHash` = `Jcs.sdHash` (SHA-256 hex of JCS of the full VC); `validatorDids` always `[]` |
| D12 | One pipeline (`PublicationService.publish`) for both POSTs; both return meta plus `qualityAssessment`, `Location`, `resourceKey`, `currentVersion` |
| D13 | `simpl:version` (default 1) and `simpl:previousVersion.@id`; chain rules and `409 versionConflict`; previous version revoked with a new `statusDatetime` in the same transaction |
| D14 | `PublicationService.publish` runs the nine steps in the digest order; steps 2, 3, 5, 6 in `VcChecks` |
| D15 | Synchronous; `202` is never returned |
| D16 | Owner only (`403 forbidden`), `404` unknown, idempotent; revoked RDs readable and listable, excluded from search and Consumer detail |
| D17 | `RdService.delete` by `sdHash`, owner only, removes the row (RD and QAR); does not reactivate anything |
| D18 | `SelfDescriptionController.list` and `RdService.ListFilter`: default `active`, ISO ranges start inclusive and end exclusive, comma lists OR within and AND across, `validators` matches nothing |
| D19 | cat `RdService.versions` (others only); qma `ParticipantController.versions` (all, owner only) |
| D20 | No notifications |
| D21 | `ParticipantController`: owner's RDs, active and revoked, the two orderings |
| D22 | `crypto/SignatureVerifier`: `JsonWebSignature2020`, header compared as JSON to `{"alg":"EdDSA","b64":false,"crit":["b64"]}`, signing input = header segment as sent + `.` + JCS(VC without proof), Ed25519 via Bouncy Castle, `verificationMethod` DID must equal `issuer` |
| D23 | `crypto/KeyRegistry` from `env/keys/participant-keys-{a,b}.json`; keys compared by `x`; the four seeds are checked in `CryptoTest` |
| D24 | `rdf/JsonLdSupport.BundledLoader` serves the two W3C contexts from the classpath and refuses any other URL (`400`) |
| D25 | `VerificationController`: flags as decided, VP refused, nothing stored |
| D26 | `validatorDids: []`; the `validators` filter matches nothing |
| D27 | `SearchService.matchesAnyTerm`: whitespace split, case-insensitive substring over every scalar under `credentialSubject` except the `@context`, `@type`, `rdf:type` and `simpl:servicePolicy` subtrees |
| D28 | cat `searchTerm`, qma `searchString` (contract pattern, `400`), cbe `q` passed on as `searchString` |
| D29 | `common/AdvancedQuery`: node selection by `rdf:type` (or `@type`) at any depth, property local names, substring or numeric range, AND, the contract error details |
| D30 | `common/AccessPolicy.discoverable`, used by cat search and both qma detail paths |
| D31 | `common/Paging`; qma `Support.page` adds `page`, `pageSize`, `total`, `totalPages`, `next` |
| D32 | qma local advanced searches only with the own id; cbe: missing `catalogueIds` = own, `local` alias (`GroupedSearch.resolve`) |
| D33 | No sort parameter |
| D34 | `common/RdProjection.mainProperties`; scores added in cat, `i` wrapper with `qualityScore`, `qualityClassification`, `catalogueId` in cbe |
| D35 | `docker-compose.yml` and `env/federation/*.json` |
| D36 | qma-a gets a client-credentials token (`TokenClient`); qma validates `/v1/federated/...` tokens of the realms a, b, c (`SecuritySupport`, federation paths) and then checks `iss` and `azp` against `trustedAuthorities` (`FederatedController.inbound`, `403`) |
| D37 | `FederationConfig.mapAttributes`; missing `User-Attributes` is `400`, empty is allowed |
| D38 | `cbe/api/GroupedSearch`: shape of 6.2, per-group paging, own catalogue first then the sent order |
| D39 | qma `GovernanceAuthorityController.combine` (single id: error, several: skip and `Simpl-Failed-Catalogues`); cbe turns errors into `UNAUTHENTICATED` or `UNAVAILABLE` groups |
| D40 | Unknown ids `400` at qma and cbe; inactive ids not called, `502 federationUnavailable` |
| D41 | Trustor nulls the scores; the Trusted side does not forward quality filters and nulls federated scores again; cbe filters only the own group |
| D42 | No schema in any detail response |
| D43 | `GovernanceAuthorityController.detail`, `FederationClient.call(detail=true)`, `cbe/api/Detail` pass `400`, `403`, `404`, `502`, `504` through, anything else `500` |
| D44 | `/federated/...` search only the own catalogue; other ids give an empty page |
| D45 | No search without explicit selection |
| D46 | `V1Controller.catalogues` lists `federatedCatalogues` of `federation-a.json` in file order |
| D47 | `Http.client`: 3 s for Trustor calls, 10 s for every other backend call |
| D48 | `FederationClient` sends a UUID v4 `Correlation-Id`; the Trustor echoes it through a servlet filter that runs before security |
| D49 | 0 to 100 everywhere; score = 100 x `score` / `maxScore`; `minimumScore` >= 0, <= 100 at cbe |
| D50 | `quality/QualityClient`: `POST /v1/score-validation`, `GET /v1/profiles/active?schemaName=` |
| D51 | `QualityEvaluator.firstFailure`: thresholds of `includesThreshold`, overall first, then dimensions in `includesDimension` order, `>=`, message `Quality Score X is below treshold Y.` with at most two decimals |
| D52 | `QualityGate.evaluate`: scoring error or timeout or invalid QAR is `502 qualityAssessmentFailed`, no profile or no thresholds is `500 qualityConfigurationError`; one log line each with the RD id |
| D53 | `QualityProfile`: label = local name of `rangeClassification`, `min <= s < max`, top range inclusive, ranks by lower bound, case-insensitive |
| D54 | `SearchService.qualityFilter` (AND, RDs without score excluded, unknown label `400`); cbe validates the label against the default profile too |
| D55 | `quality/Qar.parse` checks; `RdService.putQar` replaces and recomputes without gating |
| D56 | `SchemaController.recalc`: pattern `400`, `404 schemaNotFound`, `404 activeQualityProfileNotFound`, `202` with the active count |
| D57 | qma detail returns `qualityAssessment` for own RDs; cbe adds it to the VC as one extra member |
| D58 | `ShapeService`: SHACL only (Turtle or JSON-LD), id = local name of the offering `sh:NodeShape`, `409` on duplicates, version `"1"`, `PUBLISHED`; seed folders loaded at start-up |
| D59 | `VcChecks.schema`: `dct:conformsTo` with `dct:schemaName` and `dct:hasVersion` required; validation uses only that shape graph |
| D60 | `WebhookController` and `ShapeService.event`: name required, statuses and version stored, unknown names remembered for a later `POST /schemas` |
| D61 | `V2Controller.schemas` with `schemaType` (default `RESOURCE`), content, version, active profile |
| D62 | Not built; the stub serves one SEARCH schema |
| D63 | Jena SHACL Core, open shapes; typed numeric literals are normalised to decimal text before validation (see BUILD-LOG) |
| D64, D65 | `env/keycloak/*-realm.json`: realms, clients, users, password `Passw0rd!`, `participant_id` and `identity_attributes` mappers |
| D66, D67 | Role checks per operation through `Caller.requireAny` (table 5.4); "N/A" = any valid token of the realm |
| D68 | `common/SecuritySupport`: issuer `http://localhost:18080/realms/<realm>`, JWKS from `http://keycloak:8080/...`, own realm only on ordinary paths |
| D69 | `User-Attributes` comma separated; cat uses it when present, else the token; qma local ignores it; qma-b calls cat-b with `qma-b-service` and the mapped set |
| D70 | `data-testid` values of section 7 in the Vue components |
| D71 | `CatalogueSelector.vue`: list loaded on each tab open, `local` checked, inactive disabled, `catalogue-error` on failure |
| D72 | `QualityFilters.vue`: score 0 to 100 or `min-score-error` and a disabled `search-button`; labels from the DataOfferingShape profile in ascending order |
| D73 | `ResultsTabs.vue`: one tab per group, per-tab paging that re-requests only that catalogue, status message, scores only for the own group |
| D74 | `shapeForm.ts` and `AdvancedForm.vue`: RESOURCE list when only the own catalogue is selected, the SEARCH schema otherwise; sections per `sh:node`, numeric fields with `op1`, `value1`, `op2`, `value2` |
| D75 | `DetailView.vue` and `flatten.ts`: `detail-property` rows, quality block for own RDs, `detail-error` on any error |
| D76, D77 | Not built (Schema Manager UI, provider UI) |
| D78 | Ports and names of 5.1, port 8000 in every container, no database host port |
| D79 | `stubs/schema-stub`, `stubs/quality-stub` |
| D80 | Test data rules are respected by `smoke.sh` (run token, clean-up by `sdHash`) |
| D81 | `env/seed/schemas`, `env/seed/catalogue-only-schemas`, mounted read-only |
| D82 | Log lines written by `QualityGate` |
| D83 | No DOME option |
