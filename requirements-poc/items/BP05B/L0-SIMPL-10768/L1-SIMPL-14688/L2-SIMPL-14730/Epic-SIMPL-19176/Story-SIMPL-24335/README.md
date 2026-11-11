> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14730](../../README.md) / [Epic-SIMPL-19176](../README.md) / **Story-SIMPL-24335**

# SIMPL-24335: [API] API for retrieving a specific schema version (sd-tooling-be)

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14730](../../README.md) (L2) › [SIMPL-19176](../README.md) (Epic) › **SIMPL-24335** (User story)

## Text

#### Storycard

**As a** Provider Participant (through sd-tooling UI)\
**I want to** retrieve (via sd-tooling-be) a specific version of a schema\
**So that** I can correctly display the resource description based on the specific schema with which it was created.

#### Technical details

This ticket aims to implement an API in sd-tooling-be, exposed to the sd-tooling UI, to support the rendering of a resource description. When the UI requests to visualize a resource description, sd-tooling-be resolves the required schema version as follows: it first checks whether the schema is already available in the NFS storage and returns it if present; otherwise, it invokes the corresponding API of the sync-schema-adapter, which retrieves the schema from the schema-management-service. The retrieved schema is then returned to the UI to correctly render the resource description fields.

Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | Schema version successfully retrieved |
| **400 Bad Request** | Invalid schema identifier or version |
| **404 Not Found** | Requested schema version missing |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | Upstream schema service failure |
| **500 Internal Server Error** | Unexpected adapter internal error |

#### Acceptance criteria

**Retrieval of a specific schema version**

1. **Given** a participant requests to view a resource description through sd-tooling UI\
   **When** the sd-tooling UI invokes the sd-tooling-be API requesting a specific schema identifier and version\
   **Then** sd-tooling-be shall return the requested schema version to the UI\
   **And** the schema shall be retrieved from the NFS storage or, if not available, via the sync-schema-adapter from the schema-management-service.

**Schema version not available**

2. **Given** a participant requests to view a resource description through sd-tooling UI\
   **When** the sd-tooling UI invokes the sd-tooling-be API requesting a schema version that is not available locally\
   **And** the schema version cannot be retrieved via the sync-schema-adapter from the schema-management-service\
   **Then** sd-tooling-be shall return an explicit error indicating that the requested schema version is not available\
   **And** the sd-tooling UI shall display an error indicating that the resource description cannot be rendered.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-19176](../README.md) (Epic) A Provider retrieves from the Governance Authority the schema of a published resource type |
