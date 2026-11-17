# Spec feedback: the four components compared (17 Nov 2026, with the API checks of step 21 and the business process checks of step 22)

Steps 12b, 20 to 23 of the spec factory, run on the published specifications of four Simpl-Open components, and assembled per component by step 29. This page puts them side by side. Per component: [Contract Management](components/contract-management/validation/spec-feedback.md), [Schema Manager](components/schema-manager/validation/spec-feedback.md), [Catalogue](components/catalogue/validation/spec-feedback.md), [Vocabulary Manager](components/vocabulary-manager/validation/spec-feedback.md).

**What this is.** An automated reading of the specs as a builder would read them, before anything is built. A blocker is a point where two careful builders would build different things. The findings were made by agents and not checked one by one by a person: treat them as well-argued questions. They are pointers to where the specs can be made more precise, not a judgement on the people who wrote them.

## In numbers

| Component | Stories | Live ACs | Operations | Operations without AC | Blockers | Majors | Minors | Blockers per 100 ACs | Glossary terms | Agreed | Open | Decisions (step 30) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Contract Management | 36 | 138 | 26 | 13 | 22 | 65 | 26 | 15.9 | 45 | 11 | 10 | 49 |
| Schema Manager | 55 | 210 | 18 | 9 | 17 | 60 | 41 | 8.1 | 54 | 13 | 25 | 55 |
| Catalogue | 33 | 77 | 39 | 25 | 25 | 67 | 34 | 32.5 | 49 | 11 | 12 | 53 |
| Vocabulary Manager | 35 | 91 | 19 | 1 | 14 | 48 | 32 | 15.4 | 34 | 7 | 7 | 49 |
| **Total** | **159** | **516** | **102** | **48** | **78** | **240** | **133** | **15.1** | **182** | **42** | **54** | **206** |

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

## The business processes (step 22)

Following each process as a flow between parties found what the checks per criterion and per operation could not see:

- **The parties can end in different states.** In Contract Management the Provider's contract became Active before the Consumer had confirmed; if the Consumer's check then failed, the Consumer closed it and the Provider was never told. Step 30 now keeps both sides in Signed until the other side confirms, with a timeout and a compensating step.
- **Failure paths are mostly missing.** What happens when a step in the middle fails (a refused publication after the connector asset was created, a callback that never arrives, a dependency that is down) is rarely written down. MSV-08 (sagas) and OWT-A10 (exceptional conditions) are the rules that catch it.
- **Hand-overs have no owner.** Who tells the Consumers about a new version or a revocation of a resource description, and how, is in no specification; the process names two different senders.
- **Events are sent once or not at all.** Schema Manager webhooks were synchronous with one attempt: the Governance Authority waited for every subscriber, and a subscriber that missed a revocation could never catch up. Step 30 now uses an outbox with retries.
- **Concurrency is never mentioned.** Two requests at the same time (two new versions, two schemas for one scope) have no defined outcome in any of the four components.
- **The process texts lag behind.** Several decisions now depart from the process descriptions; Schema Manager's decisions explicitly win over the BP02C text until the process owner aligns it.

## What helps most

- **One glossary per component**, agreed by the story owners, the API owners and the CDM owners, with the states of the main object. The drafts are in each `validation/glossary.md`; 54 terms are open. Most blockers about names and states close with it.
- **One owner per operation**: the OpenAPI is the contract, and a story that needs another method or an operation that does not exist changes the OpenAPI first.
- **Numbered acceptance criteria**: Schema Manager and Vocabulary Manager number few of theirs, so tests cannot point at them in a stable way.
