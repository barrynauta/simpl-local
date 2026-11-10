> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2049](../README.md) / **Story-SIMPL-4026**

# SIMPL-4026: Sending ping request to dataspace agents

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2049](../README.md) (Epic) › **SIMPL-4026** (User story)

## Text

**As A**

dataspace participant agent wanting to test the connectivity to other dataspace agents

**I want to**

send a *ping* request to another identified dataspace participant. The response contains the identity attributes sent during the ephemeral proof exchange.

**So that**

I can verify if the connectivity between the two agents is in place

**Acceptance Criteria:**

- **Given that I am a dataspace participant agent, when I send a ping request to another identified dataspace participant agent, then the API returns a 200 OK status code.**
- **Given that I am a dataspace participant agent, when I send a ping request to another identified dataspace participant agent, then the API returns a pong response containing the identity attributes sent during the ephemeral proof exchange.**
- Given that I am a dataspace participant agent, when I send multiple ping requests to the same or different dataspace participant agents, then the API returns a response each time, with the identity attributes based on the most recent ephemeral proof exchanged.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2049](../README.md) (Epic) Secure interaction between agents |
