# Phase 5 — Job workers & Kafka integration

Checklist
- [ ] Implement Zeebe job workers in `onboarding-service` for all Service Tasks
  - PersistApplicationWorker
  - PublishApplicationSubmittedWorker
  - PublishOfferReadyWorker
  - ProvisionAccountWorker
  - PublishAccountActivatedWorker
- [ ] Implement Kafka consumers to correlate `offer.accepted` and `document.signed` messages back to Zeebe process instances
- [ ] Ensure `applicationId` is used as the correlation key when completing/continuing process instances
- [ ] Configure Spring Kafka `KafkaTemplate` (producer) and `@KafkaListener` (consumer)
- [ ] Add retries and error handling for workers and Kafka listeners
- [ ] Test with local Kafka and verify end-to-end flow for automated steps

Goal
Wire the `onboarding-service` to act as the orchestration glue: Zeebe job workers execute service tasks and publish events to Kafka; Kafka consumers correlate incoming messages to waiting process instances.

Deliverables
- Zeebe job worker implementations in `onboarding-service/src/main/java/com/northbridge/onboarding/worker/`
- Kafka producers publishing `common-lib` event DTOs to the right topics
- Kafka listeners that, on consumption, call Zeebe to correlate messages (by `applicationId`)
- Logs (SLF4J) at each step for traceability

Implementation notes
- Use `camunda-client-java` or Camunda Spring Boot starter if added in Phase 4/5
- For Kafka, prefer Spring Kafka with `JacksonJsonDeserializer`/`JacksonJsonSerializer` using `com.northbridge.common.events` package
- Make job workers idempotent where possible

Local run
```powershell
# Start Kafka
# Start application-service (to accept and persist application)
.
# Start onboarding-service (job workers)
.\mvnw.cmd -pl onboarding-service spring-boot:run
```

Verification
1. Submit an application via application-service.
2. Confirm `application.submitted` event published to Kafka.
3. Confirm Zeebe job worker processes `PersistApplication` and subsequent tasks.
4. Simulate `offer.ready` and `offer.accepted` messages and confirm process proceeds.
5. Check logs for successful correlation and job completions.

Notes
- Keep job type names and BPMN service task `type` attributes consistent between model and worker code.
- Consider temporarily shortening timers in BPMN while testing escalations.

