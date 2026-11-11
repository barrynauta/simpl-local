> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-935](../README.md) / **Story-SIMPL-31346**

# SIMPL-31346: [API] Provider retrieves the list of access policy templates

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-935](../README.md) (Epic) › **SIMPL-31346** (User story)

## Text

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** retrieve the installed access-policy templates through the policy-template API\
**So that** I can select a template when adding an access policy to a Resource Description

#### Acceptance criteria

##### Provider retrieves the access-policy templates

1. **Given** I am authenticated as a Provider with the required role\
   **When** I request the installed policy templates using the access-policy filter\
   **Then** the service responds with `200 OK`\
   **And** returns every installed access-policy template\
   **And** each item contains a stable identifier, name, and description

##### No access-policy templates are installed

2. **Given** I am authenticated as a Provider with the required role\
   **And** no access-policy templates are installed\
   **When** I request the installed policy templates using the access-policy filter\
   **Then** the service responds with `200 OK`\
   **And** returns an empty array `[]`

##### Unauthenticated client is rejected

3. **Given** I am not authenticated\
   **When** I request the installed policy templates using the access-policy filter\
   **Then** the service responds with `401 Unauthorized`\
   **And** returns no templates

##### Provider without the required role is rejected

4. **Given** I am authenticated without a role that allows me to manage Resource Descriptions\
   **When** I request the installed policy templates using the access-policy filter\
   **Then** the service responds with `403 Forbidden`\
   **And** returns no templates

#### Technical details

- This is a read-only operation in the shared policy-template API defined by SIMPL-31345.
- Access and usage templates use the same API and are distinguished through policy-type filtering.
- The access filter returns every installed access-policy template; visibility is not Provider-specific.
- Each list item contains the summary information defined by the OpenAPI contract.
- The stable identifier is used by SIMPL-31347 to retrieve the full template descriptor.
- Responses are structured JSON and follow the authentication, authorisation, and RFC 9457 error rules defined in SIMPL-31345.
- The direct OpenAPI operation link will be added when the AI Approach implementation produces the artifact.

#### Additional documentation

- Access-policy templates API - SIMPL-31345 (internal page)
- Access-policy template details - SIMPL-31347 (internal page)
- Policy Definition (internal page)
- [W3C ODRL Information Model](https://www.w3.org/TR/odrl-model/)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-935](../README.md) (Epic) A Provider is supported in defining an access policy by a using policy template via API |
