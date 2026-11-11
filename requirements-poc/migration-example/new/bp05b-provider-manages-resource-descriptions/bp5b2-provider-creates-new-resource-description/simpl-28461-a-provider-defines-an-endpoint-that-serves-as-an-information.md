<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider defines an endpoint that serves as an information point to calculate the billing data

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-28461. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider\
**I want to** define a billing information point based on the consumption logs of my resources and any other relevant logs\
**So that** I can include the billing information point in the billing schema

and generate the billing data that needs to be made available to my billing system to create an invoice

**Acceptance criteria:**

The Provider can define endpoints that use aggregations of consumption logs and any other relevant logs to track the Consumer's use of a resource in function of the calculation of the billing data. Simpl-Open will support the Provider by providing documentation on how to create such endpoints.

## User stories

1. [[Documentation][Billing] Create Billing information point definition guide for Providers](#user-story-1-documentationbilling-create-billing-information-point-definition-guide-for-providers)

### User story 1: [Documentation][Billing] Create Billing information point definition guide for Providers

Backlog item SIMPL-36085

**Goal:**\
Produce a guide that explains how a Provider defines a billing information point: an endpoint that aggregates consumption logs and other relevant logs to determine the consumed amount of billing units. This amount determines how the Consumer will be billed in the case of a pay-per-use billing model. The Provider can then reference this endpoint in the billing schema contract clause and check whether the amount of consumed billing units as a basis for billing.

**Technical Description:**

- Concepts:
   - Define what a billing information point is and how it relates to the billing schema contract clause and the usage contract;
   - List a few examples of use cases where aggregated logging data can be used to determine consumed amount of billing units.
- Data Sources: Describe the consumption logs available in Simpl-Open, including their structure, key fields, storage location, retention period and how a Provider accesses them.
- Aggregation: Explain how to define an aggregation for billing purposes
- Endpoint Definition: Explain how to expose an aggregation as an endpoint, including the request and response format and the parameters for filtering by resource, contract and period.
- Contract Integration: Explain how to reference the endpoint in the billing schema contract clause, including the endpoint URL, metric identifier, target value and measurement method.
- Example: Show how to define the endpoint for the billing schema that tracks the amount of data transfers in the past month.

**Technical Acceptance Criteria:**

- Provider Usability: A Provider who did not write the guide can define a working billing information point and reference it in a billing schema contract clause by following only the documented steps.

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
