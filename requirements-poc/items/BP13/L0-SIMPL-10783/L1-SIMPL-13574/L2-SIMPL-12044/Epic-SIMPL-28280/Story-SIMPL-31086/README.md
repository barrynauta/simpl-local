> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-31086**

# SIMPL-31086: The Infrastructure Service Manager service is available to authorised users of Infrastructure Provider Participant Type

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-31086** (User story)

## Text

**As a** Simpl-Open administrator\
**I want** authorised users of an Infrastructure Provider to have access to the Infrastructure Service Manager,\
**So that** they can manage all their cloud resources.

**Acceptance Criteria**

**AC1 The Infrastructure Service Manager** **is available to authorised Infrastructure** **Provider Participant Type**

1. **Given** a user is authenticated within an Infrastructure Provider Participant Type,\
   **When** the user role is assigned to INFRA_ADMIN, or INFRA_DEPLOYER\
   **Then** the Infrastructure Service Manager is displayed on their Simpl-Open landing page,\
   **And** this user can access the Infrastructure Service Manager.

**AC2 The Infrastructure Service Manager access is restricted for unauthorised user roles of an Infrastructure Provider**

2. **Given** a user is authenticated within an Infrastructure Provider Participant Type,\
   **When** they are not assigned to one of the roles authorised to access the Infrastructure Service Manager,\
   **Then** this service is not displayed on their Simpl-Open landing page,\
   **And** access to this service is denied.

**AC3 The Infrastructure Service Manager access is restricted for the users authorised other than an Infrastructure Provider**

3. **Given** a user is not authenticated as an Infrastructure Provider Participant Type,\
   **When** they access to their Simpl-Open landing page,\
   **Then** the Infrastructure Service Manager is not displayed on their Simpl-Open landing page,\
   **And** access to this service is denied.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
