# Phase-by-phase overview

This project is a current-account onboarding platform built as a set of Spring Boot services coordinated by Camunda 8 and Kafka. The docs below are structured by delivery phase and reflect the code currently in the repository.

## Phase 2 — Auth service

See: `docs/PHASE-2-AUTH-SERVICE.md`

Covers the auth-service module: registration/login, JWT generation/validation, security configuration, and token validation endpoints.

## Phase 3 — Application service

See: `docs/PHASE-3-APPLICATION-SERVICE.md`

Covers the application-service module: submitting applications, persisting offers, and publishing the `application.submitted` and `offer.accepted` events.

## Phase 4 — BPMN + Camunda

See: `docs/PHASE-4-BPMN-CAMUNDA.md`

Covers `current-account-onboarding.bpmn`, its service and user tasks, event-based gateways, and the consolidated decline path.

## Phase 5 — Job workers + Kafka integration

See: `docs/PHASE-5-JOB-WORKERS-KAFKA.md`

Covers onboarding-service workers, Kafka listeners, and how messages are correlated back into Camunda process instances.

## Phase 6 — Document + notification flow

See: `docs/PHASE-6-DOCUMENT-NOTIFICATION.md`

Covers the document-service workflow for document requested/signing/upload and the current notification-service scaffold status.

## Phase 7 — Frontend

See: `docs/PHASE-7-FRONTEND.md`

Covers the static frontend, service URLs, and how the browser interacts with auth-service, application-service, onboarding-service, and document-service.

## Phase 8 — End-to-end testing

See: `docs/PHASE-8-E2E-TESTING.md`

Covers how to run the services locally, verify Kafka topics, and exercise the onboarding journey from registration to account activation.

## Current architecture summary

- `auth-service` (port 8081): auth, JWT, REST endpoints.
- `application-service` (port 8082): application persistence + offer handling + Kafka events.
- `onboarding-service` (port 8083): Camunda orchestration + workers + Kafka correlation.
- `document-service` (port 8084): document lifecycle, file storage, signing workflow.
- `notification-service`: scaffold only; no functional notification logic implemented.
- `common-lib`: shared event classes and enums used across modules.

## Kafka topics in use today

| Topic | Producer | Consumer |
| --- | --- | --- |
| `application.submitted` | `application-service` | `onboarding-service` |
| `offer.ready` | `onboarding-service` | `application-service` |
| `offer.accepted` | `application-service` | `onboarding-service` |
| `document.requested` | `onboarding-service` | `document-service` |
| `document.signed` | `document-service` | `onboarding-service` |
| `application.declined` | `onboarding-service` | none currently implemented |
| `account.activated` | `onboarding-service` | none currently implemented |

## Current working assumptions

- The service ports in code are default values used by the project and are reflected in clients/forms.
- Kafka is configured against `localhost:9092` or a configured `SPRING_KAFKA_BOOTSTRAP_SERVERS` value.
- The BPMN process is the source of truth for onboarding orchestration flow.
- Notification handling remains intentionally scaffolded and not production-ready.
