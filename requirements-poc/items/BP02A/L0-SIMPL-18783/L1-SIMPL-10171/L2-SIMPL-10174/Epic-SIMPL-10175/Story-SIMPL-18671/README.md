> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10174](../../README.md) / [Epic-SIMPL-10175](../README.md) / **Story-SIMPL-18671**

# SIMPL-18671: [UI] [IMPROVEMENT] Participant detail page - Assign attributes

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10174](../../README.md) (L2) › [SIMPL-10175](../README.md) (Epic) › **SIMPL-18671** (User story)

## Text

*Epic is SIMPL-10590, not updated due to the lack of permissions. PSO to update it during review.*

*This story improves the process of assigning identity attributes to participants on the participant details page. It modifies the behaviour described in SIMPL-3905 and aligns with the behaviour described for onboarding procedure templates SIMPL-8227, ensuring consistency. See the attached video to see how the page works on the onboarding procedure templates, the same behaviour will apply for the participant details page.*

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Participants Management page, viewing a participant's detail information page\
**I want to** via UI, add Identity Attributes to the participant\
**So that** I can manage the assignment of Identity Attributes to participants

**Acceptance Criteria**

1. **Given** I am logged in as Tier 2 identity attributes manager (IATTR_M), navigating to the details page of a participant **when** I click the "Edit Attributes" button **then** I should see an empty list of identity attributes **and** an "Add Attribute" button that allows me to add them to the participant

2. **Given** I am logged in as Tier 2 identity attributes manager (IATTR_M), navigating to the details page of a participant **when** I click the "Add Attribute" button **then** I should see a pop-up window that allows me to choose a subset of identity attributes pre-defined for the data space

3. **Given** I am logged in as Tier 2 identity attributes manager (IATTR_M), navigating to the details page of a participant **and** open the pop-up window with the list of available identity attributes **when** I select one or more attributes and I click the add button **then** the popup closes and the attribute is added to the identity attributes of the participant **and** by clicking the SAVE button I confirm the selected attributes

4. **Given** I am logged in as Tier 2 identity attributes manager (IATTR_M) navigating to the details page of a participant **and** seeing the list of the identity attributes for the participant **when** I click on the "trash" icon to delete an identity attribute and click the SAVE button to confirm **then** the deleted identity attributes will be unlinked from the participant and the Identity Attributes section switches in the read only state by showing the "Edit attributes" button

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10175](../README.md) (Epic) Dataspace Governance authority links a subset of identity attributes to an onboarding procedure template |
