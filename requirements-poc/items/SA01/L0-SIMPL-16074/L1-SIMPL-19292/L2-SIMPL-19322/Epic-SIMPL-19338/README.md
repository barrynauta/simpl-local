> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19292](../../README.md) / [L2-SIMPL-19322](../README.md) / **Epic-SIMPL-19338**

# SIMPL-19338: A Participant views data orchestration workflow logs

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19292](../../README.md) (L1) › [SIMPL-19322](../README.md) (L2) › **SIMPL-19338** (Epic)

## Text

**As a** Participant\
**I want to** access detailed real-time logs of the automation workflow executions,\
**So that** I can diagnose issues in detail and ensure correct execution step by step

**Acceptance Criteria**

AC1. Real-Time Log Visibility{}

1. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a workflow is being executed,\
   **When** I open the execution view in the UI,\
   **Then** Simpl-Open displays log entries as soon as they are emitted by the workflow engine,\
   **And** new log entries appear in the UI without requiring the user to refresh the page,\
   **And** the sequence of log entries reflects the actual order of execution steps.

**

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19322](../README.md) (L2) Participant monitors a data orchestration workflow |
| **Children** | None. |
