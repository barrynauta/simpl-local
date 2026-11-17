# Swap on the Rust backend

Run on 28 Oct 2026. Both UIs from the Java swap (run 1 build UI and upstream UI) on the Rust backend of run 2, by configuration only. Same approach as [`../swap/`](../swap/README.md).

```
RB=~/src/spec-factory-build-rust/build           # Rust build working copy (stack: compose project vocab-rust)
R=~/src/simpl-local/simpl-spec-factory/experiments/swap-rust
cd $RB && docker compose up -d --wait            # API 8097, Keycloak 8184
```

## Generated UI on Rust (`build-ui/`)

`nginx.conf` sends `/v1/` to `host.docker.internal:8097`; `config.json` points the UI at the Rust Keycloak (client `vocabulary-ui`, which the Rust realm defines).

```
docker run -d --name rust-swap-build-ui -p 4333:80 --add-host host.docker.internal:host-gateway \
  -v $R/build-ui/nginx.conf:/etc/nginx/conf.d/default.conf:ro \
  -v $R/build-ui/config.json:/usr/share/nginx/html/config.json:ro vocabulary-manager-ui:latest
```

## Upstream UI on Rust (`upstream-ui/`)

`add-keycloak-client.sh` adds client `frontend-cli` to the Rust Keycloak (lost when the container is removed). `nginx.conf` maps `/api/` to the Rust backend and `/auth/` to the Rust Keycloak. The upstream UI image listens on 8080.

```
$R/upstream-ui/add-keycloak-client.sh
docker run -d --name rust-swap-upstream-ui -p 4323:8080 --add-host host.docker.internal:host-gateway \
  -v $R/upstream-ui/nginx.conf:/etc/nginx/conf.d/default.conf:ro \
  -v $R/upstream-ui/env-config.js:/usr/share/nginx/html/env-config.js:ro simpl-vocabulary-manager-ui:local
```

## Running the suites

```
cd ../../tests; TK=http://localhost:8184/realms/simpl/protocol/openid-connect/token
UI_URL=http://localhost:4333 UI_AUTH=oidc API_URL=http://localhost:8097/v1 TOKEN_URL=$TK .venv/bin/behave features/ui
UI_URL=http://localhost:4323 UI_AUTH=oidc API_URL=http://localhost:8097/v1 TOKEN_URL=$TK .venv/bin/behave features/ui
../experiments/swap/exp1-newman/run.sh rust      # upstream Postman collection
```

Results in `results/` and `../swap/exp1-newman/results/rust.*`. See [findings](../../findings.md).
