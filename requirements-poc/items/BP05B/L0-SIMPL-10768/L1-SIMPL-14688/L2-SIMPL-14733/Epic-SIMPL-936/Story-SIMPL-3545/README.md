> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-3545**

# SIMPL-3545: [UI] Access Policy multiple attributes per Permission

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-3545** (User story)

## Text

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** select one or more identity attributes for an access policy,\
**So that** I can specify which attributes a Consumer must have to access my resource.

#### Acceptance criteria

##### Select the required identity attributes

| Given|I am adding an access policy to a resource description |
| --- |

| When|I choose the policy template for identity attributes |
| --- |

| Then|I can select one or more of the available identity attributes |
| --- |

| And|the form explains that the Consumer must have every attribute I select |
| --- |

##### Add the policy to the resource description

| Given|I have selected the identity attributes required for access |
| --- |

| When|I confirm the policy |
| --- |

| Then|the policy is added to the resource description I am preparing |
| --- |

| And|it shows the identity attributes I selected |
| --- |

| And|confirming the policy does not submit the resource description |
| --- |

##### No identity attribute is selected

| Given|I am adding an access policy that requires identity attributes |
| --- |

| And|I have not selected any identity attribute |
| --- |

| When|I confirm the policy |
| --- |

| Then|I am told to select at least one identity attribute |
| --- |

| And|the policy form remains open |
| --- |

| And|the policy is not added to the resource description |
| --- |

##### Available identity attributes cannot be loaded

| Given|I am adding an access policy that requires identity attributes |
| --- |

| When|the available identity attributes cannot be loaded |
| --- |

| Then|I see an error message and an option to retry |
| --- |

| And|I cannot confirm the policy until I can select the required attributes |
| --- |

#### Technical details

The `identity-attributes` template contains an ODRL JSON-LD `policy` and ordered `fields`. The UI copies the policy and fills only the declared field path with the selected attribute codes. The multiple-attribute constraint uses `odrl:isAllOf` and a nonempty `odrl:rightOperand` list. Preserve the context declaration `@container: @list`, including when the Provider selects only one attribute. The separate single-attribute template uses `odrl:hasPart` and a typed string value.

The current template fetches choices through the Provider T1 identity-attributes API, filtered for enabled attributes assignable to roles. That authoring source is separate from the Consumer's runtime `identity_attributes` IAM claim.

The local connector evaluates the permission during contract negotiation. It compares codes case-insensitively without trimming whitespace, requires every selected code and permits extra Consumer codes. Empty or malformed requirements and missing or malformed claims deny the constraint. The evaluator does not apply the browser source filters.

Trusted publication validation checks the completed policy and any final policy IDs and asset targets. Browser checks do not replace that validation. The local implementation does not establish production deployment of the authoring integration.

#### Supporting documentation

- R08 ODRL template technical description (internal page)
- R08 policy assignment and validation requirements (internal page)
- Shared policy-template API definition, SIMPL-31345 (internal page)

##### Policy wizard screenshots

These screenshots replace the missing mock-up. The access-policy dialog shows a single-attribute example. The policy-step overview shows where the added access policy appears.

- Add an access policy, single-attribute example (internal page)
- Policy step with an access policy added (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
