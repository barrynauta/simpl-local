<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider assigns access policies related to contract consent to a new resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-29974. It has no user stories yet.

## What this Epic delivers

**As a** Provider\
**I want to** assign access policies related to contract consent to a new resource description via UI\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned the contract consent status.

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I create an access policy related to resource consent\
   **And** I indicate that contract consent is required for each new usage contract\
   **Then** Simpl-Open creates an access policy related to contract consent\
   **And** assigns the access policy related to contract consent to the new resource description.

---

Back to [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
