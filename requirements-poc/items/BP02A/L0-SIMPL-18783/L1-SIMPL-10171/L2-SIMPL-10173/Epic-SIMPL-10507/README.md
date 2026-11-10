> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10173](../README.md) / **Epic-SIMPL-10507**

# SIMPL-10507: Governance authority creates presence check rules to check if a document is uploaded

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10173](../README.md) (L2) › **SIMPL-10507** (Epic)

## Text

**As a** Dataspace governance authority,\
**I want to** define a presence check rule for an onboarding procedure template\
**So that** I can validate onboarding procedure templates based on the presence or absence of a document

**Acceptance Criteria**

1. **Given** that I am defining an onboarding procedure template **when** I add a rule of type PRESENCE CHECK **then** I should be able to insert a name, a description, a validity interval for the rule, a flag to state if the rule is enabled or not, and select one of the documents defined for the onboarding procedure template{}

2. **Given** that I am defining an onboarding procedure template **when** I add a rule of type PRESENCE CHECK **then** I should able to flag an "auto-approved" field that states that if the rule evaluator evaluates the rule positively **then** the onboarding request can be automatically approved

3. **Given** that I am defining an onboarding procedure template **when** I add a rule of type PRESENCE CHECK **then** I should able to flag a "required" field that states that the rule evaluator evaluates the rule negatively, then the onboarding request must be automatically temporarily rejected and resubmitted to the applicant

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10173](../README.md) (L2) Configure a ruleset for the automatic validation of onboarding request documents |
| **Children** | [SIMPL-10510](Story-SIMPL-10510/README.md) (User story) [API] Governance Authority manages presence check validation rules on onboarding procedure template<br>[SIMPL-10511](Story-SIMPL-10511/README.md) (User story) [UI] Governance Authority manages presence check validation rules on onboarding procedure template<br>[SIMPL-18946](Story-SIMPL-18946/README.md) (User story) [API][IMPROVEMENT] Allow users to create and update invalid presence check onboarding validation rules when disabled<br>[SIMPL-18947](Story-SIMPL-18947/README.md) (User story) [UI][IMPROVEMENT] Allow users to create and update invalid presence check onboarding validation rules when disabled |
