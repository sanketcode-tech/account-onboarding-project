# Phase 2 — Auth service

## Overview

The `auth-service` handles customer identity, registration, login, and JWT validation. It exposes a small REST API used by the frontend and by downstream services that need to validate tokens.

## Service summary

- Module: `auth-service`
- Default port: `8081`
- Main class: `AuthServiceApplication`
- Main config: `src/main/resources/application.yml`
- Security: Spring Security with stateless JWT-based authentication

## Key classes

- `AuthServiceApplication`
  - Bootstraps the Spring Boot application.
- `SecurityConfig`
  - Configures CORS, stateless session management, and JWT filter ordering.
- `JwtUtil`
  - Generates and validates JWTs, reads subject and userId claims.
- `AuthController`
  - Exposes `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, and `/api/auth/validate`.
- `GlobalExceptionHandler`
  - Centralizes REST error handling for invalid input and unexpected exceptions.
- `UserRepository`
  - JPA repository for users.
- `UserEntity`
  - User persistence entity in the auth database.

## Authentication flow

### Registration

`POST /api/auth/register`

- Accepts `RegisterRequest`
- Creates a user record using the service layer
- Returns a `UserProfileResponse`

### Login

`POST /api/auth/login`

- Validates email/password
- Generates a JWT with the user identifier and subject email
- Returns an `AuthResponse`

### Token validation

`GET /api/auth/validate`

- Reads the Bearer token from the `Authorization` header
- Validates it via `AuthService.isTokenValid()`
- Returns the token subject (email) when valid

### Current user profile

`GET /api/auth/me`

- Requires a valid JWT from `JwtAuthenticationFilter`
- Looks up the current user profile by ID

## Security behavior

- Session management is stateless
- Spring Security is configured to permit only the auth endpoints publicly
- `JwtAuthenticationFilter` is added before `UsernamePasswordAuthenticationFilter`
- CORS defaults to `http://localhost:63342`

## Relevant dependencies and configuration

- Password hashing is done with `BCryptPasswordEncoder`
- JWT secret and expiration are set in `application.yml`
- Service is configured to run with H2 in-memory persistence for local development

## Current status

The auth module is the identity and token service for the platform. It is working as the entry point for customer registration, login, and downstream authorization checks.
