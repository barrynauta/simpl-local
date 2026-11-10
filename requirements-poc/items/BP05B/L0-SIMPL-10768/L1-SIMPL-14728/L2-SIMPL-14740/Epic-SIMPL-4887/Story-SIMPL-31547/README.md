> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14740](../../README.md) / [Epic-SIMPL-4887](../README.md) / **Story-SIMPL-31547**

# SIMPL-31547: [UI] Provider saves the updated access policies on the new resource description version

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14740](../../README.md) (L2) › [SIMPL-4887](../README.md) (Epic) › **SIMPL-31547** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-4887](../README.md) (Epic) A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI |
