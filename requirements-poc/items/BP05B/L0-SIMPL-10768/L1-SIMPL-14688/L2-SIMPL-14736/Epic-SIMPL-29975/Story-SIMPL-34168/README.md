> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-29975](../README.md) / **Story-SIMPL-34168**

# SIMPL-34168: [UI] Add Resource Consent Contract Clause to Resource Description

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-29975](../README.md) (Epic) › **SIMPL-34168** (User story)

## Text

###### User Story

**As a** Provider\
**I want** to add a Resource Consent Contract Clause to a Resource Description\
**So that** Consumers explicitly agree that access to the resource may be revoked if a Data Subject withdraws consent.

###### Description

To support Resource Consent Management, SD-Tooling shall allow Providers to upload and associate a Consent Contract Clause to a Resource Description.

From a UI perspective, this Contract Clause shall be managed within the existing **Contract Clauses** step of the Resource Description wizard. The uploaded document shall comply with the structure and metadata defined in SIMPL-34167.

The Resource Consent Contract Clause represents the agreement that the Consumer acknowledges that the Provider may change the status of the related Resource Consent Assertion to WITHDRAWN, resulting in the suspension of access to the resource when a Data Subject withdraws consent.

When a **Resource Consent Access Policy** is configured within the **Service Policy** section, the **Resource Consent Contract Clause** becomes mandatory and the Provider is required to upload the related document in the **Contract Clauses** step before the Resource Description can be saved or published.

###### Acceptance Criteria

**AC1 - Resource Consent Contract Clause Availability and Mandatory Upload**

**Given** I am authenticated as a Provider\
**And** I am creating or editing a Resource Description\
**And** a Resource Consent Access Policy has been configured in the Service Policy section\
**When** I access the Contract Clauses step of the wizard\
**Then** the Resource Consent Contract Clause is available and mandatory\
**And** the Provider must upload a document associated with the Resource Consent Contract Clause

**AC2 - Upload Resource Consent Contract Clause Document**

**Given** a Resource Consent Access Policy has been configured\
**When** I upload a document for the Resource Consent Contract Clause\
**Then** the document is stored in provider storage\
**And** the Contract Clause is associated with the Resource Description

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-29975](../README.md) (Epic) A Provider adds a contract clauses related resource consent of the data resource to a new resource description via UI |
