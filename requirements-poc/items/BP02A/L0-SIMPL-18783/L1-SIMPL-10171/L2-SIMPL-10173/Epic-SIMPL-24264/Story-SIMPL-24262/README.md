> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-24264](../README.md) / **Story-SIMPL-24262**

# SIMPL-24262: [UI][IMPROVEMENT] Allow the deletion of a validation rule even if used in an existing onboarding request

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-24264](../README.md) (Epic) › **SIMPL-24262** (User story)

## Text

*This story improves SIMPL-11766 by allowing the deletion of a validation rule which is currently used by at least one onboarding request. Even if deleted existing onboarding requests continue to use a snapshot of the rule at the time it was applied (see also SIMPL-24263).*\
----\
**As A**

dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing the rule sections of an onboarding procedure template

**I want**

via UI, to delete an onboarding validation rule

**So** **that**\
I can delete unused rules or disable those that are no longer relevant for future onboarding procedures

**Acceptance Criteria**

1. [DEPRECATES AC3 of SIMPL-11766]\
   GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) *\
   WHEN* I navigate to the Rules tab of an onboarding procedure template in edit mode *\
   AND* a rule has already been linked to an existing onboarding request *\
   WHEN* I click on the trash icon for that rule, *\
   THEN* a confirmation popup appears asking to confirm the deletion operation\
   AND the rule is deleted upon confirmation

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24264](../README.md) (Epic) Governance authority deletes validation rules used in existing onboarding requests |
