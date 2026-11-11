<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) / Epic
</div>

# Epic: The Governance Authority provides feedback to a Provider about syntax validation errors of a policy

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.06 Remediate validation issues](../README.md#bp05b06-remediate-validation-issues).

In the backlog (Jira) it is item SIMPL-25371. It has no user stories yet.

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

---

Back to [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../bp5b5-governance-authority-publishes-resource-description.md#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
