> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-10508](../README.md) / **Story-SIMPL-18913**

# SIMPL-18913: [API][IMPROVEMENT] Allow users to create and update invalid composite onboarding validation rules when disabled

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-10508](../README.md) (Epic) › **SIMPL-18913** (User story)

## Text

*This story invalidates AC4 and AC5 of SIMPL-8499.* *It has been created to solve SIMPL-16360.*

**As a**\
Dataspace Governance Authority end user with the T2IAA_M (Tier 2 authorization manager) role

**I want to**\
via API, save invalid COMPOSITE onboarding validation rules in a disabled state

**So that**\
I can draft validation rules without being blocked, while ensuring only valid rules can be enabled

**Acceptance Criteria**

1. **GIVEN** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update a COMPOSITE rule **\
   **WHEN** I POST /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/ with invalid parameters\
   **AND** the rule is marked as disabled\
   **THEN** I want to receive a 200 Ok response\
   **AND** the draft disabled rule is saved
1. **GIVEN** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update a COMPOSITE rule **\
   **WHEN** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with invalid parameters\
   **AND** the rule is marked as disabled\
   **THEN** I want to receive a 200 OK response\
   **AND** the draft disabled rule is saved
1. **GIVEN** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update or create a COMPOSITE rule **\
   **WHEN** I create (POST) or update (PUT) a COMPOSITE rule with less than two rules\
   **AND** the rule is disabled\
   **THEN** I want to receive a 200 Ok response\
   **AND** the draft disabled rule is saved{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10508](../README.md) (Epic) Governance Authority creates composite rules with custom logical conditions for document validation |
