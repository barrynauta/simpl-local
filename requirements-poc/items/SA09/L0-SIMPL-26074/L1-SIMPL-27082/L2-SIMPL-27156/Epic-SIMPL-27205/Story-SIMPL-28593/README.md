> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-28593**

# SIMPL-28593: [API] Data provider updates consent assertion

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-28593** (User story)

## Text

**As a** Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions\
**I want to** via API, update the details of a consent assertion including name, description and metadata\
**So that** I can correct or enrich the assertion's information without having to recreate it.

**Acceptance Criteria**

1. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId} API\
   **And** the body contains\
   the name (mandatory, min length 3, max length 255)\
   the description (mandatory, min length 3, max length 2048)\
   a list of key-value metadata (key min length 1, key max length 22, value min length 1, value max length 2048)\
   **Then** I obtain a 200 OK response\
   **And** the body contains the details of the assertion

2. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId} API\
   **And** the name or descriptions do not comply with the required presence or field length\
   **Then** I obtain a 400 Bad Request

3. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId} API\
   **And** the key-value metadata list contains duplicated keys\
   **Then** I obtain a 400 Bad Request

4. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId} API\
   **And** the consentAssertionId does not map to an existing assertion\
   **Then** I obtain a 404 Not Found

5. **Given** I am an unauthenticated user\
   **When** I invoke a PATCH /tier1/v1/consentAssertions/{consentAssertionId}\
   **Then** I receive a 401 Unauthorized response

6. **Given** I am an authenticated user without CONSENT_M role\
   **When** I send a PATCH /tier1/v1/consentAssertions/{consentAssertionId}\
   **Then** I receive a 403 Forbidden response

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
