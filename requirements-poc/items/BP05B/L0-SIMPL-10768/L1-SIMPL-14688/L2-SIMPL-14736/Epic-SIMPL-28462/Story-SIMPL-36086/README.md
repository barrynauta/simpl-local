> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-28462](../README.md) / **Story-SIMPL-36086**

# SIMPL-36086: [Documentation][Credit status] Create Payment information point definition guide for Providers

**User story (implementable work)** · Status **New Product Backlog Item**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-28462](../README.md) (Epic) › **SIMPL-36086** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-28462](../README.md) (Epic) A Provider defines an endpoint that serves as an information point to check the credit status of their usage contract |
