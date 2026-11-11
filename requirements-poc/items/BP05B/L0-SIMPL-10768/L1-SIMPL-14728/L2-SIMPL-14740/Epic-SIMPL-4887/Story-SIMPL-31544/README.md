> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14740](../../README.md) / [Epic-SIMPL-4887](../README.md) / **Story-SIMPL-31544**

# SIMPL-31544: [UI] Provider selects an access policy template in the new resource description version wizard

**User story (implementable work)** · Development: **In development**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14740](../../README.md) (L2) › [SIMPL-4887](../README.md) (Epic) › **SIMPL-31544** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-4887](../README.md) (Epic) A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI |
