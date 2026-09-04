ARCHITECTURE — Current design (derived from code)

1) High-level overview

This repository implements a current-account onboarding platform composed of small Spring Boot services that communicate asynchronously via Kafka and are orchestrated by Camunda 8 (Zeebe-based workflows). Core responsibilities are split across services: authentication, application persistence, document handling, onboarding orchestration (BPMN workers + process), and notifications. The orchestration BPMN is current-account-onboarding.bpmn under onboarding-service resources.

2) Services

- auth-service (port: explicit references to 8081 in clients/frontend)
  - Responsibility: customer authentication and JWT issuance
  - Key classes
    - AuthServiceApplication — main Spring Boot app
    - AuthController — Authentication API controller (register/login/validate)
    - JwtUtil — JWT generation & validation utility
    - GlobalExceptionHandler — REST error handler
  - Kafka: none (REST only)

- application-service (port: referenced as 8082 in clients)
  - Responsibility: application persistence and producing application.submitted events
  - Key classes
    - ApplicationServiceApplication — main Spring Boot app
    - KafkaProducerService — sends application.submitted (maps internal ApplicationEvent -> common ApplicationSubmittedEvent)
    - OfferController — REST controller exposing offer view and accept endpoints
    - OfferService — persists offers and publishes offer.accepted on accept
    - OfferReadyListener — consumes offer.ready and persists Offer records
  - Kafka topics produced: application.submitted, offer.accepted
  - Kafka topics consumed: offer.ready

- document-service (port: referenced as 8084 in forms/frontend)
  - Responsibility: manage document lifecycle, accept uploads and publish document.signed
  - Key classes
    - DocumentServiceApplication — main Spring Boot app
    - DocumentRequestedListener — listens to document.requested to create Document records
    - DocumentService — manages signing confirmation and file upload; publishes document.signed when file saved
    - DocumentController — REST controller for document retrieval, file download/upload and signing endpoints
    - DocumentRepository — JPA repository (lookup by applicationId)
    - SignRequest — DTO for signer info
    - KafkaConfig / RestTemplateConfig — infra beans
    - OfferClient — queries application-service to verify ownership (uses application.service.url default http://localhost:8082)
  - Kafka topics produced: document.signed
  - Kafka topics consumed: document.requested

- onboarding-service (port: referenced as 8083 in .env.example)
  - Responsibility: BPMN orchestration (current-account-onboarding.bpmn), Zeebe workers, correlating Kafka messages to Camunda messages
  - Key classes
    - OnboardingServiceApplication — main Spring Boot app
    - ApplicationSubmittedListener — listens to application.submitted and starts the BPMN process
    - ValidateApplicationWorker — Camunda worker validating application fields
    - PublishOfferWorker — worker publishing OfferReadyEvent to topic offer.ready
    - PublishDocumentWorker — worker publishing DocumentSigningRequestEvent to topic document.requested
    - KafkaMessageCorrelator — listens to offer.accepted and document.signed and publishes Camunda messages to correlate process instances
    - PublishDeclinedWorker — worker publishing application.declined
    - PublishActivatedWorker / ActivateAccountWorker — activate account and publish account.activated
    - BPMN service tasks `Task_SendOfferEmail`, `Task_SendDeclineEmail`, `Task_SendActivatedEmail` — send lifecycle emails via Mailtrap HTTP connector
  - Kafka topics produced: offer.ready, document.requested, application.declined, account.activated
  - Kafka topics consumed: application.submitted, offer.accepted, document.signed

- notification-service (port: not explicitly configured in code)
  - Responsibility: scaffolded placeholder service; not the active email delivery mechanism
  - Key classes
    - NotificationServiceApplication — main Spring Boot app; currently a scaffold (no implemented consumers)
  - Current email delivery: handled directly in BPMN via Camunda HTTP connector service tasks (`Task_SendOfferEmail`, `Task_SendDeclinedEmail`, `Task_SendActivatedEmail`) to the Mailtrap sandbox API
  - Kafka: no active consumers/producers found in code (service is not yet used for lifecycle notifications)

- common-lib
  - Responsibility: shared DTOs/events and enums used across services
  - Key classes/types
    - ApplicationSubmittedEvent — common event shape for application.submitted
    - OfferReadyEvent, OfferAcceptedEvent, DocumentSigningRequestEvent, DocumentSignedEvent, ApplicationDeclinedEvent, AccountActivatedEvent — event classes used across services
    - ApplicationStatus — enum of application lifecycle states
  - Kafka: none (library only)

- frontend (port: 63342 in README/js)
  - Responsibility: customer-facing UI (static frontend in repo)
  - Notes: endpoints and ports referenced: auth -> 8081, application -> 8082, status/onboarding -> 8083, document -> 8084

3) End-to-end flow (step-by-step)

1. Register / Login
   - Service: auth-service (AuthController)
   - Topic: none (REST)
   - BPMN element: N/A (authentication happens outside BPMN)

2. Submit application
   - Service: application-service (KafkaProducerService)
   - Topic: application.submitted (produced)
   - BPMN: ApplicationSubmitted start event -> ApplicationSubmittedListener starts process (bpmn startEvent Event_ApplicationSubmitted)

3. Validation
   - Service: onboarding-service (ValidateApplicationWorker)
   - Topic: none
   - BPMN: serviceTask Task_ValidateApplication; validation failure leads to decline flow (BPMN error handling / decline path)

4. Offer published
   - Service: onboarding-service (PublishOfferWorker) — serviceTask Task_PublishOffer
   - Topic: offer.ready (produced)
   - BPMN: after publish, event-based gateway Gateway_0ex282m waits for offer.accepted or offer timeout

