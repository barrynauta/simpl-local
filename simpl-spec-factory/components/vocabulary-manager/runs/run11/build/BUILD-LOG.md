# Build log, run 11 (20 Nov 2026)

## What was built

Backend (Java 21, Spring Boot 4.1.3, Jena 6.2.0, Nimbus 10.10, Resilience4j 2.4.0), Fuseki 6.2.0
store image, Keycloak 26.8.2 realms, Vue 3 UI with nginx proxy, Docker Compose stack with two backend
replicas, `stack.sh`, `smoke.sh`, backup and restore scripts, docs. All 19 operations of the
contract are implemented with the decided changes; nothing in scope was left out.

## Choices where the specs and decisions still left room (new gaps)

1. **S4 language tag case.** Jena normalises language tags to lower case, so `"b"@EN` and `"c"@en`
   count as one language and the location shows `language 'en'`. No decision says how tags compare
   or how they are written in the location.
2. **S5 granularity.** One issue per resource and per disjoint pair of its types (a resource typed
   with three pairwise disjoint classes gives three issues). D65 fixes the location only.
3. **S3 position.** The S3 issue is placed at the later of the two declarations in file order.
4. **Prefix declared twice.** Each declaration gives its own warning or S7 error (D11 says "each
   reference").
5. **Namespaces like `<contentBase>/content/<Name>/other#`.** Neither D12 nor D69 covers a tail that
   is not `/versions/<v>`; treated as external (warning externalReferenceSkipped).
6. **External name conflict text.** Not decided; the build says "External vocabulary with name
   '<Name>' already exists." Name conflicts are checked before namespace and alias conflicts.
7. **Empty optional external description** (blank after trimming) is treated as absent.
8. **GET /health with a query parameter** gives 400 (D21 read as covering every GET).
9. **JSON body size problem.** 413 with type payloadTooLarge, issue `in: body, name: body`, detail
   "The request body is larger than 1 MB." (only status and type are decided).
10. **Accept: application/problem+json on a JSON operation** is accepted (D63) and the 200 body is
    sent as `application/json` (D127 reasoning applied).
11. **Store-down backend health.** D44 makes GET /health answer 503 DOWN when the store is down, so the
    container health check of the store-down backend uses the liveness probe on the management
    port instead; otherwise `--wait` could never succeed.
12. **Paging links for searches** are omitted (D38) and `totalPages` is 0 for an empty result.
13. **Version list `sort=name`.** All rows share the name, so the order is the tie-break (version
    descending), for both `name` and `-name`.
14. **Proxy internal path.** nginx answers `/__problems/...` (its own error page files) with its HTML
    404; every other path is proxied.
15. **Content storage.** Bytes are stored as one `xsd:base64Binary` literal per version (D106 allows
    one literal or chunks). TDB2 grows with each write; compaction is an operations task with the
    store stopped, not decided anywhere.
16. **Conditional write retries** use a fresh read of the latest version number and a short random
    pause (20 to 70 ms times the attempt); the content is not validated again (D92).

## Problems met and how they were solved (fix iterations of the stack: 5)

1. Fuseki did not start: the tmpfs for the generated password file belonged to root. Fixed with a
   fixed uid 10001 in the image and `uid=` on the tmpfs mounts.
2. Backend did not start: Spring Boot 4 no longer brings SnakeYAML for `application.yaml`. Added it
   as a dependency.
3. Odd-input probing through the proxy gave nginx 500 pages: `error_page 400 = @named` fails for
   requests nginx cannot parse ("empty URI in redirect to named location"). Replaced the named
   locations with internal static problem files. In the same round the store-down backend never got
   healthy, because the management port was behind the API security chain (401 on
   `/actuator/health`); added a separate security chain for the management port that permits only
   health and Prometheus.
4. Schemathesis found TRACE answered by nginx without an Allow header; added a path-to-methods map
   in nginx.
5. Trivy found 3 HIGH in the UI base image (musl, zlib) with fixed versions; added `apk upgrade` of
   those packages.

Smaller: Vitest 5 reports `mockRejectedValue` with a pre-built error as an unhandled rejection when the
test also uses `beforeEach(mockReset)`; tests use throwing implementations instead. A shared test
fixture object was mutated across tests; tests now copy it. Jena 6 moved result parsing classes;
`ResultSetMgr` is used.

## Checks run on the finish line

- `./stack.sh up` from a clean state (volumes removed): see the last section.
- `./smoke.sh`: 13 of 13 passed on the two-replica stack.
- `mvn verify`: 49 tests, 0 failures; Spotless check passed; JaCoCo line 93.3 %, branch 82.3 %.
- `npm run build` (in the UI image build): ESLint, Prettier check, vue-tsc, 18 Vitest tests, Vite build.
- Trivy: see SECURITY-SCAN.md.
- Schemathesis 4.33.4 (temporary venv, not part of the deliverable) against the published OpenAPI,
  30 examples per operation, seed 1: 1241 generated requests, **no 5xx**. With all checks on, the
  remaining findings are decided contract changes (int32 bounds, relaxed name filter, unknown fields
  rejected, empty arrays rejected, D79) and the TRACE Allow header, which was then fixed.
- Own odd-input probe through the proxy (`TRACE`, `QUERY`, encoded slash, `%00`, bad percent
  encoding, `//`, `;`, huge integers, `/actuator/prometheus`, garbage request line, 8 MB, 6.2 MB and
  5.3 MB uploads): every answer is problem+json with 400, 401/404, 405 or 413; no 5xx.
- Store access check (D83): anonymous query 401, reader cannot update (403), admin endpoints 404 (the
  plain Fuseki server), only `/$/ping` open; the store publishes no host port.
- Store down (`./stack.sh store-down-up`): GET /v1/health 503 `DOWN`, GET /v1/vocabularies 503 with
  Retry-After 5 and the D44 detail.
- Backup and restore round trip: PASS.
- `tools/parallel-check.sh`: 4 parallel new versions over two replicas, versions 2 to 5 once each.
- Browser check (Playwright for Python in a temporary venv, not a deliverable): login as ga-admin,
  list columns, create with the browser syntax check, success notice, Deprecate with the icon,
  Update with the version line, version panel and deprecation banner; no console or CSP errors.

## What does not work or was not done

- Keycloak image keeps 6 HIGH findings (vendor image, see SECURITY-SCAN.md).
- Q1 measured at a reduced seed; Q6 (axe) not measured.
- No CI pipeline, no Helm chart, no TLS in the local stack (see DEVIATIONS.md).
