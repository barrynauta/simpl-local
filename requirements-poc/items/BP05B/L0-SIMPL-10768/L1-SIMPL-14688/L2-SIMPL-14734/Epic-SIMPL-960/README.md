> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14734](../README.md) / **Epic-SIMPL-960**

# SIMPL-960: A Provider is supported in defining a usage policy by using a policy template via API

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14734](../README.md) (L2) › **SIMPL-960** (Epic)

## Text

**As a** Provider\
**I want to** be supported in defining a usage policy through policy templates, by retrieving via API

- the list of usage policy templates;
- the details of a usage policy template.

**So that** I can correctly and easily define usage policies.

**Acceptance Criterion 1: Retrieve the list of predefined policy templates**

**Scenario:** A Provider supported in defining a usage policy, by retrieving the list of usage policy templates, via API

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request for the available usage policy templates via an API\
   **Then** Simpl-Open shall return all policy templates that can be used for defining the usage policy.

**Acceptance Criterion 2: Retrieve the details of a selected predefined policy template**

**Scenario:** A Provider supported in defining a usage policy, by retrieving the details of a usage policy template, via API

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request the details of a usage policy template via an API\
   **Then** Simpl-Open shall return the details of a policy template in a structured format that can be used for defining the usage policy.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14734](../README.md) (L2) A Provider assigns usage policies to a new Resource description |
| **Children** | [SIMPL-3241](Story-SIMPL-3241/README.md) (User story) API for Usage Policy<br>[SIMPL-31385](Story-SIMPL-31385/README.md) (User story) [API] Provider retrieves the list of usage policy templates<br>[SIMPL-31386](Story-SIMPL-31386/README.md) (User story) [API] Provider retrieves the details of a usage policy template |
