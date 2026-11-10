> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14738](../../README.md) / [Epic-SIMPL-18028](../README.md) / **Story-SIMPL-25148**

# SIMPL-25148: [API] API for retrieving a resourceAddress info (sd-tooling-be)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14738](../../README.md) (L2) › [SIMPL-18028](../README.md) (Epic) › **SIMPL-25148** (User story)

## Text

#### Storycard

| As a|Participant Provider |
| --- |

| I want|to retrieve (via SD‑Tooling frontend) the *resourceAddress* associated with a resourceDescription, as previously stored in the connector during assetRegistration stage |
| --- |

| so that|I can correctly visualize and modify the resourceDescription using the technical information originally registered in the connector. |
| --- |

#### Technical details

This ticket aims to define and expose a new API in SD‑ToolingBE, which acts as a façade toward the underlying EdcConnectorAdapter, enabling the SD‑Tooling UI to retrieve the *resourceAddress* associated with a specific resourceDescription.

When the UI requests to visualize or update a resourceDescription, SD‑ToolingBE invokes the corresponding EdcConnectorAdapter API to retrieve the resourceAddress stored during the initial assetRegistration or during a later update.\
The retrieved resourceAddress is then returned to the SD‑Tooling UI to:

- correctly render the resourceDescription fields,
- allow the user to update them while preserving consistency with the existing configuration.

The SD‑ToolingBE API must therefore:

1. Expose a REST endpoint retrievable by SD‑Tooling UI.
1. Internally call the EdcConnectorAdapter API to obtain the resourceAddress.
1. Return the raw JSON structure to the UI in a stable format suitable for rendering and editing.\
   Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | resourceAddress successfully retrieved |
| **400 Bad Request** | Invalid resource identifier |
| **404 Not Found** | Requested resourceAddress not found for the given resourceDescription |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | EdcConnectorAdapter unreachable or returned an upstream error |
| **500 Internal Server Error** | Unexpected adapter internal error |

###### 

###### **Acceptance Criteria**

###### **Successful retrieval of resourceAddress**

**Given** a valid `resourceDescription` identifier (`{}assetID{`}) previously registered by the Provider\
**When** a request is sent to the API to retrieve the resource address associated with that `resourceDescription` identifier\
**Then** the API returns the corresponding `resourceAddress` in JSON format

###### **resourceAddress not available**

**Given** a request is sent to the API to retrieve the resource address associated with a valid `resourceDescription` identifier (`{}assetID{`})\
**When** the API is unable to obtain the corresponding `resourceAddress` due to it being absent or due to an error occurring in the retrieval process\
**Then** the API returns a response indicating that the requested `resourceAddress` is not available

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18028](../README.md) (Epic) A Provider updates common metadata of a new version of a  Resource description via UI |
