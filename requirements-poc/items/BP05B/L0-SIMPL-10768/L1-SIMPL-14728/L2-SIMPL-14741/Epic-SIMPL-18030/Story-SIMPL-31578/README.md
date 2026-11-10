> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14741](../../README.md) / [Epic-SIMPL-18030](../README.md) / **Story-SIMPL-31578**

# SIMPL-31578: [UI] Provider removes a usage policy from the new resource description version

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14741](../../README.md) (L2) › [SIMPL-18030](../README.md) (Epic) › **SIMPL-31578** (User story)

## Text

#### Storycard:

| As|a Provider with a role authorising the management of resource descriptions |
| --- |

| I want|to remove a usage policy from the new resource description version at the "Usage policies" step |
| --- |

| so that|the new version only carries the usage policies I want to keep |
| --- |

#### Acceptance criteria:

| Scenario | Provider removes a usage policy from the new version |
| --- | --- |

| Given|I am authenticated in the Provider UI with a role authorising the management of resource descriptions |
| --- |

| And|the "Usage policies" step lists one or more usage policies carried over from the previous version or added from a template |
| --- |

| When|I remove one of the listed usage policies |
| --- |

| Then|the UI removes that usage policy from the set to be saved on the new version |
| --- |

| And|the remaining usage policies stay unchanged and available for editing |
| --- |

##### Negative scenario

| Scenario | No usage policies are present to remove |
| --- | --- |

| Given|I am authenticated in the Provider UI with a role authorising the management of resource descriptions |
| --- |

| And|the "Usage policies" step has no assigned usage policies |
| --- |

| When|I view the list of usage policies |
| --- |

| Then|the remove action is not available |
| --- |

| And|the step shows an empty state for usage policies |
| --- |

#### Technical details:

- Removal operates on the usage policies held in the wizard state; the change is persisted only when the new version is submitted.
- A removed usage policy is excluded from the assembled set sent to the existing resource description API on submission.

API definition ticket: (internal page)

#### Additional notes:

- (internal page)
- (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18030](../README.md) (Epic) A Provider updates the usage policies of a new version of a resource description by using policy template via UI |
