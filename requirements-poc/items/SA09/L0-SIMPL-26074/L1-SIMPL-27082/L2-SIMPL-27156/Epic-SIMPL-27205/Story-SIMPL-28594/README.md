> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-28594**

# SIMPL-28594: [API] Data Provider lists and filters consent assertions

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-28594** (User story)

## Text

**As a** Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions\
**I want to** via API, view the list of consent assertions created within the system\
**So that** I have a clear overview of the consent assertions created within my agent, which allows me to properly manage the assertions.

**Acceptance Criteria**

1. **Given** I am an authenticated user with CONSENT_M role\
   **When** I send a GET /tier1/v1/consentAssertions API request\
   **Then** I receive a 200 OK response\
   **And** I receive a paginated list of consent assertions with:\
   the assertion ID (UUID)\
   the assertion name (String)\
   the assertion description (String)\
   the assertion type (String)\
   the assertion status (String)\
   the list of key-value metadata pairs (list of string keypairs)\
   the list of parties, with each containing\
   the ID of the party (UUID)\
   the participantId (UUID)\
   the participant type (string, PROVIDER or CONSUMER)\
   the notificationRequired boolean flag\
   the acknowledgementRequired boolean flag\
   the acknowledgementStatus (NOT_REQUIRED, ACTIVE, WITHDRAWN)\
   **And** pagination metadata: page, pageSize, total, and navigation URIs (first, last, prev, next)

2. **Given** I am an authenticated user with CONSENT_M role\
   **When** I send a GET /tier1/v1/consentAssertions\
   **And** I specified a search filter (name, status, type, metadata)\
   **Then** I receive a 200 OK response\
   **And** I receive a paginated list of consent assertions that match the filter criteria{}

3. **Given** I am an authenticated user with CONSENT_M role\
   **When** I send a GET /tier1/v1/consentAssertions\
   **And** I specified one or more sorting criteria (name, status, type, creationTimestamp, updateTimestamp, all in ascending or desending order)\
   **Then** I receive a paginated list of consent assertions sorted by the given criteria

4. **Given** I am an unauthenticated user\
   **When** I invoke a GET /tier1/v1/consentAssertions\
   **Then** I receive a 401 Unauthorized response

5. **Given** I am an authenticated user without CONSENT_M role\
   **When** I send a GET /tier1/v1/consentAssertions\
   **Then** I receive a 403 Forbidden response

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
