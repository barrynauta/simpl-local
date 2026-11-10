> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14748](../../README.md) / [Epic-SIMPL-25371](../README.md) / **Story-SIMPL-31641**

# SIMPL-31641: [UI] The Provider views policy syntax validation errors in the resource description editor

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14748](../../README.md) (L2) › [SIMPL-25371](../README.md) (Epic) › **SIMPL-31641** (User story)

## Text

#### Storycard:

| As|Provider |
| --- |

| I want|to see, in the resource description editor, the detailed syntax-validation errors of my access and usage policies |
| --- |

| so that|I can locate and correct the invalid policies before resubmitting. |
| --- |

#### Acceptance criteria:

| Scenario | Policy syntax errors are shown in the editor |
| --- | --- |

| Given|I am authenticated in the Provider UI with a role authorising the management of resource descriptions |
| --- |

| And|I have submitted a resource description whose access or usage policies contain syntax errors |
| --- |

| When|the policy syntax-validation feedback is returned |
| --- |

| Then|the editor shows the list of policy syntax errors, each pointing to the offending policy and clause with a human-readable message |
| --- |

| Scenario | No policy syntax errors are shown for valid policies |
| --- | --- |

| Given|I am authenticated in the Provider UI with a role authorising the management of resource descriptions |
| --- |

| And|I have submitted a resource description whose access and usage policies are syntactically valid |
| --- |

| When|the policy syntax-validation feedback is returned |
| --- |

| Then|the editor shows no policy syntax errors |
| --- |

| And|I can proceed |
| --- |

##### Negative scenario 1

| Scenario | The feedback cannot be retrieved |
| --- | --- |

| Given|I am authenticated in the Provider UI with a role authorising the management of resource descriptions |
| --- |

| And|the policy syntax-validation feedback cannot be retrieved because the service is unavailable |
| --- |

| When|I submit the resource description |
| --- |

| Then|the editor shows error message with retry button |
| --- |

| And|it does not silently proceed as if the policies were valid |
| --- |

#### Technical details:

- The editor consumes the structured feedback defined in the API definition ticket and produced by the API feedback story.
- Each error is rendered against the offending policy and clause (access or usage), with the rule category and message.
- Access policies are ODRL permissions; usage policies are ODRL duties/obligations/constraints.

API definition ticket: (internal page)

#### Additional notes:

- (internal page)
- (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-25371](../README.md) (Epic) The Governance Authority provides feedback to a Provider about syntax validation errors of a policy |
