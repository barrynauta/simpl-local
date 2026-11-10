> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10200](../README.md) / **Story-SIMPL-881**

# SIMPL-881: Define Assignable Identity Attributes for Tier 1 User Roles via API

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10200](../README.md) (Epic) › **SIMPL-881** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority\
**I want to** via an API, define which Identity Attributes (Identity Attribute (internal page)) are assignable to the Tier1 user roles\
**So that** Any Tier 1 user and role manager dataspace participant user can assign each Identity Attribute to any local Tier 1 user role.

**Acceptance Criteria:**

1. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for defining assignable Identity Attributes is available, **when** the user sends a PUT request to the API endpoint with a list of Identity Attributes ID and a flag set to true, **then** the response status code is 200.

2. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for defining not assignable Identity Attributes is available, **when** the user sends a PUT request to the API endpoint with a list of Identity Attributes ID and a flag set to false, **then** the response status code is 200.

3. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for retrieving all Identity Attributes is available, **when** the user sends a GET request to the API endpoint, **then** the response status code is 200, and the response body contains a list of all Identity Attributes, each with an "assignable" property indicating whether the Identity Attribute is assignable to Tier 1 user roles.

4. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for retrieving all Identity Attributes is available, **when** the user sends a GET request to the API endpoint, **then** the response body contains the Identity Attributes that were defined as assignable to Tier 1 user roles with their "assignable" property set to true.

5. **Given** that the user is logged in with the IATTR_M role, and the API endpoint for retrieving all Identity Attributes is available, **when** the user sends a GET request to the API endpoint, **then** the response body contains the Identity Attributes with their "assignable" property set to false for those that were not defined as assignable to Tier 1 user roles.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10200](../README.md) (Epic) Making identity attributes assignable on Tier1 roles |
