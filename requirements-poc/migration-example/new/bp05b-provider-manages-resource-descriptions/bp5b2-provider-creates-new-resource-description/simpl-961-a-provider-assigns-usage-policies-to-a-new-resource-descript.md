<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.5 A Provider assigns usage policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider assigns usage policies to a new resource description by using policy template via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.5 A Provider assigns usage policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-961, status **In Progress**. It has 6 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** use, via UI, an usage policy template to assign an usage policy to a new resource description\
**So that** I can ensure that the usage of the underlying resource is aligned with my internal rules and obligations

**Acceptance Criteria**

**Scenario:** **A Provider assigns usage policies** **by using policy template** **to a new resource description via UI**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I reach the step "Usage policies" in the wizard\
   **When** I retrieve the usage policy templates\
   **And** I select the usage policy template I want to use\
   **And** I fill in the parameters of the usage policy template\
   **Then** a usage policy is created based on the selected usage policy template and values I provided for the parameters\
   **And** the created usage policy is assigned to the resource description I am creating

## User stories

1. [UI for definition of usage policies](#user-story-1-ui-for-definition-of-usage-policies) (DevTeam PBI is ready for Implementation check)
2. [[UI] Provider selects a usage policy template in the resource description wizard](#user-story-2-ui-provider-selects-a-usage-policy-template-in-the-resource-description-wizard) (DevTeam PBI is ready for Implementation check)
3. [[UI] Provider fills in the usage policy template parameters in the resource description wizard](#user-story-3-ui-provider-fills-in-the-usage-policy-template-parameters-in-the-resource-description-wizard) (DevTeam PBI is ready for Implementation check)
4. [[UI] Provider sees inline validation feedback for usage policy template parameters](#user-story-4-ui-provider-sees-inline-validation-feedback-for-usage-policy-template-parameters) (DevTeam PBI is ready for Implementation check)
5. [[UI] Provider adds a usage policy to a new Resource Description](#user-story-5-ui-provider-adds-a-usage-policy-to-a-new-resource-description) (DevTeam PBI is ready for Implementation check)
6. [[UI] Provider removes a usage policy from the new resource description](#user-story-6-ui-provider-removes-a-usage-policy-from-the-new-resource-description) (DevTeam PBI is ready for Implementation check)

### User story 1: UI for definition of usage policies

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-424)

**As a** Provider, creating a Self-Description through a UI,\
**I want to** have an extension of the Self-description UI for creating a Usage Policy that will be assigned to the Self-Description\
**So that** I can define the usage policy for the Self-Description being created, i.e. define the constraints under which a consumer can use my resource

-----------------------

**Technical Detail**

The new sd-creator-frontend (SIMPL-1083 (internal page)) should be extended with the capabilities to set the parameters for the usage policies. The supported usage policies are described in (internal page)

It should be possible in the UI to describe one or more constraints for the usage of the data. The supported types of constraints are:

1. Restricted Number of Usages (one parameter that is the number of allowed access to the dataset)
1. Duration-restricted Data Usage (two date time parameter that describe the from and until of usage)
1. Deletion after usage (no parameter)

It should be possible to add constraints and set the parameter. The backend will later translate those into valid ODRL Policies.

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

**Mock**

(image not shown)

**AC:**

1. Three different types of usage policy constraint are supported
1. The user can add more constraints with a button
1. The user can remove constraints with a button
1. Depending on the type 0, 1 or 2 parameters are set
1. After pressing the "save button" the parameters are passed to the API (SIMPL-3241) and the usage policie(s) are attached to the SD

[Back to the list of user stories](#user-stories)

### User story 2: [UI] Provider selects a usage policy template in the resource description wizard

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31452)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** select a usage-policy template in the Resource Description wizard\
**So that** I can use a predefined template when adding a usage policy

#### Acceptance criteria

##### Provider selects a usage-policy template

1. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **And** I am at the "Usage policies" step while creating a Resource Description\
   **When** the UI retrieves the usage-policy templates through the shared policy-template API using the usage-policy filter\
   **Then** the UI displays all predefined usage-policy templates\
   **And** selecting a template stores it as the chosen template\
   **And** loads its details for the parameter form

##### Provider continues without selecting a template

2. **Given** one or more usage-policy templates are available\
   **And** I have not selected a template\
   **When** I continue to the next step\
   **Then** the wizard allows me to continue\
   **And** no validation message is shown for the missing selection\
   **And** no usage policy is added

##### No usage-policy templates are available

3. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I reach the "Usage policies" step\
   **And** the policy-template API returns an empty list for the usage-policy filter\
   **Then** the UI shows that no templates are available\
   **And** I can continue without adding a usage policy

##### Usage-policy templates cannot be retrieved

4. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I reach the "Usage policies" step\
   **And** the policy-template API returns an error\
   **Then** the UI shows an error and offers a retry\
   **And** no template can be selected until retrieval succeeds\
   **And** I can continue without adding a usage policy

#### Technical details

- This story covers front-end behaviour only.
- Access and usage templates use the shared policy-template API defined in SIMPL-31345, with filtering by policy type.
- The selector displays the identifier and user-facing information returned by the API.
- The selected template identifier is stored in the wizard state and is used to retrieve the template details.
- Usage policies are optional. An empty selection does not block navigation or final Resource Description submission.

#### Additional notes

- Shared API definition: SIMPL-31345 (internal page)
- Usage-template details: SIMPL-31386 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)
- [UX/UI design](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI)

[Back to the list of user stories](#user-stories)

### User story 3: [UI] Provider fills in the usage policy template parameters in the resource description wizard

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31453)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want** the Usage policies step to show the fields of the selected template\
**So that** I can enter the values needed for a usage policy

#### Acceptance criteria

##### Provider fills in a usage-policy template

1. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **And** I selected a usage-policy template in the Resource Description wizard\
   **When** the template details are loaded\
   **Then** the UI displays the fields, labels and guidance described by the template\
   **And** stores the values I enter in the wizard state

##### Provider changes the selected template

2. **Given** I selected a usage-policy template\
   **And** I entered one or more values\
   **When** I choose a different template\
   **Then** the UI asks me to confirm that the entered values will be discarded\
   **And** confirming the change clears those values and displays the new template fields\
   **And** cancelling the change keeps the current template and values

##### Template details cannot be retrieved

3. **Given** I selected a usage-policy template\
   **When** the template details cannot be retrieved\
   **Then** the UI shows an error and offers a retry\
   **And** no parameter fields are displayed\
   **And** the policy cannot be added until the details are available

#### Technical details

- This story covers displaying template fields and keeping entered values in the wizard state.
- The form is built from the selected template details and must work with any predefined usage-policy template returned by the API.
- Concrete fields are not listed in this story.
- Usage-policy templates do not use the access-policy "Applies during" scopes.
- SIMPL-31454 covers validation when the Provider confirms the policy.
- SIMPL-31455 covers creating the policy instance, attaching it to the wizard state and including it in the final Resource Description submission.

#### Additional notes

- Usage-template details: SIMPL-31386 (internal page)
- Validation: SIMPL-31454 (internal page)
- Resource Description submission: SIMPL-31455 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)

[Back to the list of user stories](#user-stories)

### User story 4: [UI] Provider sees inline validation feedback for usage policy template parameters

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31454)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want** clear validation feedback when I confirm a usage policy\
**So that** I can correct invalid values before adding the policy

#### Acceptance criteria

##### Valid values pass validation

1. **Given** I completed the fields of a usage-policy template with valid values\
   **When** I confirm the policy in the modal\
   **Then** the usage-policy backend validates the policy\
   **And** no validation errors are shown\
   **And** the valid policy can be added to the Resource Description

##### Invalid values block confirmation

2. **Given** one or more values do not meet the selected template's rules\
   **When** I confirm the policy in the modal\
   **Then** the usage-policy backend validates the policy\
   **And** the UI shows a clear message for each invalid value\
   **And** the modal remains open\
   **And** the policy is not added to the Resource Description

##### Missing required values block confirmation

3. **Given** one or more required values are missing\
   **When** I confirm the policy in the modal\
   **Then** the UI shows which values are required\
   **And** the modal remains open\
   **And** the policy is not added to the Resource Description

##### Provider corrects invalid values

4. **Given** validation errors are shown\
   **When** I correct the values and confirm the policy again\
   **Then** the policy is validated again\
   **And** resolved errors are cleared\
   **And** the valid policy can be added to the Resource Description

#### Technical details

- The required validation trigger is confirmation of the policy in the modal.
- Earlier field feedback may also be shown, but it does not replace validation on confirmation.
- Validation uses the rules supplied by the selected template; concrete fields and rules are not listed in this story.
- SIMPL-31453 covers displaying the template fields and keeping entered values.
- This story covers validation messages and blocking confirmation when values are invalid.
- SIMPL-31455 covers adding a valid policy to the wizard and including it in the final Resource Description submission.

#### Additional notes

- Usage-template details: SIMPL-31386 (internal page)
- Template fields: SIMPL-31453 (internal page)
- Resource Description submission: SIMPL-31455 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)

[Back to the list of user stories](#user-stories)

### User story 5: [UI] Provider adds a usage policy to a new Resource Description

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31455)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want** a usage policy created from a template to be added to the Resource Description I am creating\
**So that** the policy is included when I submit the Resource Description

#### Acceptance criteria

##### Provider adds a usage policy to the Resource Description

1. **Given** I selected a usage-policy template\
   **And** I entered the required values\
   **When** I confirm the policy in the modal\
   **Then** the usage-policy backend validates the policy\
   **And** a valid policy is added to the current Resource Description in the wizard\
   **And** the Resource Description is not submitted by this action alone

##### Provider submits the Resource Description with a usage policy

2. **Given** a valid usage policy is attached to the Resource Description in the wizard\
   **When** I submit the Resource Description\
   **Then** the usage policy is included in the same Resource Description payload

##### Policy validation fails

3. **Given** I entered invalid policy values\
   **When** I confirm the policy in the modal\
   **Then** the policy is not added to the Resource Description\
   **And** the modal remains open with validation feedback

##### Resource Description submission fails

4. **Given** a valid usage policy is attached to the Resource Description in the wizard\
   **When** the Resource Description submission fails\
   **Then** the UI shows the error\
   **And** keeps the wizard data and usage policy so I can retry

#### Technical details

- Confirming the policy and submitting the Resource Description are separate actions.
- Confirming a valid policy adds it to the current wizard state.
- Final submission sends the policy in the same Resource Description payload.
- No separate policy persistence endpoint is introduced.
- The exact Resource Description API operation will be linked when the implementation artifact is available.
- This flow works with any usage-policy template returned by the policy-template API.
- Usage policies do not use the access-policy "Applies during" scopes.

#### Additional notes

- Usage-policy validation: SIMPL-31454 (internal page)
- Resource Description API: SIMPL-25712 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)
- [UX/UI design](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI)

[Back to the list of user stories](#user-stories)

### User story 6: [UI] Provider removes a usage policy from the new resource description

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31704)

#### Storycard:

**As a** Provider with a role authorising the management of resource descriptions\
**I want to** remove a usage policy from the new resource description at the "Usage policies" step\
**So that** the new resource description only carries the usage policies I want to keep

#### Acceptance criteria:

**Scenario:** Provider removes a usage policy from the new resource description

1. **Given** I am authenticated in the Provider UI with a role authorising the management of resource descriptions\
   **And** I am creating a new resource description and the "Usage policies" step lists one or more usage policies added from a template\
   **When** I remove one of the listed usage policies\
   **Then** the UI removes that usage policy from the set to be saved on the new resource description\
   **And** the remaining usage policies stay unchanged and available for editing

##### Negative scenario

**Scenario:** No usage policies are present to remove

2. **Given** I am authenticated in the Provider UI with a role authorising the management of resource descriptions\
   **And** I am creating a new resource description and the "Usage policies" step has no assigned usage policies\
   **When** I view the list of usage policies\
   **Then** the remove action is not available\
   **And** the step shows an empty state for usage policies

#### Technical details:

- Removal operates on the usage policies held in the wizard state; the change is persisted only when the new resource description is submitted.
- A removed usage policy is excluded from the assembled set sent to the existing resource description API on submission.

API definition ticket: (internal page)

#### Additional notes:

- (internal page)
- (internal page)

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.5 A Provider assigns usage policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
