> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14732](../../README.md) / [Epic-SIMPL-17409](../README.md) / **Story-SIMPL-20969**

# SIMPL-20969: [UI] Add application resource type specific metadata

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14732](../../README.md) (L2) › [SIMPL-17409](../README.md) (Epic) › **SIMPL-20969** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** add application resource type specific metadata to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider adds application resource type specific metadata to a new Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Application metadata" in the wizard\
   **Then** I can add data resource type specific metadata as defined in the schema of the selected resource type to the new resource description.

**Scenario:** A Provider finishes adding application resource type specific metadata to a new Resource description via UI

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Application metadata" in the wizard\
   **And** I have added application resource type specific metadata to the new resource description\
   **Then** I can navigate to the next step in the wizard.

**Provider tries to submit a Resource Description without mandatory application metadata**

3. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I attempt to submit the Self Description\
   **And** the mandatory application resource type specific metadata required by the selected schema is missing or invalid\
   **Then** the Self Description cannot be submitted\
   **And** I am informed that the application metadata must be completed according to the selected resource type schema

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17409](../README.md) (Epic) A Provider adds resource type specific metadata to a new Resource description via UI |
