> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19292](../../README.md) / [L2-SIMPL-19322](../README.md) / **Epic-SIMPL-19339**

# SIMPL-19339: A Participant views the status indicator of a data orchestration workflow

**Epic (deliverable, testable feature)**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19292](../../README.md) (L1) › [SIMPL-19322](../README.md) (L2) › **SIMPL-19339** (Epic)

## Text

**As a** Participant\
**I want to** view the current status of a workflow (e.g., running, completed, failed),\
**So that** I can get an immediate overview of execution progress and final outcome without checking detailed logs

**Acceptance Criteria**

**AC1. Workflow Status Indicators**

1. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflow execution logs\
   **And** one or more workflows are currently executing or completed,\
   **When** I view the data orchestration workflow list or detail page,\
   **Then** Simpl-Open displays the correct execution status (e.g., Running, Completed, Failed),\
   **And** the displayed status is updated dynamically as the workflow progresses.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19322](../README.md) (L2) Participant monitors a data orchestration workflow |
| **Children** | None. |
