<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider assigns access policies to a new resource description by using policy template via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-936, status **In Progress**. It has 8 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** use, via UI, an access policy template to assign an access policy to a new resource description\
**So that** I can ensure that the access to the new resource description and the underlying resource is aligned with my internal rules and obligations.

**Acceptance Criteria**

**Scenario:** A Provider assigns access policies by using a policy template to a new resource description via UI

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies" in the wizard\
   **And** I retrieve the access policy templates\
   **And** I select the access policy template I want to use\
   **And** I fill in the parameters of the access policy template\
   **Then** an access policy is created based on the selected access policy template and values I provided for the parameters\
   **And** the created access policy is assigned to the resource description I am creating

## User stories

1. [UI for defining access policies](#user-story-1-ui-for-defining-access-policies) (DevTeam PBI is ready for Implementation check)
2. [Define Access Policy for the SD-Schema](#user-story-2-define-access-policy-for-the-sd-schema) (DevTeam PBI is ready for Implementation check)
3. [[UI] Access Policy multiple attributes per Permission](#user-story-3-ui-access-policy-multiple-attributes-per-permission) (FOR SC TO RESPOND)
4. [[UI] Provider selects an access policy template in the resource description wizard](#user-story-4-ui-provider-selects-an-access-policy-template-in-the-resource-description-wizard) (DevTeam PBI is ready for Implementation check)
5. [[UI] Provider fills in the access policy template parameters in the resource description wizard](#user-story-5-ui-provider-fills-in-the-access-policy-template-parameters-in-the-resource-description-wizard) (DevTeam PBI is ready for Implementation check)
6. [[UI] Provider sees inline validation feedback for access policy template parameters](#user-story-6-ui-provider-sees-inline-validation-feedback-for-access-policy-template-parameters) (DevTeam PBI is ready for Implementation check)
7. [[UI] Provider adds an access policy to a new Resource Description](#user-story-7-ui-provider-adds-an-access-policy-to-a-new-resource-description) (DevTeam PBI is ready for Implementation check)
8. [[UI] Provider removes an access policy from the new resource description](#user-story-8-ui-provider-removes-an-access-policy-from-the-new-resource-description) (DevTeam PBI is ready for Implementation check)

### User story 1: UI for defining access policies

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-493)

**As a** Provider User\
**I want** use an UI to support the formulation of the access policies in a machine-readable format\
**So that** I can define the access policies to my dataset/application/infrastructure, i.e. define the condition that a consumer can access these resources.

-----------------------

**Technical Detail**

Editor to set the parameters for the minimal access policy defined in (internal page)

(image not shown)(image not shown)

The UI should allow to write one or multiple access policies. For each Policy it is possible to set 4 parameters

- action: Can be either be search, consume, restrictedConsume
- attribute: select one of the available security attributes. The list of attributes are from an API of Aruba (an internal page: _*GET /identity-attribute/* retrieve all identity attributes defined by Dataspace Governance Authority, marking the attributes assigned to the caller participant agent with assigned_to = caller participantID). For the dev cluster this API needs to be replaced by a mock with a fixed list of attributes and for integration replaced by the real API_
- *valid from: date time picker to select the from value (can optionally not be set)*
- *valid until: date time picker to select the until value (can optionally not be set)*

*It should be possible to add more policies with an add policy button, and the existing ones can be deleted with another button.*

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

Currently no support for ODRL Templates which will likely be part of another release.

AC:

1. The access policy is composed by 4 properties, 2 are mandetory and 2 are optional that the user needs to fill out
1. The user can select the action (read or use) from a dropdown
1. The user can select a security attribute from a dropdown
1. After pressing the "save button" the parameters are passed to the API (SIMPL-2938) and the Access policie(s) are attached to the SD

[Back to the list of user stories](#user-stories)

### User story 2: Define Access Policy for the SD-Schema

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-2581)

Create a policy that has Read or Write as an action and link it to attributes that are in the ABAC context of Authentication and Onboarding.

Link to internal wiki:

(internal page)

FE: String field to enter the policy (TBD Internal ,,POC'') - To address in SIMPL-493

BE: to address in the SIMPL-492

AC:

1. Access policy is defined and provided to the developers

[Back to the list of user stories](#user-stories)

### User story 3: [UI] Access Policy multiple attributes per Permission

**Status:** FOR SC TO RESPOND (backlog item SIMPL-3545)

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** select one or more identity attributes for an access policy,\
**So that** I can specify which attributes a Consumer must have to access my resource.

#### Acceptance criteria

##### Select the required identity attributes

1. **Given** I am adding an access policy to a resource description\
   **When** I choose the policy template for identity attributes\
   **Then** I can select one or more of the available identity attributes\
   **And** the form explains that the Consumer must have every attribute I select

##### Add the policy to the resource description

2. **Given** I have selected the identity attributes required for access\
   **When** I confirm the policy\
   **Then** the policy is added to the resource description I am preparing\
   **And** it shows the identity attributes I selected\
   **And** confirming the policy does not submit the resource description

##### No identity attribute is selected

3. **Given** I am adding an access policy that requires identity attributes\
   **And** I have not selected any identity attribute\
   **When** I confirm the policy\
   **Then** I am told to select at least one identity attribute\
   **And** the policy form remains open\
   **And** the policy is not added to the resource description

##### Available identity attributes cannot be loaded

4. **Given** I am adding an access policy that requires identity attributes\
   **When** the available identity attributes cannot be loaded\
   **Then** I see an error message and an option to retry\
   **And** I cannot confirm the policy until I can select the required attributes

#### Technical details

The `identity-attributes` template contains an ODRL JSON-LD `policy` and ordered `fields`. The UI copies the policy and fills only the declared field path with the selected attribute codes. The multiple-attribute constraint uses `odrl:isAllOf` and a nonempty `odrl:rightOperand` list. Preserve the context declaration `@container: @list`, including when the Provider selects only one attribute. The separate single-attribute template uses `odrl:hasPart` and a typed string value.

The current template fetches choices through the Provider T1 identity-attributes API, filtered for enabled attributes assignable to roles. That authoring source is separate from the Consumer's runtime `identity_attributes` IAM claim.

The local connector evaluates the permission during contract negotiation. It compares codes case-insensitively without trimming whitespace, requires every selected code and permits extra Consumer codes. Empty or malformed requirements and missing or malformed claims deny the constraint. The evaluator does not apply the browser source filters.

Trusted publication validation checks the completed policy and any final policy IDs and asset targets. Browser checks do not replace that validation. The local implementation does not establish production deployment of the authoring integration.

#### Supporting documentation

- R08 ODRL template technical description (internal page)
- R08 policy assignment and validation requirements (internal page)
- Shared policy-template API definition, SIMPL-31345 (internal page)

##### Policy wizard screenshots

These screenshots replace the missing mock-up. The access-policy dialog shows a single-attribute example. The policy-step overview shows where the added access policy appears.

- Add an access policy, single-attribute example (internal page)
- Policy step with an access policy added (internal page)

[Back to the list of user stories](#user-stories)

### User story 4: [UI] Provider selects an access policy template in the resource description wizard

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31405)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** select an access-policy template in the Resource Description wizard\
**So that** I can use a predefined template when adding an access policy

#### Acceptance criteria

##### Provider selects an access-policy template

1. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **And** I am at the "Access policies" step while creating a Resource Description\
   **When** the UI retrieves the access-policy templates through the shared policy-template API using the access-policy filter\
   **Then** the UI displays all predefined access-policy templates\
   **And** selecting a template stores it as the chosen template\
   **And** loads its details for the parameter form

##### Provider continues without selecting a template

2. **Given** one or more access-policy templates are available\
   **And** I have not selected a template\
   **When** I continue to the next step\
   **Then** the wizard allows me to continue\
   **And** no validation message is shown for the missing selection\
   **And** no access policy is added

##### No access-policy templates are available

3. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I reach the "Access policies" step\
   **And** the policy-template API returns an empty list for the access-policy filter\
   **Then** the UI shows that no templates are available\
   **And** I can continue without adding an access policy

##### Access-policy templates cannot be retrieved

4. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I reach the "Access policies" step\
   **And** the policy-template API returns an error\
   **Then** the UI shows an error and offers a retry\
   **And** no template can be selected until retrieval succeeds\
   **And** I can continue without adding an access policy

#### Technical details

- This story covers front-end behaviour only.
- Access and usage templates use the shared policy-template API defined in SIMPL-31345, with filtering by policy type.
- The selector displays the identifier and user-facing information returned by the API.
- The selected template identifier is stored in the wizard state and is used to retrieve the template details.
- Access policies are optional. An empty selection does not block navigation or final Resource Description submission.

#### Additional notes

- Shared API definition: SIMPL-31345 (internal page)
- Access-template details: SIMPL-31347 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)
- [UX/UI design](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI)

[Back to the list of user stories](#user-stories)

### User story 5: [UI] Provider fills in the access policy template parameters in the resource description wizard

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31406)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want** the Access policies step to show the fields of the selected template\
**So that** I can enter the values needed for an access policy

#### Acceptance criteria

##### Provider fills in an access-policy template

1. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **And** I selected an access-policy template in the Resource Description wizard\
   **When** the template details are loaded\
   **Then** the UI displays the fields, labels and guidance described by the template\
   **And** shows any access-enforcement information from the template as read-only\
   **And** stores the values I enter in the wizard state

##### Provider changes the selected template

2. **Given** I selected an access-policy template\
   **And** I entered one or more values\
   **When** I choose a different template\
   **Then** the UI asks me to confirm that the entered values will be discarded\
   **And** confirming the change clears those values and displays the new template fields\
   **And** cancelling the change keeps the current template and values

##### Template details cannot be retrieved

3. **Given** I selected an access-policy template\
   **When** the template details cannot be retrieved\
   **Then** the UI shows an error and offers a retry\
   **And** no parameter fields are displayed\
   **And** the policy cannot be added until the details are available

#### Technical details

- This story covers displaying template fields and keeping entered values in the wizard state.
- The form is built from the selected template details and must work with any predefined access-policy template returned by the API.
- The initial delivery provides two access-policy templates out of the box, but the UI must not hardcode them.
- SIMPL-31407 covers validation when the Provider confirms the policy.
- SIMPL-31433 covers creating the policy instance, attaching it to the wizard state and including it in the final Resource Description submission.

#### Additional notes

- Access-template details: SIMPL-31347 (internal page)
- Validation: SIMPL-31407 (internal page)
- Resource Description submission: SIMPL-31433 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)

[Back to the list of user stories](#user-stories)

### User story 6: [UI] Provider sees inline validation feedback for access policy template parameters

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31407)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want** clear validation feedback when I confirm an access policy\
**So that** I can correct invalid values before adding the policy

#### Acceptance criteria

##### Valid values pass validation

1. **Given** I completed the fields of an access-policy template with valid values\
   **When** I confirm the policy in the modal\
   **Then** the access-policy backend validates the policy\
   **And** no validation errors are shown\
   **And** the valid policy is added to the Resource Description

##### Invalid values block confirmation

2. **Given** one or more values do not meet the selected template's rules\
   **When** I confirm the policy in the modal\
   **Then** the access-policy backend validates the policy\
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
   **And** the valid policy is added to the Resource Description

#### Technical details

- The required validation trigger is confirmation of the policy in the modal.
- Earlier field feedback may also be shown, but it does not replace validation on confirmation.
- Validation uses the rules supplied by the selected template; the story works with any predefined access-policy template.
- SIMPL-31406 covers displaying the template fields and keeping entered values.
- This story covers validation messages and blocking confirmation when values are invalid.
- SIMPL-31433 covers adding a valid policy to the wizard and including it in the final Resource Description submission.

#### Additional notes

- Access-template details: SIMPL-31347 (internal page)
- Template fields: SIMPL-31406 (internal page)
- Resource Description submission: SIMPL-31433 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)

[Back to the list of user stories](#user-stories)

### User story 7: [UI] Provider adds an access policy to a new Resource Description

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31433)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want** an access policy created from a template to be added to the Resource Description I am creating\
**So that** the policy is included when I submit the Resource Description

#### Acceptance criteria

##### Provider adds an access policy to the Resource Description

1. **Given** I selected an access-policy template\
   **And** I entered the required values\
   **When** I confirm the policy in the modal\
   **Then** the access-policy backend validates the policy\
   **And** a valid policy is added to the current Resource Description in the wizard\
   **And** the Resource Description is not submitted by this action alone

##### Provider submits the Resource Description with an access policy

2. **Given** a valid access policy is attached to the Resource Description in the wizard\
   **When** I submit the Resource Description\
   **Then** the access policy is included in the same Resource Description payload

##### Policy validation fails

3. **Given** I entered invalid policy values\
   **When** I confirm the policy in the modal\
   **Then** the policy is not added to the Resource Description\
   **And** the modal remains open with validation feedback

##### Resource Description submission fails

4. **Given** a valid access policy is attached to the Resource Description in the wizard\
   **When** the Resource Description submission fails\
   **Then** the UI shows the error\
   **And** keeps the wizard data and access policy so I can retry

#### Technical details

- Confirming the policy and submitting the Resource Description are separate actions.
- Confirming a valid policy adds it to the current wizard state.
- Final submission sends the policy in the same Resource Description payload.
- No separate policy persistence endpoint is introduced.
- The exact Resource Description API operation will be linked when the implementation artifact is available.
- This flow works with any predefined access-policy template returned by the policy-template API.

#### Additional notes

- Access-policy validation: SIMPL-31407 (internal page)
- Resource Description API: SIMPL-25712 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)
- [UX/UI design](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI)

[Back to the list of user stories](#user-stories)

### User story 8: [UI] Provider removes an access policy from the new resource description

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31703)

#### Storycard:

**As a** Provider with a role authorising the management of resource descriptions\
**I want to** remove an access policy from the new resource description at the "Access policies" step\
**So that** the new resource description only carries the access policies I want to keep

#### Acceptance criteria:

**Scenario:** Provider removes an access policy from the new resource description

1. **Given** I am authenticated in the Provider UI with a role authorising the management of resource descriptions\
   **And** I am creating a new resource description and the "Access policies" step lists one or more access policies added from a template\
   **When** I remove one of the listed access policies\
   **Then** the UI removes that access policy from the set to be saved on the new resource description\
   **And** the remaining access policies stay unchanged and available for editing

##### Negative scenario

**Scenario:** No access policies are present to remove

2. **Given** I am authenticated in the Provider UI with a role authorising the management of resource descriptions\
   **And** I am creating a new resource description and the "Access policies" step has no assigned access policies\
   **When** I view the list of access policies\
   **Then** the remove action is not available\
   **And** the step shows an empty state for access policies

#### Technical details:

- Removal operates on the access policies held in the wizard state; the change is persisted only when the new resource description is submitted.
- A removed access policy is excluded from the assembled set sent to the existing resource description API on submission.

API definition ticket: (internal page)

#### Additional notes:

- (internal page)
- (internal page)

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
