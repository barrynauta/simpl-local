> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-909**

# SIMPL-909: [UI] Identity Attribute detail page

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-909** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attributes Management page, viewing the Identity Attributes table list\
**I want to** interact with each row of the table list, clicking on them\
**So that** I view the Identity Attribute detail information page

**Acceptance Criteria:**

1. **Given** that the user is logged in with the IATTR_M role in the Dataspace Governance Authority, **when** the user is viewing the Identity Attributes table list, **then** each row in the table is clickable.

2. **Given** that the user is logged in with the IATTR_M in the Dataspace Governance Authority, **when** the user clicks on a row in the Identity Attributes table list, **then** the system navigates to the Identity Attribute detail information page for the selected Identity Attribute.

3. **Given** that the user is logged in with the IATTR_M in the Dataspace Governance Authority, **when** the user is on the Identity Attribute detail information page, **then** the page displays the detailed information of the selected Identity Attribute, including identifier, code, if it is assignable to agent local roles, creation date, last update date.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
