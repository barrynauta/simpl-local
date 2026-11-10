> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19290](../../README.md) / [L2-SIMPL-19317](../README.md) / **Epic-SIMPL-19333**

# SIMPL-19333: A Participant is prevented from deleting a data orchestration workflow with dependent datasets

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19290](../../README.md) (L1) › [SIMPL-19317](../README.md) (L2) › **SIMPL-19333** (Epic)

## Text

**As a** Participant\
**I want** the system to prevent deletion of workflows that are referenced by active published datasets\
**So that** I avoid breaking dataset dependencies and ensure the integrity of ongoing data processing during consumption.

**Acceptance Criteria**

**AC1.** **Deletion Blocked for Dependent Workflows**

1. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a data orchestration workflow is referenced by at least one published dataset in an active state,\
   **When** I try to delete the workflow definition,\
   **Then** Simpl-Open blocks the deletion of the workflow,\
   **And** informs me that active published datasets reference the workflow.

**AC2. Deletion Allowed for Non-Active Dependencies**

2. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a data orchestration workflow is referenced only by revoked datasets,\
   **When** I delete the workflow definition,\
   **Then** Simpl-Open allows the deletion of the workflow,

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19317](../README.md) (L2) Participant deletes a data orchestration workflow |
| **Children** | [SIMPL-23647](Story-SIMPL-23647/README.md) (User story) Check Whether a Workflow Has Associated Resource Descriptions |
