> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-30520](../../README.md) / [L2-SIMPL-30531](../README.md) / **Epic-SIMPL-30532**

# SIMPL-30532: A Participant defines the distributed processing clusters of the sub-service distribution model of a data orchestration workflow

**Epic (deliverable, testable feature)**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-30520](../../README.md) (L1) › [SIMPL-30531](../README.md) (L2) › **SIMPL-30532** (Epic)

## Text

**As a** Participant\
**I want to** define the distributed processing cluster of the sub-service distribution model of a data orchestration workflow\
**So that** distributed processing services can use an appropriate compute environment for large-scale execution.

**Acceptance Criteria**

1. **Given** I am authenticated as a Participant, with a role authorising the definition of data orchestration workflows\
   **And** I have defined a data orchestration workflow suitable for sub-service distribution\
   **And** I have chosen the sub-service distribution model\
   **Then** I am able to configure the target distributed processing cluster\
   **And** the selected distributed processing cluster is saved as part of the workflow configuration\
   **And** the workflow configuration can also include the distributed processing connection settings required for distributed processing during execution.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30531](../README.md) (L2) Participant defines used sub-service distribution cluster |
| **Children** | None. |
