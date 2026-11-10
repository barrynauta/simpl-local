> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19291](../../README.md) / [L2-SIMPL-1620](../README.md) / **Epic-SIMPL-2068**

# SIMPL-2068: Participant captures dataset lineage for processed datasets

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19291](../../README.md) (L1) › [SIMPL-1620](../README.md) (L2) › **SIMPL-2068** (Epic)

## Text

As a Participant (Data Provider or Data Consumer),\
**I want** dataset lineage to be automatically captured when I process datasets using Simpl-Open workflows,\
**so that** I can understand dataset origins, applied transformations, and derivation relationships, ensuring transparency and traceability of data processing activities.

**Acceptance Criteria:**\
**Scenario:** Dataset lineage is captured during workflow execution

- **Given** a data orchestration or processing workflow exists in Simpl-Open,\
   **and** the workflow processes one or more datasets,
- **When** the workflow execution completes,
- **Then** Simpl-Open must automatically capture lineage metadata describing source datasets, processing steps, and resulting datasets\
   **and** the lineage metadata must be linked with the corresponding resource description of dataset.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-1620](../README.md) (L2) Participant tracks data lineage for processed datasets |
| **Children** | [SIMPL-25746](Story-SIMPL-25746/README.md) (User story) Provider configures lineage metadata at job and service level<br>[SIMPL-25747](Story-SIMPL-25747/README.md) (User story) Participant captures lineage within workflows<br>[SIMPL-27497](Story-SIMPL-27497/README.md) (User story) Provider generates PROV-O lineage report for processed datasets |
