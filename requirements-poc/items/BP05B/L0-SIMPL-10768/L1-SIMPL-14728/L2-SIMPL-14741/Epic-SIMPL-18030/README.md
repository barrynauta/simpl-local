> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14741](../README.md) / **Epic-SIMPL-18030**

# SIMPL-18030: A Provider updates the usage policies of a new version of a resource description by using policy template via UI

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14741](../README.md) (L2) › **SIMPL-18030** (Epic)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want to|update the usage policies using an usage policy template to a new version of a resource description via UI |
| --- |

| so that|I can ensure that the use of the the new resource description and the underlying resource is aligned with my internal rules and obligations. |
| --- |

#### Acceptance criteria

**Acceptance Criterion 1: Add usage policies to a new version of a resource description**

|   |   |
| --- | --- |
| **Scenario** | **A Provider assigns the usage policies of a new version of a resource description by using usage policy template via UI** |

| Given|I am authenticated as a Provider |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new version of a resource description |
| --- |

| When|I reach the step "Usage policies" in the wizard |
| --- |

| Then|I can assign usage policies by using a usage policy template to the new resource description. |
| --- |

**Acceptance Criterion 2: Remove usage policies from a new version of a resource description**

|   |   |
| --- | --- |
| **Scenario** | **A Provider removes the usage policies from a new version of a resource description which were assigned with usage policy template via UI** |

| Given|I am authenticated as a Provider |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new version of a resource description |
| --- |

| When|I reach the step "Usage policies" in the wizard |
| --- |

| Then|I can remove usage policies which were assigned with a policy template from the new resource description. |
| --- |

**Acceptance Criterion 3: Finish updates of a new version of the resource description**

|   |   |
| --- | --- |
| **Scenario** | **A Provider finishes updating usage policies of a new version of a resource description via UI** |

| Given|I am authenticated as a Provider |
| --- |

| and |I have a role authorising the management of resource descriptions |
| --- |

| and|I am creating a new resource description |
| --- |

| When|I reach the step "Usage policies" in the wizard |
| --- |

| and|I have assigned or removed usage policies from the new resource description |
| --- |

| Then|I can navigate to the next step in the wizard. |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14741](../README.md) (L2) A Provider updates the usage policies of a new version of a Resource description |
| **Children** | [SIMPL-31575](Story-SIMPL-31575/README.md) (User story) [UI] Provider sees the usage policies carried over from the previous resource description version<br>[SIMPL-31576](Story-SIMPL-31576/README.md) (User story) [UI] Provider selects a usage policy template in the new resource description version wizard<br>[SIMPL-31577](Story-SIMPL-31577/README.md) (User story) [UI] Provider fills in the usage policy template parameters in the new resource description version wizard<br>[SIMPL-31578](Story-SIMPL-31578/README.md) (User story) [UI] Provider removes a usage policy from the new resource description version<br>[SIMPL-31579](Story-SIMPL-31579/README.md) (User story) [UI] Provider saves the updated usage policies on the new resource description version |
