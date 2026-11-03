# Contract Management, built from the specs

This folder holds a build of Simpl-Open Contract Management made only from the frozen input specs and the spec digest (`../input/spec-digest.md`). It contains:

- **Contract manager backend (CM)**: `contract-manager/`, Java 21, Spring Boot 3.5, base path `/contract/v1`.
- **Contract UI backend (UIBE)**: `contract-ui-backend/`, Java 21, Spring Boot 3.5, base path `/contract-ui/v1`.
- **Contract manager UI**: `ui/`, Vue 3 + Vite + TypeScript, served by nginx, which also proxies `/contract-ui/` to the UIBE of its party.
- **Stub service**: `stubs/`, Java 21, Spring Boot. It stands in for both connectors, the signer and VC issuer, the verifier, the wallets, the catalogue, the participant directory and the consent manager page (digest Appendix C).
- **Shared code**: `common/`: the `eddsa-jcs-2022` proof (Appendix E), JCS, base58, `did:key`, problem responses, correlation id, id checks, date formats.
- **Environment**: `docker-compose.yml` (project `cm-factory`), Keycloak realm imports in `env/keycloak/`, the Postgres init script in `env/postgres/`.

Each party (Provider `provider-01`, Consumer `consumer-01`) runs its own CM, UIBE and UI. The same images run at both parties; only the settings differ.

## Run

```bash
cd build
docker compose -p cm-factory up -d --build --wait
./smoke.sh
```

`--wait` returns when every container reports healthy. The first build downloads Maven and npm dependencies and takes a few minutes; later builds take about 30 seconds.

Stop and remove everything, including the data: `docker compose -p cm-factory down -v`.

## Host ports (D76)

| Service | URL from the host |
|---|---|
| CM Provider | http://localhost:19101/contract/v1 |
| UIBE Provider | http://localhost:19102/contract-ui/v1 |
| UI Provider | http://localhost:19103 |
| CM Consumer | http://localhost:19201/contract/v1 |
| UIBE Consumer | http://localhost:19202/contract-ui/v1 |
| UI Consumer | http://localhost:19203 |
| Stub | http://localhost:19090 |
| Keycloak | http://localhost:19080 (admin / admin) |
| Kafka | localhost:19092 |
| Postgres | localhost:19432 (simpl / simpl) |

## Keys and users

API keys (D77): `test-cm-provider-key`, `test-cm-consumer-key`, `test-uibe-provider-key`, `test-uibe-consumer-key`, connector stub `test-edc-key`.

Users (D67): realm `provider`: `provider-admin` / `ProviderAdmin1!`, `provider-operator` / `ProviderOperator1!`, `provider-norole` / `ProviderNoRole1!`. Realm `consumer`: `consumer-admin` / `ConsumerAdmin1!`, `consumer-norole` / `ConsumerNoRole1!`.

Get a token:

```bash
curl -s -d grant_type=password -d client_id=contract-ui -d username=consumer-admin \
  -d 'password=ConsumerAdmin1!' http://localhost:19080/realms/consumer/protocol/openid-connect/token
```

## Drive a negotiation by hand

```bash
# 1. an asset in the catalogue stub
curl -s -XPOST localhost:19090/_control/catalogue/assets -H 'Content-Type: application/json' \
  -d '{"assetId":"asset-demo-1","resourceName":"Air quality 2026","providerId":"provider-01"}'
# 2. start the negotiation; the stub runs existence check and offer creation
curl -s -XPOST localhost:19090/_control/negotiations -H 'Content-Type: application/json' \
  -d '{"providerId":"provider-01","consumerId":"consumer-01","assetId":"asset-demo-1"}'
# 3. accept as the Consumer user (UI at http://localhost:19203, or the UIBE)
curl -s -XPATCH -H "Authorization: Bearer $TOKEN" localhost:19202/contract-ui/v1/contractOffers/<nid>/accept
# 4. both CMs reach ACTIVE within a few seconds
curl -s -H 'X-Api-Key: test-cm-provider-key' localhost:19101/contract/v1/contracts/<nid>
```

## Tests

```bash
cd build
mvn verify
```

Unit tests cover the state machine, the validation order and the proofs (including the worked vector of Appendix E.3). Integration tests start CM, UIBE and the stub with H2 (PostgreSQL mode), mocked outbound clients and mocked JWTs. `smoke.sh` runs against the real stack.

## More

- [architecture.md](architecture.md): component map, the two parties, the flows.
- [api-notes.md](api-notes.md): the API as built, including the added operations.
- [decisions.md](decisions.md): how each digest decision is implemented.
- [../TRACEABILITY.md](../TRACEABILITY.md) and [../BUILD-LOG.md](../BUILD-LOG.md).
