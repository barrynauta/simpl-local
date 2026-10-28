# Build log

Honest log of the build. It lists every point where the specs and the digest still left a choice,
the problems met, and what does not work at the end.

## Inputs used

Only `input/specs/` and `input/spec-digest.md`. Public material used: the W3C JSON-LD contexts
`https://www.w3.org/2018/credentials/v1` and `https://w3id.org/security/suites/jws-2020/v1`, downloaded
once and bundled in `backend/common/src/main/resources/contexts/` (D24); ordinary Maven, npm and Docker Hub
artefacts. No Simpl or XFSC source was read.

## New spec gaps (beyond the digest)

These are places where the digest still did not decide, and what this build chose. Each one is a finding.

1. **Threshold value field of the MQP.** Section 6.5 names `ScoreRange`, `minScore`, `maxScore`,
   `rangeClassification`, `Threshold`, `classificationScope`, `includesDimension` and `includesThreshold`,
   but not the property that holds the threshold number. The stub uses `thresholdValue`; the parser in
   cat also accepts `value`, `threshold`, `minScore` and `score`. A test writer reading the profile could
   look for another name.
2. **Typed non-integer numbers in JSON-LD.** JSON-LD 1.1 toRdf turns a number with a fraction into the
   xsd:double lexical form, even when `@type` is `xsd:decimal`. So `{"@type":"xsd:decimal","@value":100.5}`
   becomes the ill-formed literal `"1.005E2"^^xsd:decimal` and fails `sh:datatype xsd:decimal`. D63 only
   covers plain JSON numbers. This build rewrites a numeric `@value` that has a `@type` to its plain decimal
   text before SHACL validation, so 100.5 conforms.
3. **Problem type for revoke or delete by a non-owner.** D16 and D17 say "owner only (403)" but not which
   type. This build uses `urn:problem-type:simpl:forbidden`; `providerMismatch` is used only in the
   publication pipeline.
4. **Missing `dct:conformsTo`.** D59 says it is required; D14 does not list it in the syntax step. This
   build answers `422 schemaNotSupported` (step 5), not `400`.
5. **Malformed `simpl:version`.** A version that is not an integer of at least 1 is treated as syntax:
   `400 badRequest`. D13 only covers valid numbers.
6. **`Location` encoding.** D12 says "urlencoded id". This build uses form URL encoding of the whole id
   (`:` becomes `%3A`), the same as `encodeURIComponent` for ordinary ids.
7. **Webhook body strictness.** The contract requires `data.schema.uri`, `title` and `status`; D60 adds
   `name`. This build requires `eventId`, a valid `eventType`, `timestamp`, `data.schema` and
   `data.schema.name`, and does not reject a missing `uri`, `title` or `status`, because they are not used.
8. **@id values in quick search.** D27 excludes `@context`, `@type`, `rdf:type` and `simpl:servicePolicy`.
   It does not exclude `@id`, so `credentialSubject.@id` and `dct:conformsTo.@id` values are matched too.
   A search for `DataOffering` therefore matches every data offering through its id.
9. **Order of groups in cbe.** D38 says "the order of `catalogueIds` as sent, own catalogue first when
   present". This build reads it as: own catalogue first, then the others in the sent order.
10. **Paging fields of a multi-catalogue qma call.** D39 defines concatenation and the summed `total`, not
    `totalPages` or `next`. This build sets `totalPages` to the largest per-catalogue page count and gives
    a `next` link while any catalogue has another page. cbe never makes such calls.
11. **Check order on the Trustor side.** Token and trust (401, 403) are checked before the forwarded
    headers (400). Not stated.
12. **Empty `User-Attributes` versus the contract.** The adapter contract has `minLength: 1`; D37 allows an
    empty header. D37 is followed.
13. **Classification labels for federated-only searches.** Quality filters are not forwarded to Trustors
    (D41), so an unknown label would never reach a catalogue when only federated catalogues are selected.
    cbe therefore validates `minimumClassification` itself against the default profile (`400`).
14. **A dimension threshold without a dimension score.** If the profile has a threshold for a dimension
    that the QAR does not score, this build treats the QAR as invalid: `502 qualityAssessmentFailed`.
15. **QAR replacement and classification.** D55 says the summary is recomputed without gating; it does not
    say which profile classifies. This build uses the profile of the RD's schema, or the default profile
    when that one is missing. Any authenticated user can replace any QAR (D67 "N/A"); a finding.
16. **Rule names in the summary.** `SelfDescriptionQualityRuleResult.name` is required, but `RuleEvaluation`
    nodes have no defined name field. This build uses `name` when present, else the local name of `@id`.
17. **Keycloak claims "through a default client scope".** A realm import that defines its own
    `clientScopes` replaces the built-in ones (`roles`, `basic`, ...). To keep those, the two mappers
    (`participant_id`, `identity_attributes`) are defined on every client instead. The tokens look the
    same; the realm structure differs from the digest wording.
