> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14865](../README.md) / **Epic-SIMPL-17800**

# SIMPL-17800: The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14865](../README.md) (L2) › **SIMPL-17800** (Epic)

## Text

#### Storycard

| As|Governance Authority |
| --- |

| I want|to notify Providers about a wrong version number of a new version of a resource description |
| --- |

| so that|the Provider can correct the error. |
| --- |

#### Acceptance criteria

| Given|an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority |
| --- |

| and|I have received a new resource description or a new version of a resource description with an incorrect version number |
| --- |

| When|it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm |
| --- |

| and|version number validation for the new version of a Resource description has the result "invalid". |
| --- |

| Then|a notification about an version numbering error is sent to the Provider. |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14865](../README.md) (L2) The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description |
| **Children** | [SIMPL-21594](Story-SIMPL-21594/README.md) (User story) [API] The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description |
