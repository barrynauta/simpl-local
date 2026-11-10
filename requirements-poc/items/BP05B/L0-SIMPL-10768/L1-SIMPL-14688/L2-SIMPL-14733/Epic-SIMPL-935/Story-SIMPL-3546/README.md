> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-935](../README.md) / **Story-SIMPL-3546**

# SIMPL-3546: [API] Access Policy multiple attributes per Permission

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-935](../README.md) (Epic) › **SIMPL-3546** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-935](../README.md) (Epic) A Provider is supported in defining an access policy by a using policy template via API |