18. **UI operator controls.** D74 says "operator and value inputs". This build uses a `<select>` for
    `-op1` and `-op2` (values `=`, `>`, `>=`, `<`, `<=`; `op1` preselected `=`) and text inputs for the
    values. A test that types into the operator field would fail; one that selects an option works.
19. **UI result panels.** Panels of inactive tabs stay in the DOM but are hidden, so a test can find
    `results-panel-<id>` before clicking its tab, but must click the tab before checking visibility.
20. **UI detail paths.** D75 gives one example path. Arrays get index segments (`.../0`), and value objects
    give paths ending in `/@value`, `/@type` or `/@id` (for example `rdf:type/@id`).
21. **`/api/v1/catalogues` request.** The UI calls it without a query string, so a test that intercepts that
    exact path matches. Further pages are only fetched when `total` is larger than the first page.
22. **Accept with only `application/problem+json`.** Under D9 this does not accept `application/json`, so
    a successful response is `406`. Clients should send `application/json` or nothing.
23. **qma-c has no catalogue.** Its local search operations exist but have no catalogue behind them
    (`CAT_URL` points to an unresolvable host) and answer `500`. Only its federation side is meant to be used.
24. **A token of another realm on an ordinary path** is `401` (issuer not accepted), not `403`. D68 says
    which realms are accepted; it does not name the status for the others.
25. **`GET /self-descriptions/{id}` and `DELETE /self-descriptions/{hash}`** share one path template in the
    composed root. A GET with a hash is read as an id and gives `404`.
26. **POST /schemas response body.** The contract defines only the `Location` header; this build also
    returns `{"schemaId": ...}`.

## Choices inside the decisions

- Persistence: PostgreSQL through JDBC, VC stored as JSON text. Search filters in memory over the active
  RDs. Correct for a test environment, not for large catalogues.
- JCS: `io.github.erdtman:java-json-canonicalization`. The test signer (`tools/sign-rd.mjs`) implements JCS
  with sorted keys and ECMAScript serialisation; both agree on the test data.
- The JWS header is compared as parsed JSON, so a header serialised with other key order still verifies,
  as long as the signature was made over that same header segment.
- qma validates federation tokens of all three realms on `/v1/federated/...` so that a valid but untrusted
  token gives `403` (D36) instead of `401`.
- The federation token is cached until 30 s before it expires.
- cbe calls qma also for an inactive catalogue; qma answers `502 federationUnavailable` without calling out,
  and cbe makes the group `UNAVAILABLE`.
- The UI is built with plain Vue components and a small stylesheet; no third-party component library.
- The Keycloak admin user is `admin` / `admin` (start-dev only).

## Problems met and how they were solved

1. **MockMvc and encoded ids.** In the first integration test run, three tests failed with `400` because
   MockMvc encoded the `%` of an already encoded id again and the Spring Security firewall rejected `%25`.
   Fixed in the tests by passing `java.net.URI` objects. The running services were not affected.
2. **Advanced form race in the UI.** When the catalogue selection switched to federated, the old form stayed
   visible until the SEARCH schema arrived, and a fast user could type into a field that was then replaced.
   Fixed by clearing the form at the start of each rebuild.
3. **Leftover test data.** A first browser walkthrough crashed before its clean-up and left two RDs. They
   were removed by hand; the delivered `smoke.sh` cleans up on exit.
4. No compile errors needed fixing after the first full compile of each module, and the compose stack came
   up healthy on the first start. The digest was detailed enough for that.

Rough effort: about four fix iterations in total (the three above plus one UI robustness change for the
catalogue list request).

## Finish line at the end of the build

1. `docker compose -p simpl-catalogue-factory down -v --rmi local`, then
   `docker compose -p simpl-catalogue-factory up -d --build --wait`: succeeded (all 11 containers healthy).
2. `./smoke.sh`: passed (tokens, signed publication in A and B, quick and advanced search through cbe,
   federated search grouped per catalogue, 403 without role, 401 without token).
3. `mvn verify` in `build/`: green, 37 tests.
4. The stack is left running.

## What does not work or is not built

- Out-of-scope operations answer `501`: cat `GET/POST /query`, `GET/DELETE /session`, `GET /verification`,
  `GET /schemas/latest`, `PUT /schemas/{id}`; cbe `GET /v1/contractClauses/documents/{id}`.
- Not built by decision: notifications (D20), sorting (D33), search without catalogue selection (D45),
  SEARCH schema management (D62), Schema Manager UI (D76), provider UI (D77), DOME (D83), mTLS between
  GAs (D36: tokens instead), the IAA and authentication provider interfaces (static files instead).
- No automated UI tests ship with the build (they were optional). The UI was checked with a headless
  browser walkthrough during the build: login, catalogue selection and its failure message, quality filter
  validation, quick and advanced search, federated tabs with status messages, per-tab paging controls,
  SEARCH schema switch, own and federated detail, detail error.
- `GET .../qualityAssessmentRecord` answering `204` cannot happen, because every published RD has a QAR
  (D55).
- Quality recalculation only acknowledges (D56); nothing is rescored.
- The search does not scale beyond test sizes (in-memory filtering, see above).
