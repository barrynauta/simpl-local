# Domain-driven design: ubiquitous language and entities

**A factory addition, not a Simpl guideline.** The published guidelines ask for a CDM next to every API (API-01, DATA-01) and for business nouns as entity names (DATA-09), but they do not ask that the stories, the business processes, the API and the CDM use one language. This page adds that, from domain-driven design (Evans, *Domain-Driven Design*, 2003; Vernon, *Implementing Domain-Driven Design*, 2013). Step 11 turns it into the `DDD-` rules; step 23 checks them. Agreed with the factory owner on 17 Nov 2026.

## Ubiquitous language

Each component is one bounded context. Inside it, one concept has one name, and that name is the same in the user stories, the acceptance criteria, the business process, the CDM, the API (paths, schemas, fields, enum values), the events and the code.

- A concept must have exactly one term inside a bounded context. Synonyms (for example "usage contract" and "contract agreement" for the same thing) must be resolved to one term, or the difference must be defined.
- One term must not name two different concepts inside a bounded context.
- The terms must be listed in a glossary per bounded context, with a one-line definition and the CDM class they map to.
- Status and other value sets must use the same values everywhere: the CDM, the API enum, the acceptance criteria and the UI labels. If the UI shows a label that differs from the value, the mapping must be written down.

## Entities in the APIs

- Every resource or schema an API exposes as a business object must map to a CDM entity or value object of the same name. Request and response wrappers are allowed, but they must be recognisable as such and must not introduce a business concept the CDM lacks.
- Every attribute of an exposed business object should map to a CDM attribute of the same name.
- An entity must have an identity that stays the same over its lifecycle; the API must expose that identity under one name (for example `contractAgreementId` everywhere, not `id` in one place and `agreementId` in another).
- A value object (no identity of its own, compared by value) should not get its own id in the API.
- Changes to an aggregate should go through its root: the API should not offer operations that change a part of an aggregate without its root.
- An entity's lifecycle (its states and allowed transitions) must be defined once, in the CDM or next to it, and the API operations that change state must match those transitions.

## Boundaries between contexts

- Concepts owned by another bounded context must be marked as external in the CDM (`<<external>>`) and referenced by their owner's name and identity, not redefined.
- Where the component talks to another context with a different model (for example the EDC connector), the translation must sit in one place (an anti-corruption layer), and the API of the component must use its own language, not the other context's.
- The relations between contexts (who depends on whom, who owns which concept) should be shown in a context map.
