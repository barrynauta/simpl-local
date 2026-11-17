# Guideline rules

Numbered, checkable rules extracted from the frozen guideline set in `sources/` (collected 2026-11-17) by step 11. The full record per rule (source section, word-for-word quote, check hint, factory steps, notes) is in [`rules.json`](./rules.json). In quotes, `[...]` marks text left out.

Level follows the source wording: must, shall, required = MUST; should, recommended = SHOULD; may, can = MAY. Where the source gives no strength the rule is SHOULD and `notes` says so. Check: `tool` (static tool), `test` (runtime or black-box test), `review` (a person or an agent reads), `none` (cannot be checked on an artefact).

The PSO Development Handbook is not in the sources, so there are no `HB-` rules yet.

## API: API guidelines

Sources: api-guidelines.md.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| API-01 | MUST | Every OpenAPI or AsyncAPI definition has a matching CDM delivered with it. | api-spec, data-model | review |
| API-02 | SHOULD | Schema Objects and message payloads reference the CDM instead of redefining business types locally. | api-spec | review |
| API-03 | SHOULD | The OpenAPI file is written by hand before the code and is not generated from the code. | api-spec, code | review |
| API-04 | SHOULD | The REST API specification uses OpenAPI 3.0.x. | api-spec | tool |
| API-05 | SHOULD | The OpenAPI definition file is YAML, not JSON. | api-spec | tool |
| API-06 | SHOULD | Every literal path segment is lowerCamelCase. | api-spec | tool |
| API-07 | SHOULD | Every query parameter name is lowerCamelCase. | api-spec | tool |
| API-08 | MUST | No path ends with a slash. | api-spec, code | tool |
| API-09 | SHOULD | No path segment ends with a file extension such as .json or .xml. | api-spec | tool |
| API-10 | SHOULD | Collection resources are named with a plural noun. | api-spec | review |
| API-11 | SHOULD | Each operation uses the HTTP method that fits its action (GET reads, POST creates or triggers, PUT replaces, PATCH updates, DELETE removes). | api-spec, code | review |
| API-12 | SHOULD | Each response uses the status code that best describes the outcome. | api-spec, code | test |
| API-13 | SHOULD | Every JSON property name is lowerCamelCase. | api-spec | tool |
| API-14 | SHOULD | Abbreviations inside property names are written with only the first letter capitalised (for example 'httpUrl', not 'HTTPURL'). | api-spec | tool |
| API-15 | SHOULD | Property names use American English spelling. | api-spec | review |
| API-16 | SHOULD | No property name contains an underscore, hyphen or dot. | api-spec | tool |
| API-17 | SHOULD | No property name starts with a digit. | api-spec | tool |
| API-18 | SHOULD | No property name is or contains a generic term such as 'info', 'information' or 'data'. | api-spec | tool |
| API-19 | SHOULD | Property names express the business meaning of the value, not its technical definition. | api-spec | review |
| API-20 | SHOULD | Property names do not repeat the name of the enclosing object (for example 'name' inside 'Employer', not 'employerName'). | api-spec | tool |
| API-21 | SHOULD | Every request body and response in the OpenAPI file has at least one example under 'examples'. | api-spec | tool |
| API-22 | SHOULD | Example values are realistic, not placeholders such as 'string' or 'foo'. | api-spec | review |
| API-23 | SHOULD | Every HTTP header name is Kebab-Case with each word capitalised (for example 'Content-Type'). | api-spec | tool |
| API-24 | SHOULD | Each API URI follows https://host/pathPrefix/apiName/v{majorVersion}/resources. | api-spec | tool |
| API-25 | SHOULD | Every service is reachable only over HTTPS (at least one-way TLS). | api-spec, config | test |
| API-26 | SHOULD | The apiName path segments are lowerCamelCase. | api-spec | tool |
| API-27 | SHOULD | The major version in the URI is an integer starting at 1 (v1, v2, ...). | api-spec | tool |
| API-28 | SHOULD | Each resource is a document, a collection or a controller. | api-spec | review |
| API-29 | SHOULD | No resource name contains an ownership word such as 'own' or 'my', unless the word is part of a technical term. | api-spec | tool |
| API-30 | SHOULD | Resource details are served at /resources/{resourceId}, or at /resources/{resourceId}/details when a separate detail view is needed. | api-spec | review |
| API-31 | SHOULD | info.version contains a major and a minor number, and optionally a patch number. | api-spec | tool |
| API-32 | SHOULD | The base URL carries only the major version (v1), never the minor or patch number. | api-spec | tool |
| API-33 | SHOULD | For each major version, only one minor and patch version is deployed in production at a time. | process | none |
| API-34 | MUST | info.version is present in the OpenAPI definition. | api-spec | tool |
| API-35 | SHOULD | REST client code sends an Accept header on every request. | code | review |
| API-36 | MUST | Every request or response that has a body carries a Content-Type header. | code, api-spec | test |
| API-37 | SHOULD | A request with an Accept header the API cannot satisfy gets 406. | code | test |
| API-38 | SHOULD | A request with an unsupported Content-Type gets 415. | code | test |
| API-39 | MUST | Responses from a resource that negotiates between several content types carry 'Vary: Accept'. | code, api-spec | test |
| API-40 | SHOULD | Every security scheme used is defined under components/securitySchemes. | api-spec | tool |
| API-41 | SHOULD | Every operation is covered by a 'security' section at root or operation level. | api-spec | tool |
| API-42 | SHOULD | info.license names 'European Union Public License (EUPL) 1.2' with the EUPL 1.2 URL given in the guideline. | api-spec | tool |
| API-43 | SHOULD | info.contact has name 'Simpl Programme', email 'cnect-simpl@ec.europa.eu' and url 'https://simpl-programme.ec.europa.eu/'. | api-spec | tool |
| API-44 | SHOULD | Client-caused errors return a 4xx status and server-caused errors return a 5xx status. | code, api-spec | test |
| API-45 | SHOULD | Every error response body is an RFC 9457 problem detail (media type application/problem+json). | code, api-spec | tool |
| API-46 | SHOULD | The 'type' of every problem detail is a URN starting 'urn:problem-type:simpl:' in one of the three listed forms. | code, api-spec | test |
| API-47 | SHOULD | The &lt;type&gt; part of the problem type URN is lowerCamelCase. | code, api-spec | test |
| API-48 | SHOULD | info.description is present and not empty. | api-spec | tool |
| API-49 | SHOULD | info.description states the purpose of the API in business terms. | api-spec | review |
| API-50 | SHOULD | Inside a resource representation, the resource's own identifier property is named 'id' or 'code'. | api-spec | review |
| API-51 | SHOULD | A property that references another resource is named after the relation (for example 'employer'), not after the target's identifier. | api-spec | review |
| API-52 | SHOULD | Identifier path parameters are named 'id' or 'code', prefixed with the resource type only when several identifiers appear in one path. | api-spec | tool |
| API-53 | SHOULD | Each search query parameter has the same name as the matching property in the response representation. | api-spec | tool |
| API-54 | SHOULD | Schema types for identifiers end with 'Id' or 'Code' (for example 'EmployerId'). | api-spec | review |
| API-55 | SHOULD | Where a standardised business identifier name exists, it is used instead of 'id' or 'code'. | api-spec | review |
| API-56 | SHOULD | Every parameter declares name, in, and either schema or content. | api-spec | tool |
| API-57 | SHOULD | Every parameter has a description. | api-spec | tool |
| API-58 | SHOULD | A parameter that accepts only a fixed set of values declares them with 'enum'. | api-spec | review |
| API-59 | MUST | Every enum value matches the parameter's declared type. | api-spec | tool |
| API-60 | SHOULD | Every required request parameter has 'required: true'. | api-spec | review |
| API-61 | MUST | Every path parameter has 'required: true'. | api-spec | tool |
| API-62 | MUST | Every microservice that publishes or consumes Kafka events ships an AsyncAPI file. | api-spec | review |
| API-63 | SHOULD | The AsyncAPI file lists every channel used, the operation on each channel and the messages on each channel. | api-spec, code | tool |
| API-64 | SHOULD | When a Maven project generates event models with Modelina, it does so through frontend-maven-plugin bound to the generate-sources phase. | config | tool |
| API-65 | MUST | Every Kafka message exchanged follows CloudEvents 1.0.2. | code, api-spec | test |
| API-66 | MUST | Messages use Kafka binary content mode: CloudEvents attributes are in Kafka headers ('ce_' prefix), the payload is the data. | code | test |
| API-67 | MUST | Every message carries all required CloudEvents attributes as Kafka headers. | code | test |
| API-68 | SHOULD | Every message carries the 'time' attribute as a Kafka header. | code | test |
| API-69 | MUST | The CloudEvents 'id' of every message is a UUID version 7. | code | test |
| API-70 | MUST | The CloudEvents 'source' identifies the producing application. | code | test |
| API-71 | MUST | The CloudEvents 'type' carries the event type and its version. | code, api-spec | test |
| API-72 | MUST | Every message carries the CloudEvents 'specversion' attribute. | code | test |
| API-73 | MUST | The CloudEvents 'datacontenttype' of every message is 'application/json'. | code | test |
| API-74 | MUST | The CloudEvents 'time' of every message is the time the occurrence happened. | code | test |
| API-75 | SHOULD | Consumers and producers can carry 'traceparent' and 'tracestate' headers without failing. | code | test |
| API-76 | SHOULD | Each AsyncAPI message definition takes its CloudEvents headers from the central shared YAML file through 'traits'. | api-spec | tool |
| API-77 | SHOULD | Event topics are named &lt;topic-owner-id&gt;.events.&lt;subject&gt;.&lt;past-tense verb&gt;. | api-spec, config | tool |
| API-78 | SHOULD | Command topics are named &lt;topic-owner-id&gt;.commands.&lt;subject&gt;.&lt;imperative verb&gt;. | api-spec, config | tool |
| API-79 | SHOULD | The topic-owner-id part of a topic name is the namespace of the publishing microservice. | api-spec, config | review |
| API-80 | SHOULD | On the shared dev cluster, topic names are prefixed with the Kubernetes namespace name. | config | review |

