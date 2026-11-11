<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.11 The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b511-the-governance-authority-evaluates-the-metadata-quality-of-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority evaluates the metadata quality of a Resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.11 The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b511-the-governance-authority-evaluates-the-metadata-quality-of-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog (Jira) it is item SIMPL-24060. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As** Governance Authority,\
**I want** Simpl-Open to automatically evaluate the metadata quality of a submitted resource description or a new version of a resource description by calculating:

- Metadata quality overall score
- Metadata quality dimension score for each defined dimension
- Metadata quality classification for the overall score
- Metadata quality classification for each dimension

**So that** the metadata quality assessment is consistently and objectively determined according to defined evaluation rules.

##### Acceptance Criteria

###### AC1 - Triggering the Evaluation

1. **Given** the Governance has received a new resource description or new version of a resource description for publication,\
   **When** the resource description passes structural and schema validation,\
   **Then** Simpl-Open automatically executes the metadata quality evaluation process.

###### AC2 - Quality Rule Validation

2. **Given** a valid resource description\
   **When** the evaluation process is executed\
   **Then** all quality rules are validated to be fullfilled or not. A quality rule is counted as 1 if it is validated for a resource description as true or 0 if not.

(image not shown)

###### AC3 - Dimension Score Calculation

3. **Given** a valid resource description\
   **When** the evaluation process is executed\
   **Then** for each defined quality dimension the dimension quality score is calculated as weighted sum of all quality rules of the respective dimension.

In mathematical terms the score of a dimension **d** for a resource description **rd,** is the sum over all validation of the quality rule **q** for the **rd** times the weight of **q:**

**(image not shown)**

###### AC4 - Overall Score Calculation

4. **Given** a valid resource description\
   **When** the evaluation process is executed\
   **Then** an metadata quality overall score is calculated as the weighted sum over all dimension quality score.

(image not shown)

###### AC5 - Classification Assignment

5. **Given** calculated overall and dimension scores\
   **When** classification rules are applied\
   **Then** a metadata quality classification is assigned:

   - For the overall score
   - For each quality dimension

###### AC6 - Output of Evaluation

6. **Given** the evaluation process is completed\
   **Then** the evaluation produces structured quality results (scores and classifications)\
   **And** the results are stored as metadata related to the evaluated resource description.

## User stories

