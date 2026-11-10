> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-29974](../README.md) / **Story-SIMPL-34176**

# SIMPL-34176: [UI] Configure Contract Consent Access Policy for Resource Descriptions

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-29974](../README.md) (Epic) › **SIMPL-34176** (User story)

## Text

###### User Story

**As a** Provider\
**I want to** configure a Contract Consent Access Policy for a Resource Description\
**So that** access to the resource can also be governed according to the status of Contract Consent Assertions associated with usage contracts.

###### Description

SD-Tooling shall allow a Provider to enable Contract Consent management when configuring a Resource Consent Access Policy.

A Contract Consent Access Policy can only be configured when a Resource Consent Assertion has already been selected.

When enabled, SD-Tooling shall create and associate a Contract Consent Access Policy to the Resource Description, indicating that each usage contract must be governed by a Contract Consent Assertion.

###### Acceptance Criteria

**AC1 - Enable Contract Consent Management**

1. **Given** a Resource Consent Access Policy has been configured\
   **When** I access the Service Policy step\
   **Then** I can enable the Contract Consent option

**AC2 - Create Contract Consent Access Policy**

2. **Given** a Resource Consent Access Policy exists\
   **When** I enable the Contract Consent option\
   **Then** a Contract Consent Access Policy is created

**AC3 - Assign Policy to Resource Description**

3. **Given** a Contract Consent Access Policy has been created\
   **When** the Resource Description is saved\
   **Then** the Contract Consent Access Policy is assigned to the Resource Description

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-29974](../README.md) (Epic) A Provider assigns access policies related to contract consent to a new resource description via UI |
