package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when the bank has prepared an offer for the customer.
 * Kafka Topic: offer.ready
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferReadyEvent {
private String applicationId;
private String offerId;
private String customerId;
private BigDecimal offeredLimit; // Optional: credit limit or account limit
private Instant createdAt;
}


