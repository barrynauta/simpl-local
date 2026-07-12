# infrastructure-be: findings from local build/run (2026-07-12)

Component: `development/infrastructure/infrastructure-be` (Spring Boot 3.4, Java 21, artifact `script-service`), `main` branch (release-merged, v2.2.0 line). Assessed by building, booting in an isolated local stack, running the unit suite (246 tests, all green), and reading the security/persistence/messaging hotspots. Provisioning itself is delegated to an external provisioner over Kafka; this service does **no** local process execution, so there is no command-injection surface here.

Severity uses the operational convention: **critical** = exploitable/again-breaking in production as-is; **high** = serious defect, real impact, likely to bite; **medium** = should fix.

---

## F1: HIGH (security): authentication is fully disabled in the service

`modules/common/configs/SecurityConfig.java`:

```java
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(HttpMethod.OPTIONS).permitAll()
        .requestMatchers(permittedEndpoints.toArray(new String[0])).permitAll()
        .anyRequest().permitAll())          // <-- everything open
.oauth2ResourceServer(oauth2 -> oauth2
        .jwt(...)
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .disable())                          // <-- resource server OFF
```

Code comment: *"Authentication is temporarily disabled until alignment between the Infra and TSI teams is completed."* CSRF is also disabled on that basis.

Every endpoint is unauthenticated: reading/creating/updating/deleting deployment **scripts**, cloud **provisioner templates**, cloud **environments** (endpoints + credentials), **components**, and `POST /scripts/trigger` which **initiates provisioning** (and the decommission path). Anyone with network reach to the pod has full control of what gets provisioned and torn down on Ionos/Aruba/OVH, and can read stored cloud-environment connection data.

**Demonstrated** in the local stack, no token sent:
- `GET /cloudProviders` → 200 `{Ionos,Aruba,Ovh}`
- `POST /scripts/types {"name":"SMOKETF"}` → 201 Created, persisted.

This is the same class as the schema-manager "authorization on an unverified JWT" finding and the ADR-05 "gateway does auth, service trusts" debate, but worse: here there is no token check at all, not even an unverified decode. "Temporarily disabled" in a comment on the release line is the classic dev-shortcut-into-production pattern. Even with a Tier-1 gateway in front, this is zero in-service defence-in-depth: any direct pod path or gateway misconfig is a full compromise of the provisioning control surface.

**Fix:** re-enable `oauth2ResourceServer().jwt(...)` with real issuer verification, replace `anyRequest().permitAll()` with `authenticated()` (keeping the status/swagger allowlist), and enforce role/scope on the mutating endpoints. Track the "Infra/TSI alignment" as a ticket, not a disabled security control on `main`.

---

## F2: HIGH (reliability): the shipped `local` profile cannot start

Booting with `SPRING_PROFILES_ACTIVE=local` (as the repo's own `application-local.properties` intends) fails context refresh:

```
BeanCreationException: ... 'entityManagerFactory' ...
Failed to initialize dependency 'flyway' ...
Circular depends-on relationship between 'flyway' and 'entityManagerFactory'
```

Cause: `modules/common/configs/FlywayConfig.java` declares an **unconditional** custom `Flyway` bean that injects `DataSource` and calls `flyway.migrate()` in the `@Bean` method. Spring Boot orders `entityManagerFactory` to depend on the `flyway` bean. The `local` profile *alone* also sets `spring.jpa.defer-datasource-initialization=true`, which makes the DataSource initializer depend on `entityManagerFactory`. The result is a cycle:

```
entityManagerFactory -> flyway -> dataSource -> (deferred SQL init) -> entityManagerFactory
```

The `docker`/default profile sets `defer-datasource-initialization=false`, which is why it boots. So the local profile is dead-on-arrival for anyone following the repo.

Secondary defect in the same class: the custom `flyway` bean has no `@ConditionalOnProperty`, so it runs `migrate()` even when `spring.flyway.enabled=false`; setting that flag does not disable migrations as an operator would expect.

**Fix:** make the Flyway bean conditional and let Spring Boot's own Flyway auto-config own ordering, or drop the custom bean entirely (the standard `spring.flyway.*` properties already cover locations/baseline/validate). At minimum set `defer-datasource-initialization=false` in the local profile.

**Workaround used in this stack:** run the docker-profile schema strategy (Flyway owns schema, `ddl-auto=none`, no defer). 50 migrations then apply cleanly on Postgres 16 and the service starts in ~3.4s.

---

## F3: MEDIUM-HIGH (reliability): Kafka listeners have no dead-letter / error handling

`ProvisionedListener` (topic `provisioned`) and the decommissioned listener deserialize the message directly (`mapper.readValue(message, ArgoResponseDTO.class)`) with **no `CommonErrorHandler`, no `DeadLetterPublishingRecoverer`, no DLT** configured anywhere (grep: none). A message that fails deserialization or throws is retried by the default handler and then abandoned.

**Demonstrated:** injecting a malformed response produced:
```
DefaultErrorHandler: Backoff FixedBackOff{interval=0, currentAttempts=10, maxAttempts=9} exhausted for provisioned-0@1
```
i.e. 10 immediate (interval 0) retries hammering the partition, then the record is dropped with only an ERROR log. There is no DLT to inspect and no alert.

Impact: the `provisioned`/`decommissioned` topics carry the **only** signal that a provisioning/decommission finished. A single poison or lost message means the corresponding `ScriptTrigger` stays in its in-progress state permanently, with no reconciliation path. `scriptTriggerId` is a `Long`; any non-numeric value from the provisioner is permanently unrecoverable via this path.

**Fix:** configure a `DefaultErrorHandler` with a sensible `BackOff` (non-zero interval, bounded) and a `DeadLetterPublishingRecoverer` to a `*.DLT` topic; alert on DLT depth. Consider a reconciliation/timeout sweep for triggers stuck in-progress.

---

## F4: MEDIUM (security): the "malicious content" blocklist is configured but never enforced

`application.properties` defines 12 `malicious.pattern.*` regexes (SQL-injection, a family of XSS patterns, a shell-command pattern) and they are bound into `MaliciousContentPatternProperties`. That bean is **referenced nowhere** in the codebase (grep: only its own definition), and there is **no `Filter`, `OncePerRequestFilter`, `HandlerInterceptor`, or `@Aspect`** that could apply it. It is a dead control: the configuration and the properties bean give the appearance of input hardening that is not wired to anything.

Note this is doubly moot while F1 stands (no auth at all), but it should either be enforced or removed so it does not read as an active control in review. Separately, regex blocklisting is a weak approach to SQLi/XSS even when wired (parameterised queries / output encoding are the real controls); if the intent was defence-in-depth it needs a different mechanism.

**Fix:** remove the dead config+bean, or implement the request filter that actually applies it and document what it is meant to catch on top of JPA parameterisation.

---

## Not findings (checked, clear)

- **No local command/process execution** (`ProcessBuilder`/`Runtime.exec` absent): provisioning is delegated to the external provisioner via Kafka, so no RCE surface in this service.
- **Unit suite**: 246 tests, 0 failures/errors/skipped.
- **Vault path handling** (`sanitizeVaultPath`) exists; not exercised in the local run (Vault lazy), not assessed in depth.

## Filing recommendation

F1 and F2 are the two worth filing immediately (a security hole on the release line and a broken shipped profile). F3 is a real reliability gap worth a ticket. F4 is cleanup. Per the "file incrementally while the team is under pressure" pattern, lead with F1+F2.
