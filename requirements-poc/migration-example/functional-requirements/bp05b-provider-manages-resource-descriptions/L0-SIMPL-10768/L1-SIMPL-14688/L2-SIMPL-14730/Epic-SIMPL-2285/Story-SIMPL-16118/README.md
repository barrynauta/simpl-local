> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14730](../../README.md) / [Epic-SIMPL-2285](../README.md) / **Story-SIMPL-16118**

# SIMPL-16118: [API] A provider resource description manager retrieves a list of active schemas via API for the UI

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14730](../../README.md) (L2) › [SIMPL-2285](../README.md) (Epic) › **SIMPL-16118** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** be able to retrieve a list of resource types (list of active schemas) via the SD Tooling API from the Schema Storage\
**So that** I can select a schema for the creation of a new Resource Description.

#### Acceptance criteria

**Scenario:** A Provider requests a list of resource types - positive case

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request a list of active resource types from the API\
   **Then** a list resource types is returned

**Scenario:** A Provider requests a list of resource types - wrong role

2. **Given** I am authenticated as a Provider\
   **And** I do not have a role authorising the management of resource descriptions\
   **When** I request a list of active resource types from the API\
   **Then** a message is returned that I am not authorised.

**Scenario:** A Provider requests a list of resource types - internal server error

3. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request a list of active resource types from the API\
   **And** there is an internal error\
   **Then** a message is returned that an internal error occured, including a suitable error code.

#### Technical details

Extract the metadata for a resource description from the .ttl and return it to the FE from the API.

#### Additional notes

- API definition ticket: (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2285](../README.md) (Epic) A Provider retrieves from the Governance Authority a list of published resource types |
