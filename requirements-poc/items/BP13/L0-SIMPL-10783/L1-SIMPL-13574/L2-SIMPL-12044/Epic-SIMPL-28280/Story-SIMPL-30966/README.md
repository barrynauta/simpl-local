> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-30966**

# SIMPL-30966: The Users&Roles service available to authorised users across all Participant Types

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-30966** (User story)

## Text

**As a** Simpl-Open administrator\
**I want** all Simpl-Open participant types within the specific user roles to access the Users&Roles service,\
**So that** they can define roles used by the Authorisation Tier 1, manage the roles assignment of Tier 1 Authentication Provider end users and assign identity attributes to roles.

**Acceptance Criteria**

**AC1 The Users&Roles** **service is accessed by authorised users across all Participant Types**

1. **Given** a user is authenticated within any Participant Type,\
   **When** the user role is assigned to the role T1UAR_M,\
   **Then** the Users&Role service is displayed on their Simpl-Open landing page,\
   **And** this user can access the Users&Role service.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
