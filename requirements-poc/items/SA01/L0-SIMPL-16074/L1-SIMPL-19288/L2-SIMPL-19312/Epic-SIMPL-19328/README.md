> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19288](../../README.md) / [L2-SIMPL-19312](../README.md) / **Epic-SIMPL-19328**

# SIMPL-19328: A Participant gets the list of existing data orchestration workflows

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19288](../../README.md) (L1) › [SIMPL-19312](../README.md) (L2) › **SIMPL-19328** (Epic)

## Text

|   |   |
| --- | --- |
| **As a** | Participant |
| **I want** | to retrieve the list of existing workflows that belong to me |
| **so that** | I can review my workflows and see some details about it (name, last 5 runs date and status). |

###### **AC1. Workflow Inventory Retrieval**

|   |   |
| --- | --- |
| **Given** | I am an authenticated user of a participant with a role authorising to access the data orchestration workflows |
| **When** | I request the list of my workflows via UI or API, |
| **Then** | Simpl-Open returns all workflows that belong to that Participant. |

######

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19312](../README.md) (L2) Participant consults an overview of their own data orchestration workflows |
| **Children** | None. |
