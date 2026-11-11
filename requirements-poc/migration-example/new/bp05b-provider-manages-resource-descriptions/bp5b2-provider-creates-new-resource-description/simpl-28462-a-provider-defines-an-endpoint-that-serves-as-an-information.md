<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider defines an endpoint that serves as an information point to check the credit status of their usage contract

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-28462. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** define a payment information point\
**So that** I can include the payment information point in the billing schema

and assess the credit status of my usage contract

**Acceptance criteria:**

The Provider can define endpoints that are external to Simpl-Open, and that provide the credit status of a usage contract. Defining the endpoints is considered as the responsibility of the Provider. Simpl-Open will support the Provider by providing documentation on how to these endpoints can be used in the billing schema.

## User stories

1. [[Documentation][Credit status] Create Payment information point definition guide for Providers](#user-story-1-documentationcredit-status-create-payment-information-point-definition-guide-for-providers)

### User story 1: [Documentation][Credit status] Create Payment information point definition guide for Providers

Backlog item SIMPL-36086

**Goal:**\
Produce a guide that explains how a Provider defines a payment information point: an endpoint that determines from a system outside of Simpl-Open (e.g. account/billing system) whether all the invoices for a particular usage contract have been paid in time. The endpoint should only return true/false, and only consider contracts that are at or past their due date.

The Provider can then reference this endpoint in the billing schema contract clause and use it to detect when the credit status is non-compliant and trigger notifications and contract actions (e.g. suspension of the usage contract) in case of non-compliance.

**Technical Description:**

- Concepts: Define what a payment information point is and how it relates to the billing schema contract clause and the usage contract.
- Information returned by the endpoint:
   - True/false;
   - Determines whether all the invoices for a particular usage contract have been paid in time. Only consider contracts that are at or past their due date.
- Endpoint Definition: Explain how to expose an aggregation as an endpoint, including the request and response format and the parameters for filtering by resource, contract and period.
- Contract Integration: Explain how to reference the endpoint in the billing schema contract clause, including the endpoint URL, metric identifier, target value and measurement method.

**Technical Acceptance Criteria:**

- Provider Usability: A Provider who did not write the guide can define a working payment information point and reference it in a billing schema contract clause by following only the documented steps.

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
