> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14733](../README.md) / **Epic-SIMPL-935**

# SIMPL-935: A Provider is supported in defining an access policy by a using policy template via API

**Epic (deliverable, testable feature)** · Status **In Progress**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14733](../README.md) (L2) › **SIMPL-935** (Epic)

## Text

**As a** Provider\
**I want to** be supported in defining an access policy through policy templates, by retrieving via API:

- the list of access policy templates;
- the details of an access policy template.

**So that** I can correctly and easily define access policies.

**Acceptance Criterion 1: Retrieve the list of predefined policy templates**

**Scenario:** A Provider supported in defining an access policy, by retrieving the list of access policy templates, via API

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request for the available access policy templates via an API\
   **Then** Simpl-Open shall return all policy templates that can be used for defining the access policy.

**Acceptance Criterion 2: Retrieve the details of a selected predefined policy template**

**Scenario:** A Provider supported in defining an access policy, by retrieving the details of an access policy template, via API

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request the details of an access policy template via an API\
   **Then** Simpl-Open shall return the details of a policy template in a structured format that can be used for defining the access policy.

**Acceptance Criterion 3: Provide predefined policy templates**

The Provider can use predefined policy templates to create policies. Simpl-Open will support the Provider by making predefined policy templates available and documentation on how to use them.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14733](../README.md) (L2) A Provider assigns access policies to a new Resource description |
| **Children** | [SIMPL-2938](Story-SIMPL-2938/README.md) (User story) API for Access Policy<br>[SIMPL-3546](Story-SIMPL-3546/README.md) (User story) [API] Access Policy multiple attributes per Permission<br>[SIMPL-31346](Story-SIMPL-31346/README.md) (User story) [API] Provider retrieves the list of access policy templates<br>[SIMPL-31347](Story-SIMPL-31347/README.md) (User story) [API] Provider retrieves the details of an access policy template |
