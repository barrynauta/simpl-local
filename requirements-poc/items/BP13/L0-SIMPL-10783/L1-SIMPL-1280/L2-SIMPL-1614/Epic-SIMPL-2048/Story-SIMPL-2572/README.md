> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1280](../../../README.md) / [L2-SIMPL-1614](../../README.md) / [Epic-SIMPL-2048](../README.md) / **Story-SIMPL-2572**

# SIMPL-2572: [UI] Tier 2 Gateway Resources Exclusions List - filtering

**User story (implementable work)** · Status **PBI Closed**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1280](../../../README.md) (L1) › [SIMPL-1614](../../README.md) (L2) › [SIMPL-2048](../README.md) (Epic) › **SIMPL-2572** (User story)

## Text

**As a** any participant user with the Tier 2 authorization manager, in the Tier2 resources exclusion management page, viewing the list of Tier2 gateway resources exclusions table list\
**I want to** have a filter section where I can specify the resource exclusion element I'm looking for.

The filtering is done by:

- URL

**So that**

I view only the resource exclusion element I'm looking for.

**Acceptance Criteria:**

1. **Given** that the user is logged in with the T2IAA_M role, **when** the user is viewing the Tier2 resources exclusion management page, **then** a filter section is displayed above the Tier2 gateway resources exclusions table list, containing an input field for the "URL" filter.

2. **Given** that the user is logged in with the T2IAA_M role, **when** the user enters a value in the "URL" filter input field and clicks the "Apply" button, **then** the system filters the Tier2 gateway resources exclusions table list to display only the resource exclusions whose URL contains the entered value.

3. **Given** that the user is logged in with the T2IAA_M role, **when** the user applies a filter, **then** the system updates the paginated list of Tier2 gateway resources exclusions to reflect the filtered results, and the pagination control is updated accordingly.

4. **Given** that the user is logged in with the T2IAA_M role, **when** the user clears the filter input field and clicks the "Apply" button, **then** the system resets the Tier2 gateway resources exclusions table list to display all Tier2 gateway resources exclusions, and the pagination control is updated accordingly.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2048](../README.md) (Epic) Communication agent to agent(tier 2)- exceptions in mTLS |
