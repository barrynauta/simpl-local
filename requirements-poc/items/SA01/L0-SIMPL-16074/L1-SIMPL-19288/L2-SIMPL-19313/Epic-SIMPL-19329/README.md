> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19288](../../README.md) / [L2-SIMPL-19313](../README.md) / **Epic-SIMPL-19329**

# SIMPL-19329: A Participant gets details of the existing data orchestration workflow

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19288](../../README.md) (L1) › [SIMPL-19313](../README.md) (L2) › **SIMPL-19329** (Epic)

## Text

**As a** Participant\
**I want to** retrieve the details of the existing workflow I have previously created\
**So that** I can understand the existing automation steps.

###### **AC1. Workflow Details Retrieval**

1. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** that the user has retrieved the list of their workflows,\
   **When** I select a workflow from the list via the UI, or request its details via the API,\
   **Then** Simpl-Open returns the full details of that workflow:

   - Workflow identification (name, description, status)
   - Workflow structure (steps and their sequence or dependencies)
   - Configuration defined during workflow creation, including inputs, parameters, triggers and any user-defined options
   - Operational information, such as past executions and their outcomes, when available

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19313](../README.md) (L2) Participant consults the details of a data orchestration workflow |
| **Children** | None. |
