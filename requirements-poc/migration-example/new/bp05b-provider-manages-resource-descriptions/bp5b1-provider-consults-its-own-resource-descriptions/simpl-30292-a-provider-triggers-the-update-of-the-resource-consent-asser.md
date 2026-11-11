<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.1 - A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) / [5B.1.2 A Provider consults the details of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions) / Epic
</div>

# Epic: A Provider triggers the update of the resource consent assertion related to a selected resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.1.2 A Provider consults the details of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions), under high-level requirement [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md). In the process it belongs to step [Trigger (resource description management)](../README.md#trigger--resource-description-management).

In the backlog (Jira) it is item SIMPL-30292. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** trigger the update of the resource consent assertion related to a selected resource description via UI\
**So that** I can ensure that the access to the resource is aligned the resource consent status.

**Acceptance criterion 1**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** I am retrieving the details of a resource description in the catalogue\
   **When** I check the resource consent status\
   **And** I choose to update the resource consent status\
   **Then** I am redirected to the consent manager to perform the change

**Acceptance criterion 2**

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions and consent\
   **When** I have updated the status of a resource consent assertion\
   **Then** I am redirected to the catalogue\
   **And** the resource descriptions affected by the resource consent assertion are shown.

## User stories

1. [A Provider selects a resource description and triggers the update of the resource consent status](#user-story-1-a-provider-selects-a-resource-description-and-triggers-the-update-of-the-resource-consent-status)

### User story 1: A Provider selects a resource description and triggers the update of the resource consent status

Backlog item SIMPL-36072

**As a** Provider\
**I want to** trigger the update of the resource consent assertion related to a selected resource description via UI\
**So that** I can ensure that the access to the resource is aligned the resource consent status.

**Acceptance criteria**

**AC1 - Select a resource from the catalogue for which I possibly want to update the resource consent status**

1. **Given** I am authenticated as a Provider user\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** one of my own resource descriptions has a linked resource consent assertion\
   **When** I retrieve the details of this resource description from the catalogue\
   **Then** the resource consent status is displayed\
   **And** a button to update the resource consent status is available

**AC2 - I decide to update the resource consent status**

2. **Given** I am authenticated as a Provider user\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** a resource description along with a resource consent status is displayed\
   **When** I choose to update the resource consent status\
   **Then** I am redirected to the consent manager to perform the change

**AC3 - Observe the result of the consent change in the catalogue**

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions and consent\
   **When** I have updated the status of a resource consent assertion\
   **Then** I am redirected to the catalogue\
   **And** the resource descriptions affected by the resource consent assertion are shown

**AC4 - Select a resource from the catalogue for which I want to update the resource consent status (Negative scenario: Resource consent not defined)**

4. **Given** I am authenticated as a Provider user\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** a selected resource for which resource consent is not defined\
   **When** I retrieve the details of this resource description from the catalogue\
   **Then** the resource consent status is not displayed\
   **And** a button to update the resource consent status is not shown

**AC5 -** **I decide to update the resource consent status** **(Negative scenario: Consent manager not available)**

5. **Given** I am authenticated as a Provider user\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** a resource description along with a resource consent status is displayed\
   **And** the consent manager is currently not available\
   **When** I choose to update the resource consent status\
   **Then** I receive an error message that shows that the consent manager is not available

**AC6 - Observe the result of the consent change in the catalogue (Negative scenario: Catalogue not available)**

6. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** the catalogue is not available\
   **When** I have updated the status of a resource consent assertion\
   **Then** I am redirected to the catalogue\
   **And** I receive an error message that shows that the catalogue is not available

---

Back to [5B.1.2 A Provider consults the details of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions) · [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) · [BP05B process page](../README.md)
