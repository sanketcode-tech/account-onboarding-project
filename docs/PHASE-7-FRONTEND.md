# Phase 7 — Frontend

## Overview

The project includes a static browser frontend under `frontend/`. It is not a separate microservice; it is a plain HTML/CSS/JavaScript client that calls the REST APIs exposed by the Spring Boot services and renders a customer onboarding journey.

## Current frontend pages

The frontend currently contains these pages:

- `frontend/index.html` — landing page for Northbridge Bank current-account onboarding
- `frontend/register.html` — customer registration page
- `frontend/login.html` — login page that stores the JWT in browser storage
- `frontend/apply.html` — application form for a new account request
- `frontend/status.html` — status dashboard that polls the onboarding status and offer state
- `frontend/document.html` — document signing and upload screen

## Service integration points

The client uses these default backend URLs:

- Auth: `http://localhost:8081/api/auth`
- Application: `http://localhost:8082/api/applications`
- Status / onboarding: `http://localhost:8083/api/status`
- Document: `http://localhost:8084/api/documents`

## What the frontend is actually doing today

### 1. Authentication

`js/auth.js` stores the JWT in `sessionStorage` under `jwtToken`, validates it against `/api/auth/validate`, and exposes helpers for login/logout.

- `register.html` and `login.html` call the auth-service registration and login endpoints.
- Successful login stores the token and redirects the user into the application flow.

### 2. Apply for a current account

`apply.html` requires a logged-in customer and presents a current-account application form.

- It collects personal, employment, and preference data.
- It builds a submission event with `applicationId`, `applicantName`, `email`, and `payload`.
- It posts to `POST /api/applications/submit` in `application-service`.
- After success, it redirects to `status.html?applicationId=...`.

### 3. Track application and offer progress

`status.html` is the main onboarding dashboard.

- It reads the selected `applicationId` from URL or `sessionStorage`.
- It polls `GET /api/applications/{id}/status`.
- It checks `GET /api/applications/{id}/offer` or related offer endpoint information.
- It renders states such as under review, offer pending, offer accepted, awaiting signature, account active, or declined.
- When an offer is accepted, the UI shows a clear next step to continue to the document-signing page.

### 4. Sign and upload documents

`document.html` handles the signing step of the onboarding flow.

- It loads the document metadata for the application from the document-service.
- The customer can click `Sign Document` and send a signing request to `POST /api/documents/{applicationId}/sign`.
- Once signed, the customer can upload the signed evidence via `POST /api/documents/{applicationId}/upload`.
- After upload, the browser shows a success view and returns the user to the status page.

## Frontend behavior in the end-to-end journey

The browser flow is effectively:

1. Register or log in
2. Submit an application
3. Wait for the process to publish an offer
4. Accept the offer in the status page
5. Go to `document.html` to sign and upload the required file
6. Continue until the BPMN workflow completes and the account is activated

## Current limitations

The frontend is a lightweight UX layer and not a full production banking portal. Important caveats:

- It is plain HTML/JS, not a React or Angular app.
- The browser mainly orchestrates user journey and calls REST endpoints; the BPMN workflow keeps the real orchestration state.
- There is no dedicated staff/admin interface for approvals or BPMN task management.
- The approval decisions (signing approval and provisioning approval) are handled by the Camunda process and task forms, not directly by this static UI.

## Current status

The frontend is working as a customer-facing onboarding client for registration, application submission, offer acceptance, document signing, and status tracking. It does not yet implement a complete bank portal or a separate notification dashboard, but it successfully exercises the core onboarding path against the backend services.
