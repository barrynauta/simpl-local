> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-17306**

# SIMPL-17306: [API] Dataspace Governance Authority retrieves the list of dataspace identity attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-17306** (User story)

## Text

*This user story outlines the adaptation and migration of the v1 API implementation to v2, based on guidelines derived from the API definition tasks. Linked to this, there are the API definition tasks that explain what this API will substitute.*

**As a**\
Dataspace Governance Authority end-user with Tier 2 authorisation operator (NOTARY), Tier 2 identity attributes manager (IATTR_M) roles

**I want to**\
via an API, retrieve the list of all identity attributes defined by the dataspace governance authority

**So that**\
I can view the identity attributes available in the dataspace

**Acceptance Criteria:**

1. **GIVEN** I am an authenticated user with one of the roles NOTARY, or IATTR_M\
   **WHEN** I send a GET /identityAttributes request\
   **THEN** I receive a 200 OK response\
   **AND** I receive a paginated list of identity attributes where each item contains:
- code (string, max length 255) - unique code of the attribute
- name (string, max length 255) - human-readable name
- description (string) - human-readable description
- assignableToRoles (boolean) - whether it can be assigned to roles
- enabled (boolean) - whether it is enabled
- creationTimestamp (timestamp) - creation time (ISO 8601 / RFC 3339)
- updateTimestamp (timestamp) - last update time (ISO 8601 / RFC 3339)
- right (boolean, read-only) - whether it represents a legal right
- builtIn (boolean, read-only) - whether it is built-in and immutable\
   **AND** pagination information: self, page, pageSize, total, and navigation URIs first, last, prev, next
1. **GIVEN** I am an authenticated user with the NOTARY, or IATTR_M roles\
   **WHEN** I call GET /identityAttributes\
   **AND** I specify one or more filter parameters (code, name, assignableToRoles, enabled)\
   **THEN** I receive a 200 OK response\
   **AND** the returned items match the filter criteria
1. **GIVEN** I am an authenticated user with the NOTARY, or IATTR_M roles\
   **WHEN** I call GET /identityAttributes\
   **AND** my filter does not match any identity attribute\
   **THEN** I receive a 200 OK response\
   **AND** I receive an empty item list
1. **GIVEN** I am an authenticated user with the NOTARY, or IATTR_M roles\
   **AND** I provide pagination parameters (page, pageSize)\
   **WHEN** I call GET /identityAttributes\
   **THEN** the response reflects the requested slice of results and includes navigation links (first, last, prev, next) consistent with the pagination state
1. **GIVEN** I am an authenticated user\
   **AND** I provide one or more invalid filter or pagination values (e.g., non-boolean for enabled/assignableToRoles, malformed timestamps, non-integer page/pageSize)\
   **WHEN** I call GET /identityAttributes\
   **THEN** I receive a 400 Bad Request response\
   **AND** the response includes the reason behind the error
1. **GIVEN** I am an unauthenticated caller\
   **WHEN** I request GET /identityAttributes\
   **THEN** I receive a 401 Unauthorized response
1. **GIVEN** I am an authenticated user without any of the roles NOTARY, or IATTR_M\
   **WHEN** I request GET /identityAttributes\
   **THEN** I receive a 403 Forbidden response

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
