<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the resource source address for resource sharing

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.08 Create a new version of a resource description](../README.md#bp05b08-create-a-new-version-of-a-resource-description).

In the backlog (Jira) it is item SIMPL-12250. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider,\
**I want to** update via API or UI, as part of a resource description, the Resource Source based on the resource address template that is valid for the resource sharing method, which I use for sharing my resources,\
**So that** the Resource Source is published with the resource description and provides information about the Resource Source required to support the automated resource sharing.\
----

###### Acceptance Criteria:

**1. Update of Resource Address with Existing Resource**\
*

1. **Given** I am a provider user with permission to manage resource descriptions\
   **When** I update the resource address in an existing resource description using a valid template for the selected resource sharing method\
   **Then** the updated resource address is validated with respect to the template, and feedback is provided indicating whether it is valid

**2. Acceptance of Valid Resource Address Update**\
*

2. **Given** I am a provider user with permission to manage resource descriptions\
   **And** I provide an updated resource address valid with respect to the selected resource address template\
   **When** the updated resource description is submitted\
   **Then** the new resource address is accepted and stored with the resource description

**3. Rejection of Invalid Resource Address Update**\
*

3. **Given** I attempt to update the resource address using a value that does not conform to the selected resource address template\
   **When** the update is submitted\
   **Then** the update is rejected, and a clear and actionable error message is returned

**4. Visibility of Updated Resource Address**\
*

4. **Given** a resource description with an updated resource address has been successfully submitted\
   **When** a user accesses the published resource\
   **Then** the updated resource address is visible in the resource details

**5. Error Handling for Invalid Updates**\
*

5. **Given** I attempt to update the resource address with an invalid template or one that doesn’t match the selected resource sharing method\
   **When** I submit the update\
   **Then** the update is rejected, and a clear error message is returned

**6. System Extensibility for Future Update Scenarios**\
*

6. **Given** new resource sharing methods and templates may be introduced\
   **When** I update a resource address using a newly supported template\
   **Then** the update is accepted and processed without requiring changes to the update logic

## User stories

1. [[UI] Selecting, Filling, and Submitting the Resource Source Template (Provider)](#user-story-1-ui-selecting-filling-and-submitting-the-resource-source-template-provider)

### User story 1: [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider)

Backlog item SIMPL-13565

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

---

Back to [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
