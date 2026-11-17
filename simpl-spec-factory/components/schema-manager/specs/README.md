# Input specs: Schema Manager

Frozen input set for the second factory candidate, collected **2026-10-28**. Unlike the first candidate, this set holds only material that is published or will be published: no review comments, no Confluence pages. The factory works from these files only. Do not refresh them in place: a new copy is a new, dated input set.

## What is here

| Folder | Content | Source |
|---|---|---|
| `stories/` | 55 user stories: every live story (not cancelled) of the Jira component "Schema Management". Key, summary, status, dates and the description with the Given/When/Then acceptance criteria; same layout as the first candidate. Review comments, links and authors are left out. | Jira SIMPL, collected with [`pipeline/collect_stories.py`](../../pipeline/collect_stories.py): a personal access token and a local model (Mistral Small 3.2 through Ollama). 50 converted by the model, 5 by the plain fallback (SIMPL-32258, SIMPL-32436, SIMPL-32787, SIMPL-32800, SIMPL-32807); all with a word match of 0.99 or more against the source |
| `api/schema_openapi.yaml` | OpenAPI 3, "Schema Management API" 1.1.0, 12 paths. The openapi README calls this file the active, canonical contract | code.europa.eu `simpl/simpl-open/data/semantics-and-vocabulary/schema-management/schema-manager/schema-manager-be`, commit `f9e5205` (main, 2026-10-21) |
| `functional/bp02c-manage-resource-description-schemas.md` | Business process "Manage resource description schemas" | code.europa.eu `simpl/simpl-open/foundations/business-processes`, commit `85d1b5a` (main, 2026-09-10) |

## Not included, on purpose

- **Upstream source code and tests.** Spec-only generation.
- **The 36 cancelled stories** of the same component.
- **Review comments** on the stories, and the roadmap item tech description in Confluence (not published).
- **The Figma design**, if linked from the stories.

## First observations (input to [`findings.md`](../findings.md))

- The OpenAPI contains six Vocabulary Manager operations that the service does not implement (see findings).
- 11 stories are marked "[ENHANCE]" and revisit an earlier story on the same feature. The specs do not say whether an enhancement replaces or extends the original.
- Some stories may belong to other components: federated search schema (SIMPL-31098, SIMPL-33537, in progress) and a billing schema added by a Provider (SIMPL-28815).
- Status: 18 "Ready for DG Acceptance", 28 "ready for implementation check", the rest in progress or review. The local upstream stack is a May 2026 clone, so it predates the 2026 enhancement stories.

## Story index

