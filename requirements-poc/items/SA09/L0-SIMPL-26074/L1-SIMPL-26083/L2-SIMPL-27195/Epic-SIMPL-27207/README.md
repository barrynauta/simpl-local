> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA09](../../../../README.md) / [L0-SIMPL-26074](../../../README.md) / [L1-SIMPL-26083](../../README.md) / [L2-SIMPL-27195](../README.md) / **Epic-SIMPL-27207**

# SIMPL-27207: Data Provider notifies Consumers upon consent assertion status change

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [SA09](../../../../README.md) › [SIMPL-26074](../../../README.md) (L0) › [SIMPL-26083](../../README.md) (L1) › [SIMPL-27195](../README.md) (L2) › **SIMPL-27207** (Epic)

## Text

**As a** Data Provider\
**I want to** ensure that all Consumers bound to a consent assertion are notified whenever its status changes\
**So that** Consumers can promptly act on the updated consent status of the data resource.

**Acceptance Criteria:**

1. **Given** a consent assertion has been withdrawn\
   **When** the status change is processed by the platform\
   **Then** all bound Consumers are notified that the data resource is no longer available for consumption

2. **Given** a consent assertion has been reactivated\
   **When** the status change is processed by the platform\
   **Then** all bound Consumers are notified that the data resource is available for ingestion

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27195](../README.md) (L2) Data Provider withdraws and reactivates consent assertions |
| **Children** | None. |
