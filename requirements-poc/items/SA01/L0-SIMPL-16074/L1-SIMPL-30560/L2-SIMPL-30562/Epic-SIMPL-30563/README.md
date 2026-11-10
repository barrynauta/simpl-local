> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-30560](../../README.md) / [L2-SIMPL-30562](../README.md) / **Epic-SIMPL-30563**

# SIMPL-30563: Simpl-Open executes a data orchestration workflow using the configured distribution model and execution cluster

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-30560](../../README.md) (L1) › [SIMPL-30562](../README.md) (L2) › **SIMPL-30563** (Epic)

## Text

**As a** Participant

**I want to** execute my workflows using the configured distribution model and execution cluster\
**so that** execution remains regular, predictable, and aligned with the selected processing strategy.

**Acceptance Criteria**

**Given** a workflow configured with a distribution model and execution cluster\
**When** the workflow starts\
**Then** Simpl-Open automatically loads the workflow’s saved distribution model and execution cluster

**And** validates the connectivity and authorisation for that cluster\
**And** dispatches execution only if validation succeeds.

**And** a workflow run is created using the resolved distribution model and the validated execution cluster from the saved workflow configuration.

**And** workflow run metadata is logged, including the distribution model and execution cluster used for dispatch.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30562](../README.md) (L2) Participant executes workflow on saved distribution configuration with pre-dispatch validation |
| **Children** | None. |
