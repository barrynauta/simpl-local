> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-21117](../../../README.md) / [L2-SIMPL-14642](../../README.md) / [Epic-SIMPL-18026](../README.md) / **Story-SIMPL-19790**

# SIMPL-19790: [API] A Provider notifies Consumers about revocation of Resource Description

**User story (implementable work)** · Development: **In development**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-21117](../../../README.md) (L1) › [SIMPL-14642](../../README.md) (L2) › [SIMPL-18026](../README.md) (Epic) › **SIMPL-19790** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18026](../README.md) (Epic) A Provider notifies Consumers about the revocation of a resource description |
