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
- Also update `advertised.listeners` in Kafka's `config/server.properties` to match the
  same IP (leave the `CONTROLLER` listener as `localhost` — never change that one).

## Corporate Network / TLS
- This network uses Zscaler for SSL inspection. If any Java process throws
  `PKIX path building failed` / `unable to find valid certification path`, this is
  the Zscaler root certificate missing from that JDK's trust store — NOT a real
  certificate problem with the remote service (Camunda, Maven repos, etc.).
- Do not suggest disabling TLS verification or switching to HTTP as a fix.
- Correct fix: import the Zscaler root cert (already obtained, stored locally) into
  the specific JDK's cacerts using `keytool -importcert`. Ask which JDK path is in use
  before assuming — IntelliJ's run configuration JDK can differ from `JAVA_HOME`.

## File Creation Policy
- Do NOT create `.md` files unless explicitly requested. Only create files that are
  actually needed for the task (code, config, BPMN, etc.)

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
  (current Camunda 8 Spring SDK) — never `zeebe-client-java` or other deprecated/renamed
  artifacts from older Camunda 7/early Camunda 8 tutorials. If any Camunda dependency
  still transitively pulls Jackson 2, flag it rather than silently allowing a duplicate
  Jackson version to creep back in.
- Before adding any new dependency that touches JSON serialization, check its transitive
  dependencies for Jackson 2 coordinates (`com.fasterxml.jackson.core`) — this project
  has already been burned once by a duplicate-version conflict between Jackson 2 and 3;
  don't reintroduce it.

## Git
- Do not commit directly from copilot, I will commit manually after I tested