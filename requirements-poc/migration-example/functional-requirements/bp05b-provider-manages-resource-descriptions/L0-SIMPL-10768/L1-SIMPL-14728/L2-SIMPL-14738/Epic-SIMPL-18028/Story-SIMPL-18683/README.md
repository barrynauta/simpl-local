> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14738](../../README.md) / [Epic-SIMPL-18028](../README.md) / **Story-SIMPL-18683**

# SIMPL-18683: [UI] Provider updates common metadata to a new version of Resource description

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14738](../../README.md) (L2) › [SIMPL-18028](../README.md) (Epic) › **SIMPL-18683** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want|to update the common metadata of a new version of a resource description via UI |
| --- |

| so that|the contents of the resource description is complete. |
| --- |

#### Acceptance criteria

| Scenario | A Provider updates common metadata of a new version of a resource description via UI |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new version of a resource description |
| --- |

| when|I reach the step "Common metadata" in the wizard |
| --- |

| then|the common metadata attributes are prefilled with the values from the previous version |
| --- |

| and|I can update the contents of the attributes. |
| --- |

#### Technical details

| SD UI calls SD BE to retrieve SD schema from GA for dedicated RD type |
| Result: UI parsed SD schema and rendered common metadata fields to the wizard screen |
| SD UI calls SD BE to retrieve RD data from the last RD version |
| Result: common metadata fields are pre-filled on the wizard screen and available for edit |
| By clicking on the "Next step" button, the another screen rendereing should be triggered |

#### Mockups:

| Mockup |
| --- |
| !image-2025-09-13-20-50-55-010.png | thumbnail! |

#### Additional notes

| Figma: [https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-12978&t=o3dEx1c6tEbK4PpV-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-12978&t=o3dEx1c6tEbK4PpV-0) |
| (internal page) |
| (internal page) |
| (internal page) |
| (internal page) |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18028](../README.md) (Epic) A Provider updates common metadata of a new version of a  Resource description via UI |
