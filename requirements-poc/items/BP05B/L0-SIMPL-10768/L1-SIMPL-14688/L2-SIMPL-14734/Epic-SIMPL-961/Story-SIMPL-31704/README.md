> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-961](../README.md) / **Story-SIMPL-31704**

# SIMPL-31704: [UI] Provider removes a usage policy from the new resource description

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-961](../README.md) (Epic) › **SIMPL-31704** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-961](../README.md) (Epic) A Provider assigns usage policies to a new resource description by using policy template via UI |
