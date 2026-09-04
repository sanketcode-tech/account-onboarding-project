# Phase 4 — BPMN + Camunda

## Overview

The orchestration layer uses Camunda 8 and a BPMN process definition called `current-account-onboarding.bpmn`. The process is started when an `application.submitted` event arrives and is coordinated with Kafka consumers, user tasks, and HTTP-connected notification tasks in `onboarding-service`.

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
4. `Task_SendOfferEmail`
   - Sends the initial offer email using the Camunda HTTP JSON connector to Mailtrap.
5. `Gateway_0ex282m`
   - Event-based gateway: waits for offer response or timeout.
6. `Event_WaitOfferAccepted`
   - Receives the `OfferAccepted` message via Camunda message correlation.
7. `Task_PublishDocument`
   - Produces `document.requested`.
8. `Gateway_0xivmcz`
   - Event-based gateway: waits for document signed or document timeout.
9. `Event_WaitDocumentSigned`
   - Receives the `DocumentSigned` message.
10. `Task_SigningCeremony`
   - Human review task.
11. `Gateway_1avtep0` (`Signing Approved?`)
   - Approve path continues; reject path declines.
12. `Task_ProvisioningApproval`
   - Human review task for provisioning decision.
13. `Gateway_1dbkhuu` (`Provisioning Approved?`)
   - Approve path activates account; reject path declines.
14. `Task_ActivateAccount`
   - Persists the account record.
15. `Task_PublishActivated`
   - Produces `account.activated`.
16. `Task_SendActivatedEmail`
   - Sends a success email using the same Mailtrap HTTP connector.
17. `Event_Success`
   - End event for onboarding completion.

## Email notifications in the BPMN process

The BPMN currently includes explicit email tasks that are not handled by a separate `notification-service` component. These tasks use the Camunda `io.camunda.connectors.HttpJson.v2` connector and a Mailtrap sandbox endpoint.

- `Task_SendOfferEmail`
  - Sends a welcome/offer email after the offer is created.
- `Task_SendDeclineEmail`
  - Sends a decline/rejection email when validation, timeout, or approval fails.
- `Task_SendActivatedEmail`
  - Sends the account activation email when onboarding succeeds.

Configuration used by the connector:

- Endpoint: `https://sandbox.api.mailtrap.io/api/send/4894420`
- Authentication: bearer token via `{{secrets.MAILTRAP_API_TOKEN}}`
- Payload: JSON email body with `from`, `to`, `subject`, and `html`

This is the current implementation for email delivery in the project, even though the dedicated `notification-service` remains a scaffold and is not responsible for sending these messages.

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
- `Task_SendDeclineEmail`
  - Sends a notification email to the customer
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
| `Task_SendOfferEmail` | Camunda HTTP JSON connector -> Mailtrap |
| `Task_PublishDocument` | `PublishDocumentWorker` |
| `Task_ActivateAccount` | `ActivateAccountWorker` |
| `Task_PublishActivated` | `PublishActivatedWorker` |
| `Task_SendActivatedEmail` | Camunda HTTP JSON connector -> Mailtrap |
| `Task_PublishDeclined` | `PublishDeclinedWorker` |
| `Task_SendDeclineEmail` | Camunda HTTP JSON connector -> Mailtrap |
| message-catch events | `KafkaMessageCorrelator` |
| process start | `ApplicationSubmittedListener` |

## Current status

The BPMN process is the orchestration backbone of the onboarding platform and is where the event-driven, human-review, and email-notification logic are joined together. The latest workflow includes direct HTTP-based Mailtrap email sending through the BPMN service tasks, which means the onboarding flow is now testable end-to-end with a real outbound email sandbox.
