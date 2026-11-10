> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-8340](../README.md) / **Story-SIMPL-18360**

# SIMPL-18360: Create Resource Address Template for eDelivery

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-8340](../README.md) (Epic) › **SIMPL-18360** (User story)

## Text

**As a** system or application integrating with SIMPL,\
**I want to** use standardized templates for eDelivery resource addresses during both creation (as Resource Source) and consumption (as Resource Destination),\
**So that** resource transfers via the eDelivery channel are validated and executed correctly in the SD creation (BP5) and consumption (BP9A) processes.\
----

###### **Acceptance Criteria**

1. **Given** I am defining a resource address as part of SD creation (BP5) or initiating transfer (BP9A)\
   **And** I use the template structure for type `eDelivery`\
   **Then** the system validates that the address includes the required fields based on the context (Source or Destination)\
   ----

###### **Examples**

**Resource Source Template (used during creation):**
```
{
     "type": "eDelivery"
     "edeliveryTriggerAPI": "http://simpl-poc/v1/simpl-edelivery-trigger",
     "appDataIdentifier": "simpl-edelivery-poc-01"
} 
```
**Field Requirements for Source Address:**

| param | data type | allowed values | optional/mandatory |
| --- | --- | --- | --- |
| type | string | eDelivery | mandatory |
| edeliveryTriggerAPI | URL |  | mandatory |
| appDataIdentifier | string |  | mandatory |

**Resource Destination Template (used during consumption):**
```
{
    "type": "eDelivery",
    "accessServiceIdentifier": "1",
    "requestorIdentifier": "22",
    "dateFrom": "2002-02-22",
    "dateTo": "2002-02-22",
    "format": "csv",
    "service": "service1",
    "action": "service2"
} 
```

**Field Requirements for Destination Address:**

| param | data type | allowed values | optional/mandatory |
| --- | --- | --- | --- |
| type | string | eDelivery | mandatory |
| accessServiceIdentifier | string |  | mandatory |
| requestorIdentifier | string |  | mandatory |
| dateFrom | string |  | optional |
| dateTo | string |  | optional |
| format | list or String |  | optional |
| service | string |  | optional |
| action | string |  | optional |

Useful Documentation:

(internal page)

(internal page)

(internal page)

(internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-8340](../README.md) (Epic) A Provider defines the resource source address for resource sharing |
