<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider is supported in defining an access policy by a using policy template via API

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-935. It has 4 user stories, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

**As a** Provider\
**I want to** be supported in defining an access policy through policy templates, by retrieving via API:

- the list of access policy templates;
- the details of an access policy template.

**So that** I can correctly and easily define access policies.

**Acceptance Criterion 1: Retrieve the list of predefined policy templates**

**Scenario:** A Provider supported in defining an access policy, by retrieving the list of access policy templates, via API

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request for the available access policy templates via an API\
   **Then** Simpl-Open shall return all policy templates that can be used for defining the access policy.

**Acceptance Criterion 2: Retrieve the details of a selected predefined policy template**

**Scenario:** A Provider supported in defining an access policy, by retrieving the details of an access policy template, via API

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request the details of an access policy template via an API\
   **Then** Simpl-Open shall return the details of a policy template in a structured format that can be used for defining the access policy.

**Acceptance Criterion 3: Provide predefined policy templates**

The Provider can use predefined policy templates to create policies. Simpl-Open will support the Provider by making predefined policy templates available and documentation on how to use them.

## User stories

1. [API for Access Policy](#user-story-1-api-for-access-policy) · Developed
2. [[API] Access Policy multiple attributes per Permission](#user-story-2-api-access-policy-multiple-attributes-per-permission) · Planned
3. [[API] Provider retrieves the list of access policy templates](#user-story-3-api-provider-retrieves-the-list-of-access-policy-templates) · Developed
4. [[API] Provider retrieves the details of an access policy template](#user-story-4-api-provider-retrieves-the-details-of-an-access-policy-template) · Developed

### User story 1: API for Access Policy

**Development:** Developed · backlog item SIMPL-2938

**As a** Provider User\
**I want to** use an API to assign the previously formulated machine-readable access policies to the self-description and thus to the dataset/application/infrastructure\
**So that** the access to the dataset/application/infrastructure can be evaluated and enforced as defined in the policy.

+**Technical Description**+

The API must retrieve the parameters for the access policies, create a valid ODRL expression and return it.

Definition of the policies is descriped as follows:

an internal page

Should written in the Form of ODRL Policies

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

can be embedded in the JSON-LD document of the self-description.

For the MVP we plan to support access policies with limited expressive power. It is possible to define two different actions

- [http://simpl.eu/odrl/actions/](http://simpl.eu/odrl/actions/)search: the attribute holder is able to search for the dataset/application/infrastructure
- [http://simpl.eu/odrl/actions/](http://simpl.eu/odrl/actions/)consume: The attribute holder can consume the dataset/application/infrastructure
- [http://simpl.eu/odrl/actions/](http://simpl.eu/odrl/actions/)restrictedConsume: The attribute holder can consume the dataset/application/infrastructure with restrictive access

**consume, restrictedConsume** implies search.

For constraints we plan to support date time constraints that allow to specify when the policy should be valid.

{RESSOURCE_URI}

, {POLICY_URI}, {PROVIDER_URI} are later automatically replaced with the correct URI. {SECURITY_ATTRIBUTE_ID} need to be specified, for the first iteration from a list of static IDs later available over an API provided by Aruba.

Example can be found here: an internal page

draftId=163807492&draftShareId=d0caf9a0-fa82-4153-b08e-e0238d00adc1& (internal page)

Each Policy from the frontend is mapped into a new permission:

The action, attribute and valid from/until are set by the value from the frontend. If no valid until or from is given then the part is omitted.

Those attributes are filled out automatically:\
`**{POLICY_URI}:** can be generated like uuid`

{{*{RESSOURCE_URI}

:* the web:did from the service offering}}

{{

{PROVIDER_URI}

:\ URI of the provider from API by Aruba}}

(internal page)

+**Acceptance Criteria**+

1. The API creates a valid ODRL Expression and takes the action and the security attribute
1. The API must ensure that only the speficied actions and security attributes are used
1. The API automatically inserts the provider and resource information
1. The API validates the ODRL expression format
1. The API provides comprehensive feedback / error messages in case of mismatch / validation errors
1. The API returns the JSON Object containing the ODRL Expression
1. THE JSON ODRL expression is assigned to the SD formatted as a string

Once US is done the related US can be closed/marked as done

[Back to the list of user stories](#user-stories)

### User story 2: [API] Access Policy multiple attributes per Permission

**Development:** Planned · backlog item SIMPL-3546

#### Storycard

**As an** authorised Provider using the resource-description API,\
**I want to** submit an access policy with one or more required identity attributes,\
**So that** a Consumer must have all the attributes I selected to access my resource.

#### Acceptance criteria

##### Submit a policy with several required attributes

1. **Given** I am authorised to manage resource descriptions\
   **And** my submitted resource description includes an otherwise valid access policy requiring several identity attributes\
   **When** Simpl-Open checks the policy\
   **Then** the policy is accepted for the remaining publication checks

##### Submit a policy with one required attribute

2. **Given** I am authorised to manage resource descriptions\
   **And** my submitted resource description includes an otherwise valid access policy requiring one identity attribute\
   **When** Simpl-Open checks the policy\
   **Then** the policy is accepted for the remaining publication checks

##### Reject an empty attribute requirement

3. **Given** I am authorised to manage resource descriptions\
   **And** my submitted policy requires identity attributes but does not specify any\
   **When** Simpl-Open checks the policy\
   **Then** it rejects the policy\
   **And** the feedback identifies the policy and explains that at least one attribute is required\
   **And** the resource description is not published

##### Reject an invalid attribute requirement

4. **Given** I am authorised to manage resource descriptions\
   **And** an attribute requirement in my submitted policy cannot be interpreted\
   **When** Simpl-Open checks the policy\
   **Then** the feedback identifies the policy and the invalid requirement\
   **And** the resource description is not published

#### Technical details

The existing resource-description submission flow receives the completed ODRL policy. Use the existing submission API; no dedicated multi-attribute endpoint is introduced. Publication validation checks the policy against the agreed profile and preserves its meaning. Diagnostic fields and HTTP responses belong to the shared contract in SIMPL-31639.

| Property | Contract implemented in the local connector |
| --- | --- |
| Action and rule | `odrl:use` permission |
| Left operand | `https://ec.europa.eu/simpl/profile#identityAttributes` |
| Multiple-attribute template | `odrl:isAllOf` with a nonempty JSON-LD list of string codes |
| One selection in that template | Still a list with `odrl:isAllOf` |
| Separate single-attribute template | `odrl:hasPart` with a typed `xsd:string` value |
| Matching | All selected codes are required; extra Consumer codes are allowed; comparison is case-insensitive without whitespace trimming |
| Runtime facts and scope | Consumer `identity_attributes` IAM claim, evaluated during contract negotiation |

Preserve the JSON-LD context that declares `odrl:rightOperand` as a list. Do not convert the requirements into a comma-separated `assignee` string. Blank or non-string codes, malformed lists and unsupported operators are invalid compatibility cases. Missing or malformed Consumer claims deny access at runtime.

The submission API preserves and validates the requirement. The connector decides whether a particular Consumer meets it. Authoring choices, publication validation and runtime Consumer facts have separate responsibilities.

#### Supporting documentation

- R08 ODRL template technical description (internal page)
- R08 policy assignment and validation requirements (internal page)
- Shared policy-template API definition, SIMPL-31345 (internal page)
- Policy validation response contract, SIMPL-31639 (internal page)
- (internal page)

[Back to the list of user stories](#user-stories)

### User story 3: [API] Provider retrieves the list of access policy templates

**Development:** Developed · backlog item SIMPL-31346

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

[Back to the list of user stories](#user-stories)

### User story 4: [API] Provider retrieves the details of an access policy template

**Development:** Developed · backlog item SIMPL-31347

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

[Back to the list of user stories](#user-stories)

---

Back to [5B.2.4 A Provider assigns access policies to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
