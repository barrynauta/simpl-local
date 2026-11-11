> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19291](../../README.md) / [L2-SIMPL-19320](../README.md) / **Epic-SIMPL-19336**

# SIMPL-19336: Simpl-Open automatically executes a time-triggered data orchestration workflow

**Epic (deliverable, testable feature)**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19291](../../README.md) (L1) › [SIMPL-19320](../README.md) (L2) › **SIMPL-19336** (Epic)

## Text

**As a** Participant\
**I want** my workflow to automatically start at predefined time intervals or calendar-based schedules\
**So that** I can ensure regular, predictable execution of the defined logic

**Acceptance Criteria**

**AC1. Scheduled Time-based Trigger Execution**

1. **Given** a workflow configured with a valid time-based,\
   **When** the defined time-based trigger condition is met,\
   **Then** Simpl-Open automatically starts the execution of the associated workflow,

**AC. Scheduled Calendar-based Trigger Execution**

2. **Given** a workflow configured with a valid calendar-based schedule,\
   **When** the defined calendar-based trigger condition is met,\
   **Then** Simpl-Open automatically starts the execution of the associated workflow,

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19320](../README.md) (L2) Participant executes data orchestration workflows automatically based on time |
| **Children** | None. |
