<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider defines the resource source address for resource sharing

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.01 Create a new resource description](../README.md#bp05b01-create-a-new-resource-description).

In the backlog it is item SIMPL-8340, status **Defined**. It has 6 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider,\
**I want to** provide via API or UI, as part of a resource description, the Resource Source based on the resource address template that is valid for the resource sharing method, which I use for sharing my resources,\
**So that** the Resource Source is published with the resource description and provides information about the Resource Source required to support the automated resource sharing.\
----

###### Acceptance Criteria:

**1. Submission of Resource Address with Resource Description**\
*

1. **Given** I am a provider user with permission to manage resource descriptions\
   **When** I include a resource address based on the resource sharing method in the resource description and submit it\
   **Then** the resource address is validated, and feedback is provided indicating whether it is valid

**2. Submission with Invalid Resource Address**\
*

2. **Given** I am a provider user with permission to manage resource descriptions\
   **When** I include an invalid resource address in the resource description and submit it\
   **Then** the submission is rejected, and a validation error message is displayed

**3. Publication of Resource with Resource Address**\
*

3. **Given** a resource description with a valid resource address has been submitted\
   **When** the submission is accepted\
   **Then** the resource description, including the resource address, becomes available in the catalogue

**4. Accessing the Resource Address of a Published Resource**\
*

4. **Given** I have access to retrieve published resource descriptions\
   **When** I retrieve a specific published resource\
   **Then** I can view the resource address details included in that resource description

**5. Template Use Based on Resource Sharing Method**\
*

5. **Given** I am creating a resource description\
   **When** I select a resource sharing method\
   **Then** I should be able to retrieve the corresponding resource address template that is valid for that method

## User stories

1. [[UI] Viewing Resource Sharing Methods and Their Templates (Provider)](#user-story-1-ui-viewing-resource-sharing-methods-and-their-templates-provider) (Ready for DG Acceptance)
2. [[API] Extend the SD Tooling API for Retrieving Available Resource Sharing Methods and Templates (Provider)](#user-story-2-api-extend-the-sd-tooling-api-for-retrieving-available-resource-sharing-methods-and-templates-provider) (Ready for DG Acceptance)
3. [[API] Resource Source Address Validation Based on Resource Sharing Method and Provider Template](#user-story-3-api-resource-source-address-validation-based-on-resource-sharing-method-and-provider-template) (DevTeam PBI is ready for Implementation check)
4. [[UI] Selecting, Filling, and Submitting the Resource Source Template (Provider)](#user-story-4-ui-selecting-filling-and-submitting-the-resource-source-template-provider) (Ready for DG Acceptance)
5. [[API] Resource Source Address Validation Upon Provider Resource Submission](#user-story-5-api-resource-source-address-validation-upon-provider-resource-submission) (DevTeam PBI is ready for Implementation check)
6. [Create Resource Address Template for eDelivery](#user-story-6-create-resource-address-template-for-edelivery) (Ready for DG Acceptance)

### User story 1: [UI] Viewing Resource Sharing Methods and Their Templates (Provider)

**Status:** Ready for DG Acceptance (backlog item SIMPL-10301)

**As a** provider,\
**I want to** view a list of available resource sharing methods and their associated templates,\
**So that** I can understand the available options for sharing my resources effectively.

**Acceptance Criteria:**\
*

1. **Given** I am logged in as a provider and filling in the resource description form,\
   **When** I navigate to the "Resource Sharing Method" section,\
   **Then** I should see the list of available resource sharing methods and be able to select one

*

2. **Given** I am logged in as a provider and filling in the resource description form,\
   **When** I navigate to the "Resource Sharing Method" section\
   **And** I select a resource sharing method\
   **Then** I should see the list of associated resource source templates and be able to select one

**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-11448&m=dev](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-11448&m=dev)

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-11936&m=dev](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-11936&m=dev)

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&m=dev](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&m=dev)

**Internal page (including conceptual Data Model):**

(internal page)

[Back to the list of user stories](#user-stories)

### User story 2: [API] Extend the SD Tooling API for Retrieving Available Resource Sharing Methods and Templates (Provider)

**Status:** Ready for DG Acceptance (backlog item SIMPL-10562)

**As a** provider user authorised to manage resource descriptions,\
**I want to** retrieve via an API a list of available resource sharing methods and their associated templates,\
**So that** I can provide a source address compliant with the requested sharing method when completing a resource description.

**Acceptance Criteria:**

- **Positive case**

1. **Given** I am authorised to manage resource descriptions,\
   **When** I request available resource sharing methods via the API,\
   **Then** I receive a list of supported resource sharing methods, each including its associated template and metadata needed for the resource description.

   - Negative case - unauthorised user

2. **Given** I am not authorised to manage resource descriptions,\
   **When** I attempt to retrieve available resource sharing methods via the API,\
   **Then** I receive an appropriate error response (e.g., 403 Forbidden).

[Back to the list of user stories](#user-stories)

### User story 3: [API] Resource Source Address Validation Based on Resource Sharing Method and Provider Template

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-12291)

**As a** provider user creating a resource description,\
**I want** the resource source that is submitted as part of the resource description to be syntactically validated and validated against the resource source template of the previously selected resource sharing method\
**So that** I can ensure that the provided resource source address is syntactically correct and complies with the selected resource sharing method and the associated template.

###### **Acceptance Criteria**

- authorised user
   - valid resource source

1. **Given** I am authorised to manage resource descriptions,\
   **When** I submit a valid resource description including a resource source address, valid for the selected resource sharing method,\
   **Then** I will receive a positive validation message and I am able to publish the resource description to the dataspace catalogue.

   - syntactic invalid resource source

2. **Given** I am authorised to manage resource descriptions,\
   **When** I submit a resource description including a syntactically incorrect resource source address\
   **Then** the resource description must be rejected and I receive an error message.

   - resource source not compliant with the template of the selected resource sharing method

3. **Given** I am authorised to manage resource descriptions,\
   **When** I submit a resource description including a resource source address, not compliant with the resource address template for the resource sharing method selected\
   **Then** the resource description must be rejected and I receive an error message.

[Back to the list of user stories](#user-stories)

### User story 4: [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider)

**Status:** Ready for DG Acceptance (backlog item SIMPL-13565)

**As a** provider,\
**I want to** select a resource source template associated with a sharing method, fill in the required details, and submit it as part of the resource description,\
**So that** I can configure and provide the necessary source address and metadata for resource sharing.

**Acceptance Criteria:**

- **AC1 - Template Selection**

1. **Given** I have selected a resource sharing method,\
   **When** I view the associated resource source templates,\
   **Then** I should be able to select one template from the list.

   - AC2 - Display of Template Fields

2. **Given** a resource source template is selected,\
   **When** the UI loads,\
   **Then** I should see all fields defined by the selected template, including required and optional fields.

   - AC3 - Submission Action

3. **Given** the template fields are displayed,\
   **When** I fill in the form\
   **And** I click the "Submit" button,\
   **Then** the data should be submitted for validation.

**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&t=LT29Q6aB2kEPn6ew-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&t=LT29Q6aB2kEPn6ew-0)

https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2565-53401&t=Tg2TKqZO3dR3zidj-4

**Internal page (including conceptual Data Model):**

(internal page)

[Back to the list of user stories](#user-stories)

### User story 5: [API] Resource Source Address Validation Upon Provider Resource Submission

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-13635)

**As a** provider,\
**I want to** validate a resource source address submitted as part of the resource description,\
**So that** I can ensure the resource source address is valid before completing the submission process.

**Acceptance Criteria:**

- **AC1 - Successful Validation**

1. **Given** I am a provider filling in a resource source address based on a resource source template\
   **And** all fields are valid with respect to the template (correct types, formats, and values),\
   **When** I submit the resource source address\
   **Then** the submitted resource source address is successfully validated against resource source template,\
   **And** a success message is shown, and the resource source address is stored.

   - AC2 - Failed Validation

2. **Given** I am a provider filling in a resource source address based on a resource source template\
   **And** some provided input is not valid for the selected template\
   **When** I submit the resource source address\
   **Then** validation against the selected template fails\
   **And** the validation errors are displayed\
   **And** the submission is blocked until all validation issues are resolved.

**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&t=LT29Q6aB2kEPn6ew-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2565-53401&t=Tg2TKqZO3dR3zidj-4)

**Internal page (including conceptual Data Model):**

(internal page)

[Back to the list of user stories](#user-stories)

### User story 6: Create Resource Address Template for eDelivery

**Status:** Ready for DG Acceptance (backlog item SIMPL-18360)

**As a** system or application integrating with SIMPL,\
**I want to** use standardized templates for eDelivery resource addresses during both creation (as Resource Source) and consumption (as Resource Destination),\
**So that** resource transfers via the eDelivery channel are validated and executed correctly in the SD creation (BP5) and consumption (BP9A) processes.\
----

###### **Acceptance Criteria**

1. **Given** I am defining a resource address as part of SD creation (BP5) or initiating transfer (BP9A)\
   **And** I use the template structure for type `eDelivery`\
   **Then** the system validates that the address includes the required fields based on the context (Source or Destination)\
   ----

###### **Examples**

**Resource Source Template (used during creation):**
```
{
     "type": "eDelivery"
     "edeliveryTriggerAPI": "http://simpl-poc/v1/simpl-edelivery-trigger",
     "appDataIdentifier": "simpl-edelivery-poc-01"
} 
```
**Field Requirements for Source Address:**

| param | data type | allowed values | optional/mandatory |
| --- | --- | --- | --- |
| type | string | eDelivery | mandatory |
| edeliveryTriggerAPI | URL |  | mandatory |
| appDataIdentifier | string |  | mandatory |

**Resource Destination Template (used during consumption):**
```
{
    "type": "eDelivery",
    "accessServiceIdentifier": "1",
    "requestorIdentifier": "22",
    "dateFrom": "2002-02-22",
    "dateTo": "2002-02-22",
    "format": "csv",
    "service": "service1",
    "action": "service2"
} 
```

**Field Requirements for Destination Address:**

| param | data type | allowed values | optional/mandatory |
| --- | --- | --- | --- |
| type | string | eDelivery | mandatory |
| accessServiceIdentifier | string |  | mandatory |
| requestorIdentifier | string |  | mandatory |
| dateFrom | string |  | optional |
| dateTo | string |  | optional |
| format | list or String |  | optional |
| service | string |  | optional |
| action | string |  | optional |

Useful Documentation:

(internal page)

(internal page)

(internal page)

(internal page)

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
