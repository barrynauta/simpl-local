<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.1 - A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) / [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions) / Epic
</div>

# Epic: A Provider consults the version history of one of its own resource descriptions

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions), under high-level requirement [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md). In the process it belongs to step [Trigger (resource description management)](../README.md#trigger--resource-description-management).

In the backlog (Jira) it is item SIMPL-13658. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via UI or API\
**So that** I can review it and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions via UI or API\
   **Then** the version history of this resource description is returned.

## User stories

1. [[UI] A provider consults the version history of a resource description](#user-story-1-ui-a-provider-consults-the-version-history-of-a-resource-description)
2. [[API] A provider consults the version history of a resource description via API (SD tooling)](#user-story-2-api-a-provider-consults-the-version-history-of-a-resource-description-via-api-sd-tooling)
3. [[API] The Governance authority provides the version history of a resource description via API (Query mapper)](#user-story-3-api-the-governance-authority-provides-the-version-history-of-a-resource-description-via-api-query-mapper)

### User story 1: [UI] A provider consults the version history of a resource description

Backlog item SIMPL-21646

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via UI\
**So that** I can review it and decide if any action should be taken.

#### Technical Details

From a technical perspective, the **sd‑tooling‑ui** component will invoke an API exposed by the **sd‑tooling‑be** component (defined through ticket **SIMPL‑21645** and implemented through ticket \SIMPL-21647).\
From a UI perspective, the following implementation is envisaged:

- The user accesses a summary page showing the resource descriptions created by the provider user. This summary displays only the latest version of each resource description.
- For each item in the list of resource descriptions, a button will be available that allows the user to navigate to a new view containing the list of resource descriptions representing previous versions of the current one. By clicking on one of these entries, the user will be able to view its details.

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions via UI

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to view the version history of one of my own resource descriptions\
   **Then** the version history of this resource description is displayed.

**Scenario:** Error while retrieving the version history

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to view the version history of one of my own resource descriptions\
   **And** an error occurs while retrieving the version history\
   **Then** the version history is not displayed\
   **And** I am informed that the version history cannot be retrieved

### User story 2: [API] A provider consults the version history of a resource description via API (SD tooling)

Backlog item SIMPL-21647

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via API\
**So that** I can review it and decide if any action should be taken.

#### Technical Details

From a technical perspective, the **sd-tooling-be** component will invoke the following API exposed by the query-mapper-adapter:\
`{}/participants/resourceDescriptions/{resourceDescriptionId}/version{`},\
which is defined in the following OpenAPI specification file:\
[https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/poc-gaia-edc/-/blob/main/openapi/adapter_openapi.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/poc-gaia-edc/-/blob/main/openapi/adapter_openapi.yaml?ref_type=heads)

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - positive case

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions\
   **Then** the version history of this resource description is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: not own resource description

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of a resource description not published by me\
   **Then** an error message is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: resource description not existing

3. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of a resource description that does not exist\
   **Then** an error message is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: bad request

4. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions\
   **And** I send a malformed request\
   **Then** an error message is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: internal server error

5. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions\
   **And** there is an internal server error\
   **Then** an error message is returned.

### User story 3: [API] The Governance authority provides the version history of a resource description via API (Query mapper)

Backlog item SIMPL-21648

#### Storycard

**As** Governance Authority\
**I want to** provide the version history of one of a Provider's own resource description upon request via API\
**So that** they can review it and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** The Governance authority provides the version history of a resource description via API - positive case\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description was published by the requesting Provider\
then the version history of this resource description is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: unauthorized\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is not authorised with the identity attributes to manage resource descriptions\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: not own resource description\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description was not published by the requesting Provider\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: resource description not existing\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description does not exist\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: not acceptable\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the request is invalid\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: internal server error\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and there is an internal server error\
then an error message is returned.

---

Back to [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions) · [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) · [BP05B process page](../README.md)
