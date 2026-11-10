> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-28207**

# SIMPL-28207: A Participant displays the functionalities aligned with their user role on the landing page UI

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-28207** (User story)

## Text

**As a** Participant,\
**I want to** use a landing page adapted to my Participant type and user role,\
**So that** I can easily access the relevant Simpl-Open functionalities.

###### Acceptance Criteria

###### AC1.1 - Display eligible tiles

1. **Given** I am authenticated\
   **And** the Landing Page has a Participant type configured for its deployment\
   **When** I access the Landing Page\
   **Then** I see only tiles configured for that Participant type for which I meet the configured eligibility requirement of the UI the tile opens\
   **And** a tile with no configured eligibility requirement is hidden unless it is explicitly marked public\
   **And** a tile is hidden when its eligibility cannot be determined.

###### AC1.2 - Apply eligibility during customisation

2. **Given** I am authenticated\
   **When** I customise my Landing Page\
   **Then** I can view and customise only tiles available to me under AC1.1.

###### Technical Info

- **Authentication:** Keycloak; user roles are read from the JWT `client-roles` claim.
- **Participant type:** Configured for the deployment through Helm; not read from the JWT.
- **Tile configuration:** A single deployment-managed configuration defines tile details, enabled state, target URL and required roles.
- **Role matching:** Each tile may list multiple roles; holding any one listed role makes the user eligible to see it.
- **Navigation:** Selecting a tile redirects to its configured service UI.
- **Personalisation:** Saved preferences affect tile order and personal visibility only after eligibility filtering.

###### Security & Compliance

- Each target service independently enforces access.
- Missing or invalid required-role mappings do not make a restricted tile visible.
- No sensitive Participant data is stored in tile configuration or personalisation preferences.

N.B. This story extends SIMPL-27801 by introducing role-based visibility of functionalities on the landing page. In addition to the Participant type.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
