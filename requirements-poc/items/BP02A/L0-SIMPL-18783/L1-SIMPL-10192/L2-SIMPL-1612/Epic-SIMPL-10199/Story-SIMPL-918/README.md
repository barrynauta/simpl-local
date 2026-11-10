> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-918**

# SIMPL-918: [UI] Identity Attributes Management page - ordering

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-918** (User story)

## Text

**As A**

user with *Tier 2 identity attributes manager* role in the Dataspace Governance Authority, in the Identity Attributes Management page, viewing the Identity Attributes table list

**I want to**

select the list sort order by "Identifier", "Code" or "Last update date" columns in ascending/descending direction

**So that**

I view an ordered paginated list of Identity Attributes based on the selected filter

**Acceptance Criteria:**

1. **Given that I am a user logged in with the IATTR_M role in the Dataspace Governance Authority, when I access the Identity Attributes Management page and view the Identity Attributes table list, I should see options to select the sort order by "Identifier", "Code", or "Last update date" columns in ascending or descending direction.**

1. **Given that I have selected "Identifier" as the sort criterion, when I choose the ascending direction, the Identity Attributes table list should be sorted in alphabetical order by Name in ascending order.**

1. **Given that I have selected "Code" as the sort criterion, when I choose the ascending direction, the Identity Attributes table list should be sorted in alphabetical order by Code in ascending order.**

1. **Given that I have selected "Last update date" as the sort criterion, when I choose the descending direction, the Identity Attributes table list should be sorted in reverse chronological order by Last update date in descending order.**

1. **Given that I have sorted the Identity Attributes table list based on the selected column and direction, when I navigate through the paginated list, the sorting order should be maintained consistently across all pages, ensuring that the selected sort criterion applies uniformly to all displayed Identity Attributes.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
