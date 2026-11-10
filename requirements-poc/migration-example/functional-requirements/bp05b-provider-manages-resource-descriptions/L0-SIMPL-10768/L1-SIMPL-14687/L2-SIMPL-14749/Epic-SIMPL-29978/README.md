> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14749](../README.md) / **Epic-SIMPL-29978**

# SIMPL-29978: The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description [Extension Consent]

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14749](../README.md) (L2) › **SIMPL-29978** (Epic)

## Text

This extension aims to validate the resource description schema related to contract clause of the type 'Consent':

- If an access policy of type resource consent is assigned to the resource description, then there needs to be a contract clause of type consent to cover the resource consent assertion status changes. Optionally, a second contract clause of type consent to cover intended purpose can exist.
- If  an access policy of type contract consent is assigned to the resource description, then there needs to be a contract clause of type consent to cover the contract consent assertion status changes.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14749](../README.md) (L2) The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description |
| **Children** | None. |
