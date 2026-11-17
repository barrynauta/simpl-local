<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
<p>
📍 <strong>You are here</strong> / <a href="../README.md">🏠 Home</a> / <a href="README.md">Foundations</a> / API Guidelines
</p>
</div>

[[_TOC_]]
# API Guidelines

## Data Modelling

Every API definition (REST or event-driven) MUST be accompanied by a [Common Data Model (CDM)](https://code.europa.eu/simpl/simpl-open/foundations/architecture/-/blob/master/data-modeling-guidelines.md?ref_type=heads). 
The CDM provides the canonical definition of the business entities exchanged by the API. 
OpenAPI Schema Objects and AsyncAPI message payloads SHOULD reference the CDM rather than redefining types locally, 
to ensure consistency across components.

## API Types
Simpl-Open implements the following types of API:

- Synchronous REST APIs
- Event-driven APIs

The following sections describe for each type the guidelines to be followed.

## Synchronous REST APIs

For custom-built REST APIs, the following guidelines from the Belgian Interoperability Framework are used as reference:
[https://www.belgif.be/specification/rest/api-guide](https://www.belgif.be/specification/rest/api-guide/)

Specifically, the following guidelines will be applied in priority:

- The **contract-first principle** SHOULD be followed when developing an API. In this approach, the specifications of the REST API are created first and not generated from the code. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-ctr-first).
- Specifications of the API SHOULD be provided using [**OpenAPI 3.0**](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md). OpenAPI uses the [OpenAPI Schema Object](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#schemaObject) to describe the JSON representation of resources, which is a variant of [JSON Schema](https://json-schema.org/specification-links.html#draft-5), with some significant incompatibilities. More details  [here](https://www.belgif.be/specification/rest/api-guide/#rule-oas-contra). 

For this specific guideline, Simpl-Open deviates from the BelgIF guidelines and only **YAML** is used as
file type for the OpenAPI definition.
- Path segments and query parameters within an API SHOULD use **lowerCamelCase** notation to enhance readability and to separate compound names. As lowerCamelCase is used for JSON property names as well, the casing is consistent throughout the API. **Trailing slashes MUST NOT be used.** More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-uri-extens).
- The URI SHOULD NOT contain a file extension. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-col-name)
- A **plural noun** SHOULD be used for collection names, for example `employers` or `people`. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-col-name).
- The HTTP methods SHOULD be used that are appropriate for the type of action performed on the resource. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-meth-http).
- The HTTP response status code SHOULD be returned that best describes the outcome of the treatment of the request. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-stat-codes).

### JSON property naming

Following guidelines SHOULD be respected when determining a name for a JSON property:

- use **lowerCamelCase** notation;
  - also for abbreviations: all letters after the first should be lowercase.
- use **American English**;
- do not use underscores (`_`), hyphens (`-`) or dots (`.`) in a property name, nor use a digit as first letter;
- don't use overly generic terms like `info(rmation)` and `data` as property name or as part of it;
- the name should refer to the **business meaning** of its value in relation to the object in which it is specified, rather than how it is defined;
- omit parts of the name that are already clear from the context.

Properties used from other standards, like OpenID Connect and OAuth2, are allowed to deviate from this rule. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-jsn-naming).

### Examples and headers

- Add example request and response values to the OpenAPI specification under the `examples` property. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-oas-exampl).
- Examples SHOULD contain realistic values.
- **Kebab-Case with uppercase** SHOULD be used for HTTP header names. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-hdr-case).

### URI structure

The API SHOULD match the following:

```
URI = https://host/pathPrefix/apiName/vmajorVersion/resources
```

More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-uri-format).

| Part | Description |
|---|---|
| `https://` | Services are at least secured on transport level using a one-way TLS connection. The implicit port is 443. |
| `host` | Hostname is determined by the environment where the service is deployed. |
| `pathPrefix` | Optional path prefix to discriminate between REST APIs and other resources on the same host (example: `/REST`). |
| `apiName` | Name of the API that groups a functional consistent set of resources. The API name consists of one or multiple path segments and SHOULD be written in lowerCamelCase (example: `/referenceData/geography`). |
| `majorVersion` | Major version numbers are integers and start at 1. See API versioning. |
| `resources` | All path segments identifying the actual resources. |

### Resource archetypes

