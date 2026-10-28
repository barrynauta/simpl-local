# Step 1: spec digest (Catalogue)

Given to a fresh general-purpose agent. Same method as the earlier candidates ([`../../pipeline/01-spec-digest.md`](../../pipeline/01-spec-digest.md)), adapted to a component with three backends, a UI and almost no stories for its core. `{CAT}` is this `catalogue` folder.

---

You write the **spec digest** for a spec-driven software factory experiment. Nothing is built yet. Your output is the single document that both the test writer and the code writer will work from, next to the specs. They will not see each other's work, so your decisions are what makes their results meet.

The component is the Simpl-Open **Catalogue**, taken as a whole: the federated catalogue (catalogue-be), the query mapper adapter, the catalogue client backend and the search UI.

## Input (read-only)

`{CAT}/specs/` only: the stories in `stories/`, the three OpenAPI folders in `api/`, the business processes BP05b and BP06 in `functional/`, and the index `specs/README.md` (read its first observations). Read all of it.

Do not read anything else: not upstream code (`~/src/simpl-local/simpl-catalogue`, `~/src/gitlab`, the web), not the other candidates' digests or builds, not the factory's other folders. You may search the web for public standards the specs refer to (W3C Verifiable Credentials, JSON-LD, SHACL, Gaia-X documents, openCypher), not for Simpl source code.

## What to write: `{CAT}/spec-digest.md`

1. **Source hierarchy:** which source wins when they disagree. The earlier candidates used OpenAPI > user stories > business process. Check whether that holds here: the stories are newer than parts of the contract, and the core has no stories.
2. **Component map:** which backend owns which operation, how they call each other, and which surface the UI uses. Where the three OpenAPIs overlap (for example `/selfDescriptions/advancedSearch`, the schema paths), decide what is built where.
3. **Scope:** which stories, operations and BP steps are in, which are out and why, and what is in scope without a story (OpenAPI or BP only). Things that belong to other Simpl components (Schema Manager, quality assessment, identity, DOME marketplace, the Tier 1 and Tier 2 gateways, contract and usage) are out, or a stub with a contract you define. Keep the scope buildable: a team should be able to build it in one session from your digest.
4. **Decisions:** one numbered row (D1, D2, ...) for every point where the specs are silent, vague or contradict each other. Each row: the topic, what each source says (story keys, OpenAPI file and path, BP step), and the decision for this run. Group them: component map and API, resource description lifecycle (create, versions, revoke, publication), verification and signatures, search (quick, advanced, the query language), federated search across data spaces, quality, schemas, access and identity, UI, environment and testability. Pay attention to:
   - Signing and verification of resource descriptions: what is checked, with which keys or trust anchors, and what the tests can produce. Pick something a test suite can generate without a real wallet.
   - Federated search: it needs more than one data space. Define the test topology (for example two catalogue instances, one trusted and one trustor), how they are configured and how a test seeds each one.
   - The inline review notes (`#!!`) in the catalogue-be OpenAPI: say per note what you take from it.
   - The identity provider contract for the tests (realm, clients, users, passwords, where the roles and identity attributes are in the token), and the roles per operation.
   - Ports and names for the reference environment of the build, so test writer and code writer agree. Pick free host ports: do not use 3030, 4321 to 4334, 5432, 7474, 7687, 8025 to 8027, 8080, 8081, 8085, 8086, 8096 to 8099, 8182 to 8185, 8190, 8299, 9001, 9094, 11434. Use one compose project name.
   - Test data: how tests create unique resource descriptions, and anything that cannot be cleaned up.
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
