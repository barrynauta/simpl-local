# Build log

Code writer log for the Schema Manager build. Date: 2026-10-28. Inputs read: `input/specs/`
(55 stories, OpenAPI 1.1.0, BP02C) and `input/spec-digest.md`. Nothing outside the workspace was
read. No Simpl source code was consulted.

## 1. Sequence

1. Read the digest, the OpenAPI (in-scope paths and components) and all 55 stories.
2. Backend: data model and Flyway migration, repositories, validator, service with the slot rule,
   controllers, security, notifiers. Unit tests and Testcontainers integration tests.
3. Containers: backend image, Vocabulary Manager stub, realm export, UI image, compose file.
4. Stack up, `smoke.sh`, manual checks (issuer from inside the network, webhook to
   `host.docker.internal`, semantic validation against the stub, Vocabulary Manager stopped).
5. UI: own Playwright check of the main flows as `ga-admin`, `ga-viewer`, `ga-norole`.
6. Documentation, traceability, this log. Final clean restart (`down -v`, `up --build --wait`)
   and smoke run.

## 2. Problems and how they were solved

| Problem | Fix |
|---|---|
| Java compile error: multi-catch of `InvalidMediaTypeException` and its superclass | Catch `IllegalArgumentException` only |
| Test compile error: `jwt().authorities(List<SimpleGrantedAuthority>)` does not match the generic signature | Tests use the production converter `SecurityConfig::roles`, which also tests the `roles` claim mapping |
| Testcontainers 1.21.3 (managed by Spring Boot 3.5.6) could not talk to the local Docker Engine 29 (minimum API 1.40) | Pinned `testcontainers.version` to 1.21.4 in the pom |
| One integration assertion was wrong: the SEARCH test also creates a RESOURCE twin of the same name, which correctly sends mail | Assertion now checks that no mail mentions the SEARCH-only name |
| Own UI check: waited for "any notification" while an older success message was still shown, then assumed a notification was present | Fixed in the check script (close the old one if present). The UI itself was not changed |

The compose stack came up healthy on the first attempt. `smoke.sh` passed on its first run.
Rough effort: 5 fix iterations in total, none in the delivered behaviour after the first stack start.

## 3. New spec gaps (beyond the digest)

Each entry: where the specs and the digest still left a choice, and what this build chose.

### API

1. **No metadata read for one concept.** D54 wants the details page header to show title, schema
   type, resource type and status, but D25 version items have no title and there is no
   `GET` returning one concept's metadata (D9 returns content). Choice: version items carry two
   additive fields, `title` and `description`. Finding: add a concept metadata representation
   (for example `GET /schemas/{name}` with `Accept: application/json`, which D8 now forbids).
2. **`totalPages` with zero items.** Not specified. Choice: 0.
3. **`next` content.** D26 says "keeping all filters". Choice: the given `status` and
   `resourceType`, plus the effective `schemaType` always (also when defaulted), then `page` and
   `pageSize`.
4. **resourceType mismatch: 404 or 400.** D11 says 404 on by-name operations, D20 says 400 on a
   new version. Both applied as written (GET and PATCH: 404; POST version: 400). Finding: the
   digest is not uniform here.
5. **Empty versus missing file.** Not specified. A missing `schemaFile` part gives 400
   `Required field 'schemaFile' is missing.` (metadata stage). A present but empty file goes to
   content validation and gives `Syntax Error: no sh:NodeShape found`.
6. **`schemaFile` as a text field.** D29 says filename and part type are not checked. Choice: a
   plain form field named `schemaFile` (no filename, as `curl -F schemaFile=<file` sends it) is
   accepted too.
7. **Empty `resourceType` field on a SEARCH create** (`resourceType=`). Choice: treated as absent.
   On RESOURCE it is "missing" (400).
8. **Duplicate event names in one subscription.** OpenAPI has `uniqueItems: true`, no story says
   what happens. Choice: 400. The order of validation is: JSON body, `targetUrl` (400), `events`
   present and non-empty (400), each value known (406), duplicates (400), existing subscription (409).
9. **Event order in `GET /webhooks`.** Choice: as submitted; subscriptions in creation order.
10. **Vocabulary Manager answers 4xx other than 404, or returns content that is not Turtle.**
    D36 covers only "no answer, 5xx, timeout". Choice: treated as "vocabulary does not exist"
    (`localReferenceUnavailable`).
11. **"A shape defined in the same document" (D35 check 3).** Choice: the target node is the
    subject of at least one triple in the document (inline blank node shapes count).
12. **Default properties with several node shapes.** D32 says one shape must have them all. For
    the error, the issues list the properties missing from the shape that misses the fewest.
13. **Complex `sh:path` (blank node property paths).** Ignored by the default property check and
    by the semantic checks; only IRI paths count.
14. **`location` format of semantic issues.** D35 asks for "the property path and the IRI".
    Choice: `property '<path IRI>' sh:<predicate> '<IRI>'`, or `shape '<shape>' ...` without a path.
15. **Semantic error detail text.** Choice: `Semantic Validation Error: N semantic issue(s) found. `
    followed by every issue description.
