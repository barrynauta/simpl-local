> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-21118](../../../README.md) / [L2-SIMPL-14755](../../README.md) / [Epic-SIMPL-17521](../README.md) / **Story-SIMPL-21600**

# SIMPL-21600: [BE] A Provider notifies Consumers about a new version of a Resource description

**User story (implementable work)** · Status **DevTeam PBI blocked**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-21118](../../../README.md) (L1) › [SIMPL-14755](../../README.md) (L2) › [SIMPL-17521](../README.md) (Epic) › **SIMPL-21600** (User story)

## Text

#### Storycard

| As a| Provider |
| --- |

| I want|to notify the Consumers of a resource about a published new version of a Resource description |
| --- |

| so that|so that Consumers can decide if any actions are required based on this new version. |
| --- |

##### Acceptance Criteria

###### AC1 - Provider notifies Consumers about a new version of a Resource Description

1. **Given** I, as a Provider, have requested the publication of a new version of a Resource Description\
   **When** the publication of the new version of the Resource Description is successful\
   **Then** the system retrieves the Consumers having an active contract associated with previous version of the Resource Description\
   **And** a notification is sent to those Consumers informing them that a new version of the Resource Description has been published.

###### AC2 - Publication of a new version is not successful

2. **Given** I, as a Provider, have requested the publication of a new version of a Resource Description\
   **When** the publication of the new version of the Resource Description fails\
   **Then** no notification is sent to any Consumer\
   **And** the notification process is not triggered.

##### Technical Details

The list of Consumers to be notified shall be retrieved from the Contract Management component by invoking the **`/agreements/search`** API exposed by Contract Manager ([https://code.europa.eu/simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be/-/blob/main/openapi/openapi3-v1.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be/-/blob/main/openapi/openapi3-v1.yaml?ref_type=heads)).

Only Consumers having an **active contract** associated with a previous version of the Resource Description shall be considered eligible recipients for the notification.

Once the list of eligible Consumers has been retrieved, the notification email shall be sent through the Notification Service.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17521](../README.md) (Epic) A Provider notifies Consumers about a new version of a Resource description |
