> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-936](../README.md) / **Story-SIMPL-31405**

# SIMPL-31405: [UI] Provider selects an access policy template in the resource description wizard

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-936](../README.md) (Epic) › **SIMPL-31405** (User story)

## Text

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** select an access-policy template in the Resource Description wizard\
**So that** I can use a predefined template when adding an access policy

#### Acceptance criteria

##### Provider selects an access-policy template

1. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **And** I am at the "Access policies" step while creating a Resource Description\
   **When** the UI retrieves the access-policy templates through the shared policy-template API using the access-policy filter\
   **Then** the UI displays all predefined access-policy templates\
   **And** selecting a template stores it as the chosen template\
   **And** loads its details for the parameter form

##### Provider continues without selecting a template

2. **Given** one or more access-policy templates are available\
   **And** I have not selected a template\
   **When** I continue to the next step\
   **Then** the wizard allows me to continue\
   **And** no validation message is shown for the missing selection\
   **And** no access policy is added

##### No access-policy templates are available

3. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I reach the "Access policies" step\
   **And** the policy-template API returns an empty list for the access-policy filter\
   **Then** the UI shows that no templates are available\
   **And** I can continue without adding an access policy

##### Access-policy templates cannot be retrieved

4. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I reach the "Access policies" step\
   **And** the policy-template API returns an error\
   **Then** the UI shows an error and offers a retry\
   **And** no template can be selected until retrieval succeeds\
   **And** I can continue without adding an access policy

#### Technical details

- This story covers front-end behaviour only.
- Access and usage templates use the shared policy-template API defined in SIMPL-31345, with filtering by policy type.
- The selector displays the identifier and user-facing information returned by the API.
- The selected template identifier is stored in the wizard state and is used to retrieve the template details.
- Access policies are optional. An empty selection does not block navigation or final Resource Description submission.

#### Additional notes

- Shared API definition: SIMPL-31345 (internal page)
- Access-template details: SIMPL-31347 (internal page)
- Policies (internal page)
- Resource Description UI (internal page)
- [UX/UI design](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-936](../README.md) (Epic) A Provider assigns access policies to a new resource description by using policy template via UI |
