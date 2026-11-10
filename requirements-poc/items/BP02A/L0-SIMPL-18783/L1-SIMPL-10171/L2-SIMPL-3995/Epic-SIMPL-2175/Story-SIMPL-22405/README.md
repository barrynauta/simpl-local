> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-22405**

# SIMPL-22405: [API][IMPROVEMENT] Dataspace Governance Authority updates onboarding expirationTimeframe and credentialId

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-22405** (User story)

## Text

*This change is required to correctly support the multiple-credential onboarding model, replacing the earlier v1 approach that assumed a single credential per onboarding request.*

*This user story extends the existing onboarding update API previously exposed as PATCH /onboardingRequests/{onboardingRequestId}/expirationTimeframe. The API is now consolidated into PATCH /onboardingRequests/{onboardingRequestId} to support partial updates of additional onboarding request data, specifically the association of a credentialId.*\
*The new endpoint enables updating the expiration timeframe and the bound credential independently or together, while preserving backward compatibility and allowing a smooth deprecation of the previous endpoint without disrupting ongoing onboarding processes.*

**As a** Dataspace Governance Authority user with the Tier2 authorisation operator (NOTARY) or as a user responsible for onboarding a dataspace participant\
**I want to** via an API, update the expirationTimeframe and the credentialId stored on an onboarding request\
**So that** I can store and update the onboarding requests data

**Acceptance Criteria**

1. **Given** I am an authenticated user with the role NOTARY\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the request contains a JSON body at least one of the fields\
   *expirationTimeframe* (positive integer number, min 0, max 1000) time frame for automatic rejection of the onboarding request\
   *expirationTimeframeTimeUnit* (enum with possible values equal to HOUR, DAY and YEAR) timeframe unit of measure\
   *credentialId* (Base58BTC of the SHA-384 of the certificate) the unique identifier of the credential bound to the onboarding request\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 200 OK\
   **And** the response body contains the full onboarding request representation\
   **And** the expirationTimeframe, expirationTimeframeTimeUnit or credentialId fields are updated with the values provided in the request body\
   **And** any field not provided in the request body remains unchanged

2. **Given** I am an authenticated user with the role APPLICANT\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the onboarding request is created by me\
   **And** the request contains a JSON body with the field\
   *credentialId* (Base58BTC of the SHA-384 of the certificate) the unique identifier of the credential to bound to the onboarding request\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 200 OK\
   **And** the response body contains the full onboarding request representation\
   **And** the credentialId field is updated with the value provided in the request body

3. **Given** I am an authenticated user with the role NOTARY\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the request contains a JSON body with invalid expirationTimeframe (e.g. negative number, string values), expirationTimeframeTimeUnit (e.g. value not in the possible enumeration values), or credentialId (e.g. string not representing a valid Base58BTC)\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

4. **Given** I am an authenticated user with the role NOTARY\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the request contains a JSON body that does not contain any of the supported fields (expirationTimeframe, expirationTimeframeTimeUnit, credentialId)\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

5. **Given** I am an authenticated user with the role NOTARY\
   **And** the specified onboardingRequestId is not a valid UUID\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

6. **Given** I am an authenticated user with the role APPLICANT\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the onboarding request is created by me\
   **And** the request contains a JSON body with an invalid credentialId (e.g. string not representing a valid Base58BTC)\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

7. **Given** I am an authenticated user with the role APPLICANT\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the onboarding request is created by me\
   **And** the request contains a JSON body at least one of the fields expirationTimeframe or expirationTimeframeTimeUnit\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

8. **Given** I am an authenticated user with the role APPLICANT\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the onboarding request is created by me\
   **And** the request contains a JSON body that does not contain any of the supported fields (credentialId)\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

9. **Given** I am an authenticated user with the role APPLICANT\
   **And** the specified onboardingRequestId is not a valid UUID\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

10. **Given** I am an authenticated user with the role APPLICANT\
   **And** an onboarding request with the specified onboardingRequestId exists\
   **And** the onboarding request is not created by me\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request\
   **And** the system doesn't change the data stored in the onboarding request

11. **Given** I am an authenticated user with the role APPLICANT\
   **And** the specified onboardingRequestId is a valid UUID\
   **And** no onboarding request exists with that identifier\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 400 Bad Request *(note to the testers this prevents enumeration from APPLICANTs of the existing onboarding requests)*

12. **Given** I am an authenticated user with the role NOTARY\
   **And** the specified onboardingRequestId is a valid UUID\
   **And** no onboarding request exists with that identifier\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 404 Not Found

13. **Given** that I am not authenticated\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 401 Unauthorised

14. **Given** I am an authenticated user without the NOTARY or APPLICANT role\
   **When** I send PATCH /onboardingRequests/{onboardingRequestId}\
   **Then** the API returns 403 Forbidden

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
