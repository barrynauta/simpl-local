# Step 3b: code writer, Rust backend

Run 2 of step 3 with a different language: the same input as run 1 (specs and spec digest), a backend in Rust instead of Java, no UI. The question: do the specs carry over to another stack, or do they lean on Java and Jena without saying so? The result is tested with the same black-box suite, and then the two existing UIs (the run 1 build and upstream) are put on top of it.

Given to a fresh general-purpose agent. Paths are placeholders (see [README](./README.md)).

---

You are the **code writer** in a spec-driven software factory experiment. The question: how far can a component be built from its specifications alone? You build the **backend** of the Simpl-Open **Vocabulary Manager** in **Rust**, from the specs, nothing else. A separate test writer has already written a black-box test suite from the same specs; you will NOT see it. Your build will be judged against that suite, so build what the specs and decisions say, not what you guess a test wants.

## Workspace and isolation (strict)

- Work ONLY inside `{WORKSPACE}/`.
  - Inputs (read-only): `input/specs/` (user stories in `stories/`, OpenAPI in `api/`, functional descriptions in `functional/`, an index `README.md`) and `input/spec-digest.md`.
  - Output: everything you produce goes in `build/`.
- Do NOT read anything outside that folder. In particular never look at the factory repository or any other build of this component on this machine (they contain the tests, an upstream implementation and another generated implementation), and never fetch or search for upstream Simpl source code (code.europa.eu, GitHub, the web). The experiment measures the specs; using other implementations would invalidate it. Downloading ordinary libraries via crates.io and images via Docker Hub is fine, and so is reading library documentation.
- Do not git commit or push anything. Do not touch other containers or ports.

## Read first

1. `input/spec-digest.md`: the source hierarchy (OpenAPI > stories > R07.02 > BP02B), scope (32 stories; SIMPL-393, SIMPL-395 and SIMPL-28680 are out of scope), and 30 numbered decisions D1 to D30 that resolve contradictions and gaps. **Follow these decisions exactly.** Pay special attention to D3/D4 (roles, 401/403 as Problem Details), D6 (identity provider and test-user contract), D7 to D11 (versioning and status rules), D15 to D20 (validation rules and the reference convention), D23 (name filter is case-insensitive contains), D29 (pageSize max 100), D30. Decisions about the UI (D21, D22, D24 to D26, D28) are out of your scope, except where they define API behaviour.
2. `input/specs/api/vocabulary_openapi-tier1.yaml`: the API contract. Implement every operation in it (internal vocabularies, searches, content download, external vocabularies, health), same paths under base path `/v1`, same payloads, same status codes, Problem Details (`application/problem+json`) for errors. Add 401 and 403 as per D4.
3. All story files in `input/specs/stories/` (acceptance criteria in Given/When/Then; read the UI stories too, for the behaviour the API must support) and the two functional files.

## What to build (stack is fixed)

- **Backend:** Rust, stable toolchain, Cargo. Web framework axum (tokio). Persistence in **Apache Jena Fuseki** (D27) over HTTP (SPARQL 1.1 query and update, Graph Store Protocol as needed). Turtle parsing and validation with the oxigraph crates (`oxttl`, `oxrdf`) or another maintained Rust RDF library. JWT validation against the local issuer's JWKS; roles read from a `roles` claim (array): `GA_VOCABULARY_VIEWER` read, `GA_VOCABULARY_ADMIN` read and write (D3). `/v1/health` is public. Configuration through environment variables. Multi-stage Dockerfile, small runtime image, not running as root.
- **No UI.** Two existing UIs will be put on your backend later. Answer CORS preflight for `http://localhost:4333` and `http://localhost:4323`, so a browser UI on those origins can call the API directly.
- **Identity provider:** Keycloak in the compose file with a realm imported at startup, exactly per D6: password grant enabled for public client `vocabulary-tests`, users `ga-admin` / `ga-viewer` / `ga-norole` with password `password`, roles delivered in a top-level `roles` array claim of the access token, plus a public client `vocabulary-ui` (authorization code + PKCE, redirect URIs and web origins for `http://localhost:4333/*` and `http://localhost:4323/*`). Make sure the token issuer the backend accepts matches tokens fetched from the host (localhost) and from inside the network; solve this cleanly (e.g. hostname settings) and document it.
- **docker-compose.yml** in `build/`: everything starts with a plain `docker compose up` (no manual steps, no external accounts). Host ports (others are in use on this machine, do not use 8086, 8096, 4323, 4333, 3031, 3041, 8080, 8180, 8182, 8183, 8190):
  - API (backend) at `http://localhost:8097/v1`
  - Keycloak at `http://localhost:8184`, token endpoint `http://localhost:8184/realms/simpl/protocol/openid-connect/token`
  - Fuseki not exposed, or at 3042 if you need it.
  - Use healthchecks so `docker compose up --wait` returns when the system is usable.
- **Your own tests:** Rust unit tests for domain logic and validation rules, and integration tests with mocks (e.g. an in-memory store behind a trait, and JWTs signed with a test key). Do NOT write Gherkin; that is the test writer's job. Run `cargo test` and make it green; `cargo clippy` without warnings is a plus.
- **Documentation** in `build/docs/`: `README` (what it is, how to run), architecture overview, API notes, and how the decisions D1 to D30 are implemented.
- **`build/TRACEABILITY.md`:** a table mapping each of the in-scope API stories and the API behaviour of the UI stories, and each decision D1 to D30, to the code (and your own tests) that implement it; mark anything not implemented or out of scope, with the reason.
- **`build/BUILD-LOG.md`:** an honest log: every place where the specs and digest still left you guessing and what you chose (spec gaps beyond the digest are valuable findings), anything where the specs seem to assume a particular language or library, problems you hit and how you solved them, what does not work at the end. No cherry-picking.

## Finish line

1. `cd build && docker compose up -d --build --wait` succeeds from a clean state.
2. Your own smoke check with curl: get a token for `ga-admin` via the password grant, create a vocabulary from a small Turtle file, create a second version, list, download, and check that `ga-viewer` gets 403 on create and no token gets 401. Put the smoke script in `build/smoke.sh`.
3. `cargo test` green.
4. Leave the stack running at the end.

Writing rules for all text you produce (docs, comments, logs): plain English, short sentences, never use em-dashes or en-dashes (use commas, colons, parentheses or a plain hyphen).

## Your final report (keep it under 400 words)

What was built, the result of steps 1 to 4 above, the list of new spec gaps you found beyond the digest, anything that looked Java- or Jena-specific in the specs, anything not implemented, and rough effort (how many fix iterations for the build to work). Be factual; if something failed, say so plainly.
