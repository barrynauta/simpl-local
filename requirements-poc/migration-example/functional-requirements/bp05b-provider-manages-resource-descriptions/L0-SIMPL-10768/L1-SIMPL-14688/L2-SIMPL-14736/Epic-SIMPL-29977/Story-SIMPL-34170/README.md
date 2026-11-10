> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-29977](../README.md) / **Story-SIMPL-34170**

# SIMPL-34170: [UI] Add Contract Consent Contract Clause to Resource Description

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-29977](../README.md) (Epic) › **SIMPL-34170** (User story)

## Text

###### User Story

**As a** Provider\
**I want to** add a Contract Consent Contract Clause to a Resource Description\
**So that** Consumers explicitly agree that contract consent may be withdrawn, resulting in the revocation of access rights granted through the usage contract.

###### Description

To support Contract Consent Management, SD-Tooling shall allow Providers to upload and associate a Contract Clause of type **CONTRACT** to a Resource Description.

From a UI perspective, this Contract Clause shall be managed within the existing **Contract Clauses** step of the Resource Description wizard. The uploaded document shall comply with the structure and metadata defined in SIMPL-34167.

The Contract Consent Contract Clause represents the agreement that the Consumer acknowledges that the Provider may change the status of a Contract Consent Assertion to WITHDRAWN, causing the corresponding usage contract and access rights to become invalid.

The **Contract Consent Contract Clause** shall be mandatory whenever:

- a **Resource Consent Access Policy** has been configured in the **Service Policy** section; and
- the Provider has indicated that Contract Consent is required for each new usage contract.

In such cases, the Provider must upload the related contractual document within the **Contract Clauses** step of the SD-Tooling wizard.

###### Acceptance Criteria

###### AC1 - Contract Consent Contract Clause Availability

1. **Given** I am authenticated as a Provider\
   **And** I have permissions to manage Resource Descriptions\
   **And** I am creating or editing a Resource Description\
   **And** a Resource Consent Access Policy has been configured in the Service Policy section\
   **And** Contract Consent has been enabled for each new usage contract\
   **When** I access the Contract Clauses step of the wizard\
   **Then** the Contract Consent Contract Clause is available and mandatory

###### AC2 - Upload Contract Consent Contract Clause Document

2. **Given** the Contract Consent Contract Clause is mandatory\
   **When** I upload a document for the Contract Consent Contract Clause\
   **Then** the document is validated and stored inside provider storage\
   **And** a Contract Consent Contract Clause is associated with the Resource Description\
   **And** the uploaded document is linked to the created Contract Clause

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-29977](../README.md) (Epic) A Provider adds contract clauses related to contract consent to a new resource description via UI |
