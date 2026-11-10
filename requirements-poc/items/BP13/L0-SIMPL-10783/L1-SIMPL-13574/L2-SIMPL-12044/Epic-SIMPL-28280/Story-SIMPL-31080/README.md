> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-31080**

# SIMPL-31080: The Identity Provider service service is available to authorised Governance Authority user

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-31080** (User story)

## Text

**As a** Simpl-Open administrator\
**I want** an authorised Governance Authority participant type within the specific user roles to access the Identity Provider service,\
**so that** Governance Authority can perform activities related to the credentials for a newly onboarded participants, in accordance with the user's assigned role.

###### Acceptance Criteria

###### AC1  The Identity Provider service is available to authorised Governance Authority users

**Given** a user is authenticated with the Governance Authority Participant type,\
**When** the user role is assigned the role IATTR_M, NOTARY, T2IAA_M, or FED_M,\
**Then** the Identity Provider service is displayed on their Simpl-Open landing page,\
**and** this user can access the Identity Provider service.

###### AC2 The Identity Provider service access is restricted for unauthorised user roles

**Given** a user is authenticated with the Governance Authority Participant type,\
**When** they are not assigned to one of the roles authorised for the Identity Provider  service,\
**Then** the Identity Provider service is not displayed on their Simpl-Open landing page,\
**and** access to this service is denied.

###### AC3 The Identity Provider service access is restricted for unauthorised user types

**Given** a user is authenticated with a Consumer or a Provider Participant type,\
**When** they access to their Simpl-Open landing page,\
**Then** the Identity Provider service is not displayed on their Simpl-Open landing page,\
**and** access to this service is denied.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
