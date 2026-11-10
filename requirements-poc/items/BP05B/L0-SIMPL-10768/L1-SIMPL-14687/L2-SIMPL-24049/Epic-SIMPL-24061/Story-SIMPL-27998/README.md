> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-24049](../../README.md) / [Epic-SIMPL-24061](../README.md) / **Story-SIMPL-27998**

# SIMPL-27998: Catalogue validates against the Quality Assesment Record and decides the publication before storing self-description

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-24049](../../README.md) (L2) › [SIMPL-24061](../README.md) (Epic) › **SIMPL-27998** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24061](../README.md) (Epic) The Governance Authority approves or rejects a Resource description based on metadata quality |
