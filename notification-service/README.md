# Notification Service

## Overview
The `notification-service` sends user-facing updates tied to onboarding milestones. It consumes events from the platform and uses them to trigger notifications such as application status messages or business approvals.

## Technical responsibilities
- Kafka-based event consumption.
- Notification generation and delivery workflows.
- Processing of onboarding milestone events.
- Integration with the shared event contract and application lifecycle events.

## Testing guidance
Run the service tests:

```bash
./mvnw -pl notification-service test
```

Start the service locally:

```bash
./mvnw -pl notification-service spring-boot:run
```

When validating notification logic, check:
- event consumption works for application lifecycle milestones
- the service responds correctly to status changes
- notification payloads remain consistent with the shared contracts

## Notes
This service is event-driven and should be tested against realistic event payloads to avoid silent communication failures.
