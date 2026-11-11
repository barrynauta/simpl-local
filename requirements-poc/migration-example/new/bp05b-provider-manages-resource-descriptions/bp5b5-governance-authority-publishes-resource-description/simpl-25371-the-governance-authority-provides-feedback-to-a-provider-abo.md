<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) / Epic
</div>

# Epic: The Governance Authority provides feedback to a Provider about syntax validation errors of a policy

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.06 Remediate validation issues](../README.md#bp05b06-remediate-validation-issues).

In the backlog (Jira) it is item SIMPL-25371. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Governance Authority\
**I want to** provide feedback to a Provider about invalid syntax of a policy in a resource description\
**So that** the Provider can correct the errors.

**Acceptance Criterion 1: Provide feedback about syntax validation errors of an access policies**

**Scenario:** The Governance Authority provides feedback to a Provider about syntax validation errors of an access policy (Syntax validation - negative case)

1. **Given** I have received a new resource description or a new version of a resource description with invalid syntax of an access policy\
   **When** the syntax of this policy in the resource description is automatically checked\
   **Then** a feedback about invalid syntax of the access policy is sent to the Provider with details

**Acceptance Criterion 2: Provide feedback about syntax validation errors of a usage policies**

**Scenario:** The Governance Authority provides feedback to a Provider about syntax validation errors of an usage policy (Syntax validation - negative case)

2. **Given** I have received a new resource description or a new version of a resource description with invalid syntax of a usage policy\
   **When** the syntax of this policy in the resource description is automatically checked\
   **Then** a feedback about invalid syntax of the usage policy is sent to the Provider with details

## User stories

1. [[API] The Governance Authority provides feedback to a Provider about policy syntax validation errors](#user-story-1-api-the-governance-authority-provides-feedback-to-a-provider-about-policy-syntax-validation-errors)

### User story 1: [API] The Governance Authority provides feedback to a Provider about policy syntax validation errors

Backlog item SIMPL-31640

#### Storycard

**As** Governance Authority,\
**I want** the Provider to receive clear feedback when the policies in a submitted resource description are invalid,\
**So that** the Provider can correct them before publication.

#### Acceptance criteria

##### Valid policies allow submission to continue

1. **Given** an authorised Provider submits a new resource description or a new version\
   **And** all its access and usage policies meet the supported policy rules\
   **When** Simpl-Open checks the submitted policies\
   **Then** no policy error is returned\
   **And** the resource description continues through the remaining publication checks

##### A readable policy contains an invalid rule

2. **Given** an authorised Provider submits a resource description\
   **And** the submitted content can be read\
   **And** an access or usage policy contains an unsupported action, an invalid condition, or a missing required value\
   **When** Simpl-Open checks the policies\
   **Then** the resource description is not published\
   **And** the response identifies the affected policy\
   **And** explains which part needs correcting, which rule it breaks, and why

This applies to both new resource descriptions and new versions. A readable document must still be rejected when its policy rules are invalid.

##### Access and usage policies both contain errors

3. **Given** an authorised Provider submits a resource description\
   **And** both an access policy and a usage policy contain errors\
   **When** Simpl-Open checks the policies\
   **Then** the response identifies both failed policies\
   **And** gives at least one useful correction message for each invalid policy\
   **And** the resource description is not published

##### A policy cannot be read

4. **Given** an authorised Provider submits a resource description\
   **And** the resource description can be read but one of its policy contents cannot\
   **When** Simpl-Open checks the policies\
   **Then** the resource description is not published\
   **And** the response identifies the unreadable policy and explains that its contents must be corrected

If the whole submission cannot be read, return the existing malformed-request error instead of a policy-validation success.

##### Policies carried over from an earlier resource description are checked

5. **Given** an authorised Provider submits a new resource-description version\
   **And** it retains policies from an earlier version\
   **When** Simpl-Open checks the submitted policies\
   **Then** it checks the retained policies as well as newly added or edited policies\
   **And** a missing authoring template does not cause a policy to be skipped or silently removed

The same policy checks apply to submissions made directly through the API.

##### The sender cannot publish resource descriptions

6. **Given** a sender does not have the required identity attributes to manage resource descriptions\
   **When** the sender submits a resource description\
   **Then** the submission is forbidden\
   **And** no policy-validation feedback is returned

##### The policy check cannot complete

7. **Given** an authorised Provider submits a resource description\
   **When** a technical failure prevents the required policy check from completing\
   **Then** the response reports that the check could not complete\
   **And** does not report the policies as valid\
   **And** the resource description is not published

#### Technical details

Reuse the completed-policy validation rules already applied by `POST /policyTemplates/{id}/materialize`. Saving a policy checks the entered values and the filled policy, but final submission must also validate the completed access and usage documents after combination and any publication changes. The materialization operation itself requires a template and form input, so retained policy documents need an internal integration with the completed-policy validator. No new Provider-facing validation operation is proposed.

The proposed ownership is Authority catalogue validation on the existing `POST /self-descriptions` operation, with RD tooling passing the errors back to the editor. The trusted RD-tooling flow should also check policy content before connector or workflow registration. Use the same policy rules for those checks. Catalogue rejection alone happens too late to prevent earlier registration.

Use the response defined by SIMPL-31639. Return a structured list with access/usage kind, an unambiguous reference to the submitted policy, a location within that policy where available, the violated rule category and a readable correction message. If the policy cannot be parsed, identify the whole policy rather than inventing a clause location. Return at least one error per invalid policy, including both kinds when both fail. More errors may be returned; exhaustive reporting of every error is not required.

Preserve the existing 400 rejection for malformed submission or policy content. Use 422 when the policy can be parsed but fails the supported structure or profile checks in the catalogue. This extends the catalogue's existing verification-failure convention. Earlier RD-tooling validation retains its existing 400 response. Authentication failures, forbidden submissions and service failures remain distinct.

Validation uses the reviewed R08 vocabulary and structures, including the Simpl profile namespace `https://ec.europa.eu/simpl/profile`. Access and usage identify policy purpose; they do not restrict access to permissions or usage to duties. Policy-validation success permits the other publication checks to continue and is not a publication-success claim. Non-policy RD feedback remains under SIMPL-21591.

#### Additional notes

- Policy-validation API definition, SIMPL-31639 (internal page)
- HLF2, assign policies to resource descriptions (internal page)
- R08 ODRL template technical description (internal page)

---

Back to [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
