> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19290](../../README.md) / [L2-SIMPL-19317](../README.md) / **Epic-SIMPL-19332**

# SIMPL-19332: A Participant deletes a data orchestration workflow that is not referenced by any published dataset

**Epic (deliverable, testable feature)**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19290](../../README.md) (L1) › [SIMPL-19317](../README.md) (L2) › **SIMPL-19332** (Epic)

## Text

**As a** Participant\
**I want to** delete an existing data orchestration workflow,\
**So that** I can remove obsolete or unnecessary workflow implementations while ensuring that historical execution information remains available for traceability.

**AC1. Provider retrieves deletion status**

1. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **When** I request the deletion eligibility status of a data orchestration workflow\
   **And** the system returns the list of data resource descriptions that reference the workflow\
   **Then** if the list is empty, the workflow is eligible for deletion

**AC2. Provider deletes a data orchestration workflow**

2. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** the data orchestration workflow I want to delete has no active dependencies\
   **When** I delete the workflow using the workflow management process\
   **Then** the workflow is deleted

**AC3. Audit Logs Retention after Deletion**

3. **Given** a deleted data orchestration workflow,\
   **When** the deletion is completed,\
   **Then** all past execution logs related to that workflow are preserved

**AC4. Definition Retention after Deletion**

4. **Given** a deleted data orchestration workflow,\
   **When** the deletion is completed,\
   **Then** previous versions remain in the version history

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19317](../README.md) (L2) Participant deletes a data orchestration workflow |
| **Children** | None. |
