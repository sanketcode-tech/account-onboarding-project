# Project Environment & Build Instructions

## Maven
- Always use `.\mvnw` in PowerShell terminal — NOT the global `mvn` command.

## Kafka
- Kafka runs inside WSL (Ubuntu distro `UbuntuUser`), NOT on Windows directly.
- Path inside WSL: `cd /home/dev/kafka`
- Start broker: `bin/kafka-server-start.sh config/server.properties`
- Zookeeper is NOT required — this project uses Kafka's KRaft mode (Kafka 4.x).
- Do not suggest Zookeeper-based setup steps or `config/kraft/server.properties`
  (that path doesn't exist in this Kafka version — use `config/server.properties` directly).

## WSL2 ↔ Windows Networking
- If a Windows-run service (via Maven/IDE) can't connect to Kafka running in WSL
  (`localhost:9092` fails, connection loops/rebootstraps), this is a known WSL2 ↔ Windows
  localhost-forwarding gap — not a Kafka config bug.
- Fix: get the current WSL IP with `hostname -I` (changes on reboot — always re-check,
  don't assume it's still a previously used IP), then in the affected service's
  `application.yml`:
```yaml
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:<CURRENT_WSL_IP>:9092}
```
- Also update `advertised.listeners` in Kafka's `config/server.properties` to match the same IP (leave the `CONTROLLER` listener as `localhost` — never change that one).

## Corporate Network / TLS
- This network uses Zscaler for SSL inspection. If any Java process throws
  `PKIX path building failed` / `unable to find valid certification path`, this is
  the Zscaler root certificate missing from that JDK's trust store — NOT a real
  certificate problem with the remote service (Camunda, Maven repos, etc.).
- Do not suggest disabling TLS verification or switching to HTTP as a fix.
- Correct fix: import the Zscaler root cert (already obtained, stored locally) into
  the specific JDK's cacerts using `keytool -importcert`. Ask which JDK path is in use before assuming — IntelliJ's run configuration JDK can differ from `JAVA_HOME`.

## Service Ports
- auth-service: 8081
- application-service: 8082
- onboarding-service: 8083
- document-service: 8084
- notification-service: 8085 (scaffold only, not yet implemented)

## Architecture Pattern — Event-Driven, Not REST-Chained
- Services trigger each other via Kafka events, not direct REST calls between
  services, wherever possible. E.g. onboarding-service consumes
  `application.submitted` directly to start the BPMN process — application-service
  does NOT call onboarding-service over REST to trigger it. Don't reintroduce a
  REST-based trigger pattern between services; prefer a new Kafka topic + listener
  if a new cross-service trigger is needed.
- Exception: read-only lookups between services (e.g. document-service checking
  offer ownership via application-service) do use direct REST calls. When doing so,
  the caller's JWT must be forwarded in the outgoing request's Authorization header — never make a headerless internal service-to-service call to a JWT-protected endpoint.

## Kafka Listener Pattern
- `@KafkaListener` methods must take a raw `String` parameter (the JSON payload) and
  parse it manually with `ObjectMapper.readValue(json, Map.class)` or a specific event
  class — NOT a typed POJO parameter with `JacksonJsonDeserializer` as the consumer's
  `value-deserializer`. A typed-POJO/JacksonJsonDeserializer combination without
  matching type headers silently produces an empty object, not an error, so this bug
  is easy to miss. Keep `value-deserializer: org.apache.kafka.common.serialization.StringDeserializer`
  on all consumers.
- Producers use `JacksonJsonSerializer` (writes plain JSON text, compatible with the
  StringDeserializer pattern above).

## Camunda Variables — Timestamp Gotcha
- Camunda's client SDK (`camunda-client-java`) uses its own internal, separate
  Jackson 2 `ObjectMapper` to serialize command variables (`.variables(Map.of(...))`
  on `newPublishMessageCommand()`, `newCreateInstanceCommand()`, `job.complete()`,
  etc.) — this is completely independent of the project's own Jackson 3 setup and
  cannot be configured/extended by us.
- Never pass a raw `java.time.Instant` (or other `java.time.*` type) directly into a
  Camunda variables map — it will throw `InvalidDefinitionException` at runtime.
  Always convert to `String` first (e.g. `.toString()`, or an explicit formatter if a
  specific timezone/format is needed).

## Camunda BPMN Modeling Rules
- Boundary events can only attach to Activities (Tasks, Subprocesses) — never to
  Events (message/timer catch events, start/end events). If a "race between a
  message and a timeout" pattern is needed on a catch event, use an Event-Based
  Gateway with two outgoing branches (one message catch event, one timer catch
  event) instead of a boundary timer on the catch event.
- FEEL expressions in output mappings and condition expressions need string literals
  in double quotes (e.g. `="VALIDATION_FAILED"`, `=signingDecision="APPROVED"`) — a
  bare unquoted word is interpreted as a variable reference and silently evaluates
  to null, not an error.
- Exclusive gateway conditions should be explicit on all outgoing flows (both the
  "approved" and "rejected" paths) rather than relying on a default flow, so
  unexpected/missing decision values raise a visible incident instead of being
  silently misrouted.

## Timezone
- All timestamps stay in UTC (Instant) throughout — storage, Kafka payloads, REST
  responses, and Camunda variables. This was a deliberate decision; do not convert
  to IST or any other timezone unless explicitly asked again.

## File Creation Policy
- Do NOT create `.md` files unless explicitly requested. Only create files that are
  actually needed for the task (code, config, BPMN, etc.). Keep the ones that are existing unless ask for removal
- `ARCHITECTURE.md` at the project root is the one maintained exception — keep it
  accurate when asked to update it, but don't regenerate it unprompted on unrelated
  tasks.

## Code Standards — No Deprecated APIs
- This project uses Spring Boot 4.1.0, Java 21, and **Jackson 3**
  (`tools.jackson.core:jackson-databind`) as the sole JSON library across all modules.
- Jackson 2 (`com.fasterxml.jackson.core:jackson-databind`) has been fully removed
  project-wide — do NOT reintroduce it. Never add `com.fasterxml.jackson.databind.ObjectMapper`
  or other Jackson 2 types back into any module (common-lib, application code, or config
  beans). Always use Jackson 3's `tools.jackson.databind.JsonMapper` / `tools.jackson.*`
  equivalents if a mapper bean is genuinely needed.
- Kafka JSON serialization: use `JacksonJsonSerializer`/`JacksonJsonDeserializer`
  (NOT the deprecated `JsonSerializer`/`JsonDeserializer`) — these are Jackson 3-based
  and now correctly aligned with the rest of the project.
- Camunda: use `io.camunda:camunda-spring-boot-starter` and `io.camunda:camunda-client-java`
  (current Camunda 8 Spring SDK) — never `zeebe-client-java`, `camunda-java-client`
  (not a real artifact — this name was hallucinated once before and doesn't exist on
  Maven Central), or other deprecated/renamed artifacts from older Camunda 7/early
  Camunda 8 tutorials. If any Camunda dependency still transitively pulls Jackson 2,
  flag it rather than silently allowing a duplicate Jackson version to creep back in.
- Before adding any new dependency that touches JSON serialization, check its transitive
  dependencies for Jackson 2 coordinates (`com.fasterxml.jackson.core`) — this project
  has already been burned once by a duplicate-version conflict between Jackson 2 and 3;
  don't reintroduce it.

## Frontend
- Plain HTML/CSS/JS only (see `frontend/`) — no React, no npm-based build toolchain.
  This is a deliberate choice to avoid introducing another dependency source that
  could hit corporate network/certificate issues.

## Code Comments
- Every class should have a short (1–3 line) class-level comment after the imports,
  before the class declaration, describing what it does and its role in the flow.
  Add this to any new class created; don't skip it.

## Git
- Do not commit directly from copilot, I will commit manually after I tested.

## Github Chat
- Before any edit, read the exact current file section and use the exact live text as old_str
- If No match found occurs, re-read the file and retry with the current content; do not reuse stale snippets from earlier edits
- Prefer narrow unique replacements instead of large multi-line blocks
- Always show me which files are added/updated/removed so I can decide whether to keep or not