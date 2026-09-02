package com.northbridge.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing an offer produced for an application (offer.ready / offer.accepted lifecycle).
 */
@Entity
@Table(name = "offers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_offer_application_id", columnNames = "application_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "offer_id", nullable = false)
    private String offerId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "offered_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal offeredLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OfferStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;
}
