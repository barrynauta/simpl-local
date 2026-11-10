> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-29973](../README.md) / **Story-SIMPL-34175**

# SIMPL-34175: [UI] Configure Resource Consent Access Policy for Resource Descriptions

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-29973](../README.md) (Epic) › **SIMPL-34175** (User story)

## Text

###### User Story

**As a** Provider\
**I want** to configure a Resource Consent Access Policy for a Resource Description\
**So that** access to the resource can be governed according to the status of a Resource Consent Assertion.

###### Description

SD-Tooling shall allow a Provider to create a Resource Consent Access Policy within the **Access Policy** step of the Resource Description wizard.

During the configuration, the Provider shall select a Resource Consent Assertion from the available assertions.

The selected assertion shall be associated with the newly created Resource Consent Access Policy and the policy shall be assigned to the Resource Description.

###### Acceptance Criteria

**AC1 - Select Resource Consent Assertion**

**Given** I am creating or editing a Resource Description\
**When** I access the Access Policy step\
**Then** I can retrieve and consult the available Resource Consent Assertions

**AC2 - Create Resource Consent Access Policy**

**Given** I am in the Access Policy step\
**When** I select a Resource Consent Assertion\
**Then** a Resource Consent Access Policy is created\
**And** the selected Resource Consent Assertion is linked to the policy

**AC3 - Assign Policy to Resource Description**

**Given** a Resource Consent Access Policy has been created\
**When** the Resource Description is saved\
**Then** the policy is assigned to the Resource Description

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-29973](../README.md) (Epic) A Provider assigns access policies related to resource consent to a new resource description via UI |
