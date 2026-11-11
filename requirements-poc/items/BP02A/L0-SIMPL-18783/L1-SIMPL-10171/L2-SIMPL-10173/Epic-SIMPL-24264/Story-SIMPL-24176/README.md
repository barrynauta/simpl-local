> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-24264](../README.md) / **Story-SIMPL-24176**

# SIMPL-24176: [API][IMPROVEMENT] Prevent the deletion of a document template linked to an onboarding validation rule

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-24264](../README.md) (Epic) › **SIMPL-24176** (User story)

## Text

*This user story improves SIMPL-5114 by introducing an additional validation constraint.*

*Currently, the Onboarding APIs do not have an API to delete a document template from an onboarding procedure template. Any update to an onboarding procedure template happens via a call to the PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}.*\
----

**As a** dataspace governance authority user with the *Tier 2 authorization manager* role in the Dataspace Governance Authority\
**I want** the API to update an onboarding procedure template to throw an error when I try to delete a document linked to an onboarding validation rule\
**So that** The template remains consistent by avoiding to have rules that do not reference a document template

**Acceptance Criteria**

1. **Given** that I am authenticated with the T2IAA_M (Tier 2 authorization manager) role\
   **And** the onboarding procedure template contains a document template\
   **And** the onboarding procedure template contains a rule that links to the document template\
   **When** I call PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId} to update the onboarding template content\
   **And** in the body of the request, the document template is not present\
   **Then** I receive a 422 error with a message stating that the document cannot be deleted because it is used by an onboarding validation rule

**OpenAPI definition update:** [https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/SIMPL-24176/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L1055](https://code.europa.eu/simpl/simpl-open/development/iaa/common/-/blob/feature/SIMPL-24176/simpl-api-iaa/src/main/resources/static/openapi/onboarding-v1.yaml?ref_type=heads#L1055)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24264](../README.md) (Epic) Governance authority deletes validation rules used in existing onboarding requests |
