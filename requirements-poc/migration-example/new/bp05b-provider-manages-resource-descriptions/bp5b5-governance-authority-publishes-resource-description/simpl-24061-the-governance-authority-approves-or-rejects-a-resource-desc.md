<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.13 The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b513-the-governance-authority-approvesrejects-the-metadata-of-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority approves or rejects a Resource description based on metadata quality

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.13 The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b513-the-governance-authority-approvesrejects-the-metadata-of-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog (Jira) it is item SIMPL-24061. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As** Governance Authority,\
**I want** Simpl-Open to automatically approve or reject the metadata of a submitted resource description or a new version of a resource description by:

- Evaluating whether the overall metadata quality score is higher than the overall metadata quality threshold;
- Evaluating whether the metadata quality score for each dimension is higher than the quality threshold defined for that dimension;

**So that** only resource descriptions with a minimum required level of metadata quality are approved for publication.

##### Acceptance Criteria

###### AC1 - Automatic Approval

1. **Given** a new resource description or a new version of a resource description is submitted\
   **And** the resource description is valid (i.e., it has successfully passed structural and schema validation)\
   **When** the automatic overall quality score and the quality scores per dimension are calculated\
   **And** the overall metadata quality score is higher than the overall metadata quality threshold\
   **And** the metadata quality score for each dimension is higher than the quality threshold defined for that dimension\
   **Then** Simpl-Open automatically approves the resource description for publication.

###### AC2 - Automatic Rejection

2. **Given** a new resource description or a new version of a resource description is submitted\
   **And** the resource description is valid (i.e., it has successfully passed structural and schema validation)\
   **When** the automatic overall quality score and the quality scores per dimension are calculated\
   **And** the overall metadata quality score is lower than or equal to the overall metadata quality threshold\
   Or the metadata quality score for at least one dimension is lower than or equal to the quality threshold defined for that dimension\
   **Then** Simpl-Open automatically rejects the resource description for publication.

## User stories

1. [Catalogue validates against the Quality Assesment Record and decides the publication before storing self-description](#user-story-1-catalogue-validates-against-the-quality-assesment-record-and-decides-the-publication-before-storing-self-description)

### User story 1: Catalogue validates against the Quality Assesment Record and decides the publication before storing self-description

Backlog item SIMPL-27998

**As a** Governance Authority,\
**I want to** automatically approve or reject a resource description based on overall and per-dimension thresholds,\
**So that** compliant metadata is published and non-compliant metadata is prevented from publication.

###### Technical Details:

- Integrate a synchronous call from catalogue publication flow to the score-validation endpoint.
- Parse and validate the returned QAR
- Evaluate thresholds using QAR overall and per-dimension scores.
- Persist self-description only for pass outcomes; for fail outcomes, return rejection without persistence.
- Persist the returned QAR as linked metadata when self-description is stored.
- Cover pass, reject, invalid-QAR, endpoint-failure, and configuration-error scenarios with automated tests.
- Error message: "Quality Score X is below treshold Y."

###### Acceptance Criteria

###### AC1

1. **Given** a submitted resource description is valid\
   **And** a QAR is generated for this resource description\
   **When** the overall score in this QAR is greater or equal than the overall threshold defined in the metadata quality definition\
   **And** each dimension score in the QAR is greater or equal than its dimension threshold in the metadata quality definition\
   **Then** the resource description is published in the catalogue\
   **And** the QAR is stored as metadata of the published resource description

###### AC2

2. **Given** a submitted resource description is valid\
   **And** a QAR is generated for this resource description\
   **When** the overall score in this QAR is lower than the overall threshold defined in the metadata quality definition (MQP)\
   Or at least one dimension score in the QAR is lower than its dimension threshold in the metadata quality definition\
   **Then** the resource description is not published in the catalogue\
   **And** the resource description is not stored\
   **And** an explicit error is returned explaining the reason (reached score value and threshold)

###### AC3

3. **Given** a submitted resource description is valid\
   **And** a metadata quality assessment for this resource description cannot be completed due to a technical failure (for instance missing threshold definition)\
   **When** the publication decision is requested\
   **Then** the resource description is not published in the catalogue\
   **And** an explicit technical error is returned\
   **And** the failure reason is logged.

**Remark:** In alignment with API First and the modularity in testing, the current implementation will use a mock/stub of the endpoint to retrieve the Scoring. The implementation test is here to test the integration without mocks SIMPL-30617

---

Back to [5B.5.13 The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b513-the-governance-authority-approvesrejects-the-metadata-of-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
