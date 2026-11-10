> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1275](../../../README.md) / [L2-SIMPL-1613](../../README.md) / [Epic-SIMPL-467](../README.md) / **Story-SIMPL-725**

# SIMPL-725: Identity Attribute Synchronization Procedure - attributes to local roles assignment update procedure

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1275](../../../README.md) (L1) › [SIMPL-1613](../../README.md) (L2) › [SIMPL-467](../README.md) (Epic) › **SIMPL-725** (User story)

## Text

**As A**

Dataspace participant agent, after saving the Identity Attribute ([Identity Attribute|an internal page]) local copy

**I want to**

Update the status of assignments between Identity Attributes and local roles in the following way: each Identity Attribute present in the saved local copy will enable any existing assignments. The remaining assignments with Identity Attributes not present in the local copy will be disabled.

**So that**

the assignments between local roles and participant identity attributes remain in sync.

**Acceptance Criteria:**

- **Given that I am a Dataspace participant agent, when I save the Identity Attribute** **local copy, then I can update the status of assignments between attributes and local roles.**
- **Given that I am a Dataspace participant agent, when an identity attribute is present in the saved local copy, then the corresponding assignment is enabled.**
- Given that I am a Dataspace participant agent, when an identity attribute is not present in the saved local copy, then the corresponding assignment is disabled.{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-467](../README.md) (Epic) Synchronisation process for local identity attribute list update |
