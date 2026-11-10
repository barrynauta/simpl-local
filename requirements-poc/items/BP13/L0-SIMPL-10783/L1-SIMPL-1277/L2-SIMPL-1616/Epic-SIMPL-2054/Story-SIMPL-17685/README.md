> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-17685**

# SIMPL-17685: [API] Dataspace participant agent retrieves its participant profile

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-17685** (User story)

## Text

**As a** dataspace agent\
**I want to** retrieve my participant information from the Dataspace Governance Authority\
**So that** I can review my participant details

**Acceptance Criteria:**

1. **Given** the caller is a participant agent authenticated over mTLS with a valid Tier-2 credential\
   **When** it sends a GET /participant request to the Dataspace Governance Agent\
   **Then** the API returns HTTP 200 OK\
   **And** the response body is a JSON object containing at least:\
   *id:* the participant UUID\
   *organization:* the participant's organization name\
   *creationTimestamp:* ISO 8601 timestamp of participant creation\
   *updateTimestamp:* ISO 8601 timestamp of last update\
   *tierOnePublicKey:* the Tier-1 public key in PEM format

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
