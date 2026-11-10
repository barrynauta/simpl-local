> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-17517**

# SIMPL-17517: [API] Dataspace Participant agent retrieves participant details

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-17517** (User story)

## Text

**As a**\
dataspace agent

**I want to**\
via an API, request information about a given participant

**So that**\
I can obtain the participant's organisation and identifiers to validate their identity and be sure of the peer identity

**Acceptance Criteria:**

1. **GIVEN** the caller is an internal service within the participant agent\
   **AND** it provides a valid UUID participantId\
   **WHEN** it sends a GET /participants/{participantId} request\
   **THEN** the API returns HTTP 200 OK\
   **AND** the response body contains a JSON object with at least the following fields:\
   +*id:*+ the unique UUID of the participant\
   *organization:* the name of the organisation represented by the participant\
   authorityCreationTimestamp: the timestamp of participant creation\
   authorityUpdateTimestamp: the timestamp of the last update
1. **GIVEN** the caller is an internal service within the participant agent\
   **AND** the participantId path parameter is not a valid UUID\
   **WHEN** it sends a GET /participants/{participantId}request\
   **THEN** the API returns HTTP 400 Bad Request
1. **GIVEN** the caller is an internal service within the participant agent\
   **AND** it provides a valid UUID participantId that is not associated with any participant\
   **WHEN** it sends a GET /participants/{participantId} request\
   **THEN** the API returns HTTP 404 Not Found

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
