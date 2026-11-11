<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.1 - A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) / [5B.1.1 A Provider consults an overview of its Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b11-a-provider-consults-an-overview-of-its-resource-descriptions) / Epic
</div>

# Epic: A Provider consults an overview of its Resource descriptions via UI and API

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.1.1 A Provider consults an overview of its Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b11-a-provider-consults-an-overview-of-its-resource-descriptions), under high-level requirement [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md). In the process it belongs to step [Trigger (resource description management)](../README.md#trigger--resource-description-management).

In the backlog (Jira) it is item SIMPL-12899. It has 4 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** retrieve an overview of my own Resource descriptions via UI or API\
**So that** I can review my Resource descriptions and decide if any action is to be taken.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I request to retrieve all my own Resource descriptions via UI or API\
   **Then** a list containing an overview of my own Resource descriptions is returned.

## User stories

1. [Displaying a list of a provider's own self-descriptions](#user-story-1-displaying-a-list-of-a-providers-own-self-descriptions)
2. [API to provide a list of a provider's own resource descriptions](#user-story-2-api-to-provide-a-list-of-a-providers-own-resource-descriptions)
3. [[API] API to provide a list of a provider's own resource descriptions (SD tooling)](#user-story-3-api-api-to-provide-a-list-of-a-providers-own-resource-descriptions-sd-tooling)
4. [Using identity attributes in the API to provide a list of a provider's own resource descriptions](#user-story-4-using-identity-attributes-in-the-api-to-provide-a-list-of-a-providers-own-resource-descriptions)

### User story 1: Displaying a list of a provider's own self-descriptions

Backlog item SIMPL-2295

#### Storycard

**As a** Provider end user\
**I want to** use a UI to see the self-descriptions I have published\
**So that** I can locate a specific self-description that I want to update.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by a provider\
   **When** the provider end user opens the dialog of a list of self-descriptions\
   **Then** the list of resource descriptions previously published by the provider is displayed.

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are no self-descriptions of a provider\
   **When** the provider end user opens the dialog of a list of self-descriptions\
   **Then** a message saying that there are no self-descriptions is displayed.

#### Technical details

UI Mockup:

(image not shown)

[Back to the list of user stories](#user-stories)

### User story 2: API to provide a list of a provider's own resource descriptions

Backlog item SIMPL-11277

#### Storycard

**As a** Provider\
**I want to** receive via an API a sorted (descendingly by publication date or alphabetically by Resource type) list of the resource descriptions summaries which I have published\
**So that** I can locate a specific resource description that I want to update.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by a provider\
   **When** the provider requests the own resource descriptions via API\
   **Then** a list of own resource descriptions published by the provider is returned.

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are no resource descriptions published by a provider\
   **When** the provider requests the own resource descriptions via API\
   **Then** an empty list of own resource descriptions published by the provider is returned.

3. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of Resource Descriptions\
   **When** the provider requests the own resource descriptions via API\
   **Then** message is returned that the user is not authorised.

4. **Given** I am authenticated as Provider\
   **And** there is an internal error\
   **When** the provider requests the own resource descriptions via API\
   **Then** message is returned that the request cannot be fulfilled.

[Back to the list of user stories](#user-stories)

### User story 3: [API] API to provide a list of a provider's own resource descriptions (SD tooling)

Backlog item SIMPL-15584

#### Storycard

**As a** Provider\
**I want to** provide via an API a sorted (descendingly by publication date or alphabetically by Resource type) list of the resource descriptions summaries which I have published\
**So that** so that I can see a list of my Resource Descriptions and decide on any action to take on them

#### Acceptance criteria

**Positive case - default sorting**

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by me\
   **When** I requests my own resource descriptions via API\
   **Then** the list of my previously published resource descriptions is returned, sorted by publication date in descending order (default).

**Positive case - custom sorting**

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by me\
   **When** I request my own resource descriptions via the API\
   **And** include a valid `orderBy` query parameter\
   **Then** the list of my resource descriptions is returned, sorted according to the specified field.

**No resources published**

3. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are no resource descriptions published by me\
   **When** I request the my own resource descriptions via API\
   **Then** an empty list is returned.

**Not authorised**

4. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of Resource Descriptions\
   **When** I requests my own resource descriptions via API\
   **Then** a message is returned that I am not authorised.

**Not acceptable**

5. **Given** I am authenticated as Provider\
   **And** I request my own resource descriptions via API\
   **When** I specify a media type that is not supported\
   **Then** a 406 Not Acceptable error is returned.

#### Technical Information

The API will be exposed by the SD Tooling BE, for which the API definition is done in [https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-api-be/-/blob/feature/SIMPL-14757/openapi/openapi-v2.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-api-be/-/blob/feature/SIMPL-14757/openapi/openapi-v2.yaml?ref_type=heads) ). The API will be accessible externally via the Tier-1 Gateway, making it usable by third-party systems and the UI.

The data will be retrieved by the SD Tooling BE from the Query Mapper Adapter (at the GA side).

#### Additional notes

SD Tooling BE API definition ticket: (internal page)

UI implementation ticket: (internal page)

[Back to the list of user stories](#user-stories)

### User story 4: Using identity attributes in the API to provide a list of a provider's own resource descriptions

Backlog item SIMPL-18524

#### Storycard

**As** Governance Authority\
**I want to** verify if a request to retrieve the list of own resource descriptions by a provider is authorised, based on the included identity attributes\
**So that** I can ensure that I provide the information only to authorised providers

#### Acceptance criteria

The tier 2 identity attributes authorising the management of Resource descriptions are:

- DATA_PROVIDER_PUBLISHER
- APP_PROVIDER_PUBLISHER
- INFRA_PROVIDER_PUBLISHER

When I, the Governance Authority, receive a request from a Provider to retrieve a list of their own Resource descriptions\
then I, the Governance Authority, check the authentication\
and I, the Governance Authority, check that  the identity attributes authorise the management of Resource descriptions.

#### Additional notes

For more explanations regarding tier 2 identification, authentication and authorisation see (internal page).

[Back to the list of user stories](#user-stories)

---

Back to [5B.1.1 A Provider consults an overview of its Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b11-a-provider-consults-an-overview-of-its-resource-descriptions) · [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) · [BP05B process page](../README.md)
