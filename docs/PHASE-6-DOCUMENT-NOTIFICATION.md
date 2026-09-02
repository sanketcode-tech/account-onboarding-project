# Phase 6 — Document + notification flow

## Overview

The document flow is implemented in `document-service`, which persists document metadata, validates ownership, and publishes `document.signed` after a file has been uploaded. The notification-service exists as a scaffold and does not yet implement actual delivery logic.

## Document-service summary

- Module: `document-service`
- Default port: `8084`
- Main class: `DocumentServiceApplication`
- Database: H2 in-memory database for local dev
- Main responsibilities:
  - create document records from `document.requested`
  - manage signature confirmation
  - ensure file upload happens after signing confirmation
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

## Notification-service status

The `notification-service` exists as a Spring Boot app, but there is no working notification logic in the codebase at the moment.

- `NotificationServiceApplication` is present
- No real email/SMS/consumer implementation is currently wired up
- There are no notification topic consumers found in the code

This means the project currently supports the document and onboarding flow, but not actual user notifications.

## Current status

The document flow is implemented and is a key bridge between the onboarding process and the actual signed-file review step. Notification handling remains a known gap rather than a completed feature.
