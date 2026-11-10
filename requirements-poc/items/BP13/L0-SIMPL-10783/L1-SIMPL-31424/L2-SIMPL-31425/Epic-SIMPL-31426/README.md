> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP13](../../../../README.md) / [L0-SIMPL-10783](../../../README.md) / [L1-SIMPL-31424](../../README.md) / [L2-SIMPL-31425](../README.md) / **Epic-SIMPL-31426**

# SIMPL-31426: Applicant provides participant contact email during onboarding

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP13](../../../../README.md) › [SIMPL-10783](../../../README.md) (L0) › [SIMPL-31424](../../README.md) (L1) › [SIMPL-31425](../README.md) (L2) › **SIMPL-31426** (Epic)

## What this item is

This Epic makes the onboarding request ask the Applicant for a contact email of the participant, separate from the Applicant's own email.

## Text

**As an** Applicant\
**I want to** provide a dedicated contact email for the participant, separate from my own email address, as part of the onboarding request\
**So that** the participant has a persistent point of contact that remains available to other participants in the dataspace regardless of what happens to my own account.

**Acceptance Criteria**

1. **Given** I am an Applicant, completing an onboarding request\
   **When** I submit the onboarding request\
   **Then** I am required to provide a contact email address for the participant

2. **Given** an onboarding request containing a participant contact email is approved\
   **When** the participant profile is created\
   **Then** the contact email is associated with the participant profile

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-31425](../README.md) (L2) Governance authority registers participants' contact email |
| **Children** | [SIMPL-31016](Story-SIMPL-31016/README.md) (User story) [UI][IMPROVEMENT] Governance authority views contact email address of an onboarding request<br>[SIMPL-31308](Story-SIMPL-31308/README.md) (User story) [UI][IMPROVEMENT] Applicant provides contact email address during onboarding<br>[SIMPL-31459](Story-SIMPL-31459/README.md) (User story) [API][IMPROVEMENT] Applicant provides organization contact email<br>[SIMPL-31472](Story-SIMPL-31472/README.md) (User story) [API][IMPROVEMENT] Governance authority returns contact email address in onboarding request details<br>[SIMPL-31473](Story-SIMPL-31473/README.md) (User story) [API][IMPROVEMENT] Governance authority returns contact email address in onboarding request list<br>[SIMPL-31476](Story-SIMPL-31476/README.md) (User story) [UI] Applicant views contact email address of an onboarding request |

## Notes on the source

Problems found in the source and in PSO's export of 10 November 2026. They are listed here, not fixed in the text above.

- In PSO's export the acceptance criteria were broken: only the Given lines were in a numbered list.
- The criteria have no titles.
- The story says the contact email is "separate from my own email address". No criterion tests that.
- Status "In Progress" and Epic status "To Do" contradict each other.
