# Phase 3 — application-service: Deploy & Test

Checklist
- [ ] Implement REST API to accept current account application JSON
- [ ] Persist flexible application JSON to MongoDB Atlas (document store)
- [ ] Persist relational metadata (applicationId, status, processInstanceKey) to H2 via JPA
- [ ] Validate mandatory fields and return meaningful errors
- [ ] Publish `ApplicationSubmittedEvent` to Kafka (`application.submitted` topic)
- [ ] Provide `GET /api/applications/{id}` to fetch status and metadata
- [ ] Add HTTP test file: `docs/http/application-service.http`
- [ ] Run and verify locally on port 8082

Goal
Create the application-service that receives the customer's Current Account application form, stores the full JSON in MongoDB Atlas, stores relational metadata in H2, and emits a Kafka `application.submitted` event for downstream processing.

Deliverables
- Endpoints:
  - `POST /api/applications` — create application (returns `applicationId`)
  - `GET  /api/applications/{applicationId}` — fetch application metadata and status
- MongoDB document model: `ApplicationDocument` matching spec
- JPA entity: `ApplicationMetadata` stored in H2 for quick queries
- Kafka producer wired to local Kafka bootstrap `localhost:9092` (use Spring Kafka `KafkaTemplate`)

Implementation notes
- Use `spring-boot-starter-data-mongodb` for Mongo persistence and read `MONGODB_ATLAS_URI` from `.env` when ready.
- Keep payload validation server-side (reject missing fullName, dob, PAN, etc.).
- Use `common-lib` `ApplicationSubmittedEvent` DTO to publish event (JSON serializer).

Local run
```powershell
# From repo root
.\mvnw.cmd -pl application-service spring-boot:run
```

Verification
1. Ensure Kafka is running and topic `application.submitted` exists.
2. Call `POST /api/applications` with sample JSON; expect 201 and `applicationId` in response.
3. Verify the document exists in Mongo Atlas (or check placeholder if not configured).
4. Confirm a message in Kafka topic `application.submitted` using console consumer.

Notes
- For local dev without Mongo Atlas, you can stub the Mongo connection or set a temporary local URI, but plan to use Atlas for Phase 3.

