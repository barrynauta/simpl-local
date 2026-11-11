> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP11](../../../../README.md) / [L0-SIMPL-27777](../../../README.md) / [L1-SIMPL-27783](../../README.md) / [L2-SIMPL-27785](../README.md) / **Epic-SIMPL-31853**

# SIMPL-31853: A Provider triggers the decommissioning of an infrastructure resource when revocation request is accepted

**Epic (deliverable, testable feature)**

**Trace:** [BP11](../../../../README.md) › [SIMPL-27777](../../../README.md) (L0) › [SIMPL-27783](../../README.md) (L1) › [SIMPL-27785](../README.md) (L2) › **SIMPL-31853** (Epic)

## What this item is

This Epic makes sure that once a usage contract for an infrastructure resource is revoked, the Consumer can no longer use the resource: its access credentials are switched off and decommissioning starts.

## Text

**As a** Provider,\
**I want to** decommission an infrastructure resource when the revocation request of the usage contract is accepted\
**So that** I ensure that the Consumer cannot continue using the resource after the revocation of the usage contract

**Acceptance Criteria**

1. **Given** a usage contract of an infrastructure resource for which the revocation request is accepted\
   **When** I the usage contract is revoked\
   **Then** the access credentials of the infrastructure resource are deactivated\
   **And** the decommissioning process of the infrastructure resource is triggered

Note: The decommissioning of the infrastructure resource will be performed after the quarantine period set by the Infrastructure Provider (outside of the scope of Simpl-Open).

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP11](../../../../README.md) (no public page) |
| **Parent** | [SIMPL-27785](../README.md) (L2) A Provider and Consumer respond to a revocation request for a usage contract |
| **Children** | None. |

## Notes on the source

Problems found in the source and in PSO's export of 10 November 2026. They are listed here, not fixed in the text above.

- "When I the usage contract is revoked" is a typo in the source. Kept as written.
- Given (revocation accepted) and When (contract revoked) describe almost the same moment, so it is unclear what triggers the outcome.
- PSO's export showed no parent. In the source it sits under L2 [SIMPL-27785](../README.md), in BP11.
