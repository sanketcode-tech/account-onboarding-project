# Phase-by-Phase Build Order

This document outlines the incremental build and testing plan for the Northbridge Bank Onboarding System.

## Phase 1: Project Structure (✅ COMPLETED)

**Status:** Scaffolding complete — multi-module Maven repo, POMs, `common-lib` events/enums, `.env.example`, README.

**Artifacts:**
- Root `pom.xml` with Java 21, Spring Boot 4.1.0, module list
- `common-lib/` with event DTOs and `ApplicationStatus` enum
- Module skeletons for all 5 services
- Documentation: README, `.env.example`, Kafka setup guide
- Frontend stubs

**Verification:**
```bash
mvn clean verify
#use below command in ide terminal instead of above command 
#.\mvnw.cmd clean verify
# All modules should compile without errors
```

---

## Phase 2: auth-service (Login & JWT)

**Goal:** Implement customer authentication and JWT token issuance.

**Services:** auth-service (port 8081)

**Key Components:**
- JPA entity: `UserEntity` (id, username, passwordHash, fullName, email, createdAt)
- DTOs: `RegisterRequest`, `LoginRequest`, `LoginResponse`, `UserDto`
- Controllers: `AuthController` (POST /api/auth/register, POST /api/auth/login, GET /api/auth/me)
- Services: `UserService`, `JwtService`
- Security: `SecurityConfig`, `JwtAuthenticationFilter`
- Exception handler for validation errors

**Database:** H2 (in-memory)

**Testing:**
```bash
# Terminal 1: Start auth-service
mvn -pl auth-service spring-boot:run

# Terminal 2: Test endpoints
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"P@ss123","fullName":"Alice","email":"a@b.com"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"P@ss123"}'
```

**Deliverables:**
- ✅ auth-service fully functional
- ✅ JWT token generation & validation
- ✅ H2 database with user table
- ✅ Postman/HTTP test collection for auth endpoints

**Pause & Review:** Confirm auth-service is working before moving to Phase 3.

---

## Phase 3: application-service (Application Persistence)

**Goal:** Persist current account applications to MongoDB Atlas + H2 metadata; publish Kafka events.

**Services:** application-service (port 8082)

**Key Components:**
- MongoDB document: `ApplicationDocument` (applicationId, customerId, productType, personal/employment/preferences, status, processInstanceKey, timestamps)
- H2 JPA entities: `ApplicationMetadata`, `Customer`, `Account` (basic relational structure)
- DTOs: `ApplicationRequest` (form fields), `ApplicationResponse` (with applicationId)
- Kafka producer: `ApplicationEventPublisher` — publishes `ApplicationSubmittedEvent` to `application.submitted` topic
- Controller: `ApplicationController` (POST /api/applications, GET /api/applications/{id})
- Service: `ApplicationService` (validate, save, publish event)
- Exception handler for validation errors

**Database:** MongoDB Atlas (JSON), H2 (metadata)

**Kafka:** Publish to `application.submitted` (1 partition)

**Testing:**
```bash
# Terminal 1: Keep auth-service running
mvn -pl auth-service spring-boot:run

# Terminal 2: Start application-service
mvn -pl application-service spring-boot:run

# Terminal 3: Test endpoint
curl -X POST http://localhost:8082/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT-token-from-auth-service>" \
  -d '{
    "customerId": "cust-123",
    "personalDetails": {
      "fullName": "John Doe",
      "dob": "1990-01-15",
      "panNumber": "AAAA1234A",
      "address": {"city": "Bangalore", "state": "KA"}
    },
    "employmentDetails": {
      "occupation": "Software Engineer",
      "annualIncome": 1500000,
      "employerName": "Tech Corp"
    },
    "accountPreferences": {
      "branch": "Bangalore",
      "initialDeposit": 50000,
      "debitCardRequired": true
    }
  }'

# Verify Kafka message published
# (can consume from Kafka: bin/kafka-console-consumer.sh --topic application.submitted --from-beginning)
```

