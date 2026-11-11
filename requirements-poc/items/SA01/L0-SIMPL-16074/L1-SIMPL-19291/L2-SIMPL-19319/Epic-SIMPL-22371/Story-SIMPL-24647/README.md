> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-22371](../README.md) / **Story-SIMPL-24647**

# SIMPL-24647: [BE] Integrate SD‑Tooling‑BE With AssetOrchestrator for Workflow Association

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-22371](../README.md) (Epic) › **SIMPL-24647** (User story)

## Text

#### Storycard

**As a** Provider Participant\
**I want to** validate and associate the workflow associated with the resource asset\
**So that** the asset has a validated workflow reference that can be correctly retrieved and triggered during future consumption.

#### Technical details

During the creation of a new resource description, the participant provider can select a Resource Sharing Method template.\
Some templates are explicitly workflow‑enabled, meaning they allow the provider to pick ((internal page)

- a workflow to associate with the asset
- the workflow configuration required by that workflow

When such a workflow‑enabled template is selected, SD‑Tooling‑UI exposes to the provider the additional UI fields for workflow selection/configuration (this is part of following attached ticket (internal page)) , and SD‑Tooling‑BE must perform the corresponding workflow‑validation and association logic.\
Within this context, SD‑Tooling‑BE, specifically as part of the `/selfDescriptions/enriched` API, must invoke the **AssetOrchestrator API (`{}POST /workflowsDefinitions{`})** to validate and persist the association between:

- the newly created asset,
- the workflow identifier selected by the provider,
- the related configuration.

The request must include:

- **workflow identifier**
- **workflow configuration**
- **resource asset reference** generated during resourceDescription creation

AssetOrchestrator (through /workflows API):

- validates the supplied configuration against the workflow definition,
- persists the association into its domain model if the configuration is valid,
- returns a structured error if the configuration is invalid or incomplete.

SD‑Tooling‑BE must propagate errors to the sd-tooling frontend and must **not** persist any partial workflow metadata when validation fails.\
If validation succeeds, the asset retains a workflow reference that AssetOrchestrator can later retrieve during consumption time to determine which workflow to trigger and with what configuration.

This behavior is **identical** whether the submission originates from the sd-tooling **UI** or invoking directly sd-tooling `{}/selfDescriptions/enriched API{`}.

Regarding AssetOrchestrator /workflows API is possible to check outcome in following ways:

- Invoking directly AssetOrchestrator /workflows API
- Invoking sd-tooling `/selfDescriptions/enriched`
- By UI submitting a new resource description involving a data processing workflow (this check is feasible when following ticket involving UI will be completed -&gt; (internal page)

**ACCEPTANCE CRITERIA**

###### **AC - successful asset/workflow association**

1. **Given** a provider has selected a workflow‑enabled sharing‑method template during the creation of a resource description\
   **And** the provider provides a valid workflow identifier\
   **And** the provider provides a valid and complete workflow configuration\
   **When** the resource description is submitted\
   **Then** the system validates the workflow and its configuration\
   **And** the workflow-asset association is recorded\
   **And** a success response is returned to the caller indicating that the workflow association is active

**AC - invalid workflow configuration**

2. **Given** a provider has selected a workflow‑enabled sharing‑method template during the creation of a resource description\
   **And** the provider provides an invalid or incomplete workflow configuration\
   **When** the resource description is submitted\
   **Then** the system rejects the submission\
   **And** no workflow-asset association is recorded\
   **And** a structured validation error is returned to the caller with details about the failure

**AC - invalid workflow identifier**

3. **Given** a provider has selected a workflow‑enabled sharing‑method template during the creation of a resource description\
   **And** the provider provides a workflow identifier that does not exist or is not valid\
   **When** the resource description is submitted\
   **Then** the system rejects the submission\
   **And** no workflow-asset association is recorded\
   **And** a structured error is returned indicating that the workflow identifier is invalid

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-22371](../README.md) (Epic) A Participant associates an event-triggered data orchestration workflow to a resource description |
