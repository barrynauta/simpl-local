<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider sets the VPN configuration for Agent-to-Agent data transfer via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.02 Add resource type specific metadata](../README.md#bp05b02-add-resource-type-specific-metadata).

In the backlog it is item SIMPL-30016, status **Defined**. It has no user stories yet.

## What this Epic delivers

**As a** Provider\
**I want to** indicate in a data resource description if the transfer should use a secured VPN tunnel\
**So that** I can ensure that my data resource is configured with a secured VPN tunnel that is created automatically at the moment of transfer.

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new data resource description\
   **And** the transfer method is data transfer\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** I can indicate whether the resource will be transferred through a secured VPN tunnel.

---

Back to [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
