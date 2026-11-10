> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14738](../../README.md) / [Epic-SIMPL-18028](../README.md) / **Story-SIMPL-25713**

# SIMPL-25713: [API] Implement API to atomically enrich and sign a resourceDescription (SD-Tooling))

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14738](../../README.md) (L2) › [SIMPL-18028](../README.md) (Epic) › **SIMPL-25713** (User story)

## Text

#### Storycard

| As a|Participant Provider |
| --- |

| I want|to trigger (through SD-Tooling) a single operation that automatically enriches, validates, and digitally signs a resourceDescription |
| --- |

| so that|I can securely and efficiently manage update and versioning workflows, receiving a final version that is already validated, digitally signed, immutable, and ready for publication without the risk of unintended modifications. |
| --- |

#### Technical details

This ticket introduces a new capability in SD‑Tooling, enabling the execution of enrichment and digital signature of a resourceDescription in one atomic operation.

Through a dedicated API exposed by SD‑Tooling, the system must:

1. **Receive** from the UI a resourceDescription requiring update and/or versioning.
1. Perform the enrichment process internally, applying:
   - syntactical validation
   - mandatory enrichment (e.g. connector info, contract info)
   - versioning logic
1. **Apply the digital signature immediately** after successful enrichment.
1. **Return a single final outcome** consisting of a fully enriched, validated, digitally signed, immutable resourceDescription, ready for catalog publication.

The process must guarantee that no intermediate or partially processed states are exposed to the UI, ensuring integrity and consistency.

.\
Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | The enriched and digitally signed resourceDescription is successfully returned |
| **400 Bad Request** | The provided resourceDescription is invalid or malformed |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **500 Internal Server Error** | Unexpected error during processing |

###### Acceptance Criteria

###### **AC1 Successful enrichment and signature**

1. **Given** a valid `resourceDescription` requiring update or versioning, submitted by a Participant Provider through the SD‑Tooling UI\
   **When** SD‑Tooling processes the request via the enrichment and signature operation\
   **Then** SD‑Tooling returns a fully enriched, syntactically validated, digitally signed, immutable `resourceDescription` that is ready for catalog publication.

###### **AC2 Invalid or incomplete input**

2. **Given** a malformed or incomplete `resourceDescription`\
   **When** SD‑Tooling attempts to process the request\
   **Then** the API returns an appropriate error and no enrichment or digital signature is applied.

###### **AC3 Enrichment or signature failure**

3. **Given** a valid `resourceDescription` request\
   **When** an internal error occurs during enrichment, validation, versioning, or digital signing\
   **Then** SD‑Tooling returns the appropriate error (`{}500 Internal Server Error{`}) and no partial, unsigned, or intermediate `resourceDescription` is returned.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18028](../README.md) (Epic) A Provider updates common metadata of a new version of a  Resource description via UI |
