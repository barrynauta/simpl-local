> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-22371](../README.md) / **Story-SIMPL-24652**

# SIMPL-24652: [UI] Retrieve Available Workflows and Render Workflow‑Enabled Template (Data Orchestration)

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-22371](../README.md) (Epic) › **SIMPL-24652** (User story)

## Text

#### Storycard

**As a** Provider Participant (through Sd-Tooling Frontend)\
**I want to** visualize/use a dedicated template that enables workflow configuration when creating a resource description\
**So that** I can select and configure a data‑processing workflow associated with the resource asset, whenever the selected sharing method supports workflow‑based processing.

#### Technical details

- The association of a workflow to a data resource is implemented by reusing the existing template mechanism based on the resourceDataAddress. After selecting a resource sharing method, if workflow‑based processing is supported for that method, the system exposes a dedicated processing‑workflow template among the available templates.
- Templates are defined using jsonSchema and jsonSchemaUI, which determine the fields that the frontend must render. The frontend retrieves these schemas via the already available APIs (ResourceAddressManagement APIs) and uses them to build the workflow‑related UI dynamically. This includes both the workflow selector and the YAML configuration field.
- The workflow selector is populated by application frontend logic invoking the `GET /workflows` API exposed by the AssetOrchestrator through the Tier‑1 gateway ((internal page)).
- The workflows returned by this API include those defined by the provider in Orchestration Platform, as well as those implemented within Simpl and activated by provider through Orchestration Platform itself.
- Alongside the workflow selector, the template also includes a workflow‑configuration field, allowing the user to define the workflow parameters in YAML format. This enables complete configuration of the workflow that will be associated with the data resource asset.

Following ticket for processing workflow template creation -&gt; (internal page)

**ACCEPTANCE CRITERIA**

###### **Processing‑workflow template is visible when applicable**

1. **Given** the user is creating a new resource description\
   **And** the selected sharing method supports processing workflows\
   **When** the system retrieves the list of available templates\
   **Then** the list of templates is shown\
   **And** the the user can see a dedicated template for configuring a processing workflow

###### **Workflow‑related fields are displayed**

2. **Given** the user has selected the processing‑workflow template\
   **When** the template loads\
   **Then** field for selecting a workflow is displayed\
   **And** field for providing the workflow configuration is also displayed

###### **Workflow list is shown when available**

3. **Given** the user has selected the processing‑workflow template\
   **When** the system obtains the available workflows\
   **Then** the user sees a list of workflows that can be selected

###### **Error while retrieving workflows**

4. **Given** the user has selected the processing‑workflow template\
   **When** an error occurs while obtaining the workflow list\
   **Then** the user sees an error message indicating that workflows cannot be loaded

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-22371](../README.md) (Epic) A Participant associates an event-triggered data orchestration workflow to a resource description |
