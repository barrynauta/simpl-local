<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.3 The Governance Authority validates the versioning of a new version of a Resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b53-the-governance-authority-validates-the-versioning-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority checks the version number of a new version of a Resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.3 The Governance Authority validates the versioning of a new version of a Resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b53-the-governance-authority-validates-the-versioning-of-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog (Jira) it is item SIMPL-17799. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** automatically check the version number of a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

**Scenario:** The Governance Authority checks the version number of a new version of a resource description(Version number validation - positive case)

1. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with a correct version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **Then** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "valid".

**Scenario:** The Governance Authority checks the version number of a new version of a resource description(Version number validation - negative case)

2. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with an incorrect version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **Then** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "invalid".

## User stories

1. [[API] The Governance Authority checks the version number of a new version of a Resource description](#user-story-1-api-the-governance-authority-checks-the-version-number-of-a-new-version-of-a-resource-description) · Developed

### User story 1: [API] The Governance Authority checks the version number of a new version of a Resource description

**Development:** Developed · backlog item SIMPL-21596

#### Storycard

**As** Governance Authority\
**I want to** automatically check the version number of a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

scenarios to check for both - new resource description and new version of a resource description

- 201 -  correct version
- 422 - incorrect version

- 400 - Syntax / Parsing Error
- 403 - Unauthorized
- 500 - Internal Server Error

**Scenario:** The Governance Authority checks the version number of a new version of a resource description, valid version number (Version number validation - positive case)

1. **Given** a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with a correct version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **And** the version numbering algorithm checks the version number of the new version of the Resource description\
   **Then** Simpl-Open should return with the positive publishing result with the code "201"\
   **And** the new resource description or the new version of the resource description is created

**Scenario:** The Governance Authority checks the version number of a new version of a resource description, invalid version number (Version number validation - negative case)

2. **Given** a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with an incorrect version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **And** the version numbering algorithm checks the version number of the new version of the Resource description\
   **Then** Simpl-Open should return the verification results and the details together with error code "422"\
   **And** no new resource description or new version of the resource description is created

**Scenario:** The Governance Authority checks the version number of a new version of a resource description, Provider is not authorized (Version number validation - negative case)

3. **Given** a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with an incorrect version number\
   **And** the Provider is not authorised with identity attributes to manage resource descriptions\
   **Then** Simpl-Open should return with error code 403 to indicate the operation is not authorized\
   **And** no new resource description or new version of the resource description is created

**Scenario:** The Governance Authority checks the schema for a new resource description or a new version of a resource description with an invalid JSON syntax (Schema status validation - negative case)

4. **Given** a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with a correct version number\
   **And** the Provider is not authorised with identity attributes to manage resource descriptions\
   **And** the payload has invalid JSON syntax\
   **Then** `Simpl-Open should return with HTTP status 400 Bad Request to indicate the syntax or parsing error`\
   **And** no new resource description or new version of the resource description is created

**Scenario:** The Governance Authority checks the schema for a new resource description or a new version of a resource description and an unexpected failure happens during creation (Schema status validation - negative case)

5. **Given** a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with a correct version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **And** An unexpected internal server error occurs during processing\
   **Then** Simpl-Open should return with HTTP status `500 Internal Server Error`\
   **And** no resource description or a new version of the resource description is created

#### Technical details

The version validation mechanism shall be implemented in a configurable and extensible manner so that the versioning strategy can be changed or replaced.

Approach is described here:

(internal page)

A http response with a suitable error code and a meaningful error description is sufficient.

API: [https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads)

Endpoint: POST /selfDescriptions/resource

Validations:

- incremental versioning - if latest version is 5 and I get 7 - REJECT
- numeric versioning - reject strings or other formats
- reject negative numbers

HTTP Status Code for successful validation: 201

HTTP Status Code for negative scenarios: 422

Update the OpenAPI File to reflect the Error Codes for status code 422 for this API: SIMPL-25210

---

Back to [5B.5.3 The Governance Authority validates the versioning of a new version of a Resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b53-the-governance-authority-validates-the-versioning-of-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
