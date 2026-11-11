> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-493**

# SIMPL-493: UI for defining access policies

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-493** (User story)

## Text

**As a** Provider User\
**I want** use an UI to support the formulation of the access policies in a machine-readable format\
**So that** I can define the access policies to my dataset/application/infrastructure, i.e. define the condition that a consumer can access these resources.

-----------------------

**Technical Detail**

Editor to set the parameters for the minimal access policy defined in (internal page)

(image not shown)(image not shown)

The UI should allow to write one or multiple access policies. For each Policy it is possible to set 4 parameters

- action: Can be either be search, consume, restrictedConsume
- attribute: select one of the available security attributes. The list of attributes are from an API of Aruba (an internal page: _*GET /identity-attribute/* retrieve all identity attributes defined by Dataspace Governance Authority, marking the attributes assigned to the caller participant agent with assigned_to = caller participantID). For the dev cluster this API needs to be replaced by a mock with a fixed list of attributes and for integration replaced by the real API_
- *valid from: date time picker to select the from value (can optionally not be set)*
- *valid until: date time picker to select the until value (can optionally not be set)*

*It should be possible to add more policies with an add policy button, and the existing ones can be deleted with another button.*

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

Currently no support for ODRL Templates which will likely be part of another release.

AC:

1. The access policy is composed by 4 properties, 2 are mandetory and 2 are optional that the user needs to fill out
1. The user can select the action (read or use) from a dropdown
1. The user can select a security attribute from a dropdown
1. After pressing the "save button" the parameters are passed to the API (SIMPL-2938) and the Access policie(s) are attached to the SD

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
