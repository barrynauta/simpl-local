> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27154](../../README.md) / [Epic-SIMPL-27203](../README.md) / **Story-SIMPL-28598**

# SIMPL-28598: [UI] Data Provider creates a resource consent assertion

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27154](../../README.md) (L2) › [SIMPL-27203](../README.md) (Epic) › **SIMPL-28598** (User story)

## Text

**As a** Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions\
**I want to** via UI, create a resource consent assertion by providing its name, description and metadata\
**So that** the assertion can be linked to a data resource description and its status can be used to enforce access control decisions for all Consumers

**Acceptance Criteria**

1. **Given** that I am logged in with the CONSENT_M role\
   **When** I open the page to create a consent assertion\
   **Then** I see a form that contains\
   the assertion name (mandatory, min length 3, max length 255)\
   the assertion type (blocked to RESOURCE and not modifiable)\
   an info icon near the assertion type label that, on hover, shows the message *"Resource consent assertions can be linked to resource descriptions. After creation, you will be able to reference this assertion in the UI to create resource descriptions"*\
   the assertion description (mandatory, min length 3, max length 2048)\
   a form that allows to insert a first key-value metadata pair (key min length 1, key max length 22, value min length 1, value max length 2048)\
   a button that allows me to add more metadata pairs to the assertion (metadata are not required, but must be filled properly. See the following ACs for details).\
   **And** I see a disabled Submit button

2. **Given** that I am logged in with the CONSENT_M role\
   **And** I open the page to create a consent assertion\
   **When** I fill in the name and description fields\
   **Then** the submit button becomes enabled

3. **Given** that I am logged in with the CONSENT_M role\
   **And** I open the page to create a consent assertion\
   **When** I fill two or more metadata key-value pairs with the same key (case-insensitive match)\
   **Then** the submit button becomes or remains disabled\
   **And** an error is shown saying that the metadata keys should be unique

4. **Given** that I am logged in with the CONSENT_M role\
   **And** I open the page to create a consent assertion\
   **When** I don't fill both the key and value of a metadata entry\
   **Then** the submit button becomes or remains disabled\
   **And** an error is shown saying that the metadata must contain key and value

5. **Given** that I am logged in with the CONSENT_M role\
   **And** I open the page to create a consent assertion\
   **When** I manage metadata sections\
   **Then** I can add or remove metadata entries before saving the assertion

6. **Given** that I am logged in with the CONSENT_M role\
   **And** I open the page to create a consent assertion\
   **When** I reach the maximum number of metadata entry configured in the application configuration (default to 10)\
   **Then** the UI prevents me from adding more metadata entry

7. **Given** that I am logged in with the CONSENT_M role\
   **When** I submit the new consent assertion\
   **Then** the frontend builds the participant party information by\
   calling GET tier1/v2/participant API of the authentication provider component to retrieve the participant ID (SIMPL-28808)\
   reading the participantType, notificationRequired, confirmationRequired from frontend configurations\
   **And** the frontend calls the POST tier1/v1/consentAssertions API that includes the user input data and the party information\
   **And** the frontend redirects the user to the consent assertions list page\
   **And** after the redirect a pop-up showing that the request completed appears

8. **Given** that I am logged in with the CONSENT_M role\
   **When** the API calls to tier1/v2/participant or GET /user fail for technical reasons (timeout or internal server errors)\
   **Then** a popup is shown saying that "The information to build the assertion cannot be fetched. Try again later."

9. **Given** that I am logged in with the CONSENT_M role\
   **And** the participant agent is not fully onboarded (note. GET tier1/v2/participant API of the authentication provider component returns an empty participant ID)\
   **When** I open the page to create a consent assertion\
   **Then** I am redirected to a new error page\
   **And** I see a message stating *"The Simpl-Open agent is not fully onboarded. Complete the onboarding of the agent to create the consent assertions"*

10. **Given** that I am not logged in with the CONSENT_M role\
   **When** I open the page to create a consent assertion\
   **Then** I see the "Role Restricted Access" page\
   **And** a message stating "Access denied. Your role does not grant permission to view this content."

11. **Given** that I am not logged in\
   **When** I open the page to create a consent assertion\
   **Then** I am redirected to the agent login page

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27203](../README.md) (Epic) Data Provider creates a resource consent assertion |
