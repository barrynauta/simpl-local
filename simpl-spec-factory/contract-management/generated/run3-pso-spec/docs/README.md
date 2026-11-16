# Contract Management, spec factory build (PSO run)

This folder holds a build of Simpl-Open Contract Management made from the specifications in `../../input` only:
the contract manager backend (CM), the contract UI backend (UIBE), the UI, for a Provider party and a Consumer
party, and the stub service that stands in for the connector, signer, VC issuer, wallet, catalogue and directory.

## Run it

```
cd build
docker compose -p cm-factory-pso up -d --build --wait
./smoke.sh        # a whole negotiation, a rejected offer, suspend and reactivate, expiry, 401 and 403
./pso-check.sh    # the 8 PSO scenarios of input/pso against the Provider CM
```

Stop with `docker compose -p cm-factory-pso down` (add `-v` to drop all data).

Requirements: Docker with Compose v2, curl, jq, uuidgen, python3 (only for one date sum in smoke.sh).

## Ports (D76)

| Service | Host URL |
|---|---|
| Keycloak | http://localhost:19080 (admin / admin) |
| Stub | http://localhost:19090 |
| Kafka | localhost:19092 |
| PostgreSQL | localhost:19432 (simpl / simpl) |
| Provider CM | http://localhost:19101/contract/v1 (key `test-cm-provider-key`) |
| Provider UIBE | http://localhost:19102/contract-ui/v1 |
| Provider UI | http://localhost:19103 |
| Consumer CM | http://localhost:19201/contract/v1 (key `test-cm-consumer-key`) |
| Consumer UIBE | http://localhost:19202/contract-ui/v1 |
| Consumer UI | http://localhost:19203 |

Users are those of D67 (for example `provider-admin`, `consumer-admin`).

## Build and test the code

```
cd build/backend && mvn verify      # Java 21, Maven 3.9
cd build/ui && npm ci && npm test && npm run build
```

`mvn verify` runs unit tests (state machine, validation order, proof algorithm with the Appendix E vector) and
integration tests (Spring Boot with MockMvc, H2, a mocked stub and mocked Kafka, signed test JWTs for UIBE).

## Layout

| Path | What |
|---|---|
| `backend/common` | Problem responses (D11), correlation id (D14), id formats (D16) |
| `backend/contract-manager` | CM |
| `backend/contract-ui-backend` | UIBE |
| `backend/stubs` | Stub service of Appendix C, including the eddsa-jcs-2022 proofs of Appendix E |
| `backend/Dockerfile` | One Maven build stage, one runtime target per service |
| `ui` | Vue 3 + Vite + TypeScript UI, served by nginx, which also proxies `/contract-ui/v1` |
| `keycloak` | Realm imports (D67) |
| `postgres/init.sql` | The four databases of D76 |
| `docs` | This README, architecture, API notes, decisions |

See `architecture.md`, `api-notes.md`, `decisions.md`, `../TRACEABILITY.md` and `../BUILD-LOG.md`.
