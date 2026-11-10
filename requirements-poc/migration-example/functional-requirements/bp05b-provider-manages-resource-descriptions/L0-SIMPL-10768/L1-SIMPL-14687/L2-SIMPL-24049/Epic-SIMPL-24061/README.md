> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-24049](../README.md) / **Epic-SIMPL-24061**

# SIMPL-24061: The Governance Authority approves or rejects a Resource description based on metadata quality

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-24049](../README.md) (L2) › **SIMPL-24061** (Epic)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24049](../README.md) (L2) The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description |
| **Children** | [SIMPL-27998](Story-SIMPL-27998/README.md) (User story) Catalogue validates against the Quality Assesment Record and decides the publication before storing self-description |
