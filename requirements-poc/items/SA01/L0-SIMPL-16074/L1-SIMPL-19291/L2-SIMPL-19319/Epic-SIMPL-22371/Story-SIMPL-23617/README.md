> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-22371](../README.md) / **Story-SIMPL-23617**

# SIMPL-23617: API for Workflow Execution and Execution Status

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-22371](../README.md) (Epic) › **SIMPL-23617** (User story)

## Text

**As a** Data Provider,\
**I want** Simpl- Open to trigger the workflow linked to a data resource description when a Consumer consumes the data resource\
**So that** data resources are delivered to the Consumer aligned with my requirements

##### Acceptance Criteria:

**Workflow execution**

Scenario: The data consumer consumes a resource that has been associated with a processing workflow during the publication phase. Simpl-Open will trigger the execution of the workflow before the data transfer from the data provider to the data consumer, ensuring that the data consumer receives correctly processed data.

**Positive Case:**

**AC1: testable E2E**

1. **Given** a contract is established for a given resource description of a given provider and a given consumer\
   **And** the resource description is linked to a workflow to be triggered at consumption\
   **When** the data transfer for this resource description and consumer is triggered\
   **Then** the linked workflow is triggered\
   **And** the API responds with status code 202\
   **And** the provider agent has access to the workflow execution run id and execution status

**Negative Case:**

**AC2: Only intended for API testing**

2. **Given** a valid workflow execution endpoint\
   **When** the workflow execution API is called with an invalid request parameter (e.g. assetId is blank or null)\
   **Then** the API rejects the request\
   **And** the API responds with status code 400\
   **And** the API returns an error message describing the issue

**AC3: Only intended for API testing**

3. **Given** a valid workflow execution endpoint\
   **When** the workflow execution API is called\
   **And** the assetId provided in the request has not active workflow associated with it\
   **Then** the API rejects the request\
   **And** the API responds with status code 404\
   **And** the API returns an error message describing the issue

**AC4:** testable E2E{}

4. **Given** a valid workflow execution endpoint\
   **When** the workflow execution API is called\
   **And** the service is unavailable\
   **Then** the API rejects the request\
   **And** the API responds with status code 502\
   **And** the API returns an error message describing the issue

**Workflow execution status**

Scenario: The data consumer consumes a resource that has been associated with a processing workflow during the publication phase. Simpl-Open checks the execution status of the triggered workflow and transfers the data from the data provider to the data consumer only if the processing has completed successfully.

**Positive Case:**

**AC5: Only intended for API testing**

5. **Given** a valid workflow execution status endpoint\
   **And** the workflow is linked to a resource description\
   **And** the workflow execution has started successfully\
   **When** the workflow execution status API is called\
   **Then** the API responds with status code 200\
   **And** the API returns the execution progress status

**Negative Case:**

AC6: Only intended for API testing{}

6. **Given** a valid workflow execution status endpoint\
   **When** the workflow execution status API is called with an invalid request parameter (e.g. runId is blank)\
   **Then** the API rejects the request\
   **And** the API responds with status code 400\
   **And** the API returns an error message describing the issue

**AC7: Only intended for API testing**

7. **Given** a valid workflow execution status endpoint\
   **When** the workflow execution status API is called with an invalid workflow execution run id\
   **Then** the API rejects the request\
   **And** the API responds with status code 404\
   **And** the API returns an error message describing the issue

**AC8: testable E2E**

8. **Given** a valid workflow execution status endpoint\
   **When** the workflow execution status API is called\
   **And** The service is unavailable\
   **Then** the API rejects the request\
   **And** the API responds with status code 502\
   **And** the API returns an error message describing the issue

##### Additional Notes:

The endpoints to be validated for this US, with reference to the related API Definition, are:

- POST /runs and GET /runs/{runId}

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-22371](../README.md) (Epic) A Participant associates an event-triggered data orchestration workflow to a resource description |
