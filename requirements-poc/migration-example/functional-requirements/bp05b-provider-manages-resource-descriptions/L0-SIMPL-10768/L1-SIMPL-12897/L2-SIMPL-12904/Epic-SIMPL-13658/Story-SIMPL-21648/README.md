> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12904](../../README.md) / [Epic-SIMPL-13658](../README.md) / **Story-SIMPL-21648**

# SIMPL-21648: [API] The Governance authority provides the version history of a resource description via API (Query mapper)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12904](../../README.md) (L2) › [SIMPL-13658](../README.md) (Epic) › **SIMPL-21648** (User story)

## Text

#### Storycard

**As** Governance Authority\
**I want to** provide the version history of one of a Provider's own resource description upon request via API\
**So that** they can review it and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** The Governance authority provides the version history of a resource description via API - positive case\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description was published by the requesting Provider\
then the version history of this resource description is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: unauthorized\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is not authorised with the identity attributes to manage resource descriptions\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: not own resource description\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description was not published by the requesting Provider\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: resource description not existing\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description does not exist\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: not acceptable\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the request is invalid\
then an error message is returned.

**Scenario:** The Governance authority provides the version history of a resource description via API - negative case: internal server error\
When I, the Governance Authority, receive a request for the version history of a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and there is an internal server error\
then an error message is returned.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-13658](../README.md) (Epic) A Provider consults the version history of one of its own resource descriptions |
