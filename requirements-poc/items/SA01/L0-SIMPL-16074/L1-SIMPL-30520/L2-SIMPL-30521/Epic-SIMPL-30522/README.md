> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-30520](../../README.md) / [L2-SIMPL-30521](../README.md) / **Epic-SIMPL-30522**

# SIMPL-30522: A Participant configures the execution target used for distributed workflow runs

**Epic (deliverable, testable feature)**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-30520](../../README.md) (L1) › [SIMPL-30521](../README.md) (L2) › **SIMPL-30522** (Epic)

## Text

**As a** Participant\
**I want to** configure the execution target for a workflow\
**So that** the workflow can run either against an agent-integrated runtime or an externally hosted cluster without changing the orchestration logic.

**Acceptance Criteria**

1. **Given** I am authenticated as a Participant with permission to configure workflows\
   **When** I create or update a workflow\
   **Then** I can save the distribution model together with the execution target\
   **And** the execution target can represent either an agent-integrated runtime or an external participant-owned cluster\
   **And** the workflow remains executable only when the configured target is available.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30521](../README.md) (L2) Participant configures the distributed execution |
| **Children** | [SIMPL-30451](Story-SIMPL-30451/README.md) (User story) A Participant configures the execution target used for distributed workflow runs |
