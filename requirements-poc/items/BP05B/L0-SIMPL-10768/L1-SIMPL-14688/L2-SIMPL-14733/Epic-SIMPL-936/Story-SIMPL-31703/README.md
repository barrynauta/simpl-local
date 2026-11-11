> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-31703**

# SIMPL-31703: [UI] Provider removes an access policy from the new resource description

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-31703** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
