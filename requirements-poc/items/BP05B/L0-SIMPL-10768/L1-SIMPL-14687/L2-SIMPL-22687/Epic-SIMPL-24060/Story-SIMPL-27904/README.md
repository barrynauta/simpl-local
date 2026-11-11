> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-22687](../../README.md) / [Epic-SIMPL-24060](../README.md) / **Story-SIMPL-27904**

# SIMPL-27904: Governance Authority store metadata quality assessment as related metadata

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-22687](../../README.md) (L2) › [SIMPL-24060](../README.md) (Epic) › **SIMPL-27904** (User story)

## Text

**As a** Governance Authority\
**I want to** Simpl-Open to store the quality assessment as the validation report enriched with a link to the evaluated resource description\
**So that** the outcome is reusable for approval and search.

##### Technical Details:

- Build the QAR model with all required score, classification, and traceability fields.
- Include an explicit reference to the exact Metadata Quality Profile version used for evaluation.
- Include a `dqv:computedOn` link to the evaluated resource description.
- Add correlation metadata to connect evaluation execution and persisted QAR traceability.
- Keep persistence semantics aligned with sidecar storage and rollback requirements in this story.

##### Acceptance Criteria

###### AC1

1. **Given** a resource description for which metadata quality is evaluated\
   **When** metadata quality evaluation for this resource description completes successfully\
   **Then** a Quality Assessment Report (QAR) for this evaluation is created\
   **And** the QAR includes overall score, dimension scores, overall classification, dimension classifications, and rule assertions\
   **And** Simpl-Open returns the QAR in the endpoint response\
   **And** the signed resource description payload is not changed.

###### AC2

2. **Given** a QAR is created for a resource description\
   **When** the QAR is returned by the endpoint\
   **Then** the QAR contains a `dqv:computedOn` link to the evaluated resource description\
   **And** the QAR can be unambiguously linked back to that resource description.

###### AC3

3. **Given** a QAR is created for a completed evaluation\
   **When** traceability fields are written\
   **Then** the QAR contains a link to the exact Metadata Quality Profile version used for this evaluation

###### AC4

4. **Given** QAR creation is attempted after evaluation\
   **When** QAR creation fails\
   **Then** the flow is interrupted and the operation is reported as failed\
   **And** no incomplete QAR is returned in the endpoint response\
   **And** an error is logged and returned to the caller.

**OpenAPI Spec Link:**

https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/quality-scoring-service/-/blob/develop/openapi/tier2-gateway/t2-quality-scoring-service_openapi.yaml?ref_type=heads

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24060](../README.md) (Epic) The Governance Authority evaluates the metadata quality of a Resource description |
