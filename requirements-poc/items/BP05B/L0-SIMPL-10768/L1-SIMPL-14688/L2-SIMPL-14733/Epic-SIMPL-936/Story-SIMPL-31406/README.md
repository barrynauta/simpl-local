> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-31406**

# SIMPL-31406: [UI] Provider fills in the access policy template parameters in the resource description wizard

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-31406** (User story)

## Text

#### Story card

| As|an authorised Provider managing Resource Descriptions |
| --- |

| I want|the Access policies step to show the fields of the selected template |
| --- |

| So that|I can enter the values needed for an access policy |
| --- |

#### Acceptance criteria

##### Provider fills in an access-policy template

| Given|I am authenticated as a Provider authorised to manage Resource Descriptions |
| --- |

| And|I selected an access-policy template in the Resource Description wizard |
| --- |

| When|the template details are loaded |
| --- |

| Then|the UI displays the fields, labels and guidance described by the template |
| --- |

| And|shows any access-enforcement information from the template as read-only |
| --- |

| And|stores the values I enter in the wizard state |
| --- |

##### Provider changes the selected template

| Given|I selected an access-policy template |
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

| Given|I selected an access-policy template |
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
- The form is built from the selected template details and must work with any predefined access-policy template returned by the API.
- The initial delivery provides two access-policy templates out of the box, but the UI must not hardcode them.
- SIMPL-31407 covers validation when the Provider confirms the policy.
- SIMPL-31433 covers creating the policy instance, attaching it to the wizard state and including it in the final Resource Description submission.

#### Additional notes

- Access-template details: SIMPL-31347 (internal page)
- Validation: SIMPL-31407 (internal page)
- Resource Description submission: SIMPL-31433 (internal page)
- [Policies|an internal page]
- [Resource Description UI|an internal page]

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
