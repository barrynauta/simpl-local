> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-31407**

# SIMPL-31407: [UI] Provider sees inline validation feedback for access policy template parameters

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-31407** (User story)

## Text

#### Story card

| As|an authorised Provider managing Resource Descriptions |
| --- |

| I want|clear validation feedback when I confirm an access policy |
| --- |

| So that|I can correct invalid values before adding the policy |
| --- |

#### Acceptance criteria

##### Valid values pass validation

| Given|I completed the fields of an access-policy template with valid values |
| --- |

| When|I confirm the policy in the modal |
| --- |

| Then|the access-policy backend validates the policy |
| --- |

| And|no validation errors are shown |
| --- |

| And|the valid policy is added to the Resource Description |
| --- |

##### Invalid values block confirmation

| Given|one or more values do not meet the selected template's rules |
| --- |

| When|I confirm the policy in the modal |
| --- |

| Then|the access-policy backend validates the policy |
| --- |

| And|the UI shows a clear message for each invalid value |
| --- |

| And|the modal remains open |
| --- |

| And|the policy is not added to the Resource Description |
| --- |

##### Missing required values block confirmation

| Given|one or more required values are missing |
| --- |

| When|I confirm the policy in the modal |
| --- |

| Then|the UI shows which values are required |
| --- |

| And|the modal remains open |
| --- |

| And|the policy is not added to the Resource Description |
| --- |

##### Provider corrects invalid values

| Given|validation errors are shown |
| --- |

| When|I correct the values and confirm the policy again |
| --- |

| Then|the policy is validated again |
| --- |

| And|resolved errors are cleared |
| --- |

| And|the valid policy is added to the Resource Description |
| --- |

#### Technical details

- The required validation trigger is confirmation of the policy in the modal.
- Earlier field feedback may also be shown, but it does not replace validation on confirmation.
- Validation uses the rules supplied by the selected template; the story works with any predefined access-policy template.
- SIMPL-31406 covers displaying the template fields and keeping entered values.
- This story covers validation messages and blocking confirmation when values are invalid.
- SIMPL-31433 covers adding a valid policy to the wizard and including it in the final Resource Description submission.

#### Additional notes

- Access-template details: SIMPL-31347 (internal page)
- Template fields: SIMPL-31406 (internal page)
- Resource Description submission: SIMPL-31433 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
