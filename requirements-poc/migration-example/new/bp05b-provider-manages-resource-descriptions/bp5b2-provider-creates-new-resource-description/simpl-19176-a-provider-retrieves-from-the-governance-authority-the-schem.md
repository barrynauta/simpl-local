<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.1 A provider retrieves a Resource type](../bp5b2-provider-creates-new-resource-description.md#5b21-a-provider-retrieves-a-resource-type) / Epic
</div>

# Epic: A Provider retrieves from the Governance Authority the schema of a published resource type

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.1 A provider retrieves a Resource type](../bp5b2-provider-creates-new-resource-description.md#5b21-a-provider-retrieves-a-resource-type), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.01 Create a new resource description](../README.md#bp05b01-create-a-new-resource-description).

In the backlog it is item SIMPL-19176, status **Defined**. It has 4 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** retrieve the schema of a published resource type from the Governance Authority\
**So that** I can start the creation of a new resource description.

#### Acceptance criteria

**Scenario:** A Provider retrieves the schema of a published resource type

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I request the schema of a published resource type from the Governance Authority\
   **Then** I receive the schema of the published resource type.

## User stories

1. [API for Schema Managament Webhook Notification and Trigger Schema Update](#user-story-1-api-for-schema-managament-webhook-notification-and-trigger-schema-update) (DevTeam PBI is ready for Implementation check)
2. [[UI] A provider selects the Resource type in the UI](#user-story-2-ui-a-provider-selects-the-resource-type-in-the-ui) (DevTeam PBI is ready for Implementation check)
3. [[API] API for retrieving a specific schema version (schema-sync-adapter)](#user-story-3-api-api-for-retrieving-a-specific-schema-version-schema-sync-adapter) (DevTeam PBI is ready for Implementation check)
4. [[API] API for retrieving a specific schema version (sd-tooling-be)](#user-story-4-api-api-for-retrieving-a-specific-schema-version-sd-tooling-be) (DevTeam PBI is ready for Implementation check)

### User story 1: API for Schema Managament Webhook Notification and Trigger Schema Update

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-20514)

**As a** Simpl-Open Participant\
**I want to** receive a notification via an API (exposed by schema-sync-adapter component) when a new schema version is created or revoked and synchronise my local schema storage\
**So that** I am having the latest and correct schema versions available to my applications and consistency and integrity with the resource descriptions is ensured.

**Acceptance criteria:**

**Subscription registration**

1. **Given** I am a participant (provider or consumer)\
   **And** I have an endpoint for receiving schema notifications\
   **When** the endpoint is submitted via API for registration at the Governance Authority\
   **And** the API validates the registration details\
   **Then** the subscription is registered at the Governance Authority

2. **Given** I am a participant (provider or consumer)\
   **And** I have an endpoint for receiving schema notifications\
   **When** the endpoint registration request is submitted with invalid or unreachable details\
   **Then** the Governance Authority rejects the registration request\
   **And** I receive an error response explaining why the registration failed

**Notification reception and validation**

3. **Given** I am a participant subscribed to schema notifications from the Governance Authority\
   **When** the Governance Authority publishes a new schema version\
   **Then** I receive a schema creation notification including references to the created schema

4. **Given** I am a participant subscribed to schema notifications from the Governance Authority\
   **When** the Governance Authority revokes a schema version\
   **Then** I receive a schema revocation notification including a references to the revoked schema

5. **Given** I am a participant subscribed to schema notifications from the Governance Authority\
   **When** I receive a schema notification with missing or invalid information\
   **Then** the notification is rejected\
   **And** I report a clear error message describing the error

[Back to the list of user stories](#user-stories)

### User story 2: [UI] A provider selects the Resource type in the UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-20613)

#### Storycard

**As a** Provider end user\
**I want to** be able to select a resource type in the UI\
**So that** I can start the creation of a new resource description.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I have opened the resource description management page\
   **When** I select a resource type from the list of published resource types\
   **Then** the resource description creation wizard is opened\
   **And** the steps of the wizard are based on the entities in the schema of the selected resource type.

*The contents of the individual steps of the wizard is covered in other user stories and out of the scope of this story.*

#### Mockups:

| Steps of the wizard |
| --- |
| !image-2025-11-12-11-51-43-402.png | thumbnail! |

#### Additional information

- The dropdown containing the resource types that can be selected is implemented in (internal page)
- If the dropdown does not contain any resource type, meaning that the dataspace does not have any schema, then, obviously, nothing can be selected and no wizard can be opened.
- API to retrieve the schema defined in (internal page) and implemented in (internal page)
- "Entities" in the schema are defined on an internal page and, with respect to the UI, further described on an internal page

[Back to the list of user stories](#user-stories)

### User story 3: [API] API for retrieving a specific schema version (schema-sync-adapter)

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-23442)

#### Storycard

**As a** Participant (through sd-tooling-be or adv-search-be)\
**I want to** retrieve (via sync-schema-adapter) a specific version of a schema\
**So that** I can correctly display the resource description based on the specific schema with which it was created.

#### Technical details

This ticket aims to implement an API on the sync-schema-adapter component that allows the sd-tooling-be and xfsc-advsearch-be components to retrieve a specific version of a schema. The retrieved schema version will then be returned to the UI layer (either sd-tooling UI or catalogue-ui) so that providers and consumers can correctly visualize the different fields of a resource description. When a participant, through sd-tooling or the catalogue, requests to view a resource description, the backend components (sd-tooling-be and catalogue / xfsc-adv-search) will invoke this API exposed by the sync-schema-adapter component. The adapter will request it from the schema-management-service.

Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | Schema version successfully retrieved |
| **400 Bad Request** | Invalid schema identifier or version |
| **404 Not Found** | Requested schema version missing |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | Upstream schema service failure |
| **500 Internal Server Error** | Unexpected adapter internal error |

#### Acceptance criteria

**Retrieval of a specific schema version**

1. **Given** a participant requests to view a resource description through sd-tooling or catalogue UI\
   **When** the backend component (sd-tooling-be or xfsc-advsearch-be) invokes the sync-schema-adapter API specifying the schema identifier and version\
   **Then** the sync-schema-adapter shall return the requested schema version\
   **And** the schema version shall be fetched from the schema-management-service.

**Schema version not available**

2. **Given** a participant requests to view a resource description through sd-tooling or catalogue UI\
   **When** the backend component (sd-tooling-be or xfsc-advsearch-be) invokes the sync-schema-adapter API requesting a schema version that is not available locally\
   **And** the schema version cannot be retrieved from the schema-management-service\
   **Then** the sync-schema-adapter shall return an explicit error indicating that the requested schema version is not available\
   **And** the error response shall allow the calling backend to handle the failure without attempting to render the resource description with an incorrect or incompatible schema.

[Back to the list of user stories](#user-stories)

### User story 4: [API] API for retrieving a specific schema version (sd-tooling-be)

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-24335)

#### Storycard

**As a** Provider Participant (through sd-tooling UI)\
**I want to** retrieve (via sd-tooling-be) a specific version of a schema\
**So that** I can correctly display the resource description based on the specific schema with which it was created.

#### Technical details

This ticket aims to implement an API in sd-tooling-be, exposed to the sd-tooling UI, to support the rendering of a resource description. When the UI requests to visualize a resource description, sd-tooling-be resolves the required schema version as follows: it first checks whether the schema is already available in the NFS storage and returns it if present; otherwise, it invokes the corresponding API of the sync-schema-adapter, which retrieves the schema from the schema-management-service. The retrieved schema is then returned to the UI to correctly render the resource description fields.

Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | Schema version successfully retrieved |
| **400 Bad Request** | Invalid schema identifier or version |
| **404 Not Found** | Requested schema version missing |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | Upstream schema service failure |
| **500 Internal Server Error** | Unexpected adapter internal error |

#### Acceptance criteria

**Retrieval of a specific schema version**

1. **Given** a participant requests to view a resource description through sd-tooling UI\
   **When** the sd-tooling UI invokes the sd-tooling-be API requesting a specific schema identifier and version\
   **Then** sd-tooling-be shall return the requested schema version to the UI\
   **And** the schema shall be retrieved from the NFS storage or, if not available, via the sync-schema-adapter from the schema-management-service.

**Schema version not available**

2. **Given** a participant requests to view a resource description through sd-tooling UI\
   **When** the sd-tooling UI invokes the sd-tooling-be API requesting a schema version that is not available locally\
   **And** the schema version cannot be retrieved via the sync-schema-adapter from the schema-management-service\
   **Then** sd-tooling-be shall return an explicit error indicating that the requested schema version is not available\
   **And** the sd-tooling UI shall display an error indicating that the resource description cannot be rendered.

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.1 A provider retrieves a Resource type](../bp5b2-provider-creates-new-resource-description.md#5b21-a-provider-retrieves-a-resource-type) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
