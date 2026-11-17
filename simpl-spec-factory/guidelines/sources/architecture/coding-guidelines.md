
<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
<p>
📍 <strong>You are here</strong> / <a href="../README.md">🏠 Home</a> / <a href="README.md">Foundations</a> / Coding guidelines
</p>
</div>

[[_TOC_]]

# Simpl-Open Coding Guidelines

This document defines the standards and procedures for developing Simpl-Open application components. 
It applies to any custom-built component, to ensure code is developed to a consistent quality bar and 
remains maintainable over its lifetime.

> [!note]
> Audience: developers contributing to Simpl-Open.

## 1. Coding Standards

Coding standards are the rules developers must follow when writing source code. Adhering to them improves 
readability and reduces the cost of maintenance. Each supported technology has a mandated formatter and rule set.

### 1.1 Java

- **Formatter:** [Spotless](https://github.com/diffplug/spotless), applied via the [Palantir](https://github.com/palantir/palantir-java-format) format (which is based on the Google Java style guidelines).
- **Maven integration:** Spotless is provided as a Maven goal, so formatting can be enforced in the build.
- **Quality profile:** The Palantir format is fully compatible with the PSO quality profile configured in Sonar.
- **Tooling:** Quality profiles and the IDE plugin are available for download.

### 1.2 Angular

- **Linter:** [ESLint](https://github.com/angular-eslint/angular-eslint).
- **Rule set:** The default `all` configuration from `angular-eslint` must be used: https://github.com/angular-eslint/angular-eslint/blob/main/packages/eslint-plugin/src/configs/all.json

### 1.3 Vue

- **Formatter:** [Prettier](https://prettier.io/) (TypeScript configuration) must be used for Vue applications.

## 2. Coding Practices

### 2.1 Decomposition

Each method must have a single purpose. Consider extracting a new method when a method exceeds the maximum line count, or when the same block of code is repeated in several places.

Do not over-apply this: two blocks that look similar but do different things must not be merged. A name like `openAndPrint()` is a signal that two methods are hiding in one — `open()` and `print()`.

### 2.2 Unit-testability

All code must be written with testability in mind, to enable high unit-test coverage across the codebase.

- Avoid tight coupling to concrete implementations.
- Use dependency injection (DI) and inversion of control (IoC) to supply dependencies to classes. This allows mock or stub objects to be substituted during testing.

Beyond testing, this approach promotes modularity and maintainability, making the codebase more robust and adaptable to future change.

### 2.3 Readability

Developers must follow these rules for readability:

- Know and apply the coding standards.
- Keep code as readable as possible, using English for identifiers and comments.
- Name all things well.
- Break up long statements.
- Use whitespace deliberately to aid readability.

### 2.4 Logging

Developers must write logging that makes problems diagnosable in production. The logging framework and log format are documented on the Log4J wrapper page.

Every log statement must use one of the following levels:

| Level   | When to use it |
|---------|----------------|
| Trace   | Development-only tracing of bugs. Prefer Debug instead. |
| Debug   | Developer-facing diagnostics such as response times, health checks, and queue status. |
| Info    | User-driven actions and system events (e.g. regularly scheduled operations). |
| Warning | Something unusual or not-quite-right happened, but nothing failed and the user is unlikely to notice. |
| Error   | A problem requiring investigation — disconnections, failed tasks, or failures visible to users. If an Error does not require immediate investigation, its severity should probably be lowered. |
| Fatal   | Crashes, serious latency, performance problems, or security problems. Usually signifies the end of the program. |

Logs must be machine-parseable yet human-readable. Achieve this by:

- Using a standard date and time format.
- Adding timestamps.
- Using the correct log level.
- Including the stack trace when logging a thrown exception.

Always enrich logs with context and metadata to help pinpoint production problems. Never log sensitive data or personally identifiable information (PII).

### 2.5 Exception Handling

A shared exception-handling framework provides a base exception hierarchy and the core exceptions the project needs.

The exception strategy must:

- Be consistent. Consistent handling is essential to the application's usability and maintainability.
- Account for the different exception sources: architecture exceptions, Java API exceptions from direct Java API calls, and exceptions from third-party libraries called directly by the application.
- Identify base application exceptions and define how each is handled (e.g. the error message sent to the user).

Practices developers must follow:

- Use a `finally` block or a try-with-resources statement to release resources.
- Prefer more specific exceptions over general ones.
- Document thrown exceptions in Javadoc comments.
- Catch the most specific exception first.
- Never swallow a caught exception.
- Never log an exception and then re-throw it.

The framework improves on naive handling in two ways:

- **A single, system-wide exception class.** Rather than a separate class per exception type, one class extends `RuntimeException`. This keeps the class count low and removes the need to declare exceptions that will not be handled anyway.
- **Enums for error codes.** Encoding the failure type in a free-text message leaves wording to each developer and produces inconsistent phrasing for the same failure. Instead, use an enum to indicate the exception's type, with one enum per error category.

### 2.6 Patterns

The following patterns and principles are applied across the codebase. They give a reusable solution to common problems and improve maintainability. Most are standard microservice patterns.

| Pattern | Purpose |
|---------|---------|
| API Gateway | Single entry point for all clients. Exposes the REST APIs, performs authentication and authorisation, transforms requests if needed, and forwards them to the appropriate microservices. |
| Circuit breaker | Prevents a failing service from cascading failures to others. A client calls a remote service through a proxy; when the proxy detects the service is down it short-circuits and fails subsequent calls immediately, and resumes normal calls once the service recovers. |
| DAO | Abstracts and encapsulates all access to the data source. The DAO manages the connection used to obtain and store data. |
| Dependency Injection | Spring Boot removes hard-coded dependencies, allowing them to be substituted at compile-time or run-time. |
| Externalized configuration | Lets a service run in multiple environments without code changes. Configuration is supplied at runtime by the platform. |
| Log aggregation | Logs from all services are collected centrally for searching and viewing. |
| Messaging Gateway | Encapsulates messaging-specific code (sending and receiving messages) and isolates it from the rest of the application, so only the gateway knows about the messaging system. |
| Standardized logging format | All services log in the same format. |
| Tolerant reader | The calling service extracts only the fields it needs from a response. The called service need not know its callers' specifics and can add response fields without breaking them. |

## 3. Secure Development Guidelines

Application security is a core part of the Simpl-Open product strategy. Security must be considered no later than the design and development phases. The Secure Development Guideline document contains the coding practices every developer must follow to produce source code free of flaws and avoidable attack vectors.