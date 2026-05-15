# Upstream issues — simpl-schema-manager

Tracking of issues found in upstream `simpl-schema-manager` / `simpl-schema-manager-ui` during
local evaluation. Each entry is intended to be reported upstream and is mirrored here so we
have an in-repo evidence trail.

---

## SSM-001 — `email.address` defaults to a public Mailinator inbox; Helm chart does not override

**Severity:** HIGH (security / privacy)
**Status:** Not yet reported.
**Affects:** `simpl-schema-manager` ≥ 1.0.0 (default introduced 2026-02-02), all deployments
that do not override `EMAIL_ADDRESS`.
**First found:** Local evaluation, 2026-05-15.

### Summary

The schema-manager sends an email-notification message on every schema-lifecycle event
(create, new version, status change). The recipient address is read from
`application.properties` as `email.address=${EMAIL_ADDRESS:simpl123@mailinator.com}`. The
default value is a **public, no-auth, no-account inbox** at
`https://www.mailinator.com/v4/public/inboxes.jsp?to=simpl123` — anyone with the inbox name
can read every message it has ever received.

The published Helm chart (`charts/templates/deployment.yaml`) does not bind `EMAIL_ADDRESS` to
a Secret or a `values.yaml` field; the only environment variable the chart sets on the pod is
`USE_SYSTEM_CA_CERTS`. As a result, **a vanilla `helm install` of the published chart runs in
production with the Mailinator default active**, leaking schema activity metadata to a public
inbox.

### Reproduction

```bash
# Run the schema-manager against any reachable Kafka without setting EMAIL_ADDRESS.
docker run --rm \
  -e FUSEKI_BASE_URL=http://fuseki:3030 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -p 8085:8085 \
  code.europa.eu:4567/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager:latest

# Create a schema (with a valid JWT). The resulting SendEmailRequest will have:
#   to: simpl123@mailinator.com
# This is observable in the Kafka 'notifications' topic immediately, and — once the
# notification-service consumes and dispatches it via SMTP — in the public Mailinator inbox.
```

### Evidence

- **Default value:**
  [`src/main/resources/application.properties:20`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager/-/blob/main/src/main/resources/application.properties)
  ```
  email.address=${EMAIL_ADDRESS:simpl123@mailinator.com}
  ```
- **Injection point:**
  [`src/main/java/eu/europa/ec/simpl/service/SchemaService.java:48`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager/-/blob/main/src/main/java/eu/europa/ec/simpl/service/SchemaService.java)
  ```java
  @Value("${email.address}")
  private String email;
  ```
- **Call sites (both notification builders):**
  `SchemaService.java:422` (`sendSchemaCreatedOrUpdatedEmailNotification`)
  `SchemaService.java:438` (`sendSchemaStatusChangeEmailNotification`)
  Both use `.to(email)` where `email` is the injected field above.
- **Helm chart lacks the override.** `charts/templates/deployment.yaml:152` defines the only
  `env:` block on the pod, with a single entry — `USE_SYSTEM_CA_CERTS=true`. No
  `EMAIL_ADDRESS` and no `valueFrom` reference. `charts/values.yaml` does not declare an
  `email` or `emailAddress` field either. The governance-authority overlay
  (`agents/governance-authority/app-values/simpl-schema-manager/values.yaml`) likewise
  contains no email-related setting.
- **Introduction:** Default added in commit `454623d` (`SIMPL-14791` — *"added email event
  when schema revoked"*), 2026-02-02. The publish-side notification was added earlier in
  `ae15eda` (`SIMPL-14709`), 2025-11-19, also without a Helm override.

### Impact

- **Information leak.** Notification bodies include the schema name, title, version,
  description, resource type, optional changelog text, and event timestamp. For a Governance
  Authority deployment this is metadata about EU data-space schema lifecycle activity. Any
  observer who guesses the inbox name (or reads it in upstream source) has live visibility.
- **Likely already leaking.** The default has shipped on `main` since at least November 2025.
  Any dev or staging deployment that didn't manually override `EMAIL_ADDRESS` has been
  producing real messages to the public inbox for months. Check the inbox before deciding the
  severity of historical exposure.
- **Defence-in-depth failure.** Spring Boot's `${VAR:default}` syntax silently substitutes
  defaults for unset environment variables. Combined with a Helm chart that does not declare
  the variable as required, there is no point at which the production deployment is forced to
  acknowledge the address it is sending to.

### Suggested fix

Two complementary changes:

1. **Backend — fail loud when unset.** Remove the default from `application.properties`:
   ```properties
   # Before:
   email.address=${EMAIL_ADDRESS:simpl123@mailinator.com}
   # After:
   email.address=${EMAIL_ADDRESS}
   ```
   Spring Boot will then refuse to start when `EMAIL_ADDRESS` is unset. Loud startup failure is
   strictly better than silent leak.

2. **Helm — require the variable.** Add to `charts/templates/deployment.yaml` `env:` block:
   ```yaml
   - name: EMAIL_ADDRESS
     valueFrom:
       secretKeyRef:
         name: {{ .Values.notifications.emailSecretName | required ".Values.notifications.emailSecretName is required" }}
         key: address
   ```
   And declare the required value in `charts/values.yaml` with no built-in default. The
   `required` template function ensures `helm install` aborts if the operator hasn't supplied
   a Secret reference.

3. **Test.** Add a unit/integration check that asserts `application.properties` has no
   embedded default for `email.address` — to prevent the bug from reappearing through a
   future "make it easy to dev-test" PR.

### Risk acceptance note

If for any reason the team decides the default must stay (e.g. to keep `./mvnw spring-boot:run`
working without env-var setup), the default must at minimum be:
- A non-routable address (e.g. `noreply@localhost.invalid` per RFC 6761) so leaks become DNS
  errors, not silent message delivery.
- Documented in the upstream README under "Required configuration" with explicit security
  guidance.

A public Mailinator inbox satisfies neither bar.

### Cross-references

- Related architectural observation in [`schema-manager-architecture.md`](schema-manager-architecture.md)
  and in the README's [Kafka usage](../README.md#kafka-usage) section, which also notes the
  recipient is hardcoded.
- The notification-service's own assessment
  ([`simpl-notification-service/README.md`](../../simpl-notification-service/README.md))
  predicted that the Kafka-based email path would impose disproportionate burden on callers.
  This finding is the first concrete cost — every caller now also has to worry about getting
  the recipient default right, and the upstream default is unsafe.
- [`simpl-notification-service/docs/upstream-issues.md` NS-001](../../simpl-notification-service/docs/upstream-issues.md)
  — the notification-service's consumer crashes against a `PLAINTEXT` broker, which is why
  SSM-001 is silent in the sibling `simpl-notification-service-local` stack: the leaked email
  never reaches Mailpit because the consumer dies before it can pull a message off the topic.
  In *this* stack (`--with-notifications`) the broker is configured `SASL_PLAINTEXT` to
  satisfy NS-001's hardcoded consumer expectations, which is what makes SSM-001 observable.
