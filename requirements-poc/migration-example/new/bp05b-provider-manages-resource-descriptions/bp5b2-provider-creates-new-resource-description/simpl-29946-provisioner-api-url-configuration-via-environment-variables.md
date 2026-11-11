<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: Provisioner API URL Configuration via Environment Variables (Remove Manual UI Property) [Improvement]

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.02 Add resource type specific metadata](../README.md#bp05b02-add-resource-type-specific-metadata).

In the backlog it is item SIMPL-29946, status **New**. It has no user stories yet.

## What this Epic delivers

**As a** Provider end user,\
**I want** the Provisioner API URL to be managed through deployment configuration instead of manual UI input,\
**So that** provisioning is more reliable and less error-prone.

##### Description

During review discussions, it was clarified that the Provisioner API URL is typically stable per provider and acts as a connector-to-provisioner integration endpoint. Keeping this value as a user-editable field in the UI is not user-friendly and is error-prone. A typo in this URL can break provisioning flows.

Moving this configuration to environment variables:

- aligns with the existing component-to-component configuration approach,
- reduces operational risk,
- improves user experience by removing unnecessary manual input,
- increases reliability of provisioning integration.

##### Scope of Change

- Backend: retrieve Provisioner API URL from environment variables instead of resource-description UI input.
- UI: remove the Provisioner API URL property field from the RD Manager wizard for infrastructure offerings.
- Documentation: update related tickets/epic description with context and rationale for traceability.

##### Related Tickets

- SIMPL-24655 (internal page)
- SIMPL-24656 (internal page)

##### Acceptance Criteria

###### AC1 - UI no longer exposes Provisioner API URL field

1. **Given** I am an authenticated Provider end user with permission to manage Resource Descriptions\
   **And** I am creating or editing an infrastructure offering in RD Manager\
   **When** I reach the step where infrastructure properties are configured\
   **Then** the Provisioner API URL field is not displayed\
   **And** I cannot manually enter or modify the Provisioner API URL in the UI

###### AC2 - Backend reads Provisioner API URL from deployment configuration

2. **Given** the Infrastructure Provider Agent is deployed with a configured Provisioner API URL environment variable\
   **When** the backend prepares a provisioning request for an infrastructure resource description\
   **Then** the Provisioner API URL is retrieved from environment configuration\
   **And** no Provisioner API URL value is required from the user payload

###### AC3 - Provisioning succeeds with valid environment configuration

3. **Given** the Provisioner API URL environment variable is configured with a valid endpoint\
   **And** a Provider end user submits a valid infrastructure resource description with a linked deployment script\
   **When** provisioning is triggered\
   **Then** provisioning succeeds\
   **And** the provisioning call is executed against the configured environment-based Provisioner API URL

---

Back to [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
