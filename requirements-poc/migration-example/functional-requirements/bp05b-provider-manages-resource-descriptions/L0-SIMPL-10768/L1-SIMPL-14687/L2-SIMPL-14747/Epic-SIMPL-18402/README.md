> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14747](../README.md) / **Epic-SIMPL-18402**

# SIMPL-18402: The Governance Authority validates the syntax of the policies assigned to a new Resource description or a new version of a Resource Description

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14747](../README.md) (L2) › **SIMPL-18402** (Epic)

## Text

**As a** Governance Authority\
**I want to** automatically validate the syntax of policies in a new resource description or a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

**Acceptance Criterion 1: Syntax validation access policies**

**Scenario:** The Governance Authority validates the syntax of the access policies assigned to a new resource description or a new version of a resource description\
When I receive a new resource description or a new version of a resource description\
and the sender has the identity attributes to manage resource descriptions\
then the syntax of the access policies of the resource description is automatically validated, which includes verifying the correct usage of action verbs and ODRL constraints.

**Acceptance Criterion 2: Syntax validation usage policies**

**Scenario:** The Governance Authority validates the syntax of the usage policies assigned to a new resource description or a new version of a resource description\
When I receive a new resource description or a new version of a resource description\
and the sender has the identity attributes to manage resource descriptions\
then the syntax of the usage policies of the resource description is automatically validated.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14747](../README.md) (L2) The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description |
| **Children** | [SIMPL-31673](Story-SIMPL-31673/README.md) (User story) [API] GA validates the syntax of access and usage policies of new RD version<br>[SIMPL-31674](Story-SIMPL-31674/README.md) (User story) [UI] Provider sees the policy syntax validation outcome in the RD editor for new RD version |
