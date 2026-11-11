<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider adds resource type specific metadata to a new Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.02 Add resource type specific metadata](../README.md#bp05b02-add-resource-type-specific-metadata).

In the backlog (Jira) it is item SIMPL-17409. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** add resource type specific metadata to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider adds resource type specific metadata to a new Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** I can add resource type specific metadata as defined in the schema of the selected resource type to the new resource description.

**Scenario:** A Provider finishes adding resource type specific metadata to a new Resource description via UI

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **And** I have added resource type specific metadata to the new resource description\
   **Then** I can navigate to the next step in the wizard.

## User stories

1. [[UI] Add data resource type specific metadata](#user-story-1-ui-add-data-resource-type-specific-metadata)
2. [[UI] Add application resource type specific metadata](#user-story-2-ui-add-application-resource-type-specific-metadata)
3. [[UI] Add infrastructure resource type specific metadata](#user-story-3-ui-add-infrastructure-resource-type-specific-metadata)

### User story 1: [UI] Add data resource type specific metadata

Backlog item SIMPL-18344

#### Storycard

**As a** Provider end user\
**I want to** add data resource type specific metadata to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider adds data resource type specific metadata to a new Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Data metadata" in the wizard\
   **Then** I can add data resource type specific metadata as defined in the schema of the selected resource type to the new resource description.

**Scenario:** A Provider finishes adding data resource type specific metadata to a new Resource description via UI

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Data metadata" in the wizard\
   **And** I have added data resource type specific metadata to the new resource description\
   **Then** I can navigate to the next step in the wizard.

**Invalid data metadata prevents submission of the Resource Description**

3. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I enter a value in a data metadata field that does not comply with the field validation rules\
   **Then** the invalid field is visually highlighted\
   **And** an error message is displayed next to the field indicating that the value is not valid\
   **And** the Sign & Submit action is disabled\
   **And** I cannot proceed with the publication of the resource description until all invalid values are corrected

### User story 2: [UI] Add application resource type specific metadata

Backlog item SIMPL-20969

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

### User story 3: [UI] Add infrastructure resource type specific metadata

Backlog item SIMPL-21590

#### Storycard

**As a** Provider end user\
**I want to** add infrastructure resource type specific metadata to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

*Note: Out of scope is adding the deployment script. This story only covers general infrastructure metadata.*

**Scenario:** A Provider adds infrastructure resource type specific metadata to a new Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description[link title](http://example.com/)\
   **When** I reach the step "Infrastructure metadata" in the wizard\
   **Then** I can add infrastructure resource type specific metadata as defined in the schema of the selected resource type to the new resource description.

**Scenario:** A Provider finishes adding infrastructure resource type specific metadata to a new Resource description via UI

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Infrastructure metadata" in the wizard\
   **And** I have added infrastructure resource type specific metadata to the new resource description\
   **Then** I can navigate to the next step in the wizard.

**Technical details:**

Test for the resource type infrastructure, that during the creation of the resource description, the resource type specific properties is part of the wizard, meaning every property that is exclusively part of the Resource Description Schema.

- `{}Infrastructure: I{`}nfrastructurePropertiesShape - See also [Infrastructure Schema](https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-sd-schemas/-/blob/develop/shape/service-offering/infrastructure-offeringShape.ttl?ref_type=heads)

---

Back to [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
