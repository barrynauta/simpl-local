<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the contract clauses, resulting in a new version of the resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.10 Update policies and contract related metadata](../README.md#bp05b10-update-policies-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-16252. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

**As a** Provider\
**I want to** update contract clauses resulting in a new version of the resource description\
**So that** the terms and conditions for using and providing a resource are defined

**Acceptance criteria:**

**Scenario:** A Provider updates the contract clauses, resulting in a new version of the resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Contract clauses" in the wizard\
   **Then** the contract clauses attributes are prefilled with the values from the previous version\
   **And** I can update the contents of the attributes.

## User stories

1. [A Provider updates the contract clauses, resulting in a new version of the resource description via UI](#user-story-1-a-provider-updates-the-contract-clauses-resulting-in-a-new-version-of-the-resource-description-via-ui) · Developed

### User story 1: A Provider updates the contract clauses, resulting in a new version of the resource description via UI

**Development:** Developed · backlog item SIMPL-26274

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

---

Back to [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
