# Document Service

## Overview
The `document-service` manages onboarding documents and related metadata. It is responsible for handling uploaded files, signed or verification-related document records, and any document lifecycle updates required by the broader onboarding process.

## Technical responsibilities
- REST APIs for document upload and retrieval.
- Document metadata persistence and lifecycle tracking.
- Kafka-driven integration for downstream processing.
- Support for the document requirements of customer onboarding workflows.

## Testing guidance
Run the service tests:

```bash
./mvnw -pl document-service test
```

Start the service locally:

```bash
./mvnw -pl document-service spring-boot:run
```

Focus validation on:
- successful upload and retrieval of document data
- validation of required document metadata
- event publish/consume behavior when document milestones change

## Notes
Keep document contracts and file handling logic consistent because document processing often participates in compliance and verification steps.
