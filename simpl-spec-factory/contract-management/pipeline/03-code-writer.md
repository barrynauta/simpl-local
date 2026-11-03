# Step 3: code writer (Contract Management)

Given to a fresh general-purpose agent, which reads it from its own workspace (`{WORKSPACE}/BRIEF.md`). Same method as the earlier candidates ([`../../catalogue/pipeline/03-code-writer.md`](../../catalogue/pipeline/03-code-writer.md)), adapted to the Contract Management digest.

---

You are the **code writer** in a spec-driven software factory experiment. The question: how far can a component be built from its specifications alone? You build Simpl-Open **Contract Management** (contract manager backend, contract UI backend, UI, for a Provider and a Consumer, and the stub service the digest defines) from the specs, nothing else. A separate test writer is writing a black-box test suite from the same specs; you will NOT see it. Your build will be judged against that suite, so build what the specs and decisions say, not what you guess a test wants.

## Workspace and isolation (strict)

- Work ONLY inside `{WORKSPACE}/`.
  - Inputs (read-only): `input/specs/` (stories in `stories/`, the OpenAPI folders in `api/`, business processes in `functional/`, index `README.md`) and `input/spec-digest.md`.
  - Output: everything you produce goes in `build/`.
- Do NOT read anything outside that folder. Never look at the factory repository, at any other build or stack on this machine (`~/src/simpl-local`, `~/src/spec-factory-build*` other than your own, `~/src/gitlab`), and never fetch or search for Simpl, EDC or contract-billing source code (code.europa.eu, GitHub, the web). Downloading ordinary libraries (Maven, npm) and images (Docker Hub) is fine, and so is reading library documentation and public standards.
- Do not git commit or push anything. Do not touch other containers or ports.

## Read first

1. `input/spec-digest.md`: source hierarchy, component map, scope, the decisions D1 to D83, and Appendices A to E. **Follow these decisions exactly.** The state machine of D37, the stub contract of Appendix C, the Kafka messages of Appendix D and the proof algorithm of Appendix E are contracts, not suggestions.
2. The OpenAPI folders: the contracts for the in-scope operations, as amended by the digest.
3. All stories and BP07.

## What to build (stack is fixed)

- **Backends:** Java 21, Spring Boot 3, Maven: the contract manager backend and the contract UI backend, one project each, shared code in a common module if useful. PostgreSQL per D76, Kafka per Appendix D, JWT validation as an OAuth2 resource server against the realms of D67 where the digest asks for users, API keys per D77. Proofs per Appendix E. Containers not running as root.
- **Stub service:** connector, signer and VC issuer, wallet, catalogue and directory, with the control endpoints, exactly as Appendix C defines them, in any small stack you like.
- **UI:** Vue 3 + Vite + TypeScript, a plain component library of your choice. The in-scope UI stories with the hooks of D72, sign-in via Keycloak (client `contract-ui`, authorization code + PKCE), one UI per party. Served by nginx, which also proxies the UI backend.
- **Environment in `build/docker-compose.yml`**, compose project `cm-factory`, everything up with `docker compose -p cm-factory up -d --wait`, no manual steps: Keycloak with the realms and users of D67 imported at startup (the issuer per D67), the Provider and Consumer instances, Kafka, PostgreSQL, the stub with its seed (D83), with the host ports of D76 exactly. Do not expose other ports. Health checks on `/health` so `--wait` returns when the system is usable.
- **Your own tests:** JUnit unit tests for the state machine, validation order and proofs, integration tests with mocks (Testcontainers or embedded, mocked JWTs). No Gherkin. `mvn verify` green. Frontend unit tests optional.
- **Documentation** in `build/docs/`: README (what, how to run), architecture overview with the component map and the two parties, API notes, how each decision is implemented.
- **`build/TRACEABILITY.md`:** each in-scope story, BP step and decision mapped to code and own tests; anything not implemented marked with the reason.
- **`build/BUILD-LOG.md`:** an honest log: every place where the specs and digest still left you guessing and what you chose (new spec gaps are valuable findings), problems and how you solved them, what does not work at the end. No cherry-picking.

## Finish line

1. `cd build && docker compose -p cm-factory up -d --build --wait` succeeds from a clean state.
2. `build/smoke.sh` with curl: a whole negotiation driven through the stub from request to an ACTIVE contract at both parties, with both wallets holding the two VCs (D58); one incorrectly signed offer rejected with the right reason; a suspend and a reactivate; the expiry run of D82; no token gets 401 and a user without the role gets 403.
3. `mvn verify` green.
4. Leave the stack running at the end.

If time runs short, cut in the order the digest suggests (contract clause upload 28846 and the consent link 31047 first) and say so in the build log.

Writing rules for all text you produce (docs, comments, logs): plain English, short sentences, never use em-dashes or en-dashes.

## Final report (under 400 words)

What was built, the result of the finish line, the new spec gaps beyond the digest, anything not implemented, and rough effort (fix iterations). Be factual; if something failed, say so plainly.
