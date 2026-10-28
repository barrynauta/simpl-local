# Schema Manager: second factory candidate

Status: **run 1 done** (2026-10-28). The generated build passes 291 of 291 black-box API scenarios and 36 of 36 UI scenarios. The local upstream stack (May 2026): 5 of 291 API and 0 of 36 UI, mostly because of one rule that no spec states (see below). Details: [`findings.md`](./findings.md).

## Why this candidate

The Vocabulary Manager was well specified: paired API and UI stories, 30 decisions to make it buildable. The Schema Manager was chosen as the **less well documented** contrast: 55 live stories out of 91 (36 cancelled), 11 "[ENHANCE]" stories that revisit earlier ones, and side effects (e-mail to Providers, webhooks) next to the API.

## Summary

- **Input:** published or to-be-published material only: 55 stories without review comments, the canonical OpenAPI, the BP02C business process ([`specs/`](./specs/README.md)). Stories collected locally with a Jira token and Mistral Small 3.2.
- **Digest:** 63 decisions ([`spec-digest.md`](./spec-digest.md)), twice the Vocabulary Manager's 30. Main gaps: how a schema is identified (name, name and resource type, or name and schema type), the default properties (referenced, not published), the semantic validation rules, a status model that contradicts itself, and webhook and e-mail content.
- **Contract findings:** the canonical OpenAPI also contains six Vocabulary Manager operations that the service does not implement (a stale copy); `GET /schemas/{schemaName}` is missing; the "full" file still refers to an external file.
- **Build:** Spring Boot, PostgreSQL, Vue 3, Keycloak, Mailpit and a Vocabulary Manager stub, from the specs and digest only ([`generated/run1/`](./generated/run1/)). 291 of 291 API, 36 of 36 UI.
- **Upstream applies a rule no spec states:** a schema file must declare its resource type inside the Turtle. Every create in the test suite fails on it, so the upstream baseline says little about the rest.
- **Lesson:** as with the first candidate, the specs plus a written decision list are buildable. Here the list is twice as long, and at least one rule lives only in the code. The decision list is the real deliverable for the spec owners.

## Layout

| Path | What |
|---|---|
| [`specs/`](./specs/README.md) | Frozen input set and story index |
| [`spec-digest.md`](./spec-digest.md) | Step 1: scope, enhancement pairs, decisions D1 to D63, open questions |
| [`tests/`](./tests/README.md) | Steps 2 and 4: 291 API and 36 UI scenarios (behave, requests, Playwright), webhook receiver, fixtures, reports |
| [`generated/run1/`](./generated/run1/) | Step 3: the build, with `TRACEABILITY.md` and `BUILD-LOG.md` |
| [`pipeline/`](./pipeline/) | The briefs for steps 2 to 4 as used |
| [`findings.md`](./findings.md) | Findings per step |

Not done yet: swap tests with upstream. They would test the API contract only, since upstream uses Fuseki and Kafka where the build uses PostgreSQL and HTTP webhooks.
