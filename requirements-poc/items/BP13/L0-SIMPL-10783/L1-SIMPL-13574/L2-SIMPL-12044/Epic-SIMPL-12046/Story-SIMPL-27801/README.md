> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-12046](../README.md) / **Story-SIMPL-27801**

# SIMPL-27801: A Participant displays functionalities aligned with their Participant type on the landing page UI

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-12046](../README.md) (Epic) › **SIMPL-27801** (User story)

## Text

**As a** Participant,\
**I want to** use a landing page adapted to my Participant type,\
**So that** I can easily access the relevant Simpl-Open functionalities.

###### **Acceptance Criteria**

**AC1.1 - Participant access Landing Page UI**

1. **Given** I am authenticated as a Participant user\
   **When** I access the landing page of my Simpl-Open agent\
   **Then** the landing page is displayed\
   **And** the landing page shows all the functionalities adapted to my Participant type

**AC1.2 Negative case: Participant access denied**

2. **Given** a Participant's authentication token is not valid\
   **When** a Participant attempts to access the landing page of their Simpl-Open agent\
   **Then** the landing page is not displayed\
   **And** Simpl-Open responds with a message including information about the access denied

**AC2.1 Participant is redirected to the selected functionality UI**

3. **Given** I am authenticated as a Participant user\
   **And** I have successfully opened the landing page\
   **When** I select a functionality from the landing page\
   **Then** I am redirected to the UI of the selected functionality

**AC2.2 Negative scenario: Redirection to a functionality fails**

4. **Given** I am authenticated as a Participant user\
   **And** I have successfully opened the landing page\
   **And** I have selected a functionality from the landing page UI\
   **When** the request to access the corresponding functionality UI fails\
   **Then** Simpl-Open responds with a message including information about the failed access\
   **And** I am redirected towards the landing page

**Technical Info:**

- **Authentication source:** Authentication token (JWT)
- **Participant identification:** participantType injected from the helm charts
- **Navigation mechanism:** External redirection
- **Default landing page configuration** is provided per Participant type as defined in the requirements

**Security & Compliance:**

- **Authentication:** The Simpl-Open IAA solution is used for authentication when accessing the landing page
- Authorisation: Displayed functionalities depend on the authenticated Participant type
- **Configuration security:** Service URLs managed through deployment configuration
- **Sensitive data:** No sensitive participant data stored locally beyond authentication context

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-12046](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their Participant type |
