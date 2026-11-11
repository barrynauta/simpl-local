<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.6 - A Provider notifies Consumers about a new version of a Resource description](../bp5b6-provider-notifies-consumers-about-new-version-resource-description.md) / [5B.6.1 A Provider notifies Consumers about a new version of a Resource description](../bp5b6-provider-notifies-consumers-about-new-version-resource-description.md#5b61-a-provider-notifies-consumers-about-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider notifies Consumers about a new version of a Resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.6.1 A Provider notifies Consumers about a new version of a Resource description](../bp5b6-provider-notifies-consumers-about-new-version-resource-description.md#5b61-a-provider-notifies-consumers-about-a-new-version-of-a-resource-description), under high-level requirement [5B.6 A Provider notifies Consumers about a new version of a Resource description](../bp5b6-provider-notifies-consumers-about-new-version-resource-description.md). In the process it belongs to step [BP05B.12 Inform about new version](../README.md#bp05b12-inform-about-new-version).

In the backlog (Jira) it is item SIMPL-17521. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** notify the Consumers of a resource about a published new version of a Resource description\
**So that** so that Consumers can decide if any actions are required based on this new version.

#### Acceptance criteria

**Scenario:** A Provider notifies Consumers about a new version of a Resource description

1. **Given** I, the the Provider, have requested the publication of a new version of a resource description\
   **When** the new version of the Resource description is published\
   **Then** a notification about the new version is sent to the Consumers of the previous versions of the Resource description with an active contract.

## User stories

1. [[BE] A Provider notifies Consumers about a new version of a Resource description](#user-story-1-be-a-provider-notifies-consumers-about-a-new-version-of-a-resource-description)

### User story 1: [BE] A Provider notifies Consumers about a new version of a Resource description

Backlog item SIMPL-21600

#### Storycard

**As a** Provider\
**I want to** notify the Consumers of a resource about a published new version of a Resource description\
**So that** so that Consumers can decide if any actions are required based on this new version.

##### Acceptance Criteria

###### AC1 - Provider notifies Consumers about a new version of a Resource Description

1. **Given** I, as a Provider, have requested the publication of a new version of a Resource Description\
   **When** the publication of the new version of the Resource Description is successful\
   **Then** the system retrieves the Consumers having an active contract associated with previous version of the Resource Description\
   **And** a notification is sent to those Consumers informing them that a new version of the Resource Description has been published.

###### AC2 - Publication of a new version is not successful

2. **Given** I, as a Provider, have requested the publication of a new version of a Resource Description\
   **When** the publication of the new version of the Resource Description fails\
   **Then** no notification is sent to any Consumer\
   **And** the notification process is not triggered.

##### Technical Details

The list of Consumers to be notified shall be retrieved from the Contract Management component by invoking the **`/agreements/search`** API exposed by Contract Manager ([https://code.europa.eu/simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be/-/blob/main/openapi/openapi3-v1.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be/-/blob/main/openapi/openapi3-v1.yaml?ref_type=heads)).

Only Consumers having an **active contract** associated with a previous version of the Resource Description shall be considered eligible recipients for the notification.

Once the list of eligible Consumers has been retrieved, the notification email shall be sent through the Notification Service.

---

Back to [5B.6.1 A Provider notifies Consumers about a new version of a Resource description](../bp5b6-provider-notifies-consumers-about-new-version-resource-description.md#5b61-a-provider-notifies-consumers-about-a-new-version-of-a-resource-description) · [5B.6 A Provider notifies Consumers about a new version of a Resource description](../bp5b6-provider-notifies-consumers-about-new-version-resource-description.md) · [BP05B process page](../README.md)
