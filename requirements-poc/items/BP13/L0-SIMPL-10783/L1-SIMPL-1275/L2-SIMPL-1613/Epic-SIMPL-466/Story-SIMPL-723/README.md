> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1275](../../../README.md) / [L2-SIMPL-1613](../../README.md) / [Epic-SIMPL-466](../README.md) / **Story-SIMPL-723**

# SIMPL-723: Request Ephemeral Proof for Identity Attribute Synchronization

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1275](../../../README.md) (L1) › [SIMPL-1613](../../README.md) (L2) › [SIMPL-466](../README.md) (Epic) › **SIMPL-723** (User story)

## Text

**As a** Dataspace participant agent after any request of a signed ephemeral proof to dataspace governance authority\
**I want to** check any difference between my local Identity Attribute (Identity Attribute (internal page)) list copy and the list of Identity Attributes contained in the signed Ephemeral Proof (Ephemeral Proof (internal page)) and, if there is a difference, trigger a synchronization procedure to update the local Identity Attribute list copy\
**So that** I can keep the local Identity Attribute list copy in sync with the latest ephemeral proof

**Acceptance Criteria:**

1. **Given** that I am a dataspace participant agent and the proof contains differences between my stored list and the new one, **when** I receive the proof, **then** a synchronization procedure is triggered to update the local Identity Attribute copy

2. **Given** that I am a dataspace participant agent and the proof does not contain differences between my stored list and the new one, **when** I receive the proof, **then** no synchronization procedure is triggered and the local Identity Attribute copy remains unchanged

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-466](../README.md) (Epic) Monitoring service for identity attributes syncronization |
