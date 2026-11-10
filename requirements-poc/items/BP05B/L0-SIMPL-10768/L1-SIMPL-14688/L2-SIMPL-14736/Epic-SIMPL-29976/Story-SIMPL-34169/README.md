> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-29976](../README.md) / **Story-SIMPL-34169**

# SIMPL-34169: [UI] Add Intended Purpose Contract Clause to Resource Description

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-29976](../README.md) (Epic) › **SIMPL-34169** (User story)

## Text

###### User Story

**As a** Provider\
**I want** to add an Intended Purpose Contract Clause to a Resource Description\
**So that** Consumers explicitly agree to use the data resource only for its intended purpose.

###### Description

To support Intended Purpose Management, SD-Tooling shall allow Providers to upload and associate a Contract Clause of type **INTENDED_PURPOSE** to a Resource Description.

From a UI perspective, this Contract Clause shall be managed within the existing **Contract Clauses** step of the Resource Description wizard. The uploaded document shall comply with the structure and metadata defined in SIMPL-34167.

The Intended Purpose Contract Clause represents the agreement that the Consumer acknowledges and accepts that the data resource may only be used for the intended purpose defined by the Provider.

The **Intended Purpose Contract Clause** is optionally available within the **Contract Clauses** step of the SD-Tooling wizard when a **Resource Consent Access Policy** has been configured in the **Service Policy** section. The Provider may upload a document that will be associated with the Resource Description as an Intended Purpose Contract Clause.

###### Acceptance Criteria

###### AC1 - Enable Intended Purpose Contract Clause

**Given** I am authenticated as a Provider\
**And** I have permissions to manage Resource Descriptions\
**And** I am creating or editing a Resource Description\
**And** a Resource Consent Access Policy has been configured in the Service Policy section\
**When** I access the Contract Clauses step of the wizard\
**Then** the Intended Purpose Contract Clause option is available for configuration\
**And** the Provider may optionally upload a document associated with the Intended Purpose Contract Clause

###### AC2 - Upload Intended Purpose Contract Clause Document

**Given** a Resource Consent Access Policy has been configured\
**And** I am in the Contract Clauses step of the wizard\
**When** I upload a document for the Intended Purpose Contract Clause\
**Then** the document is validated and stored inside provider storage\
**And** an Intended Purpose Contract Clause is associated with the Resource Description\
**And** the uploaded document is linked to the created Contract Clause

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-29976](../README.md) (Epic) A Provider adds a contract clauses related to the intended purpose of the data resource to a new resource description via UI |
