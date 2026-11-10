> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10173](../README.md) / **Epic-SIMPL-8337**

# SIMPL-8337: Governance authority creates content check rules for automated document validation

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10173](../README.md) (L2) › **SIMPL-8337** (Epic)

## Text

**As a**

Dataspace governance authority,

**I want to**

define a content check rule for an onboarding procedure template by adding a URL to an external validation service

**So that**\
I can validate document content by integrating custom logic through external services.

**Acceptance Criteria**

1. **GIVEN that I am defining an onboarding procedure template** **WHEN I add a rule of type** **CONTENT CHECK** **THEN I should be able to insert a name, a description,** a validity interval for the rule, a flag to state if the rule is enabled or not, and select one of the documents defined for the onboarding procedure template{}
1. **GIVEN** that I am defining an onboarding procedure template{} ** **WHEN I add a rule of type** **CONTENT CHECK** **THEN I should able to flag an "auto-approved" field that states that if the rule evaluator evaluates the rule positively THEN the onboarding request can be automatically approved**
1. **GIVEN** **that I am defining an onboarding procedure template** **WHEN I add a rule of type** **CONTENT CHECK** **THEN I should able to flag a "required" field that states that the rule evaluator evaluates the rule negatively, then the onboarding request must be automatically temporarily rejected and resubmitted to the applicant**
1. **GIVEN** **that I am defining an onboarding procedure template** **WHEN  I add a rule of type CONTENT CHECK THEN I should be able to insert a URL parameter pointing to an external validation engine used to validate the document content that implements a well-defined API contract**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10173](../README.md) (L2) Configure a ruleset for the automatic validation of onboarding request documents |
| **Children** | [SIMPL-6364](Story-SIMPL-6364/README.md) (User story) [API] Governance Authority manages content check validation rules on onboarding procedure template<br>[SIMPL-7923](Story-SIMPL-7923/README.md) (User story) [UI] Governance Authority manages content check validation rules on onboarding procedure template<br>[SIMPL-18944](Story-SIMPL-18944/README.md) (User story) [API][IMPROVEMENT] Allow users to create and update invalid content check onboarding validation rules when disabled<br>[SIMPL-18945](Story-SIMPL-18945/README.md) (User story) [UI][IMPROVEMENT] Allow users to create and update invalid content check onboarding validation rules when disabled |
