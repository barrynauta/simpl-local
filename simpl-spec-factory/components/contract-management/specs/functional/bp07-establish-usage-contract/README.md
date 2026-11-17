<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / BP07 — Consumer and Provider establish a usage contract
</div>

# BP07 – Consumer and Provider establish a usage contract for selected catalogue items

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/bp07-consumer-and-provider-establish-usage-contract-selected-catalogue-items](https://simpl-programme.ec.europa.eu/book-page/bp07-consumer-and-provider-establish-usage-contract-selected-catalogue-items)

> **See also: [Dynamic view](./bp07-dynamic-view.md)** — sequence diagram showing how this business process executes at runtime, with links to each participating solution.

## Overview

This business process addresses scenarios where a _Consumer_ seeks to access data, application, or infrastructure resources from a _Provider_. Both parties must establish a legally binding usage contract defining rights, limits, and obligations of both parties — including pricing, usage limits, data retention policies, and service level agreements (SLAs).

Usage contracts are established in two scenarios:

1. **New resource consumption** — whenever a _Consumer_ wants to access a new resource from a _Provider_.
2. **Usage contract expiration** — usage contracts are immutable, so a new contract must be created when an existing one expires.

It includes the following main steps:

- **Request usage contract offer**
- **Verify access policy**
- **Verify existence of usage contract**
- **Provide usage contract**
- **Review terms and conditions of usage contract**
- **Sign usage contract** (Consumer)
- **Validate usage contract**
- **Sign usage contract** (Provider)

> **Note:** While the possibility of usage contract term negotiation has been discussed, it is not a current requirement in this process.

## Actors

- _Consumer_
- _Provider_ — Infrastructure, Application, or Data Provider

## Assumptions

- The _Consumer_ requests a usage contract based on the selection of a desired resource from the data space catalogue.

## Prerequisites

- **Dataspace is configured** — the _Governance Authority_ has configured the data space catalogue with the corresponding vocabulary and schemas (BP02).
- **End-User authentication and authorisation** — the End-User must be authenticated and possess the necessary roles and permissions to execute the process steps (BP03B).
- **Resource description is present in the data space catalogue** — a resource description must be published in the catalogue (BP05B).

![BP07 figure 1](./media/bp07-figure-1.png)
*BP07 figure 1 — overview diagram*

![BP07 figure 2](./media/bp07-figure-2.png)
*BP07 figure 2 — detailed business process diagram*

## Process steps

### BP07.01 Request usage contract offer

The _Consumer_ requests a usage contract based on their selection of a desired resource from the data space catalogue (BP06).

### BP07.02 Verify access policy

The _Provider_ verifies the _Consumer_ description details (as described in their self-description) to ensure that they are allowed to access the requested resource according to the access policies.

### BP07.03 Verify existence usage contract

The _Provider_ verifies whether the _Consumer_ already has an established usage contract in place for the desired resource from the data space catalogue.

### BP07.04 Provide usage contract

The _Provider_ creates a formal usage contract in response to the usage contract request from the _Consumer_.

### BP07.05 Review terms and conditions of usage contract

The _Consumer_ reviews the usage contract and decides whether to agree to the terms and conditions.

### BP07.06 Sign usage contract

If the _Consumer_ agrees to the terms and conditions, they sign the usage contract.

### BP07.07 Store usage contract

The _Consumer_ securely stores the finalised usage contract as part of their official business records for future reference, compliance, and operational needs.

### BP07.08 Validate usage contract

The _Provider_ validates that the _Consumer_ has formally agreed to the usage contract, ensuring they have accepted the terms and conditions established by the _Provider_.

### BP07.09 Sign usage contract

If the validation is successful, the _Provider_ signs the usage contract.

### BP07.10 Store usage contract

The _Provider_ securely stores the finalised usage contract as part of their official business records for future reference, compliance, and operational needs.

### BP07.11 Notify Consumer

The _Provider_ notifies the _Consumer_ that they have signed and stored the usage contract. The signed usage contract is made available to the _Consumer_.

### BP07.12 Confirm usage contract

The _Consumer_ confirms the signed usage contract from the _Provider_ against the original usage contract request (from BP07.01), to ensure consistency and accuracy.

## High-level requirements

| ID | Title | Canonical source |
|----|-------|------------------|
| 7.1 | A Consumer requests a new usage contract offer. | [71-…](https://simpl-programme.ec.europa.eu/book-page/71-consumer-requests-new-usage-contract-offer) |
| 7.2 | A Provider verifies the request for a usage contract offer. | [72-…](https://simpl-programme.ec.europa.eu/book-page/72-provider-verifies-request-usage-contract-offer) |
| 7.3 | A Provider creates and provides the usage contract offer. | [73-…](https://simpl-programme.ec.europa.eu/book-page/73-provider-creates-and-provides-usage-contract-offer) |
| 7.4 | A Consumer reviews the usage contract offer. | [74-…](https://simpl-programme.ec.europa.eu/book-page/74-consumer-reviews-usage-contract-offer) |
| 7.5 | A Consumer signs, stores and provides the usage contract offer. | [75-…](https://simpl-programme.ec.europa.eu/book-page/75-consumer-signs-stores-and-provide-usage-contract-offer) |
| 7.6 | A Provider validates the signed usage contract offer. | [76-…](https://simpl-programme.ec.europa.eu/book-page/76-provider-validates-signed-usage-contract-offer) |
| 7.7 | A Provider signs, stores and provides the usage contract to the Consumer. | [77-…](https://simpl-programme.ec.europa.eu/book-page/77-provider-signs-stores-and-provides-usage-contract-consumer) |
| 7.8 | A Consumer validates the signed usage contract. | [78-…](https://simpl-programme.ec.europa.eu/book-page/78-consumer-validates-signed-usage-contract) |


### Detailed requirements

- [7.1 — A Consumer requests a new usage contract offer](./bp71-consumer-requests-new-usage-contract-offer.md)
- [7.2 — A Provider verifies the request for a usage contract offer](./bp72-provider-verifies-request-usage-contract-offer.md)
- [7.3 — A Provider creates and provides the usage contract offer](./bp73-provider-creates-and-provides-usage-contract-offer.md)
- [7.4 — A Consumer reviews the usage contract offer](./bp74-consumer-reviews-usage-contract-offer.md)
- [7.5 — A Consumer signs, stores and provide the usage contract offer](./bp75-consumer-signs-stores-and-provide-usage-contract-offer.md)
- [7.6 — A Provider validates the signed usage contract offer](./bp76-provider-validates-signed-usage-contract-offer.md)
- [7.7 — A Provider signs, stores and provides the usage contract to the Consumer](./bp77-provider-signs-stores-and-provides-usage-contract-consumer.md)
- [7.8 — A Consumer validates the signed usage contract](./bp78-consumer-validates-signed-usage-contract.md)

## Outcomes

- **Usage contract is established** — if all steps are completed successfully, the process is finalised and the usage contract is considered active. Both parties are notified of the successful completion of the process.
- **No usage contract established** — if any step fails or is rejected, the process is terminated and no usage contract is established. Both parties are notified of the termination and the reason. This may occur in the following cases:
  - Access policy restriction
  - Existing usage contract
  - Terms and conditions not accepted
  - Usage contract not validated