**Deliverables:**
- ✅ application-service REST API working
- ✅ MongoDB Atlas connected (JSON storage)
- ✅ H2 metadata tables
- ✅ Kafka producer (ApplicationSubmittedEvent)
- ✅ Validation & exception handling

**Pause & Review:** Confirm application creation and Kafka event publication before Phase 4.

---

## Phase 4: BPMN Design & Camunda 8 SaaS Integration

**Goal:** Model the BPMN workflow, deploy to Camunda 8 SaaS, configure onboarding-service to connect to Zeebe.

**Services:** onboarding-service (port 8083) + Camunda 8 SaaS

**Key Components:**
- **BPMN Diagram** (`docs/bpmn/current-account-onboarding.bpmn`):
  - Start Event: Application Submitted
  - Service Task: Persist Application (calls application-service)
  - Service Task: Publish "application.submitted" to Kafka
  - User Task: Signing Ceremony (48-hour timer)
  - Service Task: Publish "offer.ready" to Kafka
  - Message Catch Event: Wait for Customer Offer Acceptance
  - Exclusive Gateway: Offer Accepted?
  - Service Task: Provision Current Account
  - Service Task: Publish "account.activated" to Kafka
  - End Event: Onboarding Complete
- **Zeebe Configuration** in onboarding-service:
  - `CamundaClientBuilder` bean configured with SaaS credentials from env vars
  - `ProcessDeployer` (optional, disabled by default) to auto-deploy BPMN at startup
- **Controller stub:** `ProcessController` (POST /api/processes/start)

**Cloud Setup:**
1. Deploy BPMN manually from Camunda Desktop Modeler to your SaaS cluster
2. Verify BPMN is deployed in Camunda Operate

**Kafka Topics:** All 5 topics should already be created in Phase 1/Kafka setup

**Testing:**
```bash
# Terminal 1: Keep Kafka, auth-service, application-service running
# Terminal 2: Start onboarding-service
mvn -pl onboarding-service spring-boot:run

# Verify Zeebe client connected (check logs)
# Expected: "Connected to Camunda Cloud Zeebe..."

# Manually test BPMN in Camunda Tasklist:
# - Create a new process instance
# - Monitor in Operate
```

**Deliverables:**
**Deliverables (current status):**
- ✅ Initial consumer → workflow wiring implemented in `application-service` (calls `WorkflowService.startWorkflow`)
- [ ] BPMN diagram designed and modeled in Camunda Modeler
- [ ] BPMN deployed to Camunda 8 SaaS cluster
- [ ] `onboarding-service` has Zeebe client configured (SaaS mode) — pending
- [ ] ProcessDeployer (optional) wired and disabled by default — pending

**Pause & Review:** Confirm BPMN is deployed and Zeebe client connects before Phase 5.

---

## Phase 5: Job Workers & Kafka Integration

**Goal:** Implement Zeebe job workers for Service Tasks; wire Kafka producer/consumer in onboarding-service.

**Services:** onboarding-service (port 8083)

**Key Components:**
- **Job Workers** (for each Service Task):
  - `PersistApplicationWorker` — calls application-service to persist application
  - `PublishApplicationSubmittedWorker` — publishes `ApplicationSubmittedEvent` to Kafka
  - `PublishOfferReadyWorker` — publishes `OfferReadyEvent` to Kafka
  - `ProvisionAccountWorker` — creates account record, calls account-service
  - `PublishAccountActivatedWorker` — publishes `AccountActivatedEvent` to Kafka
- **Message Correlation** (for Message Catch Events):
  - `OfferAcceptanceWorker` — listens for `OfferAcceptedEvent` from Kafka, correlates to process by `applicationId`
  - `DocumentSignedWorker` — listens for `DocumentSignedEvent` from Kafka, correlates to process by `applicationId`
- **Kafka Listeners:**
  - `@KafkaListener` methods for offer acceptance and document signed events
- **Configuration:**
  - `app.cors.allowed-origins` read from environment and applied in CORS config

**Kafka Topics:** Use all 5 topics; workers will publish and consume

