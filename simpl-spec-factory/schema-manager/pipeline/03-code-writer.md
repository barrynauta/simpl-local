# Step 3: code writer (Schema Manager)

Given to a fresh general-purpose agent, which reads it from its own workspace (`{WORKSPACE}/BRIEF.md`). Same method as the first candidate ([`../../pipeline/03-code-writer.md`](../../pipeline/03-code-writer.md)), adapted to the Schema Manager digest.

---

You are the **code writer** in a spec-driven software factory experiment. The question: how far can a component be built from its specifications alone? You build the Simpl-Open **Schema Manager** (backend + UI) from the specs, nothing else. A separate test writer is writing a black-box test suite from the same specs; you will NOT see it. Your build will be judged against that suite, so build what the specs and decisions say, not what you guess a test wants.

## Workspace and isolation (strict)

- Work ONLY inside `{WORKSPACE}/`.
  - Inputs (read-only): `input/specs/` (user stories in `stories/`, OpenAPI in `api/`, business process in `functional/`, index `README.md`) and `input/spec-digest.md`.
  - Output: everything you produce goes in `build/`.
- Do NOT read anything outside that folder. Never look at the factory repository, at any other build or stack on this machine (`~/src/simpl-local`, `~/src/spec-factory-build*` other than your own, `~/src/gitlab`), and never fetch or search for Simpl source code (code.europa.eu, GitHub, the web). Downloading ordinary libraries (Maven, npm) and images (Docker Hub) is fine, and so is reading library documentation.
- Do not git commit or push anything. Do not touch other containers or ports.

## Read first

1. `input/spec-digest.md`: source hierarchy, scope (section 2), the enhancement stories (section 3) and decisions D1 to D63. **Follow these decisions exactly.** The `/vocabularies` paths and the quality profile operations are out of scope (D62): do not implement them.
2. `input/specs/api/schema_openapi.yaml`: the API contract for the in-scope operations, as amended by the digest. The versions payload refers to `./common/common-components.yaml`, which is not in the input set; the digest (D9, D10, D25) says what to do.
3. All stories in `input/specs/stories/` and the business process.

## What to build (stack is fixed)

- **Backend:** Java 21, Spring Boot 3, Maven. Persistence per D58 (PostgreSQL). JWT validation as an OAuth2 resource server per D59 (issuer and JWKS of the local Keycloak, roles from the top-level `roles` claim). Vocabulary Manager client per D60. Provider e-mail (D39, D40) over SMTP and webhook delivery (D41 to D44), both asynchronous after commit. All settings from the configuration names of D60. Container not running as root.
- **UI:** Vue 3 + Vite + TypeScript, a plain component library of your choice. All in-scope UI stories, with the `data-testid` values and texts of D45 to D57. Sign-in via Keycloak (client `schema-ui`, authorization code + PKCE). Served by nginx, which also proxies the API.
- **Environment in `build/docker-compose.yml`** (compose project name `schema-factory`), everything up with a plain `docker compose up`, no manual steps:
  - Keycloak with the realm of D59 imported at startup. Make sure the token issuer the backend accepts matches tokens fetched from the host and from inside the network; solve it cleanly (for example hostname settings) and document it.
  - PostgreSQL, not exposed on the host (5432 is taken).
  - Mailpit as the SMTP server; its HTTP API is the `MAIL_API_URL` of D60.
  - A **Vocabulary Manager stub** implementing exactly the contract of D60 (GET and PUT `/vocabularies/{name}/content`, in memory, no authentication). Tests seed it through the PUT.
  - The backend must be able to reach a webhook receiver that the tests run on the host: add `host.docker.internal:host-gateway` as an extra host. Do not filter webhook target hosts.
  - Host ports (others are in use, do not use 5432, 8080, 8085, 8086, 8096, 8097, 8182, 8183, 8184, 8190, 3030, 4322, 4323, 4333, 9094, 9001):
    - API at `http://localhost:8098/v1` (this is `SM_BASE_URL`)
    - UI at `http://localhost:4334` (`SM_UI_URL`)
    - Keycloak at `http://localhost:8185` (`KEYCLOAK_URL`)
    - Vocabulary Manager stub at `http://localhost:8099`
    - Mailpit HTTP API at `http://localhost:8027` (SMTP only inside the network)
  - Healthchecks, so `docker compose up --wait` returns when the system is usable.
- **Your own tests:** JUnit unit tests for domain logic and validation, integration tests with mocks (Testcontainers is fine, or an embedded database, mocked JWTs, a mocked Vocabulary Manager and SMTP). No Gherkin. `mvn verify` green. Frontend unit tests are optional.
- **Documentation** in `build/docs/`: README (what, how to run), architecture overview, API notes, how each decision D1 to D63 is implemented.
- **`build/TRACEABILITY.md`:** each in-scope story and each decision D1 to D63 mapped to the code and own tests; anything not implemented marked with the reason.
- **`build/BUILD-LOG.md`:** an honest log: every place where the specs and digest still left you guessing and what you chose (new spec gaps are valuable findings), problems and how you solved them, what does not work at the end. No cherry-picking.

## Finish line

1. `cd build && docker compose up -d --build --wait` succeeds from a clean state.
2. `build/smoke.sh` with curl: token for `ga-admin` by password grant (client `schema-tests`), create a RESOURCE schema from a small valid Turtle file, create a second version, list, retrieve content, check that `ga-viewer` gets 403 on create and no token gets 401, and that the Provider e-mail of D40 shows up in Mailpit.
3. `mvn verify` green.
4. Leave the stack running at the end.

Writing rules for all text you produce (docs, comments, logs): plain English, short sentences, never use em-dashes or en-dashes.

## Final report (under 400 words)

What was built, the result of the finish line, the new spec gaps beyond the digest, anything not implemented, and rough effort (fix iterations). Be factual; if something failed, say so plainly.
