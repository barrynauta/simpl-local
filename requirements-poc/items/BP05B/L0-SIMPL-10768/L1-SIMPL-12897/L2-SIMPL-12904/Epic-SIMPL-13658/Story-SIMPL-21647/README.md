> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12904](../../README.md) / [Epic-SIMPL-13658](../README.md) / **Story-SIMPL-21647**

# SIMPL-21647: [API] A provider consults the version history of a resource description via API (SD tooling)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12904](../../README.md) (L2) › [SIMPL-13658](../README.md) (Epic) › **SIMPL-21647** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via API\
**So that** I can review it and decide if any action should be taken.

#### Technical Details

From a technical perspective, the **sd-tooling-be** component will invoke the following API exposed by the query-mapper-adapter:\
`{}/participants/resourceDescriptions/{resourceDescriptionId}/version{`},\
which is defined in the following OpenAPI specification file:\
[https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/poc-gaia-edc/-/blob/main/openapi/adapter_openapi.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/poc-gaia-edc/-/blob/main/openapi/adapter_openapi.yaml?ref_type=heads)

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - positive case

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions\
   **Then** the version history of this resource description is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: not own resource description

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of a resource description not published by me\
   **Then** an error message is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: resource description not existing

3. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of a resource description that does not exist\
   **Then** an error message is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: bad request

4. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions\
   **And** I send a malformed request\
   **Then** an error message is returned.

**Scenario:** A Provider consults the version history of one of its own resource descriptions via API - negative case: internal server error

5. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions\
   **And** there is an internal server error\
   **Then** an error message is returned.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-13658](../README.md) (Epic) A Provider consults the version history of one of its own resource descriptions |
