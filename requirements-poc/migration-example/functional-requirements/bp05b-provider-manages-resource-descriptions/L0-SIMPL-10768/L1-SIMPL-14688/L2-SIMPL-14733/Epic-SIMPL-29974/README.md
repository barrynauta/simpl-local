> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14733](../README.md) / **Epic-SIMPL-29974**

# SIMPL-29974: A Provider assigns access policies related to contract consent to a new resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14733](../README.md) (L2) › **SIMPL-29974** (Epic)

## Text

**As a** Provider\
**I want to** assign access policies related to contract consent to a new resource description via UI\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned the contract consent status.

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I create an access policy related to resource consent\
   **And** I indicate that contract consent is required for each new usage contract\
   **Then** Simpl-Open creates an access policy related to contract consent\
   **And** assigns the access policy related to contract consent to the new resource description.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14733](../README.md) (L2) A Provider assigns access policies to a new Resource description |
| **Children** | [SIMPL-34176](Story-SIMPL-34176/README.md) (User story) [UI] Configure Contract Consent Access Policy for Resource Descriptions |
