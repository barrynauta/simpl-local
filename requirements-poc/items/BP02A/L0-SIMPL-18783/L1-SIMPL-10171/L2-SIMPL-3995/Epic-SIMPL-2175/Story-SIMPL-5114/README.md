> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-5114**

# SIMPL-5114: [API] Management API for Onboarding procedure template

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-5114** (User story)

## Text

**As a** dataspace governance authority user with the *Tier 2 authorization manager* role in the Dataspace Governance Authority\
**I want to** via an API, use a CRUD interface (CRUD Interface (internal page)] for the Onboarding procedure template (Onboarding procedure template (internal page)).\
**So that** I can manage the Onboarding procedure template

**Acceptance Criteria:**

1. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for retrieving an onboarding procedure template by ID is available, **when** the user sends a GET request to /onboardingProcedureTemplates/{onboardingProcedureTemplateId} with the required ID, **then** the response status code is 200 OK, and the response body contains the onboarding procedure template's details.

2. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for updating an onboarding procedure template is available, **when** the user sends a PUT request to /onboardingProcedureTemplates/{onboardingProcedureTemplateId} with the required payload, **then** the response status code is 200 OK, and the response body contains the updated onboarding procedure template’s details.

3. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for deleting an onboarding procedure template is available, **when** the user sends a DELETE request to /onboardingProcedureTemplates/{onboardingProcedureTemplateId}, **then** the response status code is 204 No Content, and the specified onboarding procedure template is removed.

4. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for creating an onboarding procedure template is available, **when** the user sends a POST request to /onboardingProcedureTemplates with the required parameters, **then** the response status code is 201 Created, and the response body contains the newly created onboarding procedure template's details.

5. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for retrieving all onboarding procedure templates is available,\
   **When** the user sends a GET request to /onboardingProcedureTemplates,\
   **Then** the response status code is 200 OK, and the response body contains a list of all onboarding procedure templates with their respective onboardingProcedureTemplateId.

6. **Given** that the user is logged in without the T2IAA_M role, **when** the user attempts to send any request (GET, POST, PUT, or DELETE) to /onboardingProcedureTemplates endpoints, **then** the response status code is 403 Forbidden, and the response body contains an error message indicating insufficient permissions.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
