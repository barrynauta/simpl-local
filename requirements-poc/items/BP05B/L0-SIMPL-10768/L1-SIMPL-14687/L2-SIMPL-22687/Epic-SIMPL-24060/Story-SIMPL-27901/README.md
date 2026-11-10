> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-22687](../../README.md) / [Epic-SIMPL-24060](../README.md) / **Story-SIMPL-27901**

# SIMPL-27901: Governance Authority assign metadata quality classifications to evaluation results

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-22687](../../README.md) (L2) › [SIMPL-24060](../README.md) (Epic) › **SIMPL-27901** (User story)

## Text

**As a** Governance Authority\
**I want to** Simpl-Open to assign classifications for overall and per-dimension scores\
**so that** Participants can understand quality levels in a standardized way.

##### Context

Classification derives from the

###### **High-level functionality 2**

The *Governance Authority* can define a **metadata quality classification** (e.g. Poor, Medium, Good) to label the level of quality for a specific metadata quality dimension. The metadata quality classification is determine based on **quality score ranges** (e.g. Poor: 0-40, Medium: 40-60, Good: 60-100). The *Governance Authority* can configure the score ranges per metadata quality classification.

[an internal page]

The **metadata quality classification** and **quality score ranges** are defined in the Metadata Quality Profile see the attached example file.

##### (attachment not shown)

##### Technical Details:

- Implement classification mapping from calculated scores to configured score ranges for overall and per-dimension scopes.
- Enforce boundary handling rules consistently for exact min/max edge cases.
- Reject invalid, overlapping, or ambiguous ranges with explicit validation errors.
- Treat unmappable scores as validation errors and block classification.
- Ensure mapping emits exactly one classification per evaluated scope.
- Keep implementation extensible for future fields and classification logic.
- Cover classification mapping logic and boundary behavior with unit tests.

##### Acceptance Criteria

###### AC1

**Given** overall and dimension scores are available\
**And** score ranges are defined in the active profile\
**When** classification mapping is applied\
**Then** one classification is assigned for the overall score\
**And** one classification is assigned for each dimension score.

###### AC2

**Given** a score falls exactly on a range boundary (edge of the defined range so for 0-40, 0 and 40 are the boundary)\
**When** classification mapping is applied\
**Then** Simpl-Open applies the boundary rule defined in the profile without ambiguity\
**And** boundary handling is consistent with configured range convention for that scope.

###### AC3

**Given** no metadata quality profile can be resolved for the provided self-description\
**When** scoring of the self-description is attempted\
**Then** Simpl-Open fails the scoring attempt and returns a error message

**OpenAPI Spec Link:**

[https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/quality-scoring-service/-/blob/develop/openapi/tier2-gateway/t2-quality-scoring-service_openapi.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/quality-scoring-service/-/blob/develop/openapi/tier2-gateway/t2-quality-scoring-service_openapi.yaml?ref_type=heads)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24060](../README.md) (Epic) The Governance Authority evaluates the metadata quality of a Resource description |
