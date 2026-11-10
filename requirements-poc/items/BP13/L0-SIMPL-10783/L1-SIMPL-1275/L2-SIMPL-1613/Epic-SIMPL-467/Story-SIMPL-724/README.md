> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1275](../../../README.md) / [L2-SIMPL-1613](../../README.md) / [Epic-SIMPL-467](../README.md) / **Story-SIMPL-724**

# SIMPL-724: Identity Attribute Synchronization Procedure

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1275](../../../README.md) (L1) › [SIMPL-1613](../../README.md) (L2) › [SIMPL-467](../README.md) (Epic) › **SIMPL-724** (User story)

## Text

**As a** Dataspace participant agent\
**I want to** Update the local Identity Attribute (Identity Attribute (internal page)) copy with the one present in the Ephemeral Proof (Ephemeral Proof (internal page))\
**So that** I can keep the Identity Attribute local copy in sync with the latest Ephemeral Proof.

**Acceptance Criteria:**

1. **Given** that I am a dataspace participant agent, **when** I receive an ephemeral proof, **then** I update the local Identity Attribute copy with the one present in the ephemeral proof

2. **Given** that I am a dataspace participant agent, **when** I receive an ephemeral proof, **then** the local Identity Attribute copy is updated with the latest and updated list of Identity Attributes

3. **Given** that I am a dataspace participant agent and I receive multiple ephemeral proofs, **when** I update the local Identity Attribute copy, **then** the local copy is updated with the latest and updated list of Identity Attributes each time

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-467](../README.md) (Epic) Synchronisation process for local identity attribute list update |
