> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14730](../../README.md) / [Epic-SIMPL-19176](../README.md) / **Story-SIMPL-23442**

# SIMPL-23442: [API] API for retrieving a specific schema version (schema-sync-adapter)

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14730](../../README.md) (L2) › [SIMPL-19176](../README.md) (Epic) › **SIMPL-23442** (User story)

## Text

#### Storycard

| As a|Participant (through sd-tooling-be or adv-search-be) |
| --- |

| I want|to retrieve (via sync-schema-adapter) a specific version of a schema |
| --- |

| so that|I can correctly display the resource description based on the specific schema with which it was created. |
| --- |

#### Technical details

This ticket aims to implement an API on the sync-schema-adapter component that allows the sd-tooling-be and xfsc-advsearch-be components to retrieve a specific version of a schema. The retrieved schema version will then be returned to the UI layer (either sd-tooling UI or catalogue-ui) so that providers and consumers can correctly visualize the different fields of a resource description. When a participant, through sd-tooling or the catalogue, requests to view a resource description, the backend components (sd-tooling-be and catalogue / xfsc-adv-search) will invoke this API exposed by the sync-schema-adapter component. The adapter will request it from the schema-management-service.

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

**Given** a participant requests to view a resource description through sd-tooling or catalogue UI

**When** the backend component (sd-tooling-be or xfsc-advsearch-be) invokes the sync-schema-adapter API specifying the schema identifier and version

**Then** the sync-schema-adapter shall return the requested schema version

**And** the schema version shall be fetched from the schema-management-service.

**Schema version not available**

**Given** a participant requests to view a resource description through sd-tooling or catalogue UI

**When** the backend component (sd-tooling-be or xfsc-advsearch-be) invokes the sync-schema-adapter API requesting a schema version that is not available locally

**And** the schema version cannot be retrieved from the schema-management-service

**Then** the sync-schema-adapter shall return an explicit error indicating that the requested schema version is not available

**And** the error response shall allow the calling backend to handle the failure without attempting to render the resource description with an incorrect or incompatible schema.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-19176](../README.md) (Epic) A Provider retrieves from the Governance Authority the schema of a published resource type |
