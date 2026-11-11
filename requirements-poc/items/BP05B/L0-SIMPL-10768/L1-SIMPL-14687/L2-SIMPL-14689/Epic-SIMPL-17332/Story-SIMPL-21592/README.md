> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14689](../../README.md) / [Epic-SIMPL-17332](../README.md) / **Story-SIMPL-21592**

# SIMPL-21592: [API] The Governance Authority checks the schema of a new resource description or a new version of a resource description

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14689](../../README.md) (L2) › [SIMPL-17332](../README.md) (Epic) › **SIMPL-21592** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17332](../README.md) (Epic) The Governance Authority checks the schema of a new resource description or a new version of a resource description |
