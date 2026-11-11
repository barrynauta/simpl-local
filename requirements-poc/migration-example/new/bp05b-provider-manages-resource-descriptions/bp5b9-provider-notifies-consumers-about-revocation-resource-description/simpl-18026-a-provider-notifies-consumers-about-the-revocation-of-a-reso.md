<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.9 - The Provider notifies Consumers about the revocation of a resource description](../bp5b9-provider-notifies-consumers-about-revocation-resource-description.md) / [5B.9.1 A Provider notifies Consumers about the revocation of a resource description](../bp5b9-provider-notifies-consumers-about-revocation-resource-description.md#5b91-a-provider-notifies-consumers-about-the-revocation-of-a-resource-description) / Epic
</div>

# Epic: A Provider notifies Consumers about the revocation of a resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.9.1 A Provider notifies Consumers about the revocation of a resource description](../bp5b9-provider-notifies-consumers-about-revocation-resource-description.md#5b91-a-provider-notifies-consumers-about-the-revocation-of-a-resource-description), under high-level requirement [5B.9 The Provider notifies Consumers about the revocation of a resource description](../bp5b9-provider-notifies-consumers-about-revocation-resource-description.md). In the process it belongs to step [BP05B.15 Inform about revocation](../README.md#bp05b15-inform-about-revocation).

In the backlog (Jira) it is item SIMPL-18026. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** notify Consumers about a revoked resource description\
**So that** they can react on this.

#### Acceptance criteria

**Scenario:** A Provider notifies Consumers about the revocation of a resource description

1. **Given** I, the Provider, have requested the revocation of a resource description\
   **When** the resource description is revoked\
   **Then** a notification about the revocation is sent to the Consumers of the resource descriptions with an active contract.

## User stories

1. [[API] A Provider notifies Consumers about revocation of Resource Description](#user-story-1-api-a-provider-notifies-consumers-about-revocation-of-resource-description)
2. [[UI] Inform end user (provider) about successful revocation](#user-story-2-ui-inform-end-user-provider-about-successful-revocation)

### User story 1: [API] A Provider notifies Consumers about revocation of Resource Description

Backlog item SIMPL-19790

#### Storycard

**As a** Provider\
**I want to** notify Consumers about the revocation of a resource description\
**So that** they can react on this.

#### Acceptance criteria

**Scenario:** The Provider notifies Consumers - revokes a resource description - positive case

1. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke an own resource description via API\
   **And** the revocation of the resource description is successful\
   **Then** Simpl-Open send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

**Scenario:** The Provider notifies Consumers - revokes a resource description - negative case: revocation is not successful

2. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke an own resource description via API\
   **And** the revocation of the resource description is not successful\
   **And** and an error message is returned via API\
   **Then** Simpl-Open does not send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

###### Technical Details

The list of Consumers to be notified shall be retrieved from the Contract Management component through the **POST /agreements/search** API, filtering agreements associated with the revoked Resource Description by its asset identifier.

Only Consumers with an **active agreement** associated with the revoked Resource Description shall be considered eligible recipients.

For each Consumer participant returned by the Contract Management component:

1. Retrieve the participant's contact information from the Authentication Provider through the **GET /tier1/v2/participants/{participantId}** API.
1. Extract the participant's **contactEmail** from the response payload.
1. Send a notification email to the extracted email address through the Notification Service using the **POST /notify** API.

This workflow ensures that all Consumers with an active agreement linked to the revoked Resource Description are notified of the revocation through their registered contact email address.

###### Orchestration Flow - GA to All Active Participants

1. Resource Description revocation completed successfully.
1. Call Contract Manager `/agreements/search` using the revoked resource identifier.
1. Extract participant IDs from active agreements.
1. For each participant ID call Authentication Provider `{}/tier1/v2/participants/{participantId}{{`}}.
1. Retrieve `{}contactEmail{`}.
1. Send notification via Notification Service.Log failures per recipient and continue processing remaining recipients.\
   #
```
curl -X POST http://localhost:8080/notify/ \
  -d "urls=mailto://${smtpuser}:${smtppass}@mailpit-common01-smtp.common01.svc.cluster.local:587/${toEmail}?from=notifications@simpl.local&mode=insecure" \
  -d "title=Notice: Metadata Quality Profile Updated (v1.2.0)" \
  -d "body=Dear Participant, a new Metadata Quality Profile version (v1.2.0) has become effective. Change type: update. Effective: 2026-07-30T14:55:00Z. Please review the impact on your quality calculations."
```

*

   - `toEmail` is resolved per Participant from *GET tier1/v2/participant/participantId*\
   (Authentication Provider)
   - `smtpuser` / `smtppass` are injected from the Notification Service deployment configuration

1. Failures per recipient are logged with participant ID and profile version; the loop continues for remaining recipients.

### User story 2: [UI] Inform end user (provider) about successful revocation

Backlog item SIMPL-22586

#### Storycard

**As a** Provider\
**I want to** notify Consumers about the revocation of a resource description\
**So that** they can react on this.

#### Acceptance criteria

**Scenario:** The Provider notifies Consumers - revokes a resource description - positive case

1. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke an own resource description via UI\
   **And** the revocation of the resource description is successful\
   **Then** Simpl-Open send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

**Scenario:** The Provider notifies Consumers - revokes a resource description - negative case: revocation is not successful

2. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke an own resource description via UI\
   **And** the revocation of the resource description is not successful\
   **And** and an error message is returned via UI\
   **Then** Simpl-Open does not send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

**Scenario:** The Provider notifies Consumers - revokes a resource description - negative case: internal server error

3. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke a resource description via UI\
   **And** there is an internal error\
   **And** and an error message is returned via UI\
   **Then** Simpl-Open does not send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

#### Technical details

---

Back to [5B.9.1 A Provider notifies Consumers about the revocation of a resource description](../bp5b9-provider-notifies-consumers-about-revocation-resource-description.md#5b91-a-provider-notifies-consumers-about-the-revocation-of-a-resource-description) · [5B.9 The Provider notifies Consumers about the revocation of a resource description](../bp5b9-provider-notifies-consumers-about-revocation-resource-description.md) · [BP05B process page](../README.md)
