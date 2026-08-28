# Application Service

## Overview
The `application-service` owns the onboarding application lifecycle for new customers. It accepts application payloads from the frontend, validates them, stores them, and publishes events so that downstream services can continue processing the application.

## Technical responsibilities
- REST endpoints for submission and retrieval of onboarding applications.
- JPA persistence for application records.
- Validation of account application data before persistence.
- Kafka event publication for status updates and downstream orchestration.

## Typical flow
1. A user submits an application from the frontend.
2. The service validates required fields and application data.
3. The application is stored in the backing database.
4. Domain events trigger additional processing by other services.

## Testing guidance
Run the test suite for this service:

```bash
./mvnw -pl application-service test
```

Run it locally:

```bash
./mvnw -pl application-service spring-boot:run
```

Focus validation on:
- valid applications are accepted and stored
- validation errors fail without persisting invalid data
- Kafka events are emitted when application processing advances

## Notes
This service is the primary entry point for onboarding requests and should remain consistent with the shared contract library.
