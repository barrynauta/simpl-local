> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-961](../README.md) / **Story-SIMPL-18346**

# SIMPL-18346: [UI] Assign usage policies to a new Resource description

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-961](../README.md) (Epic) › **SIMPL-18346** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want|to assign usage policies to a new resource description |
| --- |

| so that|the contents of the resource description is complete |
| --- |

#### Acceptance criteria

| Scenario | A Provider assigns usage policies to a new resource description via UI |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new resource description |
| --- |

| When|I reach the step "Usage policies" in the wizard |
| --- |

| Then|I can assign usage policies to the new resource description. |
| --- |

#### Technical details

| SD UI calls SD BE to retrieve Usage policies of Provider |
| Result: UI presents list of Pre-Defined Usage Policies of Provider |
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
| **Parent** | [SIMPL-961](../README.md) (Epic) A Provider assigns usage policies to a new resource description by using policy template via UI |
