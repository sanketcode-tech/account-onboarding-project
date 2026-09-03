package com.northbridge.onboarding.decline;

import com.northbridge.common.events.ApplicationDeclinedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listens for application.declined Kafka events and persists a DeclineRecord.
 */
@Slf4j
@Component
public class DeclineEventListener {

    private final DeclineRepository declineRepository;

    public DeclineEventListener(DeclineRepository declineRepository) {
        this.declineRepository = declineRepository;
    }

    @KafkaListener(topics = "application.declined", groupId = "onboarding-service", containerFactory = "kafkaListenerContainerFactory")
    public void onApplicationDeclined(String payloadJson) {
        try {
            // payload may be JSON serialized ApplicationDeclinedEvent or plain JSON map
            tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
            ApplicationDeclinedEvent event = mapper.readValue(payloadJson, ApplicationDeclinedEvent.class);

            if (event.getApplicationId() == null || event.getApplicationId().isBlank()) {
                log.warn("application.declined received without applicationId: {}", payloadJson);
                return;
            }

            DeclineRecord record = new DeclineRecord();
            record.setApplicationId(event.getApplicationId());
            record.setDeclineReason(event.getDeclineReason());
            record.setDeclinedAt(event.getDeclinedAt() == null ? Instant.now() : event.getDeclinedAt());

            // upsert: if existing, update; else save
            declineRepository.findByApplicationId(event.getApplicationId())
                    .ifPresentOrElse(existing -> {
                        existing.setDeclineReason(record.getDeclineReason());
                        existing.setDeclinedAt(record.getDeclinedAt());
                        declineRepository.save(existing);
                        log.info("Updated decline record for applicationId={}", event.getApplicationId());
                    }, () -> {
                        declineRepository.save(record);
                        log.info("Saved decline record for applicationId={}", event.getApplicationId());
                    });

        } catch (Exception ex) {
            log.error("Failed to process application.declined payload: {}", payloadJson, ex);
        }
    }
}