**Testing:**
```bash
# Terminal 1-3: Keep Kafka, auth-service, application-service running
# Terminal 4: Start onboarding-service (with job workers + Kafka listeners)
mvn -pl onboarding-service spring-boot:run

# Verify job workers are registered (check logs)
# Expected: "Subscribed to job type: ..."

# Trigger workflow:
# - Call application-service to create application
# - Monitor process instance in Camunda Operate
# - Verify job workers are executing
# - Check Kafka topics for event messages
```

**Deliverables:**
- ✅ All 5 job workers implemented
- ✅ Message correlation for async events (Kafka → Zeebe)
- ✅ Kafka producer/consumer integration
- ✅ CORS configuration externalized (`app.cors.allowed-origins`)

**Pause & Review:** Confirm job workers execute and Kafka integration works before Phase 6.

---

## Phase 6: document-service & notification-service

**Goal:** Implement document upload (document-service) and event notifications (notification-service).

**Services:** document-service (port 8084), notification-service (port 8085)

### document-service

**Key Components:**
- Controller: `UploadController` (POST /api/documents/upload)
- JPA entity: `DocumentMetadata` (id, applicationId, fileName, storageLocation, uploadedAt, signedAt)
- Service: `DocumentUploadService` (validate file, store, publish event)
- Kafka producer: publish `DocumentSignedEvent` to `document.signed` topic
- File storage: local disk (e.g., `./uploads/`) or cloud link

**Testing:**
```bash
# Terminal: Start document-service
mvn -pl document-service spring-boot:run

# Test upload
curl -X POST http://localhost:8084/api/documents/upload \
  -F "applicationId=app-123" \
  -F "file=@signed-agreement.pdf"

# Verify DocumentSignedEvent published to Kafka
```

### notification-service

**Key Components:**
- JPA entity: `Notification` (id, applicationId, customerId, type, message, sentAt)
- Repository: `NotificationRepository`
- Kafka listeners (5 separate `@KafkaListener` methods):
  - `onApplicationSubmitted(ApplicationSubmittedEvent)` — listens to `application.submitted`
  - `onOfferReady(OfferReadyEvent)` — listens to `offer.ready`
  - `onOfferAccepted(OfferAcceptedEvent)` — listens to `offer.accepted`
  - `onDocumentSigned(DocumentSignedEvent)` — listens to `document.signed`
  - `onAccountActivated(AccountActivatedEvent)` — listens to `account.activated`
- Each listener: builds notification message, persists to H2, logs simulated email/SMS
- Service: `NotificationService` (build & store notifications)

**Kafka Topics:** Consume from all 5 topics

**Testing:**
```bash
# Terminal 1-4: Keep all services running (Kafka, auth, application, onboarding, document)
# Terminal 5: Start notification-service
mvn -pl notification-service spring-boot:run

# Verify Kafka listeners are registered (check logs)
# Expected: "@KafkaListener registered for topic: application.submitted"

# Trigger full workflow:
# 1. Create application (application-service)
# 2. Watch notifications published to Kafka topics
# 3. notification-service consumes and logs notifications
# 4. H2 notifications table populated
```

**Deliverables:**
- ✅ document-service REST API (file upload)
- ✅ DocumentMetadata JPA entity
- ✅ Kafka publisher (DocumentSignedEvent)
- ✅ notification-service with 5 Kafka listeners
- ✅ Notifications table (H2)
- ✅ Console logging of simulated email/SMS

**Pause & Review:** Confirm all services can run together and Kafka topics flow correctly before Phase 7.

---

## Phase 7: Frontend (Static HTML/CSS/JS)

**Goal:** Build static web pages and connect them to backend REST APIs.

**Services:** Frontend (port 3000) — static server, not Spring Boot

**Key Pages:**
1. `login.html` — login form, calls auth-service, stores JWT
2. `apply-current-account.html` — application form (personal, employment, preferences)
3. `thank-you.html` — confirmation after submission
4. `offer.html` — shows offer, Accept/Decline buttons
5. `upload-document.html` — file upload for signed agreement
6. `status.html` — status dashboard, timeline view

**Key JavaScript:**
- `js/auth.js` — login, JWT storage, logout
- `js/application.js` — form submission, API calls
- `js/notifications.js` — polling or WebSocket for status updates
- `js/cors-enabled.js` — helper for CORS-enabled fetch calls

