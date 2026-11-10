> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14699](../../../README.md) / [L2-SIMPL-14639](../../README.md) / [Epic-SIMPL-9764](../README.md) / **Story-SIMPL-19784**

# SIMPL-19784: [UI] A Provider requests the revocation of a Resource description via UI

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14699](../../../README.md) (L1) › [SIMPL-14639](../../README.md) (L2) › [SIMPL-9764](../README.md) (Epic) › **SIMPL-19784** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** revoke a Resource description via UI\
**So that** the Resource description cannot be used any more.

#### Acceptance criteria

**Scenario:** A Provider requests the revocation of a Resource description via UI - positive case

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I request to revoke a Resource description\
   **Then** I am asked to confirm the revocation\
   **And** upon confirmation a revocation request is sent to the Governance authority.

**Scenario:** A Provider requests the revocation of a Resource description via UI - declining confirmation

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I request to revoke a Resource description\
   **And** I am asked to confirm the revocation\
   **And** I decline to confirm\
   **Then** I am guided back to the details view.

**Scenario:** A Provider requests the revocation of a Resource description via UI - API error

3. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I demand to revoke a resource description\
   **And** I have confirmed the revocation\
   **And** the API to revoke the resource description throws an error\
   **Then** the revocation of the resource description is not possible\
   **And** a notification is shown that the resource description cannot be revoked.

**Scenario:** A Provider requests the revocation of a Resource description via UI - not authenticated

4. **Given** I am not authenticated as Provider\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

**Scenario:** A Provider requests the revocation of a Resource description via UI  - no role

5. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of resource rescriptions\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

#### Mockups:

| RD Details | Confirmation popup |
| --- | --- |
| !image-2026-03-23-12-53-45-950.png | width=428,height=201! | !Confirm.jpg | width=317,height=175! |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-9764](../README.md) (Epic) A Provider requests the revocation of a resource description via UI or API |
