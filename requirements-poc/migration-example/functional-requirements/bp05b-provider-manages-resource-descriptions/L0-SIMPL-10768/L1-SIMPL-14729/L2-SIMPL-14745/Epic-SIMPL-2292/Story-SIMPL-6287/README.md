> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-6287**

# SIMPL-6287: Integrating the asset registering of the connector in to the published process of SD

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-6287** (User story)

## Text

**As a** data provider\
**I want to** during the process of publishing a resource to the catalogue, use an API

- to register the asset and policies (related to this resource) in the -EDC- connector
- Once registered, to retrieve the asset ID and insert that asset ID as an extra field in the resource description

**So that** the asset ID is available to support the consumption of the resource when a consumer selects the resource description for consumption

Note: for MVP, the connector is EDC.

-As a data provider-

-I want to register asset and policy to the EDC connector via API-

-Once registered we want to retrieve the asset ID-

-Insert the asset ID to the creation of the extra field-

-AC:-

AC:

S1. Register a data asset in the connector of the provider with the correct properties:

1. **Given** the user is logged in as a Data Provider, connector is running and necessary permissions are granted,\
   **When** make a POST request to [/api/sd/enrichAndValidate](https://creation-wizard-api.dev.simpl-europe.eu/swagger-ui/index.html#/SD%20Controller/enrichAndValidate) with a valid asset definition including SD json file,\
   **Then** the asset is successfully registered, and the correct properties are set, as well the asset ID is correctly extracted.

S2. Verify that the returned asset ID is correctly inserted into the resource description:

2. **Given** Asset registration is successful, and asset ID is received\
   **When** receiving the asset ID from the registration API response and verify the insertion of the asset ID into the resource description.\
   **Then** the resource description contains the asset ID in the specified field.

S3.Register a data asset in the connector of the provider with missing properties

3. **Given** the user is logged in as a Data Provider, connector is running and necessary permissions are granted,\
   **When** initiate the resource publishing process yet missing one or more required resource attributes.\
   **Then** 1) the API returns an error status (e.g., HTTP 400). 2) the system prompts user with messages indicating which required fields are missing.

S4. Register a data asset in the connector of the provider yet the data asset is already existing

4. **Given** the user is logged in as a Data Provider, connector is running and necessary permissions are granted,\
   **When** initiate the resource publishing process of a existing data asset\
   **Then** 1) The API returns an error status (e.g., HTTP 409). 2) Appropriate error message indicating that the asset already exists is returned.

S5. Verify that the resource can be consumed using the asset ID in the description.

5. **Given** Asset registration is successful, and asset ID is part of the resource description, and the User has accessed the resource catalog as a consumer, and had selected the resource that contains the asset ID.\
   **When** Initiate the resource consumption process using the asset ID.\
   **Then** 1) Resource consumption is successful with no errors. 2) The system retrieves the resource correctly based on the asset ID.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
