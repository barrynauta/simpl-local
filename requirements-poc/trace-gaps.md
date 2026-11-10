# Trace gaps

> Part of the [Simpl-Open requirements proof of concept](README.md). New here? [Start here](README.md) · [What the levels mean](levels.md)

The handbook says every item must trace up to a business process (see [linking rules](levels.md#linking-rules)). This page checks the ten items against those rules. A gap can mean that a link is missing in Jira, or that the export left out the item that makes the link. This page does not say which; that needs a look in Jira.

## Summary

| Result | Items |
| --- | --- |
| ✓ Traces to a business process in Jira | 3 of 10: SIMPL-18783, SIMPL-26074, SIMPL-10171 |
| ⚠ Traces only from the process side | 1 of 10: SIMPL-30560 |
| ⚠ Does not trace to any business process | 6 of 10: SIMPL-31424, SIMPL-31425, SIMPL-31426, SIMPL-31853, SIMPL-30641, SIMPL-31429 |

## Per item

| Item | Level | Rule | Finding |
| --- | --- | --- | --- |
| [SIMPL-30560](items/SIMPL-30560.md) | L1 | Linked to exactly one business process | No parent in Jira. The public SA01 page lists it as requirement 1.6. |
| [SIMPL-31424](items/SIMPL-31424.md) | L1 | Linked to exactly one business process | No parent in Jira. No public process page lists it. |
| [SIMPL-31425](items/SIMPL-31425.md) | L2 | Linked to exactly one parent L1 | Rule met (SIMPL-31424), but the chain ends at an L1 without a process. |
| [SIMPL-31426](items/SIMPL-31426.md) | Epic | Linked to at least one L2 | Rule met (SIMPL-31425), same chain as above. |
| [SIMPL-31853](items/SIMPL-31853.md) | Epic | Linked to at least one L2 | No parent. |
| [SIMPL-30641](items/SIMPL-30641.md) | User story | Sits under an Epic | No parent. |
| [SIMPL-31429](items/SIMPL-31429.md) | Epic, cancelled | Linked to at least one L2 | No parent in the export. |

## Missing from the export

The export publishes some items without their parent or children. The public process pages show these exist:

| Under | Missing item | Source |
| --- | --- | --- |
| SIMPL-18783 (BP02A) | SIMPL-10192 Configure Dataspace Identity Attributes (L1) | Public BP02A page |
| SIMPL-10171 | 2A.1.1, 2A.1.2, 2A.1.3 (detailed requirements) | Public BP02A page |
| SIMPL-26074 (SA09) | SIMPL-26083 and SIMPL-27082 (L1) | Public SA09 page |
| SIMPL-30560 | The L0 item for SA01, and 1.6.1 (detailed requirement) | Public SA01 page |

## Other inconsistencies

- **Status conflicts.** SIMPL-31424 (L1) is "Internal QA check", its L2 is "In Progress", and the Epic under that has Epic status "To Do". SIMPL-31426 shows status "In Progress" and Epic status "To Do" at the same time.
- **Published flag.** SIMPL-31424 and SIMPL-31425 say "Published on website: No" but are in the published export.
- **Uncovered requirement.** SIMPL-31424 asks that participants can update their contact email. The only matching item, SIMPL-31429, is cancelled.

## To look into

1. Do SIMPL-31424, SIMPL-31853 and SIMPL-30641 have a parent in Jira that the export did not publish, or no parent at all?
2. Why does the public SA01 page list SIMPL-30560 while Jira gives it no parent?
3. What are the publication criteria? The export does not say why an item is in or out.
