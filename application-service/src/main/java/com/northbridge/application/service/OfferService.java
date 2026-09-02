package com.northbridge.application.service;

import com.northbridge.application.model.Offer;
import com.northbridge.application.model.OfferStatus;
import com.northbridge.application.repository.OfferRepository;
import com.northbridge.common.events.OfferAcceptedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service handling offer persistence and acceptance; publishes offer.accepted when a customer accepts an offer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private final OfferRepository offerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Optional<Offer> findByApplicationId(String applicationId) {
        return offerRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Offer acceptOffer(String applicationId) {
        Offer offer = offerRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found for applicationId=" + applicationId));

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Offer is not pending for applicationId=" + applicationId);
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setAcceptedAt(Instant.now());
        Offer saved = offerRepository.save(offer);

        // include offeredLimit so downstream correlator and Camunda have it available for forms
        OfferAcceptedEvent event = new OfferAcceptedEvent(applicationId, saved.getCustomerId(), saved.getAcceptedAt(), saved.getOfferedLimit());
        kafkaTemplate.send("offer.accepted", applicationId, event);
        log.info("Accepted offer for applicationId={} and published offer.accepted event with customerId={}", applicationId, saved.getCustomerId());

        return saved;
    }
}
