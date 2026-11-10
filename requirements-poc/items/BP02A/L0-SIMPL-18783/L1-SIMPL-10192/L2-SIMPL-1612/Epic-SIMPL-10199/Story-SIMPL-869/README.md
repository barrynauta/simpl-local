> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-869**

# SIMPL-869: Disable Identity Attribute via API

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-869** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority

**I want to**

via an API, disable a specific Identity Attribute ([Identity Attribute|an internal page]) and the Identity Attribute is hidden from the Identity Attributes list

**So that**

I cannot assign anymore the specific Identity Attribute to any Participant

**Acceptance Criteria:**

- **Given that the user is logged in with the IATTR_M dataspace governance authority role, and the API endpoint for disabling an identity attribute is available, when the user sends a PUT via the API to update an Identity Attribute with the value for assignable property to false, then the Identity Attribute returned by the update got the assignable value to false**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
