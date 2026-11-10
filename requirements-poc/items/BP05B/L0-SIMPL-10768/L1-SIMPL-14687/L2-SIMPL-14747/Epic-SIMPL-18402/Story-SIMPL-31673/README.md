> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14747](../../README.md) / [Epic-SIMPL-18402](../README.md) / **Story-SIMPL-31673**

# SIMPL-31673: [API] GA validates the syntax of access and usage policies of new RD version

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14747](../../README.md) (L2) › [SIMPL-18402](../README.md) (Epic) › **SIMPL-31673** (User story)

## Text

#### Storycard

**As** Governance Authority,\
**I want** Simpl-Open to check the access and usage policies when a Provider submits a new resource-description version,\
**So that** versions with invalid policies are not published and the Provider can correct the errors.

#### Acceptance criteria

##### All policies are valid

1. **Given** an authorised Provider submits a new version of a resource description\
   **And** its access and usage policies meet the supported policy rules\
   **When** Simpl-Open checks those policies\
   **Then** the Provider receives a successful policy-validation result\
   **And** the new version can continue through the remaining publication checks

##### An access policy is invalid

2. **Given** an authorised Provider submits a new resource-description version\
   **And** one of its access policies contains an invalid requirement\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying the access policy and the requirement to correct\
   **And** the new version is not published

##### A usage policy is invalid

3. **Given** an authorised Provider submits a new resource-description version\
   **And** one of its usage policies contains an invalid condition or obligation\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying the usage policy and the condition or obligation to correct\
   **And** the new version is not published

##### A policy uses an unsupported rule

4. **Given** an authorised Provider submits a new resource-description version\
   **And** a policy uses a rule that Simpl-Open does not support\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying the policy and the unsupported rule\
   **And** the new version is not published

##### Access and usage policies are both invalid

5. **Given** an authorised Provider submits a new resource-description version\
   **And** it contains an invalid access policy and an invalid usage policy\
   **When** Simpl-Open checks the policies\
   **Then** the Provider receives feedback identifying both policies and their errors\
   **And** the new version is not published

##### The submitted policies cannot be read

6. **Given** an authorised Provider submits a new resource-description version\
   **And** the submitted policies cannot be read\
   **When** Simpl-Open processes the submission\
   **Then** the Provider receives an error explaining that the submitted policies could not be read\
   **And** the new version is not published

##### The sender is not authorised

7. **Given** the sender is not authorised to manage resource descriptions\
   **When** the sender submits a new resource-description version\
   **Then** the submission is refused\
   **And** the new version is not published

#### Technical details

Run trusted validation on the final policy content before publication, after any policy IDs and asset targets have been assigned. Validate the ODRL structure, vocabulary, operand/operator combinations, value types and applicable profile rules. Template startup checks and browser checks do not replace this step. New resource descriptions need equivalent validation, with the owning story still to be confirmed.

Access and usage policies are checked in the same validation process. SIMPL-31639 defines the response contract and SIMPL-31640 covers detailed feedback. Return HTTP 400 for malformed submissions and HTTP 422 for readable submissions containing invalid policies. Feedback identifies the affected policies and the corrections needed, including both policy categories when both are invalid.

The new local connector functions implement the following access-policy combinations. These provide concrete compatibility examples for the publication-validation profile.

| Action and rule | Expanded left operand | Operator | Authored right operand | Runtime scope |
| --- | --- | --- | --- | --- |
| `odrl:use` permission | `https://ec.europa.eu/simpl/profile#identityAttributes` | `odrl:hasPart` | One typed string code | `contract.negotiation` |
| `odrl:use` permission | `https://ec.europa.eu/simpl/profile#identityAttributes` | `odrl:isAllOf` | A nonempty JSON-LD list of string codes | `contract.negotiation` |
| `odrl:use` permission | `https://ec.europa.eu/simpl/profile#evaluationTime` | `odrl:lt` | A typed `xsd:dateTime` literal with UTC or an explicit offset | `contract.negotiation`, `transfer.process` |

The action in the access-policy examples is `odrl:use`, expanded as `http://www.w3.org/ns/odrl/2/use`. The usage examples include `odrl:use` for a usage term and `odrl:delete` for a deletion obligation. These identify the actions in the examples; they do not require further actions or define the entire approved validation profile.

Invalid compatibility examples include an empty attribute list, a blank or non-string code, `odrl:eq` for the new `identityAttributes` operand, `odrl:lteq` for `evaluationTime`, an invalid calendar date and a deadline without a timezone offset. The legacy `edc:consumption` function has a separate operator contract.

EDC unwraps the time literal to a string, so the runtime evaluator cannot check the original JSON-LD datatype. Publication validation must inspect the authored representation. Runtime time evaluation permits access strictly before the deadline and denies at equality or later. It does not implement an HLF4 monitoring obligation.

A sender without the required management identity attributes receives HTTP 403, as in the existing authorisation scenario.

#### Supporting documentation

- R08 ODRL template technical description (internal page)
- R08 policy assignment and validation requirements (internal page)
- Policy validation response contract, SIMPL-31639 (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18402](../README.md) (Epic) The Governance Authority validates the syntax of the policies assigned to a new Resource description or a new version of a Resource Description |
