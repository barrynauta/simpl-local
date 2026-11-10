> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14738](../../README.md) / [Epic-SIMPL-19293](../README.md) / **Story-SIMPL-21651**

# SIMPL-21651: [UI] A Provider starts the creation of a new version of a Resource description via UI

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14738](../../README.md) (L2) › [SIMPL-19293](../README.md) (Epic) › **SIMPL-21651** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want|to start the creation of a new version of a Resource description via UI |
| --- |

| so that|I can align the resource description with the latest information related to my resource. |
| --- |

#### Acceptance criteria

| Scenario | A Provider starts the creation of a new version of a resource description via UI - positive case |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am consulting the full contents of a Resource description |
| --- |

| When|I demand to create a new version of this Resource description (by pressing the "Create New Version" button on the resource description detail view) |
| --- |

| Then|I am guided to the first step of the wizard based on the latest version of the underlying schema for this resource type (obtained via API). |
| --- |
|  |  |

| Scenario | A Provider starts the creation of a new version of a resource description via UI - API error |
| --- | --- |

| Given|I am authenticated as Provider |
| --- |

| and|I have a role authorising the management of resource descriptions |
| --- |

| and|I am consulting the full contents of a Resource description |
| --- |

| When|I demand to create a new version of this Resource description (by pressing the "Create New Version" button on the resource description detail view)Then |
| --- |

| and |the API to retrieve the latest version of the schema of the updated resource description throws an error |
| --- |

| Then|the creation of a new version of a resource description is not possible |
| --- |

| and|a notification is shown that a new version of the resource description cannot be created. |
| --- |
|  |  |

| Scenario | A Provider starts the creation of a new version of a resource description via UI - not authenticated |
| --- | --- |

| Given|I am not authenticated as Provider |
| --- |

| When|try to open the resource description management page |
| --- |

| Then |I cannot open the resource description management page. |
| --- |
|  |  |

| Scenario | A Provider starts the creation of a new version of a resource description via UI  - no role |
| --- | --- |

| Given|I am authenticated as Provider |
| --- |

| and |I do not have a role authorising the management of resource rescriptions |
| --- |

| When|try to open the resource description management page |
| --- |

| Then |I cannot open the resource description management page. |
| --- |

#### Additional notes

Trigger for a new version on the detail page:

(image not shown)

(image not shown)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-19293](../README.md) (Epic) A Provider starts the creation of a new version of a Resource description via UI |
