> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-18235**

# SIMPL-18235: [API] [IMPROVEMENT] Governance Authority fills application instructions on an onboarding procedure template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-18235** (User story)

## Text

*Note for the reviewers: Epic is SIMPL-2175. Not inserted due to lack of the backlog permissions.*

*This story is an improvement of (internal page) and covers a new field "application instructions" to be added to the fields requested upon onboarding procedure template creation and update.*

1. **Given** the new eIDAS rule that is about to be introduced (see (internal page)) there is the need to help users to understand which documents to upload or which actions to take during their onboarding. This new field, aims to help the GA to outline these instructions and make them visibile to the applicants._\
   ----

**As a** Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want to** via API, be able to add a descriptive, human-readable "application instructions"\
**So that** Applicants can read these instructions during the onboarding process and understand which documents to upload and which rules to follow.

**Acceptance Criteria**

2. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I send a POST /onboardingProcedureTemplates\
   **And** the body contains a non-empty field *applicationInstructions*\
   **Then** the onboarding procedure template is saved with the new information

3. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I send a POST /onboardingProcedureTemplates\
   **And** the body contains an empty *applicationInstructions*\
   **Then** the onboarding procedure template is saved and the applicationInstructions property is empty

4. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I send a PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}\
   **And** the body contains a non-empty field *applicationInstructions*\
   **Then** the onboarding procedure template is updated with the new information

5. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I send a PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}\
   **And** the body contains an empty *applicationInstructions*\
   **Then** the onboarding procedure template is saved and the applicationInstructions property is emptied

6. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I send a PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}\
   **And** I provide an invalid request body with applicationInstructions longer than 2000 characters\
   **Then** the API returns HTTP 400 Bad Request

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
