> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14747](../../README.md) / [Epic-SIMPL-18402](../README.md) / **Story-SIMPL-31674**

# SIMPL-31674: [UI] Provider sees the policy syntax validation outcome in the RD editor for new RD version

**User story (implementable work)** · Development: **In development**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14747](../../README.md) (L2) › [SIMPL-18402](../README.md) (Epic) › **SIMPL-31674** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18402](../README.md) (Epic) The Governance Authority validates the syntax of the policies assigned to a new Resource description or a new version of a Resource Description |
