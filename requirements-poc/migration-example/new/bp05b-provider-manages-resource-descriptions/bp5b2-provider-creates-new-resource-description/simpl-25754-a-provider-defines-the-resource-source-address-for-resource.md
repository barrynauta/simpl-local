<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider defines the resource source address for resource sharing [Improvement]

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.01 Create a new resource description](../README.md#bp05b01-create-a-new-resource-description).

In the backlog it is item SIMPL-25754, status **Defined**. It has no user stories yet.

## What this Epic delivers

This improvement aims to separate the schema properties from the data address functionality.

The data address property used to be part of the schema before the functionality was developed. Therefore we need to clearly separate the schema from the resource, to avoid coupling.

This is done by:

- Removing the property from the schema and by using dedicated parameters in the data address templates ((internal page))
- Remove the data handover in the backend ((internal page))
- Adjusting the UI to use the dedicated parameter instead of a schema property ((internal page))

---

Back to [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
