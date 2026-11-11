> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14730](../../README.md) / [Epic-SIMPL-19176](../README.md) / **Story-SIMPL-20514**

# SIMPL-20514: API for Schema Managament Webhook Notification and Trigger Schema Update

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14730](../../README.md) (L2) › [SIMPL-19176](../README.md) (Epic) › **SIMPL-20514** (User story)

## Text

**As a** Simpl-Open Participant\
**I want to** receive a notification via an API (exposed by schema-sync-adapter component) when a new schema version is created or revoked and synchronise my local schema storage\
**So that** I am having the latest and correct schema versions available to my applications and consistency and integrity with the resource descriptions is ensured.

**Acceptance criteria:**

**Subscription registration**

1. **Given** I am a participant (provider or consumer)\
   **And** I have an endpoint for receiving schema notifications\
   **When** the endpoint is submitted via API for registration at the Governance Authority\
   **And** the API validates the registration details\
   **Then** the subscription is registered at the Governance Authority

2. **Given** I am a participant (provider or consumer)\
   **And** I have an endpoint for receiving schema notifications\
   **When** the endpoint registration request is submitted with invalid or unreachable details\
   **Then** the Governance Authority rejects the registration request\
   **And** I receive an error response explaining why the registration failed

**Notification reception and validation**

3. **Given** I am a participant subscribed to schema notifications from the Governance Authority\
   **When** the Governance Authority publishes a new schema version\
   **Then** I receive a schema creation notification including references to the created schema

4. **Given** I am a participant subscribed to schema notifications from the Governance Authority\
   **When** the Governance Authority revokes a schema version\
   **Then** I receive a schema revocation notification including a references to the revoked schema

5. **Given** I am a participant subscribed to schema notifications from the Governance Authority\
   **When** I receive a schema notification with missing or invalid information\
   **Then** the notification is rejected\
   **And** I report a clear error message describing the error

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-19176](../README.md) (Epic) A Provider retrieves from the Governance Authority the schema of a published resource type |
