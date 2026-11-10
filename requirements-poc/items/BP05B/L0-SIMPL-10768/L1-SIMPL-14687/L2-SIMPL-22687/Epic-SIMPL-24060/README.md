> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-22687](../README.md) / **Epic-SIMPL-24060**

# SIMPL-24060: The Governance Authority evaluates the metadata quality of a Resource description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-22687](../README.md) (L2) › **SIMPL-24060** (Epic)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-22687](../README.md) (L2) The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description |
| **Children** | [SIMPL-27899](Story-SIMPL-27899/README.md) (User story) Governance Authority evaluate metadata quality scores for submitted resource description<br>[SIMPL-27901](Story-SIMPL-27901/README.md) (User story) Governance Authority assign metadata quality classifications to evaluation results<br>[SIMPL-27904](Story-SIMPL-27904/README.md) (User story) Governance Authority store metadata quality assessment as related metadata |
