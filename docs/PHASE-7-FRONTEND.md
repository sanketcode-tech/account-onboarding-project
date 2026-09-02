# Phase 7 — Frontend

## Overview

The repository includes a static frontend in `frontend/`. It is a browser-based UI for authentication, application submission, offer viewing, document upload/signing, and onboarding status checks. The frontend references the backend services with fixed host/port values.

## Frontend entry points

The frontend code references these backend URLs:

- Auth: `http://localhost:8081/api/auth`
- Application: `http://localhost:8082/api/applications`
- Status/onboarding: `http://localhost:8083/api/status`
- Document: `http://localhost:8084/api/documents`

## What the frontend does

The `frontend/js/app.js` code contains the application layer used to:

- sign in / log in
- create and submit applications
- view or accept offers
- check onboarding status
- download or upload document files

## Service integration points

### Auth service

- `auth-service` is used for login and registration flows
- Token is used as an Authorization bearer token with downstream services

### Application service

- Used to submit applications and fetch offer progress
- Offer acceptance calls the `OfferController` endpoints in application-service

### Onboarding service

- Frontend is expected to query onboarding status from the status endpoints exposed by the service layer
- The BPMN workflow itself is driven by process state and events, not directly by the browser

### Document service

- Used to upload and fetch document files
- `signing-ceremony-form.form` in onboarding-service includes a link to the document file endpoint for review

## CORS note

Cross-origin browser access is configured around the `app.cors.allowed-origins` property in auth-service and other environment configuration. The expected browser origin is `http://localhost:3000`.

## Current status

The frontend is a simple UI layer and not a separate microservice. It is a client that exercises the REST endpoints and the Kafka-driven onboarding workflow behind the scenes.
