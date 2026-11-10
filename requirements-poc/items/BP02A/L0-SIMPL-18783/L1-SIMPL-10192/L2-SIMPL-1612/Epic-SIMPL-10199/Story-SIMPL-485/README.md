> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-485**

# SIMPL-485: Retrieve Paginated List of Identity Attributes via API

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-485** (User story)

## Text

**As A**

user with *Tier 1 user and role manager* role in a dataspace participant

**I want to**

via an API, have a paginated list of the last Identity Attribute Entity ([Identity Attribute|an internal page]) synchronized with the dataspace governance authority.

**So that**

I view the list of identity attributes of dataspace and assignable to participant roles

Acceptance Criteria:

1. **Given that I am a user of a participant with the T1UAR_M role and that I send a request to the API to retrieve a paginated list of identity attributes, when I receive the response from the API then I should receive a paginated list of identity attributes**
1. **Given that I am a user of a participant with the T1UAR_M role and that I retrieve a paginated list of identity attributes from the API, when I examine the properties of each identity attribute in the list, then I should see the following properties for each identity attribute: identity attribute (string value), assigned to participant (boolean value), and assignable to users (boolean value)**
1. **Given that I am a user of a participant with the T1UAR_M role and that I retrieve a paginated list of identity attributes from the API, when I examine the "assigned to participant" property of each identity attribute, then I should see that the flag is set to true if the identity attribute is assigned to my participant, and false otherwise**
1. **Given that I am a user of a participant with the T1UAR_M role and that I retrieve a paginated list of identity attributes from the API, when I examine the "assignable to roles" property of each identity attribute, then I should see that the flag is set to true if the identity attribute can be assigned to a participant role, and false otherwise**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
