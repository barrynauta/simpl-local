> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1280](../../../README.md) / [L2-SIMPL-1614](../../README.md) / [Epic-SIMPL-2048](../README.md) / **Story-SIMPL-2569**

# SIMPL-2569: [UI] Tier 2 Gateway Resources Exclusions List

**User story (implementable work)** · Status **PBI Closed**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1280](../../../README.md) (L1) › [SIMPL-1614](../../README.md) (L2) › [SIMPL-2048](../README.md) (Epic) › **SIMPL-2569** (User story)

## Text

**As a** any participant user with the Tier 2 authorization manager, in the Tier2 resources exclusion management page, viewing the list of Tier2 gateway resources exclusions table list\
**I want to** see a table list with this heading:

- URL
- reason for exclusion
- exclusion date

**So that** I view the paginated list of defined Tier2 gateway resources exclusions for my agent

**Acceptance Criteria:**

1. **Given** that the user is logged in with the T2IAA_M role, **when** the user navigates to the Tier2 resources exclusion management page, **then** the page displays an empty table with the correct column headings: "URL", "Reason for Exclusion", and "Exclusion Date".

2. **Given** that the user is logged in with the T2IAA_M role, **when** the user navigates to the Tier2 resources exclusion management page and there are defined Tier2 gateway resources exclusions, **then** the table displays a paginated list of Tier2 gateway resources exclusions with their corresponding values for "URL", "Reason for Exclusion", and "Exclusion Date".

3. **Given** that the user is logged in with the T2IAA_M role, **when** the user navigates to the Tier2 resources exclusion management page, **then** the table displays a pagination control that allows the user to navigate through the list of Tier2 gateway resources exclusions.

4. **Given** that the user is logged in with the T2IAA_M role, **when** the user navigates to the Tier2 resources exclusion management page, **then** the table displays a consistent and readable format for dates in the "Exclusion Date" column.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2048](../README.md) (Epic) Communication agent to agent(tier 2)- exceptions in mTLS |
