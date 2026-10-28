# API notes (as built)

All errors of all backends are RFC 9457 Problems with `Content-Type: application/problem+json` and
`type`, `title`, `status`, `detail`, `instance` (D2). Problem types are those of digest 6.6, plus
`urn:problem-type:simpl:methodNotAllowed` (405) for a wrong method on a known path.
Missing or invalid token is `401`, a valid token without the role is `403` (5.4). An `Accept` header
that excludes the response type gives `406`, a wrong request `Content-Type` gives `415` (D9).
Every service answers `GET /health` with `200 {"status":"UP"}` without a token.

## catalogue-be (cat-a :18101, cat-b :18201), served at the root

| Operation | Roles | Notes |
|---|---|---|
| `GET /self-descriptions` | searcher set | Filters of the composed root, comma separated or repeated. Default `statuses=active`. `withMeta` (true), `withContent` (false), `offset` (0), `limit` (100, max 1000). Order `uploadDatetime` desc, `id` asc. `meta.qualityAssessment` present when a QAR exists |
| `POST /self-descriptions` | publisher set + ownership | `application/json` VC. `201`, `Location: /self-descriptions/<URL-encoded id>`, body `SelfDescriptionPublicationResponse` |
| `POST /self-descriptions/resource` | same | Same pipeline, body `SelfDescription` plus `qualityAssessment` |
| `GET /self-descriptions/{id}` | searcher set | The stored VC, any status |
| `GET /self-descriptions/{id}/qualityAssessmentRecord` | searcher set | Stored QAR; `204` without one (unreachable in practice) |
| `POST /self-descriptions/{id}/qualityAssessmentRecord` | valid token | `application/ld+json` or `application/json`. Validates (D55), replaces, recomputes the summary. `200 {"status":"ACCEPTED"}` |
| `POST /self-descriptions/{id}/revoke` | publisher set, owner | `200` with the meta, idempotent |
| `GET /self-descriptions/{id}/versions` | searcher set | Other versions of the same resource, version desc, `page`/`pageSize` |
| `DELETE /self-descriptions/{sdHash}` | publisher set, owner | Hard delete, `200` |
| `GET /selfDescriptions/quickSearch` | searcher set | `searchTerm` (required), `minimumScore`, `minimumClassification`, `page`, `pageSize`, `User-Attributes` header. `{totalCount, items:[{n}]}` |
| `POST /selfDescriptions/advancedSearch` | searcher set | Body as D29 (a JSON string holding the object is also accepted) |
| `GET /schemas` | GA_SCHEMA_VIEWER or GA_SCHEMA_ADMIN | `{"ontologies":[], "shapes":[...], "vocabularies":[]}` |
| `POST /schemas` | valid token | `text/turtle`, `application/ld+json`, `application/json`; `201`, `Location: /schemas/<id>`, body `{"schemaId"}`. `application/rdf+xml` is accepted as a media type but answers `400` (only SHACL) |
| `GET /schemas/{schemaId}` | viewer or admin | Turtle |
| `DELETE /schemas/{schemaId}` | valid token | `200`, `404` if unknown |
| `POST /schemas/{schemaName}/recalcQualityScore` | valid token | `202` acknowledgement (D56) |
| `POST /verification` | valid token | D25 |
| `POST /webhooks/schema-events` | none | D60 |
| `GET/POST /query`, `GET/DELETE /session` | none | `501` |
| `GET /verification`, `GET /schemas/latest`, `PUT /schemas/{id}` | valid token | `501` |

## query mapper adapter (qma-a :18102, qma-b :18202, qma-c :18302), under `/v1`

