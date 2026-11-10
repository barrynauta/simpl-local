> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14732](../../README.md) / [Epic-SIMPL-21309](../README.md) / **Story-SIMPL-13605**

# SIMPL-13605: [UI] Upload Deployment Script in Popup Window

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14732](../../README.md) (L2) › [SIMPL-21309](../README.md) (Epic) › **SIMPL-13605** (User story)

## Text

**As a** Provider creating a resource description for an infrastructure resource,\
**I want** to fill out the deployment script popup with Title, Description, Cloud -Provider- Environment, and upload a deployment script,\
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

**Scenario 1: Display Upload Form in Popup**\
Given the provider clicks "Add deployment script +"\
When the upload popup opens\
Then the form fields Title, Description, Cloud Environment, and File Upload are shown and mandatory

**Scenario 2: File Upload Widget UI**\
Given the form to upload a deployment script for a resource description is visible\
When the provider drags/drops or uploads a file from its device\
Then the file name is displayed\
And a remove (bin) button appears next to it

**Scenario 3: Validate Form Before Submit**\
Given the provider fills out the form and does not fill all fields or provides incorrect values\
When the provider clicks Submit\
Then inline errors are shown for missing or invalid fields\
And the popup remains open for correction

**Scenario 4: Submit Deployment Script Successfully**\
Given the provider has provided correct values for all fields and uploaded a valid deployment script\
When the provider submits\
Then the popup closes\
And the main form shows a visual indicator with the uploaded script info and deployment script id

**Scenario 5: Handle Submission Errors**\
Given the provider has provided correct values for all fields but uploaded an invalid deployment script\
When the provider submits\
Then error messages related to deployment script validation are shown in the popup\
And the popup remains open for correction

**Scenario 6: Cancel Upload**\
Given the popup is open\
When the provider clicks Cancel\
Then the popup closes without saving or submitting\
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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-21309](../README.md) (Epic) A Provider uploads a deployment script to a new infrastructure Resource description via UI |
