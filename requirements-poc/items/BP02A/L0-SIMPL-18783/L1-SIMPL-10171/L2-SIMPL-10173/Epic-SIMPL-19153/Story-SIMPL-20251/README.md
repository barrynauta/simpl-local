> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-19153](../README.md) / **Story-SIMPL-20251**

# SIMPL-20251: [API] Governance Authority manages eIDAS rules on onboarding procedure templates

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-19153](../README.md) (Epic) › **SIMPL-20251** (User story)

## Text

**As A**

dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role

**I want to**

via API, manage the list of eIDAS validation rules for each onboarding procedure template

**So** **that**

I can automate the evaluation of eIDAS attributes during the onboarding of a participant, and support the users with NOTARY role during the approval or rejection process of onboarding requests

Acceptance Criteria{}

1. **GIVEN** that I am a Dataspace Governance Authority user with the T2IAA_M role wanting to create an eIDAS rule\
   **AND** eIDAS login is enabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call POST /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules with ruleType EIDAS\
   **AND** the request body contains (new rule parameters linked only to eIDAS rules are underlined):\
   +personType (required, either NATURAL_PERSON or LEGAL_PERSON)+\
   +eIDASAttribute (required, code of the eIDAS attribute)+\
   +condition (required, either IS_PRESENT or CONTAINS)+\
   +conditionValue (required if the contition field is equal to CONTAINS, the value that should be checked into the eidas attribute value)+\
   ruleType: +(EIDAS)+\
   name (required, string, max 255 characters)\
   description (required, string, max 500 characters\
   documentTemplate (required, reference to the id of the eIDAS document template)\
   ValidSince (required, date)\
   ValidTo (not required, date)\
   required (required, boolean indicating if the rule evaluation must complete with SUCCESS to be submitted for manual or auto approval)\
   forApproval (required, boolean indicating that if the rule evaluation completes with SUCCESS, the onboarding request is automatically approved)\
   active(boolean indicating if the rule is active)\
   **THEN** I receive a 201 Created response\
   **AND** the response body contains the details of the created rule\
   **AND** the rule is added to the template
1. **GIVEN** that I am a Dataspace Governance Authority user with the T2IAA_M role wanting to update an eIDAS rule\
   **WHEN** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId}\
   **AND** the rule with the given id (validationRuleId) exists with type eIDAS\
   **AND** the request body contains valid eIDAS rule parameters (see AC1)\
   **AND** the rule type in the request body is eIDAS\
   **THEN** I want to receive a 200 OK response, and the rule is updated
1. **GIVEN** that I am a Dataspace Governance Authority user with the T2IAA_M role wanting to update an eIDAS rule\
   **WHEN** I PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId}\
   **AND** the request contains valid eIDAS rule parameters (see AC1)\
   **AND** the rule with the given id (validationRuleId) exists with type eIDAS\
   **AND** the request body contains a rule type different from eIDAS\
   **THEN** I receive a 400 Bad Request\
   **AND** the error contains the message "The rule type cannot be modified"
1. **GIVEN** that I am a dataspace governance user WITHOUT the T2IAA_M role,\
   **WHEN** I call any of the GET, POST, PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules APIs\
   **THEN** I receive a 403 Forbidden error
1. **GIVEN** that I am not authenticated\
   **WHEN** I call any of the GET, POST, PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules* APIs\
   **THEN** I receive a 401 Unauthorized
1. **GIVEN** that I am a Dataspace Governance Authority user with the T2IAA_M role wanting to create an eIDAS rule\
   **AND** eIDAS login is disabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call POST /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules with ruleType EIDAS\
   **THEN** I receive a 403 Forbidden response with the message "eIDAS Login feature disabled"
1. **GIVEN** that I am an IT system administrator, responsible for installing the onboarding backend component ^(1)^\
   **WHEN** I read the deployment documentation of the component\
   **THEN** I see the property to use to enable or disable the eIDAS login feature\
   **AND** I see that by default the feature is disabled

*(1) By default, this feature is disabled on the component, and should be enabled at deployment time by the IT Administrators installing the component into the agent (see the last AC).*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-19153](../README.md) (Epic) Governance authority creates eIDAS rules for automated onboarding request validation |
