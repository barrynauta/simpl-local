> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-22371](../README.md) / **Story-SIMPL-26909**

# SIMPL-26909: [UI] Expose app sharing method templates with workflow configuration support (App Orchestration)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-22371](../README.md) (Epic) › **SIMPL-26909** (User story)

## Text

#### Storycard

| As a|Provider Participant |
| --- |

| I want|to visualize and use the resource address templates associated with the selected app sharing method when creating a resource description for app case |
| --- |

| so that|I can configure the required parameters for the selected sharing method and resource address template |
| --- |

#### Technical details

SD‑Tooling allows provider users to visualize and select the templates associated with the app sharing methods configured for application consumption.\
Templates are exposed according to the selected sharing method and define the structure of the data to be provided during the creation of a resource description.

The association logic is implemented by reusing the existing template mechanism based on `{}resourceDataAddress{`}.\
After selecting a sharing method, the system retrieves and exposes the resource address templates applicable to that method.\
If the selected sharing method includes resource addresses that support workflow configuration, the corresponding workflow template is included among the available templates.\
Templates are defined using `jsonSchema` and `{}jsonSchemaUI{`}, which determine the fields to be rendered by the frontend.\
The frontend retrieves these schemas through the already available ResourceAddressManagement APIs and builds the UI dynamically based on the selected template.

When a resource address template based on the use of a workflow is selected, the frontend:

- Displays a workflow selector (codeLocation, repository, workflowId) populated by invoking the `GET /workflows` API exposed by the Asset Orchestrator through the Tier‑1 Gateway ((internal page))
- Displays a workflow‑configuration field that allows the user to define workflow parameters in YAML format
- Displays a default configuration within configuratoin field once a specific workflow has been selected.

The list of workflows includes both workflows defined by the provider within the Orchestration Platform and workflows implemented within SIMPL and activated by the provider through the Orchestration Platform.

**ACCEPTANCE CRITERIA**

**Templates are shown based on the selected sharing method**

**Given** the user is creating a new resource description\
**When** the user selects an app sharing method\
**Then** the system retrieves the resource address templates associated with the selected sharing method\
**And** the user sees the list of available templates\
**And** the user can select one of these templates

**Processing‑workflow template is shown when supported**

**Given** the user is creating a new resource description\
**And** the user has selected a resource address template\
**When** the selected template includes workflow configuration\
**Then** the user sees the list of available workflows\
**And** the user can select one of these workflows

**Default workflow configuration cannot be retrieved**

**Given** the user has selected a resource address template that includes workflow configuration\
**When** the default workflow configuration is not available or cannot be loaded\
**Then** the user is informed that the default workflow configuration cannot be retrieved

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-22371](../README.md) (Epic) A Participant associates an event-triggered data orchestration workflow to a resource description |