| Key | Kind | Enhance | Status | Summary |
|---|---|---|---|---|
| [SIMPL-13343](stories/SIMPL-13343.md) | API |  | DevTeam PBI is ready for Implementation check | Governance Authority - retrieving a list of all schemas metadata via API |
| [SIMPL-13345](stories/SIMPL-13345.md) | API |  | Ready for DG Acceptance | Governance Authority - retrieving metadata for all versions of a schema via API |
| [SIMPL-13346](stories/SIMPL-13346.md) | UI |  | Ready for DG Acceptance | Governance Authority - retrieving metadata for all versions of a schema via UI |
| [SIMPL-13347](stories/SIMPL-13347.md) | API |  | Ready for DG Acceptance | Governance Authority - retrieving the contents of the latest version of a schema via API |
| [SIMPL-13353](stories/SIMPL-13353.md) | API |  | DevTeam PBI is ready for Implementation check | Governance Authority – providing details of a new schema via an API |
| [SIMPL-13355](stories/SIMPL-13355.md) | UI |  | Ready for DG Acceptance | Governance Authority – providing details of a new schema via a UI |
| [SIMPL-13357](stories/SIMPL-13357.md) | API |  | DevTeam PBI is ready for Implementation check | Governance Authority – providing details of a new version of an existing schema via API |
| [SIMPL-13358](stories/SIMPL-13358.md) | UI |  | Ready for DG Acceptance | Governance Authority – providing details of a new version of an existing schema via UI |
| [SIMPL-13360](stories/SIMPL-13360.md) | API |  | Ready for DG Acceptance | Governance Authority – revoking a schema via API |
| [SIMPL-13361](stories/SIMPL-13361.md) | UI |  | DevTeam PBI is ready for Implementation check | [UI] Governance Authority revoking a schema via UI |
| [SIMPL-13364](stories/SIMPL-13364.md) | API |  | DevTeam PBI is ready for Implementation check | Governance Authority - retrieving details of a schema version via API |
| [SIMPL-13365](stories/SIMPL-13365.md) | UI |  | Ready for DG Acceptance | Governance Authority - retrieving details of a schema version via a UI |
| [SIMPL-14354](stories/SIMPL-14354.md) | API |  | Ready for DG Acceptance | Governance Authority – retaining revoked schema for referencing where the schema was revoked via API |
| [SIMPL-14701](stories/SIMPL-14701.md) | UI |  | Ready for DG Acceptance | Governance Authority – retaining revoked schema for referencing where the schema was revoked via UI |
| [SIMPL-14703](stories/SIMPL-14703.md) | other |  | DevTeam PBI is ready for Implementation check | Governance Authority – validating a schema’s syntax and ensuring default properties |
| [SIMPL-14709](stories/SIMPL-14709.md) | other |  | DevTeam PBI is ready for Implementation check | Governance Authority – notifying Providers about when publishing a new schema |
| [SIMPL-14712](stories/SIMPL-14712.md) | other |  | DevTeam PBI is ready for Implementation check | Governance Authority – notifying Providers about schema changes - new version of the schema |
| [SIMPL-14791](stories/SIMPL-14791.md) | other |  | Ready for DG Acceptance | Governance Authority – notifying Providers about schema changes - revoked schema |
| [SIMPL-14795](stories/SIMPL-14795.md) | API |  | Ready for DG Acceptance | [API] Governance Authority – receiving an error message when submitting a syntactically invalid schema |
| [SIMPL-14796](stories/SIMPL-14796.md) | API |  | Ready for DG Acceptance | [API] Governance Authority – receiving an error message about submitting an invalid schema - missing mandatory or default property |
| [SIMPL-14797](stories/SIMPL-14797.md) | API |  | DevTeam PBI is ready for Implementation check | [API] Governance Authority – receiving an error message about submitting a schema not complying with its validation rules |
| [SIMPL-15837](stories/SIMPL-15837.md) | UI |  | DevTeam PBI is ready for Implementation check | Governance Authority - retrieving a list of all schemas metadata and displaying them in UI |
| [SIMPL-17066](stories/SIMPL-17066.md) | other |  | DevTeam PBI is ready for Implementation check | Resource Description Schema Management Access Control - Tier 1 |
| [SIMPL-19992](stories/SIMPL-19992.md) | API |  | DevTeam PBI is ready for Implementation check | API for Subscription Management of Publishing Events |
| [SIMPL-20067](stories/SIMPL-20067.md) | UI |  | Ready for DG Acceptance | [UI] Governance Authority retrieving the contents of latest version of a schema via UI |
| [SIMPL-20528](stories/SIMPL-20528.md) | other |  | Ready for DG Acceptance | Governance Authority automatically assigning an initial version number to new schema based on the applicable versioning algorithm |
| [SIMPL-20529](stories/SIMPL-20529.md) | API |  | DevTeam PBI is ready for Implementation check | [API] Governance Authority -  change the status of a schema via API |
| [SIMPL-20530](stories/SIMPL-20530.md) | UI |  | DevTeam PBI is ready for Implementation check | [UI] Governance Authority changes the status of a schema via UI |
| [SIMPL-20534](stories/SIMPL-20534.md) | other |  | Ready for DG Acceptance | Governance Authority automatically assigning the next version number, based on the applicable versioning algorithm |
| [SIMPL-20686](stories/SIMPL-20686.md) | other |  | DevTeam PBI is ready for Implementation check | Automatic status assignment when creating schemas by resourceType |
| [SIMPL-20770](stories/SIMPL-20770.md) | UI |  | DevTeam PBI is ready for Implementation check | [UI] Remove version number field from Create Schema Version screen |
| [SIMPL-22407](stories/SIMPL-22407.md) | API |  | DevTeam PBI is ready for Implementation check | [API] Governance Authority – validating a schema’s semantics |
| [SIMPL-24172](stories/SIMPL-24172.md) | UI |  | Ready for DG Acceptance | [UI] Governance Authority – displaying an error message when submitting a syntactically invalid schema |
| [SIMPL-24173](stories/SIMPL-24173.md) | UI |  | Ready for DG Acceptance | [UI] Governance Authority – receiving a notification about submitting an invalid schema - missing mandatory or default property |
| [SIMPL-24174](stories/SIMPL-24174.md) | UI |  | Ready for DG Acceptance | [UI] Governance Authority – semantic validation message after submitting a schema |
| [SIMPL-24175](stories/SIMPL-24175.md) | other |  | DevTeam PBI is ready for Implementation check | Resource Description Schema Management Access Control - Tier 2 |
| [SIMPL-26105](stories/SIMPL-26105.md) | other |  | DevTeam PBI is ready for Implementation check | Governance Authority – validating a schema’s syntax and ensuring UUID and Version default properties - improvement |
| [SIMPL-28815](stories/SIMPL-28815.md) | UI |  | DevTeam PBI is ready for Implementation check | A Provider adds a billing schema to a new resource description via UI |
| [SIMPL-31098](stories/SIMPL-31098.md) | other |  | DevTeam PBI In Progress | Governance Authority - publishing a validated federated search schema |
| [SIMPL-32258](stories/SIMPL-32258.md) | API |  | DevTeam PBI is ready for Implementation check | Governance Authority - retrieving list of schemas via API [Extension] |
| [SIMPL-32436](stories/SIMPL-32436.md) | API |  | DevTeam PBI is ready for Implementation check | Governance Authority – retaining revoked schema for resource descriptions for referencing via API |
| [SIMPL-32497](stories/SIMPL-32497.md) | UI |  | PBI for SC review | [UI] Governance Authority – retaining revoked schema for referencing via UI |
| [SIMPL-32786](stories/SIMPL-32786.md) | API | yes | PBI INTERNAL QA | ENHANCE - Governance Authority - retrieving metadata for all versions of a schema via API |
| [SIMPL-32787](stories/SIMPL-32787.md) | UI | yes | DevTeam PBI is ready for Implementation check | [ENHANCE][UI] - Governance Authority - retrieving metadata for all versions of a schema via UI |
| [SIMPL-32788](stories/SIMPL-32788.md) | API | yes | PBI INTERNAL QA | [ENHANCE] - Governance Authority - retrieving the contents of the latest version of a schema via API |
| [SIMPL-32790](stories/SIMPL-32790.md) | UI | yes | DevTeam PBI is ready for Implementation check | [ENHANCE][UI] - Governance Authority - retrieving a list of all schemas metadata and displaying them in UI |
| [SIMPL-32797](stories/SIMPL-32797.md) | UI | yes | FOR SC TO RESPOND | [ENHANCE][UI] - Governance Authority - retrieving details of a schema version via a UI |
| [SIMPL-32798](stories/SIMPL-32798.md) | API | yes | DevTeam PBI is ready for Implementation check | [ENHANCE] - Governance Authority - retrieving details of a schema version via API |
| [SIMPL-32800](stories/SIMPL-32800.md) | UI | yes | DevTeam PBI is ready for Implementation check | [ENHANCE][UI] - Governance Authority – providing details of a new schema via a UI |
| [SIMPL-32801](stories/SIMPL-32801.md) | other | yes | PBI INTERNAL QA | [ENHANCE] - Automatic status assignment when creating schemas by resourceType |
| [SIMPL-32802](stories/SIMPL-32802.md) | API | yes | DevTeam PBI is ready for Implementation check | [ENHANCE] - Governance Authority – providing details of a new schema via an API |
| [SIMPL-32807](stories/SIMPL-32807.md) | API | yes | DevTeam PBI In Progress | [ENHANCE] - Governance Authority – providing details of a new version of an existing schema via API |
| [SIMPL-32808](stories/SIMPL-32808.md) | UI | yes | DevTeam PBI is ready for Implementation check | [ENHANCE][UI] - Governance Authority – providing details of a new version of an existing schema via UI |
| [SIMPL-33537](stories/SIMPL-33537.md) | API |  | DevTeam PBI In Progress | [API Implementation] - API to manage federated Search Schema |
| [SIMPL-33694](stories/SIMPL-33694.md) | other |  | PBI INTERNAL QA | Governance Authority notifies Providers about schema changes only for RESOURCE schemas |