## CODE: Coding guidelines

Sources: coding-guidelines.md, NFR08.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| CODE-01 | MUST | Java code is formatted with Spotless using the Palantir Java format. | code | tool |
| CODE-02 | MAY | The Maven build runs the Spotless check. | config, pipeline | tool |
| CODE-03 | MUST | Angular code is linted with ESLint through angular-eslint. | code | tool |
| CODE-04 | MUST | The ESLint configuration for Angular extends the angular-eslint 'all' configuration. | config | tool |
| CODE-05 | MUST | Vue application code is formatted with Prettier using a TypeScript configuration. | code | tool |
| CODE-06 | MUST | Each method does one thing. | code | review |
| CODE-07 | SHOULD | Methods stay within the maximum line count. | code | tool |
| CODE-08 | SHOULD | A block of code repeated in several places is extracted into a method. | code | tool |
| CODE-09 | MUST | Blocks that look similar but do different things are not merged into one method. | code | review |
| CODE-10 | MUST | Code is unit-testable and reaches high unit-test coverage. | code, tests | tool |
| CODE-11 | MUST | Classes depend on interfaces or abstractions, not on concrete implementations of their collaborators. | code | review |
| CODE-12 | MUST | Dependencies are supplied to classes through dependency injection, not created inside them. | code | tool |
| CODE-13 | MUST | Code complies with the coding standards of section 1 (formatter and linter for its technology). | code | tool |
| CODE-14 | MUST | Identifiers are in English. | code | review |
| CODE-15 | MUST | Comments are in English. | code | review |
| CODE-16 | MUST | Code is kept as readable as possible. | code | review |
| CODE-17 | MUST | All names are clear and descriptive. | code | review |
| CODE-18 | MUST | Long statements are broken up. | code | tool |
| CODE-19 | MUST | Whitespace is used to aid readability. | code | tool |
| CODE-20 | MUST | Logging lets a problem in production be diagnosed from the logs alone. | code | review |
| CODE-21 | SHOULD | Logging uses the framework and format described on the Log4J wrapper page. | code, config | review |
| CODE-22 | MUST | Every log statement uses one of the levels Trace, Debug, Info, Warning, Error or Fatal. | code | tool |
| CODE-23 | SHOULD | Trace level is not used; Debug is used instead. | code | tool |
| CODE-24 | MUST | Each log statement uses the level whose 'when to use it' description fits the event. | code | review |
| CODE-25 | SHOULD | Error level is used only for events that require immediate investigation. | code | review |
| CODE-26 | MUST | Log lines are machine-parseable and human-readable. | code, config | test |
| CODE-27 | MUST | Log timestamps use a standard date and time format. | config | tool |
| CODE-28 | MUST | Every log line has a timestamp. | config | tool |
| CODE-29 | MUST | When an exception is logged, its stack trace is included. | code | tool |
| CODE-30 | SHOULD | Log statements carry context and metadata (for example request or correlation id, component, entity id). | code | review |
| CODE-31 | SHOULD | No log statement writes sensitive data such as secrets, tokens or credentials. | code | tool |
| CODE-32 | SHOULD | No log statement writes personally identifiable information. | code | review |
| CODE-33 | SHOULD | Code uses the shared exception-handling framework's base exception hierarchy. | code | review |
| CODE-34 | MUST | Exceptions are handled the same way throughout the component. | code | review |
| CODE-35 | MUST | The exception strategy covers architecture exceptions, Java API exceptions and third-party library exceptions. | code, docs | review |
| CODE-36 | MUST | Each base application exception has a defined handling, including the message returned to the user. | code, docs | review |
| CODE-37 | MUST | Resources are released in a finally block or with try-with-resources. | code | tool |
| CODE-38 | SHOULD | Code throws specific exceptions, not generic ones such as Exception or RuntimeException. | code | tool |
| CODE-39 | MUST | Every exception a public method throws is documented with @throws in its Javadoc. | code | tool |
| CODE-40 | MUST | Catch clauses are ordered from most specific to most general. | code | tool |
| CODE-41 | MUST | No catch block discards an exception without handling, rethrowing or logging it. | code | tool |
| CODE-42 | MUST | No catch block both logs an exception and rethrows it. | code | tool |
| CODE-43 | SHOULD | The component uses one system-wide exception class that extends RuntimeException, not a class per exception type. | code | tool |
| CODE-44 | SHOULD | The exception type is given by an error code enum, one enum per error category, not by free-text messages. | code | review |
| CODE-45 | SHOULD | Clients reach the component's REST APIs only through the API gateway, which performs authentication and authorisation. | config | review |
| CODE-46 | SHOULD | Calls to remote services go through a circuit breaker. | code | review |
| CODE-47 | SHOULD | All data source access goes through DAO (repository) classes. | code | tool |
| CODE-48 | SHOULD | Dependencies are wired by Spring Boot dependency injection, not hard-coded. | code | review |
| CODE-49 | SHOULD | Environment-specific configuration is supplied at runtime, not hard-coded, so the same build runs in every environment. | code, config | tool |
| CODE-50 | SHOULD | The component's logs are collected by the central log aggregation. | config | review |
| CODE-51 | SHOULD | Only the messaging gateway classes depend on the messaging system's API. | code | tool |
| CODE-52 | SHOULD | The component logs in the common Simpl-Open log format. | config | review |
| CODE-53 | SHOULD | Clients ignore unknown fields in responses and read only the fields they need. | code | test |
| CODE-54 | MUST | Every code contribution follows the documented guidelines for formatting, naming, modular design, error handling and documentation. | code | review |
| CODE-55 | SHOULD | Every language and framework used in a component has documented formatting and lint rules. | code, config | review |

