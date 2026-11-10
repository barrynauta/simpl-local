> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-12326](../README.md) / **Story-SIMPL-11766**

# SIMPL-11766: [UI] Governance Authority deletes a validation rule

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-12326](../README.md) (Epic) › **SIMPL-11766** (User story)

## Text

**As A**

dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing the rule sections of an onboarding procedure template

**I want**

via UI, to delete or disable document validation rules

**So** **that**\
I can delete unused rules or disable those that are no longer relevant for future onboarding procedures

**Acceptance Criteria**

1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) AND I navigate to the read-only screen of an onboarding procedure template WHEN the Rules tab is opened, THEN I should see a toggle button that allows me to hide or show the disabled rules, along with a count of the disabled rules**
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) WHEN I navigate to the Rules tab of an onboarding procedure template AND I click on the "Edit Rules" button, THEN I should see a trash icon next to each validation rule**
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) WHEN I navigate to the Rules tab of an onboarding procedure template in edit mode AND a rule has already been linked to an existing onboarding request WHEN I hover or click over the trash icon for that rule, THEN a message is displayed stating that the rule cannot be deleted because it is linked to an existing onboarding request AND the trash icon is shown as disabled AND the user is invited to disable the rule if needed**
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) WHEN I navigate to the Rules tab of an onboarding procedure template in edit mode AND a rule has never been linked to an onboarding request linked to the template AND I click on the trash icon for that rule, THEN a confirmation popup appears asking to confirm the deletion operation**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-12326](../README.md) (Epic) Governance authority deletes a validation rule |
