<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.1 The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b51-the-governance-authority-validates-the-schema-of-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority checks the schema of a new resource description or a new version of a resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.1 The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b51-the-governance-authority-validates-the-schema-of-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog it is item SIMPL-17332, status **Defined**. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** automatically check the schema of a new resource description or new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

**Scenario:** The Governance Authority checks that the schema is active for a new resource description or a new version of a resource description with an active schema (Schema status validation - positive case)\
When I receive a new resource description or a new version of a resource description with an active schema\
and the Provider is authorised with identity attributes to manage resource descriptions\
then it is checked that the schema of that resource description is active\
and schema status validation for the resource description or new version of a resource description has the result "valid".

**Scenario:** The Governance Authority checks that the schema is active for a new resource description or a new version of a resource description with an inactive schema (Schema status validation - negative case)\
When I receive a new resource description or a new version of a resource description with an inactive schema\
and the Provider is authorised with identity attributes to manage resource descriptions\
then it is checked that the schema of that resource description is active\
and schema status validation for the resource description or new version of a resource description has the result "invalid".

**Scenario:** The Governance Authority checks that the schema of a new version of a resource description is of the same resource type as for the previous version for a new version of a resource description with the same schema as the previous version (Schema type validation - positive case)\
When I receive a new version of a resource description with the same resource type as the previous version\
and the Provider is authorised with identity attributes to manage resource descriptions\
then it is checked that the schema of that resource description has the same resource type as the previous version\
and schema type validation for the new version of the resource description has the result "valid".

**Scenario:** The Governance Authority checks that the schema of a new version of a resource description is of the same resource type as for the previous version for a new version of a resource description with a different schema as the previous version (Schema type validation - negative case)\
When I receive a new version of a resource description with a different resource type as the previous version\
and the Provider is authorised with identity attributes to manage resource descriptions\
then it is checked that the schema of that resource description has the same resource type as the previous version\
and schema type validation for the new version of the resource description has the result "invalid".

## User stories

1. [[API] The Governance Authority checks the schema of a new resource description or a new version of a resource description](#user-story-1-api-the-governance-authority-checks-the-schema-of-a-new-resource-description-or-a-new-version-of-a-resource-description) (Ready for DG Acceptance)

### User story 1: [API] The Governance Authority checks the schema of a new resource description or a new version of a resource description

**Status:** Ready for DG Acceptance (backlog item SIMPL-21592)

#### Storycard

**As** Governance Authority\
**I want to** automatically check the schema of a new resource description or new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

scenarios to check for both - new resource description and new version of a resource description

- 201 - valid/invalid
- 422 - Schema errors
   - Schema validation:
      - "Schema error: payload must include 'dct:schemaName' in credentialSubject.dct:conformsTo"
      - "Schema error: no published schema found for name: ..."
      - "Schema error: ..." plus SHACL validation report
      - "Schema error: unknown"
   - Ontology validation:
      - "Inference model validation error..."
      - "Domain validation error: ... Expected domain: ..."

- 400 - Syntax / Parsing Error
- 403 - Unauthorized
- 500 - Internal Server Error

**Scenario:** The Governance Authority checks that the schema for a new resource description or a new version of a resource description with an active schema (Schema status validation - positive case)

1. **Given** the Provider is authorised with identity attributes to manage resource descriptions\
   **When** I receive a new resource description or a new version of a resource description with an active schema\
   **And** schema validation is done\
   **Then** Simpl-Open should return with the positive publishing result with the code "201"\
   **And** the new resource description or the new version of the resource description is created

**Scenario:** The Governance Authority checks that the schema for a new resource description or a new version of a resource description with an inactive schema (Schema status validation - negative case)

2. **Given** the Provider is authorised with identity attributes to manage resource descriptions\
   **When** I receive a new resource description or a new version of a resource description with an inactive schema\
   **And** schema validation is done\
   **Then** Simpl-Open should return the verification results and the details together with error code "422"\
   **And** no new resource description or new version of the resource description is created

**Scenario:** The Governance Authority checks that the schema for a new resource description or a new version of a resource description but the Provider is unauthorised (Schema status validation - negative case)

3. **Given** the Provider is not authorised with identity attributes to manage resource descriptions\
   **When** I receive a new resource description or a new version of a resource description\
   **Then** Simpl-Open should return with error code 403 to indicate the operation is not authorized\
   **And** no new resource description or new version of the resource description is created

**Scenario:** The Governance Authority checks the schema for a new resource description or a new version of a resource description with an invalid JSON syntax (Schema status validation - negative case)

4. **Given** the Provider is authorised with identity attributes to manage resource descriptions\
   **When** I receive a new resource description or a new version of a resource description\
   **And** the payload has invalid JSON syntax\
   **Then** `Simpl-Open should return with HTTP statu 400 Bad Request to indicate the syntax or parsing error`\
   **And** no new resource description or new version of the resource description is created

**Scenario:** The Governance Authority checks the schema for a new resource description or a new version of a resource description and an unexpected failure happens during creation (Schema status validation - negative case)

5. **Given** the Provider is authorised with identity attributes to manage resource descriptions\
   **When** I receive a new resource description or a new version of a resource description with an active schema\
   **And** An unexpected internal server error occurs during processing\
   **Then** Simpl-Open should return with HTTP status `500 Internal Server Error`\
   **And** no resource description or a new version of the resource description is created

(internal page)

---

Back to [5B.5.1 The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b51-the-governance-authority-validates-the-schema-of-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
