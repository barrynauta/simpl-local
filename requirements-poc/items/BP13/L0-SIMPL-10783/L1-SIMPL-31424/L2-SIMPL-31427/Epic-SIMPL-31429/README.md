> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP13](../../../../README.md) / [L0-SIMPL-10783](../../../README.md) / [L1-SIMPL-31424](../../README.md) / [L2-SIMPL-31427](../README.md) / **Epic-SIMPL-31429**

# SIMPL-31429: Participant updates contact email after onboarding

**Epic (deliverable, testable feature)**

**Trace:** [BP13](../../../../README.md) › [SIMPL-10783](../../../README.md) (L0) › [SIMPL-31424](../../README.md) (L1) › [SIMPL-31427](../README.md) (L2) › **SIMPL-31429** (Epic)

## What this item is

This Epic lets a participant change its contact email after onboarding, and passes the change to the Governance Authority. It covers the update part of L1 [SIMPL-31424](../../README.md).

## Text

**As a** Participant\
**I want to** update my contact email address once I have completed the onboarding procedure\
**So that** my organisation's point of contact stays updated, and other participants and the Governance Authority can keep reaching me via an up-to-date email address

**Acceptance Criteria**

1. **Given** I am an onboarded Participant\
   **When** I update my contact email address\
   **Then** the new contact email address is saved to my participant profile

2. **Given** I have updated my contact email address\
   **When** the update is completed\
   **Then** the new contact email is propagated to the Governance Authority

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-31427](../README.md) (L2) Participant updates contact email |
| **Children** | [SIMPL-31015](Story-SIMPL-31015/README.md) (User story) [UI] Participant updates its contact email address<br>[SIMPL-31486](Story-SIMPL-31486/README.md) (User story) [API] Participant user updates participant's contact email address (Tier1) |

## Notes on the source

Problems found in the source and in PSO's export of 10 November 2026. They are listed here, not fixed in the text above.

- PSO's export showed this Epic as Cancelled and put it under "Out of scope". In the source it was never cancelled: its history goes from New to Defined (checked 10 November 2026).
- PSO's export showed no parent. In the source it sits under L2 [SIMPL-31427](../README.md) Participant updates contact email.
- In PSO's export the acceptance criteria had the same broken layout as SIMPL-31426.
