> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14865](../../README.md) / [Epic-SIMPL-17800](../README.md) / **Story-SIMPL-21594**

# SIMPL-21594: [API] The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14865](../../README.md) (L2) › [SIMPL-17800](../README.md) (Epic) › **SIMPL-21594** (User story)

## Text

#### Storycard

**As** Governance Authority\
**I want to** notify Providers about a wrong version number of a new version of a resource description\
**So that** the Provider can correct the error.

#### Acceptance criteria

1. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **And** I have received a new resource description or a new version of a resource description with an incorrect version number\
   **When** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "invalid".\
   **Then** a notification about an version numbering error is sent to the Provider.

#### Technical details

The version validation mechanism shall be implemented in a configurable and extensible manner so that the versioning strategy can be changed or replaced.

Approach is described here:

(internal page)

A http response with a suitable error code and a meaningful error description is sufficient.

API: [https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads)

Endpoint: POST /selfDescriptions/resource

Validations:

- incremental versioning - if latest version is 5 and I get 7 - REJECT
- numeric versioning - reject strings or other formats
- reject negative numbers

HTTP Status Code for successful validation: 201

HTTP Status Code for negative scenarios: 422

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17800](../README.md) (Epic) The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description |