1. [Governance Authority evaluate metadata quality scores for submitted resource description](#user-story-1-governance-authority-evaluate-metadata-quality-scores-for-submitted-resource-description)
2. [Governance Authority assign metadata quality classifications to evaluation results](#user-story-2-governance-authority-assign-metadata-quality-classifications-to-evaluation-results)
3. [Governance Authority store metadata quality assessment as related metadata](#user-story-3-governance-authority-store-metadata-quality-assessment-as-related-metadata)

### User story 1: Governance Authority evaluate metadata quality scores for submitted resource description

Backlog item SIMPL-27899

**As a** Governance Authority\
**I want to** Simpl-Open to automatically evaluate a submitted resource description against the active Metadata Quality Profile\
**So that** metadata quality is measured consistently and objectively.

##### **Technical Details:**

- Implement SHACL-based rule execution for rule types `EXISTENCE`, `IS_IN`, and `CUSTOM_SHACL`.
- Transform configured rules into SHACL constraints and execute validation against the Self-description JSON-LD graph using Apache Jena.
- Can be achieved by a three step approach
   1. Generate a SHACL Constraint file from the MQP (generated_shapes_from_examples.ttl)
   1. Use a SHACL validation on generated_shapes_from_examples.ttl against the self-description, results in validation_report.ttl
   1. Enrich that validation_report.ttl with the list for all rules, scores and classification (validation_report_with_rule_status.ttl)

```
arq --data vocabulary/examples.ttl --query vocabulary/rule_to_shacl_construct.rq --results TURTLE > vocabulary/generated_shapes_from_examples.ttl

shacl validate --shapes vocabulary/generated_shapes_from_examples.ttl --data vocabulary/example_dataset_for_generated_rules.ttl > vocabulary/validation_report.ttl

arq --data vocabulary/examples.ttl --data vocabulary/generated_shapes_from_examples.ttl --data vocabulary/validation_report.ttl --query vocabulary/enrich_validation_report_with_rule_status.rq --results TURTLE > vocabulary/validation_report_with_rule_status.ttl
```

- Generate one validation graph per profile version and cache it for reuse; invalidate cache on profile version change.
- Produce rule-level outputs with at least rule identifier, rule type, and binary result (`1` or `0`).

(image not shown)

- Add explicit validation/error handling for malformed or missing rule definitions; any processing or validation error fails the evaluation with a clear error message.
- Compute weighted dimension and overall scores from rule outputs with deterministic precision and rounding behavior.

(image not shown)

(image not shown)

- Skip rules and/or dimensions with zero weights; fail evaluation when required weights are missing.
- Cover success/failure paths with unit tests and validate cache hit/miss behavior with integration tests.

##### Acceptance Criteria

###### AC1

1. **Given** a resource description is submitted for publication\
   **And** structural and schema validation succeeded\
   **When** the quality evaluation pipeline starts\
   **Then** Simpl-Open retrieves exactly one active Metadata Quality Profile linked to the RD schema from the Schema Manager

###### AC2

2. **Given** a validation graph is available\
   **And** a given Resource Description is validated against its metadata quality rules\
   **When** a rule condition is met for a property of that Resource Description\
   **Then** the rule result is set to 1.

###### AC3

3. **Given** a validation graph is available\
   **And** a given Resource Description is validated against its metadata quality rules\
   **When** a rule condition is not met for a property of that Resource Description\
   **Then** the rule result is set to 0.

###### AC4

4. **Given** a Resource Description for which metadata quality is evaluated\
   **And** the weight of each rule per dimension is available in the Metadata Quality Profile\
   **When** the quality rules have been evaluated for that Resource Description, providing a rule result for each rule\
   **Then** for each dimension, the dimension score is calculated as the weighted sum of the rule results for that dimension.

###### AC5

5. **Given** a Resource Description for which metadata quality is evaluated\
   **And** the dimension scores are available\
   **And** the weight of each dimension is available in the Metadata Quality Profile\
   **When** the overall metadata quality score is calculated\
   **Then** the overall metadata quality score is calculated as the weighted sum of the dimension scores.

###### AC6

6. **Given** no Metadata Quality Profile exists for the submitted resource description schema\
   **When** the validation step is reached\
   **Then** Simpl-Open skips validation execution\
   **And** emits a warning indicating that no MQP exists for that schema.

**Remark:** In alignment with API First and the modularity in testing, the current implementation will use a mock/stub of the endpoint to retrieve the MQP. The implementation test is here SIMPL-30617 to test the integration without mocks

### User story 2: Governance Authority assign metadata quality classifications to evaluation results

Backlog item SIMPL-27901

**As a** Governance Authority\
**I want to** Simpl-Open to assign classifications for overall and per-dimension scores\
**So that** Participants can understand quality levels in a standardized way.

##### Context

Classification derives from the

###### **High-level functionality 2**

The *Governance Authority* can define a **metadata quality classification** (e.g. Poor, Medium, Good) to label the level of quality for a specific metadata quality dimension. The metadata quality classification is determine based on **quality score ranges** (e.g. Poor: 0-40, Medium: 40-60, Good: 60-100). The *Governance Authority* can configure the score ranges per metadata quality classification.

(internal page)

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

1. **Given** overall and dimension scores are available\
   **And** score ranges are defined in the active profile\
   **When** classification mapping is applied\
   **Then** one classification is assigned for the overall score\
   **And** one classification is assigned for each dimension score.

###### AC2

2. **Given** a score falls exactly on a range boundary (edge of the defined range so for 0-40, 0 and 40 are the boundary)\
   **When** classification mapping is applied\
   **Then** Simpl-Open applies the boundary rule defined in the profile without ambiguity\
   **And** boundary handling is consistent with configured range convention for that scope.

###### AC3

3. **Given** no metadata quality profile can be resolved for the provided self-description\
   **When** scoring of the self-description is attempted\
   **Then** Simpl-Open fails the scoring attempt and returns a error message

**OpenAPI Spec Link:**

[https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/quality-scoring-service/-/blob/develop/openapi/tier2-gateway/t2-quality-scoring-service_openapi.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/quality-scoring-service/-/blob/develop/openapi/tier2-gateway/t2-quality-scoring-service_openapi.yaml?ref_type=heads)

### User story 3: Governance Authority store metadata quality assessment as related metadata

Backlog item SIMPL-27904

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

---

Back to [5B.5.11 The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b511-the-governance-authority-evaluates-the-metadata-quality-of-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
