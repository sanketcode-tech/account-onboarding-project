# Phase 8 — End-to-end testing with Postman

This document lists the real endpoints available in the project and the sample payloads to use in Postman for end-to-end testing.

## 1) Base URLs

| Service | Base URL | Notes |
| --- | --- | --- |
| Auth service | `http://localhost:8081` | JWT login/register
| Application service | `http://localhost:8082` | app submission + offers |
| Onboarding service | `http://localhost:8083` | BPMN workflow start/health |
| Document service | `http://localhost:8084` | document metadata, upload, sign |

## 2) Pre-requisites

- Start Kafka locally and make sure the services can reach it.
- Start all services in this order:
  - `auth-service`
  - `application-service`
  - `document-service`
  - `onboarding-service`
- Use Postman with `Authorization: Bearer <token>` on requests that need JWT validation.

## 3) Endpoint list

### A. Auth Service

#### 1. Register new user
- Endpoint: `POST http://localhost:8081/api/auth/register`
- Description: Creates a new customer account.
- Body (JSON):
```json
{
  "email": "john.doe@example.com",
  "password": "Password123!",
  "firstName": "John",
  "lastName": "Doe"
}
```
- Expected result: HTTP `201 Created`

#### 2. Login user
- Endpoint: `POST http://localhost:8081/api/auth/login`
- Description: Validates credentials and returns the JWT token.
- Body (JSON):
```json
{
  "email": "john.doe@example.com",
  "password": "Password123!"
}
```
- Expected result: HTTP `200 OK`
- Save the token from the response:
```json
{
  "token": "<jwt-token>",
  "type": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe"
  }
}
```

#### 3. Validate JWT
- Endpoint: `GET http://localhost:8081/api/auth/validate`
- Description: Checks whether the token is valid.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Expected result: HTTP `200 OK`
```json
{
  "subject": "john.doe@example.com"
}
```

#### 4. Get current user profile
- Endpoint: `GET http://localhost:8081/api/auth/me`
- Description: Returns the current authenticated user profile.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Expected result: HTTP `200 OK`

### B. Application Service

#### 5. Health check
- Endpoint: `GET http://localhost:8082/api/v1/applications/health`
- Description: Checks if application-service is running.
- Expected result: `Application Service is running`

#### 6. Test Kafka event send
- Endpoint: `POST http://localhost:8082/api/v1/applications/test-send?applicationId=APP-1001&applicantName=John%20Doe`
- Description: Sends a sample `application.submitted` event into Kafka using the project helper.
- Expected result: HTTP `200 OK`

#### 7. Submit custom application event
- Endpoint: `POST http://localhost:8082/api/v1/applications/submit`
- Description: Submits a custom application payload; the service sets `customerId` from the JWT subject.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Body (JSON):
```json
{
  "applicationId": "APP-1001",
  "customerId": "john.doe@example.com",
  "applicantName": "John Doe",
  "email": "john.doe@example.com",
  "status": "SUBMITTED",
  "eventType": "APPLICATION_SUBMITTED",
  "timestamp": "2026-09-02T15:00:00",
  "payload": {
    "productType": "CURRENT_ACCOUNT",
    "source": "postman-e2e",
    "notes": "Customer applied for current account"
  }
}
```
- Expected result: HTTP `202 Accepted`

#### 8. Get offer by application
- Endpoint: `GET http://localhost:8082/api/offers/{applicationId}`
- Description: Returns the offer stored for an application, after validating ownership.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Example:
```http
GET http://localhost:8082/api/offers/APP-1001
```
- Expected result: HTTP `200 OK` with a JSON offer object when the `offer.ready` event has already been emitted and persisted.

#### 9. Accept offer
- Endpoint: `POST http://localhost:8082/api/offers/{applicationId}/accept`
- Description: Accepts the current offer and emits `offer.accepted` to Kafka.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Example:
```http
POST http://localhost:8082/api/offers/APP-1001/accept
```
- Expected result: HTTP `200 OK`

### C. Onboarding Service

