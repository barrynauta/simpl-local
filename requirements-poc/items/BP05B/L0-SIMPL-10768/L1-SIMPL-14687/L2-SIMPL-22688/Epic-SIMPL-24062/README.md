> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-22688](../README.md) / **Epic-SIMPL-24062**

# SIMPL-24062: The Governance Authority notifies the Provider about metadata quality results

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-22688](../README.md) (L2) › **SIMPL-24062** (Epic)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-22688](../README.md) (L2) The Governance Authority notifies the Provider about the metadata quality |
| **Children** | [SIMPL-28001](Story-SIMPL-28001/README.md) (User story) Governance Authority and Provider access metadata quality assessment outcome through publication response |
