> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-26083](../../../README.md) / [L2-SIMPL-27195](../../README.md) / [Epic-SIMPL-27206](../README.md) / **Story-SIMPL-28591**

# SIMPL-28591: [API] Data Provider updates consent assertion status

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-26083](../../../README.md) (L1) › [SIMPL-27195](../../README.md) (L2) › [SIMPL-27206](../README.md) (Epic) › **SIMPL-28591** (User story)

## Text

**As a** Data Provider End User with the Consent Manager role (CONSENT_M)\
**I want** via API, to update the acknowledgement status of my party within a consent assertion\
**So that** the overall assertion status is recalculated to reflect my acknowledgement decision and access control decisions are enforced accordingly

**Acceptance Criteria**

1. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** my participant is included in the assertion as a party with acknowledgementRequired set to true\
   **And** the body of the request contains a status valued to ACTIVE\
   **Then** I obtain a 200 OK response\
   **And** the body of the response contains the updated assertion details\
   **And** my party acknowledgementStatus is updated to ACTIVE

2. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** my participant is included in the assertion as a party with acknowledgementRequired set to true\
   **And** the body of the request contains a status valued to WITHDRAWN\
   **Then** I obtain a 200 OK response\
   **And** my party acknowledgementStatus is updated to WITHDRAWN

3. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** my party acknowledgementStatus is successfully updated\
   **And** all parties with acknowledgementRequired set to true have their status valued to ACTIVE\
   **Then** the overall assertion status is set to ACTIVE

4. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** my party acknowledgementStatus is successfully updated\
   **And** at least one party with acknowledgementRequired set to true has their status valued to WITHDRAWN\
   **Then** the overall assertion status is set to WITHDRAWN

5. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** my participant is included in the assertion as a party with acknowledgementRequired set to false\
   **Then** I obtain a 403 Forbidden response\
   **And** the response contains an error mentioning that the calling party is not required to provide an acknowledgement for this assertion

6. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** my participant is not included in the assertion as a party\
   **Then** I obtain a 403 Forbidden response\
   **And** the response contains an error mentioning that the calling participant is not a party of this assertion

7. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the consentAssertionId does not correspond to an existing assertion\
   **Then** I obtain a 404 Not Found response

8. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the body of the request contains a status valued to something other than ACTIVE or WITHDRAWN\
   **Then** I obtain a 400 Bad Request response\
   **And** the response contains an error mentioning that the only accepted values are ACTIVE and WITHDRAWN

9. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the body of the request does not contain a status value\
   **Then** I obtain a 400 Bad Request response\
   **And** the response contains an error mentioning that the status field is mandatory

10. **Given** I am an unauthenticated user\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **Then** I receive a 401 Unauthorized response

11. **Given** I am an authenticated user without the CONSENT_M role\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **Then** I receive a 403 Forbidden response

12. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the body contains the WITHDRAWN status\
   **And** the party status is already WITHDRAWN\
   **Then** I obtain a 422 Unprocessable Entity error

13. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the body contains the ACTIVE status\
   **And** the party status is already ACTIVE\
   **Then** I obtain a 422 Unprocessable Entity error

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27206](../README.md) (Epic) Data Provider withdraws and reactivates consent assertions |
