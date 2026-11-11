> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-19335](../README.md) / **Story-SIMPL-22926**

# SIMPL-22926: API for Workflow and Default Config Retrieval

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-19335](../README.md) (Epic) › **SIMPL-22926** (User story)

## Text

**As a** Participant,\
**I want to** , through API, retrieve the list of available workflows from the orchestration platform and retrieve the default configuration for a specific workflow,\
**So that** I can associate my target resource with a processing workflow and its configuration at the time of submitting the resource description.

##### Acceptance Criteria:

**Workflow List Retrieval**

Scenario: The Provider wants to retrieve the list of workflows available in the orchestration platform, so that they can select one to associate with their target resource at the time of submitting the resource description.

**Positive Case:**

1. **Given** I am an authenticated user with a resource description management role,\
   **And** I am creating a resource description,\
   **When** I request the list of previously defined workflows,\
   **Then** the API returns the complete list of defined workflows,\
   **And** for each returned workflow, the list includes its id and name,\
   **And** the API responds with status code 200

**Negative Case:**

2. **Given** I am an authenticated user with a resource description management role,\
   **And** I am creating a resource description,\
   **When** I request the list of previously defined workflows,\
   **And** The service is unavailable\
   **Then** the API rejects the request\
   **And** the API responds with status code 502\
   **And** I receive a clear error message describing the issue

**Workflow** **Default Configuration Retrieval**

**Scenario:** Once a workflow available in the orchestration platform has been selected, the Provider is provided with the associated default configuration. In this way, the Provider can associate their target resource with a workflow and its configuration.

**Positive Case:**

3. **Given** I am an authenticated user with a resource description management role,\
   **And** I am creating a resource description,\
   **When** I submit the API request with a workflow name that is available in the orchestration platform,\
   **Then** the API retrieves the default configuration associated with that workflow\
   **And** the API responds with status code 200

**Negative Case:**

4. **Given** I am an authenticated user with a resource description management role,\
   **And** I am creating a resource description,\
   **When** I submit the API request with an invalid request parameter\
   **Then** the API rejects the default configuration request,\
   **And** the API responds with status code 400\
   **And** I receive a clear error message describing the issue

5. **Given** I am an authenticated user with a resource description management role,\
   **And** I am creating a resource description,\
   **When** I submit the API request with a workflow name that is not available in the orchestration platform,\
   **Then** the API rejects the default configuration request,\
   **And** the API responds with status code 404\
   **And** I receive a clear error message describing the issue

6. **Given** I am an authenticated user with a resource description management role,\
   **And** I am creating a resource description,\
   **When** I submit the API request with a workflow name that is available in the orchestration platform,\
   **And** The service is unavailable\
   **Then** the API rejects the request\
   **And** the API responds with status code 502\
   **And** I receive a clear error message describing the issue

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19335](../README.md) (Epic) A Participant automatically executes an event-triggered data orchestration workflow execution |
