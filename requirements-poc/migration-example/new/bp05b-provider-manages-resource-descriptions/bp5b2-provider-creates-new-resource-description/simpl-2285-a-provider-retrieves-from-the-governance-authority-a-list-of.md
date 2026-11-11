<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.1 A provider retrieves a Resource type](../bp5b2-provider-creates-new-resource-description.md#5b21-a-provider-retrieves-a-resource-type) / Epic
</div>

# Epic: A Provider retrieves from the Governance Authority a list of published resource types

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.1 A provider retrieves a Resource type](../bp5b2-provider-creates-new-resource-description.md#5b21-a-provider-retrieves-a-resource-type), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.01 Create a new resource description](../README.md#bp05b01-create-a-new-resource-description).

In the backlog it is item SIMPL-2285, status **Defined**. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** retrieve a list of published resource types from the Governance Authority\
**So that** I can start the creation of a new resource description.

#### Acceptance criteria

**Scenario:** A Provider retrieves a list of published resource types

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I request a list of published resource types from the Governance Authority\
   **Then** I receive a list containing the published resource types.

## User stories

1. [[API] API for the Self-Description Tool](#user-story-1-api-api-for-the-self-description-tool) (Ready for DG Acceptance)
2. [[UI] A provider consults a list of published resource types in the resource description manager UI](#user-story-2-ui-a-provider-consults-a-list-of-published-resource-types-in-the-resource-description-manager-ui) (DevTeam PBI is ready for Implementation check)
3. [[API] A provider resource description manager retrieves a list of active schemas via API for the UI](#user-story-3-api-a-provider-resource-description-manager-retrieves-a-list-of-active-schemas-via-api-for-the-ui) (Ready for DG Acceptance)

### User story 1: [API] API for the Self-Description Tool

**Status:** Ready for DG Acceptance (backlog item SIMPL-330)

'+**As a**+ service provider **I want** to use the API Backend of the Self-Description Tool **so that** I can create a Self-Description

**Linked Sovereign X US ID:** TAE_US_197

+**Technical specification:**+

Integrate and test the SD Creation Wizard API to create a new self-description from the provided SHACL Schema Description.

+**Acceptance Criteria:**+

1. **A service provider can query the available shapes**
1. **The frontend should be able to get the JSON form of a shape**

### User story 2: [UI] A provider consults a list of published resource types in the resource description manager UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-15867)

#### Storycard

**As a** Provider end user\
**I want to** be able to consult the published resource type in the UI\
**So that** I can select one to start the creation of a new resource description for a specific resource type.

#### Acceptance criteria

**Scenario:** Consulting the list of published resource types - positive case

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I open the resource description management page\
   **Then** the list of active resource types available from the Governance Authority is shown.

**Scenario:** Consulting the list of published resource types - API error

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I open the resource description management page\
   **And** the API to retrieve the list of resource types throws an error\
   **Then** an empty list of resource types for creating a new resource description is shown\
   **And** the creation of a new resource description is not possible\
   **And** a notification is shown that the resource types could not be retrieved and resource description is not possible.

**Scenario:** Consulting the list of published resource types - not authenticated

3. **Given** I am not authenticated as Provider\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

**Scenario:** Consulting the list of published resource types - no role

4. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of Resource Descriptions\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

#### Mockups:

| Mockup 1 | Mockup 2 |
| --- | --- |
| !RDM_with_dropdown_closed.png | thumbnail! | !RDM_with_dropdown-open.png | thumbnail! |

#### Technical info:

API call is to SD tooling BE.

API definition ticket (internal page).

### User story 3: [API] A provider resource description manager retrieves a list of active schemas via API for the UI

**Status:** Ready for DG Acceptance (backlog item SIMPL-16118)

#### Storycard

**As a** Provider\
**I want to** be able to retrieve a list of resource types (list of active schemas) via the SD Tooling API from the Schema Storage\
**So that** I can select a schema for the creation of a new Resource Description.

#### Acceptance criteria

**Scenario:** A Provider requests a list of resource types - positive case

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request a list of active resource types from the API\
   **Then** a list resource types is returned

**Scenario:** A Provider requests a list of resource types - wrong role

2. **Given** I am authenticated as a Provider\
   **And** I do not have a role authorising the management of resource descriptions\
   **When** I request a list of active resource types from the API\
   **Then** a message is returned that I am not authorised.

**Scenario:** A Provider requests a list of resource types - internal server error

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request a list of active resource types from the API\
   **And** there is an internal error\
   **Then** a message is returned that an internal error occured, including a suitable error code.

#### Technical details

Extract the metadata for a resource description from the .ttl and return it to the FE from the API.

#### Additional notes

- API definition ticket: (internal page)

---

Back to [5B.2.1 A provider retrieves a Resource type](../bp5b2-provider-creates-new-resource-description.md#5b21-a-provider-retrieves-a-resource-type) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
