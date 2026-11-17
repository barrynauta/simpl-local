# Build log (code writer, spec factory run 1, 27 Oct 2026)

Inputs used: `input/spec-digest.md`, `input/specs/` (README, OpenAPI, 32 in-scope stories with their comments where
relevant, BP02B, R07.02). Nothing else was read. No upstream code, no tests, no web search.

## 1. How the build went

| Step | Result | Fix iterations |
|---|---|---|
| Backend compile | Failed once: in Jena 5, `RDFConnection` is in a separate artifact (`jena-rdfconnection`), not in `jena-arq` | 1 |
| Unit tests | 2 of 39 failed on the first run, both in my own test expectations about Jena (see 3.1) | 1 |
| Integration tests (`ApiIT`, 25) | Green on the first run | 0 |
| UI build (`vue-tsc` + Vite) | Green on the first run | 0 |
| `docker compose up --wait` | Failed once: Fuseki main does not create the TDB2 directory; the command now runs `mkdir -p` first | 1 |
| Smoke (`smoke.sh`, 22 checks) | Green on the first run | 0 |
| Headless Chrome UI walkthrough (`ui/e2e/check.mjs`, 11 checks) | Green on the first run, but the rerun after the clean rebuild failed: the new version form was shown before the current content had loaded (a real UI race, plus a check that did not wait). Fixed in `NewVersionView.vue` (form shown only after content is loaded) and in the check; then three runs in a row green | 1 |
| Clean-state rebuild (`docker compose down -v`, then `up -d --build --wait`) and smoke again | Green | 0 |

Four fix iterations in total. One risk was avoided up front: defining `clientScopes` in a Keycloak realm import
can drop the built-in scopes (including `basic`, which adds `sub`), so the roles mapper sits on each client.

## 2. Spec gaps beyond the digest, and what I chose

Each item is a place where the specs plus D1 to D30 still left a choice. Ordered roughly by impact on a black-box
test suite.

1. **Contradiction: deprecated reference is a warning in the OpenAPI, an error in D17 and SIMPL-18184.** The OpenAPI
   example on `createVocabularyVersion` returns 201 with a `deprecatedVocabularyReference` warning. D17 (b) and the
   story say the creation must fail. I followed D17: 400.
2. **Reference namespace in the OpenAPI example differs from D18.** The example uses
   `https://authority.example/vocabulary-manager/content/LegacyVocabulary/versions/1#`; D18 uses
   `{base}/{Name}/{n}#`. I followed D18 only.
