# Onboarding Service

## Overview
The `onboarding-service` orchestrates the end-to-end account onboarding journey. It uses BPMN/Camunda workflows to drive the process from application submission through the various functional milestones relevant to customer onboarding.

## Technical responsibilities
- Camunda BPMN process orchestration.
- Service orchestration and workflow state tracking.
- Application status transitions and business-process visibility.
- Integration with Kafka and dependent downstream services.

## Typical flow
1. A new application enters the workflow.
2. Camunda executes the configured BPMN process.
3. Each step invokes or coordinates remote services.
4. The current state is exposed so the frontend can present status updates.

## Testing guidance
Run the workflow-related test suite:

```bash
./mvnw -pl onboarding-service test
```

Start the service locally:

```bash
./mvnw -pl onboarding-service spring-boot:run
```

When changing workflow logic, validate:
- process start and completion behavior
- state transitions between statuses
- integration with Kafka or downstream service calls

## Notes
Because this is the orchestration layer, workflow changes must be validated carefully to avoid breaking the end-to-end onboarding flow.
