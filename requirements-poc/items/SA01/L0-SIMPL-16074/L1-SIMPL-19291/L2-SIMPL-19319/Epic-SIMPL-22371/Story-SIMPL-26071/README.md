> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-22371](../README.md) / **Story-SIMPL-26071**

# SIMPL-26071: [UI] Retrieve workflow default from Asset Orchestrator (Data Orchestration)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-22371](../README.md) (Epic) › **SIMPL-26071** (User story)

## Text

#### Storycard

| As a|Participant Provider |
| --- |

| I want|to view the default configuration associated with a specific processing workflow |
| --- |

| so that|I can understand the predefined workflow settings and optionally adjust specific configuration values before associate it with resource description |
| --- |

#### Technical details

This ticket introduces a new capability in SD‑Tooling, allowing the application to retrieve and display the **default configuration** associated with a Processing Workflow selected by the Participant Provider.

When the user selects a workflowID (jobName), using codeLocation, repositoryname and jobname properties SD‑Tooling must:

1. Trigger a request from the SD‑Tooling frontend to retrieve the base/default configuration for the selected workflow.

1. Obtain the configuration by invoking the AssetOrchestrator API:

`GET /workflows/{repositoryName}/{codeLocation}/{jobName}/defaultConfig`

1. Receive the default configuration returned by the API.

1. Display this configuration in the SD‑Tooling UI in a format (YAML) that allows the Participant Provider to:

*

   - view all predefined workflow parameters
   - optionally modify specific values

This ensures that every Processing Workflow template is initialized with a valid and consistent baseline configuration, reducing errors and improving clarity for the user.

**Acceptance Criteria**

###### **AC1 Default configuration is displayed**

**Given** the Participant Provider selects a template of type *ProcessingWorkflow*

**When** default configuration associated with that template is retrieved

**Then** the configuration is shown to the Participant Provider in a clear and readable form.

###### **AC2 Configuration not available**

**Given** the Participant Provider selects a template of type *ProcessingWorkflow*

**When** default configuration associated with that template cannot be retrieved

**Then** the Participant Provider is informed that the configuration cannot be displayed.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-22371](../README.md) (Epic) A Participant associates an event-triggered data orchestration workflow to a resource description |
