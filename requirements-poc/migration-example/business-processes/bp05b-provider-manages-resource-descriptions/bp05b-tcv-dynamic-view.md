# TCV Dynamic - BP 05B - Provider manages resource descriptions

> **Requirements:** [5B.2 A Provider creates a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/README.md) · [5B.4 A Provider signs and submits a new Resource description or a new version](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14729/README.md) · [5B.5 The Governance Authority publishes a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/README.md)

This perspective illustrates the interactions and the flows between all the technological components.

```mermaid
sequenceDiagram
	participant User as User/Browser
	participant FE as SD Tooling FE
	participant BE as SD Tooling BE
	participant Storage as Web Storage
	participant Validator as Validation Service
	participant Adapter as EDC Connector Adapter
	participant Provider as EDC Provider Connector
	participant Signer as Signer Service
	participant Catalogue as XSFC Catalogue

	User->>FE: Select Schema List
	activate FE
	FE->>BE: GET /schemas
	activate BE
	BE-->>FE: Schema List
	deactivate BE
	FE-->>User: Schema List
	User->>FE: Select Schema
	FE->>BE: GET /schemas/{schemaId}/content
	activate BE
	BE-->>FE: Schema (TTL)
	deactivate BE
	FE->>FE: Generate JSON forms
	FE-->>User: JSON field visualization
	deactivate FE
	User->>FE: Fill form fields
	activate FE
	FE-->>User: Visualize form filled
	deactivate FE
	User->>FE: Click Publish button
	activate FE
	FE->>BE: POST /selfDescription/enriched
	activate BE
	BE->>Storage: Download SLA Agreement
	activate Storage
	Storage-->>BE: SLA Agreement
	deactivate Storage
	BE->>BE: Calculate SLA hash
	BE->>Storage: Download Billing Schema
	activate Storage
	Storage-->>BE: Billing Schema
	deactivate Storage
	BE->>BE: Calculate Billing Schema hash
	BE->>Storage: Download Contract Template
	activate Storage
	Storage-->>BE: Contract Template
	deactivate Storage
	BE->>BE: Calculate Contract Template hash
	BE->>BE: Attach hashes and algorithm to JSON-LD
	BE->>Adapter: POST /selfDescriptions/enriched
	activate Adapter
	Adapter->>Adapter: Extract Asset info from JSON-LD
	Adapter->>Provider: createAsset API
	activate Provider
	Provider-->>Adapter: Asset ID
	deactivate Provider
	Adapter->>Adapter: Extract access policy from JSON-LD
	Adapter->>Provider: createPolicy (access) API
	activate Provider
	Provider-->>Adapter: accessPolicyID
	deactivate Provider
	Adapter->>Adapter: Extract usage policies from JSON-LD
	Adapter->>Provider: createPolicy (usage) API
	activate Provider
	Provider-->>Adapter: contractPolicyID
	deactivate Provider
	Adapter->>Provider: contractDefinition API
	activate Provider
	Provider-->>Adapter: contractID
	deactivate Provider
	Adapter->>Adapter: Enrich JSON-LD
	Adapter-->>BE: Enriched JSON-LD
	deactivate Adapter
	BE->>Validator: POST /sd/validate
    activate Validator
	Validator-->>BE: Validation Result
	deactivate Validator
	BE-->>FE: JSON-LD or KO
	deactivate BE
	FE->>Signer: Sign API
	activate Signer
	Signer-->>FE: Signed SD
	deactivate Signer
	FE->>Storage: POST /sd/publish
	activate Storage
	Storage->>Catalogue: POST /publish API
	activate Catalogue
	Catalogue-->>Storage: Return
	deactivate Catalogue
	Storage-->>FE: Return
	deactivate Storage
	FE-->>User: OK/KO
	deactivate FE
```


## Schema Synchronization

The figure illustrates the role of the *schema-sync-adapter* component. This component receives notifications from the *Schema Management Service* when a schema is published, versioned, or revoked. It then retrieves the schema from the *Schema Management Service* and stores it in the schema storage (NFS), making it available for use by the *SD-Tooling*.

```mermaid
sequenceDiagram
	participant Tooling as SDTooling
	participant NFS as Schema Storage (NFS)
	participant Sync as Schema Sync Adapter
	participant SMS as Schema Management Service

	SMS->>Sync: /schemas/events
	activate Sync
	Sync->>SMS: SHACL file download request
	activate SMS
	SMS-->>Sync: SHACL file
	deactivate SMS
	Sync->>NFS: Update schema in NFS
	activate NFS
	NFS-->>Sync: Update result
	deactivate NFS
	Sync-->>SMS: Schema process result
	deactivate Sync
	Tooling->>NFS: Schema updated request
	activate NFS
	NFS-->>Tooling: Updated schema list
	deactivate NFS
```
