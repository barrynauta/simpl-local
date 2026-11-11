> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-28602**

# SIMPL-28602: [UI] Data provider shows the details of a consent assertion

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-28602** (User story)

## Text

*This page will show both resource consent assertions and contract consent assertions. The main difference between the two is that:*

- *the RESOURCE consent assertion will contain only one party of type provider that is not notified on status change.*
- *the CONTRACT consent assertion will contain two party, one provider and one consumer. Only the consumer is notified on status change.*

*For a detailed overview on the relationship between an assertion and its parties, see also "Important notes on party design" in SIMPL-27234.*\
----

**As a** Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions\
**I want to** via UI, view the details of a consent assertion\
**So that** I can have a complete overview of the assertion, including parties involved and its status

**Acceptance Criteria**

1. **Given** I am logged in with the CONSENT_M role\
   **When** I open the detail page for a consent assertion\
   **Then** I see\
   the title of the assertion\
   the type of the assertion\
   the description of the assertion\
   a metadata section that allows seeing existing metadata\
   the assertion status\
   a button to update the assertion status (see SIMPL-28599 for behaviour)\
   the creation and update timestamps of the assertion\
   a table showing the list of parties involved in the assertion, showing for each party\
   a "Type" column that shows the party type (CONSUMER or PROVIDER)\
   a "Participant ID" column showing the ID of the participant\
   a "Notified on status change" column showing if the participant must be notified upon assertion status change\
   a "Edit" button

2. **Given** that I am logged in with the CONSENT_M role\
   **When** I click on the Edit button\
   **Then** I am redirected to the consent assertion update page

3. **Given** that I am logged in with the CONSENT_M role\
   **When** the detail page for a consent assertion\
   **And** there is no assertion with the ID provided in the URL\
   **Then** I see a 404 error page

4. **Given** that I am an authenticated user without the CONSENT_M role\
   **When** I open the detail page for a consent assertion\
   **Then** I see the "Role Restricted Access" page\
   **And** a message stating "Access denied. Your role does not grant permission to view this content."

5. **Given** that I am not logged in\
   **When** I open the detail page for a consent assertion\
   **Then** I am redirected to the agent login page

6. **Given** that I am logged in with the CONSENT_M role\
   **And** the participant agent is not fully onboarded (note. GET tier1/v2/participant API of the authentication provider component returns an empty participant ID or a 404)\
   **When** I open the detail page for a consent assertion\
   **Then** I am redirected to a new error page\
   **And** I see a message stating *"The Simpl-Open agent is not fully onboarded. Complete the onboarding of the agent to create the consent assertions"*

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
