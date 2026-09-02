package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.math.BigDecimal;

/**
 * Event published when the customer accepts the offer.
 * Kafka Topic: offer.accepted
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferAcceptedEvent {
    private String applicationId;
    private String customerId;
    private Instant acceptedAt;
    private BigDecimal offeredLimit;
}


