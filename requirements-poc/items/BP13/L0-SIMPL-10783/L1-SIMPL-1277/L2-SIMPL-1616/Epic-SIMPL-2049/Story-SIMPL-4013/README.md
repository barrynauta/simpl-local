> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2049](../README.md) / **Story-SIMPL-4013**

# SIMPL-4013: Provide ping functionality to dataspace agents

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2049](../README.md) (Epic) › **SIMPL-4013** (User story)

## Text

**As a** dataspace participant agent receiving the *ping* request from another identified dataspace participant agent (the requestor)\
**I want to** expose an API that provides a *pong* response to the requestor, containing the identity attributes received during the ephemeral proof exchange from the requestor agent. This API must not have any ABAC control in place (it can be invoked by any other dataspace participant)\
**So that** the requestor can call that API to test the connectivity to other agents within dataspace.

**Acceptance Criteria:**

1. **Given** that I am a dataspace agent\
   **When** I receive a ping request via the API from an identified dataspace participant\
   **Then** the API returns a 200 OK status code.

2. **Given** that I am a dataspace agent\
   **When** I receive a ping request via the API from an identified dataspace participant\
   **Then** the API returns a pong response containing the identityAttributes received during the ephemeral proof exchange with the requesting agent.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2049](../README.md) (Epic) Secure interaction between agents |
