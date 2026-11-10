> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14732](../../README.md) / [Epic-SIMPL-21309](../README.md) / **Story-SIMPL-10289**

# SIMPL-10289: [UI] Select Resource Type "Infrastructure" and Open Deployment Script Upload

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14732](../../README.md) (L2) › [SIMPL-21309](../README.md) (Epic) › **SIMPL-10289** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-21309](../README.md) (Epic) A Provider uploads a deployment script to a new infrastructure Resource description via UI |
