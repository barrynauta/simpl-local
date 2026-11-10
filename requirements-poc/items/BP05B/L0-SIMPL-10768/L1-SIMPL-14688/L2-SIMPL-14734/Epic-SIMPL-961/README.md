> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14734](../README.md) / **Epic-SIMPL-961**

# SIMPL-961: A Provider assigns usage policies to a new resource description by using policy template via UI

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14734](../README.md) (L2) › **SIMPL-961** (Epic)

## Text

**As a** Provider\
**I want to** use, via UI, an usage policy template to assign an usage policy  to a new resource description\
**so that** I can ensure that the usage of the underlying resource is aligned with my internal rules and obligations

**Acceptance Criteria**

|   |   |
| --- | --- |
| **Scenario** | **A Provider assigns usage policies** **by using policy template** **to a new resource description via UI** |

| Given|I am authenticated as a Provider |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new resource description |
| --- |

| and|I reach the step "Usage policies" in the wizard |
| --- |

| When|I retrieve the usage policy templates |
| --- |

| And|I select the usage policy template I want to use |
| --- |

| And|I fill in the parameters of the usage policy template |
| --- |

| Then|a usage policy is created based on the selected usage policy template and values I provided for the parameters |
| --- |

| And|the created usage policy is assigned to the resource description I am creating |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14734](../README.md) (L2) A Provider assigns usage policies to a new Resource description |
| **Children** | [SIMPL-424](Story-SIMPL-424/README.md) (User story) UI for definition of usage policies<br>[SIMPL-18346](Story-SIMPL-18346/README.md) (User story) [UI] Assign usage policies to a new Resource description<br>[SIMPL-31452](Story-SIMPL-31452/README.md) (User story) [UI] Provider selects a usage policy template in the resource description wizard<br>[SIMPL-31453](Story-SIMPL-31453/README.md) (User story) [UI] Provider fills in the usage policy template parameters in the resource description wizard<br>[SIMPL-31454](Story-SIMPL-31454/README.md) (User story) [UI] Provider sees inline validation feedback for usage policy template parameters<br>[SIMPL-31455](Story-SIMPL-31455/README.md) (User story) [UI] Provider adds a usage policy to a new Resource Description<br>[SIMPL-31704](Story-SIMPL-31704/README.md) (User story) [UI] Provider removes a usage policy from the new resource description |
