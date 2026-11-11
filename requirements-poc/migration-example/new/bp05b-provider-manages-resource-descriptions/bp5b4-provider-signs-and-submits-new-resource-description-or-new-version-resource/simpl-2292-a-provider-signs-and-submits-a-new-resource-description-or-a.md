<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.4 - A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md) / [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md#5b41-a-provider-signs-and-submits-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider signs and submits a new Resource description or a new version of a Resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md#5b41-a-provider-signs-and-submits-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.4 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md). In the process it belongs to step [BP05B.04 Sign and submit the resource description](../README.md#bp05b04-sign-and-submit-the-resource-description).

In the backlog it is item SIMPL-2292, status **Defined**. It has 12 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** sign and submit a new resource description or a new version of a resource description to the Governance Authority using an API\
**So that** the resource description can be published in the Catalogue .

#### Acceptance criteria

**Scenario:** A Provider signs and submits a new Resource description or a new version of a Resource description

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I have finished editing or updating a new Resource description or a new version of a Resource description\
   **When** I request the publication of the new Resource description or new version of a Resource description\
   **Then** the new Resource description or new version of the Resource description is validated\
   **And** the elements of the resource description, required to later support consumption of the resource, are stored in my agent\
   **And** the new Resource description or new version of the Resource description is signed\
   **And** the signed new Resource description or new version of the Resource description is submitted to the Governance Authority for publication.

## User stories

1. [Implement credential management logic](#user-story-1-implement-credential-management-logic) (DevTeam PBI is ready for Implementation check)
2. [Sign created json-ld document with a signer](#user-story-2-sign-created-json-ld-document-with-a-signer) (Ready for DG Acceptance)
3. [[UI] Create UI frontend for XFSC catalogue - Dialog add resource](#user-story-3-ui-create-ui-frontend-for-xfsc-catalogue---dialog-add-resource) (DevTeam PBI is ready for Implementation check)
4. [Interactively Create a Service Self-Description](#user-story-4-interactively-create-a-service-self-description) (Ready for DG Acceptance)
5. [Deploy signer service](#user-story-5-deploy-signer-service) (DevTeam PBI is ready for Implementation check)
6. [[UI] User Interface for Sign created JSON-LD document](#user-story-6-ui-user-interface-for-sign-created-json-ld-document) (DevTeam PBI is ready for Implementation check)
7. [Syntax Validation of Self-Description](#user-story-7-syntax-validation-of-self-description) (Ready for DG Acceptance)
8. [Simpl Self-Description Frontend](#user-story-8-simpl-self-description-frontend) (Ready for DG Acceptance)
9. [Integrating the asset registering of the connector in to the published process of SD](#user-story-9-integrating-the-asset-registering-of-the-connector-in-to-the-published-process-of-sd) (Ready for DG Acceptance)
10. [Automatically populate the provider id on the resource description](#user-story-10-automatically-populate-the-provider-id-on-the-resource-description) (DevTeam PBI is ready for Implementation check)
11. [[BE] Automatically populate the version number of a new resource description](#user-story-11-be-automatically-populate-the-version-number-of-a-new-resource-description) (Ready for DG Acceptance)
12. [[BE] Automatically populate the version number of a new version of a resource description](#user-story-12-be-automatically-populate-the-version-number-of-a-new-version-of-a-resource-description) (Ready for DG Acceptance)

### User story 1: Implement credential management logic

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-110)

Implement Logic (see drawing) into newly created repository.

(image not shown)

OCM service was substituted by Signer service ([https://gitlab.eclipse.org/vkalashn/signer/-/tree/local-version?ref_type=heads](https://gitlab.eclipse.org/vkalashn/signer/-/tree/local-version?ref_type=heads))

[Back to the list of user stories](#user-stories)

### User story 2: Sign created json-ld document with a signer

**Status:** Ready for DG Acceptance (backlog item SIMPL-226)

'+**As a**+ service provider **I want** to sign my self declaration **so that** I can store them as VC in a wallet (OCM/PCM)

**Linked Sovereign X US ID:** TAE_US_034

+**Technical specification:**+

Sign created json-ld document with a signer (GX-lab signer, Notarization Service from XFSC or simply by using Javascript-code)

Remark: Use DID:WEB for the decentralized Identifier.

an internal page

+**Acceptance Criteria:**+

**1. the created json-ld document from SD-Tooling is signed**

**2. the signed json-ld document can be stored in a vault**

**3. the signed json-ld document can be viewed in vault**

[Back to the list of user stories](#user-stories)

### User story 3: [UI] Create UI frontend for XFSC catalogue - Dialog add resource

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-228)

'**As a** service provider **I want** publish a SD of a service resource **so that** my service offering can be looked up and queried by Consumers.

**Linked Sovereign X US ID:** TAE_US_035, TAE_US_036, TAE_US_037, TAE_US_038

+**Technical specification:**+

Create UI frontend for XFSC catalogue to store and publish a SD in a catalogue.\
The UI should be able to import an sign JSON-LD document and with a button publish it to the catalog. The result of the publication (either success or in case of failure a message with the reason) should be shown to the provider. The front end also contains the feature to log in which is required to get the token to use the catalog backend.

+**Mock Up:**+\
mock up docu (internal page)\
(image not shown)

+**Acceptance Criteria:**+

**1. A service provider can store/publish a self description  of a service offering**

**2. Three different types of Service Offerings can be published (APP, Data and Infra)**

**3. Invalid SD are rejected**

**4. The Federated Catalogue accepts theses created SD**

[Back to the list of user stories](#user-stories)

### User story 4: Interactively Create a Service Self-Description

**Status:** Ready for DG Acceptance (backlog item SIMPL-233)

**As a** service provider\
**I want** interactively fill a self-description for my dataset in the UI\
**So that** I can ensuring compliance with the required structure and metadata.

**Linked Sovereign X US ID:** TAE_US_029, TAE_US_032, TAE_US_033

**Technical** Specification:

The UI Frontend for the SD-Tool should support the user in specifying the self-description. Depending on the allowed data type the SD-Tool should support appropriate input methods, for instance a date picker for a date attribute.

**Acceptance Criteria:**

1. **A service provider can only input numbers if the field is numerical.**
1. -**A service provider can use a date picker for fields that contain date information.**-
1. **Supports DATA, -APP, Infrastructure-**
1. -**A service provider is provided feedback if he reaches the maximum length for a text field.**-
1. **It is possible to use tab to jump to the next input field.**

[Back to the list of user stories](#user-stories)

### User story 5: Deploy signer service

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-241)

Create the helm charts for the deployment of the Gaia-X XSFC TSA signer service. Deploy the helm chart in the development kubernates cluster. Set up the token to sign SD

`AC:`

- The Signer application is deployed on the development K8 cluster
- The deployed Signer is accessible at the designated URL [https://signer.dev.simpl-europe.eu/signer/](https://signer.dev.simpl-europe.eu/signer/liveness)

- Core features of the Signer are functional, including:
   - Signing of SDs

[Back to the list of user stories](#user-stories)

### User story 6: [UI] User Interface for Sign created JSON-LD document

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-314)

'+**As a**+ service provider **I want** to use an UI to sign my self declaration **so that** I can store them as VC in a wallet (OCM/PCM)

**Linked Sovereign X US ID:** TAE_US_034

+**Technical specification:**+

The UI Frontend for the Signing should allow to upload or insert a JSON-LD Self-Description (generated by the SD-Tooling). With a button the Sign-Service is called with the SD. After a successful sign of the document the now sign JSON-LD can be exported for the later publish to the catalog.

+**Mock Up:**+\
mock up docu (internal page)

(image not shown)

+**Acceptance Criteria:**+

**1. It is possible to import and paste the JSON-LD Self-Description**

**2. The created json-ld document from SD-Tooling is signed**

**3. In case of issues with the signing process the error is shown to the provider**

**4. the signed json-ld document can exported from the UI**

[Back to the list of user stories](#user-stories)

### User story 7: Syntax Validation of Self-Description

**Status:** Ready for DG Acceptance (backlog item SIMPL-489)

**As a** Provider Agent\
**I want** validate the syntax of the self-description\
**So that** to ensure the self-description adheres to the rules and structure defined by the JSON and JSON-LD specifications

AC

1. If any syntax errors are found, review the error messages provided by the validation engine to understand the issue
1. Check if correct syntax passes or incorrect syntax fails validation
1. Verify if the missing mandatory fields cause validation failure
1. Check if input with incorect data type cause validation failure
1. Check if alphabets in number fields where only numbers are allowed

[Back to the list of user stories](#user-stories)

### User story 8: Simpl Self-Description Frontend

**Status:** Ready for DG Acceptance (backlog item SIMPL-1083)

**As a** provider user\
**I want to** have an interface (using html forms) that I can use to define self-descriptions of various shape constraints defined using the SHACL ontology

- the interface should be dynamic and displayed based on the shape constraints
- we should have no external branding visible on the interface
- the form elements should display possible constraints and validations as data is inputed

- when submitting the form I want a JSON-LD to be created and the shape as defined to be stored on the federated catalogue (with all the prerequisite steps, eg: signing )
- initially the interface (form) should be able to handle shacl vocabulary as presented on SD-Tooling: [https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-frontend/-/blob/main/documentation/shacl-angular-mapping.md?ref_type=heads](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-frontend/-/blob/main/documentation/shacl-angular-mapping.md?ref_type=heads)
- I want the ability to change the shape for which I can define self description

Acceptance Criteria:

- I should be able to select different shapes for which I can create self-descriptions
- inputing incorrect data should provide visual feedback (show errors)
- form should not be able to submit with errors
- when submitting I should be notified of status (success, error)
- I should be able to find the submitted data in the federated catalogue

[Back to the list of user stories](#user-stories)

### User story 9: Integrating the asset registering of the connector in to the published process of SD

**Status:** Ready for DG Acceptance (backlog item SIMPL-6287)

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

[Back to the list of user stories](#user-stories)

### User story 10: Automatically populate the provider id on the resource description

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-15289)

#### Storycard

**As a** Provider\
**I want** that my technical provider identifier is automatically in a new resource description when creating it via UI\
**So that** the resource description can be identified as my own.

#### Acceptance criteria

**Positive case**

1. **Given** I am authenticated as a provider\
   **And** I create a new resource description in the UI\
   **When** I submit the filled resource description to the backend\
   **Then** the provider ID is automatically set in the new resource description.

**Negative Case**

2. **Given** I am authenticated as a provider\
   **And** I create a new resource description in the UI\
   **When** I the provider ID cannot be retrieved and set automatically\
   **Then** I am not able to submit the resource description\
   **And** I receive a comprehensive error message

#### Technical Information

- On resource description we need to link the IAA provider-id on the resource description
- Relevant API's
   - (internal page) this is the page that describes how it works, note that the method - **GET /credential/my-id** ** **is replaced by** **GET /credentials** and the page will be updated ASAP.
   - (internal page)    see component #14 - Tier 2 Authentication Provider **Credential GET /credentials** method
- (internal page)  to get information on Identification, Authentication & Authorisatio

Properties in the schema to be set:

- `providedBy`

[Back to the list of user stories](#user-stories)

### User story 11: [BE] Automatically populate the version number of a new resource description

**Status:** Ready for DG Acceptance (backlog item SIMPL-16825)

#### Storycard

**As a** Provider\
**I want** Simpl to automatically assign the initial version number, based on the applicable versioning algorithm, to a new resource description\
**So that** I can uniquely identify the resource description ensuring traceability.

#### Acceptance criteria

**Scenario:** A Provider automatically assigns a version number to a new resource desciption - Positive case

1. **Given** I am authenticated as a provider\
   **And** I create a new resource description in the UI\
   **When** I submit the filled resource description to the backend to be enriched and validated\
   **Then** Simpl-open assigns the initial version number, based on the applicable versioning algorithm for resouce desciptions.

2. **Given** a developer wants to define or configure a versioning algorithm\
   **When** they access the Simpl-open documentation\
   **Then** the documentation contains instructions describing how to define a versioning algorithm\
   **And** the documentation includes the default versioning algorithm used by Simpl-open\
   **And** the documentation is accessible through the official Simpl-open documentation in the code repository

**Note on negative AC:**\
The scope of this story is merely to set the initial version number. Thus negative AC should only cover issues with the versioning algorithm itself, not general scenarios, such as not authorised or a general internal server error.

We can assume thast the versioning algorithm has been developed and tested itself outside of the resource description management components. This implies that we also can assume, that the algorithm will be generally able to provide an initial version number and negative scenarios are not necessary in this story.

#### Technical Information

- The versioning algorithm is identical to the one used in schema management.
- The versioning algorithm is contained in the same library as used in schema management.
- The related story in schema management is (internal page)
- Setting the version number in a new version of a resource description is handled in (internal page)

[Back to the list of user stories](#user-stories)

### User story 12: [BE] Automatically populate the version number of a new version of a resource description

**Status:** Ready for DG Acceptance (backlog item SIMPL-24346)

#### Storycard

**As a** Provider\
**I want** Simpl to automatically assign the first version number, based on the applicable versioning algorithm, to a new resource description\
**So that** I can uniquely identify the resource description ensuring traceability.

#### Acceptance criteria

**Scenario:** A Provider automatically assigns a version numnber to a new resource desciption - Positive case

1. **Given** I am authenticated as a provider\
   **And** I create a new resource description in the UI\
   **When** I submit the filled resource description to the backend to be enriched and validated\
   **Then** Simpl-open assigns the initial version number, based on the applicable versioning algorithm for resouce desciptions.

**Note on negative AC:**\
The scope of this story is merely to set the initial version number. Thus negative AC should only cover issues with the versioning algorithm itself, not general scenarios, such as not authorised or a general internal server error.

We can assume thast the versioning algorithm has been developed and tested itself outside of the resource description management components. This implies that we also can assume, that the algorithm will be generally able to provide an initial version number and negative scenarios are not necessary in this story.

#### Technical Information

- The versioning algorithm is identical to the one used in schema management.
- The versioning algorithm is contained in the same library as used in schema management.
- The related story in schema management is SIMPL-20534

[Back to the list of user stories](#user-stories)

---

Back to [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md#5b41-a-provider-signs-and-submits-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.4 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md) · [BP05B process page](../README.md)
