> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10174](../../README.md) / [Epic-SIMPL-10175](../README.md) / **Story-SIMPL-7928**

# SIMPL-7928: [API] Governance Authority selects Security Identity Attributes for an Onboarding Procedure Template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10174](../../README.md) (L2) › [SIMPL-10175](../README.md) (Epic) › **SIMPL-7928** (User story)

## Text

**As a**\
dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, managing the onboarding procedure templates, which are linked to participant types

**I want**

via API, for each onboarding procedure template, to select identity attributes relevant to the type of participant

**So that**\
when a participant applies by selecting a participant type, the corresponding identity attributes are suggested to the onboarding request approvers  with NOTARY role.

**Acceptance Criteria**

1. **GIVEN** that I am a dataspace governance authority user with the Tier 2 authorization manager (T2IAA_M) role\
   **AND** the onboarding procedure template has just been created\
   **WHEN** I send a GET request to /onboardingProcedureTemplates/{id}/identityAttributes\
   **THEN** I receive a 200 OK response\
   **AND** the response body contains an empty list.
1. **GIVEN** that I am a dataspace governance authority user with the Tier 2 authorization manager (T2IAA_M) role\
   **AND** the onboarding procedure template exists and is already linked to some identity attributes\
   **WHEN** I send a non-empty list of identity attributes via PUT /onboardingProcedureTemplates/{id}/identityAttributes\
   **THEN** I receive a 204 No Content response confirming the operation has completed successfully\
   **AND** only the identity attributes provided in the request are now linked to the onboarding procedure template\
   **AND** any previously linked identity attributes not included in the request are unlinked.
1. **GIVEN** that I am a dataspace governance authority user with the Tier 2 authorisation manager (T2IAA_M) role\
   **AND** the onboarding procedure template exists\
   **WHEN** I send a PUT /onboardingProcedureTemplates/{id}/identityAttributes request containing one or more invalid or non-existing identity attribute codes\
   **THEN** I receive a 400 Bad Request response\
   **AND** the response body follows the problem-details structure, clearly describing which invalid parameter or value caused the error.
1. **GIVEN** that I am a dataspace governance authority user with the Tier 2 authorization manager (T2IAA_M) role\
   **WHEN** I send a PUT /onboardingProcedureTemplates/{id}/identityAttributes request\
   **AND** the request body structure or format is invalid (e.g., missing required fields, wrong data types, or malformed JSON)\
   **THEN** I receive a 400 Bad Request response\
   **AND** the response body follows the problem-details structure, indicating the invalid parameter and describing the issue in detail.
1. **GIVEN** that I am a dataspace governance authority user without the Tier 2 authorization manager (T2IAA_M) role\
   **WHEN** I send a PUT /onboardingProcedureTemplates/{id}/identityAttributes request\
   **THEN** I receive a 403 Forbidden response\
   **AND** the problem-details response indicates that I do not have the required permissions to perform this operation.

See the linked [API DEFINITION] task for a detailed description of the mentioned APIs.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10175](../README.md) (Epic) Dataspace Governance authority links a subset of identity attributes to an onboarding procedure template |
