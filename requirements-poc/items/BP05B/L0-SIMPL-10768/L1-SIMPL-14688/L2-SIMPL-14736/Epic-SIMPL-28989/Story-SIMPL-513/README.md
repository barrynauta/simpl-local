> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-28989](../README.md) / **Story-SIMPL-513**

# SIMPL-513: Support document mode for SLA Agreements using source + hash

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-28989](../README.md) (Epic) › **SIMPL-513** (User story)

## Text

Disclaimer: This story is to support the future SLA link and is not implementing SLA's in any way. The requirements related to SLA's need to be defined.

As a Provider User\
I want to calculate the hash of the SLA Agreements document referenced in the self-description\
So that I can check if the document was modified

**Technical Specification**

Based on the user's URL input in the SD Tooling UI, the PDF File needs to be downloaded and a hash value is calculated and returned. The returned value must then be stored in the Self-Description, as well as the hash algorithm that has been used.

The hash-algorithm should be SHA-256, we need to store three properties:

- the link to the pdf
- the hash of the document
- the hashing algorithm (SHA-256)

AC:

1. Confirm that inside the JSON-LD has the hash of the document and the algorithm
1. Check if the hash value of the document calculated with a 3rd party tool has the same value as from the SD
1. When calculating several times the hash of the same document the same value must be obtained

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-28989](../README.md) (Epic) A Provider manages contract clauses [MVP] |
