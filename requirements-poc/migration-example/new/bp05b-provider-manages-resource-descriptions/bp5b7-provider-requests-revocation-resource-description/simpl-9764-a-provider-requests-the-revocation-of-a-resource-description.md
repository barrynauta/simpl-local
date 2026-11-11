<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.7 - A Provider requests the revocation of a Resource description](../bp5b7-provider-requests-revocation-resource-description.md) / [5B.7.1 A Provider requests the revocation of a resource description](../bp5b7-provider-requests-revocation-resource-description.md#5b71-a-provider-requests-the-revocation-of-a-resource-description) / Epic
</div>

# Epic: A Provider requests the revocation of a resource description via UI or API

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.7.1 A Provider requests the revocation of a resource description](../bp5b7-provider-requests-revocation-resource-description.md#5b71-a-provider-requests-the-revocation-of-a-resource-description), under high-level requirement [5B.7 A Provider requests the revocation of a Resource description](../bp5b7-provider-requests-revocation-resource-description.md). In the process it belongs to step [BP05B.13 Request resource description revocation](../README.md#bp05b13-request-resource-description-revocation).

In the backlog it is item SIMPL-9764, status **Defined**. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** be able to revoke a Resource description via UI or API\
**So that** the Resource description cannot be used any more.

#### Acceptance criteria

**Scenario:** A Provider requests the revocation of a Resource description via UI or API

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke a Resource description\
   **Then** a revocation request is sent to the Governance authority.

## User stories

1. [[UI] A Provider requests the revocation of a Resource description via UI](#user-story-1-ui-a-provider-requests-the-revocation-of-a-resource-description-via-ui) (DevTeam PBI is ready for Implementation check)
2. [[API] Provider requests the revocation of a Resource description via API (SD tooling)](#user-story-2-api-provider-requests-the-revocation-of-a-resource-description-via-api-sd-tooling) (Ready for DG Acceptance)

### User story 1: [UI] A Provider requests the revocation of a Resource description via UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-19784)

#### Storycard

**As a** Provider\
**I want to** revoke a Resource description via UI\
**So that** the Resource description cannot be used any more.

#### Acceptance criteria

**Scenario:** A Provider requests the revocation of a Resource description via UI - positive case

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I request to revoke a Resource description\
   **Then** I am asked to confirm the revocation\
   **And** upon confirmation a revocation request is sent to the Governance authority.

**Scenario:** A Provider requests the revocation of a Resource description via UI - declining confirmation

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I request to revoke a Resource description\
   **And** I am asked to confirm the revocation\
   **And** I decline to confirm\
   **Then** I am guided back to the details view.

**Scenario:** A Provider requests the revocation of a Resource description via UI - API error

3. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I demand to revoke a resource description\
   **And** I have confirmed the revocation\
   **And** the API to revoke the resource description throws an error\
   **Then** the revocation of the resource description is not possible\
   **And** a notification is shown that the resource description cannot be revoked.

**Scenario:** A Provider requests the revocation of a Resource description via UI - not authenticated

4. **Given** I am not authenticated as Provider\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

**Scenario:** A Provider requests the revocation of a Resource description via UI  - no role

5. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of resource rescriptions\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

#### Mockups:

| RD Details | Confirmation popup |
| --- | --- |
| !image-2026-03-23-12-53-45-950.png | width=428,height=201! | !Confirm.jpg | width=317,height=175! |

### User story 2: [API] Provider requests the revocation of a Resource description via API (SD tooling)

**Status:** Ready for DG Acceptance (backlog item SIMPL-19785)

#### Storycard

**As a** Provider\
**I want to** be able to revoke a Resource description via API\
**So that** the Resource description cannot be used any more.

#### Acceptance criteria

**Scenario:** A Provider requests the revocation of a Resource description via API - positive case

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke a Resource description\
   **Then** a revocation request is sent to the Governance authority.

**Scenario:** A Provider requests the revocation of a Resource description via API - not an own resource description

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke a resource description that is not published by me\
   **Then** a message is returned that I am not authorised.

**Scenario:** A Provider requests the revocation of a Resource description via API - not existing resource description

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke an inexistent resource description\
   **Then** a message is returned that the resource description cannot be found.

**Scenario:** A Provider requests the revocation of a Resource description via API - bad request

4. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke a Resource description but using an invalid resource description ID\
   **Then** a message is returned that the request is bad.

**Scenario:** A Provider requests the revocation of a Resource description via API - wrong role

5. **Given** I am authenticated as a Provider\
   **And** I do not have a role authorising the management of Resource descriptions\
   **When** I want to revoke a Resource description\
   **Then** a message is returned that I am not authorised.

**Scenario:** A Provider requests the revocation of a Resource description via API - internal server error

6. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke a Resource description\
   **And** there is an internal error\
   **Then** a message is returned that the request cannot be fulfilled.

#### Technical information

- API of the SD tooling component.
- Endpoint defined in SIMPL-20726 (internal page) POST [/resourceDescriptions/{resourceDescriptionId}/revoke](https://code.europa.eu/-/sandbox/swagger#/Resource%20Description/revokeResourceDescription)

---

Back to [5B.7.1 A Provider requests the revocation of a resource description](../bp5b7-provider-requests-revocation-resource-description.md#5b71-a-provider-requests-the-revocation-of-a-resource-description) · [5B.7 A Provider requests the revocation of a Resource description](../bp5b7-provider-requests-revocation-resource-description.md) · [BP05B process page](../README.md)