## ARCH: Architecture principles and patterns

Sources: architecture-principles.md, architectural-patterns.md.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| ARCH-01 | SHOULD | The component works within a federation of autonomous participants and does not require central ownership of data, infrastructure or applications. | code, docs | review |
| ARCH-02 | MUST | A component can be replaced or added without changes to other components. | code, config | review |
| ARCH-03 | SHOULD | The component can be deployed and run without components it does not need. | config | test |
| ARCH-04 | SHOULD | The component has minimal dependencies on other components. | code, config | review |
| ARCH-05 | SHOULD | The component's APIs are business-oriented and hide implementation details, so internal changes do not affect consumers. | api-spec | review |
| ARCH-06 | MUST | A failure of one component has minimal impact on other components. | code, config | test |
| ARCH-07 | MUST | The component has no single point of failure (for example it can run with more than one replica). | config | review |
| ARCH-08 | SHOULD | The component's specification is open and free of proprietary claims. | docs | review |
| ARCH-09 | SHOULD | The service does not depend on a specific vendor technology. | code, config | review |
| ARCH-10 | SHOULD | The service runs in all target environments. | config | test |
| ARCH-11 | SHOULD | The service can be composed with other services into aggregated services. | api-spec, docs | review |
| ARCH-12 | SHOULD | An open development community is promoted. | process | none |
| ARCH-13 | SHOULD | The component's documentation describes how it interoperates with participants, without assuming a participant's implementation. | docs | review |
| ARCH-14 | SHOULD | The component scales vertically through configurable resources. | config | review |
| ARCH-15 | SHOULD | The component scales horizontally by adding replicas. | config, code | test |
| ARCH-16 | SHOULD | Performance does not deteriorate as load grows, given scaling. | code, config | test |
| ARCH-17 | MUST | Data confidentiality is guaranteed. | code, config | review |
| ARCH-18 | MUST | Data integrity is guaranteed. | code, config | review |
| ARCH-19 | MUST | Data availability is guaranteed. | config | review |
| ARCH-20 | MUST | Privacy of data subjects and users is assured. | code, data-model | review |
| ARCH-21 | MUST | The component complies with relevant regulations. | process | none |
| ARCH-22 | SHOULD | Every deployed service is registered and discoverable in the service registry or catalogue, visible to approved participants of the data space. | config, docs | review |
| ARCH-23 | SHOULD | Every service has a service description of its business purpose and technical interface. | docs, api-spec | review |
| ARCH-24 | SHOULD | Each service can be built and deployed on its own. | config, pipeline | review |
| ARCH-25 | SHOULD | Each service covers one business capability. | code, docs | review |
| ARCH-26 | SHOULD | Services communicate only through APIs (REST or events), not through shared databases. | code, config | review |
| ARCH-27 | SHOULD | Components communicate by producing and reacting to events. | code | review |
| ARCH-28 | SHOULD | Interactions that do not need an immediate answer are non-blocking (queues or background workers). | code | review |
| ARCH-29 | SHOULD | Each request is self-contained; the service keeps no session state between requests. | code | test |
| ARCH-30 | SHOULD | Each user and service has only the access it needs. | config, code | review |
| ARCH-31 | SHOULD | Security controls exist at more than one layer (network, gateway, service). | config, code | review |
| ARCH-32 | SHOULD | Every request, internal or external, is authenticated and authorised before access is granted. | code, config | test |
| ARCH-33 | SHOULD | Failed calls with transient errors are retried automatically after a delay. | code | review |
| ARCH-34 | SHOULD | Calls to underperforming services are stopped by a circuit breaker. | code | review |
| ARCH-35 | SHOULD | When a dependency fails, the component keeps offering the functions that do not need it. | code | test |

