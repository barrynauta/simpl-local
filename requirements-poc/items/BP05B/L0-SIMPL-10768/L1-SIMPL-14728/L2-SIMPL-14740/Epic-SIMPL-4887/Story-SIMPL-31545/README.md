> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14740](../../README.md) / [Epic-SIMPL-4887](../README.md) / **Story-SIMPL-31545**

# SIMPL-31545: [UI] Provider fills in the access policy template parameters in the new resource description version wizard

**User story (implementable work)** · Development: **In development**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14740](../../README.md) (L2) › [SIMPL-4887](../README.md) (Epic) › **SIMPL-31545** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-4887](../README.md) (Epic) A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI |
