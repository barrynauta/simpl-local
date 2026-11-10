> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-482**

# SIMPL-482: Expose Identity Attributes via API

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-482** (User story)

## Text

**As A**

Dataspace governance authority agent

**I want to**

provide an API to retrieve all the identity attributes ([Identity Attribute|an internal page]) defined in the dataspace governance authority

**So that**

any dataspace participant can retrieve the list of identity attributes

Acceptance Criteria:

1. **Given that I am a dataspace governance authority agent and that I provide an API to retrieve all identity attributes defined in the dataspace governance authority, when any another participant accesses the API then I should be able to gather the list of identity attributes**
1. **Given that I am a dataspace participant agent and that I get the whole list of identity attributes from the API when I examine the properties of each identity attribute in the list then I see the following properties for each identity attribute: identity attribute code and assignable to roles; the first is the identity attribute human readable identifier and the latter is a boolean value, true if the identity attribute is assignable to an agent role.**
1. **Given that I am not an onboarded dataspace participant agent and I try to access the list of all identity attributes defined in the dataspace governance authority, when I send a request to the API, then I should receive an unauthorized access error response wth 401 status code.**
1. **Given that I am not an onboarded dataspace participant agent and I try to access the list of all identity attributes defined in the dataspace governance authority, when I examine the error response, then I should see a generic error message containing only the "unathorized access" string.**
1. Given that I am not an onboarded dataspace participant agent and I try to access the list of all identity attributes defined in the dataspace governance authority, when I try to retrieve the list of identity attributes, then I should not be able to access the list of identity attributes.{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
