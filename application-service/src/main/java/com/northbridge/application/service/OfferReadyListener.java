package com.northbridge.application.service;

import com.northbridge.application.model.Offer;
import com.northbridge.application.model.OfferStatus;
import com.northbridge.application.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferReadyListener {

    private final OfferRepository offerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "offer.ready", groupId = "application-service", containerFactory = "kafkaListenerContainerFactory")
    public void onOfferReady(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Ignoring null or blank offer.ready message");
            return;
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize offer.ready payload: {}", payloadJson, ex);
            return;
        }

        String applicationId = payload.get("applicationId") == null ? null : String.valueOf(payload.get("applicationId"));
        String offerId = payload.get("offerId") == null ? null : String.valueOf(payload.get("offerId"));
        if (applicationId == null || applicationId.isBlank() || offerId == null || offerId.isBlank()) {
            log.warn("Ignoring offer.ready event with missing applicationId or offerId. Payload: {}", payload);
            return;
        }

        if (offerRepository.findByApplicationId(applicationId).isPresent()) {
            log.info("Offer already exists for applicationId={}, skipping duplicate offer.ready event", applicationId);
            return;
        }

        Offer offer = new Offer();
        offer.setApplicationId(applicationId);
        offer.setOfferId(offerId);
        offer.setCustomerId(payload.get("customerId") == null ? null : String.valueOf(payload.get("customerId")));
        offer.setOfferedLimit(parseBigDecimal(payload.get("offeredLimit")));
        offer.setStatus(OfferStatus.PENDING);
        offer.setCreatedAt(parseInstant(payload.get("createdAt"), Instant.now()));

        Offer saved = offerRepository.save(offer);
        log.info("Persisted offer for applicationId={} offerId={} status={}", saved.getApplicationId(), saved.getOfferId(), saved.getStatus());
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private Instant parseInstant(Object value, Instant fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Instant.parse(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
