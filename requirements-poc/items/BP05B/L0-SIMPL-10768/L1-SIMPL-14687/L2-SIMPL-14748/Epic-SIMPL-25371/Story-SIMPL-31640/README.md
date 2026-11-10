> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14748](../../README.md) / [Epic-SIMPL-25371](../README.md) / **Story-SIMPL-31640**

# SIMPL-31640: [API] The Governance Authority provides feedback to a Provider about policy syntax validation errors

**User story (implementable work)** · Status **FOR SC TO RESPOND**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14748](../../README.md) (L2) › [SIMPL-25371](../README.md) (Epic) › **SIMPL-31640** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-25371](../README.md) (Epic) The Governance Authority provides feedback to a Provider about syntax validation errors of a policy |
