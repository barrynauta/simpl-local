# Existing tests as part of the specification

Draft, 4 Nov 2026. A proposal for the next run of the factory, after the PSO results of 29 Oct.

## The question

The PSO suite failed 34 of 93 scenarios on the factory builds (59 of 93 passed). 14 of the failures are the environment (live signing, IAA onboarding, seed data); those are handled separately with stand-ins in each candidate's `experiments/pso/`. The other 20 come from places where the PSO testers and the factory read the specifications differently, or where PSO tests something no specification describes.

So the question is: can the existing tests count as part of the specification? In behaviour-driven development the feature files are the specification, and the PSO features are written from the same stories. One rule exists only in a PSO test case and in upstream code (SIMPL-18293). So the tests hold knowledge the stories do not.

But the PSO suite also follows the running system in places: the older `Error {code, message}` shape and an IAA role the catalogue OpenAPI does not name. If the tests win everywhere, the factory copies upstream's drift as well.

## Proposed precedence rule

The tests become a source, with this place in each candidate's source hierarchy:

1. Where the specifications are **silent**, a test fills the gap. The behaviour the test checks becomes a decision row in the digest, with the test named as its source.
2. Where a test **contradicts** a higher source (the OpenAPI on the wire, a story on behaviour), the higher source wins. The conflict becomes an open question for the spec owners. The test is expected to fail and is reported as a spec conflict, not as a build defect.
3. Where the **digest** contradicted a higher source and the test follows that source, the digest was wrong. The decision is corrected.
4. A scenario that PSO itself marks `@bug` is not used as a specification.

## The 20 non-environment failures under this rule

| Candidate | Scenarios | What PSO expects | Rule | Outcome |
|---|---|---|---|---|
| Schema Manager | 2 | A schema without a declared resource type is rejected (SIMPL-18293) | 1, silence | Adopt as a decision |
| Schema Manager | 2 | No token on `GET /schemas`: 400 | 2, conflict | The OpenAPI lists neither 400 nor 401 here; the shared decision (401, HTTP semantics) stays. Question: should the OpenAPI list 401? |
| Schema Manager | 1 | A new version is published although another Data schema holds the slot (D16) | 4, `@bug` | Not used |
| Schema Manager | 1 | `GET /vocabularies` on the Schema Manager | 4, `@bug` | Not used |
| Catalogue | 4 | `GET /schemas/latest`, `GET /verification`, `POST /query` (twice) | 3, own scope choice | These are in the OpenAPI; the factory left them out (D10). Implement them |
| Catalogue | 1 | `POST /query/search` with an empty body: 400 | 1, silence | Adopt what the test shows; the operation is in no specification |
| Catalogue | 2 | Error body with a `message` field | 2, conflict, with a way out | The OpenAPI uses both shapes. RFC 9457 allows extension members, so a Problem body that also carries `message` satisfies both. Question: which shape is the target? |
| Catalogue | 2 | IAA role `T2IAA_M` may post and read | 2, conflict | The catalogue OpenAPI names other roles. Mapping IAA roles onto catalogue roles is configuration; question: is `T2IAA_M` meant to have these rights? |
| Contract Management | 3 | Random ids on search, issue and create: 404 | 2, conflict | The bodies lack required fields, so the OpenAPI makes them 400 first. Question: should the OpenAPI list the 404s (story 32511 has one)? |
| Contract Management | 1 | `GET /health` without a key: 401 | 3, digest wrong | The OpenAPI puts health under the API key; D13 chose open. Correct D13 |
| Contract Management | 1 | `GET .../status` | 1, silence | The OpenAPI has only `PATCH` on this path. Adopt only if the spec owners confirm; otherwise a question |

Summary: 4 scenarios are filled by the tests (rule 1), 5 come from the factory's own choices and are corrected (rule 3), 9 are real conflicts for the spec owners (rule 2, of which 2 can be met on both sides), and 2 are marked `@bug` by PSO itself.

Expected result once the environment stand-ins work: up to 59 + 14 + 9 = 82 of 93, or 84 with the `message` extension. The 9 conflicts stay as questions, which is the useful output for the spec owners.

## Step 1 result: environment stand-ins (4 Nov 2026)

With the stand-ins in each `experiments/pso/` the builds pass **68 of 93** (was 59): Schema Manager 21 of 27, Catalogue 39 of 52, Contract Management 8 of 14. Nothing that passed before fails now. No PSO test file and no build code changed. Details per candidate in the `experiments/pso/README.md` files.

Five scenarios that looked like environment turn out not to be, once the environment is there:

| Candidate | Scenarios | What happens now | Rule |
|---|---|---|---|
| Catalogue | 2 (TCC06) | PSO sends `offeringType` "DATA"; the digest's seed shape allows only lowercase values, so the build answers 422 | 2, conflict: which spelling is the target? Test data, not environment |
| Catalogue | 1 (TCC11) | The expected 422 comes, but PSO wants a `code` field in the error | 2, the same error-shape conflict as `message`; the same way out (an extension member) |
| Catalogue | 1 | "Gaia-X Core" in the schema list: the build has no ontology store and always returns an empty list | 3, a scope choice of the factory: needs a build change |
| Contract Management | 1 (SV08) | Gets past onboarding, then the same 405 on the `GET` the OpenAPI does not have | 1, see the `GET .../status` row above |

One caution: the five TCC07 scenarios pass, but only because the build rejects the same uppercase "DATA" with 422. If the shape accepted "DATA", they would get 200 and fail. So they pass for the wrong reason and should not be counted as agreement until the spelling question is settled.

