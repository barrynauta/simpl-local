> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14732](../README.md) / **Epic-SIMPL-29946**

# SIMPL-29946: Provisioner API URL Configuration via Environment Variables (Remove Manual UI Property) [Improvement]

**Epic (deliverable, testable feature)** · Status **New**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14732](../README.md) (L2) › **SIMPL-29946** (Epic)

## Text

**As a** Provider end user,\
**I want the** Provisioner API URL to be managed through deployment configuration instead of manual UI input,\
**so that** provisioning is more reliable and less error-prone.

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

Given I am an authenticated Provider end user with permission to manage Resource Descriptions\
And I am creating or editing an infrastructure offering in RD Manager\
When I reach the step where infrastructure properties are configured\
Then the Provisioner API URL field is not displayed\
And I cannot manually enter or modify the Provisioner API URL in the UI

###### AC2 - Backend reads Provisioner API URL from deployment configuration

Given the Infrastructure Provider Agent is deployed with a configured Provisioner API URL environment variable\
When the backend prepares a provisioning request for an infrastructure resource description\
Then the Provisioner API URL is retrieved from environment configuration\
And no Provisioner API URL value is required from the user payload

###### AC3 - Provisioning succeeds with valid environment configuration

Given the Provisioner API URL environment variable is configured with a valid endpoint\
And a Provider end user submits a valid infrastructure resource description with a linked deployment script\
When provisioning is triggered\
Then provisioning succeeds\
And the provisioning call is executed against the configured environment-based Provisioner API URL

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14732](../README.md) (L2) A Provider adds resource type specific metadata to a new Resource description |
| **Children** | None. |
