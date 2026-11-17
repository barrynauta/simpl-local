# Spec feedback: the four components compared (17 Nov 2026, with the API checks of step 21)

Steps 12b, 20, 21 and 23 of the spec factory, run on the published specifications of four Simpl-Open components, and assembled per component by step 29. This page puts them side by side. Per component: [Contract Management](./contract-management/validation/spec-feedback.md), [Schema Manager](./schema-manager/validation/spec-feedback.md), [Catalogue](./catalogue/validation/spec-feedback.md), [Vocabulary Manager](./validation/spec-feedback.md).

**What this is.** An automated reading of the specs as a builder would read them, before anything is built. A blocker is a point where two careful builders would build different things. The findings were made by agents and not checked one by one by a person: treat them as well-argued questions. They are pointers to where the specs can be made more precise, not a judgement on the people who wrote them.

## In numbers

| Component | Stories | Live ACs | Operations | Operations without AC | Blockers | Majors | Minors | Blockers per 100 ACs | Glossary terms | Agreed | Open | Decisions (step 30) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Contract Management | 36 | 138 | 26 | 13 | 21 | 53 | 24 | 15.2 | 45 | 11 | 10 | 38 |
| Schema Manager | 55 | 210 | 18 | 9 | 16 | 52 | 36 | 7.6 | 54 | 13 | 25 | 48 |
| Catalogue | 33 | 77 | 39 | 25 | 24 | 58 | 27 | 31.2 | 49 | 11 | 12 | 46 |
| Vocabulary Manager | 35 | 91 | 19 | 1 | 14 | 42 | 29 | 15.4 | 34 | 7 | 7 | 44 |
| **Total** | **159** | **516** | **102** | **48** | **75** | **205** | **116** | **14.5** | **182** | **42** | **54** | **176** |

## What all four have in common

1. **The main object has more than one name.** Contract Management: usage contract, contract agreement, contract (four names). Catalogue: resource description and SelfDescription. Vocabulary Manager: vocabulary, InternalVocabulary, VocabularyCategory. Schema Manager: schema, with the version under five names. The stories and the business processes use one language, the API and the CDM another.
2. **The identity of the main object has several names.** Eight for a resource description, five for a schema version, two keys for a usage contract. Nothing says which id one call passes to the next.
3. **The lifecycle is not defined once.** No CDM of the four defines the states and transitions of its main object in a way the API and the stories share: ACCEPTED against APPROVED (Contract Management), REVOKED against "unavailable" (Schema Manager), active and revoked against published and pending (Catalogue), vocabulary status against version status (Vocabulary Manager).
4. **Stories and OpenAPI disagree on operations.** Methods and status codes differ (POST 201 against PATCH 200), stories name operations that no OpenAPI has (`GET /schemas/{schemaName}`, the lifecycle operations of Contract Management, the federated search of Catalogue), and almost half of the operations have no acceptance criterion.
5. **The CDMs are not yet a source of truth.** All 23 register entries are "In review", none approved; most live on feature branches only; the Contract Management CDMs point to the old repositories; the Schema Manager CDM describes another API revision. See [`cdm/README.md`](./cdm/README.md).

## The API definitions (step 21)

- **The OpenAPI files do not follow the decisions yet.** Every component has a list of changes for its API owner: response codes, required fields, error types, the security scheme. The step 30 decisions win over the files until they are updated.
- **Security schemes do not fit the roles.** Three components declare OAuth2 scopes on a placeholder server, while the stories and decisions use Keycloak roles; catalogue-be uses a scheme it does not define.
- **Operations without a story.** 48 of 102 operations have no acceptance criterion. Most should be retired (18 in Catalogue, 6 in Contract Management, 6 vocabulary operations in the Schema Manager); in Contract Management one of them lets any API-key caller finalise or terminate a contract, around the state machine.
- **One security risk found by the checks:** the Schema Manager accepts any URL as a webhook target (server-side request forgery). Step 30 now requires an allow list.
- **Error bodies:** catalogue-be and the Vocabulary Manager do not answer errors as problem+json, as the API guidelines ask.

## What helps most

- **One glossary per component**, agreed by the story owners, the API owners and the CDM owners, with the states of the main object. The drafts are in each `validation/glossary.md`; 54 terms are open. Most blockers about names and states close with it.
- **One owner per operation**: the OpenAPI is the contract, and a story that needs another method or an operation that does not exist changes the OpenAPI first.
- **Numbered acceptance criteria**: Schema Manager and Vocabulary Manager number few of theirs, so tests cannot point at them in a stable way.
