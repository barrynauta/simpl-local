# Input specs: Vocabulary Manager

Frozen input set for the first factory run, collected **2026-10-27**. The factory works from these files only. Do not refresh them in place: a new copy is a new, dated input set.

## What is here

| Folder | Content | Source |
|---|---|---|
| `stories/` | 35 user stories, one file each: key, summary, status, dates and the description with the Given/When/Then acceptance criteria, as shown on each Jira issue page (rendered text, so link targets and formatting are not kept). Review comments, links and authors are left out. | Jira SIMPL, copied from the issue pages in the browser |
| `api/vocabulary_openapi-tier1.yaml` | OpenAPI 3, "Vocabulary Management API" 1.0.0, 13 paths | code.europa.eu `simpl/simpl-open/data/semantics-and-vocabulary/vocabulary-hub/vocabulary-manager/vocabulary-manager-be`, commit `7755e1f` (main, 2026-09-29) |
| `functional/bp02b-manage-vocabularies.md` | Business process "Manage vocabularies" | code.europa.eu `simpl/simpl-open/foundations/business-processes`, commit `8090f32` (main, 2026-10-23); canonical source: simpl-programme.ec.europa.eu |
| R07.02 tech description (not copied here) | Roadmap item tech description: scope, roles, domain model, lifecycle and validation rules, draft API | Confluence page 137235370, v5 (2026-05-20). Not yet published, so only referenced; the spec digest quotes what it uses. |

## Not included, on purpose

- **Upstream source code and tests.** Spec-only generation; tests may be used afterwards as a check, never as input.
- **The 3 cancelled stories:** SIMPL-18179 (Warning on deprecated vocabulary use via API), SIMPL-18185 (Automatic status validation during new vocabulary/version creation via API), SIMPL-18192 (Automatic deprecation of previous vocabulary version via API).
- **Review comments** on the stories (296) and on the Confluence BP02B page (180). They are review discussion, not specification, and contain names. Clarifications that mattered are captured as decisions in the spec digest.
- **Local-only originals.** An earlier, fuller copy of the same stories (with review comments) and the Confluence exports are kept in `specs/_private/` (gitignored). Factory run 1 used that fuller copy. The story text is the same as in `stories/`, apart from link targets and formatting; the BP02B Confluence version (v155) and the GitLab version have the same content structure.
- **The Figma design** linked from the stories (not reachable from here) and one image attachment (`vocab.png` on page 151552237). The UI is generated without the visual design; record that as a limitation.

## First observations (input to `findings.md`)

- The tech description's draft API (`PATCH /vocabularies/{name}/status`, public `GET /vocabulary/{name}`) does not match the OpenAPI paths. Which one is the spec?
- The OpenAPI has five `/external-vocabularies` endpoints with no story of their own; only the tech description covers them ("External reference mapping management").
- Stories split 14 "via API", 20 "via UI" and 1 other. 10 are still "ready for implementation check", so upstream may not implement them yet.

## Story index

