# The proposal

> Part of the [Simpl-Open requirements proof of concept](README.md).

This page explains what this proof of concept changes compared with the first publication attempt in [`simpl/pso/jira-test-issues`](https://code.europa.eu/simpl/pso/jira-test-issues), and what the final publication should do. Both use the same ten items.

## The problem

A reader who opens a page in the first attempt sees a requirement item without context.

- **No introduction.** The repository README is still the GitLab template. Nothing says what Simpl-Open is, what the repository contains or who it is for.
- **Levels not explained.** L0, L1, L2, Epic and Story are folder names only. The Development Handbook defines them, but the pages do not use those definitions.
- **No trace to the business process.** The process is named only in the two L0 items, as the title of an internal page without a link or as "SA09: [[REDACTED]]". Lower items do not show their process at all. Four of the nine published items have no parent.
- **Content hidden behind metadata.** Each page opens with timeline and classification tables. The requirement comes last.
- **Broken formatting.** Acceptance criteria lose their Given / When / Then structure in two items, and raw HTML (`<strong>`, `<br>`) shows in the text.
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
| Acceptance criteria as Given / When / Then lists | Criteria are readable and testable. |
| **⚠ Trace gap** tags and a [trace gaps](trace-gaps.md) page | Missing links are visible, not hidden. |
| "Not in this folder" entries on the [process view](processes.md) | The reader sees what the export left out. |
| "Notes on the source" on each page | Problems in the source are listed without changing the text. |
| Folders follow the levels: business process › L0 › L1 › L2 › Epic › user story, one folder per item, named after its level and key (for example `L1-SIMPL-10171`) | The structure shows the trace by itself, also when there are hundreds of items. Names stay short and stable. |
| Cancelled items stay, marked as cancelled | History is kept and the reader is not misled. |

## What the final publication should do

A list of requirements for the generator that PSO will build.

1. Generate the start page, the levels page and the process view, not only item pages.
2. Give every item page a context line, a level, a trace line and a short description.
3. Link each item to its public business process page and requirements site page.
4. Put each item in a folder named after its level and key, for example `L1-SIMPL-10171`, inside the folder of its parent, from the business process down. Items without a parent go in the folder of their business process, or in a `no-business-process` folder.
5. Render user stories and acceptance criteria as markdown, with the Given / When / Then structure kept.
6. Put the content first. Show the level and status only, and leave out workflow fields such as priority, version and completion rate.
7. Check every item against the handbook linking rules, and tag and list the items that fail.
8. Publish a complete tree: when an item is published, show its parent and children too, or at least list them as "not published".
9. State the publication criteria, so readers know why an item is in or out.
10. Keep cancelled items, marked, with the reason for the cancellation.
11. Check the source text for obvious problems (typos, contradicting status fields, published flag) and report them, without changing the text.

## Open points

- Should the trace line also show the process step (for example BP02A.02)? The items have no field for it today. It could become a field on the item, or the step could be kept on the process page only.
- Should the final publication link to the source items for readers who have a login?
