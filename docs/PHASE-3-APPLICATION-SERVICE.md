# Phase 3 — Application service

## Overview

The `application-service` stores application data, exposes offer endpoints, and emits system events into Kafka. It is the service that starts the onboarding journey by publishing the initial `application.submitted` event after an application is created.

## Service summary

- Module: `application-service`
- Default port: `8082`
- Main class: `ApplicationServiceApplication`
- Persistence: H2 in-memory database

## Key classes

- `ApplicationServiceApplication`
  - Bootstraps the application.
- `KafkaProducerService`
  - Converts an internal `ApplicationEvent` into a shared `ApplicationSubmittedEvent` and sends it to Kafka.
- `OfferController`
  - REST controller exposing offer view and accept endpoints for the customer-facing UI.
- `OfferService`
  - Loads offers and accepts them, then publishes `offer.accepted`.
- `OfferReadyListener`
  - Consumes `offer.ready` and persists Offer records.
- `OfferRepository`
  - Repository for offer records.
- `Offer`
  - JPA entity representing an offer for an application.
- `ApplicationEvent`
  - Internal application payload model used before Kafka publication.
- `AuthClient`
  - Calls the auth-service to validate tokens and request subject data.

## Kafka topics

| Topic | Direction | Producer/Consumer |
| --- | --- | --- |
| `application.submitted` | Outbound | `KafkaProducerService` |
| `offer.ready` | Inbound | `OfferReadyListener` |
| `offer.accepted` | Outbound | `OfferService.acceptOffer()` |

## API surface

### Application submission flow

The project currently emits application events in code-level flows such as test or manual submission endpoints.

- `KafkaProducerService.sendApplicationEvent(ApplicationEvent event)`
  - Normalizes the event into a `ApplicationSubmittedEvent`
  - Produces Kafka message on topic `application.submitted`
  - Sets payload metadata and customer ID fallback when needed

### Offer endpoints

`GET /api/offers/{applicationId}`
- Returns an offer after ownership validation against the current auth token

`POST /api/offers/{applicationId}/accept`
- Validates subject ownership
- Calls `OfferService.acceptOffer(applicationId)`
- Publishes `offer.accepted` to Kafka

## Offer lifecycle

1. `onboarding-service` publishes `offer.ready`
2. `OfferReadyListener` consumes the event
3. The app persists an `Offer` record
4. Customer fetches the offer via `OfferController`
5. Customer accepts it and `OfferService` sends `offer.accepted`
6. `onboarding-service` correlates the accepted message back into the BPMN flow

## Notes

- Current code makes the application-service behave as the producer of the initial submission event and the coordinator of offer acceptance.
- The project is not currently exposing a large application CRUD surface; instead it focuses on event production and offer processing.
