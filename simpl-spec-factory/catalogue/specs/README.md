# Input specs: Catalogue

Frozen input set for the third factory candidate, collected **2026-10-28**. As for the Schema Manager, it holds only material that is published or will be published: no review comments, no Confluence pages. The factory works from these files only. Do not refresh them in place: a new copy is a new, dated input set.

Scope chosen: the **whole catalogue**: the federated catalogue (catalogue-be), the query mapper adapter, the catalogue client backend and the search UI.

## What is here

| Folder | Content | Source |
|---|---|---|
| `stories/` | 33 user stories: every live story (not cancelled) of the Jira components "Federated Catalogue", "Search" and "XSFC Components". Key, summary, status, dates and the description with the acceptance criteria; same layout as the earlier candidates. Review comments, links and authors are left out. | Jira SIMPL, collected with [`pipeline/collect_stories.py`](../../pipeline/collect_stories.py): a personal access token and a local model (Mistral Small 3.2 through Ollama). 28 converted by the model, 5 by the plain fallback (SIMPL-31148, SIMPL-31151, SIMPL-31152, SIMPL-31767, SIMPL-31769); all with a word match of 0.98 or more against the source |
| `api/catalogue-be/` | OpenAPI of the federated catalogue: root `fc_openapi.yaml` (references only), operations in `tier1-gateway/` and `tier2-gateway/`, shared parts in `common/`, self-contained `*-itb.yaml` versions, layout `README.md`. 17 paths | code.europa.eu `simpl/simpl-open/integration/resource-discovery/resource-catalogue/federated-catalogue/catalogue-be`, commit `fab79eb` (main, 2026-10-21) |
| `api/query-mapper-adapter-be/` | OpenAPI of the query mapper adapter, same composed layout, root `adapter_openapi.yaml` 1.2.0, plus `resource-relationships.md`. 7 paths | `.../search-engine/catalogue-search/query-mapper-adapter-be`, commit `b395914` (main, 2026-10-22) |
| `api/catalogue-client-be/` | OpenAPI of the catalogue client backend (advanced search), versions v1 and v2. 5 paths in v2 | `.../search-engine/catalogue-search/catalogue-client-be`, commit `5b4c4c6` (main, 2026-10-21) |
| `functional/bp05b-*/` | Business process BP05b "Provider manages resource descriptions" (9 sub-processes) | code.europa.eu `simpl/simpl-open/foundations/business-processes`, commit `8090f32` (main, 2026-10-23), without the `media/` diagrams |
| `functional/bp06-*/` | Business process BP06 "Consumer searches resources" (10 sub-processes) | same commit |

## Not included, on purpose

- **Upstream source code and tests.** Spec-only generation.
- **The 5 cancelled stories** of the same components, and the `*-legacy.yaml` OpenAPI snapshots.
- **Review comments** on the stories, roadmap item descriptions in Confluence (not published).
- **Diagrams** of the business processes (`media/`), and any Figma design linked from the stories.

## First observations (input to [`findings.md`](../findings.md))

- **The core catalogue has no stories.** Publishing, revoking and versioning a resource description, quick search and advanced search exist as API operations and as business processes (BP05b, BP06), but no live story describes them. The 33 stories are all from 2026 (May to October) and cover new features: federated search across data spaces, the federated search schema, quality filters and the quality assessment record.
- **Many stories are not settled:** 10 are "FOR SC TO RESPOND", 6 "PBI Defined", 4 in review. 11 are "ready for implementation check". 8 stories have no Given/When/Then criteria.
- **Three backends with overlapping paths:** `/selfDescriptions/advancedSearch` is in both catalogue-be and the adapter, schema paths in both catalogue-be and catalogue-client-be.
- **Review notes inside the contract:** the active catalogue-be files still carry 13 inline notes from an early design review (`#!! ... -> answer`), for example that a field is "NOT FURTHER SPECIFIED". They are part of the contract text the writers read.
- The local upstream stack (`simpl-catalogue`) builds from the earlier `development/gaia-x-edc` repositories, a May 2026 version. It predates most of these stories.

## Story index

