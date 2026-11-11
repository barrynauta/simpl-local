> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-22688](../../README.md) / [Epic-SIMPL-24062](../README.md) / **Story-SIMPL-28001**

# SIMPL-28001: Governance Authority and Provider access metadata quality assessment outcome through publication response

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-22688](../../README.md) (L2) › [SIMPL-24062](../README.md) (Epic) › **SIMPL-28001** (User story)

## Text

**As a** Governance Authority\
**I want to** return the metadata quality scores of a submitted resource description to the provider that submitted it\
**So that** the provider can decide whether improvements are required on the metadata

###### Technical Details:

Integrate quality-assessment and decision payload generation directly into the publication endpoint response.\
Ensure the publication response payload contains all mandatory quality and decision fields.\
Return explicit technical errors and structured logs when payload generation or response assembly fails.\
Validate publication response and error schemas with contract tests.

###### Technical Details

the response payload includes:

- One entry for each quality rule with\
   -- the status ("FAILED"/"PASSED")
- One entry for each dimension with:\
   -- per-dimension score (Numeric)\
   -- per-dimension classification (String, for instance red, yellow, green)\
   -- optional per-dimension thresholdStatus ("FAILED"/"PASSED")
- Exactly one entry for the overall information\
   -- total weighted score (Numeric)\
   -- classification (String, for instance red, yellow, green)\
   -- overall thresholdStatus ("FAILED"/"PASSED")

###### OpenAPI Reference

The publication endpoint to be extended is `POST /self-descriptions` (`operationId: addSelfDescription`) defined in the Tier 2 Gateway OpenAPI specification:\
[t2-fc-openapi.yaml]([https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/tier2-gateway/t2-fc-openapi.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/tier2-gateway/t2-fc-openapi.yaml?ref_type=heads))

The `201 Created` response schema (currently `$ref: SelfDescription`) must be extended to include the metadata quality outcome fields listed in AC1 below. The endpoint is also browsable via the sandbox Swagger UI: [POST /self-descriptions]([https://code.europa.eu/-/sandbox/swagger#/SelfDescriptions/addSelfDescription](https://code.europa.eu/-/sandbox/swagger#/SelfDescriptions/addSelfDescription)).

###### Acceptance Criteria

###### AC1

1. **Given** the metadata quality evaluation of a valid resource description is completed\
   **When** the verification of the metadata quality scores against the thresholds defined in the metadata quality definition is completed\
   **Then** Simpl-Open informs the Provider whether the resource description is accepted for publication or rejected\
   **And** the resource description is accepted for publication when all mandatory thresholds are met\
   **And** the resource description is rejected when one or more mandatory thresholds are not met\
   **And** Simpl-Open provides the result of the metadata quality evaluation\
   **And** this evaluation includes:

   - overall score
   - overall classification
   - per-dimension scores
   - per-dimension classifications
   - failed thresholds when the resource description is rejected

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24062](../README.md) (Epic) The Governance Authority notifies the Provider about metadata quality results |
