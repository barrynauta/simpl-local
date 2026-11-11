<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.4 A Provider updates the usage policies of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b34-a-provider-updates-the-usage-policies-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the usage policies of a new version of a resource description by using policy template via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.4 A Provider updates the usage policies of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b34-a-provider-updates-the-usage-policies-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.10 Update policies and contract related metadata](../README.md#bp05b10-update-policies-and-contract-related-metadata).

In the backlog it is item SIMPL-18030, status **In Progress**. It has 5 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** update the usage policies using an usage policy template to a new version of a resource description via UI\
**So that** I can ensure that the use of the the new resource description and the underlying resource is aligned with my internal rules and obligations.

#### Acceptance criteria

**Acceptance Criterion 1: Add usage policies to a new version of a resource description**

**Scenario:** A Provider assigns the usage policies of a new version of a resource description by using usage policy template via UI

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Usage policies" in the wizard\
   **Then** I can assign usage policies by using a usage policy template to the new resource description.

**Acceptance Criterion 2: Remove usage policies from a new version of a resource description**

**Scenario:** A Provider removes the usage policies from a new version of a resource description which were assigned with usage policy template via UI

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Usage policies" in the wizard\
   **Then** I can remove usage policies which were assigned with a policy template from the new resource description.

**Acceptance Criterion 3: Finish updates of a new version of the resource description**

**Scenario:** A Provider finishes updating usage policies of a new version of a resource description via UI

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Usage policies" in the wizard\
   **And** I have assigned or removed usage policies from the new resource description\
   **Then** I can navigate to the next step in the wizard.

## User stories

1. [[UI] Provider sees the usage policies carried over from the previous resource description version](#user-story-1-ui-provider-sees-the-usage-policies-carried-over-from-the-previous-resource-description-version) (PBI INTERNAL QA)
2. [[UI] Provider selects a usage policy template in the new resource description version wizard](#user-story-2-ui-provider-selects-a-usage-policy-template-in-the-new-resource-description-version-wizard) (PBI INTERNAL QA)
3. [[UI] Provider fills in the usage policy template parameters in the new resource description version wizard](#user-story-3-ui-provider-fills-in-the-usage-policy-template-parameters-in-the-new-resource-description-version-wizard) (PBI INTERNAL QA)
4. [[UI] Provider removes a usage policy from the new resource description version](#user-story-4-ui-provider-removes-a-usage-policy-from-the-new-resource-description-version) (PBI INTERNAL QA)
5. [[UI] Provider saves the updated usage policies on the new resource description version](#user-story-5-ui-provider-saves-the-updated-usage-policies-on-the-new-resource-description-version) (FOR SC TO RESPOND)

### User story 1: [UI] Provider sees the usage policies carried over from the previous resource description version

**Status:** PBI INTERNAL QA (backlog item SIMPL-31575)

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** see the usage policies already assigned to a resource description when I prepare a new RD version,\
**So that** I can review and retain them or make changes before submitting.

#### Acceptance criteria

##### Show the existing usage policies

1. **Given** I am creating a new version of an existing resource description\
   **And** the existing resource description has been loaded\
   **When** I reach the Usage policies step\
   **Then** its assigned usage policies are shown with their saved values\
   **And** policies supported by the available templates can be opened for editing\
   **And** I can keep or remove the listed policies

##### No usage policies are assigned

2. **Given** I am creating a new resource-description version\
   **And** the existing resource description has no usage policies\
   **When** I reach the Usage policies step\
   **Then** the UI shows an empty list\
   **And** I can add a usage policy if a template is available\
   **And** I can continue without adding a usage policy

##### A saved policy cannot be reopened for editing

3. **Given** the existing resource description contains a usage policy\
   **When** the form needed to edit that policy cannot be opened\
   **Then** the policy remains in the resource description with its saved values\
   **And** I can view it as read-only information\
   **And** I can keep it or remove it and add a replacement from an available template\
   **And** I can continue preparing the resource description

##### The existing resource description cannot be loaded

4. **Given** I want to create a new version of an existing resource description\
   **When** the existing resource description cannot be retrieved\
   **Then** the UI shows an error and offers a retry\
   **And** it does not open an empty new-version wizard in its place

#### Technical details

- Read the assigned usage policies from the existing RD content and copy their saved values into wizard state. Display editable policies through the same controls used for policies added in the wizard.
- Resolve an available template only to restore the policy's form. Open it for editing when its fixed terms and field structure are compatible with the saved policy.
- If the template is missing, unavailable or incompatible, preserve the completed policy as read-only. Do not replace saved values with current template defaults.
- Retention applies to the saved policy document. To replace a retained document with template-based policies, remove it first and then add the replacement policies.
- Loading an editing form and loading the RD are separate operations. Failure to load a form does not discard the RD or its policy content.
- Keep the saved policies in wizard state. Final submission and policy validation use the existing RD submission flow and the related stories.
- This story covers showing the existing usage policies and their editing availability. Selection, field entry, removal and submission are covered by SIMPL-31576, SIMPL-31577, SIMPL-31578 and SIMPL-31579.

#### Additional notes

- API definition ticket: (internal page)
- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 technical description (internal page)

[Back to the list of user stories](#user-stories)

### User story 2: [UI] Provider selects a usage policy template in the new resource description version wizard

**Status:** PBI INTERNAL QA (backlog item SIMPL-31576)

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** select a usage-policy template while creating a new resource-description version,\
**So that** I can add a usage policy from a reviewed template.

#### Acceptance criteria

##### Retrieve the list of usage-policy templates

1. **Given** I am selecting a usage-policy template in the new-version wizard\
   **When** the available template list is retrieved successfully\
   **Then** the UI displays the returned usage-policy templates with their user-facing names

##### Select a template from the list

2. **Given** the available usage-policy templates are displayed\
   **When** I select a template and its details are retrieved successfully\
   **Then** the selected template and its details are available for preparing the policy

Field entry remains covered by SIMPL-31577. Adding the policy remains covered by SIMPL-31579.

##### Continue without a selection

3. **Given** I have not selected a usage-policy template\
   **When** I continue to the next step\
   **Then** the wizard allows me to continue without adding a new usage policy\
   **And** it does not remove any carried-over policies

##### No templates are installed

4. **Given** I am creating a new resource-description version\
   **When** I reach the Usage policies step\
   **And** the template API returns an empty list for kind usage\
   **Then** the UI shows that no templates are available\
   **And** I can continue without adding a new policy

##### Template retrieval fails

5. **Given** I am creating a new resource-description version\
   **When** I reach the Usage policies step\
   **And** the template API returns an error\
   **Then** the UI shows an error and offers a retry\
   **And** no new template can be selected until retrieval succeeds\
   **And** I can continue without adding a new policy

#### Technical details

This story covers selection. The field-entry story covers rendering. Policy confirmation and final RD submission are separate actions. The shared API is defined in SIMPL-31345. A failure to retrieve new templates does not remove existing usage policies.

#### Additional notes

- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

[Back to the list of user stories](#user-stories)

### User story 3: [UI] Provider fills in the usage policy template parameters in the new resource description version wizard

**Status:** PBI INTERNAL QA (backlog item SIMPL-31577)

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** enter values in the selected usage-policy template,\
**So that** I can prepare a usage policy for the new RD version.

#### Acceptance criteria

##### Render the template fields

1. **Given** I selected a usage-policy template in the new-version wizard\
   **When** its details are retrieved\
   **Then** the UI renders its ordered fields using their labels, types and controls\
   **And** it retrieves approved choices where the field defines a value source

##### Keep entered values

2. **Given** the selected template's fields are displayed\
   **When** I enter or change a field value\
   **Then** the wizard keeps that value in the policy draft

##### Change a template after entering values

3. **Given** I entered one or more values for the selected template\
   **When** I select another template\
   **Then** the UI asks me to confirm that those values will be discarded\
   **And** confirmation clears the old values and loads the new fields\
   **And** cancellation keeps the existing template and values

##### Template details cannot be retrieved

4. **Given** I selected a template\
   **When** retrieval of its details fails\
   **Then** the UI shows an error and offers a retry\
   **And** it does not display a form from another template\
   **And** it does not add the policy until the correct details are available

#### Technical details

Usage-policy forms do not display the catalogue/negotiation/transfer selector used by access policies. Any future monitoring metadata must follow the HLF4 contract.

This story covers field rendering and value capture. SIMPL-31454 (internal page) defines validation on policy confirmation. The save/add story defines copying the ODRL policy, filling declared slots and adding the completed policy to wizard state. The UI must support the templates returned by the API without hardcoding a preset count.

Policy confirmation and final submission: SIMPL-31579 (internal page).

#### Additional notes

- API definition ticket: (internal page)
- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

[Back to the list of user stories](#user-stories)

### User story 4: [UI] Provider removes a usage policy from the new resource description version

**Status:** PBI INTERNAL QA (backlog item SIMPL-31578)

#### Storycard:

**As a** Provider with a role authorising the management of resource descriptions\
**I want to** remove a usage policy from the new resource description version at the "Usage policies" step\
**So that** the new version only carries the usage policies I want to keep

#### Acceptance criteria:

**Scenario:** Provider removes a usage policy from the new version

1. **Given** I am authenticated in the Provider UI with a role authorising the management of resource descriptions\
   **And** the "Usage policies" step lists one or more usage policies carried over from the previous version or added from a template\
   **When** I remove one of the listed usage policies\
   **Then** the UI removes that usage policy from the set to be saved on the new version\
   **And** the remaining usage policies stay unchanged and available for editing

##### Negative scenario

**Scenario:** No usage policies are present to remove

2. **Given** I am authenticated in the Provider UI with a role authorising the management of resource descriptions\
   **And** the "Usage policies" step has no assigned usage policies\
   **When** I view the list of usage policies\
   **Then** the remove action is not available\
   **And** the step shows an empty state for usage policies

#### Technical details:

- Removal operates on the usage policies held in the wizard state; the change is persisted only when the new version is submitted.
- A removed usage policy is excluded from the assembled set sent to the existing resource description API on submission.

API definition ticket: (internal page)

#### Additional notes:

- (internal page)
- (internal page)

[Back to the list of user stories](#user-stories)

### User story 5: [UI] Provider saves the updated usage policies on the new resource description version

**Status:** FOR SC TO RESPOND (backlog item SIMPL-31579)

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** include the usage policies I kept, added or changed in the new resource-description version,\
**So that** the submitted version carries the policy set I chose.

#### Acceptance criteria

##### Add or save a policy in the wizard

1. **Given** I entered the required values for a usage-policy template\
   **When** I click Add policy for a new policy or Save changes for an edited policy\
   **Then** the UI checks the values against the template's field rules\
   **And** it copies the template's ODRL policy and fills only the declared slots\
   **And** a valid completed policy is added to or replaces its entry in wizard state\
   **And** the policy stays in the current wizard until I submit the complete resource description

##### Submit the complete version

2. **Given** the new-version wizard contains the usage policies I intend to keep\
   **When** I submit the complete resource-description version\
   **Then** the UI includes those completed policies in the RD submission payload\
   **And** the publication component validates the final policies before publication\
   **And** only a successful submission saves those policies with the new version\
   **And** the previous published version remains unchanged

##### Invalid policy values

3. **Given** required policy values are missing or invalid\
   **When** I confirm that policy\
   **Then** the UI shows validation feedback\
   **And** it keeps the form open with the entered values\
   **And** it does not add an invalid policy to the RD

##### Final submission fails

4. **Given** I submit the new resource-description version\
   **When** the submission fails\
   **Then** the UI shows the failure\
   **And** retains the wizard data and policy set for correction or retry\
   **And** it does not report the new version as published

##### Unauthorised submission

5. **Given** I am authenticated without a role authorising management of resource descriptions\
   **When** I submit the new resource-description version with its policies\
   **Then** the service returns 403 Forbidden\
   **And** the policies are not saved

#### Technical details

The template envelope contains an ODRL JSON-LD `policy` and ordered `fields`. The UI uses field names to read values and field paths to replace only declared slots in a copy of the policy. It preserves fixed policy structure. The publication component validates the completed policy and any final policy IDs and asset targets before publication. Browser validation alone does not establish that a policy is valid.

Usage policies do not acquire access-enforcement scopes in this flow. Usage monitoring belongs to HLF4. Keep retained usage policies as completed ODRL policies; do not force every policy into a duty-only structure.

Use the existing RD submission integration linked by SIMPL-25712. Invalidate a prior validation result when the Provider changes the submitted policy values.

#### Additional notes

- API definition ticket: (internal page)
- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

[Back to the list of user stories](#user-stories)

---

Back to [5B.3.4 A Provider updates the usage policies of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b34-a-provider-updates-the-usage-policies-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
