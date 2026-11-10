> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14733](../README.md) / **Epic-SIMPL-936**

# SIMPL-936: A Provider assigns access policies to a new resource description by using policy template via UI

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14733](../README.md) (L2) › **SIMPL-936** (Epic)

## Text

**As a** Provider\
**I want to** use, via UI, an access policy template to assign an access policy to a new resource description\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned with my internal rules and obligations.

**Acceptance Criteria**

| Scenario|**A Provider assigns access policies by using a policy template to a new resource description via UI** |
| --- |

| Given|I am authenticated as a Provider |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new resource description |
| --- |

| When|I reach the step "Access policies" in the wizard |
| --- |

| And|I retrieve the access policy templates |
| --- |

| And|I select the access policy template I want to use |
| --- |

| And|I fill in the parameters of the access policy template |
| --- |

| Then|an access policy is created based on the selected access policy template and values I provided for the parameters |
| --- |

| And|the created access policy is assigned to the resource description I am creating |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14733](../README.md) (L2) A Provider assigns access policies to a new Resource description |
| **Children** | [SIMPL-493](Story-SIMPL-493/README.md) (User story) UI for defining access policies<br>[SIMPL-2581](Story-SIMPL-2581/README.md) (User story) Define Access Policy for the SD-Schema<br>[SIMPL-3545](Story-SIMPL-3545/README.md) (User story) [UI] Access Policy multiple attributes per Permission<br>[SIMPL-3548](Story-SIMPL-3548/README.md) (User story) [UI] Access Policy possible to define Prohibitions<br>[SIMPL-18345](Story-SIMPL-18345/README.md) (User story) [UI] Assign access policies to a new Resource description<br>[SIMPL-31405](Story-SIMPL-31405/README.md) (User story) [UI] Provider selects an access policy template in the resource description wizard<br>[SIMPL-31406](Story-SIMPL-31406/README.md) (User story) [UI] Provider fills in the access policy template parameters in the resource description wizard<br>[SIMPL-31407](Story-SIMPL-31407/README.md) (User story) [UI] Provider sees inline validation feedback for access policy template parameters<br>[SIMPL-31433](Story-SIMPL-31433/README.md) (User story) [UI] Provider adds an access policy to a new Resource Description<br>[SIMPL-31703](Story-SIMPL-31703/README.md) (User story) [UI] Provider removes an access policy from the new resource description |