3. **A vocabulary's own namespace under the reference base.** D18 does not say whether a file may use
   `{base}/{OwnName}/...` for its own terms. If those IRIs were checked, a first upload would fail ("vocabulary does
   not exist"). I treat IRIs under the vocabulary's own name as its own terms and do not check them.
4. **How a referenced term is matched (D17 a).** A referenced vocabulary may define its terms in any namespace (for
   example `https://example.org/x#Person`), not in the D18 namespace. I match the referenced local name (after `#`,
   else after the last `/`) against the local names of all IRI subjects in the referenced version. This is lenient.
5. **Is an unused prefix declaration a reference?** I say yes: `@prefix l: <{base}/Legacy/1#> .` alone is enough to
   trigger the deprecated or missing checks.
6. **Which namespaces get the "unresolvable external namespace" warning.** D17 says every namespace that is neither
   internal nor registered. Taken literally, `rdf:`, `owl:` and the file's own namespace would warn on every upload.
   I exempt the language namespaces rdf, rdfs, owl, xsd, skos (skos because D16 requires `skos:ConceptScheme`) and
   xml, and any namespace in which the file itself has subjects. Only namespaces that are actually used in triples
   warn; unused prefixes do not.
7. **What counts as "Turtle" for 415 (D15).** Multipart file parts often arrive as `application/octet-stream`. Rule:
   Turtle if the part type is `text/turtle` or `application/x-turtle`, or the file name ends with `.ttl`, or the
   type is generic (`application/octet-stream`, `text/plain`, none) and the file has no extension. So `x.json`,
   `x.rdf`, `x.xml` and `x.txt` get 415. A Turtle file named `vocab.txt` would also get 415.
8. **Order of checks.** Not specified anywhere. Chosen: 401, 403, path format 400, 404, form fields 400, 409
   (deprecated vocabulary or duplicate name), 415, 413, content 400. A test that sends, for example, a duplicate name
   with a non-Turtle file gets 409, not 415.
9. **Version PATCH and the vocabulary status (D10).** D10 defines the vocabulary-to-versions direction only. My
   choice: a version PATCH changes only that version (and the vocabulary's `lastUpdateDate`), never the vocabulary
   status. So a DEPRECATED vocabulary can have an ACTIVE version after a version PATCH, and an ACTIVE vocabulary
   can end up with no ACTIVE version.
10. **PATCH to the status a resource already has.** Not stated; the OpenAPI 409 example ("Vocabulary is already in
    status ACTIVE") suggests 409. I return 409 for both PATCH operations.
11. **`creationDate` and `lastUpdated` filter semantics.** "Filter by ISO 8601 date or date-time" does not say exact
    or same day. Matching an exact millisecond is useless, so both forms match items on the same UTC day.
12. **Default sort orders.** Not given. List: by name ascending. Versions: newest first (SIMPL-18167 "chronological
    list" and the OpenAPI example both show version 2 before 1). Ties are broken by name or version.
13. **Unknown query parameters.** SIMPL-18164 says an "unsupported filter or sort field" is rejected. I reject every
    unknown query parameter on the three list operations with 400.
14. **`createdBy` value.** The OpenAPI example is an e-mail address; D6 says nothing. I use `preferred_username`
    (so `ga-admin`), falling back to `email`, then `sub`.
15. **Creator of a version.** SIMPL-18176 AC3 asks for the creator of each version, but `VocabularyMetadata` has no
    `createdBy`. I added `createdBy` to version responses (additive).
16. **`description` on two levels.** Vocabulary and version both have a description; a new version carries a new
    one. The vocabulary-level description is the latest version's description ("latest metadata").
17. **`version` field on create.** The ban on a client `version` is written for new versions only. I also reject it
    on create (400), for consistency with SIMPL-18169 ("no field for version").
18. **`validationWarnings` when empty.** Present as `[]` or absent? I always include it on 201 responses.
19. **Warnings when errors block the request.** The Problem schema has no place for warnings; SIMPL-18183 says
    "errors or warnings are returned". I add a `validationWarnings` extension member to the 400 body.
20. **D3 read literally makes `POST /external-vocabularies/content` admin only.** It is a read (download by
    namespace), but D3 lists only the two search POSTs for viewers. I followed D3: viewer gets 403 there.
21. **External vocabulary rules.** Not specified: validation depth (I do syntax only, plus "not empty"; no
    `owl:Ontology` requirement because copies such as DCMI Terms may not have one), duplicate namespace or alias
    across registrations (409), alias input format in multipart (repeated fields or comma separated), and whether
    `namespaceUri` search should also match aliases (I match canonical only for `namespaceUri(s)` and aliases only
    for `aliasUri(s)`; the content POST matches either, as its description says).
22. **Search response paging.** `VocabularySearchResponse` is a `CollectionPage`, but the request has no paging
    parameters. I return all matches with `page` 1, `pageSize` max(20, matches), `totalPages` 1 (0 when empty), and
    no links. Duplicate request items are returned once.
23. **Path spelling `/external-vocabulary/searches`** (singular) next to `/external-vocabularies/...` (plural)
    looks like a typo in the OpenAPI. Implemented exactly as written.
24. **Upload limit example.** The OpenAPI 413 example says 5 MB; D30 says 10 MB. I used 10 MB (10 x 1024 x 1024
    bytes) and the message names the configured value.
25. **Name uniqueness and case.** Not stated. Uniqueness is case-sensitive (names are PascalCase anyway).
26. **"Validation on file upload" in the UI (SIMPL-18180 AC1)** has no API support (there is no validate-only
    operation). The UI runs a browser pre-check (n3 parser plus the D16 rules) when a file is selected and blocks
    submit while it fails. The browser parser stops at the first syntax error, so the full list only comes from the
    server on submit.
27. **"Form pre-filled with existing content" (SIMPL-18172 AC1).** A file input cannot be pre-filled. The new version
    form shows the current Turtle content in an editable text area; an uploaded file replaces it.
28. **Health `DEGRADED`.** The OpenAPI defines it, nothing says when. Never returned.
29. **Roles claim for a user without roles.** Keycloak omits the `roles` claim entirely for `ga-norole` (no empty
    array). The backend treats a missing claim as no roles.

## 3. Problems hit and how they were solved

### 3.1 Jena parser behaviour

- Jena stops at the first fatal Turtle error. SIMPL-18180 AC7 and SIMPL-18181 want all errors. Solution in
  `TurtleSyntaxValidator`: after a fatal error, skip to the next line that ends with `.`, and parse the rest again
  with the prefix declarations seen so far (line numbers are shifted back). This finds independent errors on later
  statements. It is a heuristic: a `.` inside a multi-line string can make it resume in the wrong place, and errors
  inside the same statement are reported once.
- Jena 5 accepts a last triple without the closing `.` at end of file. Not reported as an error.
- Jena reports "newline in string" at the start of the next line, so the reported line is one after the line with
  the open quote.
- Jena warnings (for example unusual IRIs) are ignored; only errors and fatal errors block.

### 3.2 Infrastructure

- `RDFConnection` moved to `jena-rdfconnection` in Jena 5 (compile error, added the dependency).
- Fuseki: there is no official Apache image on Docker Hub. I used `secoresearch/fuseki:5.3.0` and bypassed its
  entrypoint to run the plain Fuseki main server with one TDB2 dataset and update enabled. It needed `mkdir -p` for
  the database directory.
- Keycloak token issuer: solved with `KC_HOSTNAME=http://localhost:8183` and
  `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. Verified that tokens from the host and from inside the network both carry
  `iss=http://localhost:8183/realms/simpl` and both are accepted. The backend also accepts a list of issuers
  (`APP_ISSUERS`) if a test environment needs more.
- Keycloak 26 has no `curl` in its image; the healthcheck uses bash `/dev/tcp` against the management port 9000.
- Error responses are written directly to the servlet response, so a 404 on a `text/turtle` download is still sent
  as `application/problem+json` (Spring's converters would otherwise fail on the `Accept` header).

## 4. What does not work, or works only partly

- **Multi-error syntax reporting is heuristic** (see 3.1). Simple cases (several broken statements) work.
- **Concurrency** is handled with an in-process lock, so it is safe for one backend instance only.
- **No silent token renewal in the UI.** Tokens live 1 hour (realm setting); after that the next API call sends the
  user back to the sign-in page.
- **UI parts without own automated tests:** filter and sort controls, download buttons, the reference picker and
  the deprecated-reference warnings in the new version form. They compile and were not exercised by the walkthrough.
- **No UI** for external vocabularies or version-level status (no stories).
- **The 500 path** of SIMPL-18162 is implemented (generic handler, Problem Details) but not tested.
- **Fuseki has no authentication.** It is not exposed to the host, only reachable inside the compose network.
- **The design (Figma) was not used** (D21). The UI is functional, not styled to the Simpl design system.
