<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) / Epic
</div>

# Epic: The Governance Authority notifies a Provider about syntax validation errors

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.06 Remediate validation issues](../README.md#bp05b06-remediate-validation-issues).

In the backlog (Jira) it is item SIMPL-17801. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** notify a Provider about invalid syntax of a new resource description or a new version of a resource description\
**So that** the Provider can correct the errors.

#### Acceptance criteria

**Scenario:** The Governance Authority notifies a Provider about syntax validation errors (Syntax validation - negative case)

1. **Given** I have received a new resource description or a new version of a resource description with invalid syntax\
   **When** the syntax of the content of the resource description is automatically checked\
   **Then** a notification about invalid syntax is sent to the Provider.

## User stories

1. [[API] The Governance Authority notifies a Provider about syntax validation errors](#user-story-1-api-the-governance-authority-notifies-a-provider-about-syntax-validation-errors) · Developed

### User story 1: [API] The Governance Authority notifies a Provider about syntax validation errors

**Development:** Developed · backlog item SIMPL-21591

#### Storycard

**As** Governance Authority\
**I want to** notify a Provider about invalid syntax of a new resource description or a new version of a resource description\
**So that** the Provider can correct the errors.

#### Acceptance criteria

*Note that syntax validation errors of policies are not part of this story*

**Scenario:** The Governance Authority notifies a Provider about syntax validation errors (Syntax validation - negative case)

1. **Given** I have received a new resource description or a new version of a resource description with invalid syntax\
   **When** the syntax of the content of the resource description is automatically checked\
   **Then** a notification about invalid syntax is sent to the Provider.

#### Technical details

A http response with a suitable error code and a meaningful error description is sufficient.

---

Back to [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