5. Application-service persists offer
   - Service: application-service (OfferReadyListener)
   - Topic: consumes offer.ready
   - BPMN: no BPMN element here (consumer is external persistence)

6. Customer views / accepts offer
   - Service: application-service (OfferController -> OfferService)
   - Topic: offer.accepted (produced on accept)
   - BPMN: onboarding-service Correlator consumes offer.accepted; intermediateCatchEvent Event_WaitOfferAccepted receives message (messageName OfferAccepted)

7. Document requested
   - Service: onboarding-service (PublishDocumentWorker) produces document.requested (serviceTask Task_PublishDocument)
   - Topic: document.requested
   - BPMN: event-based gateway Gateway_0xivmcz waits for document.signed or document timeout

8. Customer signs and uploads
   - Service: document-service (DocumentRequestedListener creates Document), then client uploads via DocumentController -> DocumentService.uploadDocument
   - Topic: document.signed (produced when file saved)
   - BPMN: onboarding-service Correlator consumes document.signed; intermediateCatchEvent Event_WaitDocumentSigned receives message

9. Signing Ceremony (human review)
   - Service: onboarding-service (user task Task_SigningCeremony)
   - Topic: N/A (user task in Camunda forms)
   - BPMN: userTask Signing Ceremony -> exclusiveGateway Gateway_1avtep0 (Signing Approved? -> continue or decline)

10. Provisioning Approval (human review)
    - Service: onboarding-service (user task Task_ProvisioningApproval)
    - Topic: N/A
    - BPMN: userTask Provisioning Approval -> exclusiveGateway Gateway_1dbkhuu (Provisioning Approved? -> Activate or decline)

11. Account activation
    - Service: onboarding-service (ActivateAccountWorker + PublishActivatedWorker)
    - Topic: account.activated (produced)
    - BPMN: serviceTask Task_ActivateAccount then Task_PublishActivated -> endEvent Event_Success

12. Account details visible
    - Service: onboarding-service persists account (AccountRepository used by ActivateAccountWorker); downstream UIs / services can query
    - Topic: account.activated (event to notify other systems)
    - BPMN: endEvent Event_Success

Decline path
- Any validation failure, timeouts (Offer Expired, Document Signing Expired), or explicit user rejection is funneled to Task_PublishDeclined which publishes application.declined and ends at Event_Declined.
- Service: onboarding-service (PublishDeclinedWorker)
- Topic: application.declined

4) Kafka topics (reference)

| Topic | Produced by | Consumed by |
|-------|-------------|-------------|
| application.submitted | application-service (KafkaProducerService) | onboarding-service (ApplicationSubmittedListener)
| offer.ready | onboarding-service (PublishOfferWorker) | application-service (OfferReadyListener)
| offer.accepted | application-service (OfferService.acceptOffer) | onboarding-service (KafkaMessageCorrelator)
| document.requested | onboarding-service (PublishDocumentWorker) | document-service (DocumentRequestedListener)
| document.signed | document-service (DocumentService.uploadDocument) | onboarding-service (KafkaMessageCorrelator)
| application.declined | onboarding-service (PublishDeclinedWorker) | (no consumers in code; placeholder for notification/archival)
| account.activated | onboarding-service (PublishActivatedWorker) | (no consumers in code; placeholder for notification/other systems)

5) BPMN process reference (current-account-onboarding.bpmn)

- Start: Event_ApplicationSubmitted — process triggered by application.submitted
- Task_ValidateApplication — serviceTask (ValidateApplicationWorker)
- Task_PublishOffer — serviceTask (PublishOfferWorker)
- Gateway_0ex282m — event-based gateway that races between: Event_WaitOfferAccepted (message catch) and Event_OfferTimeout (timer/timeout)
- Task_PublishDocument — serviceTask (PublishDocumentWorker)
- Gateway_0xivmcz — event-based gateway that races between: Event_WaitDocumentSigned (message catch) and Event_DocumentTimeout (timer/timeout)
- Task_SigningCeremony — userTask (human review). Followed by exclusive gateway Gateway_1avtep0 (Signing Approved?) — approve = continue, reject = decline
- Task_ProvisioningApproval — userTask (human review). Followed by exclusive gateway Gateway_1dbkhuu (Provisioning Approved?) — approve = Activate, reject = decline
- Task_ActivateAccount / Task_PublishActivated — service tasks to persist account and publish account.activated
- Task_PublishDeclined -> Event_Declined — consolidated decline flow for all rejection/timeouts

6) Known gaps / not yet implemented (derived from code)

- notification-service is a scaffold (NotificationServiceApplication exists) but no consumers or email/SMS sending logic found — notifications are not yet implemented.
- application.declined and account.activated topics are published but have no consumers in the current codebase — downstream handling (notification, archival, downstream systems) is not implemented.
- Frontend and module README/docs were removed because they were outdated. The frontend references ports (3000 and backends 8081..8084) in code; verify environment before running.
- No explicit HTTP port configuration for notification-service found in code; other services have client defaults: auth 8081, application 8082, onboarding 8083 (env/example), document 8084 — these are defaults used in clients/forms, but per-service application.yml may override at runtime.

Deleted files (scanned and removed):
- frontend/README.md
- document-service/README.md
- notification-service/README.md
- application-service/README.md
- .github/copilot-instructions.md
- auth-service/README.md
- common-lib/README.md
- onboarding-service/README.md

Notes / next steps
- If any of the removed docs must be preserved for audit, revert from VCS. The new ARCHITECTURE.md is authoritative and derived from live code only.
- Recommend adding simple consumers for application.declined and account.activated (notification-service) or hook them into monitoring/archival.

(End of ARCHITECTURE.md)
