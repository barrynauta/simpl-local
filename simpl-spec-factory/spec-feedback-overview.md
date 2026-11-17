# Spec feedback: the four components compared (17 Nov 2026)

Steps 12b, 20 and 23 of the spec factory, run on the published specifications of four Simpl-Open components, and assembled per component by step 29. This page puts them side by side. Per component: [Contract Management](./contract-management/validation/spec-feedback.md), [Schema Manager](./schema-manager/validation/spec-feedback.md), [Catalogue](./catalogue/validation/spec-feedback.md), [Vocabulary Manager](./validation/spec-feedback.md).

**What this is.** An automated reading of the specs as a builder would read them, before anything is built. A blocker is a point where two careful builders would build different things. The findings were made by agents and not checked one by one by a person: treat them as well-argued questions. They are pointers to where the specs can be made more precise, not a judgement on the people who wrote them.

## In numbers

| Component | Stories | Live ACs | Operations | Operations without AC | Blockers | Majors | Minors | Blockers per 100 ACs | Glossary terms | Agreed | Open |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Contract Management | 36 | 138 | 26 | 13 | 20 | 47 | 18 | 14.5 | 45 | 11 | 10 |
| Schema Manager | 55 | 210 | 18 | 9 | 16 | 47 | 28 | 7.6 | 54 | 13 | 25 |
| Catalogue | 33 | 77 | 39 | 25 | 21 | 48 | 17 | 27.3 | 49 | 11 | 12 |
| Vocabulary Manager | 35 | 91 | 19 | 1 | 13 | 39 | 21 | 14.3 | 34 | 7 | 7 |
| **Total** | **159** | **516** | **102** | **48** | **70** | **181** | **84** | **13.6** | **182** | **42** | **54** |

## What all four have in common

1. **The main object has more than one name.** Contract Management: usage contract, contract agreement, contract (four names). Catalogue: resource description and SelfDescription. Vocabulary Manager: vocabulary, InternalVocabulary, VocabularyCategory. Schema Manager: schema, with the version under five names. The stories and the business processes use one language, the API and the CDM another.
2. **The identity of the main object has several names.** Eight for a resource description, five for a schema version, two keys for a usage contract. Nothing says which id one call passes to the next.
3. **The lifecycle is not defined once.** No CDM of the four defines the states and transitions of its main object in a way the API and the stories share: ACCEPTED against APPROVED (Contract Management), REVOKED against "unavailable" (Schema Manager), active and revoked against published and pending (Catalogue), vocabulary status against version status (Vocabulary Manager).
4. **Stories and OpenAPI disagree on operations.** Methods and status codes differ (POST 201 against PATCH 200), stories name operations that no OpenAPI has (`GET /schemas/{schemaName}`, the lifecycle operations of Contract Management, the federated search of Catalogue), and almost half of the operations have no acceptance criterion.
5. **The CDMs are not yet a source of truth.** All 23 register entries are "In review", none approved; most live on feature branches only; the Contract Management CDMs point to the old repositories; the Schema Manager CDM describes another API revision. See [`cdm/README.md`](./cdm/README.md).

## What helps most

- **One glossary per component**, agreed by the story owners, the API owners and the CDM owners, with the states of the main object. The drafts are in each `validation/glossary.md`; 54 terms are open. Most blockers about names and states close with it.
- **One owner per operation**: the OpenAPI is the contract, and a story that needs another method or an operation that does not exist changes the OpenAPI first.
- **Numbered acceptance criteria**: Schema Manager and Vocabulary Manager number few of theirs, so tests cannot point at them in a stable way.