| Key | Kind | Status | Summary |
|---|---|---|---|
| [SIMPL-28680](stories/SIMPL-28680.md) | API | DevTeam PBI is ready for Implementation check | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile |
| [SIMPL-18176](stories/SIMPL-18176.md) | UI | Ready for DG Acceptance | Access deprecated vocabulary versions in UI |
| [SIMPL-18164](stories/SIMPL-18164.md) | API | Ready for DG Acceptance | Apply filters and sorting via vocabulary list API |
| [SIMPL-18184](stories/SIMPL-18184.md) | UI | DevTeam PBI is ready for Implementation check | Automatic check for deprecated vocabularies during new version creation in UI |
| [SIMPL-18191](stories/SIMPL-18191.md) | UI | Ready for DG Acceptance | Automatic deprecation of previous vocabulary version UI |
| [SIMPL-18186](stories/SIMPL-18186.md) | UI | Ready for DG Acceptance | Automatic publication of vocabularies UI |
| [SIMPL-18190](stories/SIMPL-18190.md) | API | Ready for DG Acceptance | Automatic versioning of vocabularies via API |
| [SIMPL-18189](stories/SIMPL-18189.md) | UI | Ready for DG Acceptance | Automatic versioning of vocabularies via UI |
| [SIMPL-18173](stories/SIMPL-18173.md) | API | Ready for DG Acceptance | Create a new version of a vocabulary via API |
| [SIMPL-18172](stories/SIMPL-18172.md) | UI | Ready for DG Acceptance | Create a new version of a vocabulary via UI |
| [SIMPL-18170](stories/SIMPL-18170.md) | API | Ready for DG Acceptance | Create a new vocabulary via API |
| [SIMPL-18169](stories/SIMPL-18169.md) | UI | DevTeam PBI is ready for Implementation check | Create a new vocabulary via UI |
| [SIMPL-18452](stories/SIMPL-18452.md) | UI | Ready for DG Acceptance | Download and view full details of all vocabulary versions in the UI |
| [SIMPL-18165](stories/SIMPL-18165.md) | UI | Ready for DG Acceptance | Download and view full details of the newest vocabulary version in the UI |
| [SIMPL-393](stories/SIMPL-393.md) | other | Ready for DG Acceptance | Provide CPS (Core Public Services) vocabulary |
| [SIMPL-395](stories/SIMPL-395.md) | UI | DevTeam PBI is ready for Implementation check | Provide UI for additional vocabulary |
| [SIMPL-18187](stories/SIMPL-18187.md) | API | Ready for DG Acceptance | Publish vocabulary via API |
| [SIMPL-18194](stories/SIMPL-18194.md) | API | Ready for DG Acceptance | Receive vocabulary validation report via API |
| [SIMPL-18193](stories/SIMPL-18193.md) | UI | Ready for DG Acceptance | Receive vocabulary validation report via UI |
| [SIMPL-18166](stories/SIMPL-18166.md) | API | Ready for DG Acceptance | Retrieve full details of a vocabulary version via API |
| [SIMPL-18162](stories/SIMPL-18162.md) | API | Ready for DG Acceptance | Retrieve metadata of all vocabularies via API |
| [SIMPL-18168](stories/SIMPL-18168.md) | API | Ready for DG Acceptance | Retrieve version history of a vocabulary via API |
| [SIMPL-18196](stories/SIMPL-18196.md) | API | Ready for DG Acceptance | Role-based access control for vocabulary management API |
| [SIMPL-18195](stories/SIMPL-18195.md) | UI | Ready for DG Acceptance | Role-based access control for vocabulary management UI |
| [SIMPL-18163](stories/SIMPL-18163.md) | UI | Ready for DG Acceptance | Sort and filter vocabulary list via UI |
| [SIMPL-18175](stories/SIMPL-18175.md) | API | Ready for DG Acceptance | Update vocabulary status via API |
| [SIMPL-18174](stories/SIMPL-18174.md) | UI | DevTeam PBI is ready for Implementation check | Update vocabulary status via UI |
| [SIMPL-18183](stories/SIMPL-18183.md) | API | DevTeam PBI is ready for Implementation check | Validate vocabulary semantics via API |
| [SIMPL-18182](stories/SIMPL-18182.md) | UI | DevTeam PBI is ready for Implementation check | Validate vocabulary semantics via UI |
| [SIMPL-18180](stories/SIMPL-18180.md) | UI | DevTeam PBI is ready for Implementation check | Validate vocabulary syntax and structure via UI |
| [SIMPL-18181](stories/SIMPL-18181.md) | API | Ready for DG Acceptance | Validate vocabulary syntax via API |
| [SIMPL-18161](stories/SIMPL-18161.md) | UI | Ready for DG Acceptance | View All vocabularies and their definitions via UI |
| [SIMPL-18167](stories/SIMPL-18167.md) | UI | Ready for DG Acceptance | View version history of a vocabulary in the UI |
| [SIMPL-18178](stories/SIMPL-18178.md) | UI | DevTeam PBI is ready for Implementation check | Warning on deprecated vocabularies in Vocabulary Management UI |
| [SIMPL-18559](stories/SIMPL-18559.md) | UI | DevTeam PBI is ready for Implementation check | Warning on deprecated vocabulary versions in Vocabulary History UI |
