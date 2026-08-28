# Phase 8 — End-to-End Testing, Documentation, Finalization

Checklist
- [ ] Execute a full end-to-end manual test of the onboarding flow
  - Register/login as customer
  - Submit application
  - Confirm application persisted and `application.submitted` published
  - Claim Signing Ceremony task in Camunda Tasklist, prepare an offer
  - Publish `offer.ready`, customer accepts offer
  - Upload signed document, publish `document.signed`
  - Claim Provisioning Approval task, approve provisioning
  - Confirm `account.activated` published and notification delivered
- [ ] Validate Kafka message flow for all topics
- [ ] Verify MongoDB documents and H2 tables for metadata, accounts, documents, notifications
- [ ] Finalize README and all docs; add Postman collection or `.http` files
- [ ] Run smoke tests and basic integration tests

Goal
Validate that the system functions end-to-end across services, Camunda SaaS, Kafka, and MongoDB Atlas; ensure documentation is complete to reproduce the flow.

Acceptance Criteria
- All 5 Kafka topics receive expected messages in correct sequence
- The Camunda process instance reaches the `Onboarding Complete` end state when happy path is followed
- Notifications are recorded in H2 and logged (simulated send)
- No unhandled exceptions in service logs; errors are surfaced with structured logs

Testing Steps
1. Start local Kafka (WSL) and create topics.
2. Start backend services:
```powershell
.\mvnw.cmd -pl auth-service spring-boot:run
.\mvnw.cmd -pl application-service spring-boot:run
.\mvnw.cmd -pl onboarding-service spring-boot:run
.\mvnw.cmd -pl document-service spring-boot:run
.\mvnw.cmd -pl notification-service spring-boot:run
```
3. Serve frontend on port 3000 and perform the full flow.
4. Use Camunda Tasklist to claim/complete user tasks.
5. Monitor Kafka topics with console consumers for messages.
6. Inspect MongoDB Atlas for application JSON; inspect H2 (via console) for relational rows.

Documentation Deliverables
- Postman collection or `.http` files for all API calls
- Sequence diagram showing service interactions and event flow
- Troubleshooting guide and runbook (Kafka, Camunda, Mongo tips)

Notes
- Consider shortening timers on BPMN during testing to avoid waiting 48/24 hours; revert to real timers later.
- After successful E2E tests, prepare a cleanup script to remove test topics/messages and test data.

