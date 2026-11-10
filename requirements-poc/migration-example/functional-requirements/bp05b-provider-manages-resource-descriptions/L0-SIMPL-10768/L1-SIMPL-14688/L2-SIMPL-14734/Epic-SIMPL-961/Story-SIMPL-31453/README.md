> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-961](../README.md) / **Story-SIMPL-31453**

# SIMPL-31453: [UI] Provider fills in the usage policy template parameters in the resource description wizard

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-961](../README.md) (Epic) › **SIMPL-31453** (User story)

## Text

#### Story card

| As|an authorised Provider managing Resource Descriptions |
| --- |

| I want|the Usage policies step to show the fields of the selected template |
| --- |

| So that|I can enter the values needed for a usage policy |
| --- |

#### Acceptance criteria

##### Provider fills in a usage-policy template

| Given|I am authenticated as a Provider authorised to manage Resource Descriptions |
| --- |

| And|I selected a usage-policy template in the Resource Description wizard |
| --- |

| When|the template details are loaded |
| --- |

| Then|the UI displays the fields, labels and guidance described by the template |
| --- |

| And|stores the values I enter in the wizard state |
| --- |

##### Provider changes the selected template

| Given|I selected a usage-policy template |
| --- |

| And|I entered one or more values |
| --- |

| When|I choose a different template |
| --- |

| Then|the UI asks me to confirm that the entered values will be discarded |
| --- |

| And|confirming the change clears those values and displays the new template fields |
| --- |

| And|cancelling the change keeps the current template and values |
| --- |

##### Template details cannot be retrieved

| Given|I selected a usage-policy template |
| --- |

| When|the template details cannot be retrieved |
| --- |

| Then|the UI shows an error and offers a retry |
| --- |

| And|no parameter fields are displayed |
| --- |

| And|the policy cannot be added until the details are available |
| --- |

#### Technical details

- This story covers displaying template fields and keeping entered values in the wizard state.
- The form is built from the selected template details and must work with any predefined usage-policy template returned by the API.
- Concrete fields are not listed in this story.
- Usage-policy templates do not use the access-policy "Applies during" scopes.
- SIMPL-31454 covers validation when the Provider confirms the policy.
- SIMPL-31455 covers creating the policy instance, attaching it to the wizard state and including it in the final Resource Description submission.

#### Additional notes

- Usage-template details: SIMPL-31386 (internal page)
- Validation: SIMPL-31454 (internal page)
- Resource Description submission: SIMPL-31455 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-961](../README.md) (Epic) A Provider assigns usage policies to a new resource description by using policy template via UI |
