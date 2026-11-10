> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-15289**

# SIMPL-15289: Automatically populate the provider id on the resource description

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-15289** (User story)

## Text

#### Storycard

| As a|Provider |
| --- |

| I want|that my technical provider identifier is automatically in a new resource description when creating it via UI |
| --- |

| so that|the resource description can be identified as my own. |
| --- |

#### Acceptance criteria

**Positive case**

| Given|I am authenticated as a provider |
| --- |

| and|I create a new resource description in the UI |
| --- |

| when|I submit the filled resource description to the backend |
| --- |

| Then|the provider ID is automatically set in the new resource description. |
| --- |

**Negative Case**

| Given|I am authenticated as a provider |
| --- |

| and|I create a new resource description in the UI |
| --- |

| when|I the provider ID cannot be retrieved and set automatically |
| --- |

| Then|I am not able to submit the resource description |
| --- |

| and|I receive a comprehensive error message |
| --- |

#### Technical Information

- On resource description we need to link the IAA provider-id on the resource description
- Relevant API's
   - (internal page) this is the page that describes how it works, note that the method - **GET /credential/my-id** ** **is replaced by** **GET /credentials** and the page will be updated ASAP.
   - (internal page)    see component #14 - Tier 2 Authentication Provider **Credential GET /credentials** method
- (internal page)  to get information on Identification, Authentication & Authorisatio

Properties in the schema to be set:

- `providedBy`

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
