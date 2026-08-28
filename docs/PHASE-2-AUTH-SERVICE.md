# Phase 2 — auth-service: Deploy & Test

Checklist
- [ ] Implement authentication API (register/login/me)
- [ ] Add JPA `UserEntity` and `UserRepository` using H2
- [ ] Implement password hashing (BCrypt) and validation
- [ ] Implement JWT generation and validation (HS256 using `AUTH_JWT_SECRET`)
- [ ] Add Spring Security config and JWT filter
- [ ] Add basic integration tests and HTTP test file
- [ ] Run and verify service locally on port 8081

Goal
Implement a secure authentication service that issues JWTs to customers and exposes a minimal identity API that other services and the frontend can use.

Deliverables
- REST endpoints:
  - `POST /api/auth/register` — register a user
  - `POST /api/auth/login` — authenticate and return JWT
  - `GET  /api/auth/me` — returns current user profile (requires JWT)
- H2 in-memory database for user storage (dev profile)
- `application.yml` placeholders for JWT secret and expirations
- HTTP test collection: `docs/http/auth-service.http`

Implementation notes
- Use `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, H2, and `jjwt`.
- Passwords must be stored hashed with `BCryptPasswordEncoder`.
- JWT secret must be read from `AUTH_JWT_SECRET` environment variable.
- Keep CORS enabled and externalized via `app.cors.allowed-origins`.

Local run (after implementation)
```powershell
# From repo root
.\mvnw.cmd -pl auth-service spring-boot:run
```

Verification
1. Start service and call register endpoint.
2. Call login and receive JWT.
3. Call `/api/auth/me` with `Authorization: Bearer <token>` and confirm profile.
4. Confirm user row in H2 console (if enabled).

Notes
- Keep endpoints minimal and secure. We will extend identity/profile later when integrating with application-service and onboarding.

