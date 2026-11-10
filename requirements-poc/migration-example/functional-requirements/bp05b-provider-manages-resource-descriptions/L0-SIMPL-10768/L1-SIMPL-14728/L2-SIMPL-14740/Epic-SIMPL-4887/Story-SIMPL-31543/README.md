> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14740](../../README.md) / [Epic-SIMPL-4887](../README.md) / **Story-SIMPL-31543**

# SIMPL-31543: [UI] Provider sees the access policies carried over from the previous resource description version

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14740](../../README.md) (L2) › [SIMPL-4887](../README.md) (Epic) › **SIMPL-31543** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-4887](../README.md) (Epic) A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI |
