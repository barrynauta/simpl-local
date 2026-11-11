# The proposal

> Part of the [Simpl-Open requirements proof of concept](README.md).

This page explains what this proof of concept changes compared with the first publication attempt in [`simpl/pso/jira-test-issues`](https://code.europa.eu/simpl/pso/jira-test-issues), and what the final publication should do. Both start from the same ten items. This proof of concept adds the full tree they belong to.

## The problem

A reader who opens a page in the first attempt sees a requirement item without context.

- **No introduction.** The repository README is still the GitLab template. Nothing says what Simpl-Open is, what the repository contains or who it is for.
- **Levels not explained.** L0, L1, L2, Epic and Story are folder names only. The Development Handbook defines them, but the pages do not use those definitions.
- **No trace to the business process.** The process is named only in the two L0 items, as the title of an internal page without a link or as "SA09: [[REDACTED]]". Lower items do not show their process at all.
- **Content hidden behind metadata.** Each page opens with timeline and classification tables. The requirement comes last.
- **Broken formatting.** Acceptance criteria lose their Given / When / Then structure in two items, and raw HTML (`<strong>`, `<br>`) shows in the text.
- **No common layout.** Each team writes user stories and acceptance criteria in its own way: all in capitals, all in bold, as bullets or as numbers, one sentence or one line per step.
- **Lost links.** Six of the ten items show no parent, so they look like orphans. All six have a parent in the source. The export also leaves out most children: SIMPL-10171 shows none of its three L2 items.
- **Wrong status.** SIMPL-31429 is shown as Cancelled under "Out of scope". The source never cancelled it.
- **Long file names.** Files are named after the full title, so links turn into `SIMPL-30641%20-%20A%20Provider%20removes...`.

## What this proof of concept does

| Change | Why |
| --- | --- |
| A [start page](README.md) that explains Simpl-Open, the levels and how to read a page | A first-time reader gets oriented in two minutes. |
| A [levels page](levels.md) with the handbook definitions and linking rules | Readers learn what L0 to Story mean, and gaps can be checked against a rule. |
| Every page opens with a short context line and links to the start page | Any page can be the entry point. |
| A **trace line** on every page, from the business process down to the item | The process is visible from any level. |
| Links to the public process pages and requirements site | Traceability works without a login. |
| A short "What this item is" paragraph | The reader knows what the item is about before reading the formal text. |
| Content first, and only the level and status as metadata | The requirement is what readers come for. Workflow fields mean nothing to a reader. |
| One layout for all user stories and acceptance criteria: **As a**, **I want to**, **So that** on three lines; one numbered scenario per **Given**, with **When**, **Then**, **And**, **But** on their own lines | Every item reads the same way, whoever wrote it. Criteria are readable and testable. |
| A fuller text on each business process page: overview, who takes part, what must be in place, the steps, and the high-level requirements in the folder | A reader understands the process before opening its items. |
| The full tree of each business process, from L0 down to user story | The reader sees an item with all its context, not a sample. |
| A [trace gaps](trace-gaps.md) page that checks every item against the linking rules | Missing links would be visible, not hidden. In this tree there are none. |
| Only published items show their text; the others show key, level, title and status | The tree is complete, and nothing unpublished leaks. |
| "Notes on the source" on each page | Problems in the source are listed without changing the text. |
| Folders follow the levels: business process › L0 › L1 › L2 › Epic › user story, one folder per item, named after its level and key (for example `L1-SIMPL-10171`) | The structure shows the trace by itself, also when there are hundreds of items. Names stay short and stable. |
| Cancelled items are left out | They are not part of the requirements, so they have no value for a reader. |

## What the final publication should do

The requirements for the generator are on their own page now, in order of priority: [Requirements for the publication](requirements.md).

## Open points

- Should the trace line also show the process step (for example BP02A.02)? The items have no field for it today. It could become a field on the item, or the step could be kept on the process page only.
- Should the final publication link to the source items for readers who have a login?
