> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-16251](../README.md) / **Story-SIMPL-29384**

# SIMPL-29384: A Provider adds a generic contract clause to a new resource description via UI

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-16251](../README.md) (Epic) › **SIMPL-29384** (User story)

## Text

**As a** Provider end user\
**I want to** to add the data of a generic contract clause to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the schema of the generic contract clause

**Acceptance Criteria**

**AC1 - A Provider adds a generic contract clause to a new resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new generic contract clause in the UI wizard,\
   **Then** the UI shows the properties of the generic contract clause type:

   - Title;
   - Source.

**AC2 - A Provider finishes adding a generic contract clause to a new resource description via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new generic contract clause in the UI wizard,\
   **When** I provide the values of the properties of the generic contract clause type:

   - Title: Text;
   - Source: Button to select and upload a document.

   **Then** a generic contract clause is associated with the current resource description being created\
   **And** the uploaded document (if any) is stored and the document metadata is generated (documentId, hash, hashAlgorithm) and linked to the contract clause\
   **And** I can navigate to the next step in the wizard

*Note: The upload of the document is covered in (internal page)

**AC3 - Syntactically invalid data metadata prevents submission of the resource description via UI**

3. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I enter a value in a data metadata field that does not comply with the field validation rules of the contract clause\
   **Then** the invalid field is visually highlighted\
   **And** an error message is displayed next to the field indicating that the value is not valid\
   **And** the Sign & Submit action is disabled\
   **And** I cannot proceed with the publication of the resource description until all invalid values are corrected

**AC4 - A Provider is only allowed to assign one generic contract clause to a resource description**

4. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have already added a generic contract clause in the UI wizard\
   **When** I add another Contract Clause\
   **Then** the UI does not show the option to select the type 'Generic contract clause' for this contract clause

**AC5 - A Provider uploads a document when creating a new resource description via UI, but decides to replace it before submission**

5. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have chosen to add a new contract clause in the UI wizard,\
   **And** I have provided the values of the properties of the contract clause\
   **And** I have uploaded a document\
   **When** I want to change the document before submission\
   **Then** I can click on the button to select another document\
   **And** the new uploaded document is stored and the document metadata is generated (documentId, hash, hashAlgorithm) and linked to the contract clause (overwriting the previously linked metadata)\
   **And** I can navigate to the next step in the wizard

**AC6 - A Provider uploads a document when creating a new resource description via UI, but decides to remove it before submission**

6. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have chosen to add a new contract clause in the UI wizard,\
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
| **Parent** | [SIMPL-16251](../README.md) (Epic) A Provider adds contract clauses to a new Resource description via UI |
