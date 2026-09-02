# Phase 5 — Job workers + Kafka integration

## Overview

The onboarding service contains the system's task workers and event listeners. These workers execute the BPMN tasks, and Kafka listeners correlate external domain events back into the running Camunda process.

## Service configuration

- Module: `onboarding-service`
- Default port: `8083`
- Kafka bootstrap is configured in `application.yml`
- Camunda Cloud client configuration is present, but deployment is controlled with flags such as `BPMN_DEPLOYMENT_ENABLED` and `PROCESS_DEPLOYER_ENABLED`

## Core workers

### `ValidateApplicationWorker`

- Type: `validate-application`
- Responsibility: validate required fields (`applicationId`, `applicantName`, `email`)
- If required fields are missing, throws a `BpmnError("VALIDATION_FAILED", ...)`
- Completes the job with `validationPassed` and `validationResult`

### `PublishOfferWorker`

- Type: `publish-offer`
- Responsibility: creates `OfferReadyEvent` and sends it to Kafka topic `offer.ready`
- Completes the job with `offerPublished`, `offerId`, and `offeredLimit`

### `PublishDocumentWorker`

- Type: `publish-document`
- Responsibility: creates `DocumentSigningRequestEvent` and sends it to Kafka topic `document.requested`
- Completes the job with `documentPublished` and `documentId`

### `PublishDeclinedWorker`

- Type: `publish-declined`
- Responsibility: derives a decline reason and emits `ApplicationDeclinedEvent` on topic `application.declined`
- Completes the job with `applicationDeclined` and the decline reason

### `ActivateAccountWorker`

- Type: `activate-account`
- Responsibility: persists an `Account` record and validates `applicationId`
- Completes the job with `accountActivated` and `accountId`

### `PublishActivatedWorker`

- Type: `publish-activated`
- Responsibility: emits `AccountActivatedEvent` to Kafka topic `account.activated`
- Completes the job with `accountPublished` and `accountId`

## Kafka listeners and correlation

### `ApplicationSubmittedListener`

- Consumer group: `onboarding-service`
- Topic: `application.submitted`
- Responsibility: parse the payload and start the BPMN process with `camundaClient.newCreateInstanceCommand()`
- The BPMN process id is `current-account-onboarding`

### `KafkaMessageCorrelator`

- Topic listeners:
  - `offer.accepted`
  - `document.signed`
- Responsibility: deserialize each event and publish a Camunda message with the matching message name
- Message names used:
  - `OfferAccepted`
  - `DocumentSigned`

### `KafkaCorrelationListener`

- Legacy no-op placeholder kept for compatibility
- The code comments indicate this is intentionally not the active correlation path

## Kafka topics in onboarding-service

| Topic | Produced by | Consumed by |
| --- | --- | --- |
| `application.submitted` | `application-service` | `ApplicationSubmittedListener` |
| `offer.accepted` | `application-service` | `KafkaMessageCorrelator` |
| `document.signed` | `document-service` | `KafkaMessageCorrelator` |
| `offer.ready` | `PublishOfferWorker` | `application-service` |
| `document.requested` | `PublishDocumentWorker` | `document-service` |
| `application.declined` | `PublishDeclinedWorker` | none currently implemented |
| `account.activated` | `PublishActivatedWorker` | none currently implemented |

## Current status

The onboarding service is the orchestration and integration hub: it manages the process workers and it is the place where Kafka domain events are turned back into BPMN process messages.
