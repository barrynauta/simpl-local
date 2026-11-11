<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider manages contract clauses [MVP]

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-28989. It has 6 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

This epic serves as a collector for the user stories related to contract clauses management that were implemented as part of the MVP.

As part of R16 Contract Clauses, the related concepts have been revised.

The epics and stories that describe the implementation of contract clauses can hence be found under R16.

## User stories

1. [UI for assigning a defining billing schemas [Placeholder]](#user-story-1-ui-for-assigning-a-defining-billing-schemas-placeholder)
2. [Support document mode for Billing Schema using source + hash](#user-story-2-support-document-mode-for-billing-schema-using-source--hash)
3. [UI for assigning a defining SLA Agreements [Placeholder]](#user-story-3-ui-for-assigning-a-defining-sla-agreements-placeholder)
4. [Support document mode for SLA Agreements using source + hash](#user-story-4-support-document-mode-for-sla-agreements-using-source--hash)
5. [UI for assigning a Contract Template to a Resource](#user-story-5-ui-for-assigning-a-contract-template-to-a-resource)
6. [Support document mode for Contract Clauses using source + hash](#user-story-6-support-document-mode-for-contract-clauses-using-source--hash)

### User story 1: UI for assigning a defining billing schemas [Placeholder]

Backlog item SIMPL-509

Disclaimer: The User Story is intended to link resources that are referred as a placeholder for the billing schema, which is yet to be defined.

**As a** Provider User\
**I want to** use an UI to set the reference to the billing schema to the self-description\
**So that** I can set the billing schema for my dataset/application/infrastructure.

**Technical Specification**

The UI of the SD Tooling will be extended by displaying another field, where a URL must be inserted. The extension of the attributes is done accordingly in the Schema File where the Structure of the Self-Descriptions is based on. The Hash Value that has been created based on the URL (SIMPL-510 (internal page)) will not be displayed to the end user, it will be contained in the Json-ld after saving / signing only. Another field shall contain the hash algorithm that has been used.

AC:

1. Check if the URL field is present on the SD Tooling UI

[Back to the list of user stories](#user-stories)

### User story 2: Support document mode for Billing Schema using source + hash

Backlog item SIMPL-510

Disclaimer: This story is to support the future billing schema link and is not implementing billing in any way. The requirements related to billing need to be defined.

**As a** Provider User\
**I want to** calculate the hash of the billing schema document referenced in the self-description\
**So that** I can check if the document was modified

**Technical Specification**

Based on the user's URL input in the SD Tooling UI, the PDF File needs to be downloaded and a hash value is calculated and returned. The returned value must then be stored in the Self-Description, as well as the hash algorithm that has been used.

The hash-algorithm should be SHA-256, we need to store three properties:

- the link to the pdf
- the hash of the document
- the hashing algorithm (SHA-256)

AC:

1. Confirm that inside the JSON-LD has the hash of the document and the algorithm
1. Check if the hash value of the document calculated with a 3rd party tool has the same value as from the SD
1. When calculating several times the hash of the same document the same value must be obtained
1. Check if the URL address leads to the containing the file (Error handling in case file does not exists or Link is not reachable)

[Back to the list of user stories](#user-stories)

### User story 3: UI for assigning a defining SLA Agreements [Placeholder]

Backlog item SIMPL-512

Disclaimer: The User Story is intended to link resources that are referred as a placeholder for the SLA, which is yet to be defined.

**As a** Provider User\
**I want to** use an UI to set the reference to SLA Agreements to the self-description\
**So that** I can set the SLA Agreements for my dataset/application/infrastructure.

**Technical Specification**

The UI of the SD Tooling will be extended by displaying another field, where a URL must be inserted. The extension of the attributes is done accordingly in the Schema File where the Structure of the Self-Descriptions is based on. The Hash Value that has been created based on the URL (SIMPL-513 (internal page)) will not be displayed to the end user, it will be contained in the Json-ld after saving / signing only. Another field shall contain the hash algorithm that has been used.

AC:

1. Check if the URL field is present on the SD Tooling UI

[Back to the list of user stories](#user-stories)

### User story 4: Support document mode for SLA Agreements using source + hash

Backlog item SIMPL-513

Disclaimer: This story is to support the future SLA link and is not implementing SLA's in any way. The requirements related to SLA's need to be defined.

**As a** Provider User\
**I want to** calculate the hash of the SLA Agreements document referenced in the self-description\
**So that** I can check if the document was modified

**Technical Specification**

Based on the user's URL input in the SD Tooling UI, the PDF File needs to be downloaded and a hash value is calculated and returned. The returned value must then be stored in the Self-Description, as well as the hash algorithm that has been used.

The hash-algorithm should be SHA-256, we need to store three properties:

- the link to the pdf
- the hash of the document
- the hashing algorithm (SHA-256)

AC:

1. Confirm that inside the JSON-LD has the hash of the document and the algorithm
1. Check if the hash value of the document calculated with a 3rd party tool has the same value as from the SD
1. When calculating several times the hash of the same document the same value must be obtained

[Back to the list of user stories](#user-stories)

### User story 5: UI for assigning a Contract Template to a Resource

Backlog item SIMPL-516

Disclaimer: The User Story is intended to link resources that are referred as a placeholder for the usage contract template, which is yet to be defined.

**As a** Provider User\
**I want to** use an UI to set the reference Contract Template to the self-description\
**So that** I can set the Contract Template for my dataset/application/infrastructure.

**Technical Specification**

The UI of the SD Tooling will be extended by displaying another field, where a URL must be inserted. The extension of the attributes is done accordingly in the Schema File where the Structure of the Self-Descriptions is based on. The Hash Value that has been created based on the URL ((internal page)) will not be displayed to the end user, it will be contained in the Json-ld after saving / signing only. Another field shall contain the hash algorithm that has been used.

AC:

1. Check if the URL field is present on the SD Tooling UI

[Back to the list of user stories](#user-stories)

### User story 6: Support document mode for Contract Clauses using source + hash

Backlog item SIMPL-517

Disclaimer: This story is to support the future contract link and is not implementing contracts in any way. The requirements related to contracts need to be defined.

**As a** Provider User\
**I want to** calculate the hash of the Contract Template document referenced in the self-description\
**So that** I can check if the document was modified

**Technical Specification**

Based on the user's URL input in the SD Tooling UI, the PDF File needs to be downloaded and a hash value is calculated and returned. The returned value must then be stored in the Self-Description, as well as the hash algorithm that has been used.

The hash-algorithm should be SHA-256, we need to store three properties:

- the link to the pdf
- the hash of the document
- the hashing algorithm (SHA-256)

AC:

1. Confirm that inside the JSON-LD has the hash of the document and the algorithm
1. Check if the hash value of the document calculated with a 3rd party tool has the same value as from the SD
1. When calculating several times the hash of the same document the same value must be obtained

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
