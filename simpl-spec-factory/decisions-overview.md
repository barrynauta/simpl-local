# Step 30: what the validators found compared with run 1 (17 Nov 2026)

Run 1 (late October 2026) prepared each component in one step: read the specs, write a digest of numbered decisions. This run splits that work: validators find the gaps (steps 20 and 23), a separate step decides them (30a), blind to the run 1 digest, and a gate checks that every blocker is closed (30b). Step 30c then compared the two, per component, against the run 1 digest as first committed. Details: `compare-run1.md` in each component's `decisions/` folder ([Vocabulary Manager](components/vocabulary-manager/decisions/compare-run1.md), [Schema Manager](components/schema-manager/decisions/compare-run1.md), [Catalogue](components/catalogue/decisions/compare-run1.md), [Contract Management](components/contract-management/decisions/compare-run1.md)).

## Did the validators find the run 1 gaps?

Run 1 decisions that are environment choices (store, identity provider, ports) or scope statements are left out: they are not gaps in the specs.

| Component | Run 1 decisions | Real gaps | Found by the validators | Only decided in 30a | Missed |
|---|---|---|---|---|---|
| Vocabulary Manager | 30 | 27 | 22 (81%) | 2 | 3 |
| Schema Manager | 63 | 55 | 50 (91%) | 2 | 3 |
| Catalogue | 83 | 69 | 48 (70%) | 7 | 14 |
| Contract Management | 83 | 65 | 50 (77%) | 4 | 11 |
| **Total** | **259** | **216** | **170 (79%)** | **15** | **31 (14%)** |

The validators also found gaps that run 1 did not have: 13 blockers and 32 majors over the four components (plus 7 in the Catalogue schema stories that run 1 left out of scope).

## Why the misses were missed

1. **Input outside the set.** Run 1 read material this run did not have as input: the R07.02 tech description, Figma comments, an older `-itb` API root. The validators cannot find a conflict with a source they do not read.
2. **Cross-cutting questions no single criterion raises.** The order of checks when several errors combine (401, 403, 415, 400, 404, 409), what happens when a dependency is down, content negotiation (406), signing formats and key registries, sanitising content that comes from another party. Each criterion tests one thing; nobody asks about the combination. The 20b brief now asks these questions for every component (check AC-C07).
3. **Operations no story drives.** An OpenAPI operation with no story behind it (for example the Contract Management finalisation `PATCH`) gets no finding, so no decision.

## Do the decisions agree?

Not often. Where both runs decided the same gap, they disagree on about half: 11 of 24 for the Vocabulary Manager, 18 of 50 for the Schema Manager, and more than half for Catalogue and Contract Management. In those two, one scope choice of 30a explains most of it (Catalogue leaves the catalogue-be lifecycle out; Contract Management builds two backends and takes the lifecycle after Active, consent and the clause upload as accepted risk).

The disagreements are not technical details: whether a vocabulary status cascades to its versions, whether a revoked schema may be republished, what a schema key is, synchronous or asynchronous signing. Two careful readers of the same specs decide them differently. That is the strongest argument for sending the questions to the spec owners (the [spec feedback](./spec-feedback-overview.md)): until they answer, every build makes its own choice.

## What changed in the factory because of this

- Step 20b got check AC-C07, cross-cutting questions, asked once per component.
- The gate (30b) warns when decisions mix error body formats or close one finding twice. Catalogue mixes two formats.
- To do: give steps 20 and 23 the `SEC-` rules (the HTML sanitising miss), and list OpenAPI operations without a story as findings of their own (step 21).
