> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-30560](../../README.md) / [L2-SIMPL-30562](../README.md) / **Epic-SIMPL-30564**

# SIMPL-30564: Simpl-Open enforces authorization before cluster selection or use

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-30560](../../README.md) (L1) › [SIMPL-30562](../README.md) (L2) › **SIMPL-30564** (Epic)

## Text

**As a** Participant\
**I want to** use only clusters I am authorized to access\
**So that** I can avoid invalid targets and comply with access rules.

**Acceptance Criteria**

1. **Given** a workflow is configured with a target cluster\
   **When** the workflow run starts\
   **Then** Simpl-Open validates that the participant is authorized to use the target cluster\
   **And** only dispatches the workflow if authorization and connectivity checks succeed.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30562](../README.md) (L2) Participant executes workflow on saved distribution configuration with pre-dispatch validation |
| **Children** | None. |
