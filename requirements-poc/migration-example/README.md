> Part of the [Simpl-Open requirements proof of concept](../README.md).

# Migration example: BP05B

What one business process page in GitLab could look like if its requirements came from the backlog (Jira), with all their levels: high-level requirements, detailed requirements, Epics, user stories and acceptance criteria. The example takes BP05B, the process with the largest tree.

There are two versions of the same folder of `foundations/business-processes`, side by side:

| Version | What it is | Start here |
| --- | --- | --- |
| **Original** | The BP05B folder as it is in GitLab, unchanged: `develop` branch, commit 3bcdac9 of 10 November 2026. Also [in GitLab at that commit](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/3bcdac9/bp05b-provider-manages-resource-descriptions). | [Original BP05B page](original/bp05b-provider-manages-resource-descriptions/README.md) |
| **New** | The same folder with the requirements taken from Jira. | [New BP05B page](new/bp05b-provider-manages-resource-descriptions/README.md) |

Read the two side by side, for example the page of [5B.2 in the original](original/bp05b-provider-manages-resource-descriptions/bp5b2-provider-creates-new-resource-description.md) and [5B.2 in the new version](new/bp05b-provider-manages-resource-descriptions/bp5b2-provider-creates-new-resource-description.md).

## What is the same

Everything that is not a requirement. The overview, actors, assumptions, prerequisites, diagrams and process steps of the BP05B page are unchanged, and so are the two dynamic views and the images. The file names are the same, so links from other pages still work.

## What changed

Only the requirements part. In the original it lists the 9 high-level requirements, and each of their pages lists its detailed requirements in one line each, with a link to the requirements site. In the new version:

- **BP05B page, section High-level requirements.** It starts with a short explanation of the four levels (high-level requirement, detailed requirement, Epic, user story) and how to move between them. The table keeps the IDs 5B.1 to 5B.9 and the links to the requirements site. The titles now open the requirement pages, and new columns show the status, the process steps and the number of detailed requirements. The list under it shows, for each page, how many detailed requirements, Epics and user stories it has.
- **The nine requirement pages** (`bp5b1-...md` to `bp5b9-...md`). The top of the page is unchanged. Under it:
  - *About this requirement*: which process steps it covers, with links to those steps, where the dynamic view shows it, and how many detailed requirements, Epics and user stories it has.
  - *Description*: the text from Jira.
  - *Detailed requirements*: a table, then one section per detailed requirement with its text, status, process step, its link to the requirements site, and the Epics that deliver it.
  - Under each detailed requirement, its Epics, each with one line taken from its story card (for example *As a Provider, I want to retrieve a list of published resource types*), so a reader knows what an Epic is about before opening it.
- **Epic pages, new.** One folder per requirement page, with one page per Epic (69 in total). An Epic page explains where it sits (detailed requirement, high-level requirement, process step), gives the Epic text, and then its user stories (140 in total), each with its story card and acceptance criteria. Long pages have a link back to the list of user stories after each story, and every page ends with links back up.
- **A README in each Epic folder.** GitLab shows it when someone opens the folder, so a visitor who lands there sees which requirement the Epics belong to, grouped by detailed requirement.

Links say where they go ("5B.2.1 on the requirements site", "Epic: ...", "BP05B.01 Create a new resource description"); step numbers in tables are plain text, not bare links. Jira keys are shown as text, not as links, because jira.simplprogramme.eu is closed to public visitors. Statuses are the ones in Jira.

## Open points

- **Process steps.** The link between a detailed requirement and a process step (for example 5B.2.3 to BP05B.02) is our reading; the items in Jira have no field for it. 5B.1 (a Provider consults its own descriptions) has no step of its own and is placed under the trigger. A field or a label in Jira would make these links repeatable.
- **Dynamic view sections.** The same: the link from a requirement to a section of the dynamic view is made by hand.
- **Requirements site numbers.** The IDs such as 5B.2.1 exist only on the requirements site. 12 of the 32 detailed requirements were matched to them by hand, all under 5B.5, because the site uses short titles there.
- **Which text and which status.** The site shows every requirement as Proposed, Jira shows Defined or In Progress. The final publication must choose one.
- **Old story text.** A few older user stories (for example SIMPL-330) still have Jira formatting that the conversion does not handle, such as `+**As a**+`.

Built with `example_bp05b.py` in the build folder, from the proof-of-concept tree of 10 November 2026.
