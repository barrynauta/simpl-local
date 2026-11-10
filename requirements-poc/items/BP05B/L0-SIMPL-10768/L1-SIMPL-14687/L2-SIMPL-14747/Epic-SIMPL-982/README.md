> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14747](../README.md) / **Epic-SIMPL-982**

# SIMPL-982: The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14747](../README.md) (L2) › **SIMPL-982** (Epic)

## Text

#### Storycard

**As** Governance Authority\
**I want to** automatically check the syntax of a new resource description or a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

**Scenario:** The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description with valid syntax (Syntax validation - positive case)\
When I, the Governance Authority, receive a new resource description or a new version of a resource description with valid syntax\
and the Provider is authorised with the identity attributes to manage resource descriptions\
then the syntax of the content of the resource description is automatically checked\
and the syntax validation for the resource description or new version of the resource description has the result "valid".

**Scenario:** The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description with invalid syntax (Syntax validation - negative case)\
When I, the Governance Authority, receive a new resource description or a new version of a resource description with invalid syntax\
and the Provider is authorised with the identity attributes to manage resource descriptions\
then the syntax of the content of the resource description is automatically checked\
and the syntax validation for the resource description or new version of the resource description has the result "invalid".

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14747](../README.md) (L2) The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description |
| **Children** | [SIMPL-488](Story-SIMPL-488/README.md) (User story) Syntax Validation Engine |
