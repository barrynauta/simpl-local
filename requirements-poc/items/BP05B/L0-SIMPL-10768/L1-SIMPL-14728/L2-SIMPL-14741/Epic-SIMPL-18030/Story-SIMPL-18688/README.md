> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14741](../../README.md) / [Epic-SIMPL-18030](../README.md) / **Story-SIMPL-18688**

# SIMPL-18688: [UI] Assign usage policies to the new version of Resource description

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14741](../../README.md) (L2) › [SIMPL-18030](../README.md) (Epic) › **SIMPL-18688** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want to|update the usage policies of a new version of a resource description via UI |
| --- |

| so that|the contents of the resource description is complete |
| --- |

#### Acceptance criteria

| Scenario | A Provider updates the usage policies of a new version of a Resource description via UI |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of Resource Descriptions |
| --- |

| and|I am creating a new version of a resource description |
| --- |

| when|I reach the step "Usage policies" in the wizard |
| --- |

| then|the usage policies are prefilled with the values of the previous version |
| --- |

| and|I can update the usage policies.| |
| --- |

#### Technical details

| SD UI calls SD BE to retrieve Usage policies of Provider |
| Result: UI presents list of Pre-Defined Usage Policies of Provider |
| SD UI calls SD BE to retrieve RD data from the last RD version |
| Result: Usage Policies fields are pre-filled on the wizard screen and available for edit |
| By clicking on the "Previous step" button, the "Access policies" screen is shown with all filled in data |
| By clicking on the "Next step" button, the "Provider information" screen rendereing should be triggered |

#### Mockups:

| Mockup |
| --- |
| !image-2025-09-14-01-09-21-966.png | width=432,height=336! |

#### Additional notes

| Figma: [https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=3226-58928&t=o3dEx1c6tEbK4PpV-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=3226-58928&t=o3dEx1c6tEbK4PpV-0) |
| [an internal page] |
| [an internal page] |
| [an internal page] |
| [an internal page] |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18030](../README.md) (Epic) A Provider updates the usage policies of a new version of a resource description by using policy template via UI |
