> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27155](../../README.md) / [Epic-SIMPL-27204](../README.md) / **Story-SIMPL-28590**

# SIMPL-28590: [API] Data Provider creates a contract consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27155](../../README.md) (L2) › [SIMPL-27204](../README.md) (Epic) › **SIMPL-28590** (User story)

## Text

*This user story describes the full API contract for creating a contract consent assertion, which is designed to support multiple parties within a single assertion.*

*However, the Simpl-Open contract component will be scoped to a single use case: creating a consent assertion with one party of type `PROVIDER` and one party of type CONSUMER (as covered by AC 1).*

See also "Important notes on party design" in SIMPL-27234.\
----\
**As a**

dataspace agent component

**I want to**

via API, create a contract consent assertion providing its name, description, metadata, and the Consumer party bound by that contract

**So that**

the assertion can be linked to a specific contract and its status can be used to enforce access control decisions exclusively for the bound Consumer

**Acceptance Criteria**

1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the body of the request contains\
    the name  (mandatory / min length 3 / max length 255)\
    the description  (mandatory / min length 3 / max length 2048)\
    the type valued to CONTRACT\
    a list of key-value metadata (key min length 1 / key max length 22 / value min length 1 / value max length 2048)\
    a list of parties with one participant with\
   FIRST PARTICIPANT TYPE\
     the participantType equals to PROVIDER\
      the participantId equals to the participant ID of the data provider\
      a notificationRequired flag set to true or false\
      an acknowledgementRequired flag set to true\
      an acknowledgementStatus field valued to ACTIVE\
   SECOND PARTICIPANT TYPE\
     the participantType equals to CONSUMER\
      the participantId equals to the participant ID of the consumer\
      a notificationRequired flag set to true or false\
      an acknowledgementRequired flag set to false\
      an acknowledgementStatus field valued to NOT_REQUIRED\
   **THEN** I obtain a 201 Created response\
   **AND** the body of the response contains the details of the created assertion\
   **AND** the created assertion has an ACTIVE status
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the body of the request contains the values listed in AC1\
   **AND** the acknowledgementStatus of the PROVIDER is WITHDRAWN\
   **THEN** I obtain a 201 Created response\
   **AND** the body of the response contains the details of the created assertion\
   **AND** the created assertion has a WITHDRAWN status
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** a party acknowledgementRequired flag set to true\
   **AND** the party acknowledgementStatus field with no value\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that a value for the status must be provided
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the name or descriptions do not comply with the required presence or field length\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the key-value metadata list contains duplicated keys (case insensitive match)\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the type of the assertion is CONTRACT\
   **AND** the request does not contain at least one provider and one consumer\
   **THEN** I obtain a 400 bad request response\
   **AND** the response contains an error mentioning that a PROVIDER and a CONSUMER must be included in the request
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the type of the assertion is CONTRACT\
   **AND** the request contains a list of parties\
   **AND** the request does not contain the party identifying the current participant\
   **THEN** I obtain a 400 bad request response\
   **AND** the response contains an error mentioning that the current participant party must be included in the request as a PROVIDER
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the type of the assertion is CONTRACT\
   **AND** the acknowledgementRequired flag of the CONSUMER party is true\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that the consumer cannot acknolwedge a contract consent assertion
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the request contains a list of parties\
   **AND** the request contains other parties besides the PROVIDER identifying the current participant and CONSUMER party\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that the resource consent assertion must contain only one party of type PARTICIPANT and one party of type CONSUMER
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **AND** the participant is not onboarded (the GET tier1/v2/participant API of the authentication provider returns 404 or an empty participant id)\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** at the participant\
   **THEN** I obtain a 422 Unprocessable Entity\
   **AND** the body of the response contains "Participant is not onboarded. Consent assertion cannot be created"
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** the description or the name or the type of the assertion is missing from the request\
   **THEN** I obtain a 400 Bad Request
1. **GIVEN** I am an agent component\
   **WHEN** I invoke the POST /tier1/v1/consentAssertions API\
   **AND** at least one between the participant type / the participant ID / the acknowledgement flags / the notification required flags are missing from one of the parties\
   **THEN** I obtain a 400 Bad Request

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27204](../README.md) (Epic) Data Provider creates a contract consent assertion |
