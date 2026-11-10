> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-961](../README.md) / **Story-SIMPL-424**

# SIMPL-424: UI for definition of usage policies

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-961](../README.md) (Epic) › **SIMPL-424** (User story)

## Text

**As a** Provider, creating a Self-Description through a UI,\
**I want to** have an extension of the Self-description UI for creating a Usage Policy that will be assigned to the Self-Description\
**So that** I can define the usage policy for the Self-Description being created, i.e. define the constraints under which a consumer can use my resource

-----------------------

**Technical Detail**

The new sd-creator-frontend (SIMPL-1083 (internal page)) should be extended with the capabilities to set the parameters for the usage policies. The supported usage policies are described in (internal page)

It should be possible in the UI to describe one or more constraints for the usage of the data. The supported types of constraints are:

1. Restricted Number of Usages (one parameter that is the number of allowed access to the dataset)
1. Duration-restricted Data Usage (two date time parameter that describe the from and until of usage)
1. Deletion after usage (no parameter)

It should be possible to add constraints and set the parameter. The backend will later translate those into valid ODRL Policies.

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

**Mock**

(image not shown)

**AC:**

1. Three different types of usage policy constraint are supported
1. The user can add more constraints with a button
1. The user can remove constraints with a button
1. Depending on the type 0, 1 or 2 parameters are set
1. After pressing the "save button" the parameters are passed to the API (SIMPL-3241) and the usage policie(s) are attached to the SD

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-961](../README.md) (Epic) A Provider assigns usage policies to a new resource description by using policy template via UI |
