> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../README.md) · [What the levels mean](../../../../../levels.md) · [All items](../../../../../all-items.md) · [By business process](../../../../../processes.md)

Folder: [items](../../../../README.md) / [no-business-process](../../../README.md) / [SIMPL-31424](../../README.md) / [SIMPL-31425](../README.md) / **SIMPL-31426**

# SIMPL-31426: Applicant provides participant contact email during onboarding

**Epic (deliverable, testable feature)** · Status **In Progress**

> ⚠ **Trace gap.** This Epic has a parent chain, but it ends at SIMPL-31424, which has no business process. See [trace gaps](../../../../../trace-gaps.md).

**Trace:** ⚠ no business process › [SIMPL-31424](../../../../../README.md) (L1) › [SIMPL-31425](../../../../../README.md) (L2) › **SIMPL-31426** (Epic)

## What this item is

This Epic makes the onboarding request ask the Applicant for a contact email of the participant, separate from the Applicant's own email.

## User story

**As an** Applicant\
**I want to** provide a dedicated contact email for the participant, separate from my own email address, as part of the onboarding request\
**so that** the participant has a persistent point of contact that remains available to other participants in the dataspace regardless of what happens to my own account.

## Acceptance criteria

**AC1**
- **Given** I am an Applicant, completing an onboarding request
- **When** I submit the onboarding request
- **Then** I am required to provide a contact email address for the participant

**AC2**
- **Given** an onboarding request containing a participant contact email is approved
- **When** the participant profile is created
- **Then** the contact email is associated with the participant profile

## Where it fits

| | |
| --- | --- |
| **Business process** | None, see trace gap. |
| **Parent** | [SIMPL-31425](../../../../../README.md) (L2) |
| **Children in this folder** | None. No user stories are in the export. |

## Item details

As exported on 10 November 2026. Not kept up to date.

| | |
| --- | --- |
| **Status** | In Progress |
| **Epic status** | To Do |
| **Priority** | Medium |
| **Requirement type** | Functional |
| **Impact on** | Testing |
| **Completion rate** | 10 (unit not given) |
| **Created** | 2026-08-11 |
| **Updated** | 2026-11-09 |

## Notes on the source

Problems found in the published export. They are listed here, not fixed in the text above.

- In the export the acceptance criteria are broken: only the Given lines are in a numbered list, and When and Then fall outside it. Shown here as two criteria.
- The criteria have no titles.
- The story says the contact email is "separate from my own email address". No criterion tests that.
- Status "In Progress" and Epic status "To Do" contradict each other.
