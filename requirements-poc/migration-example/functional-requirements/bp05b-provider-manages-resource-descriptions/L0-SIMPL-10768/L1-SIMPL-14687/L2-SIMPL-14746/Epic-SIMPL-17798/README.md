> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14746](../README.md) / **Epic-SIMPL-17798**

# SIMPL-17798: The Governance Authority notifies a Provider about an invalid schema

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14746](../README.md) (L2) › **SIMPL-17798** (Epic)

## Text

#### Storycard

**As** Governance Authority\
**I want to** notify a Provider about an invalid schema of a new resource description or a new version of a resource description\
**So that** the Provider can correct the errors.

#### Acceptance criteria

**Scenario:** The Governance Authority notifies a Provider about an inactive schema of a new resource description or a new version of a resource description (Schema status validation - notification in negative case)

1. **Given** I have received a new resource description or a new version of a resource description with an inactive schema\
   **When** it is checked if the schema of that resource description is active\
   **And** schema status validation for the resource description or new version of a resource description has the result "invalid"\
   **Then** a notification about an inactive schema error is sent to the Provider.

**Scenario:** The Governance Authority notifies a Provider about a different resource type of a new version of a resource description (Schema type validation - notification in negative case)

2. **Given** I have received a new version of a resource description with resource type different to the resource type of the previous version\
   **When** it is checked if the schema of that resource description has the same resource type as the previous version\
   **And** schema type validation for the new version of the resource description has the result "invalid"\
   **Then** a notification about a wrong resource type is sent to the Provider.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14746](../README.md) (L2) The Governance Authority notifies a Provider about an inactive schema |
| **Children** | [SIMPL-21597](Story-SIMPL-21597/README.md) (User story) [API] The Governance Authority notifies a Provider about an invalid schema |
