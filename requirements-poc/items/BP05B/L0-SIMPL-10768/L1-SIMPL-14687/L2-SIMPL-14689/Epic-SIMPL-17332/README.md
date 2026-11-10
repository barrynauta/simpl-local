> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14689](../README.md) / **Epic-SIMPL-17332**

# SIMPL-17332: The Governance Authority checks the schema of a new resource description or a new version of a resource description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14689](../README.md) (L2) › **SIMPL-17332** (Epic)

## Text

#### Storycard

| As|Governance Authority |
| --- |

| I want|to automatically check the schema of a new resource description or new version of a resource description |
| --- |

| So that|the published resource descriptions are consistent, reliable and compliant with pre-defined standards. |
| --- |

#### Acceptance criteria

| Scenario | The Governance Authority checks that the schema is active for a new resource description or a new version of a resource description with an active schema (Schema status validation - positive case) |
| --- | --- |

| When|I receive a new resource description or a new version of a resource description with an active schema |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| then|it is checked that the schema of that resource description is active |
| --- |

| and|schema status validation for the resource description or new version of a resource description has the result "valid". |
| --- |
|  |  |

| Scenario | The Governance Authority checks that the schema is active for a new resource description or a new version of a resource description with an inactive schema (Schema status validation - negative case) |
| --- | --- |

| When|I receive a new resource description or a new version of a resource description with an inactive schema |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| then|it is checked that the schema of that resource description is active |
| --- |

| and|schema status validation for the resource description or new version of a resource description has the result "invalid". |
| --- |
|  |  |

| Scenario | The Governance Authority checks that the schema of a new version of a resource description is of the same resource type as for the previous version for a new version of a resource description with the same schema as the previous version (Schema type validation - positive case) |
| --- | --- |

| When|I receive a new version of a resource description with the same resource type as the previous version |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| then|it is checked that the schema of that resource description has the same resource type as the previous version |
| --- |

| and|schema type validation for the new version of the resource description has the result "valid". |
| --- |
|  |  |

| Scenario | The Governance Authority checks that the schema of a new version of a resource description is of the same resource type as for the previous version for a new version of a resource description with a different schema as the previous version (Schema type validation - negative case) |
| --- | --- |

| When|I receive a new version of a resource description with a different resource type as the previous version |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| then|it is checked that the schema of that resource description has the same resource type as the previous version |
| --- |

| and|schema type validation for the new version of the resource description has the result "invalid". |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14689](../README.md) (L2) The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description |
| **Children** | [SIMPL-21592](Story-SIMPL-21592/README.md) (User story) [API] The Governance Authority checks the schema of a new resource description or a new version of a resource description |
