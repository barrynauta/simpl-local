<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider adds contract clauses related to contract consent to a new resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-29977, status **Defined**. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** add a contract clause related to contract consent of the data resource to a new resource description via UI\
**So that** the Consumer agrees that contract consent (and thus access to the resource) can be withdrawn

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I create an access policy related to resource consent\
   **And** I indicate that contract consent is required for each new usage contract\
   **And** I upload a document related to the contract consent status changes\
   **Then** Simpl-Open adds a contract clause of the type consent to the resource description linked to the uploaded document

## User stories

1. [[UI] Add Contract Consent Contract Clause to Resource Description](#user-story-1-ui-add-contract-consent-contract-clause-to-resource-description) (FOR SC TO RESPOND)
2. [[UI] Enable Contract Consent Management Contract Clauses Document Download in SDTooling UI](#user-story-2-ui-enable-contract-consent-management-contract-clauses-document-download-in-sdtooling-ui) (FOR SC TO RESPOND)

### User story 1: [UI] Add Contract Consent Contract Clause to Resource Description

**Status:** FOR SC TO RESPOND (backlog item SIMPL-34170)

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

### User story 2: [UI] Enable Contract Consent Management Contract Clauses Document Download in SDTooling UI

**Status:** FOR SC TO RESPOND (backlog item SIMPL-34487)

**As a** Provider,\
**I want to** download the documents associated with Contract Clauses categories (Contract Consent) for the resources published on the Catalogue,\
**So that** I can verify and review the related contractual documentation.

**Technical Description**\
This task aims to enhance the Contract Clauses section of the SDTooling UI by providing access to documents uploaded by the provider. For each Contract Clauses category (Contract Consent), SD-Tooling UI shall verify whether the document is available. Whenever the document exists for a given category (Contract Consent), the application shall display a **Download** button associated with that document. Upon user interaction, SD-Tooling UI shall invoke the API exposed by **xfsc-adv-search component** implemented under **ticket** SIMPL-29049 to retrieve the selected document and download it to the user's device.

**Acceptance Criteria**

###### AC1 - Display Download Action

1. **Given** the Provider is viewing the Contract Clauses section of a published resource\
   **And** the document is available for a Contract Consent Contract Clauses\
   **When** the section is displayed\
   **Then** a Download button shall be displayed for each available document\
   **And** the document name shall be visible to the user.

###### AC2 - Successful Download

2. **Given** a Download button is displayed for a document\
   **When** the Provider clicks the Download button\
   **And** the document is successfully retrieved by the backend service\
   **Then** the document shall be downloaded to the user's device.

###### AC4 - No Documents Available

3. **Given** the Provider is viewing a Contract Clauses category for which no documents are available\
   **When** the Contract Clauses section is displayed\
   **Then** no Download button shall be displayed for that category (Contract Consent).

###### AC5 - Download Failure Handling

4. **Given** a Download button is displayed for a document\
   **When** the Provider clicks the Download button\
   **And** the document cannot be retrieved due to an API or backend error\
   **Then** the download shall not start\
   **And** the user shall be notified with an appropriate error message displayed within the dedicated SD-Tooling error notification area located at the top of the page containing following message "A technical issue has occurred, and it is not possible to access the file content"

.

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
