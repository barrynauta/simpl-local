> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-915**

# SIMPL-915: [UI] Identity Attributes Management page - filtering

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-915** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attributes Management page, viewing the Identity Attributes table list

**I want to**

have a filter section where I can specify either the Identifier or the Code for the Identity Attribute I'm looking for

**So that**

I view only the Identity Attributes I'm looking for

**Acceptance Criteria:**

1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user is viewing the Identity Attributes Management page, then a filter section is displayed above the Identity Attributes table list, containing input fields for "Name" and "Code" filters.**

1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user enters a value in the "Name" filter input field and clicks the "Apply" button, then the system filters the Identity Attributes table list to display only the Identity Attributes whose identifier contains the entered value.**

1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user enters a value in the "Code" filter input field and clicks the "Apply" button, then the system filters the Identity Attributes table list to display only the Identity Attributes whose code matches the entered value.**

1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user applies a filter, then the system updates the paginated list of Identity Attributes to reflect the filtered results, and the pagination control is updated accordingly.**

1. **Given that the user is logged in with the** **IATTR_M** **role in the Dataspace Governance Authority, when the user clears the filter input fields and clicks the "Apply" button, then the system resets the Identity Attributes table list to display all Identity Attributes, and the pagination control is updated accordingly.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
