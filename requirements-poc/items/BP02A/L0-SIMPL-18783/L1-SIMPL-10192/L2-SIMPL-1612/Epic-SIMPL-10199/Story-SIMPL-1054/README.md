> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-1054**

# SIMPL-1054: [UI] Editing Identity Attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-1054** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attribute edit page

**I want to**

see a submission form where I have to specify the mandatory fields of an Identity Attribute prepopulated with the current values. These fields are:

- identifier: a string value representing the identity attribute identifier;

- name: a string value representing a human-readable identity attribute description;

- assignable to roles: a boolean value indicating whether the identity attribute can be assigned to a participant's local role (true) or not (false).

- enabled: a boolean value, default is true, will be set to false if the attribute has to be hidden from the usable identity attributes (without removing it).

**So that**

I can edit the existing Identity Attribute

**Acceptance Criteria:**

1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute edit page, then the page displays a submission form with input fields for "Identifier", "Name", "Assignable to Roles", and "Enabled" prepopulated with the current values of the Identity Attribute.**
1. Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute edit page, then the "Enabled" field is a boolean input field (e.g. checkbox) that accepts a true or false value, and the field is prepopulated with the current enabled value of the Identity Attribute, with a default value of true.{}
1. Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user submits the form with valid changes, then the system updates the existing Identity Attribute with the new values, and redirects the user to the Identity Attribute detail information page, displaying a success message indicating that the Identity Attribute was updated successfully.
1. Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attribute edit page, then the form includes a "Cancel" button that allows the user to cancel the editing process and return to the Identity Attribute detail information page, and a "Save" button that submits the form when clicked.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
