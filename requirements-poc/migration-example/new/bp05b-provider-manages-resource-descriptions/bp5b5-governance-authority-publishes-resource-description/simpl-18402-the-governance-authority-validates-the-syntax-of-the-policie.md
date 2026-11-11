<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority validates the syntax of the policies assigned to a new Resource description or a new version of a Resource Description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog it is item SIMPL-18402, status **In Progress**. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Governance Authority\
**I want to** automatically validate the syntax of policies in a new resource description or a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

**Acceptance Criterion 1: Syntax validation access policies**

**Scenario:** The Governance Authority validates the syntax of the access policies assigned to a new resource description or a new version of a resource description\
When I receive a new resource description or a new version of a resource description\
and the sender has the identity attributes to manage resource descriptions\
then the syntax of the access policies of the resource description is automatically validated, which includes verifying the correct usage of action verbs and ODRL constraints.

**Acceptance Criterion 2: Syntax validation usage policies**

**Scenario:** The Governance Authority validates the syntax of the usage policies assigned to a new resource description or a new version of a resource description\
When I receive a new resource description or a new version of a resource description\
and the sender has the identity attributes to manage resource descriptions\
then the syntax of the usage policies of the resource description is automatically validated.

## User stories

1. [[API] GA validates the syntax of access and usage policies of new RD version](#user-story-1-api-ga-validates-the-syntax-of-access-and-usage-policies-of-new-rd-version) (FOR SC TO RESPOND)
2. [[UI] Provider sees the policy syntax validation outcome in the RD editor for new RD version](#user-story-2-ui-provider-sees-the-policy-syntax-validation-outcome-in-the-rd-editor-for-new-rd-version) (PBI INTERNAL QA)

### User story 1: [API] GA validates the syntax of access and usage policies of new RD version

**Status:** FOR SC TO RESPOND (backlog item SIMPL-31673)

#### Storycard

**As** Governance Authority,\
**I want** Simpl-Open to check the access and usage policies when a Provider submits a new resource-description version,\
**So that** versions with invalid policies are not published and the Provider can correct the errors.

#### Acceptance criteria

##### All policies are valid

1. **Given** an authorised Provider submits a new version of a resource description\
   **And** its access and usage policies meet the supported policy rules\
   **When** Simpl-Open checks those policies\
   **Then** the Provider receives a successful policy-validation result\
   **And** the new version can continue through the remaining publication checks

##### An access policy is invalid

2. **Given** an authorised Provider submits a new resource-description version\
   **And** one of its access policies contains an invalid requirement\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying the access policy and the requirement to correct\
   **And** the new version is not published

##### A usage policy is invalid

3. **Given** an authorised Provider submits a new resource-description version\
   **And** one of its usage policies contains an invalid condition or obligation\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying the usage policy and the condition or obligation to correct\
   **And** the new version is not published

##### A policy uses an unsupported rule

4. **Given** an authorised Provider submits a new resource-description version\
   **And** a policy uses a rule that Simpl-Open does not support\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying the policy and the unsupported rule\
   **And** the new version is not published

##### Access and usage policies are both invalid

5. **Given** an authorised Provider submits a new resource-description version\
   **And** it contains an invalid access policy and an invalid usage policy\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying both policies and their errors\
   **And** the new version is not published

##### The submitted policies cannot be read

6. **Given** an authorised Provider submits a new resource-description version\
   **And** the submitted policies cannot be read\
   **When** Simpl-Open processes the submission\
   **Then** the Provider receives an error explaining that the submitted policies could not be read\
   **And** the new version is not published

##### The sender is not authorised

7. **Given** the sender is not authorised to manage resource descriptions\
   **When** the sender submits a new resource-description version\
   **Then** the submission is refused\
   **And** the new version is not published

#### Technical details

Run trusted validation on the final policy content before publication, after any policy IDs and asset targets have been assigned. Validate the ODRL structure, vocabulary, operand/operator combinations, value types and applicable profile rules. Template startup checks and browser checks do not replace this step. New resource descriptions need equivalent validation, with the owning story still to be confirmed.

Access and usage policies are checked in the same validation process. SIMPL-31639 defines the response contract and SIMPL-31640 covers detailed feedback. Return HTTP 400 for malformed submissions and HTTP 422 for readable submissions containing invalid policies. Feedback identifies the affected policies and the corrections needed, including both policy categories when both are invalid.

The new local connector functions implement the following access-policy combinations. These provide concrete compatibility examples for the publication-validation profile.

| Action and rule | Expanded left operand | Operator | Authored right operand | Runtime scope |
| --- | --- | --- | --- | --- |
| `odrl:use` permission | `https://ec.europa.eu/simpl/profile#identityAttributes` | `odrl:hasPart` | One typed string code | `contract.negotiation` |
| `odrl:use` permission | `https://ec.europa.eu/simpl/profile#identityAttributes` | `odrl:isAllOf` | A nonempty JSON-LD list of string codes | `contract.negotiation` |
| `odrl:use` permission | `https://ec.europa.eu/simpl/profile#evaluationTime` | `odrl:lt` | A typed `xsd:dateTime` literal with UTC or an explicit offset | `contract.negotiation`, `transfer.process` |

The action in the access-policy examples is `odrl:use`, expanded as `http://www.w3.org/ns/odrl/2/use`. The usage examples include `odrl:use` for a usage term and `odrl:delete` for a deletion obligation. These identify the actions in the examples; they do not require further actions or define the entire approved validation profile.

Invalid compatibility examples include an empty attribute list, a blank or non-string code, `odrl:eq` for the new `identityAttributes` operand, `odrl:lteq` for `evaluationTime`, an invalid calendar date and a deadline without a timezone offset. The legacy `edc:consumption` function has a separate operator contract.

EDC unwraps the time literal to a string, so the runtime evaluator cannot check the original JSON-LD datatype. Publication validation must inspect the authored representation. Runtime time evaluation permits access strictly before the deadline and denies at equality or later. It does not implement an HLF4 monitoring obligation.

A sender without the required management identity attributes receives HTTP 403, as in the existing authorisation scenario.

#### Supporting documentation

- R08 ODRL template technical description (internal page)
- R08 policy assignment and validation requirements (internal page)
- Policy validation response contract, SIMPL-31639 (internal page)

### User story 2: [UI] Provider sees the policy syntax validation outcome in the RD editor for new RD version

**Status:** PBI INTERNAL QA (backlog item SIMPL-31674)

#### Storycard

**As a** Provider preparing a new version of a resource description,\
**I want** the editor to check each access or usage policy when I add or change it,\
**So that** I can fix errors before submitting the full version.

#### Acceptance criteria

##### Add or change a valid policy

1. **Given** I have entered the values for an access or usage policy\
   **When** I choose "Add policy" or "Save changes"\
   **And** the backend accepts the policy\
   **Then** the editor adds or updates it in the current RD form\
   **And** I can see the policy in the list\
   **And** this action does not submit the RD version

##### Show errors and block submission

2. **Given** I have entered an invalid access or usage policy\
   **When** I choose "Add policy" or "Save changes"\
   **And** the backend returns validation errors\
   **Then** the editor keeps the policy form open with my values\
   **And** it shows which field or rule needs correction and what is wrong\
   **And** it does not add or update the invalid policy\
   **And** I cannot submit the RD version while these errors remain unresolved

##### Identify the affected part

3. **Given** a policy contains several rules or constraints\
   **When** the backend returns a validation error\
   **Then** the editor identifies whether it concerns the access or usage policy\
   **And** it identifies the affected policy entry by its displayed name where one is available\
   **And** it names the field, rule or constraint that needs correction when the backend provides that detail\
   **And** it shows a clear message that tells me what to change

##### The backend check is unavailable

4. **Given** I am adding or changing a policy\
   **When** the backend check is unavailable\
   **Then** the editor shows that it could not check the policy\
   **And** it keeps the form and entered values editable\
   **And** it offers me a way to retry "Add policy" or "Save changes"\
   **And** it does not treat the policy as valid or let me submit it with unresolved errors

##### Correct an error

5. **Given** the editor showed a policy error\
   **When** I correct the values and choose "Add policy" or "Save changes" again\
   **And** the backend accepts the policy\
   **Then** the editor updates the policy entry\
   **And** it clears the errors for that entry\
   **And** I can continue preparing the RD version

#### Technical details

The editor calls the existing Policy Manager backend when the Provider chooses "Add policy" or "Save changes". The backend checks the entered values and completed policy. A successful result adds or updates the policy in the current editor state. It does not save or publish the RD version. The UI should show field errors beside the matching input and other policy errors in the same error area. It need not navigate automatically to a field when the Provider clicks an error.

An RD holds one access-policy document and one usage-policy document. Each document may contain several rules or constraints, including more than one constraint concerning the same operand. The editor can also show separate entries before it combines them into those documents. A message should identify the access or usage document, the displayed entry name when available, and the affected field, rule or constraint when the backend can locate it. For an error in the whole policy, show a whole-policy message instead of inventing a field location.

A successful "Add policy" or "Save changes" action is the visible confirmation that the policy passed this check. The policy appears in the list. There is no separate "Validate" button or separate success claim for the full RD. The existing RD submission remains blocked while a policy has unresolved errors. Submission has its own final checks for retained policies and direct API submissions under SIMPL-31640 and SIMPL-31673. This editor check does not replace them.

A failed check leaves the current form editable. Retry repeats the same add or save action after a known check failure. It does not replay an RD submission. This story does not add a server-side draft; the current editor state remains available only for the current session under the existing behaviour.

Related work: SIMPL-31639 (internal page) documents the current API; SIMPL-31640 (internal page) and SIMPL-31673 (internal page) cover final policy checks at publication; SIMPL-31641 (internal page) is the duplicate whose detailed-error requirements are included here.

---

Back to [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
