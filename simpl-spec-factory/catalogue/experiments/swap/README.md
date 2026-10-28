# Swap experiments: Catalogue

Run on 28 Oct 2026, after run 1 of the build and the UI browser steps. Configuration only: no source file of either implementation was changed. Upstream is the local stack of May 2026 (the earlier `development/gaia-x-edc` repositories): catalogue backend on 8081, adapter on 8084, UI on 4321, one data space, no Keycloak, no client backend. The build is the factory stack of data space A (catalogue 18101, adapter 18102, client backend 18103, UI 18104).

The catalogue has more joints than the earlier candidates: UI to client backend, client backend to adapter, adapter to catalogue backend. Both sides set these links in configuration, so each joint was crossed on its own, and the black-box suite was run over each combination.

## Summary

| Combination | Suite scenarios passed | Reference |
|---|---|---|
| Build adapter on the build catalogue | 63 of 63 adapter scenarios | build run |
| **A.** Build adapter on the upstream catalogue | 39 of 63 | |
| **E.** Upstream adapter on the build catalogue | 20 of 63 | upstream adapter on its own catalogue: 11 of 46 (no tokens, role checks skipped) |
| Build client backend on the build adapter | 68 of 68 client backend scenarios | build run |
| **B.** Build client backend on the upstream adapter and catalogue | 55 of 68 | |
| Build UI on the build | 26 of 26 UI scenarios | step 4 |
| **C.** Build UI, through the build client backend, on the upstream adapter and catalogue | 21 of 26 | |
| **D.** Upstream UI on the build adapter and catalogue | hand walk: quick search and detail work, same calls as on upstream | upstream UI on upstream: same walk; suite 0 of 26 (selectors) |

Federation scenarios are left out: upstream has one data space.

Quick search, advanced search and the resource description itself cross every joint. The differences are one contract point (the status values), the newer stories upstream does not have yet (quality filters and scores, the detail endpoint of the client backend), and role and error handling.

## A. Build adapter on the upstream catalogue

```
X=~/src/simpl-local/simpl-spec-factory/catalogue/experiments/swap
cd ~/src/spec-factory-build-cat/build
docker compose -p simpl-catalogue-factory -f docker-compose.yml -f $X/qma-on-upstream-cat.override.yml up -d --no-deps --wait qma-a
cd $X/../../tests && CAT_A_URL=http://localhost:8081 .venv/bin/behave --tags=@qma --tags=~@federation --tags=~@ui
```

39 of 63. Restore with the same `up` command without the override file.

- **Status values, 17 scenarios:** the OpenAPI enum `SelfDescriptionStatus` is `active` and `revoked`, and the build sends `statuses=active`. Upstream's catalogue only accepts upper case and answers 400 ("Failed to convert ... for value [active]"). Upstream does not follow the current contract here.
- **Newer stories:** no quality scores or filters (5), no punctuation in a quick search string (500 from upstream).
- **Revoke:** upstream's catalogue answers 403 to a token it does not know.

## B. Build client backend on the upstream adapter

```
docker compose -p simpl-catalogue-factory -f docker-compose.yml -f $X/cbe-on-upstream-qma.override.yml up -d --no-deps --wait cbe-a
CAT_A_URL=http://localhost:8081 .venv/bin/behave --tags=@cbe --tags=~@federation --tags=~@ui
```

55 of 68. Quick and advanced search work across the joint, grouped per catalogue. The 13 failures: the detail lookup through the Governance Authority is not in the May adapter (404, a newer story), no quality scores or filters, paging order, and the revoke of experiment A.

## C. Build UI on the upstream adapter and catalogue

Same configuration as B, then `CAT_A_URL=http://localhost:8081 .venv/bin/behave features/ui --tags=~@federation`. 21 of 26. The 5 failures: the detail page (the detail endpoint of B), and the quality filters and scores.

## D. Upstream UI on the build

```
$X/D-upstream-ui-on-build/run.sh                 # second upstream UI container on 4326
P=$X/../../tests/.venv/bin/python
$P $X/walk_upstream_ui.py http://localhost:4326 http://localhost:18101 oidc
$P $X/walk_upstream_ui.py http://localhost:4321 http://localhost:8081 none     # baseline on upstream
docker rm -f swap-upstream-ui-on-build
```

The upstream UI calls the APIs from its own server routes, with the token from its cookie `token`; the walk sets that cookie to a real token of the build's Keycloak. Results: `results/D-upstream-ui-on-build-walk.txt`, `results/base-upstream-ui-walk.txt`.

- Quick search finds the description published to the build, and the detail page shows all its properties. The calls and statuses are the same as on upstream's own stack.
- Both stacks answer 404 on `/api/schemas`: the advanced search form needs an SD Tooling service that neither stack has.
- On upstream's own stack the detail page also shows a "Get Data Offer Details" block with the price, and calls a connector endpoint (500). On the build it does not. Both backends return the same body for the description, so this follows the sign-in state of the page (anonymous on upstream, signed in on the build), not the contract.

## E. Upstream adapter on the build catalogue

```
$X/E-upstream-qma-on-build-cat.sh                # second upstream adapter container on 8087
QMA_A_URL=http://localhost:8087/v1 .venv/bin/behave --tags=@qma --tags=~@federation --tags=~@ui
docker rm -f swap-upstream-qma-on-build
```

20 of 63; the upstream adapter on its own catalogue passes 11 of 46 of the same scenarios (run without tokens, so the role scenarios drop out). Quick search through the upstream adapter finds descriptions in the build's catalogue. The failures: the upstream adapter does not pass the token on every call (401 from the build), endpoints of newer stories are missing (plain 404), no quality filters, `iana:` problem types and plain JSON errors where D2 asks for Problem JSON.
