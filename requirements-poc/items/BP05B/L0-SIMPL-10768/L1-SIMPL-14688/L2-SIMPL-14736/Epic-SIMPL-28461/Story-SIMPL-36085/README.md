> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-28461](../README.md) / **Story-SIMPL-36085**

# SIMPL-36085: [Documentation][Billing] Create Billing information point definition guide for Providers

**User story (implementable work)** · Status **New Product Backlog Item**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-28461](../README.md) (Epic) › **SIMPL-36085** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-28461](../README.md) (Epic) A Provider defines an endpoint that serves as an information point to calculate the billing data |
