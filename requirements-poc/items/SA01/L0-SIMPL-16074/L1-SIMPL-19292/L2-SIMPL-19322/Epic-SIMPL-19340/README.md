> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19292](../../README.md) / [L2-SIMPL-19322](../README.md) / **Epic-SIMPL-19340**

# SIMPL-19340: A Participant views historical data orchestration workflow logs

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19292](../../README.md) (L1) › [SIMPL-19322](../README.md) (L2) › **SIMPL-19340** (Epic)

## Text

|   |   |
| --- | --- |
| **As a** | Participant |
| **I want** | to retrieve historical logs of past workflow executions, |
| **so that** | I can audit previous runs, identify trends, and troubleshoot recurring issues. |

**Acceptance Criteria**

**AC1. Historical Log Availability**

|   |   |
| --- | --- |
| **Given** | I am an authenticated user of a participant with a role authorising to access the data orchestration workflows |
| **and** | a workflow has completed in the past, |
| **When** | I request access to its historical logs, |
| **Then** | Simpl-Open provides the full set of stored logs for that specific execution. |

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19322](../README.md) (L2) Participant monitors a data orchestration workflow |
| **Children** | None. |
