> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-26083](../../../README.md) / [L2-SIMPL-27195](../../README.md) / [Epic-SIMPL-27207](../README.md) / **Story-SIMPL-32215**

# SIMPL-32215: [API Definition] Contract notifies Contract-UI about contract offer status change

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-26083](../../../README.md) (L1) › [SIMPL-27195](../../README.md) (L2) › [SIMPL-27207](../README.md) (Epic) › **SIMPL-32215** (User story)

## Text

- Why: We need to deliver contract agreement status change from provider to consumer
- What: As consumer side contract manager I want to be notified about contract agreement status changes, so I will be able to pass this information to contract-ui-backend to show it on contract list on UI.
- Acceptance Criteria: New endpoint will be added to contract API definition.
- Technical Info: Endpoint:\
   PUT: .../contractOffers/{contractNegotiationId}/update\
   with body like: {"status":&lt;new status&gt;,"additionalInfo":&lt;optional additional information&gt;}

**Review checklist:**

|   |   |   |   |
| --- | --- | --- | --- |
| **Rule #** | **Rule Description** | **Reviewed by &lt;insert reviewer firstname and surname&gt;** |  |
| 1 | The contract-first principle SHOULD be followed when developing an API. In this approach, the specifications of the REST API are created first and not generated from the code. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-ctr-first]. | (name removed) |  |
| 2 | Specifications of the API SHOULD be provided using [**OpenAPI 3.0** | https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md]. OpenAPI uses the [OpenAPI Schema Object | https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#schemaObject] to describe the JSON representation of resources, which is a variant of [**JSON Schema** | https://json-schema.org/specification-links.html#draft-5], with some significant incompatibilities. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-oas-contra]. For this specific guideline, Simpl-Open deviates from the BeglIF guidelines and only YAML is used as file type for the OpenAPI definition. | (name removed) |  |
| 3 | Path segments and query parameters within an API SHOULD use lowerCamelCase notation to enhance readability and to separate compound names. As lowerCamelCase is used for JSON property names as well, the casing is consistent throughout the API. Trailing slashes MUST NOT be used. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-uri-notat]. | (name removed) |  |
| 4 | The URI SHOULD NOT contain a file extension. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-uri-extens]. | (name removed) |  |
| 5 | A plural noun SHOULD be used for collection names, for example 'employers' or 'people'. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-col-name]. | (name removed) |  |
| 6 | The HTTP methods SHOULD be used that are appropriate for the type of action performed on the resource. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-meth-http]. | (name removed) |  |
| 7 | The HTTP response status code SHOULD be returned that best describes the outcome of the treatment of the request. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-stat-codes]. | (name removed) |  |
|8|Following guidelines SHOULD be respected when determining a name for a JSON property:
·       use *lowerCamelCase* notation;\
·       also for abbreviations: all letters after the first should be lowercase.\
·       use American English;\
·       do not use underscores (_), hyphens  or dots (.) in a property name, nor use a digit as first letter;\
·       don't use overly generic terms like info(rmation) and data as property name or as part of it;\
·       the name should refer to the business meaning of its value in relation to the object in which it is specified, rather than how it is defined;\
·       omit parts of the name that are already clear from the context.\
Properties used from other standards, like OpenID Connect and OAuth2, are allowed to deviate from this rule. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-jsn-naming).|(name removed)| |
| 9 | Add example response values to the OpenAPI specification under the examples property. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-oas-exampl]. * Examples SHOULD contain realistic values. | (name removed) |  |
| 10 | Kebab-Case with uppercase SHOULD be used for HTTP header names. More details [here | https://www.belgif.be/specification/rest/api-guide/#rule-hdr-case]. | (name removed) |  |
|11|The API SHOULD match the following: **URI =** https://host/pathPrefix/apiName/vmajorVersion/resources. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-uri-format).
·       **[https://**](https://%2A/) - Services are at least secured on transport level using a one-way TLS connection. The implicit port is 443.\
·       **host** - Hostname is determined by the environment where the service is deployed\
·       **pathPrefix** - Optional path prefix to discriminate between REST APIs and other resources on the same host. (example: /REST)\
·       **apiName** - Name of the API that groups a functional consistent set of resources. The API name consists of one or multiple path segments and SHOULD be written in lowerCamelCase (example: /referenceData/geography)\
·       **majorVersion** - Major version numbers are integers and start at 1. See API versioning.\
·       **resources** - All path segments identifying the actual resources.|(name removed)| |\
|12|Resources should follow the following 3 resource archetypes. More details [here](https://www.belgif.be/specification/rest/api-guide/#resource-archetypes).
·       Document\
·       Collection\
·       Controller |(name removed)| |
| 13 | Resource name SHOULD NOT include ownership words (e.g. "own", "my", ...). | (name removed) |  |
| 14 | Details of a resource should be obtained either through "/resources/{resourceId}" or  "/resources/{resourceId}/details" if a distinction should be made. | (name removed) |  |
| 15 | The version string of an API SHOULD contain a major, minor and optionally a patch number. Only the major version of the API is part of the base URL (basePath in Swagger) of the API. Thereby, for a major version only a single minor and patch version can be available in production at a time. The version of the API MUST be included in the info section of the OpenAPI definition. More details [here | https://www.belgif.be/specification/rest/api-guide/#api-versioning]. | (name removed) |  |
|16|REST clients SHOULD specify the media type(s) that is(/are) acceptable for the response in the Accept HTTP header. A REST response or request with payload MUST include the Content-Type HTTP header to indicate the media type of the HTTP payload. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-cnt-negot). *
·       Status code 406 MUST be used to indicate that the requested media type could not be provided.\
·       Status code 415 MUST be used to indicate an unsupported media type in the request payload.\
·       If a resource supports multiple content types through negotiation, the response header Vary: Accept MUST be added to avoid undesired cache hits.|(name removed)| |\
|17|All security schemes used by the API SHOULD be defined in the global components/securitySchemes section. This section contains a list of named security schemes, where each scheme can be of type: *
·       http - for [Basic](https://swagger.io/docs/specification/authentication/basic-authentication/), [Bearer](https://swagger.io/docs/specification/authentication/bearer-authentication/) and other HTTP authentications schemes\
·       apiKey - for [API keys](https://swagger.io/docs/specification/authentication/api-keys/) and [cookie authentication](https://swagger.io/docs/specification/authentication/cookie-authentication/)\
·       oauth2 - for [OAuth 2](https://swagger.io/docs/specification/authentication/oauth2/)\
·       openIdConnect - for [OpenID Connect Discovery](https://swagger.io/docs/specification/authentication/openid-connect-discovery/)|(name removed)| |
| 18 | Security schemes SHOULD be applied them to the whole API or individual operations by adding the security section on the root level or operation level, respectively. | (name removed) |  |
|19|License information SHOULD be included in the top-level info object as follow:
openapi: 3.0.0\
info:\
license:\
name: European Union Public License (EUPL) 1.2\
url: [https://[interoperable-europe.ec]|http://interoperable-europe.ec/].europa.eu/sites/default/files/custom-page/attachment/eupl_v1.2_en.pdf|(name removed)| |\
|20|Contact information SHOULD be included in the top-level info object as follow:
openapi: 3.0.0\
info:\
contact:\
name: Simpl Programme\
email: [an email address|mailto:an email address]\
url: [https://[simpl-programme.ec]|http://simpl-programme.ec/].europa.eu/|(name removed)| |\
|21|For any problem occurred during processing, whether it be caused by the client or the server, the API provider supplies
a suitable status code: 4xx for consumer or 5xx for server errors\
a RFC 9457 problem detail messageMore details here.|(name removed)| |\
|22|A global API description SHOULD be included in the top-level info object as follow:
openapi: 3.0.0\
info:\
description: &lt; Clear description of the purpose of the API &gt;|(name removed)| |\
|23|Following naming guidelines should be applied when using an identifier or code in a REST API:
*

   - JSON property:
      - within an object that represents the entire or part of a resource: use id or code
      - to reference a resource within another one’s representation: property name should designate the relation between the resources; no need to suffix with id or code
   - path parameter: use id or code, OPTIONALLY prefixed with the resource type to disambiguate when there are multiple identifiers as path parameters
   - query search parameter: use same name as the property in the JSON resource representation of the response
   - OpenAPI type: add suffix Id or Code to distinguish it from the type of the full resource representation\
   As an exception, use the standardized name for the business identifier if one exists, rather than id or code.\
   If multiple identifiers or coding schemes may be used within the same context, a suffix can be added to the name to disambiguate.\
   More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-id-name).|(name removed)| |\
   |24|Parameters should be described by specifying name, **location** (in), **data type** (defined by either schema or content) and other attributes, such as **description** or required.{code:java}\
   // code placeholderpaths:\
   /users/{}{userId}:\
   get:\
   summary: Get a user by ID\
   parameters:
- in: path\
   name: userId\
   schema:\
   type: integer\
   required: true\
   description: Numeric ID of the user to get

```
While the format and allowed values are by default defined by the data type, if a parameter only allows certain set of values, it should be restricted by adding the *enum* to the parameter’s schema. The enum values must be of the same type as the parameter data type.{code:java}
parameters:
  - in: query
    name: status
    schema:
      type: string
      enum:
        - available
        - pending
        - sold

```
|25|Required request parameters should be explicitly marked as such using "required: true" keyword.
```
parameters:
  - in: path
    name: userId
    schema:
      type: integer
    required: true # <----------
    description: Numeric ID of the user to get.

```
Note that path parameters must have "required: true", because they are always required. |(name removed)| |

Link to API definition:\
[https://code.europa.eu/simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be/-/blob/feature/SIMPL-28596/openapi/openapi3-v1.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be/-/blob/feature/SIMPL-28596/openapi/openapi3-v1.yaml?ref_type=heads)

Flow diagram (only last scenario "Update contract offer status" applies to this API change)\
(image not shown)

CDM:\
(image not shown)

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27207](../README.md) (Epic) Data Provider notifies Consumers upon consent assertion status change |
