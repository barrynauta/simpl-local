> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-10507](../README.md) / **Story-SIMPL-10510**

# SIMPL-10510: [API] Governance Authority manages presence check validation rules on onboarding procedure template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-10507](../README.md) (Epic) › **SIMPL-10510** (User story)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want to** via API, manage the list of PRESENCE CHECK validation rules for each onboarding procedure template\
**So that** I can define the parameters to support the automatic check the presence or the absence of a document and support the approval or rejection process of the users with NOTARY role.

**Acceptance Criteria**

1. **Given** that I am a dataspace governance authority user with the T2IAA_M role **when** I GET /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules (see [API Definition](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/api_SIMPL-10173_validation_rules/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L27)) **then** I want to see all the content rules specified for the template

2. **Given** that I am a dataspace governance authority user with the T2IAA_M role wanting to create a presence check rule **when** I POST /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules with ruleType DOCUMENT_PRESENCE_CHECK with all the required parameters (see [API Definition](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/api_SIMPL-10173_validation_rules/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L62)) **then** I want to receive a 201 Created response and the rule is created

3. **Given** that I am a dataspace governance authority user with the T2IAA_M role wanting to update a PRESENCE CHECK rule **when** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with all the required parameters (see [API Definition](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/api_SIMPL-10173_validation_rules/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L170)) **then** I want to receive a 200 OK response and the rule is updated

4. **Given** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update a PRESENCE CHECK rule **when** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with invalid parameters (see [API Definition](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/api_SIMPL-10173_validation_rules/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L170)) **then** I want to receive a 400 bad request and the update is rejected

5. **Given** that I am a dataspace governance authority user with the T2IAA_M role, wanting to update a PRESENCE CHECK rule **when** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} with a new ruleType different from PRESENCE CHECK (see [API Definition](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/api_SIMPL-10173_validation_rules/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L170)) **then** I want to receive a 400 bad request and the update is rejected{}

6. **Given** that I am a dataspace governance user WITHOUT the T2IAA_M role, **when** I to call any of the GET, POST, PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules APIs and receive a 403 Forbidden

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10507](../README.md) (Epic) Governance authority creates presence check rules to check if a document is uploaded |
