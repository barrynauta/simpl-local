> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-16251](../README.md) / **Story-SIMPL-33084**

# SIMPL-33084: [UI] Enable Contract Clauses Document Download in CatalogueUI

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-16251](../README.md) (Epic) › **SIMPL-33084** (User story)

## Text

**As a** Provider or Consumer,\
**I want** to download the documents associated with Contract Clauses categories (SLA, License, Billing, and Generic) for the resources published on the Catalogue,\
**so that** I can verify and review the related contractual documentation.

**Technical Description**\
This task aims to enhance the Contract Clauses section of the Catalogue UI by providing access to documents uploaded by the provider. For each Contract Clauses category (SLA, License, Billing, and Generic), Catalogue UI shall verify whether one or more supporting documents are available. Whenever at least one document exists for a given category, the application shall display a **Download** button associated with that document. Upon user interaction, SD-Tooling UI shall invoke the API exposed by **xfsc-adv-search component** implemented under **ticket** SIMPL-29049 to retrieve the selected document and download it to the user's device.

**Acceptance Criteria**

###### AC1 - Display Download Action

**Given** the Provider or Consumer is viewing the Contract Clauses section of a published resource\
**And** one or more documents are available for a Contract Clauses category (SLA, License, Billing, or Generic)\
**When** the section is displayed\
**Then** a **Download** button shall be displayed for each available document\
**And** the document name shall be visible to the user.

###### AC2 - Successful Download

**Given** a **Download** button is displayed for a document\
**When** the Provider or Consumer clicks the **Download** button\
**And** the document is successfully retrieved by the backend service\
**Then** the document shall be downloaded to the user's device.

###### AC3 - No Documents Available

**Given** the Provider or Consumer is viewing a Contract Clauses category for which no documents are available\
**When** the Contract Clauses section is displayed\
**Then** no **Download** button shall be displayed for that category.

###### AC4 - Download Failure Handling

**Given** a **Download** button is displayed for a document\
**When** the Provider or Consumer clicks the **Download** button\
**And** the document cannot be retrieved due to an API or backend error\
**Then** the download shall not start\
**And** the user shall be notified with an appropriate error message indicating that the document could not be downloaded.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-16251](../README.md) (Epic) A Provider adds contract clauses to a new Resource description via UI |
