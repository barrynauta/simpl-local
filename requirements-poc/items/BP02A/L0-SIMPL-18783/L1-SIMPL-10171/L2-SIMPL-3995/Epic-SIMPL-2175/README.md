> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-3995](../README.md) / **Epic-SIMPL-2175**

# SIMPL-2175: Onboarding Process Governance Framework - resources/documents

**Epic (deliverable, testable feature)**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-3995](../README.md) (L2) › **SIMPL-2175** (Epic)

## Text

**As a** dataspace governance authority,\
**I want** a UI or an API, to define resources/documentation that an applicant dataspace participant can provide to start the onboarding procedure,\
**So that** those resources/documentation needed by the onboarding process are clearly defined.

**Acceptance Criteria**

1. **Given** a Dataspace Governance Authority user with the appropriate role, **when** they create a new onboarding procedure template, **then** they can specify required documents/resources per participant type, and the template is saved successfully.

2. **Given** a Dataspace Governance Authority user with the appropriate role, **when** they create a new onboarding procedure template documents/resources, **then** they can specify if the document/resource is mandatory or not

3. **Given** an authorized user in the Dataspace Governance Authority, **when** they access the onboarding procedure templates summary page, **then** they see a list of all templates with relevant metadata and can navigate to manage them.

4. **Given** an API client with the correct permissions, **when** they request the list of supported mime types, **then** the API returns a structured response containing all allowed mime types.

5. **Given** an authorized API client, **when** they create, update, retrieve, or delete an onboarding procedure template, **then** the API enforces proper validation and executes the request successfully.

6. **Given** a user without the required role, **when** they attempt to access the onboarding administration dashboard, **then** they receive an "Access Denied" message and are prevented from proceeding.

7. **Given** a user with the required role, **when** they access the onboarding administration dashboard, **then** they can view and manage onboarding procedure templates and related configurations.

8. **Given** a valid request to manage an onboarding procedure template, **when** an API request is made to create, modify, or delete a template, **then** the system correctly validates, processes, and persists the changes.

9. **Given** an API call to retrieve available mime types, **when** the request is processed, **then** the API returns all supported file formats for onboarding documents.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-3995](../README.md) (L2) Define the onboarding process documentation |
| **Children** | [SIMPL-246](Story-SIMPL-246/README.md) (User story) [UI] Onboarding administration dashboard - access granted<br>[SIMPL-255](Story-SIMPL-255/README.md) (User story) [UI] Onboarding administration dashboard - access denied<br>[SIMPL-4746](Story-SIMPL-4746/README.md) (User story) [UI] Dataspace Governance Authority onboarding procedure templates summary page<br>[SIMPL-4747](Story-SIMPL-4747/README.md) (User story) [UI] Dataspace Governance Authority creates an onboarding procedure template for a given partipant type<br>[SIMPL-5114](Story-SIMPL-5114/README.md) (User story) [API] Management API for Onboarding procedure template<br>[SIMPL-5115](Story-SIMPL-5115/README.md) (User story) [API] Management API for mime types<br>[SIMPL-8410](Story-SIMPL-8410/README.md) (User story) [API] Add the "name" attribute to document templates<br>[SIMPL-8887](Story-SIMPL-8887/README.md) (User story) [UI] Add the "name" attribute to document templates<br>[SIMPL-18234](Story-SIMPL-18234/README.md) (User story) [UI] [IMPROVEMENT] Governance Authority fills application instructions on an onboarding procedure template<br>[SIMPL-18235](Story-SIMPL-18235/README.md) (User story) [API] [IMPROVEMENT] Governance Authority fills application instructions on an onboarding procedure template<br>[SIMPL-20517](Story-SIMPL-20517/README.md) (User story) [UI][IMPROVEMENT] Governance Authority Manages document template types on onboarding procedure templates<br>[SIMPL-20532](Story-SIMPL-20532/README.md) (User story) [API][IMPROVEMENT] Governance Authority Manages different document template types on onboarding procedure templates<br>[SIMPL-22405](Story-SIMPL-22405/README.md) (User story) [API][IMPROVEMENT] Dataspace Governance Authority updates onboarding expirationTimeframe and credentialId |
