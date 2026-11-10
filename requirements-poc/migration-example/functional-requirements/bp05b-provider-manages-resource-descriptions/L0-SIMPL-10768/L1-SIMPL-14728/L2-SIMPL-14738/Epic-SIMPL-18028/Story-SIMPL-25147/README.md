> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14738](../../README.md) / [Epic-SIMPL-18028](../README.md) / **Story-SIMPL-25147**

# SIMPL-25147: [API] API for retrieving a resourceAddress info (EdcConnectorAdapter)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14738](../../README.md) (L2) › [SIMPL-18028](../README.md) (Epic) › **SIMPL-25147** (User story)

## Text

#### Storycard

| As a|Participant Provider |
| --- |

| I want|to retrieve (via sd-tooling-be) the *resourceAddress* associated with a resourceDescription, as previously stored in the connector during the assetRegistration stage |
| --- |

| so that|I can correctly display the resource description based on the specific schema with which it was created. |
| --- |

#### Technical details

This ticket aims to implement a new API directly in the EdcConnectorAdapter, which communicates with the underlying EDC Connector to retrieve the *resourceAddress* associated with a specific resourceDescription.

When a consumer (e.g., SD‑ToolingBE or another client) requests to visualize or update a resourceDescription, the EdcConnectorAdapter shall:

- retrieve the resourceAddress stored during the initial assetRegistration or during any subsequent update by querying the EDC Connector’s API,
- return this data in JSON format to the requester.

The newly implemented EdcConnectorAdapter API must therefore:

1. **Expose a REST endpoint** to external components (e.g., SD‑ToolingBE).
1. **Query the EDC Connector** to obtain the resourceAddress for the requested resourceDescription.
1. Return the retrieved structure as raw JSON, preserving its format for correct downstream rendering and editing.\
   Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | resourceAddress successfully retrieved |
| **400 Bad Request** | Invalid resource identifier |
| **404 Not Found** | Requested resourceAddress not found for the given resourceDescription |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | Underlying EDC Connector unreachable, failed request upstream |
| **500 Internal Server Error** | Unexpected adapter internal error |

###### Acceptance Criteria

###### Successful retrieval of resourceAddress

1. **Given** a valid `resourceDescription` identifier (`{}assetID{`}) is provided in a request to the API\
   **When** the API processes the request to retrieve the resource address associated with that identifier\
   **Then** the API returns the corresponding `resourceAddress` in JSON format

###### resourceAddress not available

2. **Given** a request is sent to the API to retrieve the resource address for a valid `resourceDescription` identifier (`{}assetID{`})\
   **When** the API is unable to obtain the corresponding `resourceAddress` due to it being unavailable, missing, or due to an error occurring during retrieval\
   **Then** the API returns a response indicating that the requested `resourceAddress` is not available

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18028](../README.md) (Epic) A Provider updates common metadata of a new version of a  Resource description via UI |
