# Common Library

## Overview
The `common-lib` module provides the shared contracts that every service in the Northbridge onboarding platform depends on. It centralizes the DTOs, enums, event payloads, and validation models used across the microservices so that API and messaging contracts stay consistent.

## Responsibilities
- Shared request and response DTOs for application, auth, document, and notification flows.
- Common domain enums used across the onboarding lifecycle.
- Reusable event objects for Kafka-driven communication between services.
- JSON-friendly model classes with Lombok support to avoid duplication in each service.

## Why it matters
Because the project is split into multiple Spring Boot services, this library prevents drift between service contracts. Changes to a shared model are made in one place and then consumed by all dependent services.

## Testing guidance
Run the library tests directly:

```bash
./mvnw -pl common-lib test
```

For a quick validation after changing shared DTOs or serialization behavior, verify that both the library and dependent modules compile:

```bash
./mvnw test
```

## Notes
Keep model changes backward-compatible whenever possible, because all services share the same contract definitions.
