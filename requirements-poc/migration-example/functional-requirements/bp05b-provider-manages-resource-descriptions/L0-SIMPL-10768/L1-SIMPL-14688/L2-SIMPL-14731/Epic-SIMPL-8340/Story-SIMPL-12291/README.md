> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-8340](../README.md) / **Story-SIMPL-12291**

# SIMPL-12291: [API] Resource Source Address Validation Based on Resource Sharing Method and Provider Template

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-8340](../README.md) (Epic) › **SIMPL-12291** (User story)

## Text

**As a** provider user creating a resource description,\
**I want** the resource source that is submitted as part of the resource description to be syntactically validated and validated against the resource source template of the previously selected resource sharing method\
**So that** I can ensure that the provided resource source address is syntactically correct and complies with the selected resource sharing method and the associated template.

###### **Acceptance Criteria**

- authorised user
   - valid resource source

1. **Given** I am authorised to manage resource descriptions,\
   **When** I submit a valid resource description including a resource source address, valid for the selected resource sharing method,\
   **Then** I will receive a positive validation message and I am able to publish the resource description to the dataspace catalogue.

   - syntactic invalid resource source

2. **Given** I am authorised to manage resource descriptions,\
   **When** I submit a resource description including a syntactically incorrect resource source address\
   **Then** the resource description must be rejected and I receive an error message.

   - resource source not compliant with the template of the selected resource sharing method

3. **Given** I am authorised to manage resource descriptions,\
   **When** I submit a resource description including a resource source address, not compliant with the resource address template for the resource sharing method selected\
   **Then** the resource description must be rejected and I receive an error message.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-8340](../README.md) (Epic) A Provider defines the resource source address for resource sharing |
