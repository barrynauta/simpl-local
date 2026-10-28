# Architecture

## Component map

```
  Browser                                    smoke.sh, tests
     |                                              |
     v                                              v
  ui-a (nginx :8000) --/api/v1,/api/v2--> cbe-a (/v1, /v2) --> qma-a (/v1) --> cat-a (/)          data space A
                                            |   |              |  \
                                            |   +-- QAR ------>|   +-- federation token, User-Attributes, Correlation-Id
                                            |                  |        |
                                            +--> schema-stub   |        +--> qma-b (/v1/federated) --> cat-b   data space B
                                            +--> quality-stub  |        +--> qma-c (/v1/federated)             data space C (trusts nobody)
                                                               |        +--> schema-stub /stub/slow/v1        "slow" catalogue
                                         cat-a, cat-b --> quality-stub
                                         all backends --> keycloak (JWKS)   qma-a, qma-b --> keycloak (client credentials)
```

| Container | Role | Talks to |
|---|---|---|
| keycloak | Realms `dataspace-a`, `dataspace-b`, `dataspace-c`, users, clients | |
| catalogue-db | PostgreSQL, databases `cat_a`, `cat_b` | |
| cat-a, cat-b | catalogue-be of data space A and B | catalogue-db, quality-stub, keycloak (JWKS) |
| qma-a | Trusted GA of A: local search, GA search across selected catalogues, detail, participant operations | cat-a, qma-b, qma-c, schema-stub (slow), keycloak |
| qma-b | Trustor GA of B: inbound federated search and detail, attribute mapping | cat-b, keycloak |
| qma-c | GA of C, trusts nobody, has no catalogue | keycloak |
| cbe-a | Consumer backend of A: grouped search, detail, schemas, quality profile | qma-a, cat-a (QAR only), schema-stub, quality-stub |
| ui-a | Vue single page app on nginx, proxies `/api/v1/*` and `/api/v2/*` to cbe-a | cbe-a; the browser talks to keycloak directly |
| schema-stub | Schema Manager stand-in, `/stub/slow/...` answers after 10 s | seed files |
| quality-stub | Quality Scoring and MQP stand-in with a control API | |

Every service listens on port 8000 inside its container. Containers run as non-root users
(`app` for the Java images, `node` for the stubs, the nginx-unprivileged user for the UI,
uid 1000 for Keycloak; the official PostgreSQL image drops to its `postgres` user).

## Code layout

```
backend/
  common/                 eu.simpl.catalogue.common: Problem types and advice, JWT resource server set-up (multi realm),
                          Caller (claims of D65), Paging, QualityParams, AccessPolicy (D30), AdvancedQuery (D29),
                          RdProjection (D34), FederationConfig (6.3) with attribute mapping (D37), TokenClient, Http
  catalogue-be/           eu.simpl.catalogue.cat
    crypto/               Jcs (RFC 8785, sdHash), DidJwk, SignatureVerifier (D22), KeyRegistry (D23)
    rdf/                  JsonLdSupport (bundled contexts, D24), ShapeSupport (SHACL, D58, D63)
    quality/              QualityClient (stub calls), QualityProfile (D53), Qar (D55), QualityEvaluator (D51)
    store/                RdRepository, ShapeRepository (JDBC), schema.sql
    service/              VcChecks (D14 steps 2, 3, 5, 6), PublicationService (D13, D14), QualityGate (D50 to D52),
                          SearchService (D27, D30, D31, D54), RdService (list, revoke, delete, versions, QAR), ShapeService
    api/                  controllers, one per tag
  query-mapper-adapter/   eu.simpl.catalogue.qma
    client/               CatClient (own catalogue), FederationClient (Trustor calls, D36, D39, D43, D47, D48)
    api/                  LocalSearchController, GovernanceAuthorityController, FederatedController, ParticipantController
  catalogue-client-be/    eu.simpl.catalogue.cbe
    api/                  V1Controller, V2Controller, GroupedSearch (D38, D39, 6.2), Detail (D57)
ui/src/                   SearchView, DetailView, CatalogueSelector, QualityFilters, AdvancedForm, ResultsTabs,
                          shapeForm.ts (D74), flatten.ts (D75), auth.ts (keycloak-js, PKCE S256)
stubs/                    schema-stub, quality-stub (Node.js)
env/                      keycloak realms, federation files, seed shapes, participant key registries, db init
tools/sign-rd.mjs         test signer (D22, D23)
```

## Main flows

### Publication (BP05b 04 to 11, D14)

1. Provider POSTs the signed VC to `cat` `/self-descriptions` (or `/resource`).
2. `PublicationService` checks role, JSON and VC structure, JSON-LD expansion with bundled contexts,
   the Ed25519 signature over JCS, ownership and key registration, the schema reference, SHACL against
   that one shape, duplicate id, version chain.
3. `QualityGate` posts the VC to quality-stub `/v1/score-validation`, validates the QAR, reads the
   active profile for the schema, evaluates the thresholds, and builds the quality summary.
4. The RD is stored with its QAR and summary; for version n > 1 the previous version becomes `revoked`.

### Consumer search (BP06 01 to 11)

1. The UI calls cbe-a `/v1/selfDescriptions` with the selected catalogues.
2. cbe-a calls qma-a once per catalogue, in parallel: the own catalogue through the local
   `/v1/selfDescriptions` (quality filters included), each federated one through
   `/v1/governanceAuthority/selfDescriptions?catalogueIds=<id>` (no quality filters).
3. qma-a calls cat-a with the user's token and `User-Attributes` from the token, or calls the Trustor
   `{authorityURL}/federated/selfDescriptions` with its federation token, the user's attributes and a
   fresh `Correlation-Id`, with a 3 s timeout.
4. qma-b checks that the token issuer and `azp` match a trusted authority, maps the attributes
   (CONSUMER to PARTNER_CONSUMER, plus FEDERATED), and calls cat-b with its own service token.
5. cat applies the discovery policy with the attribute set and the quality filters.
6. cbe-a assembles the groups of section 6.2; a failed catalogue becomes a group with status
   `UNAUTHENTICATED` or `UNAVAILABLE`.

### Detail (SIMPL-31143, 31148, 31150, 31153, 31769, 31770, 33885)

cbe `/v2/selfDescriptions/{id}?catalogueId=` calls qma `/v1/governanceAuthority/resourceDescriptions/{id}`.
For the own catalogue qma reads the RD from cat and checks status and policy; for a federated one it calls
the Trustor `/v1/federated/resourceDescriptions/{id}`. cbe returns the VC with `qualityAssessment` added
for own RDs.

## Persistence

`cat` stores RDs, shapes and remembered schema statuses in PostgreSQL through plain JDBC (`schema.sql`).
The VC is kept as JSON text, with indexed columns for status, owner, resource key and timestamps.
Search loads the active RDs and filters them in memory (policy, matching, quality). That is simple and
correct for the size of a test environment; it is not meant for large catalogues.
