> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19292](../../README.md) / [L2-SIMPL-19321](../README.md) / **Epic-SIMPL-19337**

# SIMPL-19337: A Participant captures data orchestration workflow execution logs

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19292](../../README.md) (L1) › [SIMPL-19321](../README.md) (L2) › **SIMPL-19337** (Epic)

## Text

**As a** Participant\
**I want** Simpl-Open to capture and store detailed execution logs of workflows\
**So that** I can have them available both during execution and retrospectively to monitor my executions.

**Acceptance Criteria**

**AC1. Real-Time Log Capture**

1. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a workflow is being executed\
   **When** the execution starts and progresses through its defined steps\
   **Then** Simpl-Open captures detailed execution logs in real time\
   **And** persist them in the designated logging storage

**AC2. Log Completeness**

2. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a workflow is being executed\
   **When** logs are generated\
   **Then** Simpl-Open captures all relevant information, including at minimum:

   - Timestamp of when each event occurred.
   - Type of event recorded during the workflow execution.
   - Identifier of the workflow step or operation involved.
   - Status or outcome of the step at the time of the event.
   - Error details when a failure occurs.
   - Duration of the step when available.
   - Any user-provided information configured for the step during the workflow definition phase.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19321](../README.md) (L2) Participant captures data orchestration workflow execution logs |
| **Children** | None. |
