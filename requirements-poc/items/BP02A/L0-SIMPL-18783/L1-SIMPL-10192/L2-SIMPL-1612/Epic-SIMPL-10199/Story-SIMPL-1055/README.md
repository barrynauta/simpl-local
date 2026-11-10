> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-1055**

# SIMPL-1055: [UI] Removing an Identity Attribute

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-1055** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attributes Management page, viewing the Identity Attributes table list\
**I want to** have for each element in the table list a button to delete the Identity Attribute that after accepting a confirmation dialog on removal, removes the Identity Attribute\
**So that** The Identity Attribute can no longer be assigned

**Acceptance Criteria:**

1. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user navigates to the Identity Attributes Management page, **then** each row in the Identity Attributes table list displays a "Delete" button.

2. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user clicks on the "Delete" button for an Identity Attribute, **then** a confirmation dialog is displayed, asking the user to confirm the removal of the Identity Attribute.

3. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user confirms the removal of an Identity Attribute, **then** the system removes the Identity Attribute from the list and updates the table list accordingly.

4. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user cancels the removal of an Identity Attribute from the confirmation dialog, **then** the system does not remove the Identity Attribute and the table list remains unchanged.

5. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the system removes an Identity Attribute, **then** a success message is displayed indicating that the Identity Attribute was removed successfully.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
