> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-875**

# SIMPL-875: Attempt to Delete Assigned Identity Attribute via API

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-875** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, via API deleting an Identity Attribute assigned to a dataspace participant

**I want to**

not delete the Identity Attribute

**So that**

I receive an unauthorized error and the deletion is prohibited

**Acceptance Criteria:**

1. **Given that the user is logged in with the IATTR_M role, and the API endpoint for deleting an Identity Attribute is available, when the user sends a DELETE request to the API endpoint with the specific Identity Attribute ID that is assigned to a dataspace participant, then the response status code is 403 Forbidden, and the response body contains an error message indicating that the deletion of an assigned Identity Attribute is not allowed.**

1. **Given that the user is logged in with the** **IATTR_M** **role, and the API endpoint for deleting an Identity Attribute is available, when the user sends a DELETE request to the API endpoint with the specific Identity Attribute ID that is assigned to a dataspace participant, then the Identity Attribute remains assigned to the dataspace participant.**

1. **Given that the user is logged in with the** **IATTR_M** **role, and the API endpoint for retrieving the Identity Attributes list is available, when the user sends a GET request to the API endpoint, then the response status code is 200 OK, and the response body contains the Identity Attribute that was attempted to be deleted, still assigned to the dataspace participant.**

1. **Given that the user is logged in with the** **IATTR_M** **role, and the API endpoint for deleting an Identity Attribute is available, when the user sends a DELETE request to the API endpoint with an invalid Identity Attribute ID, then the response status code is 404 Not Found, and the response body contains an error message indicating that the Identity Attribute does not exist.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
