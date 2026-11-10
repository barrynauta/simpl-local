> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-29975](../README.md) / **Story-SIMPL-34485**

# SIMPL-34485: [UI] Enable Consent Management Contract Clauses Document Download in SDTooling UI

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-29975](../README.md) (Epic) › **SIMPL-34485** (User story)

## Text

**As a** Provider,\
**I want** to download the documents associated with Contract Clauses categories (Resource Consent) for the resources published on the Catalogue,\
**so that** I can verify and review the related contractual documentation.

**Technical Description**\
This task aims to enhance the Contract Clauses section of the SDTooling UI by providing access to documents uploaded by the provider. For each Contract Clauses category (Resource Consent), SD-Tooling UI shall verify whether the document is available. Whenever the document exists for a given category (resource consent), the application shall display a **Download** button associated with that document. Upon user interaction, SD-Tooling UI shall invoke the API exposed by **xfsc-adv-search component** implemented under **ticket** SIMPL-29049 to retrieve the selected document and download it to the user's device.

**Acceptance Criteria**

###### AC1 - Display Download Action

**Given** the Provider is viewing the Contract Clauses section of a published resource\
**And** the document is available for a Resource Consent Contract Clauses\
**When** the section is displayed\
**Then** a **Download** button shall be displayed for each available document\
**And** the document name shall be visible to the user.

###### AC2 - Successful Download

**Given** a **Download** button is displayed for a document\
**When** the Provider clicks the **Download** button\
**And** the document is successfully retrieved by the backend service\
**Then** the document shall be downloaded to the user's device.

###### AC3 - No Documents Available

**Given** the Provider is viewing a Contract Clauses category for which no documents are available\
**When** the Contract Clauses section is displayed\
**Then** no **Download** button shall be displayed for that category.

###### AC4 - Download Failure Handling

**Given** a **Download** button is displayed for a document\
**When** the Provider clicks the **Download** button\
**And** the document cannot be retrieved due to an API or backend error\
**Then** the download shall not start\
**And** the user shall be notified with an appropriate error message displayed within the dedicated SD-Tooling error notification area located at the top of the page containing following message "A technical issue has occurred, and it is not possible to access the file content"

.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-29975](../README.md) (Epic) A Provider adds a contract clauses related resource consent of the data resource to a new resource description via UI |
