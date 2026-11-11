<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider assigns access policies related to contract consent to a new resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-29974, status **Defined**. It has 1 user story, each with acceptance criteria that say when the work is done.

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

## User stories

1. [[UI] Configure Contract Consent Access Policy for Resource Descriptions](#user-story-1-ui-configure-contract-consent-access-policy-for-resource-descriptions) (FOR SC TO RESPOND)

### User story 1: [UI] Configure Contract Consent Access Policy for Resource Descriptions

**Status:** FOR SC TO RESPOND (backlog item SIMPL-34176)

###### User Story

**As a** Provider\
**I want to** configure a Contract Consent Access Policy for a Resource Description\
**So that** access to the resource can also be governed according to the status of Contract Consent Assertions associated with usage contracts.

###### Description

SD-Tooling shall allow a Provider to enable Contract Consent management when configuring a Resource Consent Access Policy.

A Contract Consent Access Policy can only be configured when a Resource Consent Assertion has already been selected.

When enabled, SD-Tooling shall create and associate a Contract Consent Access Policy to the Resource Description, indicating that each usage contract must be governed by a Contract Consent Assertion.

###### Acceptance Criteria

**AC1 - Enable Contract Consent Management**

1. **Given** a Resource Consent Access Policy has been configured\
   **When** I access the Service Policy step\
   **Then** I can enable the Contract Consent option

**AC2 - Create Contract Consent Access Policy**

2. **Given** a Resource Consent Access Policy exists\
   **When** I enable the Contract Consent option\
   **Then** a Contract Consent Access Policy is created

**AC3 - Assign Policy to Resource Description**

3. **Given** a Contract Consent Access Policy has been created\
   **When** the Resource Description is saved\
   **Then** the Contract Consent Access Policy is assigned to the Resource Description

---

Back to [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
