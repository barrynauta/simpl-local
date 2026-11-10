> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-31034**

# SIMPL-31034: The Onboarding service is available to authorised Governance Authority user

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-31034** (User story)

## Text

**As a** Simpl-Open administrator\
**I want** an authorised Governance Authority participant type within the specific user roles to access the Onboarding service,\
**So that** Governance Authority can perform activities related to onboarding process of an applicant dataspace participant, follow the onboarding requests, define and change the onboarding procedure, in accordance with the user's assigned role.

###### Acceptance Criteria

###### AC1  The Onboarding service is available to authorised Governance Authority users

1. **Given** a user is authenticated with the Governance Authority Participant type,\
   **When** the user role is assigned the role NOTARY, T2IAA_M, or APPLICANT,\
   {}Then the Onboarding service is displayed on their Simpl-Open landing page,\
   **And** this user can access the Onboarding service.

###### AC2 The Onboarding service access is restricted for unauthorised user roles

2. **Given** a user is authenticated with the Governance Authority Participant type,\
   **When** they are not assigned to one of the roles authorised for the Onboarding service,\
   **Then** the Onboarding service is not displayed on their Simpl-Open landing page,\
   **And** access to this service is denied.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
