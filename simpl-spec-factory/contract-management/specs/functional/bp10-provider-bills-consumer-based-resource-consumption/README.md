<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / BP10 - Provider bills Consumer based on resource consumption
</div>

# BP10 - Provider bills Consumer based on resource consumption

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/bp10-provider-bills-consumer-based-resource-consumption](https://simpl-programme.ec.europa.eu/book-page/bp10-provider-bills-consumer-based-resource-consumption)

## Overview

This business process covers the billing of a _Consumer_ by a _Provider_ for the consumption of a resource. The resource description published in the catalogue carries a billing schema, which holds:

- **Billing model** - how the _Consumer_ will be charged.
- **Billing unit** - the basis of measurement, for example MB transferred or number of data transfers.
- **Billing schedule** - the trigger for billing, either time-based or consumption-based.
- **Payment actions** - the actions a _Provider_ takes when the _Consumer_ does not comply with the agreed credit terms.

Both the _Provider_ and the _Consumer_ formalise their commitment to the billing process through the usage contract. The process applies within a data space and across federated data spaces.

During consumption, billing units are measured for pay-per-use models. When the billing trigger fires, the billing data is prepared from the billing schema, the _Consumer_ and _Provider_ information and the reference to the usage contract. That data is stored in the Simpl-Open agent of both parties for auditing. Invoice generation itself is outside the scope of Simpl-Open.

A _Provider_ may connect their own billing system to track credit status. Non-compliance triggers notifications and the contractual actions agreed in the usage contract, for example suspension. Those actions are described in BP11.

![BP10 figure 1](./media/bp10-figure-1.jpg)
*BP10 figure 1 - detailed level view (source page)*

### Relationship with other business processes

- [BP02 - Configuration of data space Governance Authority](../bp02-configuration-governance-authority/README.md) - the data space must be configured with its vocabulary and schemas.
- [BP03A - Onboarding of a new data space participant](../bp03a-onboarding-participant-providers/README.md) - the Participant must be onboarded.
- [BP05B - Provider manages resource descriptions](../bp05b-provider-manages-resource-descriptions/README.md) - the _Provider_ defines the contract clause holding the billing schema.
- [BP07 - Consumer and Provider establish a usage contract](../bp07-establish-usage-contract/README.md) - the usage contract must be active.
- [BP12B - Single node logging & monitoring](../bp12b-single-node-logging-monitoring/README.md) - the consumption logs must be captured and available.
- **BP11 - contract enforcement** - named by step BP10.05 and by the diagram. **No BP11 page exists in the published requirements book**, so this dependency cannot be followed to a source. See the source defects below.

## Actors

- _Provider_
- _Consumer_

## Assumptions

- The logging and monitoring information needed to generate the billing data is available to the _Provider_.
- The _Provider_ has an external billing system for invoices and payment follow-up.

## Prerequisites

- **Data space configured** - vocabulary and schemas are in place (BP02).
- **Participant onboarded** - the Participant is successfully onboarded (BP03A).
- **Billing schema defined** - the _Provider_ has defined the contract clause holding the billing schema (BP05B).
- **Usage contract active** - an active usage contract is established for the resource (BP07).
- **Consumption logs available** - consumption of the resource is logged and the data is available (BP12B).

## Process steps

### Trigger - billing

A time-based or consumption-based trigger initiates the calculation of the bill.

### BP10.01 Prepare and store billing data

The _Provider_ prepares the billing data from the billing schema, the _Consumer_ and _Provider_ information and the reference to the usage contract, and stores it for auditability.

### BP10.02 Make billing data available to the Provider's billing system

The _Provider_ makes the billing data available to their own billing system for the generation of the invoice. Invoice generation is outside the scope of Simpl-Open.

### BP10.03 Send billing data to the Consumer

The _Provider_ sends the prepared billing data to the _Consumer_.

### BP10.04 Store the Consumer's billing data

The _Consumer_ stores the received billing data for auditability and traceability.

### BP10.05 Contract enforcement (BP11)

The _Provider_ checks the credit status and takes the follow-up actions agreed in the contract, for example suspension, notifying both parties.

## High-level requirements

| ID | Title | Canonical source |
|----|-------|------------------|
| 10.1 | A Provider manages the billing process of their usage contract. | [101-…](https://simpl-programme.ec.europa.eu/book-page/101-provider-manages-billing-process-their-usage-contract) |
| 10.2 | A Consumer keeps track of the billing data of their usage contract. | [102-…](https://simpl-programme.ec.europa.eu/book-page/102-consumer-keeps-track-billing-data-their-usage-contract) |

## Outcomes

Upon successful completion of this business process:

- **Billing data stored** - the billing data is stored by both the _Provider_ and the _Consumer_ for auditability.
- **Billing data available for invoicing** - the billing data is made available to the _Provider_'s billing system for the creation of the invoice.
- **Usage contract enforced** - the usage contract is enforced, with notifications sent and follow-up actions executed.

## Source page metadata

- **Author:** Yevheniia Tkachova
- **Published:** 25 September 2026
- **Status on source site:** not shown on the captured page
- **Snapshot taken:** 25 September 2026

## Source defects observed at capture (25 September 2026)

Recorded so this mirror can be reconciled when the source page is corrected.

1. **BP11 is referenced but does not exist.** Step BP10.05 and the diagram both name BP11 for contract enforcement. The published requirements book has no BP11 page, so the step that enforces the contract points at nothing. Same shape as the BP02E reference that BP14 carried at its capture.
2. **BP10 is missing from the public business processes overview page.** The page exists in the book navigation on [Simpl Requirements](https://simpl-programme.ec.europa.eu/book-page/simpl-requirements) but not on the [overview index](https://simpl-programme.ec.europa.eu/documentation/requirements/business-processes-overview), so it has no published icon. That is why there is no BP10 tile in the parent README.
3. **No changelog entry.** The [requirements changelog](https://simpl-programme.ec.europa.eu/page/requirements-changelog) has no entry for BP10. The most recent entry is 24 September 2026 for the monetisation additions.
4. **Unpadded process references** in the prerequisites on the source page: BP2, BP3A, BP5B, BP7. Normalised here to BP02, BP03A, BP05B, BP07 to match the rest of the book.
5. **Diagram file named B10** rather than BP10, and the caption reads "B10 Detailed level view".
