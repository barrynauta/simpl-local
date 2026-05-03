# Catalogue UI — manual setup walkthrough

This walkthrough is the manual equivalent of the UI portion of `./start.sh`. Use it when you want to understand each step, debug a build failure, or run the UI standalone without `start.sh`. For a one-shot setup, see the [main README](../README.md#quick-start).

The UI cannot run usefully on its own — it talks to fc-service for browse and to xfsc-advsearch-be for search. fc-service is the only one wired in our local stack; without advsearch-be, search returns errors but browse and direct-URL views work. Make sure you've run [`docs/fc-service-manual-setup.md`](fc-service-manual-setup.md) first (or `./start.sh` through the fc-service phase).

After completing the steps below you will have:

- `simpl-catalogue-client` running on `:4321`
- A browser UI that lists / shows individual self-descriptions from fc-service
- Search returning errors gracefully (xfsc-advsearch-be is not deployed)

---

## 1. Clone the upstream catalogue client source

```bash
mkdir -p repos
git clone https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-catalogue-client.git repos/simpl-catalogue-client
```

`repos/` is gitignored, so the cloned upstream code is never committed back to this repo.

## 2. Write a build-time `.env` file in the cloned source

The UI is built with Astro + dotenvx. PUBLIC_ env vars are baked into the client bundle at build time via `dotenvx run -- astro build`, which reads `.env` from the project root. Drop a file with our local URLs:

```bash
cat > repos/simpl-catalogue-client/.env <<'EOF'
PUBLIC_AUTH_KEYCLOAK_SERVER_URL=
PUBLIC_AUTH_KEYCLOAK_REALM=
PUBLIC_AUTH_KEYCLOAK_CLIENT_ID=
PUBLIC_FEDERATED_CATALOGUE_API_URL=http://localhost:8081
PUBLIC_SEARCH_API_URL=http://localhost:8081
PUBLIC_SEARCH_API_VERSION=v1
PUBLIC_CONTRACT_CONSUMPTION_API_URL=
PUBLIC_CONTRACT_CONSUMPTION_API_VERSION=v1
PUBLIC_AGENT_TYPE=consumer
USE_MOCK_APIS=false
EOF
```

Empty Keycloak values disable the auth flow. The same `localhost:8081` URL is used for both `PUBLIC_FEDERATED_CATALOGUE_API_URL` (browse) and `PUBLIC_SEARCH_API_URL` (search) — see Architecture for the networking trick that makes a single URL string work in both browser and SSR contexts.

This file is overwritten by `start.sh` on every run, so editing it directly is fine for ad-hoc experiments but won't persist if you re-run `start.sh`.

## 3. Build the Docker image

```bash
cd repos/simpl-catalogue-client
docker build -t simpl-catalogue-client:local .
cd ../..
```

First run: ~3–5 minutes (downloads `node:22` base image, runs `npm install`, runs `astro check && astro build`). Subsequent builds: under a minute thanks to layer cache, but **note**: changing the `.env` file does NOT invalidate the cache automatically, because the upstream Dockerfile does `COPY . .` before `RUN npm install && npm run build`. To force a rebuild after editing `.env`:

```bash
docker rmi simpl-catalogue-client:local
docker build -t simpl-catalogue-client:local repos/simpl-catalogue-client
```

## 4. Run via compose

The UI is wired into the main `docker-compose.yml` with `extra_hosts: ["localhost:host-gateway"]` (see Architecture for why) and a complete `environment:` block setting the same PUBLIC_ vars again. From the project root:

```bash
docker compose up -d ui
```

This brings up just the UI container (assumes postgres / neo4j / fc-service are already running). To start the whole stack:

```bash
docker compose up -d
```

## 5. Verify

```bash
docker logs simpl-cat-ui --tail 5
```

Expect:

```
@astrojs/node Server listening on
  local: http://localhost:4321
  network: http://192.168.x.x:4321
```

Open `http://localhost:4321` in your browser. The home page is a quick-search box. Search itself returns errors gracefully because xfsc-advsearch-be is not deployed — that's expected.

To verify a real catalogue entry renders end-to-end (assuming you've run `./seed.sh` so the catalogue has at least one self-description), navigate directly to:

```
http://localhost:4321/resourceDescriptions/<URL-encoded SD id>
```

For the seed-shipped DataOffering whose id is `did:web:registry.gaia-x.eu:DataOffering:fMw2UtNCDW-ydI83YKscqCKa-n75jJ0qY7v0`, that's:

```
http://localhost:4321/resourceDescriptions/did%3Aweb%3Aregistry.gaia-x.eu%3ADataOffering%3AfMw2UtNCDW-ydI83YKscqCKa-n75jJ0qY7v0
```

You should see the SD's offer details, billing schema, contract template, and SLA agreements rendered as a structured page.

---

For limitations, the env-var build-time/runtime split, and the localhost-as-host-gateway trick, see [`docs/catalogue-ui-architecture.md`](catalogue-ui-architecture.md).