**Styling:**
- `css/styles.css` — Northbridge Bank branding (navy blue + gold)
- Flexbox layout, clean & professional

**Setup:**
```bash
# Install Node.js http-server or use Python
npm install -g http-server

# Terminal: Serve frontend on port 3000
cd frontend
http-server . -p 3000
```

**Testing:**
```bash
# Open browser: http://localhost:3000
# Test workflow:
# 1. Login
# 2. Fill application form
# 3. Submit
# 4. View offer
# 5. Accept/Decline
# 6. Upload document
# 7. Monitor status
```

**Deliverables:**
- ✅ All 6 pages (HTML/CSS/JS)
- ✅ Frontend connected to backend APIs (CORS-enabled)
- ✅ JWT-based authentication flow
- ✅ Application form validation
- ✅ Status polling/updates
- ✅ Professional branding

**Pause & Review:** Confirm frontend runs and communicates with all backend services before Phase 8.

---

## Phase 8: Integration Testing, Documentation, Finalization

**Goal:** End-to-end testing, complete documentation, and prepare for deployment.

**Testing:**
1. **Manual E2E flow:**
   - User logs in
   - Submits application
   - Bank staff reviews in Camunda Tasklist, prepares offer
   - Customer accepts offer
   - Customer uploads signed document
   - Bank staff approves provisioning in Camunda
   - Account activated, customer notified

2. **Kafka message flow verification:**
   - Monitor all 5 Kafka topics
   - Verify event order and timestamps

3. **Database inspection:**
   - H2: verify customers, applications, notifications tables
   - MongoDB Atlas: verify application JSON documents

4. **Error scenarios:**
   - Validation errors (missing fields)
   - Timeout handling (user doesn't accept offer within 48 hours)
   - Transaction rollbacks

**Documentation:**
- ✅ README.md (comprehensive setup & run guide)
- ✅ `.env.example` (all required env vars)
- ✅ `docs/kafka-wsl-commands.txt` (Kafka setup)
- ✅ `docs/application-local.yml.example` (config template)
- ✅ Postman collection or `.http` files (API test requests)
- ✅ Sequence diagram (process flow)
- ✅ Architecture diagram (services & integration points)

**Deliverables:**
- ✅ All services running and integrated
- ✅ End-to-end workflow tested
- ✅ Complete documentation
- ✅ Clean README with quick-start instructions

---

## Build Checklist

- [ ] Phase 1: Scaffold complete (mvn clean verify passes)
- [ ] Phase 2: auth-service deployed & tested
- [ ] Phase 3: application-service deployed & tested
- [ ] Phase 4: BPMN designed & deployed to Camunda SaaS
- [ ] Phase 5: Job workers & Kafka integration working
- [ ] Phase 6: document-service & notification-service working
- [ ] Phase 7: Frontend running on port 3000
- [ ] Phase 8: E2E testing complete, all docs written

---

## Running Everything (Production-like)

Once all phases are complete, you can run the entire system:

```bash
# Terminal 1: Kafka (keep running)
cd ~/kafka
./bin/kafka-server-start.sh config/kraft/server.properties

# Terminal 2: auth-service
mvn -pl auth-service spring-boot:run

# Terminal 3: application-service
mvn -pl application-service spring-boot:run

# Terminal 4: onboarding-service
mvn -pl onboarding-service spring-boot:run

# Terminal 5: document-service
mvn -pl document-service spring-boot:run

# Terminal 6: notification-service
mvn -pl notification-service spring-boot:run

# Terminal 7: Frontend
cd frontend
http-server . -p 3000
```

Then open `http://localhost:3000` in your browser and test the complete flow.

---

## Next Steps After Complete

- Migrate to Kafka Streams topology in notification-service (future learning)
- Add API Gateway (Kong, Zuul) for centralized auth/routing
- Add distributed tracing (Jaeger, Zipkin)
- Containerize services (Docker) and deploy to Kubernetes (optional; not part of current plan)
- Add comprehensive integration tests and CI/CD pipeline

