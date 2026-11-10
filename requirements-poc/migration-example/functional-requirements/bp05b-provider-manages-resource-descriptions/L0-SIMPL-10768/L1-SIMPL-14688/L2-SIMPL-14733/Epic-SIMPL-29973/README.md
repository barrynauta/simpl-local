> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14733](../README.md) / **Epic-SIMPL-29973**

# SIMPL-29973: A Provider assigns access policies related to resource consent to a new resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14733](../README.md) (L2) › **SIMPL-29973** (Epic)

## Text

**As a** Provider\
**I want to** assign access policies related to resource consent to a new resource description via UI\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned the resource consent status.

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I retrieve the list of resource consent assertions\
   **And** I select one of the resource consent assertions to use in the access policy\
   **Then** Simpl-Open creates an access policy related to resource consent\
   **And** links the selected resource consent assertion to the access policy\
   **And** assigns the access policy related to resource consent to the new resource description.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14733](../README.md) (L2) A Provider assigns access policies to a new Resource description |
| **Children** | [SIMPL-34175](Story-SIMPL-34175/README.md) (User story) [UI] Configure Resource Consent Access Policy for Resource Descriptions |
