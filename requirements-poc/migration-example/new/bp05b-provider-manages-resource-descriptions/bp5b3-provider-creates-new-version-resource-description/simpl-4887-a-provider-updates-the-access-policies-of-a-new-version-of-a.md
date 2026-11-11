<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.3 A Provider updates the access policies of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b33-a-provider-updates-the-access-policies-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.3 A Provider updates the access policies of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b33-a-provider-updates-the-access-policies-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.10 Update policies and contract related metadata](../README.md#bp05b10-update-policies-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-4887. It has 5 user stories, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** to use, via UI, an access policy template to assign an access policy to a new version of a resource description\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned with my internal rules and obligations.

#### Acceptance criteria

**Acceptance Criterion 1: Add access policies to a new version of a resource description**

**Scenario:** Provider assigns access policies of a new version of a resource description by using a policy template

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Access policies" in the wizard\
   **Then** I can retrieve the access policy templates\
   **And** I can select the access policy template I want to use\
   **And** I can fill in the parameters of the access policy template

**Acceptance Criterion 2: Remove access policies from a new version of a resource description**

**Scenario:** A Provider removes access policy from a new version of a resource description which were assigned with a policy template via UI

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Access policies" in the wizard\
   **Then** I can remove access policies which were assigned with a policy template from the new resource description.

**Acceptance Criterion 3: Finish updates of a new version of the resource description**

**Scenario:** A Provider finishes updating access policies of a new version of a resource description via UI

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies" in the wizard\
   **And** I have assigned or removed access policies to from the new resource description\
   **Then** the updated access policies are assigned to the resource description I am updating

## User stories

1. [[UI] Provider sees the access policies carried over from the previous resource description version](#user-story-1-ui-provider-sees-the-access-policies-carried-over-from-the-previous-resource-description-version) · In development
2. [[UI] Provider selects an access policy template in the new resource description version wizard](#user-story-2-ui-provider-selects-an-access-policy-template-in-the-new-resource-description-version-wizard) · In development
3. [[UI] Provider fills in the access policy template parameters in the new resource description version wizard](#user-story-3-ui-provider-fills-in-the-access-policy-template-parameters-in-the-new-resource-description-version-wizard) · In development
4. [[UI] Provider removes an access policy from the new resource description version](#user-story-4-ui-provider-removes-an-access-policy-from-the-new-resource-description-version) · In development
5. [[UI] Provider saves the updated access policies on the new resource description version](#user-story-5-ui-provider-saves-the-updated-access-policies-on-the-new-resource-description-version) · Planned

### User story 1: [UI] Provider sees the access policies carried over from the previous resource description version

**Development:** In development · backlog item SIMPL-31543

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** see the previous version's access policies in the new-version wizard,\
**So that** I can retain them or change the new version.

#### Acceptance criteria

##### Carry over existing policies

1. **Given** I am creating a new version of a resource description\
   **When** the previous version's contents are retrieved successfully\
   **Then** the UI copies its access policies into the new version's wizard state\
   **And** it preserves their policy content and recorded scope association\
   **And** it shows any available "Applies during" information without letting me change it\
   **And** it does not replace existing values with current-template defaults

##### The previous version has no access policies

2. **Given** the previous version contains no access policies\
   **When** I reach the Access policies step\
   **Then** the UI shows an empty policy list\
   **And** I can add a policy using the template-selection flow\
   **And** I can continue without adding an access policy

##### Show enforcement information consistently

3. **Given** an access policy is carried over or newly added\
   **When** its scope information is available\
   **Then** the UI shows "Applies during" for both carried-over and newly added policies\
   **And** the Provider cannot change the template's intended scopes through the form

##### Previous contents cannot be retrieved

4. **Given** I am creating a new resource-description version\
   **When** retrieval of the previous version fails\
   **Then** the UI shows an error and offers a retry\
   **And** it does not treat the failed retrieval as a confirmed empty policy list

#### Technical details

The UI uses the existing previous-version retrieval flow. This story introduces no new endpoint. Policy-template list and detail data come from the shared policy-template API defined in SIMPL-31345. New policies use the current selected template. Existing policies retain their meaning; they are not rebuilt from the current template merely to display them.

#### Additional notes

- API definition ticket: (internal page)
- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

[Back to the list of user stories](#user-stories)

### User story 2: [UI] Provider selects an access policy template in the new resource description version wizard

**Development:** In development · backlog item SIMPL-31544

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** select an access-policy template while creating a new resource-description version,\
**So that** I can add an access policy from a reviewed template.

#### Acceptance criteria

##### Retrieve the list of access-policy templates

1. **Given** I am selecting an access-policy template in the new-version wizard\
   **When** the available template list is retrieved successfully\
   **Then** the UI displays the returned access-policy templates with their user-facing names

##### Select a template from the list

2. **Given** the available access-policy templates are displayed\
   **When** I select a template and its details are retrieved successfully\
   **Then** the selected template and its details are available for preparing the policy

Field entry remains covered by SIMPL-31545. Adding the policy remains covered by SIMPL-31547.

##### Continue without a selection

3. **Given** I have not selected an access-policy template\
   **When** I continue to the next step\
   **Then** the wizard allows me to continue without adding a new access policy\
   **And** it does not remove any carried-over policies

##### No templates are installed

4. **Given** I am creating a new resource-description version\
   **When** I reach the Access policies step\
   **And** the template API returns an empty list for kind access\
   **Then** the UI shows that no templates are available\
   **And** I can continue without adding a new policy

##### Template retrieval fails

5. **Given** I am creating a new resource-description version\
   **When** I reach the Access policies step\
   **And** the template API returns an error\
   **Then** the UI shows an error and offers a retry\
   **And** no new template can be selected until retrieval succeeds\
   **And** I can continue without adding a new policy

#### Technical details

This story covers selection. The field-entry story covers rendering. Policy confirmation and final RD submission are separate actions. The shared API is defined in SIMPL-31345. A failure to retrieve new templates does not remove existing access policies.

#### Additional notes

- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

[Back to the list of user stories](#user-stories)

### User story 3: [UI] Provider fills in the access policy template parameters in the new resource description version wizard

**Development:** In development · backlog item SIMPL-31545

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** enter values in the selected access-policy template,\
**So that** I can prepare an access policy for the new RD version.

#### Acceptance criteria

##### Render the template fields

1. **Given** I selected an access-policy template in the new-version wizard\
   **When** its details are retrieved\
   **Then** the UI renders its ordered fields using their labels, types and controls\
   **And** it retrieves approved choices where the field defines a value source

##### Retain entered values

2. **Given** the selected template's fields are displayed\
   **When** I enter or change a field value\
   **Then** the entered value is retained in the policy draft

##### Show access-enforcement information

3. **Given** the selected access template declares intended scopes\
   **When** the form is displayed\
   **Then** the UI shows that information as read-only\
   **And** it does not insert scope metadata into the ODRL policy

##### Change a template after entering values

4. **Given** I entered one or more values for the selected template\
   **When** I select another template\
   **Then** the UI warns me that switching will discard those values and asks me to confirm\
   **And** only after I confirm does the UI clear the old values and load the new fields\
   **And** if I cancel, the UI keeps the existing template and values

##### Template details cannot be retrieved

5. **Given** I selected a template\
   **When** retrieval of its details fails\
   **Then** the UI shows an error and offers a retry\
   **And** it does not display a form from another template\
   **And** it does not add the policy until the correct details are available

#### Technical details

This story covers field rendering and value capture. SIMPL-31407 (internal page) defines validation on policy confirmation. The save/add story defines copying the ODRL policy, filling declared slots and adding the completed policy to wizard state. The UI must support the templates returned by the API without hardcoding a preset count.

Policy confirmation and final submission: SIMPL-31547 (internal page).

#### Additional notes

- API definition ticket: (internal page)
- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

[Back to the list of user stories](#user-stories)

### User story 4: [UI] Provider removes an access policy from the new resource description version

**Development:** In development · backlog item SIMPL-31546

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** remove an access policy while preparing a new resource-description version,\
**So that** the new version contains only the policies I choose to keep.

#### Acceptance criteria

##### Remove an access policy

1. **Given** I am preparing a new resource-description version\
   **And** its access policies include a policy I want to remove\
   **When** I choose Remove for that policy\
   **Then** the policy is removed immediately from the resource description I am preparing\
   **And** the remaining policies stay unchanged\
   **And** the previously published resource description stays unchanged

##### Submit after removing a policy

2. **Given** I removed an access policy while preparing a new resource-description version\
   **When** I submit the completed resource description\
   **Then** the submitted policy set excludes the removed policy

##### No access policies are present

3. **Given** the resource description I am preparing has no access policies\
   **When** I view its access policies\
   **Then** the UI shows an empty list\
   **And** no policy removal action is available

#### Technical details

- Remove acts directly on the policy set held in wizard state, without a confirmation dialog.
- The change is persisted only when the complete resource-description version is submitted through the existing RD API.
- A saved policy that cannot be reopened through its template can still be removed. If it is retained as one saved policy document, removal applies to that document as a whole.
- This story covers removal. Template selection, field entry and final submission are covered separately.

#### Additional notes

- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 technical description (internal page)

[Back to the list of user stories](#user-stories)

### User story 5: [UI] Provider saves the updated access policies on the new resource description version

**Development:** Planned · backlog item SIMPL-31547

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** include the access policies I kept, added or changed in the new resource-description version,\
**So that** the submitted version carries the policy set I chose.

#### Acceptance criteria

##### Add or save a policy in the wizard

1. **Given** I entered the required values for an access-policy template\
   **When** I click Add policy for a new policy or Save changes for an edited policy\
   **Then** the UI checks the values against the template's field rules\
   **And** it copies the template's ODRL policy and fills only the declared slots\
   **And** a valid completed policy is added to or replaces its entry in wizard state\
   **And** the policy stays in the current wizard until I submit the complete resource description

##### Submit the complete version

2. **Given** the new-version wizard contains the access policies I intend to keep\
   **When** I submit the complete resource-description version\
   **Then** the UI includes those completed policies in the RD submission payload\
   **And** the publication component validates the final policies before publication\
   **And** only a successful submission saves those policies with the new version\
   **And** the previous published version remains unchanged

##### Invalid policy values

3. **Given** required policy values are missing or invalid\
   **When** I save changes to that policy\
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

The template envelope contains an ODRL JSON-LD `policy` and ordered `{}fields{`}. The UI uses field names to read values and field paths to replace only declared slots in a copy of the policy. It preserves fixed policy structure. The publication component validates the completed policy and any final policy IDs and asset targets before publication. Browser validation alone does not establish that a policy is valid.

Keep access-scope routing metadata associated with the policy outside its ODRL tree. Do not derive a historical policy's scope from a changed current template.

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

Back to [5B.3.3 A Provider updates the access policies of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b33-a-provider-updates-the-access-policies-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
