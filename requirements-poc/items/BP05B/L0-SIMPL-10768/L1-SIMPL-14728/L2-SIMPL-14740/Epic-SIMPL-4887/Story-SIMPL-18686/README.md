> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14740](../../README.md) / [Epic-SIMPL-4887](../README.md) / **Story-SIMPL-18686**

# SIMPL-18686: [UI] Asssign access policy to a new version of a resource description

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14740](../../README.md) (L2) › [SIMPL-4887](../README.md) (Epic) › **SIMPL-18686** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want to|to update the access policies of a new version of a resource description via UI |
| --- |

| so that|the contents of the resource description is complete |
| --- |

#### Acceptance criteria

| Scenario | A Provider updates the access policies of a new version of a resource description via UI |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new version of a resource description |
| --- |

| when|I reach the step "Access policies" in the wizard |
| --- |

| then|the access policies are prefilled with the values of the previous version |
| --- |

| and|I can update the access policies. |
| --- |

#### Technical details

| SD UI calls SD BE to retrieve contract template and Access Policies |
| Result: UI present list of Contract Templates and related Access Policies |
| SD UI calls SD BE to retrieve RD data from the last RD version |
| Result: Contract Templates and Access Policies fields are pre-filled on the wizard screen and available for edit |
| By clicking on the "Previous step" button, the "Pricing information" screen is shown with all filled in data |
| By clicking on the "Next step" button, the "Usage policies" screen rendereing should be triggered |

#### Mockups:

| Mockup |
| --- |
| !image-2025-09-14-00-17-39-888.png | width=479,height=352! |

#### Additional notes

| Figma: [https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-13252&t=o3dEx1c6tEbK4PpV-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-13252&t=o3dEx1c6tEbK4PpV-0) |
| [an internal page] |
| [an internal page] |
| [an internal page] |
| [an internal page] |
| [an internal page] |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-4887](../README.md) (Epic) A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI |
