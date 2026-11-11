<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog (Jira) it is item SIMPL-982. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** automatically check the syntax of a new resource description or a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

**Scenario:** The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description with valid syntax (Syntax validation - positive case)\
When I, the Governance Authority, receive a new resource description or a new version of a resource description with valid syntax\
and the Provider is authorised with the identity attributes to manage resource descriptions\
then the syntax of the content of the resource description is automatically checked\
and the syntax validation for the resource description or new version of the resource description has the result "valid".

**Scenario:** The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description with invalid syntax (Syntax validation - negative case)\
When I, the Governance Authority, receive a new resource description or a new version of a resource description with invalid syntax\
and the Provider is authorised with the identity attributes to manage resource descriptions\
then the syntax of the content of the resource description is automatically checked\
and the syntax validation for the resource description or new version of the resource description has the result "invalid".

## User stories

1. [Syntax Validation Engine](#user-story-1-syntax-validation-engine) · Developed

### User story 1: Syntax Validation Engine

**Development:** Developed · backlog item SIMPL-488

**As a** Governance Authority agent\
**I want** integrate a validation engine that is able to validate the syntax of self-descriptions.\
**So that** I can verify if a self-description adheres to the rules and structure defined by the JSON and JSON-LD specifications

**Technical Description:**

Customise Compliance service of GX to implement validation service for JSON-LD (GA Node) . SimplRegistry and SimplComplianceService with NotaryService creating a Compliance Credential in the name of the GovernanceAuthority

[https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/blob/development/README-api.md](https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/blob/development/README-api.md)

Repo:

[https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/tree/development](https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/tree/development)

AC

1. The validation engine successfully validates the resource file against the constraints
1. The validation result is parsed correctly
1. The validation result indicates whether the resource file adheres to the constraints or not
1. If any syntax errors are found, review the error messages provided by the validation engine to understand the issue

---

Back to [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
