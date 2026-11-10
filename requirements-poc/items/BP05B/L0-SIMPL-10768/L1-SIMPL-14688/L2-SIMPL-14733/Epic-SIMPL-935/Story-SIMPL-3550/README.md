> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-935](../README.md) / **Story-SIMPL-3550**

# SIMPL-3550: [QueryMapper] Access Policy possible to define Prohibitions

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-935](../README.md) (Epic) › **SIMPL-3550** (User story)

## Text

**As a** Provider User,\
**I want to** use the API to define negative rules on the access policy, i.e., prohibit specific user with the attributes from actions.\
**So that** I can define the access policy that restrict the access for specific attributes.

-----------------------------------

**Technical Details**

Additional to the already checked permissions the query mapper need to check the prohibitions in the access policy.

1. extract the prohibition part of the self-description
1. extend the query to check if the attributes of the consumer are not in the prohibitions list, i.e., where consumer_attribute != prohibition_attribute

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-935](../README.md) (Epic) A Provider is supported in defining an access policy by a using policy template via API |
