# Auth Service

## Overview
The `auth-service` is responsible for identity and access control in the onboarding platform. It handles customer sign-in, registration-related authentication flows, and JWT issuance so that other services can validate access without duplicating security logic.

## Technical responsibilities
- Spring Boot REST API for authentication requests.
- Secure password handling and user validation.
- JWT generation and token-based authorization.
- Integration with the shared domain objects and persistence layer.

## Typical flow
1. The frontend sends credentials to the auth endpoint.
2. The service validates the user.
3. A signed JWT is returned to the client.
4. Other services accept the token and enforce authorization rules.

## Testing guidance
Run the service test suite:

```bash
./mvnw -pl auth-service test
```

Start the service locally:

```bash
./mvnw -pl auth-service spring-boot:run
```

When changing token or security logic, validate the happy path and failure path using:
- successful login returns a valid JWT
- invalid credentials return a 401-style failure
- expired or malformed tokens are rejected

## Notes
Keep authentication and authorization changes tightly reviewed because they affect every protected service in the platform.
