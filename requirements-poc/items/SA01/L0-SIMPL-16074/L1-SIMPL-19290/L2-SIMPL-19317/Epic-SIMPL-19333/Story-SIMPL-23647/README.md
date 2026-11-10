> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19290](../../../README.md) / [L2-SIMPL-19317](../../README.md) / [Epic-SIMPL-19333](../README.md) / **Story-SIMPL-23647**

# SIMPL-23647: Check Whether a Workflow Has Associated Resource Descriptions

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19290](../../../README.md) (L1) › [SIMPL-19317](../../README.md) (L2) › [SIMPL-19333](../README.md) (Epic) › **SIMPL-23647** (User story)

## Text

**As a** Participant,\
**I want to** retrieve, through an API, the list of workflow definitions linked to a resource,\
**So that** I can understand whether a workflow is still in use before deciding to remove it.

###### Acceptance Criteria

Scenario: **Retrieve workflow definitions linked to a resource**\
The Participant needs visibility on whether a workflow is associated with an existing resource, to avoid deleting workflows that are still used.

**Positive Case - Resource description provided**

1. **Given** I am an authenticated user,\
   **And** I am managing workflows,\
   **When** I request the list of workflows associated with a resource description provided in the request,\
   **Then** the API returns only the workflows linked to that resource description,\
   **And** each workflow includes its identifier and name,\
   **And** the API responds with status code 200.

**Positive Case - No resource description provided**

2. **Given** I am an authenticated user,\
   **And** I am managing workflows,\
   **When** I request the list of workflows without providing any specific resource description,\
   **Then** the API returns all workflows that have at least one linked resource description,\
   **And** each workflow includes its identifier and name,\
   **And** the API responds with status code 200.

**Negative Case - Resource description not found (404)**

3. **Given** I am an authenticated user,\
   **And** I am managing workflows,\
   **When** I request the workflows for a resource description that does not exist in the orchestration platform,\
   **Then** I am informed that the resource description was not found,\
   **And** the API responds with status code 404.

**Negative Case - Unauthorised (401)**

4. **Given** I am not authenticated,\
   **And** I am managing workflows,\
   **When** I request the workflows for a resource description,\
   **Then** I am informed that authentication is required,\
   **And** the API responds with status code 401.

**Negative Case -** **Forbidden (403)**

5. **Given** I am an authenticated user,\
   **And** I do not have permission to manage workflows,\
   **When** I request the workflows for a resource description,\
   **Then** I am informed that I am not authorised to access the requested resource,\
   **And** the API responds with status code 403.

**Negative Case -** **Bad request (400)**

6. **Given** I am an authenticated user,\
   **And** I am managing workflows,\
   **When** I request the workflows for a resource description using an invalid or malformed identifier,\
   **Then** I am informed that the request parameters are not valid,\
   **And** the API responds with status code 400.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-19333](../README.md) (Epic) A Participant is prevented from deleting a data orchestration workflow with dependent datasets |
