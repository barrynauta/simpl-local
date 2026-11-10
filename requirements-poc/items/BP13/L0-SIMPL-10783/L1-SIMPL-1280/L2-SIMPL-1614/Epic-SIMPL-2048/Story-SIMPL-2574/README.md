> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1280](../../../README.md) / [L2-SIMPL-1614](../../README.md) / [Epic-SIMPL-2048](../README.md) / **Story-SIMPL-2574**

# SIMPL-2574: [UI] Tier 2 Gateway Resources Exclusions List - ordering

**User story (implementable work)** · Status **PBI Closed**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1280](../../../README.md) (L1) › [SIMPL-1614](../../README.md) (L2) › [SIMPL-2048](../README.md) (Epic) › **SIMPL-2574** (User story)

## Text

**As A**

any participant user with the Tier 2 authorization manager, in the Tier2 resources exclusion management page, viewing the list of Tier2 gateway resources exclusions table list

**I want to**

select the list sort order by "URL" or "exclusion date" columns in ascending/descending direction

**So that**

I view an ordered paginated list of resources exclusions based on the selected filter

**Acceptance Criteria:**

1. **Given that I am a user logged in with the** **T2IAA_M** **role, when I access the Tier2 resources exclusion management page and view the Tier2 gateway resources exclusions table list, I should see options to select the sort order by "URL" or "Exclusion Date" columns in ascending or descending direction.**
1. **Given that I have selected "URL" as the sort criterion, when I choose the ascending direction, the Tier2 gateway resources exclusions table list should be sorted in alphabetical order by URL in ascending order.**
1. **Given that I have selected "URL" as the sort criterion, when I choose the descending direction, the Tier2 gateway resources exclusions table list should be sorted in reverse alphabetical order by URL in descending order.**
1. **Given that I have selected "Exclusion Date" as the sort criterion, when I choose the ascending direction, the Tier2 gateway resources exclusions table list should be sorted in chronological order by Exclusion Date in ascending order.**
1. **Given that I have selected "Exclusion Date" as the sort criterion, when I choose the descending direction, the Tier2 gateway resources exclusions table list should be sorted in reverse chronological order by Exclusion Date in descending order.**
1. **Given that I have sorted the Tier2 gateway resources exclusions table list based on the selected column and direction, when I navigate through the paginated list, the sorting order should be maintained consistently across all pages, ensuring that the selected sort criterion applies uniformly to all displayed resource exclusions.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2048](../README.md) (Epic) Communication agent to agent(tier 2)- exceptions in mTLS |
