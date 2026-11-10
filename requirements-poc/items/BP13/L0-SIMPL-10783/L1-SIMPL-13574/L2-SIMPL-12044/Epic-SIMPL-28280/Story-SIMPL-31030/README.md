> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-31030**

# SIMPL-31030: The Catalogue Search service is available to authorised users of Consumer and Provider Participant Types

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-31030** (User story)

## Text

**As a** Simpl-Open administrator\
**I want** authorised users of a Provider and a Consumer to have access to the Catalogue Search service,\
**so that** they can perform only the search activity across own data catalogue.

**Acceptance Criteria**

**AC1 The Catalogue Search service is available to authorised Consumer Participant Type**

**Given** a user is authenticated within a Consumer Participant Type,\
**When** the user role is assigned to CATALOGUE_SEARCHER, SD_CONSUMER, RESEARCHER, or Ro-MU-A,\
**Then** the Catalogue Search service is displayed on their Simpl-Open landing page,\
**and** this user can access the Catalogue Search service.

**AC2** The Catalogue Search service is available to authorised Provider Participant Type

**Given** a user is authenticated within a Provider Participant Type,\
**When** the user role is assigned to CATALOGUE_SEARCHER, SD_PUBLISHER, RESEARCHER, or Ro-Pa-A,\
**Then** the Catalogue Search service is displayed on their Simpl-Open landing page,\
**and** this user can access the Catalogue Search service.

**AC3 The Catalogue Search service access is restricted for unauthorised user roles**

**Given** a user is authenticated within a Consumer or a Provider Participant Type,\
**When** they are not assigned to one of the roles authorised to access the Catalogue Search service,\
**Then** the Catalogue Search service is not displayed on their Simpl-Open landing page,\
**and** access to this service is denied.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
