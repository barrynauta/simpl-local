> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14730](../README.md) / **Epic-SIMPL-19176**

# SIMPL-19176: A Provider retrieves from the Governance Authority the schema of a published resource type

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14730](../README.md) (L2) › **SIMPL-19176** (Epic)

## Text

#### Storycard

| As a|Provider |
| --- |

| I want|to retrieve the schema of a published resource type from the Governance Authority |
| --- |

| so that|I can start the creation of a new resource description. |
| --- |

#### Acceptance criteria

| Scenario | A Provider retrieves the schema of a published resource type |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I have a role authorising the management of Resource Descriptions |
| --- |

| When|I request the schema of a published resource type from the Governance Authority |
| --- |

| then|I receive the schema of the published resource type. |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14730](../README.md) (L2) A provider retrieves a Resource type |
| **Children** | [SIMPL-20514](Story-SIMPL-20514/README.md) (User story) API for Schema Managament Webhook Notification and Trigger Schema Update<br>[SIMPL-20613](Story-SIMPL-20613/README.md) (User story) [UI] A provider selects the Resource type in the UI<br>[SIMPL-23442](Story-SIMPL-23442/README.md) (User story) [API] API for retrieving a specific schema version (schema-sync-adapter)<br>[SIMPL-24335](Story-SIMPL-24335/README.md) (User story) [API] API for retrieving a specific schema version (sd-tooling-be) |
