> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-19153](../README.md) / **Story-SIMPL-20519**

# SIMPL-20519: [API] Governance Authority Manages eIDAS document on onboarding procedure templates

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-19153](../README.md) (Epic) › **SIMPL-20519** (User story)

## Text

*This User Story covers only the management of the eIDAS document template type. Document templates do not have a dedicated API, but they are created or updated along with the creation and update of the onboarding template they are linked to. More generic ACs related to document types are covered in SIMPL-20532.*\
----\
**As a**\
dataspace governance authority user with the _Tier 2 Authorization Manager (T2IAA_M)_ role, viewing the details of an onboarding procedure template

**I want to**\
via an API, to be able to specify a document template of type *eIDAS* when creating a document template, ensuring that only one such template can exist per onboarding procedure template

**So that**\
I can enable the submission and validation of eIDAS attributes during a participant’s onboarding process

**Acceptance Criteria**

1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** eIDAS login is enabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call *POST /onboardingProcedureTemplates* to create an onboarding procedure template\
   **AND** the request contains a list of documentTemplates\
   **AND** for one document template in the list, I specify a type attribute with value EIDAS\
   **THEN** I receive a 201 OK response, and the document template is created with the EIDAS type
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** eIDAS login is enabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call *POST /onboardingProcedureTemplates* to create an onboarding procedure template\
   **AND** the request contains a list of document templates\
   **AND** for more than one documentTemplate in the *documentTemplates* list, I specify a type attribute with value EIDAS\
   **THEN** I receive a 409 Conflict response, and the error message is "There can be only one document template of type eIDAS for each onboarding template"
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** eIDAS login is enabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call PUT */onboardingProcedureTemplates/{onboardingProcedureTemplateID}* to update an onboarding procedure template\
   **AND** the request contains a list of both existing and new documentTemplates\
   **AND** a document template of type eIDAS has already been created for this onboarding template\
   **AND** for a new document template in the list, I specify a type attribute with value EIDAS\
   **THEN** I receive a 409 Conflict response, and the error message is "There can be only one document template of type eIDAS for each onboarding template"
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** eIDAS login is disabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call *POST /onboardingProcedureTemplates* to create an onboarding procedure template\
   **AND** the request contains a list of both existing and new documentTemplates\
   **AND** for a new document template in the list, I specify a type attribute with value EIDAS\
   **THEN** I receive a 403 Forbidden response with the message "eIDAS Login feature disabled"
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** eIDAS login is enabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call *PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateID}* to update an onboarding procedure template\
   **AND** the request contains a list of both existing and new documentTemplates\
   **AND** for a new document template in the list, I specify a type attribute with value EIDAS\
   **THEN** I receive a 403 Forbidden response with the message "eIDAS Login feature disabled"{}
1. **GIVEN** that I am an IT system administrator, responsible for installing the onboarding backend component ^(1)^\
   **WHEN** I read the deployment documentation of the component\
   **THEN** I see the property to use to enable or disable the eIDAS login feature\
   **AND** I see that by default the feature is disabled

*(1) By default, this feature is disabled on the component, and should be enabled at deployment time by the IT Administrators installing the component into the agent (see last AC).*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-19153](../README.md) (Epic) Governance authority creates eIDAS rules for automated onboarding request validation |
