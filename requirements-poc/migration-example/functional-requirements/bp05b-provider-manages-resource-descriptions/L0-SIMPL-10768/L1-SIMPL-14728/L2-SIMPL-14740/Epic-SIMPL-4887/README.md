> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14740](../README.md) / **Epic-SIMPL-4887**

# SIMPL-4887: A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14740](../README.md) (L2) › **SIMPL-4887** (Epic)

## Text

#### Storycard

**As a** Provider end user\
**I want to** to use, via UI, an access policy template to assign an access policy to a new version of a resource description\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned with my internal rules and obligations.

#### Acceptance criteria

**Acceptance Criterion 1: Add access policies to a new version of a resource description**

**Scenario:** Provider assigns access policies of a new version of a resource description by using a policy template

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Access policies" in the wizard\
   **Then** I can retrieve the access policy templates\
   **And** I can select the access policy template I want to use\
   **And** I can fill in the parameters of the access policy template

**Acceptance Criterion 2: Remove access policies from a new version of a resource description**

**Scenario:** A Provider removes access policy from a new version of a resource description which were assigned with a policy template via UI

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Access policies" in the wizard\
   **Then** I can remove access policies which were assigned with a policy template from the new resource description.

**Acceptance Criterion 3: Finish updates of a new version of the resource description**

**Scenario:** A Provider finishes updating access policies of a new version of a resource description via UI

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies" in the wizard\
   **And** I have assigned or removed access policies to from the new resource description\
   **Then** the updated access policies are assigned to the resource description I am updating

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14740](../README.md) (L2) A Provider updates the access policies of a new version of a Resource description |
| **Children** | [SIMPL-31543](Story-SIMPL-31543/README.md) (User story) [UI] Provider sees the access policies carried over from the previous resource description version<br>[SIMPL-31544](Story-SIMPL-31544/README.md) (User story) [UI] Provider selects an access policy template in the new resource description version wizard<br>[SIMPL-31545](Story-SIMPL-31545/README.md) (User story) [UI] Provider fills in the access policy template parameters in the new resource description version wizard<br>[SIMPL-31546](Story-SIMPL-31546/README.md) (User story) [UI] Provider removes an access policy from the new resource description version<br>[SIMPL-31547](Story-SIMPL-31547/README.md) (User story) [UI] Provider saves the updated access policies on the new resource description version |
