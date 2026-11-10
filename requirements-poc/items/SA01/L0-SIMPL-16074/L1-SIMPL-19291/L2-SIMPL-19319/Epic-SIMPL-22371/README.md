> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19291](../../README.md) / [L2-SIMPL-19319](../README.md) / **Epic-SIMPL-22371**

# SIMPL-22371: A Participant associates an event-triggered data orchestration workflow to a resource description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19291](../../README.md) (L1) › [SIMPL-19319](../README.md) (L2) › **SIMPL-22371** (Epic)

## Text

|   |   |
| --- | --- |
| **As a** | Participant |
| **I want** | to associate one of my workflows with a resource description during its creation, |
| **so that** | the workflow automatically starts when the defined resource description is consumed. |

**Acceptance Criteria**

**AC1. Association of workflow with resource description**

|   |   |
| --- | --- |
| **Given** | I am an authenticated user of a participant |
| **and** | I have a role authorising the access to data orchestration workflows and the management of resource descriptions |
| **When** | I submit a resource description for a dataset that must trigger a workflow during consumption |
| **and** | I have assigned one of my previously created and stored workflows to the resource description |
| **and** | I have provided a configuration for the parameters required by the workflow |
| **Then** | Simpl-Open validates the provided configuration for the selected workflow |
| **and** | registers the association between the resource description, the workflow, and its parameter configuration to be triggered on the consumption |

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19319](../README.md) (L2) Participant executes data orchestration workflows automatically based on events |
| **Children** | [SIMPL-20783](Story-SIMPL-20783/README.md) (User story) API for Resource - Workflow Association and Configuration Validation<br>[SIMPL-23617](Story-SIMPL-23617/README.md) (User story) API for Workflow Execution and Execution Status<br>[SIMPL-24647](Story-SIMPL-24647/README.md) (User story) [BE] Integrate SD‑Tooling‑BE With AssetOrchestrator for Workflow Association<br>[SIMPL-24652](Story-SIMPL-24652/README.md) (User story) [UI] Retrieve Available Workflows and Render Workflow‑Enabled Template (Data Orchestration)<br>[SIMPL-26071](Story-SIMPL-26071/README.md) (User story) [UI] Retrieve workflow default from Asset Orchestrator (Data Orchestration)<br>[SIMPL-26909](Story-SIMPL-26909/README.md) (User story) [UI] Expose app sharing method templates with workflow configuration support (App Orchestration) |
