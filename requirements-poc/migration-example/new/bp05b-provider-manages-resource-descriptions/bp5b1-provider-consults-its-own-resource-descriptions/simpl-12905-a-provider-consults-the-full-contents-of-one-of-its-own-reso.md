<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.1 - A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) / [5B.1.2 A Provider consults the details of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions) / Epic
</div>

# Epic: A Provider consults the full contents of one of its own Resource descriptions via UI and API

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.1.2 A Provider consults the details of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions), under high-level requirement [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md). In the process it belongs to step [Trigger (resource description management)](../README.md#trigger--resource-description-management).

In the backlog (Jira) it is item SIMPL-12905. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** retrieve the full contents of one of my own Resource descriptions via UI or API\
**So that** I can check it and decide if any action should be taken.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I request to retrieve one particular of my own Resource descriptions via UI or API\
   **Then** the full contents of this Resource description is returned.

## User stories

1. [Display self-description details](#user-story-1-display-self-description-details)
2. [API to provide the full content of a publisher's resource description](#user-story-2-api-to-provide-the-full-content-of-a-publishers-resource-description)
3. [[API] API to provide the full contents of a provider's own resource description to the UI](#user-story-3-api-api-to-provide-the-full-contents-of-a-providers-own-resource-description-to-the-ui)

### User story 1: Display self-description details

Backlog item SIMPL-9838

#### Storycard

**As a** Provider end user\
**I want to** be able to see the details of one of my self-descriptions\
**So that** I can inform myself about the details and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** A provider consults the details of one of its own Resource descriptions in the UI - positive case

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am consulting the overview of Resource descriptions\
   **And** there is a Resource description published by me\
   **When** I open the resource description details page\
   **Then** all details of the selected Resource description are shown.

**Scenario:** A provider consults the details of one of its own Resource descriptions in the UI - negative case

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am consulting the overview of Resource descriptions\
   **And** there is a Resource description published by me\
   **When** I open the Resource description details page\
   **And** the API returns an error\
   **Then** an error message showing the error description is shown.

*Note: as this screen can only be reached if there exists at least one Resource description of the Provider, a negative case for "no Resource description" does not exist.*

**Scenario:** A user is not authenticated as Provider

**When** I am not autenticated as Provider\
   **Then** I cannot access Resource description management in the UI.

**Scenario:** A user does not have role authorising the management of Resource Descriptions

3. **Given** I am autenticated as Provider\
   **When** I do not have a role authorising the management of Resource Descriptions\
   **Then** I cannot access Resource description management in the UI.

#### Technical Info

The details of the selfdestruction should be based on the schema (labels of the fields, which fields are shown and such).

UI Mockup:

(image not shown)

#### Additional Notes

API implementation story: (internal page)

UI for the list of Resource descriptions: (internal page)

### User story 2: API to provide the full content of a publisher's resource description

Backlog item SIMPL-11293

#### Storycard

**As a** provider\
**I want to** receive the details of one of my self-descriptions via an API\
**So that** I can inform myself about the details.

#### Acceptance criteria

1. **Given** there is a resource description published by a provider\
   **When** the provider requests the details of an own resource description via API\
   **Then** all details of the resource description are returned.

2. **Given** the provider is authorised\
   **When** a provider requests a resource description with a syntactically valid ID\
   **And** the ID refers to a not existing Resource description\
   **Then** an message is returned that the resource is not found.

3. **Given** a user is a provider\
   **When** the user requests a resource description not created by themself\
   **Then** an message is returned that the user is not authorised.

### User story 3: [API] API to provide the full contents of a provider's own resource description to the UI

Backlog item SIMPL-15622

#### Storycard

**As a** provider\
**I want to** receive the details of one of my self-descriptions via an API\
**So that** I can inform myself about the details.

#### Acceptance criteria

1. **Given** there is a resource description published by myself\
   **When** I request the details of an own resource description with its ID via API\
   **Then** all details of the resource description are returned.

2. **Given** I authorised as a provider\
   **When** I requests a resource description with a syntactically valid ID\
   **And** the ID refers to a not existing Resource description\
   **Then** an message is returned that the resource is not found.

3. **Given** I am authorised as a provider\
   **When** I request a resource description not created by myself\
   **Then** an message is returned that I am not authorised.

4. **Given** I am authorised as a provider\
   **When** I requests a resource description with an incorrect UUID (e.g. too short or too many characters).\
   **Then** an message is returned that the request is bad.

#### Technical information

The endpoint is to be implemented in the SD Tooling backend.

The API will be accessible externally via the `{}Tier-1 Gateway{`}, making it available to the frontend or other provider applications.

The full contents of a provider's own resource description can be obtained by the SD tooling BE from the Query mapper adapter.

#### Additional notes

- The analogous implementation ticket for the Query Mapper adapter is (internal page)
- The API definition ticket for the SD Tooling backend is (internal page)

---

Back to [5B.1.2 A Provider consults the details of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions) · [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) · [BP05B process page](../README.md)
