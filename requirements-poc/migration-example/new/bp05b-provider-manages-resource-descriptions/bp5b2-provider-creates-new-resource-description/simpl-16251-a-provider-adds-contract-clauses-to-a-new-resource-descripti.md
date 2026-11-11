<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider adds contract clauses to a new Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-16251, status **In Progress**. It has 9 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** add contract clauses to a new resource description\
**So that** the terms and conditions for using and providing a resource are defined

**Acceptance criteria:**

**Scenario:** A Provider adds contract clauses to a new Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Contract clauses" in the wizard\
   **Then** I can add contract clauses attributes to the new resource description.

## User stories

1. [A Provider adds a licence agreement to a new resource description via UI](#user-story-1-a-provider-adds-a-licence-agreement-to-a-new-resource-description-via-ui) (DevTeam PBI is ready for Implementation check)
2. [A Provider adds a billing schema to a new resource description via UI](#user-story-2-a-provider-adds-a-billing-schema-to-a-new-resource-description-via-ui) (DevTeam PBI is ready for Implementation check)
3. [A Provider adds an SLA to a new resource description via UI](#user-story-3-a-provider-adds-an-sla-to-a-new-resource-description-via-ui) (DevTeam PBI is ready for Implementation check)
4. [Store uploaded Contract Clauses documents and generate hash metadata](#user-story-4-store-uploaded-contract-clauses-documents-and-generate-hash-metadata) (DevTeam PBI is ready for Implementation check)
5. [[API] - API to store Contract Clause document](#user-story-5-api---api-to-store-contract-clause-document) (DevTeam PBI is ready for Implementation check)
6. [[API] - API to retrieve Contract Clause document (xfsc-adv-search)](#user-story-6-api---api-to-retrieve-contract-clause-document-xfsc-adv-search) (DevTeam PBI is ready for Implementation check)
7. [A Provider adds a generic contract clause to a new resource description via UI](#user-story-7-a-provider-adds-a-generic-contract-clause-to-a-new-resource-description-via-ui) (DevTeam PBI is ready for Implementation check)
8. [[UI] Enable Contract Clauses Document Download in SDTooling UI](#user-story-8-ui-enable-contract-clauses-document-download-in-sdtooling-ui) (PBI INTERNAL QA)
9. [[UI] Enable Contract Clauses Document Download in CatalogueUI](#user-story-9-ui-enable-contract-clauses-document-download-in-catalogueui) (PBI INTERNAL QA)

### User story 1: A Provider adds a licence agreement to a new resource description via UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-26472)

**As a** Provider end user\
**I want to** to add the data of the contract clause License Agreement to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the schema of the contract clause 'License Agreement'

**Acceptance Criteria**

**AC1 - A Provider adds a contract clause of type 'Licence Agreement' to a new resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I am on the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause of type 'Licence Agreement' in the UI wizard,\
   **Then** the UI shows the properties of the contract clause of type 'Licence Agreement':

   - Title;
   - Source.

**AC2 - A Provider finishes adding a contract clause of type 'Licence Agreement' to a new resource description via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause of type 'Licence Agreement' in the UI wizard,\
   **When** I provide the values of the properties of the contract clause of type 'Licence Agreement':

   - Title: Text;
   - Source: Button to select and upload a document or provide a link to an existing licence (text).

   **Then** a contract clause of type 'Licence Agreement' is associated with the current resource description being created\
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

**AC4 - A Provider is only allowed to assign one licence to a resource description**

4. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have already added a contract clause of type 'Licence Agreement' in the UI wizard\
   **When** I add another Contract Clause\
   **Then** the UI does not show the option to select the type 'Licence Agreement' for this contract clause

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

[Back to the list of user stories](#user-stories)

### User story 2: A Provider adds a billing schema to a new resource description via UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-28815)

**As a** Provider end user\
**I want to** to add the data of the contract clause 'Billing Schema' to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the schema of the contract clause 'Billing Schema'

**Acceptance Criteria**

**AC1 - A Provider adds a contract clause of type 'Billing Schema' to a new resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause of type 'Billing Schema' in the UI wizard,\
   **Then** the UI shows the properties of the contract clause of type 'Billing Schema':

   - Title;
   - Billing model;
   - Source;
   - Billing unit;
   - Billing unit price;
   - Billing schedule.

**AC2 - A Provider finishes adding a contract clause of type 'Billing Schema' to a new resource description via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause of type 'Billing Schema' in the UI wizard,\
   **When** I provide the values of the properties of the contract clause of type 'Billing Schema':

   - Title (Text);
   - Billing model (Subscription, Pay-per-use, One-off);
   - Source (Upload a document);
   - Billing unit (Text);
   - Billing unit price (Decimal(10,2));
   - Billing schedule (Text).

   **Then** a contract clause of type 'Billing Schema' is associated with the current resource description being created\
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

**AC4 - A Provider is only allowed to assign one billing schema to a resource description**

4. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I am on the step 'Contract Clauses' in the wizard\
   **And** I have already added a contract clause of type 'Billing Schema' in the UI wizard\
   **When** I add another Contract Clause\
   **Then** the UI does not show the option to select the type 'Billing Schema' for this contract clause

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

[Back to the list of user stories](#user-stories)

### User story 3: A Provider adds an SLA to a new resource description via UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-28816)

**As a** Provider end user\
**I want to** to add the data of the contract clause 'SLA' to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the schema of the contract clause 'SLA'

**Acceptance Criteria**

**AC1 - A Provider adds a contract clause of type 'SLA' to a new resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause of type 'SLA' in the UI wizard,\
   **Then** the UI shows the properties of the contract clause of type 'SLA':

   - Title;
   - Source.

**AC2 - A Provider finishes adding a contract clause of type 'SLA' to a new resource description via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I reach the step 'Contract Clauses' in the wizard\
   **And** I choose to add a new contract clause of type 'SLA' in the UI wizard,\
   **When** I provide the values of the properties of the contract clause of type 'SLA':

   - Title (Text);
   - Source (Upload a document).

   **Then** I can proceed adding SLA entries to the SLA in the wizard.\
   **And** the uploaded document (if any) is stored and the document metadata is generated (documentId, hash, hashAlgorithm) and linked to the contract clause

**Note:* The upload of the document is covered in SIMPL-28846

**AC3 - A Provider adds an SLA entry to an SLA in a new resource description via UI**

3. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I have added a new contract clause of type 'SLA' in the UI wizard,\
   **Then** the UI shows the properties of the SLA entry:

   - SLA type;
   - SLA target;
   - Operator;
   - SLA unit.

   **Then** I can proceed adding SLA entries to the SLA in the wizard.

**AC4 - A Provider finishes adding an SLA entry to an SLA in a new resource description via UI**

4. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I have added a new contract clause of type 'SLA' in the UI wizard,\
   **When** I provide the values of the properties of the SLA entry:

   - SLA type (Text);
   - SLA target (Text);
   - Operator (Comparison operator);
   - SLA unit (Text).

   **Then** I can proceed adding another SLA entry to the SLA in the wizard or finish adding the SLA.

**AC5 - A Provider finishes adding an SLA in a new resource description via UI**

5. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **And** I have added a new contract clause of type 'SLA' in the UI wizard,\
   **And** I have added one or more SLA entries to the SLA,\
   **And** I choose to finish editing the SLA\
   **Then** a contract clause of type 'SLA' is associated with the current resource description being created\
   **And** I can navigate to the next step in the wizard

**AC6 - Syntactically invalid data metadata prevents submission of the resource description via UI**

6. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I enter a value in a data metadata field that does not comply with the field validation rules of the contract clause\
   **Then** the invalid field is visually highlighted\
   **And** an error message is displayed next to the field indicating that the value is not valid\
   **And** the Sign & Submit action is disabled\
   **And** I cannot proceed with the publication of the resource description until all invalid values are corrected

**AC7 - A Provider uploads a document when creating a new resource description via UI, but decides to replace it before submission**

7. **Given** I am authenticated as a Provider end user\
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

**AC8 - A Provider uploads a document when creating a new resource description via UI, but decides to remove it before submission**

8. **Given** I am authenticated as a Provider end user\
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

[Back to the list of user stories](#user-stories)

### User story 4: Store uploaded Contract Clauses documents and generate hash metadata

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-28846)

**Goal**

Implement the Provider-side backend support required to handle uploaded Contract Clause documents and return the generated document metadata / reference to the caller.

This story is limited to receiving the uploaded Contract Clause document, storing the physical document on the Provider side, calculating the document hash, generating technical metadata and returning the metadata / document reference after successful upload processing.

This story does not cover creating or updating the Resource Description. Inclusion of returned document metadata into the Resource Description is handled by the Resource Description / SD Tooling flow and related stories.

**User Story**

**As a** Provider,\
**I want to** upload a Contract Clause document through the agreed upload capability,\
**So that** the document can be stored on the Provider side and the generated metadata / reference can be returned for later use in the Resource Description flow.

**Architecture decision**

Uploaded Contract Clause documents are stored on the Provider side.

The physical document content is not embedded into the Resource Description.

This story only covers Provider-side upload, storage and metadata generation.

The story returns document metadata / reference to the caller. The caller may use the returned metadata in the Resource Description flow, but that Resource Description update is outside the scope of this story.

The Provider-side document storage must not be exposed as a directly accessible storage location for Consumer or GA / Catalogue as part of this story.

Any external retrieval of uploaded Contract Clause documents must use the agreed secure access mechanism, including Tier 2 / connector-related access where applicable. This story does not define the final external retrieval mechanism.

**Scope**

This story covers:

- receiving a Contract Clause document upload request,

- validating the uploaded document against the rules defined by the upload API implementation,

- storing the physical document on the Provider side,

- generating a unique document reference / documentId where available,

- calculating the document hash from the exact uploaded document content,

- identifying the hash algorithm,

- determining or validating documentName and mediaType where available,

- returning generated metadata / document reference to the caller,

- handling upload, storage, validation, hash calculation and metadata generation failures.

Expected metadata returned by the upload capability may include:

- documentId where available,

- documentName where available,

- mediaType where available,

- hash,

- hashAlgorithm,

- document reference / source reference where available.

The hash algorithm must be SHA-256 unless the upload API definition specifies a different agreed algorithm.

**Functional rules**

The uploaded document and uploaded file refer to the same physical artefact in this story.

The physical document must be stored outside the Resource Description.

A successful upload must return metadata / reference only. The response must not return or embed the full physical document content.

If upload, storage, validation, hash calculation or metadata generation fails, the document must not be treated as successfully stored and no successful metadata response must be returned.

If the same document content is uploaded again, reuse of an existing stored document object may be allowed only if the exact content hash matches and reuse does not alter or overwrite any source/reference already used by a published Resource Description version. Otherwise, a new stored document object and a new document reference must be created.

A later upload must not silently overwrite a document already referenced by a published Resource Description version.

Supported document formats and limits

Supported MIME types, document formats and maximum file size must be defined by the upload API implementation / Swagger definition.

Unsupported file types or files exceeding configured limits must be rejected with a clear validation error.

**Security note**

Provider-side document storage must not be exposed as a directly accessible storage location for Consumer or GA / Catalogue by this story.

This story only returns metadata / document reference to the caller after upload processing.

Any external retrieval of uploaded Contract Clause documents must follow the agreed secure access mechanism, including Tier 2 / connector-related access where applicable.

Security assumptions for document retrieval must be confirmed by the storage / upload API owners and relevant architecture stakeholders.

**Acceptance Criteria**

Acceptance Criteria

AC1 Uploaded document is validated

1. **Given** a Provider user uploads a Contract Clause document\
   **When** the upload request is submitted\
   **Then** Simpl-Open validates the document against the supported document format, MIME type and file size rules defined by the upload API implementation.

AC2 Valid document is stored and metadata is returned

2. **Given** a Provider user uploads a Contract Clause document\
   **And** the uploaded document is successfully validated\
   **When** Simpl-Open processes the uploaded document\
   **Then** Simpl-Open calculates the hash from the exact uploaded document content using the configured hash algorithm\
   **And** Simpl-Open generates document metadata including hash, hashAlgorithm, document reference / source reference and documentId, documentName, mediaType where available\
   **And** Simpl-Open stores the document and the generated metadata on the Provider side\
   **And** Simpl-Open returns the generated metadata, including the hash and hashAlgorithm, to the Provider user.

AC3 Invalid document is rejected

3. **Given** a Provider user uploads a Contract Clause document\
   **When** the document is not successfully validated against the supported document format, MIME type or file size rules defined by the upload API implementation\
   **Then** Simpl-Open returns a clear validation error to the Provider user\
   **And** Simpl-Open does not store the document.

AC4 Metadata generation failure is handled

4. **Given** a Provider user uploads a Contract Clause document\
   **And** the uploaded document is successfully validated\
   **When** metadata generation fails\
   **Then** Simpl-Open returns a clear error message to the Provider user\
   **And** Simpl-Open does not treat the upload as successful\
   **And** Simpl-Open does not return successful document metadata to the Provider user.

AC5 Storage failure is handled

5. **Given** a Provider user uploads a Contract Clause document\
   **And** the uploaded document is successfully validated\
   **And** the required metadata is successfully generated\
   **When** storing the document with its metadata fails\
   **Then** Simpl-Open returns a clear error message to the Provider user\
   **And** Simpl-Open does not treat the upload as successful\
   **And** Simpl-Open does not return successful document metadata to the Provider user.

AC6 Re-uploaded document does not overwrite an existing stored document

6. **Given** a Contract Clause document has already been uploaded and stored\
   **When** the Provider user uploads another Contract Clause document\
   **Then** Simpl-Open processes the upload as a separate upload request\
   **And** Simpl-Open must not silently overwrite an already stored document object\
   **And** the returned metadata must refer to the document content processed in the current upload request.

**Dependencies**

This story depends on the upload API / Swagger definition owned by the responsible implementation team.

The upload API definition must clarify:

- endpoint path and method,

- request format,

- response format,

- documentId / document reference format,

- hash format,

- supported MIME types,

- maximum file size,

- validation errors,

- technical error responses,

- security assumptions for document access and retrieval.

[Back to the list of user stories](#user-stories)

### User story 5: [API] - API to store Contract Clause document

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-29022)

**As a** Provider\
**I want to** store a contract clause document\
**So that** I can publish a resource description that informs potential Consumers about the contractual terms and obligations for the resource.

**Acceptance Criteria**

**AC1: A Provider stores a contract clause document - positive case**

1. **Given** I am authenticated as a Provider, with a role authorising contract clauses management\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **And** the body of the request contains\
   the documentName (string)\
   the sourceDocument\
   **Then** I receive a 201 Created response\
   **And** the contract clause document is stored and document hash is generated\
   **And** the body of the response contains\
   the documentId (UUID)\
   the documentHash (string)\
   the hashAlgorithm (string)\
   the documentName (string)

**AC2: A Provider stores a contract clause document - error case (authenticated)**

2. **Given** I am authenticated as a Provider, with a role authorising contract clauses management\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **And** the request cannot be properly fulfilled\
   **Then** an appropriate error response is returned (e.g. 400, 406, 415, 500)

**AC3: A Provider stores a contract clause document - unauthenticated case**

3. **Given** I am not authenticated\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **Then** the request is rejected with a 401 Unauthorized error

**AC4: A Provider stores a contract clause document - error case (authenticated with unauthorised role)**

4. **Given** I am authenticated as a Provider, without a role authorising contract clauses management\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **Then** the request is rejected with a 403 Forbidden

[Back to the list of user stories](#user-stories)

### User story 6: [API] - API to retrieve Contract Clause document (xfsc-adv-search)

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-29049)

**As a** Provider or Consumer\
**I want to** retrieve a contract clause document\
**So that** I can investigate the details of the contract clause when inspecting a resource description or usage contract

**Acceptance Criteria**

**AC1: A Provider or Consumer retrieves a contract clause document - positive case**

1. **Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
   **And** a contract clause document has been created in the past\
   **When** I request to retrieve a contract clause document for a specific documentId\
   **And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
   **Then** I receive a 200 Ok response\
   **And** the body of the response contains the requested document

**AC2: A Provider or Consumer retrieves a contract clause document - error case (documentId not found)**

2. **Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
   **When** I request to retrieve a contract clause document for a specific documentId\
   **And** the documentId does not exist\
   **And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
   **Then** the request is rejected with a 404 Not Found

**AC3:** **A Provider or Consumer** **retrieves a contract clause document - error case (authenticated)**

3. **Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
   **When** I request to retrieve a contract clause document for a specific documentId\
   **And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
   **And** the request cannot be properly fulfilled\
   **Then** an appropriate error response is returned (e.g. 400, 406, 415, 500)

**AC4:** **A Provider or Consumer** **retrieves a contract clause document - unauthenticated case**

4. **Given** I am not authenticated\
   **When** I request to retrieve a contract clause document for a specific documentId\
   **And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
   **Then** the request is rejected with a 401 Unauthorized error

**AC5:** **A Provider or Consumer** **retrieves a contract clause document - error case (authenticated with unauthorised role)**

5. **Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
   **When** I request to retrieve a contract clause document for a specific documentId\
   **And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
   **Then** the request is rejected with a 403 Forbidden

[Back to the list of user stories](#user-stories)

### User story 7: A Provider adds a generic contract clause to a new resource description via UI

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-29384)

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

[Back to the list of user stories](#user-stories)

### User story 8: [UI] Enable Contract Clauses Document Download in SDTooling UI

**Status:** PBI INTERNAL QA (backlog item SIMPL-33065)

**As a** Provider,\
**I want to** download the documents associated with Contract Clauses categories (SLA, License, Billing, and Generic) for the resources published on the Catalogue,\
**So that** I can verify and review the related contractual documentation.

**Technical Description**\
This task aims to enhance the Contract Clauses section of the SDTooling UI by providing access to documents uploaded by the provider. For each Contract Clauses category (SLA, License, Billing, and Generic), SD-Tooling UI shall verify whether one or more supporting documents are available. Whenever at least one document exists for a given category, the application shall display a **Download** button associated with that document. Upon user interaction, SD-Tooling UI shall invoke the API exposed by **xfsc-adv-search component** implemented under **ticket** SIMPL-29049 to retrieve the selected document and download it to the user's device.

**Acceptance Criteria**

###### AC1 - Display Download Action

1. **Given** the Provider is viewing the Contract Clauses section of a published resource\
   **And** one or more documents are available for a Contract Clauses category (SLA, License, Billing, or Generic)\
   **When** the section is displayed\
   **Then** a Download button shall be displayed for each available document\
   **And** the document name shall be visible to the user.

###### AC2 - Successful Download

2. **Given** a Download button is displayed for a document\
   **When** the Provider clicks the Download button\
   **And** the document is successfully retrieved by the backend service\
   **Then** the document shall be downloaded to the user's device.

###### AC3 - Open Public URL in Browser Tab

3. **Given** a Public URL is configured for a Contract Clauses category (SLA, License, Billing, or Generic)\
   **When** the user clicks the corresponding link or action associated with the Public URL\
   **Then** SD-Tooling UI shall open the Public URL in a new browser tab\
   **And** the user shall be redirected to the configured public resource.

###### AC4 - No Documents Available

4. **Given** the Provider is viewing a Contract Clauses category for which no documents are available\
   **When** the Contract Clauses section is displayed\
   **Then** no Download button shall be displayed for that category.

###### AC5 - Download Failure Handling

5. **Given** a Download button is displayed for a document\
   **When** the Provider clicks the Download button\
   **And** the document cannot be retrieved due to an API or backend error\
   **Then** the download shall not start\
   **And** the user shall be notified with an appropriate error message displayed within the dedicated SD-Tooling error notification area located at the top of the page containing following message "A technical issue has occurred, and it is not possible to access the file content"

.

[Back to the list of user stories](#user-stories)

### User story 9: [UI] Enable Contract Clauses Document Download in CatalogueUI

**Status:** PBI INTERNAL QA (backlog item SIMPL-33084)

**As a** Provider or Consumer,\
**I want to** download the documents associated with Contract Clauses categories (SLA, License, Billing, and Generic) for the resources published on the Catalogue,\
**So that** I can verify and review the related contractual documentation.

**Technical Description**\
This task aims to enhance the Contract Clauses section of the Catalogue UI by providing access to documents uploaded by the provider. For each Contract Clauses category (SLA, License, Billing, and Generic), Catalogue UI shall verify whether one or more supporting documents are available. Whenever at least one document exists for a given category, the application shall display a **Download** button associated with that document. Upon user interaction, SD-Tooling UI shall invoke the API exposed by **xfsc-adv-search component** implemented under **ticket** SIMPL-29049 to retrieve the selected document and download it to the user's device.

**Acceptance Criteria**

###### AC1 - Display Download Action

1. **Given** the Provider or Consumer is viewing the Contract Clauses section of a published resource\
   **And** one or more documents are available for a Contract Clauses category (SLA, License, Billing, or Generic)\
   **When** the section is displayed\
   **Then** a Download button shall be displayed for each available document\
   **And** the document name shall be visible to the user.

###### AC2 - Successful Download

2. **Given** a Download button is displayed for a document\
   **When** the Provider or Consumer clicks the Download button\
   **And** the document is successfully retrieved by the backend service\
   **Then** the document shall be downloaded to the user's device.

###### AC3 - No Documents Available

3. **Given** the Provider or Consumer is viewing a Contract Clauses category for which no documents are available\
   **When** the Contract Clauses section is displayed\
   **Then** no Download button shall be displayed for that category.

###### AC4 - Download Failure Handling

4. **Given** a Download button is displayed for a document\
   **When** the Provider or Consumer clicks the Download button\
   **And** the document cannot be retrieved due to an API or backend error\
   **Then** the download shall not start\
   **And** the user shall be notified with an appropriate error message indicating that the document could not be downloaded.

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
