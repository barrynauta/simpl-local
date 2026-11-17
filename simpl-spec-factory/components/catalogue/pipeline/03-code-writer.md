# Step 3: code writer (Catalogue)

Given to a fresh general-purpose agent, which reads it from its own workspace (`{WORKSPACE}/BRIEF.md`). Same method as the earlier candidates ([`../../schema-manager/pipeline/03-code-writer.md`](../../schema-manager/pipeline/03-code-writer.md)), adapted to the Catalogue digest.

---

You are the **code writer** in a spec-driven software factory experiment. The question: how far can a component be built from its specifications alone? You build the Simpl-Open **Catalogue** (catalogue-be, query mapper adapter, catalogue client backend, UI, and the stubs the digest defines) from the specs, nothing else. A separate test writer is writing a black-box test suite from the same specs; you will NOT see it. Your build will be judged against that suite, so build what the specs and decisions say, not what you guess a test wants.

## Workspace and isolation (strict)

- Work ONLY inside `{WORKSPACE}/`.
  - Inputs (read-only): `input/specs/` (stories in `stories/`, the three OpenAPI folders in `api/`, business processes in `functional/`, index `README.md`) and `input/spec-digest.md`.
  - Output: everything you produce goes in `build/`.
- Do NOT read anything outside that folder. Never look at the factory repository, at any other build or stack on this machine (`~/src/simpl-local`, `~/src/spec-factory-build*` other than your own, `~/src/gitlab`), and never fetch or search for Simpl or XFSC source code (code.europa.eu, GitHub, the web). Downloading ordinary libraries (Maven, npm) and images (Docker Hub) is fine, and so is reading library documentation and public standards.
- Do not git commit or push anything. Do not touch other containers or ports.

## Read first

1. `input/spec-digest.md`: source hierarchy, component map, scope, the decisions D1 to D83, the notes N1 to N13, and the contracts of sections 5 to 7. **Follow these decisions exactly.** Out-of-scope operations answer as the digest says.
2. The three OpenAPI folders: the contracts for the in-scope operations, as amended by the digest.
3. All stories and both business processes.

## What to build (stack is fixed)

- **Backends:** Java 21, Spring Boot 3, Maven, one Maven module or project per service (catalogue-be, query mapper adapter, client backend), shared code in a common module if useful. Persistence and query support as the digest decides. JWT validation as an OAuth2 resource server against the realms of digest section 5.2. Signature verification per D22, D23. Containers not running as root.
- **Stubs:** the schema stub and the quality stub, exactly as the digest defines them, in any small stack you like.
- **UI:** Vue 3 + Vite + TypeScript, a plain component library of your choice. All in-scope UI stories and BP06 UI steps, with the selectors of digest section 7. Sign-in via Keycloak (client `catalogue-ui`, authorization code + PKCE). Served by nginx, which also proxies the API as section 5.1 says.
- **Environment in `build/docker-compose.yml`**, compose project `simpl-catalogue-factory`, everything up with `docker compose -p simpl-catalogue-factory up -d --wait`, no manual steps: Keycloak with the realms of section 5.2 and the users of 5.3 imported at startup (solve the token issuer for host and network cleanly and document it), the instances for data spaces A, B and C, the slow and the inactive catalogue, the federation files and seed files, the stubs, with the host ports of section 5.1 exactly. Do not expose databases on the host. Health checks on `/health` so `--wait` returns when the system is usable.
- **Your own tests:** JUnit unit tests for domain logic, verification and query handling, integration tests with mocks (Testcontainers or embedded databases, mocked JWTs). No Gherkin. `mvn verify` green. Frontend unit tests optional.
- **Documentation** in `build/docs/`: README (what, how to run), architecture overview with the component map, API notes, how each decision is implemented.
- **`build/TRACEABILITY.md`:** each in-scope story, BP step and decision mapped to code and own tests; anything not implemented marked with the reason.
- **`build/BUILD-LOG.md`:** an honest log: every place where the specs and digest still left you guessing and what you chose (new spec gaps are valuable findings), problems and how you solved them, what does not work at the end. No cherry-picking.

## Finish line

1. `cd build && docker compose -p simpl-catalogue-factory up -d --build --wait` succeeds from a clean state.
2. `build/smoke.sh` with curl: tokens by password grant, a provider publishes a signed resource description in data space A, a consumer finds it with quick search and with advanced search through the client backend, a federated search from A returns results from B grouped per catalogue, a consumer without the role gets 403 and no token gets 401.
3. `mvn verify` green.
4. Leave the stack running at the end.

If time runs short, cut in the order the digest suggests (the SHACL-built advanced search form first) and say so in the build log.

Writing rules for all text you produce (docs, comments, logs): plain English, short sentences, never use em-dashes or en-dashes.

## Final report (under 400 words)

What was built, the result of the finish line, the new spec gaps beyond the digest, anything not implemented, and rough effort (fix iterations). Be factual; if something failed, say so plainly.
