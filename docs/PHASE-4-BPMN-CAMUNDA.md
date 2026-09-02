# Phase 4 — BPMN + Camunda

## Overview

The orchestration layer uses Camunda 8 and a BPMN process definition called `current-account-onboarding.bpmn`. The process is started when an `application.submitted` message arrives and is enriched with Kafka-correlation listeners and task workers in the onboarding service.

## BPMN file

- File: `onboarding-service/src/main/resources/bpmn/current-account-onboarding.bpmn`
- Process ID: `current-account-onboarding`

## Main process structure

### Happy path

1. `Event_ApplicationSubmitted`
   - Start event triggered when the app processes a Kafka application submission.
2. `Task_ValidateApplication`
   - Validates required application fields.
3. `Task_PublishOffer`
   - Produces `offer.ready`.
4. `Gateway_0ex282m`
   - Event-based gateway: waits for offer response or timeout.
5. `Event_WaitOfferAccepted`
   - Receives the `OfferAccepted` message via Camunda message correlation.
6. `Task_PublishDocument`
   - Produces `document.requested`.
7. `Gateway_0xivmcz`
   - Event-based gateway: waits for document signed or document timeout.
8. `Event_WaitDocumentSigned`
   - Receives the `DocumentSigned` message.
9. `Task_SigningCeremony`
   - Human review task.
10. `Gateway_1avtep0` (`Signing Approved?`)
    - Approve path continues; reject path declines.
11. `Task_ProvisioningApproval`
    - Human review task for provisioning decision.
12. `Gateway_1dbkhuu` (`Provisioning Approved?`)
    - Approve path activates account; reject path declines.
13. `Task_ActivateAccount`
    - Persists the account record.
14. `Task_PublishActivated`
    - Produces `account.activated`.
15. `Event_Success`
    - End event for onboarding completion.

## Event-based gateways and response races

### Offer response gateway

- Gateway: `Gateway_0ex282m`
- Name: `Offer Response?`
- It waits for one of the following:
  - `OfferAccepted` message event
  - `Offer Expired (72h)` timeout event

### Document response gateway

- Gateway: `Gateway_0xivmcz`
- Name: `Document Response?`
- It waits for one of the following:
  - `DocumentSigned` message event
  - `Document Signing Expired (48h)` timeout event

These event-based gateways are the mechanism that decouples the process from the external Kafka-driven events.

## Human approval decisions

### Signing Ceremony approval

- User task: `Task_SigningCeremony`
- Exclusive gateway: `Gateway_1avtep0` (`Signing Approved?`)
- Branches:
  - approved -> continue to provisioning approval
  - rejected / default decline -> go to decline path

### Provisioning approval

- User task: `Task_ProvisioningApproval`
- Exclusive gateway: `Gateway_1dbkhuu` (`Provisioning Approved?`)
- Branches:
  - approved -> activation path
  - rejected / default decline -> decline path

## Consolidated decline path

The project has a single decline flow rather than multiple special-case closure routes.

- `Task_PublishDeclined`
  - Publishes `application.declined` to Kafka
- `Event_Declined`
  - End event reached from validation failures, timeouts, or approval rejections

This is the central terminal path for unsuccessful onboarding outcomes.

## Camunda message names used by process correlation

- `OfferAccepted`
- `DocumentSigned`

These message names are used when `KafkaMessageCorrelator` publishes correlation messages back into the running BPMN process.

## Service mapping to BPMN

| BPMN task | Worker / class |
| --- | --- |
| `Task_ValidateApplication` | `ValidateApplicationWorker` |
| `Task_PublishOffer` | `PublishOfferWorker` |
| `Task_PublishDocument` | `PublishDocumentWorker` |
| `Task_ActivateAccount` | `ActivateAccountWorker` |
| `Task_PublishActivated` | `PublishActivatedWorker` |
| `Task_PublishDeclined` | `PublishDeclinedWorker` |
| message-catch events | `KafkaMessageCorrelator` |
| process start | `ApplicationSubmittedListener` |

## Current status

The BPMN process is the orchestration backbone of the onboarding platform and is where the event-driven and human-review branches are joined together.
