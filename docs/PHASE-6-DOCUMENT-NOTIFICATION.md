# Phase 6 — Document + notification flow

## Overview

The document flow is implemented in `document-service`, which persists document metadata, validates ownership, and publishes `document.signed` after a signed file has been uploaded. The current notification behavior is handled in the BPMN workflow itself through Camunda HTTP connector tasks rather than a dedicated `notification-service` consumer.

## Document-service summary

- Module: `document-service`
- Default port: `8084`
- Main class: `DocumentServiceApplication`
- Database: H2 in-memory database for local dev
- Main responsibilities:
  - create document records from `document.requested`
  - manage signature confirmation
  - enforce upload only after signing confirmation
  - publish the `document.signed` event

## Key classes

- `DocumentRequestedListener`
  - Consumes `document.requested`
  - Creates a `Document` entity if it does not already exist
- `DocumentService`
  - Handles signing and upload logic
  - Publishes `document.signed` when upload succeeds
- `DocumentController`
  - Exposes document retrieval, file download, upload, and sign endpoints
- `Document`
  - JPA entity storing document metadata and status
- `DocumentRepository`
  - Repository used for lookup by `applicationId`
- `OfferClient`
  - Calls application-service to get customer information for authorization checks
- `AuthClient`
  - Validates token and user identity for document actions
- `KafkaConfig`
  - Kafka producer/consumer setup for the document-service

## Kafka topics

| Topic | Produced by | Consumed by |
| --- | --- | --- |
| `document.requested` | `onboarding-service` | `document-service` |
| `document.signed` | `document-service` | `onboarding-service` |

## Document lifecycle

### 1. Document request

`PublishDocumentWorker` in onboarding-service emits `document.requested`.

### 2. Document record creation

`DocumentRequestedListener` consumes the event and saves a record.

### 3. Sign confirmation

`DocumentController.signDocument()` calls `DocumentService.signDocument()`.

- It marks the document as signed in metadata
- It stores signer name if present
- It does not publish a Kafka event here because the actual file upload is what triggers final signature publication

### 4. Upload file

`DocumentController.uploadDocument()` delegates to `DocumentService.uploadDocument()`.

- Checks that the document has been signed
- Writes to `uploads/`
- Stores the file path in `storageLocation`
- Saves status as `UPLOADED`
- Publishes `document.signed` to Kafka

### 5. File retrieval

`DocumentController.getFile()` allows the customer or officer to fetch the uploaded document.

## Notification flow status

The dedicated `notification-service` remains scaffolded and is not the active email delivery implementation.

The current project uses the BPMN task service connectors instead:

- `Task_SendOfferEmail`
- `Task_SendDeclineEmail`
- `Task_SendActivatedEmail`

These tasks use the Camunda `HttpJson` connector to `https://sandbox.api.mailtrap.io/api/send/4894420` with a bearer `MAILTRAP_API_TOKEN`. This is the real outbound email mechanism currently used to send onboarding updates to customers in the sandbox environment.

## Current status

The document flow is implemented and is the bridge between onboarding orchestration and signed evidence upload. Email notifications are now handled by the BPMN process via the Mailtrap HTTP connector, while the separate notification-service remains a future extension rather than an active delivery component.
