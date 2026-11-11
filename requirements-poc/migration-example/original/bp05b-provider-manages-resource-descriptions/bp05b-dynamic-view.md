<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp05b-provider-manages-resource-descriptions?ref_type=heads) / Dynamic view
</div>

# BP05B Dynamic View

## Source

> **See also: [Business process overview](./README.md)** — narrative description of this business process, including actors, prerequisites, outcomes, and the full hierarchy of sub-processes.

---

## Trace

## Define and Publish Self-Description

This process outlines how a self-description can be defined and subsequently published in the Catalogue. Certain fields within the self-description link to other resources, which therefore need to be created beforehand. For instance, an infrastructure offering requires a deployment script to be added in advance so that it can be referenced within the self-description.

![image-2025-6-18_15-29-40.png](./media/image-2025-6-18_15-29-40.png)

1. **Schema Synchronisation:** The SD Tooling component on the Provider side initiates a request for schema definitions from its local Schema Registry, which is kept in sync with the one on the Governance Authority node. This ensures consistent schema access and alignment across participants, supporting unified self-description formats in the Data Space. The retrieved schema definitions are stored in a local Schema Datastore on the Provider’s end, ensuring quick access and version control.
2. **Create Self-Description:** Providers can create a new self-description or modify an existing one through the User Interface. This interface allows them to fill in necessary fields or use a previously stored self-description template.
3. **Syntax Validation:** The Syntax Validation component within SD Tooling checks the initial structure of the self-description to confirm it meets the required format. While primarily focusing on the form of the self-description, this step also checks for basic schema compliance. If any issues are found, the provider is prompted to make corrections before proceeding.
4. **Registering Self-Description:** Following syntax validation, the self-description is directed to the Connector component, where it is registered as an asset. This registration is critical for linking the self-description to a specific connector instance, enabling controlled access for consumption by the consumer.
5. **Signing and Publication:** The Signing/Publication Service manages the integrity and authenticity of the self-description. It signs the document using the Provider’s private key to prevent tampering and then publishes the signed self-description to the Catalogue. A copy of the signed self-description is also stored locally in the Provider’s Wallet for record-keeping purposes. The Wallet maintains a history of signed copies, with any necessary purge or retention policies applied to manage storage effectively. These policies should specify when older records are archived or deleted to optimise space and meet governance standards.
6. **Semantic Validation:** After publication, the Catalogue on the governance node initiates Semantic Validation. This step checks that the self-description adheres to the Data Space’s vocabularies and ontology standards, ensuring semantic consistency.
7. **Quality Check:** The Catalogue also performs a Quality Rules check to verify that the self-description meets all mandatory quality standards. If any semantic issues are identified, the End User is notified to address specific violations. If the self-description passes all checks successfully, the End User receives a confirmation notification, indicating that the resource is now ready for publication within the Data Space.
8. **Database Storage:** Upon passing all validations, the self-description is stored in the Catalogue’s Database along with its associated metadata, making it discoverable and accessible to other participants in the Data Space.

## Retrieve SD Metadata

This process illustrates how the metadata (such as status) of a self-description (SD) can be retrieved by a Provider. The different possible statuses for a self-description are outlined in the Gaia-X Federation Services documentation (40.3 Product Constraints) [here](https://gaia-x.gitlab.io/technical-committee/federation-services/federation-service-specifications/L05_FC_CCF/fc_ccf/).

![image-2025-6-18_15-30-7.png](./media/bp05b-sub-flow-2.png)

1. **Initiate Metadata Request:** The Provider initiates a request to retrieve metadata associated with a specific self-description. This request includes the unique identifier of the desired self-description and is sent to the **Catalogue** on the **Governance Authority Node**.
2. **Query Metadata:** Upon receiving the request, the Governance Authority Node processes it by querying its **Metadata Database** to locate the requested metadata. This step ensures that the metadata aligns with the unique identifier provided.
3. **Return Metadata:** After locating the requested metadata, the **Catalogue** prepare a response containing the metadata details. This ensures the requested information is available for consumption.
4. **Display to User:** The **SD Manager** receives the metadata and presents it to the Provider's end user, allowing them to view details like the status of the self-description.

## Retrieve Full Self-Description

This sequence describes the steps taken by a provider to retrieve a complete Self-Description (SD) for a resource.

![image-2025-6-18_15-30-25.png](./media/bp05b-sub-flow-3.png)

1. **Initiate SD Request**: The process begins with the provider using the **SD Manager** to initiate a request for a specific self-description by sending its unique identifier (SD ID) to the **Management Service** hosted on the **Governance Authority Node**.
2. **Query Self-Descriptions**: After processing, the **Management Service** queries the **Self-Description Database** to retrieve the detailed self-description associated with the given SD ID.
3. **Respond with Self-Description**: Once the full self-description is retrieved, it is sent back to the provider through the **Response SD** component. This self-description includes all necessary metadata and resource information.
4. **Display to User**: The SD Manager on the provider node displays the retrieved self-description to the provider’s end-user through its User Interface.
5. **Optional Storage in Wallet**: Optionally, the retrieved self-description can be stored in the provider’s **Wallet** for local record-keeping or offline access.

## Revoke a Self-Description (SD)

This process outlines how a provider can revoke an SD in the system, with possible statuses detailed in the [Gaia-X documentation](https://gaia-x.gitlab.io/technical-committee/federation-services/federation-service-specifications/L05_FC_CCF/) (40.3 Product Constraints).

![update SD status.jpg](./media/bp05b-sub-flow-4.jpg)

1. **Initiate Status Change**: The provider, through the **SD Manager** on the Provider Node, initiates a request to revoke a specific SD by sending the SD ID to the **Catalogue** on the **Governance Authority Node**.
2. **Revoke SD**: The system then revokes the SD in the **Catalogue** database to reflect the new status for the Self-Description.
3. **Response and Display**: The **Management Service** confirms the status update by returning the new status to the **SD Manager**. If the user interface (UI) is used, the updated status is displayed to the provider end user.

---

## Participants

- [**sd-tooling**](https://code.europa.eu/simpl/simpl-open/data/semantics-and-vocabulary/schema-management/sd-tooling-api) — SD Tooling / SD Manager (self-description creation, syntax validation, signing, publication)
- [**simpl-schema-manager**](https://code.europa.eu/simpl/simpl-open/data/semantics-and-vocabulary/schema-management/simpl-schema-manager) — Schema Management Service (enables _Governance Authority_ to create, version, and revoke resource description schemas).
- [**schema-sync-service**](https://code.europa.eu/simpl/simpl-open/data/semantics-and-vocabulary/schema-management/schema-sync-adapter) — Schema Sync Service (keeps Provider-side schema registry in sync with Governance Authority)
- [**connector**](https://code.europa.eu/simpl/simpl-open/integration/resource-sharing/resource-sharing-runtime/connector) — Connector (registers the SD as an asset for controlled consumer access)
- [**simpl-catalogue**](https://code.europa.eu/simpl/simpl-open/integration/resource-discovery/resource-catalogue/xfsc-federated-catalogue) — Simpl Catalogue (semantic validation, quality checks, SD storage and discoverability)
- [**signer-service**](https://code.europa.eu/simpl/simpl-open/security/credential-management/signing) — Signer Service (signs the SD with the Provider's private key before publication)
- [**wallet**](https://code.europa.eu/simpl/simpl-open/security/credential-management/wallet) — Wallet (stores signed copies of published SDs; temporary storage during retrieve flow)
