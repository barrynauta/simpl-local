> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14730](../../README.md) / [Epic-SIMPL-19176](../README.md) / **Story-SIMPL-20613**

# SIMPL-20613: [UI] A provider selects the Resource type in the UI

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14730](../../README.md) (L2) › [SIMPL-19176](../README.md) (Epic) › **SIMPL-20613** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want|to be able to select a resource type in the UI |
| --- |

| so that|I can start the creation of a new resource description. |
| --- |

#### Acceptance criteria

| Given|I am authenticated as Provider |
| --- |

| and|I have a role authorising the management of Resource Descriptions |
| --- |

| and |I have opened the resource description management page |
| --- |

| when|I select a resource type from the list of published resource types |
| --- |

| then|the resource description creation wizard is opened |
| --- |

| and|the steps of the wizard are based on the entities in the schema of the selected resource type. |
| --- |

*The contents of the individual steps of the wizard is covered in other user stories and out of the scope of this story.*

#### Mockups:

| Steps of the wizard |
| --- |
| !image-2025-11-12-11-51-43-402.png | thumbnail! |

#### Additional information

- The dropdown containing the resource types that can be selected is implemented in (internal page)
- If the dropdown does not contain any resource type, meaning that the dataspace does not have any schema, then, obviously, nothing can be selected and no wizard can be opened.
- API to retrieve the schema defined in (internal page) and implemented in (internal page)
- "Entities" in the schema are defined on [an internal page|an internal page] and, with respect to the UI, further described on an internal page

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-19176](../README.md) (Epic) A Provider retrieves from the Governance Authority the schema of a published resource type |
