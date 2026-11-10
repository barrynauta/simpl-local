> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-28595**

# SIMPL-28595: [API] Data provider retrieve the details of a consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-28595** (User story)

## Text

**As a**

Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions

**I want to**

via API, view the details of a consent assertion

**so that**

I can have a complete overview of the assertion, including parties involved and its status

**Acceptance Criteria**

1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **AND** a consent assertion has been created in the past\
   **WHEN** I GET /tier1/v1/consentAssertions/{consentAssertionId} on that assertion\
   **THEN** I receive a 200 Ok response\
   **AND** the body of the response contains\
   the assertion ID (UUID)\
   the assertion name (String)\
   the assertion description (String)\
   the assertion type (String)\
   the assertion status (String)\
   the list of key-value metadata pairs (list of string keypairs)\
   the list of parties, with each containing\
   the id of the party (UUID)\
   the participantId (UUID)\
   the participant type (string, PROVIDER or CONSUMER)\
   the notificationRequired boolean flag\
   the acknowledgementRequired boolean flag\
   the acknowledgementStatus (NOT_REQUIRED, ACTIVE, WITHDRAWN)\
   the creation timestamp\
   the update timestamp
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the GET /tier1/v1/consentAssertions/{consentAssertionId} API\
   **AND** the consentAssertionId does not map to an existing assertion\
   **THEN** I obtain a 404 Not Found
1. **GIVEN** I am an unauthenticated user\
   **WHEN** I invoke a GET /tier1/v1/consentAssertions/{consentAssertionId}\
   **THEN** I receive a 401 Unauthorized response
1. **GIVEN** I am an authenticated user without CONSENT_M role\
   **WHEN** I send a GET /tier1/v1/consentAssertions/{consentAssertionId}\
   **THEN** I receive a 403 Forbidden response

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
