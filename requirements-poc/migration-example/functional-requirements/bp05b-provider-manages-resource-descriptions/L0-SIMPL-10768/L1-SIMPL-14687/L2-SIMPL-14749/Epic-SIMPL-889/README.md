> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14749](../README.md) / **Epic-SIMPL-889**

# SIMPL-889: The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14749](../README.md) (L2) › **SIMPL-889** (Epic)

## Text

#### Storycard

| As|Governance Authority |
| --- |

| I want|to check the semantics of a new resource description or a new version of a resource description |
| --- |

| so that|the published resource descriptions are consistent, reliable and compliant with pre-defined standards. |
| --- |

#### Acceptance criteria

| Scenario | The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description with valid semantics (Semantic validation - positive case) |
| --- | --- |

| When|I, the Governance Authority, receive a new resource description or a new version of a resource description with valid semantics |
| --- |

| and|the Provider is authorised with the identity attributes to manage resource descriptions |
| --- |

| Then|the semantics of the content or the resource description is automatically checked |
| --- |

| and|the semantic validation for the resource description or new version of a resource description has the result "valid". |
| --- |
|  |  |

| Scenario | The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description with invalid semantics (Semantic validation - negative case) |
| --- | --- |

| When|I, the Governance Authority, receive a new resource description or a new version of a resource description with invalid semantics |
| --- |

| and|the Provider is authorised with the identity attributes to manage resource descriptions |
| --- |

| Then|the semantics of the content or the resource description is automatically checked |
| --- |

| and|the semantic validation for the resource description or new version of a resource description has the result "invalid". |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14749](../README.md) (L2) The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description |
| **Children** | [SIMPL-490](Story-SIMPL-490/README.md) (User story) Semantic Validation Engine<br>[SIMPL-491](Story-SIMPL-491/README.md) (User story) Semantic Validation of Self-Description |
