<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates common metadata of a new version of a Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.08 Create a new version of a resource description](../README.md#bp05b08-create-a-new-version-of-a-resource-description).

In the backlog (Jira) it is item SIMPL-18028. It has 5 user stories, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** update the common metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider updates common metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Common metadata" in the wizard\
   **Then** the common metadata are prefilled with the values from the previous version\
   **And** I can update common metadata.

## User stories

1. [[UI] Provider updates common metadata to a new version of Resource description](#user-story-1-ui-provider-updates-common-metadata-to-a-new-version-of-resource-description) · Developed
2. [[API] API for retrieving a resourceAddress info (EdcConnectorAdapter)](#user-story-2-api-api-for-retrieving-a-resourceaddress-info-edcconnectoradapter) · Developed
3. [[API] API for retrieving a resourceAddress info (sd-tooling-be)](#user-story-3-api-api-for-retrieving-a-resourceaddress-info-sd-tooling-be) · Developed
4. [[UI] Retrieve resourceAddress info for updating/visualizing resource description](#user-story-4-ui-retrieve-resourceaddress-info-for-updatingvisualizing-resource-description) · Developed
5. [[API] Implement API to atomically enrich and sign a resourceDescription (SD-Tooling))](#user-story-5-api-implement-api-to-atomically-enrich-and-sign-a-resourcedescription-sd-tooling) · Developed

### User story 1: [UI] Provider updates common metadata to a new version of Resource description

**Development:** Developed · backlog item SIMPL-18683

#### Storycard

**As a** Provider end user\
**I want to** update the common metadata of a new version of a resource description via UI\
**So that** the contents of the resource description is complete.

#### Acceptance criteria

**Scenario:** A Provider updates common metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Common metadata" in the wizard\
   **Then** the common metadata attributes are prefilled with the values from the previous version\
   **And** I can update the contents of the attributes.

#### Technical details

|   |
| --- |
| SD UI calls SD BE to retrieve SD schema from GA for dedicated RD type |
| Result: UI parsed SD schema and rendered common metadata fields to the wizard screen |
| SD UI calls SD BE to retrieve RD data from the last RD version |
| Result: common metadata fields are pre-filled on the wizard screen and available for edit |
| By clicking on the "Next step" button, the another screen rendereing should be triggered |

#### Mockups:

| Mockup |
| --- |
| !image-2025-09-13-20-50-55-010.png | thumbnail! |

#### Additional notes

| Figma: [https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-12978&t=o3dEx1c6tEbK4PpV-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-12978&t=o3dEx1c6tEbK4PpV-0) |
| (internal page) |
| (internal page) |
| (internal page) |
| (internal page) |

[Back to the list of user stories](#user-stories)

### User story 2: [API] API for retrieving a resourceAddress info (EdcConnectorAdapter)

**Development:** Developed · backlog item SIMPL-25147

#### Storycard

**As a** Participant Provider\
**I want to** retrieve (via sd-tooling-be) the *resourceAddress* associated with a resourceDescription, as previously stored in the connector during the assetRegistration stage\
**So that** I can correctly display the resource description based on the specific schema with which it was created.

#### Technical details

This ticket aims to implement a new API directly in the EdcConnectorAdapter, which communicates with the underlying EDC Connector to retrieve the *resourceAddress* associated with a specific resourceDescription.

When a consumer (e.g., SD‑ToolingBE or another client) requests to visualize or update a resourceDescription, the EdcConnectorAdapter shall:

- retrieve the resourceAddress stored during the initial assetRegistration or during any subsequent update by querying the EDC Connector’s API,
- return this data in JSON format to the requester.

The newly implemented EdcConnectorAdapter API must therefore:

1. **Expose a REST endpoint** to external components (e.g., SD‑ToolingBE).
1. **Query the EDC Connector** to obtain the resourceAddress for the requested resourceDescription.
1. Return the retrieved structure as raw JSON, preserving its format for correct downstream rendering and editing.\
   Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | resourceAddress successfully retrieved |
| **400 Bad Request** | Invalid resource identifier |
| **404 Not Found** | Requested resourceAddress not found for the given resourceDescription |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | Underlying EDC Connector unreachable, failed request upstream |
| **500 Internal Server Error** | Unexpected adapter internal error |

###### Acceptance Criteria

###### Successful retrieval of resourceAddress

1. **Given** a valid `resourceDescription` identifier (`{}assetID{`}) is provided in a request to the API\
   **When** the API processes the request to retrieve the resource address associated with that identifier\
   **Then** the API returns the corresponding `resourceAddress` in JSON format

###### resourceAddress not available

2. **Given** a request is sent to the API to retrieve the resource address for a valid `resourceDescription` identifier (`{}assetID{`})\
   **When** the API is unable to obtain the corresponding `resourceAddress` due to it being unavailable, missing, or due to an error occurring during retrieval\
   **Then** the API returns a response indicating that the requested `resourceAddress` is not available

[Back to the list of user stories](#user-stories)

### User story 3: [API] API for retrieving a resourceAddress info (sd-tooling-be)

**Development:** Developed · backlog item SIMPL-25148

#### Storycard

**As a** Participant Provider\
**I want to** retrieve (via SD‑Tooling frontend) the *resourceAddress* associated with a resourceDescription, as previously stored in the connector during assetRegistration stage\
**So that** I can correctly visualize and modify the resourceDescription using the technical information originally registered in the connector.

#### Technical details

This ticket aims to define and expose a new API in SD‑ToolingBE, which acts as a façade toward the underlying EdcConnectorAdapter, enabling the SD‑Tooling UI to retrieve the *resourceAddress* associated with a specific resourceDescription.

When the UI requests to visualize or update a resourceDescription, SD‑ToolingBE invokes the corresponding EdcConnectorAdapter API to retrieve the resourceAddress stored during the initial assetRegistration or during a later update.\
The retrieved resourceAddress is then returned to the SD‑Tooling UI to:

- correctly render the resourceDescription fields,
- allow the user to update them while preserving consistency with the existing configuration.

The SD‑ToolingBE API must therefore:

1. Expose a REST endpoint retrievable by SD‑Tooling UI.
1. Internally call the EdcConnectorAdapter API to obtain the resourceAddress.
1. Return the raw JSON structure to the UI in a stable format suitable for rendering and editing.\
   Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | resourceAddress successfully retrieved |
| **400 Bad Request** | Invalid resource identifier |
| **404 Not Found** | Requested resourceAddress not found for the given resourceDescription |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **502 Bad Gateway** | EdcConnectorAdapter unreachable or returned an upstream error |
| **500 Internal Server Error** | Unexpected adapter internal error |

###### 

###### **Acceptance Criteria**

###### **Successful retrieval of resourceAddress**

1. **Given** a valid `resourceDescription` identifier (`{}assetID{`}) previously registered by the Provider\
   **When** a request is sent to the API to retrieve the resource address associated with that `resourceDescription` identifier\
   **Then** the API returns the corresponding `resourceAddress` in JSON format

###### **resourceAddress not available**

2. **Given** a request is sent to the API to retrieve the resource address associated with a valid `resourceDescription` identifier (`{}assetID{`})\
   **When** the API is unable to obtain the corresponding `resourceAddress` due to it being absent or due to an error occurring in the retrieval process\
   **Then** the API returns a response indicating that the requested `resourceAddress` is not available

[Back to the list of user stories](#user-stories)

### User story 4: [UI] Retrieve resourceAddress info for updating/visualizing resource description

**Development:** Developed · backlog item SIMPL-25170

#### Storycard

**As a** Participant Provider\
**I want to** view the resourceAddress information associated with an existing resourceDescription when accessing its details (in read‑only or editing mode)\
**So that** I can verify the previously configured values and, optionally update them before submitting a modified resourceDescription.

#### Technical details

When a provider opens an existing resourceDescription, either in read‑only mode or for editing, the SD‑Tooling‑UI must be able to reconstruct the full structure of the resourceAddress based on the template originally used at creation time.\
To achieve this, the frontend must retrieve information both from **SD‑Tooling‑BE** and, when necessary, from the Asset Orchestrator, following a precise sequence of API calls.

The logic must support all three types of templates:

1. Templates with **simple string fields**
1. Templates of type **ProcessingWorkflow**
1. Templates containing **deployment script identifiers** (infrastructure case)

##### **Step 1. Retrieve templateId from the connector**

When loading a resourceDescription (for viewing or editing), the SD‑Tooling‑UI receives from the connector the `templateId` originally associated with the asset.\
This identifier indicates which `resourceAddressTemplate` is required to correctly interpret and render the resourceAddress content.\
This is the foundation for reconstructing the UI form.

##### **Step 2. Retrieve template definition via SD‑Tooling resourceAddressManagement APIs**

Before performing any workflow lookup (when applicable), the frontend must call the SD‑Tooling‑BE **resourceAddressManagement** APIs to retrieve the JSON Schema describing the selected template:

- `GET /resourceAddressManagement/templates`
- `{}GET /resourceAddressManagement/templates/{templateId{`}}

These APIs return:

- The JSON schema for sourceAddress and destinationAddress
- The expected fields (string fields, workflow fields, scriptID fields, etc.)
- Validation rules
- Template type information (e.g., `{}ProcessingWorkflow{`}, `{}ManualAccess{`}, `{}AppDeployer{`}, etc.)

