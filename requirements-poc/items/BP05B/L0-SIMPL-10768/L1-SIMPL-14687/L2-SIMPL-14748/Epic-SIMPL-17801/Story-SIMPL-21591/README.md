> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14748](../../README.md) / [Epic-SIMPL-17801](../README.md) / **Story-SIMPL-21591**

# SIMPL-21591: [API] The Governance Authority notifies a Provider about syntax validation errors

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14748](../../README.md) (L2) › [SIMPL-17801](../README.md) (Epic) › **SIMPL-21591** (User story)

## Text

#### Storycard

**As** Governance Authority\
**I want to** notify a Provider about invalid syntax of a new resource description or a new version of a resource description\
**So that** the Provider can correct the errors.

#### Acceptance criteria

*Note that syntax validation errors of policies are not part of this story*

**Scenario:** The Governance Authority notifies a Provider about syntax validation errors (Syntax validation - negative case)

1. **Given** I have received a new resource description or a new version of a resource description with invalid syntax\
   **When** the syntax of the content of the resource description is automatically checked\
   **Then** a notification about invalid syntax is sent to the Provider.

#### Technical details

A http response with a suitable error code and a meaningful error description is sufficient.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17801](../README.md) (Epic) The Governance Authority notifies a Provider about syntax validation errors |
