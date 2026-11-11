> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-28280](../README.md) / **Story-SIMPL-32348**

# SIMPL-32348: A Participant uses the landing page to access the ticketing system

**User story (implementable work)** · Development: **In development**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-28280](../README.md) (Epic) › **SIMPL-32348** (User story)

## Text

**As a** Participant,\
**I want to** navigate to the Simpl-Open ticketing system via my landing page,\
**So that** I can manage all my support tickets

###### **Acceptance Criterion 1 - Ticketing system configured**

1. **Given** I am authenticated as a Participant user\
   **And** the URL of the ticketing system is configured by the Participant\
   **When** I access my Simpl-Open agent landing page\
   **And** I choose to access the ticketing system\
   **Then** I am redirected to the ticketing system

###### **Acceptance Criterion 2 - Ticketing system not configured**

2. **Given** I am authenticated as a Participant user\
   **And** the URL of the ticketing system is not configured by the Participant\
   **When** I access my Simpl-Open agent landing page\
   **And** I choose to access the ticketing system\
   **Then** Simpl-Open responds with a message including information about unavailability of the ticketing system

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-28280](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] |