## DATA: Data modelling

Sources: data-modeling-guidelines.md.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| DATA-01 | SHOULD | A Conceptual Data Model (CDM) exists for the component. | data-model | review |
| DATA-02 | SHOULD | A Logical Data Model (LDM) exists for a component that persists data. | data-model | review |
| DATA-03 | SHOULD | A Physical Data Model (PDM) exists for a component that persists data. | data-model | review |
| DATA-04 | SHOULD | The CDM describes the business domain, not storage or transfer. | data-model | review |
| DATA-05 | SHOULD | The CDM contains no UUIDs, database keys, audit fields or security fields unless they have explicit business value. | data-model | review |
| DATA-06 | SHOULD | Relationships in the CDM are shown as associations, not as attributes. | data-model | review |
| DATA-07 | SHOULD | Every status or type attribute in the CDM lists its valid business values. | data-model | review |
| DATA-08 | SHOULD | CDM associations are labelled with verbs where possible. | data-model | review |
| DATA-09 | SHOULD | Each CDM entity is a UML class with a meaningful business noun as name. | data-model | review |
| DATA-10 | SHOULD | CDM attribute types are general business types (Money, Date, Text), not physical types such as int or varchar. | data-model | review |
| DATA-11 | SHOULD | The CDM uses business concepts instead of technical boolean flags. | data-model | review |
| DATA-12 | SHOULD | CDM date attributes are named after business events, not generic timestamps. | data-model | review |
| DATA-13 | SHOULD | Every CDM association shows its multiplicity at both ends. | data-model | review |
| DATA-14 | SHOULD | The CDM contains no API mechanics such as request, response or DTO classes. | data-model | review |
| DATA-15 | SHOULD | The CDM does not mix technical and business concepts. | data-model | review |
| DATA-16 | SHOULD | No attribute or relationship appears twice in the CDM. | data-model | review |
| DATA-17 | SHOULD | The LDM is a UML class diagram. | data-model | review |
| DATA-18 | SHOULD | LDM attributes are shown on each class and named in snake_case. | data-model | tool |
| DATA-19 | SHOULD | LDM attributes have logical data types and each class declares its candidate keys. | data-model | review |
| DATA-20 | SHOULD | LDM associations show multiplicities. | data-model | review |
| DATA-21 | SHOULD | Many-to-many associations that carry their own data are resolved into association classes. | data-model | review |
| DATA-22 | SHOULD | The PDM is a UML class diagram with one class per table. | data-model | review |
| DATA-23 | SHOULD | PDM columns are attributes with physical database types. | data-model | review |
| DATA-24 | SHOULD | The PDM shows primary and foreign keys. | data-model | review |
| DATA-25 | SHOULD | The PDM declares indexes and other access structures where relevant. | data-model | review |
| DATA-26 | MUST | Every domain change updates the CDM. | data-model, process | review |
| DATA-27 | MUST | Every domain change updates the affected API contract. | api-spec, process | review |
| DATA-28 | MUST | Every domain change updates the documentation that references the model. | docs, process | review |
| DATA-29 | MUST | The CDM and the APIs are versioned together from one source of truth. | data-model, api-spec | review |

## SEC: Security

Sources: coding-guidelines.md section 3, security.md.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| SEC-01 | MUST | Security is addressed in the design, before or during development. | docs, process | review |
| SEC-02 | MUST | Code follows the coding practices of the Secure Development Guideline document. | code | review |
| SEC-03 | MUST | Each participant has a local IT security plan covering personnel, IT systems and its agent deployment. | process | none |
| SEC-04 | SHOULD | The agent's features comply with the Simpl-Open IT Security Plan. | code, config | none |
| SEC-05 | SHOULD | Tier 1 access control is role based (RBAC). | code, config | test |
| SEC-06 | SHOULD | Tier 2 access control is attribute based (ABAC) on identity attributes. | code, config | test |
| SEC-07 | SHOULD | Tier 1 user authentication uses OpenID Connect through the Tier 1 Authentication Provider (Keycloak). | code, config, api-spec | review |
| SEC-08 | SHOULD | Agent-to-agent communication is encrypted. | code, config | test |
| SEC-09 | SHOULD | Agent-to-agent communication is integrity protected. | code, config | test |
| SEC-10 | SHOULD | Agent-to-agent communication is mutually authenticated (mTLS). | code, config | test |
| SEC-11 | SHOULD | The component's logs feed the central monitoring service (ELK). | config | review |
| SEC-12 | SHOULD | Tier 1 authorisation is enforced at the Spring Cloud Gateway. | config | review |
| SEC-13 | SHOULD | Tier 2 authorisation is enforced at the Spring Cloud Gateway. | config | review |
| SEC-14 | SHOULD | Participants consider DMZ, IDS, firewall, VPN and a 2-3 tier deployment for their node. | process | none |
| SEC-15 | SHOULD | Agent-to-agent HTTP calls are made through the mTLS HTTP client. | code | review |
| SEC-16 | SHOULD | The caller takes the ephemeral proof from the cache, or requests a new one from the Authority and caches it until it expires. | code | test |
| SEC-17 | SHOULD | The caller always performs an OCSP credential check against the Authority Identity Provider. | code | test |
| SEC-18 | SHOULD | The called gateway performs an OCSP check only when no validated proof for the credential's public key is cached. | code | test |
| SEC-19 | SHOULD | The called agent validates a proof not yet in its cache and caches it with a TTL derived from the proof's expiry. | code | test |
| SEC-20 | SHOULD | A request is processed only after the ABAC policies have passed. | code, config | test |
| SEC-21 | SHOULD | When the target is not a Simpl-Open agent, the client falls back to standard HTTPS. | code | test |
| SEC-22 | SHOULD | Agent administration (first configuration, creation of operations accounts) is done with technical accounts. | config, process | review |
| SEC-23 | SHOULD | Administrative rights are assigned to roles, and roles to people, never rights directly to people. | config, process | review |

