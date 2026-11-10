> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-30655](../README.md) / **Story-SIMPL-30657**

# SIMPL-30657: A Provider removes contract clauses from a resource description via UI, when creating a new resource description

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-30655](../README.md) (Epic) › **SIMPL-30657** (User story)

## Text

**As a** Provider end user\
**I want to** remove previously added contract clauses via UI before the submission of the resource description\
**So that** I can ensure that the resource description includes the updated set of contract clauses

**Acceptance Criteria**

**AC1 - A Provider removes contract clauses from a resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I have reached the step 'Contract Clauses' in the wizard\
   **When** I have created one or more contract clauses\
   **Then** I can remove one or more of the previously defined contract clauses

**AC2 - A Provider finishes removing contract clauses via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I have reached the step 'Contract Clauses' in the wizard\
   **And** I have created one or more contract clauses\
   **When** I choose to remove one or more of the previously defined contract clauses in the UI wizard,\
   **Then** those contract clauses are not part of the set of contract clauses that will be associated with the resource description anymore\
   **And** I can navigate to the next step in the wizard

**References / Supporting Material / Documentation / Validation rules:** Contract Clauses management (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-30655](../README.md) (Epic) A Provider adds contract clauses to a new Resource description via UI [Extension] |
