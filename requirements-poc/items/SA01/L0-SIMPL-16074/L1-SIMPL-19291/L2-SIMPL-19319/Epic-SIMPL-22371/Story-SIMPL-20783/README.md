> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-19319](../../README.md) / [Epic-SIMPL-22371](../README.md) / **Story-SIMPL-20783**

# SIMPL-20783: API for Resource - Workflow Association and Configuration Validation

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-19319](../../README.md) (L2) › [SIMPL-22371](../README.md) (Epic) › **SIMPL-20783** (User story)

## Text

**As a** Provider\
**I want to** associate a resource processing workflow with a specific target resource\
**So that** the target resource can be automatically processed when consumed, ensuring that the Consumer receives data in the expected form and quality.

##### Acceptance Criteria:

**Workflow Association**

**Scenario:** The provider wants to associate its resource with a previously defined workflow and its configuration, which will be executed at resource consumption time. The association is performed when the resource description is submitted, if the selected workflow exists and the configuration provided is valid according to its configuration schema.

**Positive Case:**

1. **Given** I am an authenticated user with a resource description management role\
   **And** I am creating a resource description\
   **And** I have identified a previously defined workflow\
   **And** I have provided a configuration valid for the associated configuration schema of the workflow\
   **When** I submit the resource description with the workflow and its configuration\
   **Then** the API validates the configuration\
   **And** the API associates the resource with the selected workflow and the provided configuration\
   **And** the association is stored in the asset orchestrator registry storage\
   **And** the API responds with status code 201

**Negative Case:**

2. **Given** I am an authenticated user with a resource description management role\
   **And** I am creating a resource description\
   **And** I have identified a workflow that does not exist\
   **When** I submit the resource description with the workflow and its configuration\
   **Then** the API rejects the association due to the identified workflow not existing\
   **And** no association is stored\
   **And** the API responds with status code 404

3. **Given** I am an authenticated user with a resource description management role\
   **And** I am creating a resource description\
   **And** I have identified a previously defined workflow\
   **And** I have provided a configuration not valid for the associated configuration schema of the workflow\
   **When** I submit the resource description with the workflow and its configuration\
   **Then** the API validates the configuration\
   **And** the API rejects the association due to an invalid configuration\
   **And** the API responds with status code 400\
   **And** a clear error message describing the validation issues is presented

**Configuration Validation**

**Scenario:** The provider wants to validate the workflow configuration already during the configuration definition step of the resource description, in order to receive faster feedback on configuration errors. The validation is performed against a configuration schema defined during the workflow definition phase.

**Positive Case:**

4. **Given** I am an authenticated user with a resource description management role\
   **And** I am creating a resource description\
   **And** I have identified a previously defined workflow\
   **And** I have provided a configuration valid for the associated configuration schema of the workflow\
   **And** I want to validate the configuration before submitting the resource description\
   **When** I submit the configuration for validation via the API\
   **Then** the API validates the provided configuration against the defined configuration schema\
   **And** the API confirms that the configuration is valid by responding with status code 200

**Negative Case:**

5. **Given** I am an authenticated user with a resource description management role\
   **And** I am creating a resource description\
   **And** I have identified a previously defined workflow\
   **And** I have provided a configuration not valid for the associated configuration schema of the workflow\
   **And** I want to validate the configuration before submitting the resource description\
   **When** I submit the configuration for validation via the API\
   **Then** the API rejects the configuration by responding with status code 200\
   **And** a clear error message describing the validation issues is presented

6. **Given** I am an authenticated user with a resource description management role\
   **And** I am creating a resource description\
   **And** I have identified a previously defined workflow\
   **And** I want to validate the configuration before submitting the resource description\
   **When** I submit the configuration for validation via the API with an invalid request parameter,\
   **Then** the API rejects the configuration by responding with status code 400\
   **And** a clear error message describing the validation issues is presented

##### Additional Notes:

The purpose of this US is to contribute to the implementation of a scenario in which a Data Provider wants to execute a previously defined workflow on their resource at the moment the resource is consumed by a Consumer.

From a functional perspective, the scenario described above can be divided into three phases:

1. Definition phase: a workflow designer from the provider designs the workflow and its configuration (or an existing workflow is reused).

1. Association phase: the provider defines the resource description and selects the workflow to be executed upon consumption, including its configuration. The association between the resource, the workflow, and its configuration takes place at the time the resource description is submitted, after validating the existence of the workflow and the validity of the configuration.

1. Consumption phase: the consumer requests access to the dataset, and the associated workflow is automatically executed.

This US focuses on the *Association phase.*

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-22371](../README.md) (Epic) A Participant associates an event-triggered data orchestration workflow to a resource description |
