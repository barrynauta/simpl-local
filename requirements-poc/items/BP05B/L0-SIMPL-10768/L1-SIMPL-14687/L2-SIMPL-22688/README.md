> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../README.md) · [What the levels mean](../../../../../levels.md) · [All items](../../../../../all-items.md) · [By business process](../../../../../processes.md)

Folder: [items](../../../../README.md) / [BP05B](../../../README.md) / [L0-SIMPL-10768](../../README.md) / [L1-SIMPL-14687](../README.md) / **L2-SIMPL-22688**

# SIMPL-22688: The Governance Authority notifies the Provider about the metadata quality

**L2 requirement (detailed requirement)**

**Trace:** [BP05B](../../../README.md) › [SIMPL-10768](../../README.md) (L0) › [SIMPL-14687](../README.md) (L1) › **SIMPL-22688** (L2)

## Text

Simpl-Open shall support the Governance Authority to automatically notify the metadata quality feedback to a Provider that submitted a resource description by:

- Informing them about the metadata quality overall score;
- Informing them about the metadata quality dimension score for each dimension;
- Informing them about the metadata quality classification for the metadata quality overall score;
- Informing them about the metadata quality classification for each dimension;
- Informing them whether the level of metadata quality was sufficient for publication based on the quality threshold.

The notification shall be triggered automatically after the metadata quality calculation is completed and shall not require a manual action by the Governance Authority.

The recipient of the notification shall be the Provider associated with the submitted resource description, as registered in the system.

Simpl-Open shall ensure reliable processing of the notification and shall log notification delivery failures.

Additionally, the Provider can at all times retrieve the metadata quality scores overall, for each dimension, and the individual metadata quality scores related to each dimension for their published resources.

Simpl-Open shall provide this retrieval capability via the UI and API without requiring a prior notification to be available.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14687](../README.md) (L1) The Governance Authority publishes a Resource description |
| **Children** | [SIMPL-24062](Epic-SIMPL-24062/README.md) (Epic) The Governance Authority notifies the Provider about metadata quality results |