Resources should follow the following 3 resource archetypes (more details [here](https://www.belgif.be/specification/rest/api-guide/#resource-archetypes)):

- Document
- Collection
- Controller

Resource name SHOULD NOT include ownership words (e.g. `own`, `my`, ...). Exceptions are to be made when the said 
ownership word is part of a technical term (e.g. `selfDescription`).

Details of a resource should be obtained either through `/resources/{resourceId}` or `/resources/{resourceId}/details` if
a distinction should be made.

### Versioning

The version string of an API SHOULD contain a major, minor and optionally a patch number. Only the major version of the
API is part of the base URL (`basePath` in Swagger) of the API. Thereby, for a major version only a single minor and patch
version can be available in production at a time. The version of the API MUST be included in the `info` section of the
OpenAPI definition. More details [here](https://www.belgif.be/specification/rest/api-guide/#api-versioning).

### Content negotiation

- REST clients SHOULD specify the media type(s) that is(/are) acceptable for the response in the `Accept` HTTP header. A REST response or request with payload MUST include the `Content-Type` HTTP header to indicate the media type of the HTTP payload. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-cnt-negot).
- Status code **406** SHOULD be used to indicate that the requested media type could not be provided.
- Status code **415** SHOULD be used to indicate an unsupported media type in the request payload.
- If a resource supports multiple content types through negotiation, the response header `Vary: Accept` MUST be added to avoid undesired cache hits.

### Security schemes

All security schemes used by the API SHOULD be defined in the global `components/securitySchemes` section. 
This section contains a list of named security schemes, where each scheme can be of type:

- `http` - for [Basic](https://swagger.io/docs/specification/authentication/basic-authentication/), [Bearer](https://swagger.io/docs/specification/authentication/bearer-authentication/) and other HTTP authentication schemes
- `apiKey` - for [API keys](https://swagger.io/docs/specification/authentication/api-keys/) and [cookie authentication](https://swagger.io/docs/specification/authentication/cookie-authentication/)
- `oauth2` - for [OAuth 2](https://swagger.io/docs/specification/authentication/oauth2/)
- `openIdConnect` - for [OpenID Connect Discovery](https://swagger.io/docs/specification/authentication/openid-connect-discovery/)

Security schemes SHOULD be applied to the whole API or individual operations by adding the `security` 
section on the root level or operation level, respectively.

### License and contact info

License information SHOULD be included in the top-level `info` object as follows:

```yaml
  openapi: 3.0.0
  info:
      license:
          name: European Union Public License (EUPL) 1.2
          url: https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/eupl_v1.2_en.pdf
```

Contact information SHOULD be included in the top-level `info` object as follows:

```yaml
  openapi: 3.0.0
  info:
      contact:
          name: Simpl Programme
          email: cnect-simpl@ec.europa.eu
          url: https://simpl-programme.ec.europa.eu/
```

### Error handling

For any problem occurred during processing, whether it be caused by the client or the server, the API provider supplies:

- a suitable status code: `4xx` for consumer or `5xx` for server errors
- a **RFC 9457** problem detail message

Problem type SHOULD be specified as a URN in one of the following formats:

- `urn:problem-type:simpl:<api>:<type>`
- `urn:problem-type:simpl:<type>`
- `urn:problem-type:simpl:<type>:<specificType>`

with:

- `<api>`: name of the API. It MAY be used for API-specific problem types
- `<type>`: type of the problem in lowerCamelCase

More details [here](https://www.belgif.be/specification/rest/api-guide/#problem-details).

### API description

A global API description SHOULD be included in the top-level `info` object as follows:

```yaml
  openapi: 3.0.0
  info:
      description: < Clear description of the purpose of the API >
```

This description should clearly describe the purpose of the API from a business perspective.

### Identifiers and codes

Following naming guidelines should be applied when using an identifier or code in a REST API:

- **JSON property:**
  - within an object that represents the entire or part of a resource: use `id` or `code`
  - to reference a resource within another one's representation: property name should designate the relation between the resources; no need to suffix with `id` or `code`
- **Path parameter:** use `id` or `code`, OPTIONALLY prefixed with the resource type to disambiguate when there are multiple identifiers as path parameters
- **Query search parameter:** use same name as the property in the JSON resource representation of the response
- **OpenAPI type:** add suffix `Id` or `Code` to distinguish it from the type of the full resource representation

As an exception, use the standardized name for the business identifier if one exists, rather than `id` or `code`.

If multiple identifiers or coding schemes may be used within the same context, a suffix can be added to the name to
disambiguate. More details [here](https://www.belgif.be/specification/rest/api-guide/#rule-id-name).

### Parameters

Parameters should be described by specifying `name`, location (`in`), data type (defined by either `schema` or `content`)
and other attributes, such as `description` or `required`.

```yaml
  paths:
    /users/{userId}:
      get:
        summary: Get a user by ID
        parameters:
          - in: path
            name: userId
            schema:
              type: integer
            required: true
            description: Numeric ID of the user to get
```

While the format and allowed values are by default defined by the data type, if a parameter only allows a certain set of
values, it should be restricted by adding the `enum` to the parameter's schema. The enum values must be of the same type
as the parameter data type.

```yaml
  parameters:
    - in: query
      name: status
      schema:
        type: string
        enum:
          - available
          - pending
          - sold
```

Required request parameters should be explicitly marked as such using the `required: true` keyword.

```yaml
  parameters:
    - in: path
      name: userId
      schema:
        type: integer
      required: true # <----------
      description: Numeric ID of the user to get.
```

> **Note:** path parameters must have `required: true`, because they are always required.

---

## Event-driven APIs

These guidelines provide a standardized approach for integrating microservices within the Simpl-Open ecosystem using
**AsyncAPI** and **CloudEvents**. They outline how to describe, publish, and consume events to ensure interoperability and
consistency across all contributors.

The purpose of these guidelines is to:

- Standardize event-based communication across contributors.
- Promote interoperability between microservices.
- Facilitate code generation and distributed tracing.

### AsyncAPI Definitions

Every microservice offering an event-driven API must provide an **AsyncAPI** file describing its interaction with the
Kafka-based messaging system. This file acts as a contract, outlining:

- **Channels** (Kafka topics) the microservice publishes to or subscribes from.
- **Operations** (publish or subscribe) associated with each channel.
- **Messages** that are exchanged on each channel.

> Use [AsyncAPI Studio](https://studio.asyncapi.com/) for preview.

### Code Generation

[Modelina](https://github.com/asyncapi/modelina) is a JavaScript tool provided by AsyncAPI to generate data models from
AsyncAPI inputs.

To include Modelina code generation into a Maven project, configure the
[frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin) to generate the code during the
`generate-sources` Maven phase.

**Example — Frontend Maven Plugin with Modelina code generation:**

```xml
  <plugin>
      <groupId>com.github.eirslett</groupId>
      <artifactId>frontend-maven-plugin</artifactId>
      <version>${frontend-maven-plugin.version}</version>
      <configuration>
          <nodeVersion>${node.version}</nodeVersion>
      </configuration>
      <executions>
          <execution>
              <id>install node and npm</id>
              <goals>
                  <goal>install-node-and-npm</goal>
              </goals>
          </execution>
          <execution>
              <id>npm install</id>
              <goals>
                  <goal>npm</goal>
              </goals>
          </execution>
          <execution>
              <id>npm asyncapi-generate authenticationprovider</id>
              <goals>
                  <goal>npm</goal>
              </goals>
              <phase>generate-resources</phase>
              <configuration>
                  <arguments>run asyncapi-generate -- java
  ${basedir}/src/main/resources/asyncapi/authenticationprovider/v1/asyncapi.yaml --javaArrayType=List --javaIncludeComments
  --packageName=eu.europa.ec.simpl.events.authenticationprovider.v1 -o ${project.build.directory}/generated-sources/asyncapi
  /src/main/java/eu/europa/ec/simpl/events/authenticationprovider/v1</arguments>
              </configuration>
          </execution>
      </executions>
  </plugin>
```

### CloudEvents Conventions

Microservices must follow [CloudEvents](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md) conventions
when exchanging messages and follow these guidelines:

- Use **Kafka binary content mode** (CloudEvents fields in Kafka message headers).
- The Kafka message headers must include all required CloudEvents fields.
- The Kafka message headers should include the `time` field containing the timestamp of the event.

The following attributes are required in Simpl-Open messages. Please refer to the official CloudEvents specification for
more details.

| Attribute | Type | Description |
|---|---|---|
| `id` | String | The unique identifier of the message in UUID v7 format. |
| `source` | String | The identifier of the application producing the event (e.g. `eu.europa.ec.simpl.events.authenticationprovider`). |
| `type` | String | The event type and version (e.g. `eu.europa.ec.simpl.events.authenticationprovider.v1.CredentialUpdatedEvent`). |
| `specversion` | String | The version of the CloudEvents spec. Must be set to `application/json`. |
| `datacontenttype` | String | The data content type. Must be set to `application/json`. |
| `time` | Timestamp | Timestamp of when the occurrence happened. |

In the future, distributed tracing fields (`traceparent` and `tracestate`) may be introduced to correlate events across
services. Microservices should be designed to accommodate these headers when they become available.

To promote harmonisation across all Simpl-Open services, the CloudEvent message headers are externalised into a dedicated
YAML file, stored centrally on GitLab. A reference to this file is then used in the `traits` field of each message
definition, as depicted in the example below.

**AsyncAPI example with external link to CloudEvents headers:**

```yaml
  asyncapi: 3.0.0
  info:
    title: Authentication Provider Async API
    version: 1.0.0
    description: "The Authentication Provider component publishes events related to the Tier 2 status of the agent, such as
  credential update or deletion and changes to the assigned identity attributes."
    license:
      name: European Union Public License (EUPL) 1.2
      url: https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/eupl_v1.2_en.pdf
    contact:
      name: Simpl Programme
      url: https://simpl-programme.ec.europa.eu/
      email: cnect-simpl@ec.europa.eu
    tags:
      - name: Credentials
        description: Messages related to the credentials of the agent
      - name: Identity Attributes
        description: Messages related to the identity attributes of the agent
  servers:
    kafka:
      host: "{host}:{port}"
      protocol: kafka
  defaultContentType: application/json
  channels:
    credentialUpdatedEvent:
      address: '{prefix}authentication-provider.events.credentialUpdated'
      messages:
        credentialUpdatedEvent:
          $ref: '#/components/messages/credentialUpdatedEvent'
      description: The channel on which credential update events will be published.
      parameters:
        prefix:
          $ref: '#/components/parameters/prefix'
    assignedIdentityAttributesUpdatedEvent:
      address: '{prefix}authentication-provider.events.identity-attributes-changed'
      messages:
        assignedIdentityAttributesUpdatedEvent:
          $ref: '#/components/messages/assignedIdentityAttributesUpdatedEvent'
      description: The channel on which identity attribute update events will be published.
      parameters:
        prefix:
          $ref: '#/components/parameters/prefix'
  operations:
    publishCredentialUpdatedEvent:
      action: send
      description: Publish credential updated event
      tags:
        - name: Credentials
      channel:
        $ref: '#/channels/credentialUpdatedEvent'
      messages:
        - $ref: '#/channels/credentialUpdatedEvent/messages/credentialUpdatedEvent'
    publishAssignedIdentityAttributesUpdatedEvent:
      action: send
      description: Publish assigned identity attributes updated event
      tags:
        - name: Identity Attributes
      channel:
        $ref: '#/channels/assignedIdentityAttributesUpdatedEvent'
      messages:
        - $ref: '#/channels/assignedIdentityAttributesUpdatedEvent/messages/assignedIdentityAttributesUpdatedEvent'
  components:
    messages:
      credentialUpdatedEvent:
        name: credentialUpdatedEvent
        title: Credential updated event
        summary: Signal that the agent's credential has been updated or removed
        description: A new credential has been uploaded to the authentication provider component of the agent, or the
  existing credential has been deleted.
        contentType: application/json
        traits:
          - $ref: 'https://code.europa.eu/simpl/simpl-open/development/common-components/shared-specs/-/blob/feature/cloudev
  entsHeaders/cloudEventsHeaders.yaml#/messageTraits/cloudEventsHeaders'
        payload:
          $ref: '#/components/schemas/credentialUpdatedEvent'
      assignedIdentityAttributesUpdatedEvent:
        name: assignedIdentityAttributesUpdatedEvent
        title: Assigned identity attributes updated event
        summary: Signals that the identity attributes assigned to the participant have changed.
        description: A new ephemeral proof has been received from the governance authority, containing a different set of
  identity attributes assigned to the participant.
        contentType: application/json
        traits:
          - $ref: 'https://code.europa.eu/simpl/simpl-open/development/common-components/shared-specs/-/blob/feature/cloudev
  entsHeaders/cloudEventsHeaders.yaml#/messageTraits/cloudEventsHeaders'
        payload:
          $ref: '#/components/schemas/assignedIdentityAttributesUpdatedEvent'
    schemas:
      credentialUpdatedEvent:
        x-parser-schema-id: credentialUpdatedEvent
        properties:
          deleted:
            $ref: '#/components/schemas/credentialDeleted'
      assignedIdentityAttributesUpdatedEvent:
        x-parser-schema-id: assignedIdentityAttributesUpdatedEvent
        properties:
          assignedIdentityAttributesCodes:
            description: The list of identity attributes which are now assigned to the participant.
            schema:
              type: array
              items:
                $ref: '#/components/schemas/identityAttributeCode'
      credentialDeleted:
        description: Whether the credential has been deleted or only updated.
        type: boolean
      identityAttributeCode:
        type: string
    parameters:
      prefix:
        description: A prefix used to distinguish the channels used by different agents that insist on the same message
  broker.
```

### Topic Naming Convention

Follow an **action-based naming convention** for Kafka topics:

**Events:**

```
  $topic-owner-id.events.credential.updated
  $topic-owner-id.events.identity-attributes.changed
```

**Commands:**

```
  $topic-owner-id.commands.identity-attributes.assign
```

- `$topic-owner-id` is the namespace of the microservice that publishes the event (e.g. `eu.europa.ec.simpl.authenticationprovider.events.credential.updated`)

> **Note:** Since the dev cluster has a shared Kafka broker, prefix your topics with the namespace name.