# Phase 6 — document-service & notification-service

Checklist
- [ ] Implement `document-service` endpoints for file upload and metadata storage
  - `POST /api/documents/upload` (multipart/form-data)
  - Persist `DocumentMetadata` in H2
  - Store files locally under `./uploads/` (or store URI if cloud)
  - Publish `DocumentSignedEvent` to Kafka
- [ ] Implement `notification-service` with 5 Kafka listeners (one per topic)
  - `application.submitted` → onApplicationSubmitted
  - `offer.ready` → onOfferReady
  - `offer.accepted` → onOfferAccepted
  - `document.signed` → onDocumentSigned
  - `account.activated` → onAccountActivated
- [ ] Persist notifications to H2 table `notifications` and log simulated email/SMS sends
- [ ] Add HTTP test files for upload and notification verification

Goal
Support customer signed-document uploads and persist metadata; provide a notification-service that consumes lifecycle events and generates notifications (simulated email/SMS) and stores them for audit.

Deliverables
- `document-service`:
  - Multipart upload controller (`UploadController`)
  - `DocumentMetadata` JPA entity
  - Kafka publisher for `DocumentSignedEvent`
- `notification-service`:
  - 5 `@KafkaListener` methods, one per topic
  - `Notification` JPA entity and repository
  - Logging of simulated notification delivery

Implementation notes
- Keep file uploads size-limited and validate file type (PDF) and applicationId presence
- For local dev, store files under `document-service/uploads/` and include `storageLocation` in event
- Notification messages should include `applicationId` and human-friendly text

Local run
```powershell
# Start Kafka
# Start document-service
.\mvnw.cmd -pl document-service spring-boot:run

# Start notification-service
.\mvnw.cmd -pl notification-service spring-boot:run
```

Verification
1. Upload a signed PDF for an existing `applicationId`.
2. Confirm `document.signed` published to Kafka.
3. Confirm `notification-service` consumed event, logged simulated send, and stored notification in H2.
4. Check `notification` table via H2 console or querying endpoint (if provided).

Notes
- Notifications are simulated via logs; later you can plug in an email/SMS provider.

