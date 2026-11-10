> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2049](../README.md) / **Story-SIMPL-17653**

# SIMPL-17653: [API] Dataspace Participant agent pings a peer participant

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2049](../README.md) (Epic) › **SIMPL-17653** (User story)

## Text

**As a**\
dataspace agent

**I want to**\
send a ping request to a peer participant's endpoint

**So that**\
I can verify mutual connectivity and retrieve the peer's public identity details recognized in the dataspace

**Acceptance Criteria:**

1. **GIVEN** the caller is a participant agent presenting a valid Tier-2 credential and an ephemeral proof over mTLS\
   **WHEN** it sends a GET /ping request to a peer agent\
   **THEN** the API returns HTTP 200 OK\
   **AND** the response body contains a JSON object with at least:\
   *id:* the peer participant UUID\
   *organization:* the peer participant's organization name\
   *identityAttributes:* an array containing the peer's assigned identity attributes
1. **GIVEN** the caller is a participant agent authenticated over mTLS\
   **AND** the caller does not provide a Tier-2 credential to identify itself\
   **WHEN** it sends a GET /ping request\
   **THEN** the API returns HTTP 401 Unauthorized

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2049](../README.md) (Epic) Secure interaction between agents |
