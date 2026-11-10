> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-26083](../../../README.md) / [L2-SIMPL-27195](../../README.md) / [Epic-SIMPL-27206](../README.md) / **Story-SIMPL-28591**

# SIMPL-28591: [API] Data Provider updates consent assertion status

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-26083](../../../README.md) (L1) › [SIMPL-27195](../../README.md) (L2) › [SIMPL-27206](../README.md) (Epic) › **SIMPL-28591** (User story)

## Text

**As a**\
Data Provider End User with the Consent Manager role (CONSENT_M)\
**I want**\
via API, to update the acknowledgement status of my party within a consent assertion\
**So that**\
the overall assertion status is recalculated to reflect my acknowledgement decision and access control decisions are enforced accordingly

**Acceptance Criteria**

1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** my participant is included in the assertion as a party with acknowledgementRequired set to true\
   **AND** the body of the request contains a status valued to ACTIVE\
   **THEN** I obtain a 200 OK response\
   **AND** the body of the response contains the updated assertion details\
   **AND** my party acknowledgementStatus is updated to ACTIVE
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** my participant is included in the assertion as a party with acknowledgementRequired set to true\
   **AND** the body of the request contains a status valued to WITHDRAWN\
   **THEN** I obtain a 200 OK response\
   **AND** my party acknowledgementStatus is updated to WITHDRAWN
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** my party acknowledgementStatus is successfully updated\
   **AND** all parties with acknowledgementRequired set to true have their status valued to ACTIVE\
   **THEN** the overall assertion status is set to ACTIVE
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** my party acknowledgementStatus is successfully updated\
   **AND** at least one party with acknowledgementRequired set to true has their status valued to WITHDRAWN\
   **THEN** the overall assertion status is set to WITHDRAWN
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** my participant is included in the assertion as a party with acknowledgementRequired set to false\
   **THEN** I obtain a 403 Forbidden response\
   **AND** the response contains an error mentioning that the calling party is not required to provide an acknowledgement for this assertion
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** my participant is not included in the assertion as a party\
   **THEN** I obtain a 403 Forbidden response\
   **AND** the response contains an error mentioning that the calling participant is not a party of this assertion
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** the consentAssertionId does not correspond to an existing assertion\
   **THEN** I obtain a 404 Not Found response
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** the body of the request contains a status valued to something other than ACTIVE or WITHDRAWN\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that the only accepted values are ACTIVE and WITHDRAWN
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** the body of the request does not contain a status value\
   **THEN** I obtain a 400 Bad Request response\
   **AND** the response contains an error mentioning that the status field is mandatory
1. **GIVEN** I am an unauthenticated user\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **THEN** I receive a 401 Unauthorized response
1. **GIVEN** I am an authenticated user without the CONSENT_M role\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **THEN** I receive a 403 Forbidden response
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** the body contains the WITHDRAWN status\
   **AND** the party status is already WITHDRAWN\
   **THEN** I obtain a 422 Unprocessable Entity error
1. **GIVEN** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** the body contains the ACTIVE status\
   **AND** the party status is already ACTIVE\
   **THEN** I obtain a 422 Unprocessable Entity error

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27206](../README.md) (Epic) Data Provider withdraws and reactivates consent assertions |
