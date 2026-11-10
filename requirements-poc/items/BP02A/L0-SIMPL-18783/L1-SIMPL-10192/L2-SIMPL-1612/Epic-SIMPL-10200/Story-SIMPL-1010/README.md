> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10200](../README.md) / **Story-SIMPL-1010**

# SIMPL-1010: [UI] Identity Attribute assignability flag editing

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10200](../README.md) (Epic) › **SIMPL-1010** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attributes Management page, viewing the Identity Attribute detail information page\
**I want to** enable or disable the assignable flag for the specific attribute\
**So that** the Identity Attribute is assignable to Tier 1 local roles by each dataspace participant

**Acceptance Criteria:**

1. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user views the Identity Attribute detail information page, **then** the system displays the current assignable flag status for the selected Identity Attribute.

2. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user toggles the assignable flag status for the selected Identity Attribute, **then** the system updates the assignable flag status for the selected Identity Attribute.

3. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user saves the changes to the assignable flag status, **then** the system reflects the updated assignable flag status for the selected Identity Attribute.

4. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user views the Identity Attribute detail information page after updating the assignable flag status, **then** the system displays the updated assignable flag status for the selected Identity Attribute.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10200](../README.md) (Epic) Making identity attributes assignable on Tier1 roles |