The frontend must use this schema to dynamically construct the form structure, either in read‑only or editable mode.

##### **Step3. UI rendering based on JSON Schema**

Using the returned JSON Schema:

- The UI builds a dynamic UI form,
- It populates each field using values retrieved from the stored `{}resourceAddress{`},\
   *\
   Depending on the access mode, the interface renders fields as: * Read‑only values, or
- **Editable inputs** for user modification.

For **simple templates**\
(e.g., MinIOS3, Provider Hosted Manual Access):

→ No additional lookups or processing are needed.\
→ The UI simply displays the string fields (read‑only or editable).

For **templates with deployment script identifiers**\
(e.g., Infrastructure templates):

→ The scriptID and related parameters are displayed to the user.

##### **Additional matching required for ProcessingWorkflow templates**

For templates of type ProcessingWorkflow, the frontend must perform an additional lookup to correctly rebuild workflow information.

After retrieving the JSON Schema the UI has to invoke AssetOrchestrator GET /workflows in order to retrieve structure of fields related to codeLocation, repositoryName, workflowID, workflowConfig

**Acceptance Criteria**

###### **AC1 - The provider can clearly view the resourceAddress information**

1. **Given** a provider opens an existing resource description\
   **When** the system displays the resource details (in read‑only or edit mode)\
   **Then** the provider can see all the previously entered resourceAddress information\
   **And** the information is presented in a clear and readable way

