# Frontend overview

This folder contains the customer-facing static frontend for the Northbridge Bank current-account onboarding flow. It is a plain HTML/CSS/JavaScript application and does not run as a separate backend service.

## Pages in the frontend

- `index.html` — landing page and product overview
- `register.html` — customer registration form
- `login.html` — login form and token setup
- `apply.html` — current account application form
- `status.html` — onboarding dashboard showing application and offer status
- `document.html` — document signing and upload screen

## What works today

### 1. Registration and login

- The browser calls the auth-service endpoints under `http://localhost:8081/api/auth`.
- Successful login stores the JWT in `sessionStorage` under `jwtToken`.
- The token is used for downstream calls to application-service and document-service.

### 2. Application submission

- The logged-in user fills in personal, employment, and account preference data.
- The front-end builds an application payload and posts it to the application-service submission endpoint.
- The flow creates an `applicationId` and redirects the user to the status page.

### 3. Status tracking and offer flow

- `status.html` polls the application status endpoint and renders the pipeline progress.
- It checks whether the application is still in review, whether an offer is available, and whether the offer has been accepted.
- When the offer is accepted, the customer is guided to continue with the document-signing step.

### 4. Document signing and file upload

- `document.html` loads the document record for the application.
- The customer can sign the document by sending a sign request to document-service.
- After signing, they can upload the signed file.
- Once uploaded, the status screen reflects the progression toward account activation.

## Current workflow from the browser perspective

1. Register or log in
2. Submit an account application
3. Wait for the backend orchestration to generate an offer
4. Accept the offer
5. Sign and upload the required document
6. See the onboarding status until approval and activation complete

## Important notes

- This frontend is intentionally lightweight and static; it is not a React or full single-page app.
- The actual orchestration and decision-making are handled by the backend services and Camunda BPMN process.
- The frontend does not directly control the BPMN tasks or email sending; it only triggers the REST endpoints that kick off and progress those workflows.
- The page flow is meant for demo/testing and customer journey validation rather than full production banking experience.

## Local testing

Open `index.html` in a browser or serve the folder with a simple static file server. The expected backend services are:

- auth-service: `localhost:8081`
- application-service: `localhost:8082`
- onboarding-service: `localhost:8083`
- document-service: `localhost:8084`