| Operation | Caller | Notes |
|---|---|---|
| `GET /selfDescriptions` | user, searcher set | Local quick search. `searchString` pattern of the contract. Calls cat with the user token and `User-Attributes` from the token (an incoming header is ignored) |
| `POST /selfDescriptions/advancedSearch` | user, searcher set | `catalogueIds` required; searches only if it contains the own id (or `local`), else an empty page |
| `GET /governanceAuthority/selfDescriptions` | user, searcher set | `catalogueIds` repeated (required), own and federated ids. One id: a federation error is the answer (502, 504). Several ids: pages are concatenated, failed ids are named in `Simpl-Failed-Catalogues` |
| `POST /governanceAuthority/selfDescriptions/advancedSearch` | user, searcher set | Tier-1 draft (D6), same rules |
| `GET /governanceAuthority/resourceDescriptions/{id}?catalogueId=` | user, searcher set | Section 6.1. `{catalogueId, resourceDescription, qualityAssessment?}` |
| `GET /federated/selfDescriptions` | federation token of a trusted GA | Trustor inbound quick search. `User-Attributes` (may be empty) and `Correlation-Id` required |
| `POST /federated/selfDescriptions/advancedSearch` | same | Quality fields ignored |
| `GET /federated/resourceDescriptions/{id}` | same | `{resourceDescription}` |
| `GET /participants/resourceDescriptions` | publisher set | Own RDs, active and revoked; `orderBy` `publicationDate` (desc) or `resourceType` (asc, then date desc) |
| `GET /participants/resourceDescriptions/{id}` | publisher set, owner | Stored VC |
| `PATCH /participants/resourceDescriptions/{id}` | publisher set, owner | Revoke, `204`, idempotent |
| `GET /participants/resourceDescriptions/{id}/version` | publisher set, owner | All versions including the requested one, `versionHistoryResponsePayload` |

Search responses carry `totalCount`, `items`, `page`, `pageSize`, `total`, `totalPages` and `next`
(relative, without `/v1`, `null` on the last page). `Correlation-Id` is echoed on every `/federated/...`
response when the request had one, errors included (also `401`).

Federation errors seen by the Trusted side: Trustor `401`/`403` is `502 federationUnauthenticated`;
connection error, Trustor `5xx` or an inactive catalogue is `502 federationUnavailable`; more than 3 s is
`504 federationTimeout`. On the detail, a Trustor `404` is `404` and a Trustor `403 discoveryPolicyDenied`
is `403 discoveryPolicyDenied`.

## catalogue client backend (cbe-a :18103)

| Operation | Notes |
|---|---|
| `GET /v1/selfDescriptions` | `q` (required), `catalogueIds` (repeated or comma separated, optional, `local` alias), `minimumScore` (0 to 100), `minimumClassification`, `page`, `pageSize` (per group). Response of section 6.2 |
| `POST /v1/selfDescriptions/advanced` | Body: criteria plus optional `catalogueIds`, `minimumScore`, `minimumClassification` |
| `GET /v1/selfDescriptions/{id}` | Own catalogue detail |
| `GET /v1/selfDescriptions/{id}/qualityAssessmentRecord` | Proxied to cat |
| `GET /v1/catalogues` | `federatedCatalogues` of `federation-a.json`, paged: `{items, page, pageSize, total}` |
| `GET /v1/contractClauses/documents/{documentId}` | `501` |
| `GET /v2/schemas` | `resourceType`, `schemaType` (`RESOURCE` default or `SEARCH`) |
| `GET /v2/schemas/{schemaId}/content`, `GET /v2/schemas/{schemaId}/{version}` | Turtle from schema-stub |
| `GET /v2/schemas/{schemaName}/qualityProfiles/active` | Profile from quality-stub; `400` for the PascalCase pattern, `404` if none |
| `GET /v2/selfDescriptions/{id}?catalogueId=` | Detail by catalogue; VC plus `qualityAssessment` for own RDs |

All cbe operations need the searcher set.

## Stubs

schema-stub (:18401): `GET /schemas?resourceType=&schemaType=`, `GET /schemas/{id}/content`,
`GET /schemas/{id}/{version}`, `ANY /stub/slow/...` (10 s, then `503`).

quality-stub (:18402): `POST /v1/score-validation`, `GET /v1/profiles/active?schemaName=`,
`PUT /stub/scores/{id}` (`{"overall", "dimensions"}` or `{"mode": "invalid"|"error"|"timeout"}`),
`GET` and `DELETE /stub/scores/{id}`. The profile is JSON-LD with an `@graph`: one
`MetadataQualityProfile` node (`includesDimension`, `includesThreshold`), `QualityDimension`,
`QualityClassification`, `ScoreRange` (`minScore`, `maxScore`, `rangeClassification`) and `Threshold`
nodes (`thresholdValue`, `classificationScope`).

## UI (ui-a :18104)

Single page app with the selectors of digest section 7. Routes: `/` (search) and
`/details?id=<RD id>&catalogueId=<id>`. nginx proxies `/api/v1/*` to `cbe-a:8000/v1/*` and `/api/v2/*`
to `cbe-a:8000/v2/*` and answers `/health` itself.
