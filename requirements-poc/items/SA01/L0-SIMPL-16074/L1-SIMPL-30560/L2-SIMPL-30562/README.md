> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../README.md) · [What the levels mean](../../../../../levels.md) · [All items](../../../../../all-items.md) · [By business process](../../../../../processes.md)

Folder: [items](../../../../README.md) / [SA01](../../../README.md) / [L0-SIMPL-16074](../../README.md) / [L1-SIMPL-30560](../README.md) / **L2-SIMPL-30562**

# SIMPL-30562: Participant executes workflow on saved distribution configuration with pre-dispatch validation

**L2 requirement (detailed requirement)** · Status **Defined**

**Trace:** [SA01](../../../README.md) › [SIMPL-16074](../../README.md) (L0) › [SIMPL-30560](../README.md) (L1) › **SIMPL-30562** (L2)

## Text

At workflow run start, Simpl-Open shall load the workflow’s saved distribution model and execution cluster, validate connectivity and authorisation for that target, and dispatch execution through the configured orchestration platform only if validation succeeds.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30560](../README.md) (L1) Participant executes workflow on saved distribution configuration |
| **Children** | [SIMPL-30563](Epic-SIMPL-30563/README.md) (Epic) Simpl-Open executes a data orchestration workflow using the configured distribution model and execution cluster<br>[SIMPL-30564](Epic-SIMPL-30564/README.md) (Epic) Simpl-Open enforces authorization before cluster selection or use |
