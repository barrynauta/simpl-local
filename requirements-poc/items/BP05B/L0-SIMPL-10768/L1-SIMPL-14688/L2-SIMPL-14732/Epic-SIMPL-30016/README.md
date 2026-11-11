> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14732](../README.md) / **Epic-SIMPL-30016**

# SIMPL-30016: A Provider sets the VPN configuration for Agent-to-Agent data transfer via UI

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14732](../README.md) (L2) › **SIMPL-30016** (Epic)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14732](../README.md) (L2) A Provider adds resource type specific metadata to a new Resource description |
| **Children** | None. |
