> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14748](../README.md) / **Epic-SIMPL-25371**

# SIMPL-25371: The Governance Authority provides feedback to a Provider about syntax validation errors of a policy

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14748](../README.md) (L2) › **SIMPL-25371** (Epic)

## Text

**As a** Governance Authority

**I want to** provide feedback to a Provider about invalid syntax of a policy in a resource description\
**so that** the Provider can correct the errors.

**Acceptance Criterion 1: Provide feedback about syntax validation errors of an access policies**

| Scenario|The Governance Authority provides feedback to a Provider about syntax validation errors of an access policy (Syntax validation - negative case) |
| --- |

| Given|I have received a new resource description or a new version of a resource description with invalid syntax of an access policy |
| --- |

| When|the syntax of this policy in the resource description is automatically checked |
| --- |

| Then|a feedback about invalid syntax of the access policy is sent to the Provider with details |
| --- |

**Acceptance Criterion 2: Provide feedback about syntax validation errors of a usage policies**

| Scenario|The Governance Authority provides feedback to a Provider about syntax validation errors of an usage policy (Syntax validation - negative case) |
| --- |

| Given|I have received a new resource description or a new version of a resource description with invalid syntax of a usage policy |
| --- |

| When|the syntax of this policy in the resource description is automatically checked |
| --- |

| Then|a feedback about invalid syntax of the usage policy is sent to the Provider with details |
| --- |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14748](../README.md) (L2) The Governance Authority provides feedback to a Provider about syntax validation errors |
| **Children** | [SIMPL-31640](Story-SIMPL-31640/README.md) (User story) [API] The Governance Authority provides feedback to a Provider about policy syntax validation errors<br>[SIMPL-31641](Story-SIMPL-31641/README.md) (User story) [UI] The Provider views policy syntax validation errors in the resource description editor |
