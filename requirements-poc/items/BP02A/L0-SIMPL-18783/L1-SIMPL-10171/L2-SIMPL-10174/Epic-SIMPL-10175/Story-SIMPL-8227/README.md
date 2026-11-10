> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10174](../../README.md) / [Epic-SIMPL-10175](../README.md) / **Story-SIMPL-8227**

# SIMPL-8227: [UI] Governance Authority selects Security Identity Attributes for an Onboarding Procedure Template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10174](../../README.md) (L2) › [SIMPL-10175](../README.md) (Epic) › **SIMPL-8227** (User story)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, managing the onboarding procedure templates, which are linked to participant types\
**I want** via UI, for each onboarding procedure template, to select identity attributes relevant to the type of participant

when a participant applies by selecting a participant type, the corresponding identity attributes are suggested to the onboarding request approvers with NOTARY role.

**Acceptance Criteria**

1. **Given** I am logged in as Tier 2 Authorization Manager (T2IAA_M) navigating to the details page of a newly created onboarding template for a given participant type **when** I open the Identity Attributes tab **then** I should see an empty list of identity attributes **and** an "Edit Attributes" button that allow me to add them to the template{}

2. **Given** I am logged in as Tier 2 Authorization Manager (T2IAA_M) navigating to the details page of a newly created onboarding template for a given participant type **when** I open the Identity Attributes tab and click the "Edit Attributes" button **then** I should see an empty list of identity attributes **and** an "Add Attribute" button that allow me to add them to the template

3. **Given** I am logged in as Tier 2 Authorization Manager (T2IAA_M) navigating to the Identity Attributes tab of an onboarding template for a given participant type **when** I click the "Add Attribute" button **then** I should see a pop-up window that allows me to choose a subset of identity attributes pre-defined for the data space

4. **Given** I am logged in as Tier 2 Authorization Manager (T2IAA_M) navigating to the Identity Attributes tab of an onboarding template for a given participant type **and** open the pop-up window with the list of available identity attributes **when** I select one or more attributes and I click the add button **then** the popup closes and the attribute is added to the identity attributes of the onboarding template **and** by clicking the SAVE button I confirm the selected attributes

5. **Given** I am logged in as Tier 2 Authorization Manager (T2IAA_M) navigating to the Identity Attributes tab of an onboarding template for a given participant type and seeing the list of the identity attributes for the onboarding template **when** I click on the "trash" icon to delete an identity attributes from the Onboarding Template and click the SAVE button to confirm **then** the deleted identity attributes will be unlinked from the onboarding procedure template and the Identity Attributes section switches in the read only state by showing the "Edit attributes" button

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10175](../README.md) (Epic) Dataspace Governance authority links a subset of identity attributes to an onboarding procedure template |
