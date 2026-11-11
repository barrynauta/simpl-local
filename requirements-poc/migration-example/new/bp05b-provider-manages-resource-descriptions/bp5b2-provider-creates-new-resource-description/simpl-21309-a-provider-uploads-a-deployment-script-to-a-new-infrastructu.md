<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider uploads a deployment script to a new infrastructure Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.02 Add resource type specific metadata](../README.md#bp05b02-add-resource-type-specific-metadata).

In the backlog (Jira) it is item SIMPL-21309. It has 2 user stories, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** add a deployment script to a new infrastructure resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider adds a deployment script to a new infrastructure resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new infrastructure resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** I can add a deployment script to the new resource description.

## User stories

1. [[UI] Select Resource Type "Infrastructure" and Open Deployment Script Upload](#user-story-1-ui-select-resource-type-infrastructure-and-open-deployment-script-upload) · Developed
2. [[UI] Upload Deployment Script in Popup Window](#user-story-2-ui-upload-deployment-script-in-popup-window) · Developed

### User story 1: [UI] Select Resource Type "Infrastructure" and Open Deployment Script Upload

**Development:** Developed · backlog item SIMPL-10289

**As a** provider creating a resource description for an infrastructure resource,\
**I want to** be able to upload a deployment script and related information while filling out the infrastructure resource description form,\
**So that** I have a seamless experience creating the infrastructure resource description along with its deployment script.\
----

###### **Steps**

1. **Infrastructure Resource Type Selection:**\
   Provider selects an infrastructure resource type from a dropdown or predefined list. *(Note: This step is part of the overall flow but not in scope for current acceptance criteria)*
1. **Offering Details Form:**\
   After selecting the resource type, a form appears where the provider inputs details about the infrastructure offering. The form includes multiple fields related to the offering.
1. **Deployment Script ID Field and Button:**\
   The **"Deployment script id"** field is part of the infrastructure offering form and is disabled for manual input. The field may require scrolling to be visible depending on screen size or form length.\
   The **"Add deployment script +"** button is visible to the right of this field.
1. **Button Visibility and Access:**\
   The **"Add deployment script +"** button is clearly labeled and clickable.
1. **Popup Behavior on Click:**\
   Clicking the **"Add deployment script +"** button opens a popup window (modal) over the current form.\
   The resource description form becomes non-interactive while the popup is active.\
   The popup contains fields and behaviors as described in [SIMPL-13605 - [UI] Upload Deployment Script in Popup Window].
1. **Post-Upload Behavior:**\
   After successful upload and closing the popup, the provider is returned to the infrastructure offering form.\
   *(Post-upload visual indicator and auto-fill of deployment script ID are covered by SIMPL-13605.)*

----

###### **Acceptance Criteria**

**Scenario 1: Deployment Script ID Field and Button**

1. **Given** I am filling in the form for an infrastructure resource description,\
   **When** I view the deployment script id field,\
   **Then** the field is disabled for manual input,\
   **And** the "Add deployment script +" button is visible and clickable.

**Scenario 2: Open Deployment Script Upload Popup**

2. **Given** I click the "Add deployment script +" button,\
   **When** the popup opens,\
   **Then** the resource description form becomes non-interactive,\
   **And** the popup displays upload fields and behaviors as described in SIMPL-13605.\
   ----

###### Technical description:

The Deployment Script Mgmt. Component also has an API to upload the Deployment Script, so the idea is to Use the UI of the SD Tooling and upload the Deployment Script via the API of the Deployment Script Mgmt. Component and then retrieve the ID automatically and put it into the Resource Description for seamless user experience.

API: [https://code.europa.eu/simpl/simpl-open/development/infrastructure/infrastructure-be/-/blob/develop/openapi/script-management-api.yaml](https://code.europa.eu/simpl/simpl-open/development/infrastructure/infrastructure-be/-/blob/develop/openapi/script-management-api.yaml)

About the Hardcoded Provider List - we don't know which provider's will be supported, so would initially hardcode a list of default values, that later on can be replaced by an API where we can retrieve all supported providers from.

**Retrieval of the Deployment Script ID:**

(image not shown)\
----\
**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-12918&t=x8NzI6SDyYwfsXSm-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2378-12918&t=x8NzI6SDyYwfsXSm-0)

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2408-29830&t=A3ZwsdGxI2cRrcTG-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2408-29830&t=A3ZwsdGxI2cRrcTG-0)

### User story 2: [UI] Upload Deployment Script in Popup Window

**Development:** Developed · backlog item SIMPL-13605

**As a** Provider creating a resource description for an infrastructure resource,\
**I want to** fill out the deployment script popup with Title, Description, Cloud -Provider- Environment, and upload a deployment script,\
**So that** I can associate a valid deployment script with my infrastructure resource description.\
----

###### Steps

1. **Provider opens the infrastructure resource description form** and clicks "Add deployment script +".
1. A **popup window** opens displaying a form with the following mandatory fields: **Title** (text), **Description** (text), **Cloud Environment** (dropdown), and **Add script file** (file upload widget, only one file allowed).
1. The **file upload widget** supports drag-and-drop and upload from device. Once the file is uploaded the URL link to the script file is displayed.
1. After successful upload, the **uploaded file name is displayed** and a **bin (remove) button** appears.
1. All fields must be filled. When the provider clicks Submit, the **UI validates** the presence of all required fields and **shows inline errors** if any are missing or invalid.
1. The Deployment Script Management Component, owned by the Infrastructure Team, performs backend validation of the deployment script and returns either a **deployment script ID** (on success) or **validation errors** (on failure).
1. If validation is successful, the popup closes, the **deployment script ID is auto-filled** in the main infrastructure resource form, and a **visual indicator** (e.g., filename badge) confirms the upload.
1. If the script is invalid, the **popup remains open** and displays **specific error messages from the backend** for correction.
1. Clicking **Cancel** closes the popup **without saving or submitting** the deployment script.
1. While the popup is open, the **main resource description form is non-interactive** (modal behavior).

----

###### Acceptance Criteria

**Scenario 1: Display Upload Form in Popup**

1. **Given** the provider clicks "Add deployment script +"\
   **When** the upload popup opens\
   **Then** the form fields Title, Description, Cloud Environment, and File Upload are shown and mandatory

**Scenario 2: File Upload Widget UI**

2. **Given** the form to upload a deployment script for a resource description is visible\
   **When** the provider drags/drops or uploads a file from its device\
   **Then** the file name is displayed\
   **And** a remove (bin) button appears next to it

**Scenario 3: Validate Form Before Submit**

3. **Given** the provider fills out the form and does not fill all fields or provides incorrect values\
   **When** the provider clicks Submit\
   **Then** inline errors are shown for missing or invalid fields\
   **And** the popup remains open for correction

**Scenario 4: Submit Deployment Script Successfully**

4. **Given** the provider has provided correct values for all fields and uploaded a valid deployment script\
   **When** the provider submits\
   **Then** the popup closes\
   **And** the main form shows a visual indicator with the uploaded script info and deployment script id

**Scenario 5: Handle Submission Errors**

5. **Given** the provider has provided correct values for all fields but uploaded an invalid deployment script\
   **When** the provider submits\
   **Then** error messages related to deployment script validation are shown in the popup\
   **And** the popup remains open for correction

**Scenario 6: Cancel Upload**

6. **Given** the popup is open\
   **When** the provider clicks Cancel\
   **Then** the popup closes without saving or submitting\
   ----

Technical description:

The Deployment Script Mgmt. Component also has an API to upload the Deployment Script, so the idea is to Use the UI of the SD Tooling and upload the Deployment Script via the API of the Deployment Script Mgmt. Component and then retrieve the ID automatically and put it into the Resource Description for seamless user experience.

API: [https://code.europa.eu/simpl/simpl-open/development/infrastructure/infrastructure-be/-/blob/develop/openapi/script-management-api.yaml](https://code.europa.eu/simpl/simpl-open/development/infrastructure/infrastructure-be/-/blob/develop/openapi/script-management-api.yaml)

About the Hardcoded Provider List - we don't know which provider's will be supported, so would initially hardcode a list of default values, that later on can be replaced by an API where we can retrieve all supported providers from.

**Retrieval of the Deployment Script ID:**

(image not shown)\
----\
**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2408-29308&t=wKHDnMbpPZCRNLNc-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2408-29308&t=wKHDnMbpPZCRNLNc-0)

---

Back to [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