#### 10. Manual workflow start
- Endpoint: `POST http://localhost:8083/api/workflows/start`
- Description: Starts the Camunda process manually if `process.start.api.enabled` is enabled.
- Body (JSON):
```json
{
  "applicationId": "APP-1001",
  "customerId": "john.doe@example.com",
  "applicantName": "John Doe",
  "email": "john.doe@example.com",
  "status": "SUBMITTED",
  "variables": {
    "productType": "CURRENT_ACCOUNT"
  }
}
```
- Expected result: HTTP `202 Accepted` with a `processInstanceKey` value.

#### 11. Onboarding health check
- Endpoint: `GET http://localhost:8083/api/workflows/health`
- Description: Checks if the onboarding service is up.
- Expected result: `Onboarding Service is running`

### D. Document Service

#### 12. Get document metadata
- Endpoint: `GET http://localhost:8084/api/documents/{applicationId}`
- Description: Returns the document metadata for an application.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Example:
```http
GET http://localhost:8084/api/documents/APP-1001
```
- Expected result: HTTP `200 OK` with document metadata when the document request has been created.

#### 13. Sign document
- Endpoint: `POST http://localhost:8084/api/documents/{applicationId}/sign`
- Description: Confirms the customer has signed the document.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Body (JSON):
```json
{
  "signerName": "John Doe"
}
```
- Expected result: HTTP `200 OK`

#### 14. Upload signed document file
- Endpoint: `POST http://localhost:8084/api/documents/{applicationId}/upload`
- Description: Uploads the actual PDF/image file after signing confirmation. This is the point that publishes `document.signed` to Kafka.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Body type: `form-data`
  - `file`: choose a file, e.g. `sample-document.pdf`
- Expected result: HTTP `200 OK`

#### 15. Download document file
- Endpoint: `GET http://localhost:8084/api/documents/{applicationId}/file`
- Description: Downloads the uploaded file for review.
- Headers:
  - `Authorization: Bearer <jwt-token>`
- Expected result: HTTP `200 OK`, `Content-Type` based on file type.

## 4) Suggested end-to-end Postman sequence

1. Register a user
2. Login and copy token
3. Submit application to `/api/v1/applications/submit`
4. Confirm `application.submitted` is produced and the onboarding BPMN starts
5. Wait for the process to reach the offer step and check `offer.ready`
6. GET the offer from `/api/offers/{applicationId}`
7. POST to `/api/offers/{applicationId}/accept`
8. Confirm `offer.accepted` is produced and correlated into Camunda
9. Wait for the document step; `/api/documents/{applicationId}` should exist when `document.requested` is processed
10. POST `/api/documents/{applicationId}/sign`
11. Upload the file to `/api/documents/{applicationId}/upload`
12. Confirm `document.signed` is produced and correlated to Camunda
13. Complete the human review tasks in the BPMN process (Signing Ceremony, Provisioning Approval)
14. Verify final activation path and confirm `account.activated` is produced

## 5) Test data examples

Use these values for quick manual testing:

```json
{
  "applicationId": "APP-1001",
  "customerId": "john.doe@example.com",
  "applicantName": "John Doe",
  "email": "john.doe@example.com",
  "productType": "CURRENT_ACCOUNT",
  "status": "SUBMITTED"
}
```

For password:
```text
Password123!
```

For document upload:
- File name: `sample-document.pdf`
- File type: PDF or image

## 6) Kafka topics to watch while testing

Use Kafka console or a Kafka UI to watch these topics:

- `application.submitted`
- `offer.ready`
- `offer.accepted`
- `document.requested`
- `document.signed`
- `application.declined`
- `account.activated`

## 7) Common Postman tips

- Save the JWT token in a Postman variable: `{{authToken}}`
- Add one collection-level variable for `baseAuth`, `baseApp`, `baseOnboarding`, `baseDocument`
- For file upload, choose `Body > form-data` and set the key to `file`
- If a request returns `401 Unauthorized`, re-login and refresh the token
- If a request returns `403 Forbidden`, verify the JWT subject matches the `customerId` stored for the application or offer

## 8) Expected successful flow summary

A successful end-to-end test should emit these Kafka events in order:

1. `application.submitted`
2. `offer.ready`
3. `offer.accepted`
4. `document.requested`
5. `document.signed`
6. `account.activated`

The flow ends with the BPMN process reaching `Event_Success` when the account has been activated.
