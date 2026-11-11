> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14730](../../README.md) / [Epic-SIMPL-2285](../README.md) / **Story-SIMPL-15867**

# SIMPL-15867: [UI] A provider consults a list of published resource types in the resource description manager UI

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14730](../../README.md) (L2) › [SIMPL-2285](../README.md) (Epic) › **SIMPL-15867** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** be able to consult the published resource type in the UI\
**So that** I can select one to start the creation of a new resource description for a specific resource type.

#### Acceptance criteria

**Scenario:** Consulting the list of published resource types - positive case

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I open the resource description management page\
   **Then** the list of active resource types available from the Governance Authority is shown.

**Scenario:** Consulting the list of published resource types - API error

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I open the resource description management page\
   **And** the API to retrieve the list of resource types throws an error\
   **Then** an empty list of resource types for creating a new resource description is shown\
   **And** the creation of a new resource description is not possible\
   **And** a notification is shown that the resource types could not be retrieved and resource description is not possible.

**Scenario:** Consulting the list of published resource types - not authenticated

3. **Given** I am not authenticated as Provider\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

**Scenario:** Consulting the list of published resource types - no role

4. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of Resource Descriptions\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

#### Mockups:

| Mockup 1 | Mockup 2 |
| --- | --- |
| !RDM_with_dropdown_closed.png | thumbnail! | !RDM_with_dropdown-open.png | thumbnail! |

#### Technical info:

API call is to SD tooling BE.

API definition ticket (internal page).

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2285](../README.md) (Epic) A Provider retrieves from the Governance Authority a list of published resource types |
