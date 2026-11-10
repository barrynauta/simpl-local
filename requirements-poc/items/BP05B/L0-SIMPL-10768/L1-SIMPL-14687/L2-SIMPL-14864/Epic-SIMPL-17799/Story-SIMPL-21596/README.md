> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14864](../../README.md) / [Epic-SIMPL-17799](../README.md) / **Story-SIMPL-21596**

# SIMPL-21596: [API] The Governance Authority checks the version number of a new version of a Resource description

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14864](../../README.md) (L2) › [SIMPL-17799](../README.md) (Epic) › **SIMPL-21596** (User story)

## Text

#### Storycard

| As|Governance Authority |
| --- |

| I want|to automatically check the version number of a new version of a resource description |
| --- |

| so that|the published resource descriptions are consistent, reliable and compliant with pre-defined standards. |
| --- |

scenarios to check for both - new resource description and new version of a resource description

- 201 -  correct version
- 422 - incorrect version

- 400 - Syntax / Parsing Error
- 403 - Unauthorized
- 500 - Internal Server Error

| Scenario | The Governance Authority checks the version number of a new version of a resource description, valid version number (Version number validation - positive case) |
| --- | --- |

| Given |a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority |
| --- |

| When|I, the Governance Authority, receive a new version of a resource description with a correct version number |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| and|the version numbering algorithm checks the version number of the new version of the Resource description |
| --- |

| Then|Simpl-Open should return with the positive publishing result with the code "201" |
| --- |

| and|the new resource description or the new version of the resource description is created |
| --- |
|  |  |

| Scenario | The Governance Authority checks the version number of a new version of a resource description, invalid version number (Version number validation - negative case) |
| --- | --- |

| Given |a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority |
| --- |

| When|I, the Governance Authority, receive a new version of a resource description with an incorrect version number |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| and|the version numbering algorithm checks the version number of the new version of the Resource description |
| --- |

| Then|Simpl-Open should return the verification results and the details together with error code "422" |
| --- |

| and|no new resource description or new version of the resource description is created |
| --- |
|  |  |
|  |  |

| Scenario | The Governance Authority checks the version number of a new version of a resource description, Provider is not authorized (Version number validation - negative case) |
| --- | --- |

| Given |a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority |
| --- |

| When|I, the Governance Authority, receive a new version of a resource description with an incorrect version number |
| --- |

| and|the Provider is not authorised with identity attributes to manage resource descriptions |
| --- |

| Then|Simpl-Open should return with error code 403 to indicate the operation is not authorized |
| --- |

| and|no new resource description or new version of the resource description is created |
| --- |
|  |  |

| Scenario|**The Governance Authority checks the schema for a new resource description or a new version of a resource description with an invalid JSON syntax (Schema status validation - negative case)** |
| --- |

| Given |a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority |
| --- |

| When|I, the Governance Authority, receive a new version of a resource description with a correct version number |
| --- |

| and|the Provider is not authorised with identity attributes to manage resource descriptions |
| --- |

| and|the payload has invalid JSON syntax |
| --- |

| `Then`|`Simpl-Open should return with HTTP status 400 Bad Request to indicate the syntax or parsing error` |
| --- |

| and|no new resource description or new version of the resource description is created |
| --- |
|  |  |

| Scenario|**The Governance Authority checks the schema for a new resource description or a new version of a resource description and an unexpected failure happens during creation (Schema status validation - negative case)** |
| --- |

| Given |a version numbering algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority |
| --- |

| When|I, the Governance Authority, receive a new version of a resource description with a correct version number |
| --- |

| and|the Provider is authorised with identity attributes to manage resource descriptions |
| --- |

| and|An unexpected internal server error occurs during processing |
| --- |

| Then|Simpl-Open should return with  HTTP status `500 Internal Server Error` |
| --- |

| and|no resource description or a new version of the resource description is created |
| --- |

| | |
| --- |

#### Technical details

The version validation mechanism shall be implemented in a configurable and extensible manner so that the versioning strategy can be changed or replaced.

Approach is described here:

(internal page)

A http response with a suitable error code and a meaningful error description is sufficient.

API: [https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads)

Endpoint: POST /selfDescriptions/resource

Validations:

- incremental versioning - if latest version is 5 and I get 7 - REJECT
- numeric versioning - reject strings or other formats
- reject negative numbers

HTTP Status Code for successful validation: 201

HTTP Status Code for negative scenarios: 422

Update the OpenAPI File to reflect the Error Codes for status code 422 for this API: SIMPL-25210

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17799](../README.md) (Epic) The Governance Authority checks the version number of a new version of a Resource description |
