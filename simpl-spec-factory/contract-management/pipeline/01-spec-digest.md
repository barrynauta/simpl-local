# Step 1: spec digest (Contract Management)

Given to a fresh general-purpose agent. Same method as the earlier candidates ([`../../pipeline/01-spec-digest.md`](../../pipeline/01-spec-digest.md)), adapted to a component that runs at two parties (Provider and Consumer), talks over events, and depends on a signer, a wallet and a connector that are not in the input. `{CM}` is this `contract-management` folder.

---

You write the **spec digest** for a spec-driven software factory experiment. Nothing is built yet. Your output is the single document that both the test writer and the code writer will work from, next to the specs. They will not see each other's work, so your decisions are what makes their results meet.

The component is Simpl-Open **Contract Management**: the contract manager backend, the contract UI backend and the contract manager UI, at a Provider and at a Consumer. Together they establish a usage contract (request, offer, sign at both sides, validate, store) and manage it afterwards (overview, details, consent status, suspend, reactivate, revoke, terminate, audit data, expiry of unsigned offers).

## Input (read-only)

`{CM}/specs/` only: the stories in `stories/`, the OpenAPI folders in `api/`, the business processes BP07 and BP10 in `functional/`, and the index `specs/README.md` (read its first observations). Read all of it.

Do not read anything else: not upstream code (`~/src/simpl-local/simpl-contract`, `~/src/gitlab`, the web), not test suites of any kind, not the other candidates' digests or builds, not the factory's other folders. You may search the web for public standards the specs refer to (W3C Verifiable Credentials, JSON-LD, ODRL, the Eclipse Dataspace Protocol, JWS and JAdES), not for Simpl source code.

## What to write: `{CM}/spec-digest.md`

1. **Source hierarchy:** which source wins when they disagree. The earlier candidates used OpenAPI > user stories > business process. Check whether that holds here, with replaced stories still live and two backends exposing the same paths.
2. **Component map:** which backend owns which operation, how the two parties talk to each other (HTTP, events, or both), what the UI uses, and what the consumption adapter OpenAPI tells you about the boundary (it is context, not scope).
3. **Scope:** which stories, operations and BP steps are in, which are out and why, and what is in scope without a story. For each replaced story pair, say which one counts. The signer, the wallet, the VC issuer, the EDC connector, the catalogue, identity and billing belong to other components: each is a stub with a contract you define, or out. Keep the scope buildable: a team should be able to build it in one session from your digest.
4. **Decisions:** one numbered row (D1, D2, ...) for every point where the specs are silent, vague or contradict each other. Each row: the topic, what each source says (story keys, OpenAPI file and path, BP step), and the decision for this run. Group them: component map and API, contract establishment (request, offer, signatures, validation, storage), contract lifecycle (consent status, suspend, reactivate, revoke, terminate, expiry), events between the parties, signer and wallet and credentials, audit and data model, access and identity, UI, environment and testability. Pay attention to:
   - **The state machine** of a contract offer and a usage contract: every state, every transition, who may trigger it, and what each side sees. Write it as a table the tests can check transition by transition.
   - **Events:** there is no published event contract. Define topic names, payloads, keys and ordering for this run, and say how a test observes them.
   - **Signing and verification:** what is signed, in which format, with which test keys, and what "incorrectly signed" means so a test can produce it. Pick something a test suite can generate without a real wallet or signer.
   - **Two parties:** define the test topology (a Provider and a Consumer instance, each with its own database), how they find each other, and how a test drives a whole negotiation from one side.
   - The identity provider contract for the tests (realm, clients, users, passwords, where roles and the participant are in the token), and the roles per operation.
   - Time-based rules (expiry of unsigned offers after X days): how a test can trigger them without waiting.
   - Ports and names for the reference environment of the build, so test writer and code writer agree. Pick free host ports in the range 19000 to 19999. Do not use 4323, 5433, 8086, 9002 or 9095 (the local upstream stack). Use one compose project name.
   - Test data: how tests create unique contracts, and anything that cannot be cleaned up.
5. **Open questions for the spec owners,** ordered by impact on buildability.
6. **Next steps** for the test writer and the code writer: what each must read, and the points where they are most likely to diverge.

## Rules

- Decide, do not defer: every row gets a decision, even a provisional one, so two writers resolve each gap the same way.
- Every decision is also a finding: phrase it so a spec owner can act on it.
- Positive tone: the spec writers did their best; describe what is missing, not who missed it.
- Roles, not names. Do not repeat names that appear in the specs.
- Plain English, short sentences, never use em-dashes or en-dashes.
- Do not git commit.

## Report back (under 400 words)

Number of decisions per group, scope in and out (counts), the five biggest gaps by impact on buildability, and whether you think the scope is buildable in one session.
