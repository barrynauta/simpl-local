> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-21117](../../README.md) / [L2-SIMPL-14642](../README.md) / **Epic-SIMPL-18026**

# SIMPL-18026: A Provider notifies Consumers about the revocation of a resource description

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-21117](../../README.md) (L1) › [SIMPL-14642](../README.md) (L2) › **SIMPL-18026** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** notify Consumers about a revoked resource description\
**So that** they can react on this.

#### Acceptance criteria

**Scenario:** A Provider notifies Consumers about the revocation of a resource description

1. **Given** I, the Provider, have requested the revocation of a resource description\
   **When** the resource description is revoked\
   **Then** a notification about the revocation is sent to the Consumers of the resource descriptions with an active contract.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14642](../README.md) (L2) A Provider notifies Consumers about the revocation of a resource description |
| **Children** | [SIMPL-19790](Story-SIMPL-19790/README.md) (User story) [API] A Provider notifies Consumers about revocation of Resource Description<br>[SIMPL-22586](Story-SIMPL-22586/README.md) (User story) [UI] Inform end user (provider) about successful revocation |
