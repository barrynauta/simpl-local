# Requirements for the publication of Simpl-Open requirements in GitLab

> Part of the [Simpl-Open requirements proof of concept](README.md). Draft for internal validation by DG CONNECT, 11 November 2026.

## Purpose

PSO publishes the Simpl-Open requirements and backlog items from Jira to GitLab, as read-only Markdown pages. This page says what that publication must give its readers, in order of priority. It builds on PSO's [Model 1 - Proposed Implementation Plan](https://confluence.simplprogramme.eu/spaces/DCPIAS/pages/212636688) and on DG CONNECT's comment on it, which asks for a hierarchy instead of flat folders.

How the generator is built, and with which tools, is PSO's choice. These requirements only describe the result.

What stays as in Model 1:

- Jira stays the only source. GitLab is a publication, never edited by hand.
- Levels L0, L1, L2, Epic and user story. Tasks are out of scope.
- Only approved Epics and user stories are published (requirement 5a). Cancelled items are left out.
- An automatic run once a week, updating pages without duplicates.

## Priority 0: the tree

The reader must see the hierarchy in the GitLab file tree itself, without going back and forth between README pages.

| # | Requirement | Done when |
| --- | --- | --- |
| 1 | Every item is a folder **inside the folder of its parent**: business process › L0 › L1 › L2 › Epic › user story. | Opening a folder in GitLab shows its children, and only its children. |
| 2 | Folder names are the **level and the Jira key**, for example `L1-SIMPL-14688`. The item page is the `README.md` of its folder, so GitLab shows it when the folder is opened. | No titles or spaces in file or folder names. Names do not change when a title changes. |
| 3 | The top folders are the **business processes**, named as in [`foundations/business-processes`](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) (for example `bp05b-provider-manages-resource-descriptions`). | Each business process page and its requirements can link to each other with one fixed path. |
| 4 | An item with **two parents** is placed once, under its first parent; the other parent lists it as "also under". An item **without a parent** goes into a folder `no-business-process`. | No item is published twice; no item is missing. |
| 5 | An item that is no longer eligible (cancelled, or out of the status range) is **removed** at the next run. | The tree only holds eligible items. The history stays in git. |
| 5a | **Only approved Epics and user stories** are published, each on its own status: Epics from *Defined* onwards, user stories from *PBI Defined* onwards. A user story whose Epic is not approved is left out too, even when the story itself is approved. L0, L1 and L2 are not filtered on status, because their status is not kept up to date; for them, publication on the requirements site decides (requirement 10). | No Epic or user story before approval appears, not even its title. |

## Priority 1: the content

The reader comes for the requirement. Workflow data means nothing to them.

| # | Requirement | Done when |
| --- | --- | --- |
| 6 | Each page shows **title, level, Jira key (as plain text), text, parent and children**. Nothing else: no priority, completion rate, versions, dates or names of people, and no status except the development status of user stories (requirement 16). | No other workflow field appears on any page. |
| 7 | **No links to Jira or Confluence.** Both need a login. Internal links in a text become plain text. | No link on any page asks for a login. |
| 8 | **Content first**: the page opens with the title and the text. Parent and children come after. | The requirement is visible without scrolling. |
| 9 | **One layout** for user stories and acceptance criteria, whatever the source looks like: *As a* / *I want to* / *So that* on three lines; one numbered scenario per *Given*, with *When*, *Then*, *And* on their own lines. Words are never changed. Jira markup is converted, so no raw HTML or Jira codes remain. | A sample of 20 user stories from different teams reads the same way. |
| 10 | An L0, L1 or L2 that is not published on the requirements site shows its title, level and key only, without its text. Comments and attachments are never published. | Nothing unpublished leaks. |
| 16 | User stories show their **development status**, mapped to three labels: *Planned* (PBI Defined), *In development* (DevTeam PBI In Progress, PBI INTERNAL QA, DevTeam PBI blocked), *Developed* (DevTeam PBI is ready for Implementation check, Ready for DG Acceptance, PBI Closed with resolution Done). The page explains that Developed means built, not necessarily released. Other levels show no status. A PBI Closed with another resolution (for example Invalid) is not published. | Every user story shows one of the three labels, and nothing else of its workflow. |

## Priority 2: context for the reader

A first-time reader must understand where they are, from any page.

| # | Requirement | Done when |
| --- | --- | --- |
| 11 | A **start page** that says what Simpl-Open is, what the repository holds, and what the levels mean (definitions from the Development Handbook). | A newcomer understands the repository in two minutes. |
| 12 | Every page starts with **one line of context**: what the item is and where it sits, from the business process down. | Any page can be the entry point. |
| 13 | **Links say where they go**: "5B.2.1 on the requirements site", "Epic: A Provider adds contract clauses...", never a bare key or number. Links lead down the tree or back up to the parent, not in circles. | No link text is a bare ID. |
| 14 | Each L1 and L2 links to its **public page on the requirements site**, and each business process folder to its **public process page**. | Traceability works without a login. |
| 15 | Under an L2, each Epic shows **one line from its story card** (*As a ..., I want to ...*), so the reader knows what it is about before opening it. | Every Epic with a story card has its line. |

## Not in this scope

- Tasks, bugs and tests.
- The approved L0 content that today sits in Confluence. PSO plans this as a next phase.
- A quality report on the source (orphans, missing links, typos, approved user stories under an Epic that is not approved). Useful for PSO and the teams, not for the public pages. In the Jira export of 10 November 2026, 40 approved user stories sit under Epics that are not approved (yet, or again).

## Open points for DG CONNECT

- **Location.** A `requirements` folder in `foundations` (Model 1), a separate repository, or inside each business process folder (as in the example below).
- **Process steps.** Linking a detailed requirement to a process step (for example 5B.2.3 to BP05B.02) needs a field in Jira. Today it is done by hand.
- **One text.** Jira and the requirements site have almost the same texts, with small differences. The publication should use one source.
- **Reopened Epics.** An Epic that is reopened for correction goes back to In Preparation. Under requirement 5a, its user stories then disappear from the publication, even the ones already developed, until the Epic is approved again. Example: SIMPL-10199. Is that what we want, or should the last approved version stay published?
- **FOR SC TO RESPOND.** This user story status can come after PBI Defined as well. PSO to confirm where it sits in the workflow, so the story is not dropped by mistake.

## Examples

These show the requirements in practice. They are made by hand from a Jira export of 10 November 2026, as inspiration, not as the implementation.

- [The full tree](items/README.md) of the ten items of PSO's first test, from business process down to user story. It shows the folder tree (requirements 1, 2 and 4), the layout (9 and 10) and the context for the reader (11 and 12). It also shows the development status of user stories (16).
- [Migration example BP05B](migration-example/README.md): one business process page as it is today, next to the same page with its requirements, Epics and user stories built in. It shows the reading experience (7, 9, 13, 14 and 15). It keeps the file layout of today's business process folder, so it does not show the folder tree of requirement 1.
