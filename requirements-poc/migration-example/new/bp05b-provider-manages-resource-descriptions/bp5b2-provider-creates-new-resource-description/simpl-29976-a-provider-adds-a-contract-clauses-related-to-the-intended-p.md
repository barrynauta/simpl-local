<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider adds a contract clauses related to the intended purpose of the data resource to a new resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-29976. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** add a contract clause related to the intended purpose of the data resource to a new resource description via UI\
**So that** I can ensure that the Consumer agrees that they will only use the data resource for the intended purpose

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I create an access policy related to resource consent\
   **And** I upload a document related to the intended purpose of the data resource\
   **Then** Simpl-Open adds a contract clause of the type consent to the resource description linked to the uploaded document

## User stories

1. [[UI] Add Intended Purpose Contract Clause to Resource Description](#user-story-1-ui-add-intended-purpose-contract-clause-to-resource-description)
2. [[UI] Enable Consent Management Intended purpose Contract Clauses Document Download in SDTooling UI](#user-story-2-ui-enable-consent-management-intended-purpose-contract-clauses-document-download-in-sdtooling-ui)

### User story 1: [UI] Add Intended Purpose Contract Clause to Resource Description

Backlog item SIMPL-34169

###### User Story

**As a** Provider\
**I want to** add an Intended Purpose Contract Clause to a Resource Description\
**So that** Consumers explicitly agree to use the data resource only for its intended purpose.

###### Description

To support Intended Purpose Management, SD-Tooling shall allow Providers to upload and associate a Contract Clause of type **INTENDED_PURPOSE** to a Resource Description.

From a UI perspective, this Contract Clause shall be managed within the existing **Contract Clauses** step of the Resource Description wizard. The uploaded document shall comply with the structure and metadata defined in SIMPL-34167.

The Intended Purpose Contract Clause represents the agreement that the Consumer acknowledges and accepts that the data resource may only be used for the intended purpose defined by the Provider.

The **Intended Purpose Contract Clause** is optionally available within the **Contract Clauses** step of the SD-Tooling wizard when a **Resource Consent Access Policy** has been configured in the **Service Policy** section. The Provider may upload a document that will be associated with the Resource Description as an Intended Purpose Contract Clause.

###### Acceptance Criteria

###### AC1 - Enable Intended Purpose Contract Clause

1. **Given** I am authenticated as a Provider\
   **And** I have permissions to manage Resource Descriptions\
   **And** I am creating or editing a Resource Description\
   **And** a Resource Consent Access Policy has been configured in the Service Policy section\
   **When** I access the Contract Clauses step of the wizard\
   **Then** the Intended Purpose Contract Clause option is available for configuration\
   **And** the Provider may optionally upload a document associated with the Intended Purpose Contract Clause

###### AC2 - Upload Intended Purpose Contract Clause Document

2. **Given** a Resource Consent Access Policy has been configured\
   **And** I am in the Contract Clauses step of the wizard\
   **When** I upload a document for the Intended Purpose Contract Clause\
   **Then** the document is validated and stored inside provider storage\
   **And** an Intended Purpose Contract Clause is associated with the Resource Description\
   **And** the uploaded document is linked to the created Contract Clause

### User story 2: [UI] Enable Consent Management Intended purpose Contract Clauses Document Download in SDTooling UI

Backlog item SIMPL-34486

**As a** Provider,\
**I want to** download the documents associated with Contract Clauses categories (Intended purpose) for the resources published on the Catalogue,\
**So that** I can verify and review the related contractual documentation.

**Technical Description**\
This task aims to enhance the Contract Clauses section of the SDTooling UI by providing access to documents uploaded by the provider. For each Contract Clauses category (Intended Purpose), SD-Tooling UI shall verify whether the document is available. Whenever the document exists for a given category (Intended Purpose), the application shall display a **Download** button associated with that document. Upon user interaction, SD-Tooling UI shall invoke the API exposed by **xfsc-adv-search component** implemented under **ticket** SIMPL-29049 to retrieve the selected document and download it to the user's device.

**Acceptance Criteria**

###### AC1 - Display Download Action

1. **Given** the Provider is viewing the Contract Clauses section of a published resource\
   **And** the document is available for an Intended Purpose Contract Clauses\
   **When** the section is displayed\
   **Then** a Download button shall be displayed for each available document\
   **And** the document name shall be visible to the user.

###### AC2 - Successful Download

2. **Given** a Download button is displayed for a document\
   **When** the Provider clicks the Download button\
   **And** the document is successfully retrieved by the backend service\
   **Then** the document shall be downloaded to the user's device.

###### AC3 - No Documents Available

3. **Given** the Provider is viewing a Contract Clauses category for which no documents are available\
   **When** the Contract Clauses section is displayed\
   **Then** no Download button shall be displayed for that category.

###### AC4 - Download Failure Handling

4. **Given** a Download button is displayed for a document\
   **When** the Provider clicks the Download button\
   **And** the document cannot be retrieved due to an API or backend error\
   **Then** the download shall not start\
   **And** the user shall be notified with an appropriate error message displayed within the dedicated SD-Tooling error notification area located at the top of the page containing following message "A technical issue has occurred, and it is not possible to access the file content"

.

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
