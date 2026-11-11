> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19291](../../README.md) / [L2-SIMPL-19319](../README.md) / **Epic-SIMPL-19335**

# SIMPL-19335: A Participant automatically executes an event-triggered data orchestration workflow execution

**Epic (deliverable, testable feature)**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19291](../../README.md) (L1) › [SIMPL-19319](../README.md) (L2) › **SIMPL-19335** (Epic)

## Text

**As a** Participant\
**I want** my workflow to automatically start when the event that has been defined as its trigger occurs\
**So that** I can ensure that the defined logic is executed in reaction to relevant data activities, without manual intervention

**Acceptance Criteria**

**AC1. Automatic Trigger Before Dataset Consumption**

1. **Given** a workflow configured with an event-based trigger linked to the consumption of a target dataset already published in the catalogue,\
   **When** a consumer requests the consumption of this dataset,\
   **Then** Simpl-Open automatically starts the execution of the associated workflow to prepare the required data,\
   **And** the workflow must complete successfully before the dataset is made available for consumption.

**AC2. Automatic Trigger on Source Dataset Change**

2. **Given** a workflow configured with an event-based trigger linked to the modification of a source dataset already published in the catalogue,\
   **When** that source dataset is updated,\
   **Then** Simpl-Open automatically starts the execution of the associated workflow without manual action,\
   **And** the run status must be updated accordingly.

**AC3. Handling Workflow Failure on Event-Based Execution**

3. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a workflow is automatically triggered by an event\
   **When** the workflow execution fails\
   **Then** Simpl-Open applies a retry policy (if configured by the user in the creation phase)\
   **And** notifies the relevant users that the failure occurred.

**AC4. Handling Multiple Event-Based Execution**

4. **Given** I am an authenticated user of a participant with a role authorising to access the data orchestration workflows\
   **And** a workflow is associated to one or more event as triggering events\
   **When** multiple triggers happen at the same time\
   **Then** Simpl-Open automatically creates one workflow execution for each triggering event.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19319](../README.md) (L2) Participant executes data orchestration workflows automatically based on events |
| **Children** | [SIMPL-22926](Story-SIMPL-22926/README.md) (User story) API for Workflow and Default Config Retrieval |
