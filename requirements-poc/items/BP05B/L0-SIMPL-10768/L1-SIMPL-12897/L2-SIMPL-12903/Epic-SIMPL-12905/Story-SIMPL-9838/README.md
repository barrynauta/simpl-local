> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12903](../../README.md) / [Epic-SIMPL-12905](../README.md) / **Story-SIMPL-9838**

# SIMPL-9838: Display self-description details

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12903](../../README.md) (L2) › [SIMPL-12905](../README.md) (Epic) › **SIMPL-9838** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** be able to see the details of one of my self-descriptions\
**So that** I can inform myself about the details and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** A provider consults the details of one of its own Resource descriptions in the UI - positive case

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am consulting the overview of Resource descriptions\
   **And** there is a Resource description published by me\
   **When** I open the resource description details page\
   **Then** all details of the selected Resource description are shown.

**Scenario:** A provider consults the details of one of its own Resource descriptions in the UI - negative case

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am consulting the overview of Resource descriptions\
   **And** there is a Resource description published by me\
   **When** I open the Resource description details page\
   **And** the API returns an error\
   **Then** an error message showing the error description is shown.

*Note: as this screen can only be reached if there exists at least one Resource description of the Provider, a negative case for "no Resource description" does not exist.*

**Scenario:** A user is not authenticated as Provider

**When** I am not autenticated as Provider\
   **Then** I cannot access Resource description management in the UI.

**Scenario:** A user does not have role authorising the management of Resource Descriptions

3. **Given** I am autenticated as Provider\
   **When** I do not have a role authorising the management of Resource Descriptions\
   **Then** I cannot access Resource description management in the UI.

#### Technical Info

The details of the selfdestruction should be based on the schema (labels of the fields, which fields are shown and such).

UI Mockup:

(image not shown)

#### Additional Notes

API implementation story: (internal page)

UI for the list of Resource descriptions: (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12905](../README.md) (Epic) A Provider consults the full contents of one of its own Resource descriptions via UI and API |
