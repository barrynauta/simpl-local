> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-17645**

# SIMPL-17645: [API] Dataspace Participant agent validates a credential

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-17645** (User story)

## Text

*This user story outlines the adaptation and migration of the v1 API implementation to v2, based on guidelines derived from the API definition tasks. Linked to this, there are the API definition tasks that explain what this API will substitute.*

**As a** dataspace agent\
**I want to** via an API, submit a credential (X.509 certificate in PEM format) to be verified\
**So that** I can confirm the credential is valid in the dataspace before trusting or proceeding with peer interactions

**Acceptance Criteria:**

1. **Given** the caller is an internal service within the participant agent (not via a Tier 1 gateway)\
   **And** it provides a JSON body containing the credential PEM under the content field\
   **When** it sends a POST /credentials/validate\
   **Then** the API returns HTTP 204 No Content (the credential is valid)

2. **Given** the caller is an internal service within the participant agent (not via a Tier 1 gateway)\
   **And** it provides an invalid JSON body\
   OR it provides a JSON body containing a malformed content field as the credential to verify\
   **When** it sends a POST /credentials/validate\
   **Then** the API returns HTTP 400 Bad Request

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