| Key | Kind | Given/When/Then | Status | Summary |
|---|---|---|---|---|
| [SIMPL-27998](stories/SIMPL-27998.md) |  | yes | DevTeam PBI is ready for Implementation check | Catalogue validates against the Quality Assesment Record and decides the publication before storing self-description |
| [SIMPL-29292](stories/SIMPL-29292.md) | UI | yes | DevTeam PBI is ready for Implementation check | [UI] Provider use quick search with quality filter (minimum score and classification) |
| [SIMPL-29293](stories/SIMPL-29293.md) | UI | yes | DevTeam PBI is ready for Implementation check | [UI] Provider use advanced search with quality filter (minimum score and classification) |
| [SIMPL-30848](stories/SIMPL-30848.md) |  | yes | PBI Defined | Trusted GA configure Search endpoint of a trustor data space |
| [SIMPL-30850](stories/SIMPL-30850.md) | UI | yes | FOR SC TO RESPOND | Trusted GA create new version of federated search schema via UI |
| [SIMPL-31102](stories/SIMPL-31102.md) | UI | yes | DevTeam PBI is ready for Implementation check | [UI] A Consumer selects federated catalogues for quick search via UI |
| [SIMPL-31103](stories/SIMPL-31103.md) | UI | yes | DevTeam PBI is ready for Implementation check | [UI] A Consumer selects federated catalogues for advanced search via UI |
| [SIMPL-31106](stories/SIMPL-31106.md) | API | no | PBI For DG Review | A Trustor Governance Authority provides resource descriptions matching a forwarded federated search request to a Trusted Governance Authority via API |
| [SIMPL-31111](stories/SIMPL-31111.md) |  | no | FOR SC TO RESPOND | The Governance Authority filters resource descriptions matching a quick search request from own catalogues |
| [SIMPL-31112](stories/SIMPL-31112.md) |  | no | FOR SC TO RESPOND | The Governance Authority filters resource descriptions matching an advanced search request from own catalogues |
| [SIMPL-31113](stories/SIMPL-31113.md) | API | no | FOR SC TO RESPOND | A Trusted Governance Authority forwards federated search requests to Trustor Governance Authorities via API |
| [SIMPL-31114](stories/SIMPL-31114.md) |  | no | FOR SC TO RESPOND | A Governance Authority maps identity attributes from a federated search request |
| [SIMPL-31138](stories/SIMPL-31138.md) | API | yes | FOR SC TO RESPOND | [API-Implementation] A Governance Authority provides resource descriptions matching a quick search request from a Trusted Governance Authority |
| [SIMPL-31139](stories/SIMPL-31139.md) | API | yes | PBI Defined | [API Implementation]A Governance Authority provides resource descriptions matching an advanced search request from a Trusted Governance Authority |
| [SIMPL-31140](stories/SIMPL-31140.md) |  | yes | PBI Defined | A Governance Authority provides a consolidated search result to a Consumer |
| [SIMPL-31141](stories/SIMPL-31141.md) | UI | yes | PBI for SC review | [UI] A Consumer consults the overview of resource descriptions returned by a search request via UI |
| [SIMPL-31142](stories/SIMPL-31142.md) | UI | yes | FOR SC TO RESPOND | [UI] A Consumer consults the overview of resource descriptions returned from federated catalogues by a search request via UI |
| [SIMPL-31143](stories/SIMPL-31143.md) | UI | yes | PBI for SC review | A Consumer consults the full contents of a resource description returned by a search request via UI |
| [SIMPL-31148](stories/SIMPL-31148.md) | UI | yes | PBI Defined | [UI] A Consumer consults the full contents of a resource description returned from a federated catalogue by a search request via UI |
| [SIMPL-31149](stories/SIMPL-31149.md) | API | no | FOR SC TO RESPOND | A Governance Authority forwards a request for the details of a resource description to a Trustor Governance Authority via API |
| [SIMPL-31150](stories/SIMPL-31150.md) | API | yes | DevTeam PBI In Progress | [API Implementation] A Governance Authority handles request for the details of a resource description from a Trusted Governance Authority via API |
| [SIMPL-31151](stories/SIMPL-31151.md) |  | no | PBI Defined | A Governance Authority maps identity attributes from a request for the details of a resource description |
| [SIMPL-31152](stories/SIMPL-31152.md) |  | no | PBI Defined | A Governance Authority provides the details of resource description for a forwarded request to the Trusted Governance Authority requesting it |
| [SIMPL-31153](stories/SIMPL-31153.md) | API | yes | FOR SC TO RESPOND | A Trusted Governance Authority forwards and receives resource-description details via API |
| [SIMPL-31767](stories/SIMPL-31767.md) | API | yes | DevTeam PBI is ready for Implementation check | [BE] A Consumer consults the overview of resource descriptions returned by a search request via API |
| [SIMPL-31768](stories/SIMPL-31768.md) | API | yes | For PSO Review | A Consumer consults the overview of resource descriptions returned from federated catalogues by a search request via API |
| [SIMPL-31769](stories/SIMPL-31769.md) | API | yes | PBI For DG Review | A Consumer consults the full contents of a resource description returned by a search request via API |
| [SIMPL-31770](stories/SIMPL-31770.md) | API | yes | DevTeam PBI is ready for Implementation check | [BE] A Consumer consults the full contents of a resource description returned from a federated catalogue by a search request via API |
| [SIMPL-31815](stories/SIMPL-31815.md) | API | yes | DevTeam PBI is ready for Implementation check | GA defines federated Search Schema via API |
| [SIMPL-31816](stories/SIMPL-31816.md) | UI | yes | DevTeam PBI is ready for Implementation check | [UI] Trusted GA defines federated Search Schema via UI |
| [SIMPL-31817](stories/SIMPL-31817.md) | API | yes | DevTeam PBI is ready for Implementation check | GA create new version of federated search schema via API |
| [SIMPL-33885](stories/SIMPL-33885.md) | API | yes | DevTeam PBI is ready for Implementation check | [API Implementation] API to get detail of federated resource description (xfsc-adv-search) |
| [SIMPL-35874](stories/SIMPL-35874.md) | UI | yes | FOR SC TO RESPOND | [UI] Advanced Search: Use SEARCH Schema for Federated Catalogue Queries |
