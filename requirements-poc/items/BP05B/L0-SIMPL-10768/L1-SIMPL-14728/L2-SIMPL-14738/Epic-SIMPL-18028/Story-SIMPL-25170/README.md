> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14738](../../README.md) / [Epic-SIMPL-18028](../README.md) / **Story-SIMPL-25170**

# SIMPL-25170: [UI] Retrieve resourceAddress info for updating/visualizing resource description

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14738](../../README.md) (L2) › [SIMPL-18028](../README.md) (Epic) › **SIMPL-25170** (User story)

## Text

#### Storycard

| As a|Participant Provider |
| --- |

| I want|to view the resourceAddress information associated with an existing resourceDescription when accessing its details (in read‑only or editing mode) |
| --- |

| so that|I can verify the previously configured values and, optionally update them before submitting a modified resourceDescription. |
| --- |

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

**Given** a provider opens an existing resource description\
**When** the system displays the resource details (in read‑only or edit mode)

**Then** the provider can see all the previously entered resourceAddress information\
**And** the information is presented in a clear and readable way

###### **AC2 - The information is displayed according to the structure of the original template**

**Given** a resource description was created using a specific template\
**When** the provider views it in edit mode\
**Then** the system shows only the fields defined by that template\
**And** the information appears embedded in the correct template‑specific layout and field structure

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18028](../README.md) (Epic) A Provider updates common metadata of a new version of a  Resource description via UI |
