<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.12 The Governance Authority notifies the Provider about the metadata quality](../bp5b5-governance-authority-publishes-resource-description.md#5b512-the-governance-authority-notifies-the-provider-about-the-metadata-quality) / Epic
</div>

# Epic: The Governance Authority notifies the Provider about metadata quality results

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.12 The Governance Authority notifies the Provider about the metadata quality](../bp5b5-governance-authority-publishes-resource-description.md#5b512-the-governance-authority-notifies-the-provider-about-the-metadata-quality), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.06 Remediate validation issues](../README.md#bp05b06-remediate-validation-issues).

In the backlog it is item SIMPL-24062, status **Defined**. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As** Governance Authority,\
**I want** Simpl-Open to automatically notify the Provider who submitted a resource description about the metadata quality assessment results\
**So that** the Provider is transparently informed about the metadata quality evaluation outcome and the publication decision.

##### Acceptance Criteria

1. **Given** a new valid resource description or a new valid version of a resource description is received by the Governance Authority\
   **When** the metadata quality evaluation is completed\
   **Then** Simpl-Open automatically notifies the Provider who submitted the resource description\
   **And** the notification shall include at least the following information:

   - The overall metadata quality score
   - The overall metadata quality classification
   - The metadata quality score for each defined quality dimension
   - The metadata quality classification for each defined quality dimension
   - The overall decision result:
   - Approved for publication (metadata quality is higher than all required thresholds), or
   - Rejected (overall score is lower than the overall threshold and/or at least one dimension score is lower than its defined threshold)
   - A clear indication of which threshold(s) were not met, in case of rejection

## User stories

1. [Governance Authority and Provider access metadata quality assessment outcome through publication response](#user-story-1-governance-authority-and-provider-access-metadata-quality-assessment-outcome-through-publication-response) (DevTeam PBI is ready for Implementation check)

### User story 1: Governance Authority and Provider access metadata quality assessment outcome through publication response

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-28001)

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

---

Back to [5B.5.12 The Governance Authority notifies the Provider about the metadata quality](../bp5b5-governance-authority-publishes-resource-description.md#5b512-the-governance-authority-notifies-the-provider-about-the-metadata-quality) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
