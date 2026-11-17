<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
<p>
📍 <strong>You are here</strong> / <a href="../README.md">🏠 Home</a> / <a href="README.md">Foundations</a> / Architectural patterns
</p>
</div>

[[_TOC_]]

# Architectural Patterns

Architectural patterns are recurring design approaches applied consistently across Simpl-Open components. Where principles state what properties the system must have, patterns describe the concrete structural and behavioural techniques used to achieve them.

## Microservices Architecture

Breaks down applications into small, independently deployable services. Each service focuses on a specific business capability and communicates via APIs, enhancing flexibility and scalability.

## Event-Driven Architecture

Components communicate by producing and reacting to events, promoting loose coupling and scalability. This pattern enhances responsiveness and supports real-time processing.

## Asynchronous Communication

Allows components to interact without waiting for immediate responses. Improves system performance, decoupling, and scalability by enabling non-blocking interactions, typically using queues or background workers.

## Stateless Design

Ensures each request is independent and self-contained, avoiding reliance on stored session data. Improves scalability, simplifies fault tolerance, and supports load balancing.

## Least Privilege

Grants each user or service only the minimum level of access required to perform its tasks. This limits potential damage in the event of a breach and reduces the attack surface.

## Defence in Depth

Applies multiple layers of security controls throughout the system. If one layer is compromised, others still provide protection, improving the overall resilience of the system.

## Zero Trust

Assumes no implicit trust in users or systems, whether inside or outside the network. Continuously verifies identities and enforces strict access controls before granting access to resources.

## Retries

Automatically re-attempts failed operations after a delay, particularly useful in cases of transient failures. Helps improve reliability without requiring manual intervention.

## Circuit Breakers

Monitors service calls and halts repeated failures by stopping requests to underperforming services. This prevents cascading failures and gives systems time to recover.

## Graceful Degradation

Ensures that the system continues to provide limited or reduced functionality when some components fail. Improves user experience and system robustness under failure conditions.

