> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27154](../../README.md) / [Epic-SIMPL-27203](../README.md) / **Story-SIMPL-28589**

# SIMPL-28589: [API] Data Provider creates a resource consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27154](../../README.md) (L2) › [SIMPL-27203](../README.md) (Epic) › **SIMPL-28589** (User story)

## Text

*This user story describes the full API contract for creating a resource consent assertion, which is designed to support multiple parties within a single assertion.*

*However, the Simpl-Open frontend implementation will be scoped to a single use case: creating a consent assertion with one party of type `PROVIDER` (as covered by AC 1).* See also "Important notes on party design" in SIMPL-27234.\
----\
**As a**

Data Provider End User with the Consent Manager role (CONSENT_M)

**I want to**

via API, create a resource consent assertion providing its name, description, metadata and parties

**So that**

the assertion can be linked to a data resource description and its status can be used to enforce access control decisions for all Consumers

**Acceptance Criteria**

1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the body of the request contains\
   the name (mandatory / min length 3 / max length 255)\
   the description (mandatory / min length 3 / max length 2048)\
   the type valued to RESOURCE\
   a list of key-value metadata (key min length 1 / key max length 22 / value min length 1 / value max length 2048)\
   a list of parties with one participant with\
   the participantType equals to PROVIDER\
   the participantId equals to the participant ID of the data provider\
   a notificationRequired flag set to false or true\
   an acknowledgementRequired flag set to true\
   an acknowledgementStatus field valued to ACTIVE\
   **THEN** I obtain a 201 Created response\
   **AND** the body of the response contains the detailed of the created assertion\
   **AND** the created assertion has an ACTIVE status
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the body of the request contains the values listed in AC1\
   **AND** the party acknowledgementStatus field valued to WITHDRAWN\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the name or descriptions do not comply with the required presence or field length\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the key-value metadata list contains duplicated keys (case insensitive match)\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the type of the assertion is RESOURCE\
   **AND** the request contains no parties\
   **THEN** I obtain a 400 bad request response\
   **AND** the response contains an error mentioning that the current participant party must be included in the request as PROVIDER
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the body of the request contains the values listed in AC1\
   **AND** the acknowledgementRequired flag set to false\
   **THEN** I obtain a 400 Bad request\
   **AND** the created assertion has a WITHDRAWN status\
   **AND** the response contains an error mentioning that the PROVIDER party must be able to acknowledge a resource consent assertion
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the party acknowledgementRequired flag set to true\
   **AND** the party acknowledgementStatus field with no value\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that a value for the status must be provided
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the request contains a list of parties\
   **AND** the request does not contain the PROVIDER party identifying the current participant\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that the current participant party must be included in the request as PROVIDER
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the request contains a list of parties\
   **AND** the request contains other parties besides the PROVIDER party identifying the current participant\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that the resource consent assertion can contain only one party of type PARTICIPANT
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the description the name or the type of the assertion is missing from the request\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** at least one between the participant type / the participant ID / the acknowledgement flags / the notification required flags and status is missing from the party -one of the parties-\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **AND** the participant is not onboarded (the GET tier1/v2/participant API of the authentication provider returns 404 or an empty participant id)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** at the participant\
   **THEN** I obtain a 422 Unprocessable Entity\
   **AND** the body of the response contains "Participant is not onboarded. Consent assertion cannot be created"
1. **GIVEN** I am an unauthenticated user\
   **WHEN** I invoke a POST /tier1/v1/consentAssertions\
   **THEN** I receive a 401 Unauthorized response
1. **GIVEN** I am an authenticated user without CONSENT_M role\
   **WHEN** I send a POST /tier1/v1/consentAssertions\
   **THEN** I receive a 403 Forbidden response

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27203](../README.md) (Epic) Data Provider creates a resource consent assertion |
