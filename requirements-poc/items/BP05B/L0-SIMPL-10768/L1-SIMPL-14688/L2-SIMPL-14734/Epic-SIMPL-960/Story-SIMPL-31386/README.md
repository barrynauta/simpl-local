> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-960](../README.md) / **Story-SIMPL-31386**

# SIMPL-31386: [API] Provider retrieves the details of a usage policy template

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-960](../README.md) (Epic) › **SIMPL-31386** (User story)

## Text

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** retrieve the details of a usage-policy template\
**So that** I can use the template when defining a usage policy

#### Acceptance criteria

##### Provider retrieves a usage-policy template

1. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **And** a usage-policy template exists with the requested identifier\
   **When** I request that template through the detailed policy-template API\
   **Then** the service responds with `200 OK`\
   **And** returns the template details needed to display and complete the template

##### Unauthenticated client is rejected

2. **Given** I am not authenticated\
   **When** I request a usage-policy template\
   **Then** the service responds with `401 Unauthorized`\
   **And** returns no template

##### Unauthorised client is rejected

3. **Given** I am authenticated but not authorised to manage Resource Descriptions\
   **When** I request a usage-policy template\
   **Then** the service responds with `403 Forbidden`\
   **And** returns no template

##### Requested template does not exist

4. **Given** I am authenticated as a Provider authorised to manage Resource Descriptions\
   **When** I request a usage-policy template using an unknown identifier\
   **Then** the service responds with `404 Not Found`

#### Technical details

- This is a read-only operation in the shared policy-template API defined in SIMPL-31345.
- The same API is used for access and usage templates, with filtering by policy type.
- All usage-policy templates can be retrieved by their stable identifier; visibility is not Provider-specific.
- Usage-policy templates do not use connector access-enforcement scopes.
- The direct OpenAPI operation link will be added to this story when it is available.

#### Additional notes

- Shared API definition: SIMPL-31345 (internal page)
- Usage-template list: SIMPL-31385 (internal page)
- AI implementation approach: SIMPL-32078 (internal page)
- Policy Definition (internal page)
- [W3C ODRL Information Model](https://www.w3.org/TR/odrl-model/)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-960](../README.md) (Epic) A Provider is supported in defining a usage policy by using a policy template via API |
