> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-24264](../README.md) / **Story-SIMPL-18911**

# SIMPL-18911: [UI][IMPROVEMENT] Prevent the deletion of a document template linked to an onboarding validation rule

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-24264](../README.md) (Epic) › **SIMPL-18911** (User story)

## Text

**As a**\
Dataspace Governance Authority end user with the T2IAA_M (Tier 2 authorization manager) role, viewing the documents tab of an onboarding procedure template

**I want**\
the UI to prevent me from deleting a document template if it is used by an onboarding validation rule

**So that**\
I can make an informed decision by first deleting or modifying the rules before deleting the linked document

**Acceptance Criteria**

1. **GIVEN** that I am authenticated with the T2IAA_M (Tier 2 authorization manager) role\
   **AND** a document template of an onboarding procedure template is linked to a validation rule\
   **WHEN** I try to delete a document template from the onboarding procedure\
   **THEN** I see a message stating that the document cannot be deleted because it is linked to one or more validation rules

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24264](../README.md) (Epic) Governance authority deletes validation rules used in existing onboarding requests |