16. **Syntax error issues.** Not required. Choice: one issue `syntaxError` with the parser position
    (`line: x, col: y`), or `noNodeShape`.
17. **Unsupported method.** Not in D63. Choice: 405 with type `urn:problem-type:simpl:methodNotAllowed`.
    Oversized upload (over 10 MB): 413 `payloadTooLarge`. Neither appears in the specs.
18. **401 detail.** Choice: `Invalid or expired token` (OpenAPI example) also for a missing token.
19. **Timestamps.** ISO 8601 UTC with milliseconds (`...47.165Z`); webhook `occurredAt` and the
    e-mail `Timestamp` are truncated to seconds, as in the D43 example.
20. **Revoke e-mail and PATCH webhook: which version.** Choice: the highest version and its changelog.
21. **D44 retry timing.** "Retried twice (after 1 s and 5 s)" read as: second attempt 1 s after the
    first failure, third attempt 5 s after the second failure.
22. **Accept on the latest-content endpoint.** D8 reserves `application/json` metadata for D10.
    Choice: `GET /schemas/{name}` with `Accept: application/json` gives 406.
23. **Content-Type of content responses.** Choice: exactly `text/turtle`, no charset parameter.
24. **Name order.** D26 says name ascending. Choice: code point order (`collate "C"`), so
    upper-case letters sort before lower-case ones.
25. **Unmatched paths and authentication.** D62 says out-of-scope paths answer 404. Choice:
    paths outside `/schemas` and `/webhooks` need no token and give 404 problem JSON; a GET under
    `/schemas` that matches no operation (for example `/schemas/{name}/qualityProfiles/active`)
    still needs a viewer role first.
26. **Token signing key location.** D59 says the service validates "issuer and JWKS" against
    `{KEYCLOAK_URL}/realms/simpl`, but `localhost:8185` cannot be reached from inside the backend
    container. Choice: an extra setting `KEYCLOAK_JWKS_URL` (internal Keycloak host) next to the
    D60 names; the issuer check still uses `KEYCLOAK_URL`. Finding: D60 needs an internal URL name.

### UI

27. **Actions column on the details page.** D54 lists seven columns, but D57 puts a download
    action on every row. Choice: an eighth column `Actions`. A test that compares the full header
    list to the seven names would fail on this.
28. **"Any filter" for the empty-state text (D53).** Choice: status set, resource type set, or
    schema type changed to SEARCH counts as a filter.
29. **New version success message.** Not specified. Choice: `Schema version {version} created
    successfully.` on the details page after the redirect.
30. **Read-only schema type on the new version form (D50).** Choice: a disabled `<select>` with
    `data-testid="field-schema-type"`.
31. **Inline validation timing (D49, D50).** Choice: an inline message appears once a field has
    been touched; `submit-button` is disabled from the start until the form is valid.
32. **Notification lifetime.** Not specified. Choice: one shared slot for `notification-success` or
    `notification-error`, shown for 15 s or until replaced or closed. It survives route changes.
33. **Role check in the UI.** Choice: roles are read from the access token; a user without a schema
    role sees the permission message without any API call. An API 403 shows the same message.
34. **Component library.** The brief allows "a plain component library of your choice". Choice:
    none; native HTML controls and plain CSS, so each `data-testid` sits on the real control.
35. **Date display.** Choice: `YYYY-MM-DD HH:mm:ss` in UTC.

### Environment

36. **Realm mapper placement.** D59 says "a mapper on a default client scope". Choice: the same
    realm role mapper (`roles`, multivalued, access token) is defined on each of the two clients.
    Defining a `clientScopes` list in the import would replace Keycloak's built-in scopes
    (`profile`, `basic`, and so on). The token claim is the same; only the place differs.
37. **Provider recipients and instructions URL.** D39 and D40 leave the values open. Compose uses
    `providers@simpl.test` and `http://localhost:4334/schemas`.
38. **UI container user.** The brief asks for a non-root backend container (done, uid 10001). The UI
    image is the stock `nginx:alpine`, whose master process runs as root.

## 4. What does not work or is not covered

- SIMPL-28815 is out of scope (digest 2.2) and not built. `/vocabularies` and the quality profile
  operations are not built (D62).
- Tier 2 credentials and the gateway (SIMPL-24175) are not built (D7).
- No frontend unit tests (optional in the brief). The UI is covered only by the own Playwright
  check, which does not exercise list paging, sorting clicks, the SIMPL-24173 and SIMPL-24174
  messages, or download errors.
- Webhook retries (D44) and concurrent slot transitions (D16) are implemented but have no
  automated test. The slot rule is backed by a database index, so a race can only end in a 409.
- 500 paths for unexpected server errors exist but cannot be triggered by a test.
- `mvn verify` needs Docker for the Testcontainers integration tests. The Docker image build
  skips tests for that reason.
- The data volume persists between `docker compose up` runs. Use `docker compose down -v` for an
  empty store. At hand-over the stack was restarted from a clean volume and only `smoke.sh` ran
  against it.
