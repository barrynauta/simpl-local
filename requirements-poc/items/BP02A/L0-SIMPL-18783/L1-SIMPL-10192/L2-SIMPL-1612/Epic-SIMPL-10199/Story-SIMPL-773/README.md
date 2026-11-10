> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-773**

# SIMPL-773: Management API for Identity Attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-773** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority\
**I want to** via an API, use a CRUD interface (CRUD Interface (internal page)) for the Identity Attributes (Identity Attribute (internal page))\
**So that** I can manage the identity attributes

**Acceptance Criteria:**

1. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for reading an identity attribute by ID is available, **when** the user sends a GET request to /attribute/{id} with the required parameter, **then** the response status code is 200 OK, and the response body contains the requested identity attribute's details.

2. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for updating an identity attribute is available, **when** the user sends a PUT request to /attribute/{id} with the required parameters, **then** the response status code is 200 OK, and the response body contains the updated identity attribute's details.

3. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for deleting an identity attribute is available, **when** the user sends a DELETE request to /attribute/{id} with the required parameter, **then** the response status code is 204 No Content.

4. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for creating an identity attribute is available, **when** the user sends a POST request to /attribute with the required parameters, **then** the response status code is 201 Created, and the response body contains the newly created identity attribute's details.

5. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for searching identity attributes is available, **when** the user sends a GET request to /attribute/search with the required parameters, **then** the response status code is 200 OK, and the response body contains a list of identity attributes matching the search criteria.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