###### **AC2 - The information is displayed according to the structure of the original template**

2. **Given** a resource description was created using a specific template\
   **When** the provider views it in edit mode\
   **Then** the system shows only the fields defined by that template\
   **And** the information appears embedded in the correct template‑specific layout and field structure

[Back to the list of user stories](#user-stories)

### User story 5: [API] Implement API to atomically enrich and sign a resourceDescription (SD-Tooling))

**Development:** Developed · backlog item SIMPL-25713

#### Storycard

**As a** Participant Provider\
**I want to** trigger (through SD-Tooling) a single operation that automatically enriches, validates, and digitally signs a resourceDescription\
**So that** I can securely and efficiently manage update and versioning workflows, receiving a final version that is already validated, digitally signed, immutable, and ready for publication without the risk of unintended modifications.

#### Technical details

This ticket introduces a new capability in SD‑Tooling, enabling the execution of enrichment and digital signature of a resourceDescription in one atomic operation.

Through a dedicated API exposed by SD‑Tooling, the system must:

1. **Receive** from the UI a resourceDescription requiring update and/or versioning.
1. Perform the enrichment process internally, applying:
   - syntactical validation
   - mandatory enrichment (e.g. connector info, contract info)
   - versioning logic
1. **Apply the digital signature immediately** after successful enrichment.
1. **Return a single final outcome** consisting of a fully enriched, validated, digitally signed, immutable resourceDescription, ready for catalog publication.

The process must guarantee that no intermediate or partially processed states are exposed to the UI, ensuring integrity and consistency.

.\
Following list of response code:

| HTTP Code | Description (context-specific) |
| --- | --- |
| **200 OK** | The enriched and digitally signed resourceDescription is successfully returned |
| **400 Bad Request** | The provided resourceDescription is invalid or malformed |
| **406 Not Acceptable** | Unsupported response media type |
| **415 Unsupported Media Type** | Unsupported request media type |
| **500 Internal Server Error** | Unexpected error during processing |

###### Acceptance Criteria

###### **AC1 Successful enrichment and signature**

1. **Given** a valid `resourceDescription` requiring update or versioning, submitted by a Participant Provider through the SD‑Tooling UI\
   **When** SD‑Tooling processes the request via the enrichment and signature operation\
   **Then** SD‑Tooling returns a fully enriched, syntactically validated, digitally signed, immutable `resourceDescription` that is ready for catalog publication.

###### **AC2 Invalid or incomplete input**

2. **Given** a malformed or incomplete `resourceDescription`\
   **When** SD‑Tooling attempts to process the request\
   **Then** the API returns an appropriate error and no enrichment or digital signature is applied.

###### **AC3 Enrichment or signature failure**

3. **Given** a valid `resourceDescription` request\
   **When** an internal error occurs during enrichment, validation, versioning, or digital signing\
   **Then** SD‑Tooling returns the appropriate error (`{}500 Internal Server Error{`}) and no partial, unsigned, or intermediate `resourceDescription` is returned.

[Back to the list of user stories](#user-stories)

---

Back to [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
