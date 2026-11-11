> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-24264](../README.md) / **Story-SIMPL-24263**

# SIMPL-24263: [API][IMPROVEMENT] Allow the deletion of a validation rule even if used in an existing onboarding request

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-24264](../README.md) (Epic) › **SIMPL-24263** (User story)

## Text

This story improves SIMPL-11767 by allowing the deletion of validation rules\
----

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want** via API, to delete a document validation rule\
**So that** I can delete all the rules from an onboarding procedure template.

**Acceptance Criteria**

1. [DEPRECATES AC2 OF SIMPL-11767]

1. **Given** that I am a Dataspace Governance Authority user with the T2IAA_M role\
   **And** a validation rule is used in at least one onboarding request\
   **When** I DELETE /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules/{validationRuleId} (See [OpenAPI Spec](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/v2.11.0/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=tags#L742))\
   **Then** the rule is physically deleted\
   **And** existing onboarding requests continue to use a snapshot of the rule at the time it was applied

   - [DEPRECATES AC4 OF SIMPL-11767]{}

2. **Given** that I am a Dataspace Governance Authority user with the T2IAA_M role\
   **And** a validation rule in the current list of rules is used in at least one onboarding request\
   **When** I update the list of rules via PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}/validationRules (See [OpenAPI Spec](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/v2.11.0/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=tags#L133))\
   **And** not include such rule in the updated list\
   **Then** the rule is physically deleted\
   **And** existing onboarding requests continue to use a snapshot of the rule at the time it was applied

   - [DOCUMENTATION] The OpenAPI endpoint documentation and error codes of the above endpoints is updated according to the new behaviour (e.g. remove "If a rule already associated with the template is not provided, an error is returned)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24264](../README.md) (Epic) Governance authority deletes validation rules used in existing onboarding requests |
