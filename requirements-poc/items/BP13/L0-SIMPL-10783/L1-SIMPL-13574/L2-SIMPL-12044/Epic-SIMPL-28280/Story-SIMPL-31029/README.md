> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-31029**

# SIMPL-31029: The Resource Description Management service is available to authorised users of Provider Participant Type

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-31029** (User story)

## Text

**As a** Simpl-Open administrator\
**I want** authorised users of a Provider to have access to the Resource Description Management service,\
**So that** they can manage the resource descriptions in the data space catalogue.

**Acceptance Criteria**

**AC1 A Provider access to the Resource Description Management service**

1. **Given** a user is authenticated within the Provider Participant Type,\
   **When** the user role is assigned to SD_PUBLISHER, Ro-MU-A,or Ro-Pa-A,\
   **Then** the Resource Description Management service is displayed on their Simpl-Open landing page,\
   **And** this user can access the Resource Description Management service.

**AC2 The Resource Description Management service access is restricted for unauthorised user roles of a Provider**

2. **Given** a user is authenticated within the Provider Participant Type,\
   **When** they are not assigned to one of the roles authorised to access the Resource Description Management service,\
   **Then** this service is not displayed on their Simpl-Open landing page,\
   **And** access to this service is denied.

**AC3The Resource Description Management service access is restricted for the users authorised other than a Provider**

3. **Given** a user is authenticated as a Consumer Participant Type,\
   **When** they access to their Simpl-Open landing page,\
   **Then** the Resource Description Management service is not displayed on their Simpl-Open landing page,\
   **And** access to this service is denied.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
