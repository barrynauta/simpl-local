> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14750](../../README.md) / [Epic-SIMPL-18040](../README.md) / **Story-SIMPL-21593**

# SIMPL-21593: [API] The Governance Authority notifies a Provider about semantic validation errors

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14750](../../README.md) (L2) › [SIMPL-18040](../README.md) (Epic) › **SIMPL-21593** (User story)

## Text

#### Storycard

| As|Governance Authority |
| --- |

| I want|to notify a Provider about  invalid semantics of a new resource description or a new version of a resource description |
| --- |

| so that|the Provider can correct the errors. |
| --- |

#### Acceptance criteria

*Note that notifications about invalid policies are not part of this story*

| Scenario | The Governance Authority notifies a Provider about semantic validation errors (Semantic validation - negative case) |
| --- | --- |

| Given|I receive a new resource description or a new version of a resource description with invalid semantics |
| --- |

| When|the semantics of the content or the resource description is automatically checked |
| --- |

| Then|a notification about invalid semantics is sent to the Provider. |
| --- |

#### Technical details

A http response with a suitable error code and a meaningful error description is sufficient.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18040](../README.md) (Epic) The Governance Authority provides feedback to a Provider about semantic validation errors |
