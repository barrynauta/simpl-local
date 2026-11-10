> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP13](../../../../README.md) / [L0-SIMPL-10783](../../../README.md) / [L1-SIMPL-1275](../../README.md) / [L2-SIMPL-1613](../README.md) / **Epic-SIMPL-467**

# SIMPL-467: Synchronisation process for local identity attribute list update

**Epic (deliverable, testable feature)** · Status **Internal QA check**

**Trace:** [BP13](../../../../README.md) › [SIMPL-10783](../../../README.md) (L0) › [SIMPL-1275](../../README.md) (L1) › [SIMPL-1613](../README.md) (L2) › **SIMPL-467** (Epic)

## Text

**As A**

dataspace participant

**I want to**

have a synchronisation process for identity attributes that will compare the local copy of identity attributes

**So that**

I have an updated list of assignable identity attributes

**Acceptance Criteria**

- **Given a dataspace participant agent with a local role-to-attribute mapping, when the synchronisation process runs, then it updates the role assignments based on the latest identity attribute definitions from the Dataspace Governance Authority**
- **Given an identity attribute that was removed from the Dataspace Governance Authority, when the synchronisation process runs, then the system removes the attribute from all local role assignments**
- **Given a newly introduced identity attribute, when the synchronisation process runs, then the system makes it available for role assignment according to the defined rules**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-1613](../README.md) (L2) Tier 2 attributes management - services |
| **Children** | [SIMPL-724](Story-SIMPL-724/README.md) (User story) Identity Attribute Synchronization Procedure<br>[SIMPL-725](Story-SIMPL-725/README.md) (User story) Identity Attribute Synchronization Procedure - attributes to local roles assignment update procedure<br>[SIMPL-19923](Story-SIMPL-19923/README.md) (User story) [API] Participant triggers manual synchronisation of identity attributes<br>[SIMPL-20512](Story-SIMPL-20512/README.md) (User story) [UI] Participant triggers manual synchronisation of identity attributes |
