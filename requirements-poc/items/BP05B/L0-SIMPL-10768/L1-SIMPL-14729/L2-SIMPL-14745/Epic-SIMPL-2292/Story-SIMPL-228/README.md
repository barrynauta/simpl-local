> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-228**

# SIMPL-228: [UI] Create UI frontend for XFSC catalogue - Dialog add resource

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-228** (User story)

## Text

'**As a** service provider **I want** publish a SD of a service resource **so that** my service offering can be looked up and queried by Consumers.

**Linked Sovereign X US ID:** TAE_US_035, TAE_US_036, TAE_US_037, TAE_US_038

+**Technical specification:**+

Create UI frontend for XFSC catalogue to store and publish a SD in a catalogue.\
The UI should be able to import an sign JSON-LD document and with a button publish it to the catalog. The result of the publication (either success or in case of failure a message with the reason) should be shown to the provider. The front end also contains the feature to log in which is required to get the token to use the catalog backend.

+**Mock Up:**+\
[mock up docu|an internal page]\
(image not shown)

+**Acceptance Criteria:**+

**1. A service provider can store/publish a self description  of a service offering**

**2. Three different types of Service Offerings can be published (APP, Data and Infra)**

**3. Invalid SD are rejected**

**4. The Federated Catalogue accepts theses created SD**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
