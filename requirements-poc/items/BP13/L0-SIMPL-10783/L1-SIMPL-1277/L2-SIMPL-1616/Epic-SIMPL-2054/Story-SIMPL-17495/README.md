> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-17495**

# SIMPL-17495: [API] Participant agent retrieves participant details by credential

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-17495** (User story)

## Text

**As a** participant agent inquiring the Dataspace Identity Provider\
**I want to** via an API, request information about a participant associated with a given credential\
**So that** I can obtain the participant's organisation, identifiers, and tier 1 public key to validate their identity and be sure of the peer identity

**Acceptance Criteria:**

1. **Given** I am a participant agent\
   **And** I am authenticated via mTLS using a valid certificate issued by the Dataspace Governance Authority\
   **When** I send a GET /credentials/{credentialId}/participant request\
   **And** I provide a valid credentialId\
   **Then** the API returns HTTP 200 OK\
   **And** the response body contains a JSON object with at least the following fields:\
   id: the unique UUID of the participant\
   organization: the name of the organisation represented by the participant\
   tierOnePublicKey: the participant's Tier 1 public key in PEM format\
   creationTimestamp: the timestamp of participant creation\
   updateTimestamp: the timestamp of the last update

2. **Given** I am a participant agent\
   **And** I am not properly authenticated via mTLS\
   **When** I send a GET /credentials/{credentialId}/participant request\
   **Then** the API returns HTTP 401 Unauthorized

3. **Given** I am a participant agent\
   **And** I am authenticated via mTLS using a valid certificate issued by the Dataspace Governance Authority\
   **And** I provide a malformed credentialId\
   **When** I send a GET /credentials/ {credentialId}/participant request\
   **Then** the API returns HTTP 400 Bad Request

4. **Given** I am a participant agent\
   **And** I am authenticated via mTLS using a valid certificate issued by the Dataspace Governance Authority\
   **And** the credentialId I provide is valid in format, but not associated with any participant\
   **When** I send a GET /credentials/{credentialId}/participant request\
   **Then** the API returns HTTP 404 Not Found

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
