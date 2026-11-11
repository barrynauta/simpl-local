<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.5 A Provider assigns usage policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider is supported in defining a usage policy by using a policy template via API

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.5 A Provider assigns usage policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-960, status **In Progress**. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** be supported in defining a usage policy through policy templates, by retrieving via API

- the list of usage policy templates;
- the details of a usage policy template.

**So that** I can correctly and easily define usage policies.

**Acceptance Criterion 1: Retrieve the list of predefined policy templates**

**Scenario:** A Provider supported in defining a usage policy, by retrieving the list of usage policy templates, via API

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request for the available usage policy templates via an API\
   **Then** Simpl-Open shall return all policy templates that can be used for defining the usage policy.

**Acceptance Criterion 2: Retrieve the details of a selected predefined policy template**

**Scenario:** A Provider supported in defining a usage policy, by retrieving the details of a usage policy template, via API

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request the details of a usage policy template via an API\
   **Then** Simpl-Open shall return the details of a policy template in a structured format that can be used for defining the usage policy.

## User stories

1. [API for Usage Policy](#user-story-1-api-for-usage-policy) (Ready for DG Acceptance)
2. [[API] Provider retrieves the list of usage policy templates](#user-story-2-api-provider-retrieves-the-list-of-usage-policy-templates) (DevTeam PBI is ready for Implementation check)
3. [[API] Provider retrieves the details of a usage policy template](#user-story-3-api-provider-retrieves-the-details-of-a-usage-policy-template) (DevTeam PBI is ready for Implementation check)

### User story 1: API for Usage Policy

**Status:** Ready for DG Acceptance (backlog item SIMPL-3241)

**As a** Provider User\
**I want to** use an API to assign the previously formulated machine-readable usage policies to the self-description and thus to the dataset/application/infrastructure\
**So that** the usage to the dataset/application/infrastructure can be evaluated and enforced as defined in the policy.

+**Technical Description**+

The API must retrieve the parameters for the usage policies, create a valid ODRL expression and return it.

Definition of the policies is described as follows:

(internal page)

Should written in the form of ODRL Policies

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

can be embedded in the JSON-LD document of the self-description.

Example can be found here:

(internal page)

```
{
  "@context": "http://www.w3.org/ns/odrl.jsonld",
  "@type": "Policy",
  "uid": "{POLICY_URI}",
  "profile": "http://www.w3.org/ns/odrl/2/odrl.jsonld",
  "target": "{RESSOURCE_URI}",
  "assigner": {
    "uid": "{PROVIDER_URI}",
    "role": "http://www.w3.org/ns/odrl/2/assigner"
  },
  "permission": [
    {
      "assignee": {
        "uid": "Consumer",
        "role": "http://www.w3.org/ns/odrl/2/assignee"
      },
      "action": [
        { "name": "http://www.w3.org/ns/odrl/2/use" }
      ],
      "target": "{RESSOURCE_URI}",
      "constraint": [
        {to be added by frontend)
      ]
    }
  ]
}
```

- POLICY_URI: generated uuid
- RESSOURCE_URI: web:did of the service offering (SD)
- PROVIDER_URI: participant ID from Aruba API

the constraints are set using the information from the frontend

**Restricted Number of Usages:**
```
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/count",
          "operator": "http://www.w3.org/ns/odrl/2/lteq",
          "rightOperand": "10"
        } 
```
The 10 is replaced by the parameter provided in the frontend

**Duration-restricted Data Usage**
```
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/dateTime",
          "operator": "http://www.w3.org/ns/odrl/2/leq",
          "rightOperand": "2024-12-31T23:59:59"
        },
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/dateTime",
          "operator": "http://www.w3.org/ns/odrl/2/geq",
          "rightOperand": "2024-01-01T00:00:00"
        } 
```
the rightOperand are replaced by the value from the frontend

**Deletion after usage (no parameter)**
```
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/deletion",
          "operator": "http://www.w3.org/ns/odrl/2/eq",
          "rightOperand": "after_use"
        } 
```
+**Acceptance Criteria**+

1. The API creates a valid ODRL Expression (manual testing, validaton of JSON format automatically)
1. The API must ensure that all specified constraints are part of the ODRL Expression
1. The API automatically inserts the provider and resource information
1. The API validates the ODRL expression format
1. The API provides comprehensive feedback / error messages in case of missmatch / validation errors
1. The API returns the JSON Object containing the ODRL Expression
1. THE JSON ODRL expression is assigned to the SD formatted as a string

Once US is done the related US can be closed/marked as done

### User story 2: [API] Provider retrieves the list of usage policy templates

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31385)

#### Story card

**As an** authorised Provider managing Resource Descriptions\
**I want to** retrieve the usage-policy templates\
**So that** I can select a template when adding a usage policy to a Resource Description

#### Acceptance criteria

##### Provider retrieves the usage-policy templates

1. **Given** I am authenticated as a Provider with the required role\
   **When** I request the policy templates using the usage-policy filter\
   **Then** the service responds with `200 OK`\
   **And** returns all usage-policy templates\
   **And** each item contains a stable identifier, name, and description

##### No usage-policy templates are available

2. **Given** I am authenticated as a Provider with the required role\
   **And** no usage-policy templates are available\
   **When** I request the policy templates using the usage-policy filter\
   **Then** the service responds with `200 OK`\
   **And** returns an empty array `[]`

##### Unauthenticated client is rejected

3. **Given** I am not authenticated\
   **When** I request the policy templates using the usage-policy filter\
   **Then** the service responds with `401 Unauthorized`\
   **And** returns no template data

##### Provider without the required role is rejected

4. **Given** I am authenticated without a role that allows me to manage Resource Descriptions\
   **When** I request the policy templates using the usage-policy filter\
   **Then** the service responds with `403 Forbidden`\
   **And** returns no template data

#### Technical details

- This is a read-only operation in the shared policy-template API defined by SIMPL-31345.
- Access and usage templates use the same API and are distinguished through policy-type filtering.
- The usage filter returns all usage-policy templates; visibility is not Provider-specific.
- The stable identifier is used by SIMPL-31386 to retrieve the full template details.
- The direct OpenAPI operation link will be added to this story when available.

#### Additional notes

- Shared policy-template API - SIMPL-31345 (internal page)
- Usage-policy template details - SIMPL-31386 (internal page)
- AI-supported implementation expedite - SIMPL-32078 (internal page)
- Policy Definition (internal page)
- [W3C ODRL Information Model](https://www.w3.org/TR/odrl-model/)

### User story 3: [API] Provider retrieves the details of a usage policy template

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-31386)

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

---

Back to [5B.2.5 A Provider assigns usage policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
