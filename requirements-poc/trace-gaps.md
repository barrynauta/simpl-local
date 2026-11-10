> **Simpl-Open requirements, proof of concept.** New here? [Start here](README.md) · [What the levels mean](levels.md) · [All items](all-items.md) · [By business process](processes.md)

# Trace gaps

The handbook says every item must trace up to a business process (see [linking rules](levels.md#linking-rules)). This page checks the items in this folder against those rules, and compares the result with PSO's export of 10 November 2026.

## Summary

| Check | Result |
| --- | --- |
| L1 linked to exactly one L0 | ✓ all 47 |
| L2 linked to exactly one L1 | ✓ all 117 |
| Epic linked to at least one L2 | ✓ all 221 |
| User story under an Epic | ✓ all 362 |

So, inside this folder, every item traces up to a business process. This says nothing about items that have no parent at all: the folder is built from the six L0 items downwards, so such items cannot appear in it.

## What PSO's export lost

PSO's export showed six of its ten items without a parent, so they looked like orphans. All six have a parent in the source. The export left out the links, not the source.

| Item | In PSO's export | In the source |
| --- | --- | --- |
| [SIMPL-18783](items/BP02A/L0-SIMPL-18783/README.md) (L0) | 1 of its 4 L1 children | 4 L1 children |
| [SIMPL-26074](items/SA09/L0-SIMPL-26074/README.md) (L0) | none of its 2 L1 children | 2 L1 children |
| [SIMPL-10171](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10171/README.md) (L1) | parent [SIMPL-18783](items/BP02A/L0-SIMPL-18783/README.md), none of its 3 L2 children | 3 L2 children |
| [SIMPL-30560](items/SA01/L0-SIMPL-16074/L1-SIMPL-30560/README.md) (L1) | no parent | under [SIMPL-16074](items/SA01/L0-SIMPL-16074/README.md) (SA01) |
| [SIMPL-31424](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/README.md) (L1) | no parent | under [SIMPL-10783](items/BP13/L0-SIMPL-10783/README.md) (BP13) |
| [SIMPL-31425](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/README.md) (L2) | parent [SIMPL-31424](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/README.md) | same |
| [SIMPL-31426](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/README.md) (Epic) | parent [SIMPL-31425](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/README.md) | same |
| [SIMPL-31853](items/BP11/L0-SIMPL-27777/L1-SIMPL-27783/L2-SIMPL-27785/Epic-SIMPL-31853/README.md) (Epic) | no parent | under [SIMPL-27785](items/BP11/L0-SIMPL-27777/L1-SIMPL-27783/L2-SIMPL-27785/README.md) (BP11) |
| [SIMPL-30641](items/BP05B/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/Epic-SIMPL-30635/Story-SIMPL-30641/README.md) (User story) | no parent | under [SIMPL-30635](items/BP05B/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/Epic-SIMPL-30635/README.md) (BP05B) |
| [SIMPL-31429](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31429/README.md) (Epic) | no parent, status Cancelled | under [SIMPL-31427](items/BP13/L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/README.md) (BP13), status Defined, never cancelled |

## Other findings

- **Two parents.** 3 user stories sit under two Epics: [SIMPL-5114](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-5114/README.md) (also under [SIMPL-2181](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2181/README.md)), [SIMPL-5115](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-5115/README.md) (also under [SIMPL-2181](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2181/README.md)), [SIMPL-13565](items/BP05B/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-13565/README.md) (also under [SIMPL-12250](items/BP05B/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-12250/README.md)). They are shown under the first and listed on the page of the second.
- **Not published.** 67 of the 170 L0, L1 and L2 items are not published on the requirements website. Their pages, and the pages of the Epics and stories under them, show only key, level, title and status. The whole of BP11 is unpublished, from its L0 down.
- **No publication flag.** 2 L2 items have no value in the publication field: [SIMPL-10177](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10176/L2-SIMPL-10177/README.md), [SIMPL-10178](items/BP02A/L0-SIMPL-18783/L1-SIMPL-10176/L2-SIMPL-10178/README.md). They are treated as not published.
- **Cancelled items.** 70 Epics and user stories are cancelled. They are kept and marked.

## To look into

1. Why does PSO's export drop the parent links of items it publishes?
2. Why does PSO's export show SIMPL-31429 as Cancelled, while the source never cancelled it?
3. Should unpublished L0 to L2 items appear at all, or only as a count?
