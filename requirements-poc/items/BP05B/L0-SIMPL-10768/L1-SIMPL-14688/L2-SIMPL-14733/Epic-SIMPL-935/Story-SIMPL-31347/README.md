> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-935](../README.md) / **Story-SIMPL-31347**

# SIMPL-31347: [API] Provider retrieves the details of an access policy template

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-935](../README.md) (Epic) › **SIMPL-31347** (User story)

## Text

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** retrieve the details of an installed access-policy template\
**So that** I can use the template to define an access policy

#### Acceptance criteria

##### Provider retrieves an access-policy template

1. **Given** I am authenticated as a Provider with the required role\
   **And** an installed access-policy template exists for the requested identifier\
   **When** I request that template through the detail policy-template API\
   **Then** the service responds with `200 OK`\
   **And** returns the full template descriptor\
   **And** the descriptor contains the information needed to render the form and create a valid ODRL access-policy instance

##### Unauthenticated client is rejected

2. **Given** I am not authenticated\
   **When** I request an access-policy template by identifier\
   **Then** the service responds with `401 Unauthorized`\
   **And** returns no template data

##### Provider without the required role is rejected

3. **Given** I am authenticated without a role that allows me to manage Resource Descriptions\
   **When** I request an access-policy template by identifier\
   **Then** the service responds with `403 Forbidden`\
   **And** returns no template data

##### Requested template does not exist

4. **Given** I am authenticated as a Provider with the required role\
   **When** I request an identifier that does not match an installed access-policy template\
   **Then** the service responds with `404 Not Found`

#### Technical details

- This is a read-only operation in the shared policy-template API defined by SIMPL-31345.
- Access and usage templates use the same API and are distinguished through policy-type filtering.
- The operation can retrieve any installed access-policy template, including shipped and configured templates.
- Template visibility does not differ by Provider.
- The descriptor contains the fixed ODRL rule metadata, allowed operators, right-operand shape, authored fields and validation metadata, information point, and access-enforcement metadata needed by the client.
- The API implementation details and OpenAPI artifact will be added directly to this story when available.

#### Additional notes

- API definition - SIMPL-31345 (internal page)
- AI-supported implementation expedite - SIMPL-32078 (internal page)
- Policy Definition (internal page)
- [W3C ODRL Information Model](https://www.w3.org/TR/odrl-model/)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-935](../README.md) (Epic) A Provider is supported in defining an access policy by a using policy template via API |
