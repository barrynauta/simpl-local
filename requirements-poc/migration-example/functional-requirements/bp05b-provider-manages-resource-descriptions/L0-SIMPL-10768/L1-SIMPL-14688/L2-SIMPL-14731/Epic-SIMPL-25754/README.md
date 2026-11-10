> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14731](../README.md) / **Epic-SIMPL-25754**

# SIMPL-25754: A Provider defines the resource source address for resource sharing [Improvement]

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14731](../README.md) (L2) › **SIMPL-25754** (Epic)

## Text

This improvement aims to separate the schema properties from the data address functionality.

The data address property used to be part of the schema before the functionality was developed. Therefore we need to clearly separate the schema from the resource, to avoid coupling.

This is done by:

- Removing the property from the schema and by using dedicated parameters in the data address templates ((internal page))
- Remove the data handover in the backend ((internal page))
- Adjusting the UI to use the dedicated parameter instead of a schema property ((internal page))

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14731](../README.md) (L2) A Provider adds common metadata to a new Resource description |
| **Children** | None. |
