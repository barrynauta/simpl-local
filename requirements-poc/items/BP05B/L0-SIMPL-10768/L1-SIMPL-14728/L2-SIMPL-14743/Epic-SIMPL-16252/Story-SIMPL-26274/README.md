> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14743](../../README.md) / [Epic-SIMPL-16252](../README.md) / **Story-SIMPL-26274**

# SIMPL-26274: A Provider updates the contract clauses, resulting in a new version of the resource description via UI

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14743](../../README.md) (L2) › [SIMPL-16252](../README.md) (Epic) › **SIMPL-26274** (User story)

## Text

**As a** Provider end user\
**I want to** update the data of a contract clause via UI\
**So that** I can ensure that the new version of the resource description includes the updated information to be compliant with the schema of the contract clause

**Acceptance Criteria**

**AC1 - A Provider updates the contract clauses of a resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **Then** the UI shows the properties of the contract clauses prefilled with the values from the previous version\
   **And** I can update the values of the properties of existing contract clauses, including the linked document\
   **And** I can add new contract clauses

**AC2 - A Provider finishes updating a contract clause via UI, resulting in a new version of a resource description**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to update a contract clause in the UI wizard,\
   **When** I provide the updated values of the properties or delete the properties of the selected contract clause and/or upload a new document\
   **Then** the updated contract clause is associated with the new version of the resource description\
   **And** the uploaded document (if any) is stored and the document metadata is generated (documentId, hash, hashAlgorithm) and linked to the contract clause\
   **And** I can navigate to the next step in the wizard

**AC3.1 - A Provider adds a new contract clause of type generic, licence agreement, billing schema to a resource description via UI**

3. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I have not yet added a contract clause of type generic or licence agreement or billing schema\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause in the UI wizard,\
   **And** I select one of the contract clause types that was not added before (generic or licence agreement or billing schema)\
   **Then** the UI shows the properties of the contract clause for the selected contract clause type

**AC3.2 - A Provider adds a new contract clause of type SLA to a resource description via UI**

4. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause in the UI wizard,\
   **And** I select the contract clause type SLA\
   **Then** the UI shows the properties of the contract clause for the selected contract clause type

**AC3.3 - A Provider finishes adding a new contract clause to a new version of a resource description via UI**

5. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause to the new version of the resource description in the UI wizard\
   **When** I provide the values of the properties of the contract clause and (optionally) upload a document\
   **Then** the new contract clause is associated with the new version of the resource description\
   **And** the uploaded document (if any) is stored and the document metadata is generated (documentId, hash, hashAlgorithm) and linked to the contract clause\
   **And** I can navigate to the next step in the wizard

**AC4 - Syntactically invalid data metadata prevents submission of the resource description via UI**

6. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I enter a value in a contract clauses metadata field that does not comply with the field validation rules of the contract clause\
   **Then** the invalid field is visually highlighted\
   **And** an error message is displayed next to the field indicating that the value is not valid\
   **And** the Sign & Submit action is disabled\
   **And** I cannot proceed with the publication of the resource description until all invalid values are corrected

**AC5 - A Provider uploads a document when adding/updating a contract clause to a new version of a resource description via UI, but decides to replace it before submission**

7. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have chosen to update or add a new contract clause in the UI wizard,\
   **And** I have provided the values of the properties of the contract clause\
   **And** I have uploaded a document\
   **When** I want to change the document before submission\
   **Then** I can click on the button to select another document\
   **And** the new uploaded document is stored and the document metadata is generated (documentId, hash, hashAlgorithm) and linked to the contract clause (overwriting the previously linked metadata)\
   **And** I can navigate to the next step in the wizard

**AC6 - A Provider uploads a document when adding/updating a contract clause to a new version of a resource description via UI, but decides to remove it before submission**

8. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have chosen to update or add a new contract clause in the UI wizard,\
   **And** I have provided the values of the properties of the contract clause\
   **And** I have uploaded a document\
   **When** I want to change the document before submission\
   **Then** I can click on the button to remove the current document\
   **And** the previously linked document metadata is unlinked from the contract clause (overwriting the previously linked metadata)\
   **And** I can navigate to the next step in the wizard

**References / Supporting Material / Documentation / Validation rules:** Contract Clauses management (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-16252](../README.md) (Epic) A Provider updates the contract clauses, resulting in a new version of the resource description via UI |
