> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-17516**

# SIMPL-17516: [API] Dataspace Participant agent retrieves participant details by credential

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-17516** (User story)

## Text

**As a** dataspace agent\
**I want to** via an API, request information about the participant associated with a given credential\
**So that** I can resolve a credential to participant information (organization and identifier) to validate their identity and be sure of the peer identity

**Acceptance Criteria:**

1. **Given** the caller is an internal service within the participant agent\
   **And** it provides a valid credentialId\
   **When** it sends a GET /credentials/{credentialId}/participant request\
   **Then** the API returns HTTP 200 OK\
   **And** the response body contains a JSON object with at least the following fields:\
   *id:* the unique UUID of the participant\
   *organization:* the name of the organisation represented by the participant\
   *authorityCreationTimestamp:* the timestamp of participant creation\
   authorityUpdateTimestamp: the timestamp of the last update

2. **Given** the caller is an internal service within the participant agent\
   **And** it provides a malformed credentialId\
   **When** it sends a GET /credentials/{credentialId}/participant request\
   **Then** the API returns HTTP 400 Bad Request

3. **Given** the caller is an internal service within the participant agent\
   **And** it provides a credentialId that is not associated with any participant\
   **When** it sends a GET /credentials/ {credentialId}/participant request\
   **Then** the API returns HTTP 404 Not Found

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
