> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12898](../../README.md) / [Epic-SIMPL-12899](../README.md) / **Story-SIMPL-15584**

# SIMPL-15584: [API] API to provide a list of a provider's own resource descriptions (SD tooling)

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12898](../../README.md) (L2) › [SIMPL-12899](../README.md) (Epic) › **SIMPL-15584** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** provide via an API a sorted (descendingly by publication date or alphabetically by Resource type) list of the resource descriptions summaries which I have published\
**So that** so that I can see a list of my Resource Descriptions and decide on any action to take on them

#### Acceptance criteria

**Positive case - default sorting**

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by me\
   **When** I requests my own resource descriptions via API\
   **Then** the list of my previously published resource descriptions is returned, sorted by publication date in descending order (default).

**Positive case - custom sorting**

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by me\
   **When** I request my own resource descriptions via the API\
   **And** include a valid `orderBy` query parameter\
   **Then** the list of my resource descriptions is returned, sorted according to the specified field.

**No resources published**

3. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are no resource descriptions published by me\
   **When** I request the my own resource descriptions via API\
   **Then** an empty list is returned.

**Not authorised**

4. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of Resource Descriptions\
   **When** I requests my own resource descriptions via API\
   **Then** a message is returned that I am not authorised.

**Not acceptable**

5. **Given** I am authenticated as Provider\
   **And** I request my own resource descriptions via API\
   **When** I specify a media type that is not supported\
   **Then** a 406 Not Acceptable error is returned.

#### Technical Information

The API will be exposed by the SD Tooling BE, for which the API definition is done in [https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-api-be/-/blob/feature/SIMPL-14757/openapi/openapi-v2.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-api-be/-/blob/feature/SIMPL-14757/openapi/openapi-v2.yaml?ref_type=heads) ). The API will be accessible externally via the Tier-1 Gateway, making it usable by third-party systems and the UI.

The data will be retrieved by the SD Tooling BE from the Query Mapper Adapter (at the GA side).

#### Additional notes

SD Tooling BE API definition ticket: (internal page)

UI implementation ticket: (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12899](../README.md) (Epic) A Provider consults an overview of its Resource descriptions via UI and API |
