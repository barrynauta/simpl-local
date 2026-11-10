> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19289](../../README.md) / [L2-SIMPL-19315](../README.md) / **Epic-SIMPL-19330**

# SIMPL-19330: A Participant updates the data orchestration workflow

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19289](../../README.md) (L1) › [SIMPL-19315](../README.md) (L2) › **SIMPL-19330** (Epic)

## Text

|   |   |
| --- | --- |
| **As a** | Participant |
|**I want**|to create a new version of an existing data orchestration workflow, including: *

   - Editing the services, using open-source libraries suggested by Simpl-Open or other open-source libraries, and control flow;
   - Editing the configurable parameters for the workflow
   - For scheduled triggers and triggers on data change, editing the execution configuration of the workflow, including execution parameters (for example, actual input datasets, output destinations), and the scheduling configuration (e.g., time, date, recurrence rules).|\
   |**so that**|I can perform the operation required by my business logic\
   |

**Acceptance Criteria**

**

**AC1. Provider updates an existing workflow by developing a new version**

|   |   |
| --- | --- |
| **Given** | I am an authenticated user |
| **and** | I am a user who wants to update an existing workflow |
| **When** | I start the process to create a new version of an existing data orchestration workflow |
| **Then** | I am able to update the workflow by modifying its logic and steps through the capabilities of the integrated workflow tooling from my development environment |
| **and** | I can implement the changes required to produce a new workflow version in code |

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19315](../README.md) (L2) Participant creates a new version of a data orchestration workflow |
| **Children** | None. |
