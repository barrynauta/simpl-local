> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14740](../../README.md) / [Epic-SIMPL-4887](../README.md) / **Story-SIMPL-31546**

# SIMPL-31546: [UI] Provider removes an access policy from the new resource description version

**User story (implementable work)** · Development: **In development**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14740](../../README.md) (L2) › [SIMPL-4887](../README.md) (Epic) › **SIMPL-31546** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-4887](../README.md) (Epic) A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI |
