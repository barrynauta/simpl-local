<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider assigns access policies related to resource consent to a new resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-29973, status **Defined**. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** assign access policies related to resource consent to a new resource description via UI\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned the resource consent status.

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I retrieve the list of resource consent assertions\
   **And** I select one of the resource consent assertions to use in the access policy\
   **Then** Simpl-Open creates an access policy related to resource consent\
   **And** links the selected resource consent assertion to the access policy\
   **And** assigns the access policy related to resource consent to the new resource description.

## User stories

1. [[UI] Configure Resource Consent Access Policy for Resource Descriptions](#user-story-1-ui-configure-resource-consent-access-policy-for-resource-descriptions) (PBI Defined)

### User story 1: [UI] Configure Resource Consent Access Policy for Resource Descriptions

**Status:** PBI Defined (backlog item SIMPL-34175)

###### User Story

**As a** Provider\
**I want to** configure a Resource Consent Access Policy for a Resource Description\
**So that** access to the resource can be governed according to the status of a Resource Consent Assertion.

###### Description

SD-Tooling shall allow a Provider to create a Resource Consent Access Policy within the **Access Policy** step of the Resource Description wizard.

During the configuration, the Provider shall select a Resource Consent Assertion from the available assertions.

The selected assertion shall be associated with the newly created Resource Consent Access Policy and the policy shall be assigned to the Resource Description.

###### Acceptance Criteria

**AC1 - Select Resource Consent Assertion**

1. **Given** I am creating or editing a Resource Description\
   **When** I access the Access Policy step\
   **Then** I can retrieve and consult the available Resource Consent Assertions

**AC2 - Create Resource Consent Access Policy**

2. **Given** I am in the Access Policy step\
   **When** I select a Resource Consent Assertion\
   **Then** a Resource Consent Access Policy is created\
   **And** the selected Resource Consent Assertion is linked to the policy

**AC3 - Assign Policy to Resource Description**

3. **Given** a Resource Consent Access Policy has been created\
   **When** the Resource Description is saved\
   **Then** the policy is assigned to the Resource Description

---

Back to [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
