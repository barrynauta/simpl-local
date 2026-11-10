> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2049](../README.md) / **Story-SIMPL-17648**

# SIMPL-17648: [API] Participant user pings a remote participant agent

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2049](../README.md) (Epic) › **SIMPL-17648** (User story)

## Text

*This user story outlines the adaptation and migration of the v1 API implementation to v2, based on guidelines derived from the API definition tasks. Linked to this, there are the API definition tasks that explain what this API will substitute.*

**As a**\
participant user with no specific roles in the agent

**I want to**\
check connectivity to another participant agent by its fully qualified domain name (FQDN)

**So that**\
I can verify the remote agent is reachable

**Acceptance Criteria:**

1. **GIVEN** that I am an authenticated user with no specific role\
   **AND** I provide a valid fqdn query parameter\
   **WHEN** I send a GET /ping request\
   **THEN** the API returns HTTP 200 OK\
   **AND** the response body is a JSON object containing at least:\
   *id:* the unique UUID of the remote participant\
   *organization:* the name of the remote participant's organization\
   *identityAttributes:* a list of assigned identity attributes (each with at least id, code, name)
1. **GIVEN** that I am an authenticated user with no specific role\
   **AND** the FQDN query parameter is missing or invalid\
   **WHEN** I send a GET /ping request\
   **THEN** the API returns HTTP 400 Bad Request
1. **GIVEN** that I am not authenticated\
   **WHEN** I send a GET /ping? request\
   **THEN** the API returns HTTP 401 Unauthorized

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2049](../README.md) (Epic) Secure interaction between agents |
