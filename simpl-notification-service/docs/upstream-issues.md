# Upstream issues — simpl-notification-service

Tracking of issues found in upstream `simpl-notification-service` during local
evaluation. Each entry is intended to be reported upstream and is mirrored here
so we have an in-repo evidence trail.

---

## NS-001 — Kafka consumer hardcodes `SASL_PLAINTEXT`/`PLAIN`; cannot consume from a PLAINTEXT broker

**Severity:** HIGH — component is non-functional in every configuration where the broker speaks plain `PLAINTEXT`, including the broker shipped in this local stack and the env-var contract documented in upstream `application.properties`.
**Status:** Not yet reported.
**Affects:** notification-service as built from upstream `main` (the `ConsumerConfig.class` artefact dated 2026-05-05 exhibits the issue; the source has not been re-built since).
**First found:** Local evaluation, 2026-05-15.

### Summary

The notification-service ships with a Spring `@Configuration` class
`eu.europa.ec.simpl.notification_service.kafka.ConsumerConfig` that constructs a
`DefaultKafkaConsumerFactory` with `security.protocol=SASL_PLAINTEXT` and
`sasl.mechanism=PLAIN` **hardcoded** into the consumer properties map. These
values override the env-var-driven `spring.kafka.properties.security.protocol`
/ `spring.kafka.properties.sasl.mechanism` bindings that `application.properties`
documents as configurable.

Result: the consumer can ONLY connect to a broker that speaks `SASL_PLAINTEXT`
with `PLAIN` mechanism. On any plain `PLAINTEXT` broker — including the broker
this local stack ships — the consumer immediately logs
`Unexpected handshake request with client mechanism PLAIN, enabled mechanisms are []`,
fires `Fatal consumer exception`, and stops permanently. The Spring Boot
application stays up (HTTP API + actuator still respond), but the notification
path is dead.

### Reproduction

```bash
cd ~/src/simpl-local/simpl-notification-service
docker compose -f docker-compose.yml -f docker-compose.test.yml up -d
sleep 8

# 1. Consumer is dead within ~150ms of startup.
docker logs simpl-notification-service | grep -c "Fatal consumer exception"
# → 1

# 2. The advertised email path produces zero deliveries.
docker exec -i simpl-kafka kafka-console-producer --broker-list kafka:9093 --topic notifications <<< \
  '{"channel":"email","to":"test@example.com","subject":"x","message":"y"}'
sleep 3
curl -s "http://localhost:8025/api/v1/messages?limit=1" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["total"])'
# → 0
```

### Evidence

- **Decompiled class file** — the bytecode for
  `BOOT-INF/classes/eu/europa/ec/simpl/notification_service/kafka/ConsumerConfig.class`
  contains the literal strings, baked into the constant pool, not loaded from
  any externalised config:
  ```
  security.protocol
  SASL_PLAINTEXT
  sasl.mechanism
  org/apache/kafka/common/security/plain/PlainLoginModule
  ```

- **Live `ConsumerConfig values:` log line** at consumer startup confirms the
  effective Kafka client config regardless of `KAFKA_SECURITY_PROTOCOL`/
  `KAFKA_SASL_MECHANISM` env vars:
  ```
  security.protocol = SASL_PLAINTEXT
  sasl.mechanism = PLAIN
  ```

- **Crash sequence** (timestamps from a 2026-05-15 fresh run; identical to
  the stale 2026-05-05 container preserved on disk from the original
  "verified" run):
  ```
  INFO  Started NotificationServiceApplication in 1.65 seconds
  INFO  [Consumer] Failed authentication with kafka/...:9093 (channelId=-1)
        (Unexpected handshake request with client mechanism PLAIN, enabled mechanisms are [])
  ERROR [Consumer] Connection to node -1 ... failed authentication due to: ...
  ERROR Authentication/Authorization Exception and no authExceptionRetryInterval set
  ERROR Fatal consumer exception; stopping container
  INFO  notifications-id: Consumer stopped
  ```

- **`application.properties` misleadingly suggests the env vars work** for both
  paths:
  ```
  spring.kafka.properties.security.protocol=${KAFKA_SECURITY_PROTOCOL}
  spring.kafka.properties.sasl.mechanism=${KAFKA_SASL_MECHANISM}
  ```
  These are honoured by the producer (which uses Spring's auto-configured
  `ProducerFactory`). The consumer ignores them because the bespoke
  `ConsumerConfig` bean builds its own `ConsumerFactory` from scratch.

### Impact

- **The component is non-functional in every documented local-test
  configuration.** The local stack's broker is `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT`,
  matching what every cited example in upstream README/docs assumes. Against
  that broker the consumer never receives a single message.

- **`KAFKA_SECURITY_PROTOCOL` is configuration deception.** A reader of
  `application.properties` will reasonably conclude they can toggle the
  protocol via env var. They can — but only for the producer. There is no
  warning, no compile-time check, no runtime log that flags the discrepancy.

- **Cross-stack consequence.** The companion finding
  [`../../simpl-schema-manager/docs/upstream-issues.md` SSM-001](../../simpl-schema-manager/docs/upstream-issues.md)
  (the schema-manager addressing notifications to a public mailinator inbox by
  default) is only **observable** when the consumer works. Anyone evaluating
  the email path of the schema-manager will wrongly conclude "no email is
  sent" — until they bypass NS-001 by reconfiguring the broker.

- **Phase 6 of the local stack's "Status" table was incorrectly marked ✅.**
  See README Status section. The Mailpit-verification step has never worked
  in this local stack with the documented configuration; NS-002 captures the
  README correction.

### Suggested fix

Three options, ascending in scope:

1. **Surgical — drop the hardcoded protocol/mechanism in `ConsumerConfig`.**
   Read those keys from `KafkaProperties.getProperties()` so the env-var
   contract `application.properties` advertises is honoured. Approx. 10 lines
   of code change, zero API change.

2. **Stronger — delete `ConsumerConfig` entirely.** Spring Boot's
   `KafkaAutoConfiguration` already builds a `ConsumerFactory` from
   `spring.kafka.*` properties; the bespoke class duplicates that machinery
   and adds the bug. Removal has a small blast radius (only callers that
   inject the bespoke factory bean specifically), and aligns the consumer
   with the documented configuration path.

3. **Strongest — revisit ADR-03.** This stack's README already documents
   the Kafka transport as architecturally disproportionate for a single
   SMTP relay (FAIL assessment). NS-001 is further evidence: the transport
   you're forced to adopt is also misconfigured. If ADR-03 is reopened,
   replacing Kafka with REST removes the whole class of bugs by removing
   the consumer.

### Cross-references

- `simpl-schema-manager/docs/upstream-issues.md` SSM-001 — the schema-manager
  produces notifications addressed to a public mailinator inbox by default.
  NS-001 is what hides SSM-001 from anyone running the canonical local stack.
- `simpl-notification-service-local/README.md` Status table — Phase 5/6 were
  incorrectly marked ✅ in earlier revisions; root cause is this issue. See
  NS-002 (inline correction in the README).
