> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19291](../../README.md) / [L2-SIMPL-19318](../README.md) / **Epic-SIMPL-19334**

# SIMPL-19334: A Participant manually starts the execution of a data orchestration workflow

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19291](../../README.md) (L1) › [SIMPL-19318](../README.md) (L2) › **SIMPL-19334** (Epic)

## Text

|   |   |
| --- | --- |
| **As a** | Participant |
| **I want** | to manually trigger the execution of a workflow |
| **so that** | I can run the defined logic and view the results of it |

**Acceptance Criteria**

**AC1. Manual Trigger via UI and API**

|   |   |
| --- | --- |
| **Given** | I am an authenticated user of a participant with a role authorising to access the data orchestration workflows |
| **and** | an existing workflow with a valid configuration, |
| **When** | I manually trigger its execution via UI or API, |
| **Then** | Simpl-Open executes a new run of that workflow |

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19318](../README.md) (L2) Participant manually executes a data orchestration workflow |
| **Children** | None. |