The stand-ins encode assumptions a reader should know: the gateway does not check tokens, the Contract Management Provider counts as already onboarded, and the catalogue signer stand-in signs with the build's own test key.

## Step 2 result: decision rows and build changes (4 Nov 2026)

The precedence rule is now in the source hierarchy of the three digests, with the PSO suite (commit `ec7a546`) named as a source. The decision rows were written first, then the factory's own suites and then the builds were changed, as clarification round run 1.2 of each candidate. In this round one agent did both sides from the digest rows, so the separation of test writer and code writer is weaker than in run 1. No PSO test file changed.

| Candidate | Rows | Own suites before | Own suites after | PSO before | PSO after |
|---|---|---|---|---|---|
| Schema Manager | D64 (rule 1) | 291 API, 36 UI, all pass | 293 of 293 API, 36 of 36 UI | 21 of 27 | 23 of 27 |
| Catalogue | D10 revised, D84 to D87 (rule 3), D88 (rule 1), D89 (rule 2, partial way out) | 454 of 455 API, 26 of 26 UI | 482 of 483 API (28 new), 26 of 26 UI | 39 of 52 | 46 of 52 |
| Contract Management | D13 revised, with D65 and D66 (rule 3) | 345 of 345 API, 38 of 38 UI | 345 of 345 API, 38 of 38 UI | 8 of 14 | 9 of 14 |
| Total | | | | 68 of 93 | **78 of 93** |

The one own failure in the Catalogue is the D14 order deviation of run 1. Nothing that passed in PSO before fails now. One Schema Manager scenario that PSO marks `@bug` (TC_SM_19) passes as a side effect; without it the total is 77.

The plan expected up to 82 of 93, or 84 with the `message` extension. It stays below that for three reasons:

- The error shape has no full way out. PSO validates error bodies with a schema that is `oneOf` Problem or `Error`. A body with both shapes matches both branches and fails. A first build with `code` on every Problem lost two scenarios that passed before (44 of 52). The build now adds `message` everywhere and `code` only on verification failures. TCC11 still fails: it expects an exact message for a body the build rejects one step earlier.
- Gaia-X Core did not fall out of the graph work. The graph holds the claims of resource descriptions, not schemas, so `GET /schemas` still lists no ontologies.
- The rule-2 conflicts were not implemented, as planned: 400 against 401 without a token (2), the role `T2IAA_M` (2), 404 against 400 on bodies without required fields (3), the uppercase "DATA" (2), the `GET .../status` the OpenAPI lacks (2).

How `/query` works now: openCypher on Neo4j Community 5.26, one database per catalogue in the compose file. The claims of each active resource description (its `credentialSubject` as RDF, the graph SHACL validates) are loaded on publish and removed on revoke, delete and a new version. Nodes are scoped per resource description, `rdf:type` gives labels, literals give properties and links give relationships, all named by local name (digest D85, marked to validate). Statements run read-only with the request's timeout.

Questions for the spec owners added to the digests: Schema Manager questions 16 and 17, Catalogue questions 13 to 18, Contract Management questions 18 and 19.

## Keeping an independent check: hold-out

The PSO suite is currently the one yardstick nobody tuned the builds to. If the writers read it, a pass proves less. So the next run splits it:

- Per candidate, split the PSO scenarios in two halves with a fixed seed, stratified by feature file, so both halves cover the same areas.
- The test writer and the code writer get the **visible half** as an extra source, under the rule above.
- The score is taken on the **hidden half** only, which neither writer has seen.
- Compare with the 29 Oct baseline on the same hidden half.

That answers a better question than "can we reach 93 of 93": do tests used as specification generalise to tests the builder never saw?

## Step 3 result: hold-out run with the PSO tests on top (16 Nov 2026, Contract Management)

Run with a stronger rule than above, asked by Barry: the PSO scenarios are bullet-proof and win over the OpenAPI and the stories (rule 2 reversed, rule 4 not needed: no visible scenario is `@bug`). Contract Management only, split 8 visible and 6 hidden, base digest run 1.1, three fresh agents and a build from scratch. Own suite 399 of 399, PSO visible 8 of 8, **PSO hidden 2 of 6, the same as before**. Details: [`contract-management/experiments/pso-as-spec/`](components/contract-management/experiments/pso-as-spec/README.md).

## Points to validate later

- **openCypher engine for the catalogue `/query` (4 Nov 2026, built in run 1.2).** Barry asked to implement `GET/POST /query` after all (it was out of scope under D10 because it needs an openCypher engine). The build uses Neo4j as an environment choice; the behaviour comes from the OpenAPI and the stories only, and the mapping is catalogue digest D85. To validate: is openCypher over the self-description claims the intended query surface for Simpl-Open; is the D85 mapping meant (nodes scoped per resource description, local names for labels, properties and relationships, or for example full IRIs, one shared node per IRI, or an RDF import with namespace prefixes); should revoked descriptions stay queryable for existing contracts; does the discovery access policy apply to `/query`; and does the result shape (one item per row, nodes as property maps) match what consumers of the query API expect.
- **Error bodies (4 Nov 2026).** A Problem cannot also be a full `Error {code, message}` for PSO's `oneOf` schema. To validate with the spec owners which shape is the target before the hold-out run.

## Steps

1. Environment stand-ins: done 4 Nov 2026, 68 of 93, see above.
2. Add the precedence rule to the three digests and write the decision rows above (rules 1 and 3): done 4 Nov 2026, 78 of 93, see above.
3. Make the hold-out split, rebuild with the visible half, score on the hidden half: done 16 Nov 2026 for Contract Management, with the PSO tests on top, see above.
4. Send the 9 conflicts to the spec owners as questions, together with the open questions already in the digests.
