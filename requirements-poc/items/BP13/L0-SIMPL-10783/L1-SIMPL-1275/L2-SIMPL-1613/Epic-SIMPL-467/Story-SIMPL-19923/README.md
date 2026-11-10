> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1275](../../../README.md) / [L2-SIMPL-1613](../../README.md) / [Epic-SIMPL-467](../README.md) / **Story-SIMPL-19923**

# SIMPL-19923: [API] Participant triggers manual synchronisation of identity attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1275](../../../README.md) (L1) › [SIMPL-1613](../../README.md) (L2) › [SIMPL-467](../README.md) (Epic) › **SIMPL-19923** (User story)

## Text

*The endpoint covered with this user story has been managed with API Definition Task SIMPL-20533*\
----

**As a** authenticated user with one one of the\
**I want to** via an API, update my local copy of identity attributes\
**So that** I can manage my participant's agent with up-to-date identity attributes provided by the Governance Authority.

**Acceptance Criteria:**

1. **Given** I am an authenticated user with T1UAR_M role\
   **When** I send a POST /tier1/v2/identityAttributes/synchronize\
   **Then** the local copy of identity attributes is updated by retrieving the list from the Governance Authority\
   **And** I receive a 204 No Content response

2. **Given** I am an authenticated user with T1UAR_M role\
   **When** I send a POST /tier1/v2/identityAttributes/synchronize\
   **And** the request is invalid\
   **Then** I receive a 400 Bad Request

3. **Given** I am an unauthenticated user\
   **When** I send a POST /tier1/v2/identityAttributes/synchronize\
   **Then** I receive a 401 Unauthorized response

4. **Given** I am an authenticated without T1UAR_M role\
   **When** I send a POST /tier1/v2/identityAttributes/synchronize\
   **Then** I receive a 403 Forbidden response

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-467](../README.md) (Epic) Synchronisation process for local identity attribute list update |
