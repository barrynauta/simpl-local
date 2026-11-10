> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10200](../README.md) / **Story-SIMPL-800**

# SIMPL-800: Management API for Identity Attributes and Participant Types

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10200](../README.md) (Epic) › **SIMPL-800** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority\
**I want to** via an API, use a CRUD interface (CRUD Interface (internal page)) for managing the connection between Identity Attributes (Identity Attribute (internal page)) and Participant Types (Participant Type (internal page)).

Each Identity Attribute can be related to more than one Participant Type and vice versa.

**So that**

I can manage the entity connections

**Acceptance Criteria:**

1. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for creating an Identity Attribute is available, **when** the user sends a POST request to /attribute with the required parameters, including a list of Participant Types to assign, **then** the response status code is 201 Created, and the response body contains the newly created Identity Attribute's details, including the assigned Participant Types.

2. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for reading an Identity Attribute is available, **when** the user sends a GET request to /attribute/{id}, **then** the response status code is 200 OK, and the response body contains the requested Identity Attribute's details, including the assigned Participant Types.

3. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for updating an Identity Attribute is available, **when** the user sends a PUT request to /attribute/{id} with the required parameters, including a list of Participant Types to assign, **then** the response status code is 200 OK, and the response body contains the updated Identity Attribute's details, including the assigned Participant Types.

4. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for deleting an Identity Attribute is available, **when** the user sends a DELETE request to /attribute/{id}, **then** the response status code is 204 No Content.

5. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for listing all Identity Attributes is available, **when** the user sends a GET request to /attribute, **then** the response status code is 200 OK, and the response body contains a list of all Identity Attributes, including their assigned Participant Types.

6. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for searching Identity Attributes is available, **when** the user sends a GET request to /attribute/search with the required parameters, **then** the response status code is 200 OK, and the response body contains a list of Identity Attributes matching the search criteria, including their assigned Participant Types.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10200](../README.md) (Epic) Making identity attributes assignable on Tier1 roles |
