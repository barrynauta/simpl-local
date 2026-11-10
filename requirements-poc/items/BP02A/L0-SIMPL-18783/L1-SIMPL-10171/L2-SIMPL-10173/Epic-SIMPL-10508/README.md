> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10173](../README.md) / **Epic-SIMPL-10508**

# SIMPL-10508: Governance Authority creates composite rules with custom logical conditions for document validation

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10173](../README.md) (L2) › **SIMPL-10508** (Epic)

## Text

**As a**

Dataspace governance authority,

**I want to**

define a composite rule combining content check and presence check rules or other composite rules. In such combination. I want to be able to indicate that all must be valid or that at least one must be valid.

**So that**

I can define custom custom logical conditions and combine multiple validation checks

**Acceptance Criteria**

1. **GIVEN that I am defining an** **onboarding procedure template** **WHEN I add a rule of type** **COMPOSITE** **THEN I should be able to insert a name, a description,** **a validity interval for the rule, a flag to state if the rule is enabled or not**
1. **GIVEN that I am defining an onboarding procedure template WHEN I add a rule of type** **COMPOSITE CHECK** **THEN I should able to define an "auto-approved" flag that states that if the rule evaluator evaluates the rule positively THEN the onboarding request can be automatically approved**
1. **GIVEN that I am defining an onboarding procedure template WHEN I add a rule of type** **COMPOSITE** **THEN I should able to flag a "required" field that states that the rule evaluator evaluates the rule negatively, then the onboarding request must be automatically temporarily rejected and resubmitted to the applicant**
1. **GIVEN that I am defining an onboarding procedure template** **WHEN I add a rule of type COMPOSITE THEN I should be able to create an unlimited number of children rules of type CONTENT CHECK, PRESENCE CHECK or COMPOSITE**
1. **GIVEN that I am defining an onboarding procedure template** **WHEN I add a rule of type COMPOSITE and its children rule THEN I should NOT be able to create flag the children rules with the "auto-approved" and "required" flags**
1. **GIVEN that I am defining an onboarding procedure template** WHEN clicking the button to create a rule of type COMPOSITE THEN I should be able to choose a logical strategy ("At least one must be valid" or "All must be valid") to specify whether the composite rule is considered successful when either one or all of its child rules are evaluated successfully.{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10173](../README.md) (L2) Configure a ruleset for the automatic validation of onboarding request documents |
| **Children** | [SIMPL-8499](Story-SIMPL-8499/README.md) (User story) [API] Governance Authority manages composite validation rules on onboarding procedure template<br>[SIMPL-8500](Story-SIMPL-8500/README.md) (User story) [UI] Governance Authority manages composite validation rules on onboarding procedure template<br>[SIMPL-18913](Story-SIMPL-18913/README.md) (User story) [API][IMPROVEMENT] Allow users to create and update invalid composite onboarding validation rules when disabled<br>[SIMPL-18914](Story-SIMPL-18914/README.md) (User story) [UI][IMPROVEMENT] Allow users to create and update invalid composite onboarding validation rules when disabled |
