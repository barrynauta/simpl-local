> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-19335](../README.md) / **Story-SIMPL-21844**

# SIMPL-21844: API for Workflows and Default Config Retrieval

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-19335](../README.md) (Epic) › **SIMPL-21844** (User story)

## Text

**As a** Provider

**I want to** retrieve the default configuration for a workflow and access the list of available workflows from the orchestration platform

**So that** I can quickly configure workflows and select the correct workflow for resource processing.

Acceptance Criteria:

**Workflow List Retrieval**

**Given** I am a participant configuring a workflow execution for a resource\
**And** I want to explore available workflows in the orchestration platform\
**When** I submit a request to retrieve the list of workflows via API\
**Then** the API returns the complete list of workflows with their identifiers and descriptions

**Given** I am a participant configuring a workflow execution for a resource\
**And** I want to explore available workflows in my orchestration platform\
**When** The service is unavailable or there arent't available workflow\
**Then** the API rejects the request\
**And** I receive a clear error message describing the issue

**Workflow** **Default Configuration Retrieval**

**Given** I am a participant configuring a workflow execution for a resource\
**And** I select a workflow\
**When** I submit a request to retrieve the default workflow configuration via API\
**Then** the API returns a configuration template with default values

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19335](../README.md) (Epic) A Participant automatically executes an event-triggered data orchestration workflow execution |
