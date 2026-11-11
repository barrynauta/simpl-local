> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-10508](../README.md) / **Story-SIMPL-8499**

# SIMPL-8499: [API] Governance Authority manages composite validation rules on onboarding procedure template

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-10508](../README.md) (Epic) › **SIMPL-8499** (User story)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want to** via API, manage the list of COMPOSITE validation rules for each onboarding procedure template\
**So that** I can define a hierarchical tree structure combining content check and presence check rules to automate the validation of a document's content. This structure also supports users with the NOTARY role during the approval or rejection process of onboarding requests.

**Acceptance Criteria**

1. **Given** that I am a dataspace governance authority user with the T2IAA_M role **when** I GET /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules **then** I want to see all the composite rules specified for the template

2. **Given** that I am a dataspace governance authority user with the T2IAA_M role wanting to create a COMPOSITE rule **when** I POST /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules with ruleType COMPOSITE with all the required parameters (including at least two child rules) **then** I want to receive a 201 Created response and the rule is created

3. **Given** that I am a dataspace governance authority user with the T2IAA_M role wanting to update a COMPOSITE rule **when** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with all the required parameters (including at least two child rules){} **then** I want to receive a 200 OK response and the rule is updated{}

4. **Given** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update a COMPOSITE rule **when** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with invalid parameters **then** I want to receive a 400 BAD REQUEST and the update is rejected

5. **Given** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update or create a COMPOSITE rule **when** I create (POST) or update (PUT) a COMPOSITE rule with less than two rules **then** I want to receive a 400 BAD REQUEST and the update is rejected

6. **Given** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update a COMPOSITE rule **when** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with a new rule type different from COMPOSITE **then** I want to receive a 400 bad request and the update is rejected{}

7. **Given** that I am a dataspace governance user WITHOUT the T2IAA_M role, **when** I call any of the GET, POST, PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules APIs **then** I want to receive a 403 Forbidden and the operation is rejected

APIs for onboarding validation rules are defined in (internal page).

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10508](../README.md) (Epic) Governance Authority creates composite rules with custom logical conditions for document validation |
