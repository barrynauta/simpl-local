> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-17496**

# SIMPL-17496: [API] Participant agent retrieves participant details by identifier

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-17496** (User story)

## Text

**As a**\
participant agent inquiring the Dataspace Identity Provider

**I want to**\
via an API, request information about a participant identified by a given identifier

**So that**\
I can obtain the participant's organization, identifiers, and tier 1 public key to validate their identity and be sure of the peer identity

**Acceptance Criteria:**

1. **GIVEN** I am a participant agent\
   **AND** I am authenticated via mTLS using a valid certificate issued by the Dataspace Governance Authority\
   **WHEN** I send a GET /participants/{participantId} request\
   **AND** I provide a valid UUID participantId\
   **THEN** the API returns HTTP 200 OK\
   **AND** the response body contains a JSON object with at least the following fields:\
   *id:* the unique UUID of the participant\
   *organization:* the name of the organization represented by the participant\
   *tierOnePublicKey:* the participant's Tier 1 public key in PEM format\
   *creationTimestamp:* the timestamp of participant creation\
   *updateTimestamp:* the timestamp of last update
1. **GIVEN** I am a participant agent\
   **AND** I am not properly authenticated via mTLS\
   **WHEN** I send a GET /participants/{participantId} request\
   **THEN** the API returns HTTP 401 Unauthorized
1. **GIVEN** I am a participant agent\
   **AND** I am authenticated via mTLS using a valid certificate issued by the Dataspace Governance Authority\
   **AND** the participantId path parameter is not a valid UUID\
   **WHEN** I send a GET /participants/{participantId} request\
   **THEN** the API returns HTTP 400 Bad Request
1. **GIVEN** I am a participant agent\
   **AND** I am authenticated via mTLS using a valid certificate issued by the Dataspace Governance Authority\
   **AND** I provide a valid UUID participantId that is not associated to any participant\
   **WHEN** I send a GET /participants/{participantId} request\
   **THEN** the API returns HTTP 404 Not Found

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
