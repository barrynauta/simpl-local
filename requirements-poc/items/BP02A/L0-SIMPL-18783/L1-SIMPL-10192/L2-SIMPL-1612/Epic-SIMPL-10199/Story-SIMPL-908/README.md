> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-908**

# SIMPL-908: [UI] Identity Attributes Management page

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-908** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attributes Management page, viewing the Identity Attributes table list

**I want to**

see a table list with this heading:

- Identifier;

- Code;

- Assignable to roles (boolean value true if the Identity Attribute can be assigned to a participant's local role);

- Enabled (boolean value true if the Identity Attribute is assignable to participants);

- In use (boolean value true if assigned to some participant in dataspace)
- Creation date;

- Last update date.

**So that**

I view the paginated list of defined Identity Attributes

**Acceptance Criteria:**

1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page, then the page displays an empty table with the correct column headings: "Identifier", "Code", "Assignable to roles", "Enabled", "In use", "Creation Date", and "Last Update Date".**
1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page and there are Identity Attributes defined, then the table displays a paginated list of Identity Attributes with their corresponding values for "Identifier", "Code", "Assignable to roles", "Enabled", "In use", "Creation Date", and "Last Update Date".**
1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page, then the "Assignable to roles" column displays a boolean value (true or false) indicating whether the Identity Attribute can be assigned to a participant's local role.**
1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page, then the "Enabled" column displays a boolean value (true or false) indicating whether the Identity Attribute is assignable to participants.**
1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page, then the "In use" column displays a boolean value (true or false) indicating whether the Identity Attribute is assigned to at least one participant in the dataspace.**
1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page, then the table displays a pagination control that allows the user to navigate through the list of Identity Attributes.**
1. **Given that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, when the user navigates to the Identity Attributes Management page, then the table displays a consistent and readable format for dates in the "Creation Date" and "Last Update Date" columns.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