## OPS: DevSecOps

Sources: devsecops.md, NFR08.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| OPS-01 | SHOULD | The component does not depend on OVH managed services such as managed databases. | config | review |
| OPS-02 | SHOULD | Tests in the development stage use synthetic data. | tests | review |
| OPS-03 | SHOULD | Production data used in integration or pre-prod is sanitised so no sensitive information remains. | tests, process | review |
| OPS-04 | MUST | New environments are requested through an issue in Simpl/Operations/Environment-onboarding. | process | none |
| OPS-05 | SHOULD | The pipeline runs SAST on every commit and merge. | pipeline | tool |
| OPS-06 | MUST | Code is merged to main only when the SAST quality gate passes. | pipeline | tool |
| OPS-07 | SHOULD | The pipeline runs SCA (dependency scanning) on every commit and merge. | pipeline | tool |
| OPS-08 | SHOULD | The pipeline scans container images on every branch, commit and merge. | pipeline | tool |
| OPS-09 | SHOULD | DAST runs on the integrated agents in pre-prod after end-to-end testing. | process | none |
| OPS-10 | SHOULD | The component's deployment defines pod-level ingress and egress network policies. | config | tool |
| OPS-11 | SHOULD | Sensitive runtime data is stored in Kubernetes Secrets or Vault, not in plain configuration. | config | tool |
| OPS-12 | SHOULD | Pipeline secrets are held in an external secret store such as Vault, not in the repository. | pipeline | tool |
| OPS-13 | SHOULD | Pods run with a dedicated Kubernetes service account. | config | tool |
| OPS-14 | SHOULD | Container images are scanned before they are deployed. | pipeline | tool |
| OPS-15 | SHOULD | Every released artefact (jar, image, Helm chart) has a Semantic Versioning version. | pipeline, config | tool |
| OPS-16 | SHOULD | The repository follows GitFlow with main, develop, feature/*, release and hotfix branches. | process | review |
| OPS-17 | SHOULD | The main and develop branches are protected. | config | review |
| OPS-18 | SHOULD | Merges to main come only from develop, release or hotfix branches. | process | review |
| OPS-19 | SHOULD | Merges to develop come only from feature/* branches. | process | review |
| OPS-20 | SHOULD | Hotfix branches are created from a tag on main. | process | review |
| OPS-21 | SHOULD | Release and hotfix branches are deleted after they are merged to main. | process | review |
| OPS-22 | SHOULD | Feature branches are deleted after they are merged to develop. | process | review |
| OPS-23 | SHOULD | The pipeline runs the unit tests on every build. | pipeline, tests | tool |
| OPS-24 | SHOULD | The pipeline runs Sonar static code analysis. | pipeline | tool |
| OPS-25 | SHOULD | The pipeline builds the container image and pushes it to the GitLab container registry. | pipeline | tool |
| OPS-26 | SHOULD | The pipeline produces an SBOM for the image. | pipeline | tool |
| OPS-27 | SHOULD | The pipeline releases the component's Helm chart to the GitLab Package Registry. | pipeline | tool |
| OPS-28 | SHOULD | Each component is released as a Helm chart. | config | tool |
| OPS-29 | SHOULD | Component chart values can be overridden from the agent-level chart. | config | review |
| OPS-30 | SHOULD | Each deployment declares its deployment strategy. | config | tool |
| OPS-31 | SHOULD | Only DevSecOps admins destroy environment infrastructure. | process | none |
| OPS-32 | MUST | Necessary data is backed up before infrastructure is destroyed. | process | none |
| OPS-33 | MUST | The pipeline runs linters, formatter checks and static analysis. | pipeline | tool |
| OPS-34 | MUST | Guidelines are kept in a central, version-controlled location. | process | none |
| OPS-35 | MUST | Guideline violations block the merge automatically (pre-commit hook, CI job or merge gate). | pipeline | tool |
| OPS-36 | MUST | Guidelines are reviewed every quarter. | process | none |
| OPS-37 | MUST | Guideline enforcement is in the pipeline from the first pipeline version of the component. | pipeline | review |

## OWASP: OWASP Top Ten (NFR14)

Sources: NFR14 plus OWASP Top 10:2021.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| OWASP-01 | MUST | The component complies with the OWASP Top Ten. | code, config | review |
| OWASP-02 | MUST | Every operation enforces access control on the server side; users cannot act outside their permissions. | code, config | test |
| OWASP-03 | MUST | Sensitive data is protected with sound cryptography in transit and at rest (secure data storage). | code, config | tool |
| OWASP-04 | MUST | All untrusted input is validated and never reaches an interpreter (SQL, LDAP, OS, template) without parameterisation or encoding. | code, config | tool |
| OWASP-05 | MUST | The design addresses threats (threat model, secure design patterns) before implementation. | code, config | review |
| OWASP-06 | MUST | The component ships with secure defaults: no default credentials, no unneeded features, hardened headers and error output. | code, config | tool |
| OWASP-07 | MUST | No dependency or base image has known vulnerabilities above the agreed threshold. | code, config | tool |
| OWASP-08 | MUST | Authentication is done through the platform identity provider; no custom password handling, and sessions and tokens are validated. | code, config | test |
| OWASP-09 | MUST | Code, dependencies, images and data received are integrity checked; no unsafe deserialisation of untrusted data. | code, config | tool |
| OWASP-10 | MUST | Security-relevant events (logins, access denials, input validation failures) are logged and monitored. | code, config | review |
| OWASP-11 | MUST | Outgoing requests to URLs taken from input are validated against an allow list. | code, config | tool |
| OWASP-12 | SHOULD | The pipeline runs automated security scanning. | pipeline | tool |
| OWASP-13 | SHOULD | Security code reviews are done. | process | review |
| OWASP-14 | SHOULD | Penetration tests are run and their results recorded. | process | none |

## UX: UX (NFR15)

Sources: NFR15.

| Id | Level | Rule | Applies to | Check |
|---|---|---|---|---|
| UX-01 | MUST | Every user interface follows the established UX guidelines. | ui | review |
| UX-02 | SHOULD | UI adherence is measured with usability testing, user feedback and evaluations. | ui, process | test |
| UX-03 | SHOULD | UI prototypes are validated with user testing before build. | ui, process | none |
| UX-04 | SHOULD | UX guidelines are applied from the design phase and refined from user feedback. | process | none |

## DDD, MACH, MSV, OWT, OWA, BLG: engine rules

Since 17 Nov 2026 the factory's own rules (domain-driven design, MACH, microservices, OWASP Top 10:2025, OWASP API Security Top 10 2023, Belgif REST guide) live in the engine: `spec-factory/guidelines/rules.md`. The scripts read the engine rules and these project rules together.

## Not a rule

Sections read and not turned into rules, with the reason. Everything else in the sources has at least one rule above.

- **README.md** (guidelines folder): provenance of the frozen copies.
- **api-guidelines.md**
  - *API Types*: lists the two API types; the sections below carry the rules.
  - *Synchronous REST APIs*, opening sentence: names BelgIF as the reference; only the listed points are turned into rules, not the whole BelgIF guide.
  - *Synchronous REST APIs*, OpenAPI Schema Object explanation (JSON Schema variant): background.
  - *JSON property naming*, "Properties used from other standards ... are allowed to deviate": an exception, kept in the notes of API-13 and API-16.
  - *URI structure*, rows `host`, `pathPrefix`, `resources`: definitions, no requirement.
  - *Security schemes*, list of scheme types (http, apiKey, oauth2, openIdConnect): explanation.
  - *Error handling*, "`<api>` ... MAY be used for API-specific problem types": a permission, kept in the notes of API-46.
  - *Identifiers and codes*, "a suffix can be added to the name to disambiguate": a permission, not checkable.
  - *Parameters*, YAML examples: examples.
  - *Event-driven APIs*, purpose list: background.
  - *AsyncAPI Definitions*, "Use AsyncAPI Studio for preview": a tooling tip.
  - *Code Generation*, Modelina description and the frontend-maven-plugin XML example: example; used for the conflict on API-64.
  - *CloudEvents Conventions*, the AsyncAPI example: example; used for the conflicts on API-77 and API-79.
- **coding-guidelines.md**
  - Introduction and audience note.
  - *1. Coding Standards* intro: the "mandated" wording gives the strength of CODE-01 to CODE-05.
  - *1.1 Java*, "Quality profile" (Palantir is compatible with the PSO Sonar profile) and "Tooling" (profiles and IDE plugin can be downloaded): statements of fact.
  - *2.1 Decomposition*, `openAndPrint()` example: example for CODE-06.
  - *2.2 Unit-testability*, "Beyond testing, this approach promotes modularity": background.
  - *2.4 Logging*, the "When to use it" rows for Debug, Info, Warning and Fatal: content of CODE-24.
  - *2.5 Exception Handling*, "The framework improves on naive handling in two ways": the two points became CODE-43 and CODE-44; the reasons given are background.
  - *2.6 Patterns*, intro: background.
  - *3. Secure Development Guidelines*, "Application security is a core part of the Simpl-Open product strategy": background.
- **architecture-principles.md**
  - Intro ("all are considered equally important"): background.
  - *Architecture approach* (the five TOGAF-inspired layers): describes how the architecture is documented, not a property of a component.
  - *Modularity*, "every component with a different open-source technology": a consequence, not a requirement.
  - *Loose Coupling*, "This allows service owners to change implementation ...": background.
  - *Openness & Agnosticism*, "makes adding, updating or changing components easy": background.
  - *Composability & Extensibility*, "Simpl-Open remains open to iterative growth": background.
  - *Interoperability*, first sentence: covered by ARCH-13.
  - *Scalability & Elasticity*, first sentence: covered by ARCH-14 to ARCH-16.
- **architectural-patterns.md**: intro and the "improves..." benefit sentences of each pattern.
- **data-modeling-guidelines.md**
  - *Scope* note: semantic and ontology modelling (RDF, OWL, SHACL, DCAT-AP) is out of scope; recorded under "Not covered".
  - Definitions of CDM, LDM and PDM: covered by DATA-01 to DATA-03.
  - *Purpose*: aims, not requirements.
  - *Conceptual Data Model (CDM)* intro paragraph: background.
  - *Key principles*, "Abstraction" (the model may differ from API objects): a permission. "Clarity": covered by DATA-06 to DATA-09.
  - *Step-by-step*, step 2 (attributes with business value), the repeated "indicate the valid business values", "Convert foreign keys", "Do not list a relationship as an attribute" and "Name relationships with verbs": repeat DATA-05 to DATA-08.
  - *Anti-patterns*, "Do not include redundant attributes or technical IDs": repeats DATA-05 and DATA-06.
  - *Examples* table and the two images: examples (images not copied).
  - *LDM* and *PDM* intro paragraphs, "can be used to generate a database schema": background.
- **security.md**
  - *Introduction and Perimeter of Intervention*: the end-to-end flow, the network and local node responsibility split, roles of Governance Authority, network deployment and development; scope statements. Only the participant and agent items became SEC-03 and SEC-04.
  - *DevSecOps Security Architecture related aspects* table: user management and audit process are images; OVH audit trails and Velero backups describe the hosting; SAST, SCA and DAST are covered by OPS-05 to OPS-09.
  - *Simpl-Open (Product) Security Architecture* intro and the "legacy location" notes: background.
  - *Functional Security* rows 3 (Local Directory System), 4, 5, 7 and 8: name features owned by the IAA components (Keycloak, EJBCA, Security Attribute Provider, Tier 2 Authentication Provider), with details in ACVs outside the sources. They describe which component does what, not a rule another component can be checked on.
  - *Technical Security* rows 1, 4, 5, 6 and 7: same reason; rows 2 and 3 became SEC-12 and SEC-13.
  - *Enhanced version* of the handshake: a future design. Rules were taken from the current release only. If the enhanced version ships, SEC-17 and SEC-19 change.
  - The images.
- **devsecops.md**
  - *Overview*, cluster list, Jira and Xray: description of the toolchain.
  - *Planning and Design of Clusters*: team responsibilities, Rancher, sizing; the stage table's purpose, user group, release and deployment columns. Test data rules became OPS-02 and OPS-03.
  - *Cluster Provisioning and Setup*: empty heading.
  - *Environment Onboarding Process* steps 1 to 4: DevSecOps team procedure.
  - *Security and Access Control*: role list, role mapping table, Keycloak for tools. Toolchain administration, not the build.
  - *Security Checks*: the explanations of SAST, SCA, container scanning and DAST, the WHAT, WHERE and WHO lines, "a scan can be requested manually" for feature branches, the 2-3 day DAST runtime.
  - *Kubernetes best practices*: reasons for choosing Kubernetes; the OVH default network policies; cluster RBAC with Keycloak groups (cluster administration); regular updates and patching (done by OVH and the admins).
  - *Continuous Deployment and GitOps*: goals and the architecture list.
  - *GitFlow*: advantages; the branch descriptions for main and develop (covered by OPS-16).
  - *CI/CD Pipeline*: "Building the Code", "Create package for distribution", the SAST and SCA lines (covered by OPS-05 and OPS-07), and the tracking of pipeline runs.
  - *Release Management*: the two levels, the environments and the four release steps. "A Production environment is not planned for Simpl-Open" is used in the note of API-33.
  - *Helm Charts*: chart management and the App of Apps benefits.
  - *Argo CD*: application definition and triggers; teams are free to configure triggers.
  - *Testing*: points to a testing document that is not in the sources.
  - *Monitoring and Logging*: the DevSecOps monitoring stack, 1 month metrics retention, email alerting.
  - *Backup and restore*: Velero and the per-cluster backup policy.
  - *Deprovision of Environments*, step 1 and the Terraform explanation in step 2.
- **NFR08, NFR14, NFR15**: canonical source links; the "Realistic" bullets (justification); the NFR08 metrics list (number of linting violations, percentage of repositories with enforced formatting, review checklists), which are programme-level measures; the NFR08 "Teams receive support through onboarding"; the NFR14 "Achievable" bullet (integrating practices, audits, training) and "Timely" bullet, which repeat SEC-01; the NFR descriptions, which repeat the "Specific" bullets (CODE-54, OWASP-01, UX-01).

## Conflicts and overlaps

**Conflicts**

1. **CloudEvents `specversion`** (API-72): the table says it must be `application/json`. CloudEvents 1.0.2, which API-65 makes mandatory, requires `1.0`. Looks like a copy of the `datacontenttype` row.
2. **CloudEvents `time`**: SHOULD in the bullet list (API-68), required in the attribute table (API-74).
3. **Topic names** (API-77, API-79): the convention is `<owner>.events.credential.updated`, but the AsyncAPI example in the same document uses `{prefix}authentication-provider.events.credentialUpdated` and `...events.identity-attributes-changed`. The owner id is shown both as `eu.europa.ec.simpl.authenticationprovider` and as `authentication-provider`.
4. **Modelina phase** (API-64): the text says `generate-sources`, the XML example uses `generate-resources`.
5. **CDM name**: "Common Data Model" in api-guidelines.md (API-01), "Conceptual Data Model" in data-modeling-guidelines.md (DATA-01).
6. **CDM anti-pattern** (DATA-14): "Do not include ...Request" clashes with the guideline's own good example entity `RoleAssignmentRequest`. A name-based check gives a false finding.
7. **Exceptions**: CODE-38 (prefer specific exceptions) against CODE-43 (one system-wide exception class). CODE-44 (enum error codes) is what reconciles them; the text does not say so.
8. **Security scanning tools**: NFR14 names SonarQube for security scanning (OWASP-12); devsecops.md names Fortify for SAST, SCA and DAST and Trivy for images (OPS-05 to OPS-08), and Sonar only for quality (OPS-24).
9. **Path parameter names** (API-52): the rule says `id`, prefixed only when several identifiers are in the path; the Parameters example uses `/users/{userId}` with one identifier.
10. **Production versions** (API-33): one minor version per major version in production, but devsecops.md says no Production environment is planned for Simpl-Open.
11. **GitFlow merges** (OPS-19): develop accepts merges only from `feature/*`, yet GitFlow needs release and hotfix fixes merged back into develop.
12. **Spelling**: API-15 asks for American English in JSON property names; the guideline texts use British spelling. Not a real conflict, but it will surface in reviews.

**Overlaps** (same requirement in more than one place)

- CDM required: API-01 and DATA-01.
- HTTPS and transport encryption: API-25, SEC-08, SEC-09, OWASP-03.
- Dependency injection: CODE-12 and CODE-48.
- Circuit breaker: CODE-46 and ARCH-34.
- API gateway and authorisation: CODE-45, SEC-12, SEC-13.
- Log format: CODE-21 and CODE-52. Log aggregation and monitoring: CODE-50, SEC-11, OWASP-10.
- Sensitive data in logs: CODE-31, CODE-32, OWASP-10.
- Least privilege and access control: ARCH-30, SEC-05, OWASP-02.
- Security in design: SEC-01 and OWASP-05.
- Vendor neutrality: ARCH-09 and OPS-01.
- Availability: ARCH-06, ARCH-07, ARCH-19.
- Service description: ARCH-23, API-48, API-49.
- Formatting and linting: CODE-01 to CODE-05, CODE-13, CODE-54, OPS-33, OPS-35. CODE-02 is MAY, OPS-33 and OPS-35 are MUST for the same enforcement.
- Security scanning: OPS-05, OPS-07, OPS-08, OPS-14, OWASP-07, OWASP-12, and the DevSecOps table in security.md.
- Secure coding: SEC-02 and OWASP-01 (both point to documents or standards outside the sources).
- Naming layers: LDM attributes are snake_case (DATA-18), API properties lowerCamelCase (API-13). Different layers, but the mapping between them is not described.

## Vague rules

Rules that cannot be checked as written, and what would make them checkable. Findings for the guideline owners.

| Rule | What is unclear | What would make it checkable |
|---|---|---|
| CODE-07 | "maximum line count" has no number | State the limit (for example 30 lines) and the Sonar or Checkstyle rule |
| CODE-10 | "high unit-test coverage" | A coverage threshold (line and branch) and the tool |
| CODE-16, CODE-17, CODE-18, CODE-19 | "as readable as possible", "name well", "long", "deliberately" | Cognitive complexity limit, naming conventions per language, line length |
| CODE-20 | "diagnosable in production" | A list of what an error log must contain |
| CODE-21, CODE-52 | The Log4J wrapper page and the common log format are not in the sources | Add the page, or state the format (for example JSON with named fields) |
| CODE-27 | "a standard date and time format" | Name it (ISO 8601 with time zone) |
| CODE-30 | "context and metadata" | A list of required fields (correlation id, component, user or agent id) |
| CODE-30, CODE-31, CODE-32 | "always" and "never" without a modal verb; read as SHOULD | Say MUST if that is meant; define "sensitive data" |
| CODE-33 | The shared exception framework is not named or linked | Link the library and version |
| CODE-34 | "consistent" | Name the one handling mechanism (for example one global handler) |
| CODE-55 | Only Java, Angular and Vue have formatter or linter rules | Rules for every language in use (TypeScript outside Angular, Python, Go, Helm, shell) |
| SEC-02 | The Secure Development Guideline document is not in the sources | Add it or link it |
| SEC-04 | The Simpl-Open IT Security Plan is not in the sources | Add it or link it |
| SEC-11 | Business logging specification still being migrated | Finish the migration (SIMPL-33642) |
| SEC-21 | How the client decides an endpoint is not an agent | State the detection and whether the fallback is logged |
| UX-01 | The "established UX guidelines" are not in the sources | Add or link the UX guideline document |
| OWASP-01 to OWASP-11 | NFR14 names no OWASP Top 10 version; 2021 is used here | Name the edition (2021 or 2025) |
| OWASP-07 | No vulnerability threshold | State the blocking severity (for example no Critical or High) |
| API-02 | How a JSON schema can reference a UML CDM | A machine-readable CDM, or a naming rule linking schemas to CDM entities |
| API-12 | "best describes" | Adopt the BelgIF status code table as the reference |
| API-18, API-29 | Open lists ("like", "...") | Closed lists of banned words |
| API-19, API-20, API-49 | Judgement on business meaning and clarity | Accept as review-only, or give examples of pass and fail |
| API-55 | "standardized name for the business identifier" | A list of standard identifiers to use |
| API-57 | "other attributes, such as" | List the required parameter attributes |
| API-70, API-71 | `source` and `type` patterns given only by example | State the pattern |
| API-75 | "accommodate" trace headers | Say ignore, propagate or log |
| API-76 | The shared CloudEvents header file is referenced on a feature branch | Reference a tag or main |
| API-79 | "namespace" shown in two forms | One definition of the topic owner id |
| ARCH-01, ARCH-04, ARCH-06, ARCH-07, ARCH-10, ARCH-11, ARCH-25, ARCH-27, ARCH-28, ARCH-31 | Principles with no measure ("federate", "minimal", "maximum extent possible", "all environments", "when to use events") | Measurable criteria per principle, or accept them as review-only |
| ARCH-16 | No performance target | Response time and throughput targets per load level |
| ARCH-17 to ARCH-21 | "guaranteed", "assured", "relevant regulations" | Link to concrete security and privacy requirements, name the regulations |
| ARCH-22 | Which registry or catalogue | Name it |
| DATA-08, DATA-25 | "where possible", "where relevant" | Accept as review-only |
| DATA-29 | Where the single source of truth lives | Name the repository and versioning scheme |
| OPS-36 | "major development milestones" | Name the milestones |

## Not covered

Good practice the sources do not state and that matters for a build. Not rules; listed so the gap is visible.

- No unit-test coverage threshold, and no rules for integration, contract or end-to-end tests (the testing document is not in the sources).
- No formatter or linter rules for languages other than Java, Angular and Vue, and none for Helm charts or Dockerfiles.
- No REST conventions for pagination, filtering, sorting or partial responses.
- No API deprecation or backward compatibility policy beyond "major version in the URL".
- No rules for event payload schema evolution, consumer idempotency or delivery guarantees.
- No health, readiness or liveness endpoints and no metrics exposure rule for components.
- No container image rules (base image, non-root user, read-only file system, resource limits).
- No secret scanning requirement in the pipeline.
- No licence compliance rule for dependencies (SCA mentions licence risk, but there is no rule).
- No database migration tooling convention.
- No accessibility standard for UIs (for example WCAG level) and no internationalisation rule.
- No rules for semantic or ontology models (RDF, SHACL, DCAT-AP); excluded by the data modelling scope.
- The PSO Development Handbook (`HB-` rules) is not in the sources yet.

## Totals

**277 rules** from the Simpl guidelines. The engine adds 146 generic rules (see above).

**Per prefix**

| | Count |
|---|---|
| API | 80 |
| CODE | 55 |
| ARCH | 35 |
| DATA | 29 |
| SEC | 23 |
| OPS | 37 |
| OWASP | 14 |
| UX | 4 |

**Per level**

| | Count |
|---|---|
| MUST | 84 |
| SHOULD | 192 |
| MAY | 1 |

**Per check type**

| | Count |
|---|---|
| tool | 93 |
| test | 44 |
| review | 125 |
| none | 15 |

**Per factory step**

| Step | Rules |
|---|---|
| 21 (API validation) | 71 |
| 50 (code writer brief) | 255 |
| 70 (verification) | 277 |
