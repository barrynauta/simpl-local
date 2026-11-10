> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA09](../../../../README.md) / [L0-SIMPL-26074](../../../README.md) / [L1-SIMPL-26083](../../README.md) / [L2-SIMPL-27195](../README.md) / **Epic-SIMPL-27206**

# SIMPL-27206: Data Provider withdraws and reactivates consent assertions

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA09](../../../../README.md) › [SIMPL-26074](../../../README.md) (L0) › [SIMPL-26083](../../README.md) (L1) › [SIMPL-27195](../README.md) (L2) › **SIMPL-27206** (Epic)

## Text

**As a** Data Provider\
**I want to** withdraw or reactivate a consent assertion\
**So that** the consent status of assertions accurately reflects the current consent basis for the linked data resources and governs access control decisions for bound Consumers accordingly.

**Acceptance Criteria:**

1. **Given** I am a Data Provider user with the appropriate roles\
   **When** I withdraw a consent assertion\
   **Then** the assertion status is set to WITHDRAWN

2. **Given** I am a Data Provider user with the appropriate roles\
   **When** I reactivate a previously withdrawn consent\
   **Then** the assertion status is set to ACTIVE

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27195](../README.md) (L2) Data Provider withdraws and reactivates consent assertions |
| **Children** | [SIMPL-28591](Story-SIMPL-28591/README.md) (User story) [API] Data Provider updates consent assertion status<br>[SIMPL-28592](Story-SIMPL-28592/README.md) (User story) [API] Data Provider returns consent assertion status<br>[SIMPL-28599](Story-SIMPL-28599/README.md) (User story) [UI] Data Provider updates consent assertion status from consent assertion details page<br>[SIMPL-29116](Story-SIMPL-29116/README.md) (User story) [UI] Data Provider updates consent assertion status from consent assertion list |
