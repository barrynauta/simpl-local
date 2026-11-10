> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-5115**

# SIMPL-5115: [API] Management API for mime types

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-5115** (User story)

## Text

**As a** dataspace governance authority user with the *Tier 2 authorization manager* role in the Dataspace Governance Authority\
**I want to** via an API, use a CRUD interface (CRUD Interface) (internal page)for the mime types needed in the definition of the onboarding procedure template (Onboarding procedure template (internal page)).\
**So that** I can manage the mime types definition

**Acceptance Criteria:**

1. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for reading a mime type by its description is available, **when** the user sends a GET request to /mime-type/{id} with the required parameter, **then** the response status code is 200 OK, and the response body contains the requested mime type's details.

2. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for updating a mime type is available, **when** the user sends a PUT request to /mime-type/{id} with the required parameters, **then** the response status code is 200 OK, and the response body contains the updated mime type's details.

3. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for deleting a mime type is available, **when** the user sends a DELETE request to /mime-type/{id} with the required parameter, **then** the response status code is 204 No Content, and the mime type is removed.

4. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for creating a mime type is available, **when** the user sends a POST request to /mime-type with the required parameters, **then** the response status code is 201 Created, and the response body contains the newly created mime type's details.

5. **Given** that the user is logged in with the T2IAA_M role, and the API endpoint for retrieving all mime types is available, **when** the user sends a GET request to /mime-type, **then** the response status code is 200 OK, and the response body contains a list of all mime types.

6. **Given** that the user is logged in without the T2IAA_M role, and the API endpoint is available, **when** the user sends a request (GET, POST, PUT, or DELETE) to any of the /mime-type endpoints, **then** the response status code is 403 Forbidden, and the response body contains an error message indicating insufficient permissions.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
| **Also under** | [SIMPL-2181](../../Epic-SIMPL-2181/README.md) (Epic) API for onboarding Process Governance Framework - mandatory resources/documents |
