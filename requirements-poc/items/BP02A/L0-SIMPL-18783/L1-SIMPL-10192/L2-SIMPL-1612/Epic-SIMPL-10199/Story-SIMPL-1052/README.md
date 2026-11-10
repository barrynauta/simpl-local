> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-1052**

# SIMPL-1052: [UI] Identity Attribute creation page

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-1052** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attribute creation page

**I want to**

see a submission form where I have to specify the mandatory fields for creating an Identity Attribute. These fields are:

- code: a string value representing the identity attribute identifier;

- name: a string value representing a human-readable identity attribute description;

- assignable to roles: a boolean value indicating whether the identity attribute can be assigned to a participant's local role (true) or not (false).

**So that**

I can store the new Identity Attribute

Acceptance Criteria:

1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute creation page, then the page displays a submission form with input fields for "Code", "Name", and "Assignable to Roles" with clear and concise labels.**
1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute creation page, then the "Code" field is a text input field that accepts a string value, and the field is marked as required.**
1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute creation page, then the "Name" field is a text input field that accepts a string value, and the field is marked as required.**
1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute creation page, then the "Assignable to Roles" field is a boolean input field (e.g. checkbox) that accepts a true or false value, and the field is marked as required.**
1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user submits the form with valid data, then the system creates a new Identity Attribute with the specified code, name, and assignable to roles value, and redirects the user to the Identity Attributes Management page, displaying a success message indicating that the Identity Attribute was created successfully.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
