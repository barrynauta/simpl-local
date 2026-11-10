> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12898](../../README.md) / [Epic-SIMPL-12899](../README.md) / **Story-SIMPL-18524**

# SIMPL-18524: Using identity attributes in the API to provide a list of a provider's own resource descriptions

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12898](../../README.md) (L2) › [SIMPL-12899](../README.md) (Epic) › **SIMPL-18524** (User story)

## Text

#### Storycard

**As** Governance Authority\
**I want to** verify if a request to retrieve the list of own resource descriptions by a provider is authorised, based on the included identity attributes\
**So that** I can ensure that I provide the information only to authorised providers

#### Acceptance criteria

The tier 2 identity attributes authorising the management of Resource descriptions are:

- DATA_PROVIDER_PUBLISHER
- APP_PROVIDER_PUBLISHER
- INFRA_PROVIDER_PUBLISHER

When I, the Governance Authority, receive a request from a Provider to retrieve a list of their own Resource descriptions\
then I, the Governance Authority, check the authentication\
and I, the Governance Authority, check that  the identity attributes authorise the management of Resource descriptions.

#### Additional notes

For more explanations regarding tier 2 identification, authentication and authorisation see (internal page).

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12899](../README.md) (Epic) A Provider consults an overview of its Resource descriptions via UI and API |
