> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-12326](../README.md) / **Story-SIMPL-11767**

# SIMPL-11767: [API] Governance Authority deletes a validation rule

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-12326](../README.md) (Epic) › **SIMPL-11767** (User story)

## Text

*This story covers only the deletion of a rule. The action of disabling a rule is covered in SIMPL-7923 (AC6), SIMPL-10511 (AC6), SIMPL-8500 (AC12).*\
----

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want** via API, to delete a document validation rule\
**So that** I can delete unused rules from an onboarding procedure template.

**Acceptance Criteria**

1. **Given** that I am a dataspace governance authority user with the T2IAA_M role and a validation rule is not used in any existing onboarding request **when** I DELETE /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} **then** the rule is physically deleted

2. **Given** that I am a dataspace governance authority user with the T2IAA_M role and a validation rule is used in at least one onboarding request **when** I DELETE /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} **then** the request is rejected with a 400 bad request{}

3. **Given** that I am a dataspace governance authority user with the T2IAA_M role and a validation rule in the current list of rules is not used in any existing onboarding request **when** I update the list of rules via PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules **and** not include such rule in the updated list **then** the rule is physically deleted

4. **Given** that I am a dataspace governance authority user with the T2IAA_M role and a validation rule in the current list of rules is used in at least one onboarding request **when** I update the list of rules via PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules **and** not include such rule in the updated list **then** the request is rejected with a 400 bad request

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-12326](../README.md) (Epic) Governance authority deletes a validation rule |
