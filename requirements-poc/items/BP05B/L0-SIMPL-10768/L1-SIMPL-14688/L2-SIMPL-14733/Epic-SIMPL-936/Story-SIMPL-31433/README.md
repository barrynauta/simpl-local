> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-31433**

# SIMPL-31433: [UI] Provider adds an access policy to a new Resource Description

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-31433** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
