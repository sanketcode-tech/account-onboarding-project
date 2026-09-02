package com.northbridge.application.service;

import com.northbridge.application.model.ApplicationEvent;
import com.northbridge.common.events.ApplicationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka Producer Service
 * Sends application events to the 'application.submitted' Kafka topic using the common ApplicationSubmittedEvent shape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, com.northbridge.common.events.ApplicationSubmittedEvent> kafkaTemplate;

    private static final String TOPIC_NAME = "application.submitted";

    /**
     * Send an application event to Kafka
     */
    public void sendApplicationEvent(ApplicationEvent event) {
        log.info("Sending application event to Kafka topic '{}': {}", TOPIC_NAME, event);

        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        // Map internal ApplicationEvent to common ApplicationSubmittedEvent
        ApplicationSubmittedEvent common = new ApplicationSubmittedEvent();
        common.setApplicationId(event.getApplicationId());
        // forward customerId if present, otherwise generate a test customerId so downstream consumers get a non-null value
        common.setCustomerId(event.getCustomerId() != null ? event.getCustomerId() : (event.getApplicationId() != null ? "CUST-" + event.getApplicationId() : null));
        common.setProductType("CURRENT_ACCOUNT");
        common.setCreatedAt(Instant.now());
        // include applicant details so consumers can validate immediately
        common.setApplicantName(event.getApplicantName());
        common.setEmail(event.getEmail());
        // If payload is provided, serialize it into payloadRef so downstream consumers can access it without Mongo.
        if (event.getPayload() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String payloadJson = mapper.writeValueAsString(event.getPayload());
                common.setPayloadRef(payloadJson);
            } catch (Exception ex) {
                log.warn("Failed to serialize payload for applicationId={}: {}", event.getApplicationId(), ex.getMessage());
                common.setPayloadRef(null);
            }
        } else {
            common.setPayloadRef(null);
        }

        Message<ApplicationSubmittedEvent> message = MessageBuilder
                .withPayload(common)
                .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
                .setHeader("kafka_messageKey", event.getApplicationId())
                .build();

        CompletableFuture<SendResult<String, com.northbridge.common.events.ApplicationSubmittedEvent>> future = this.kafkaTemplate.send(message);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send application event with ID: {}", event.getApplicationId(), ex);
            } else if (result != null && result.getRecordMetadata() != null) {
                log.info("Application event sent successfully with ID: {}. Partition: {}, Offset: {}",
                        event.getApplicationId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.info("Application event send completed for ID: {} (no metadata)", event.getApplicationId());
            }
        });
    }

    /**
     * Send a simple test message
     */
    public void sendTestMessage(String applicationId, String applicantName) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(applicationId);
        event.setApplicantName(applicantName);
        event.setEmail(applicantName.toLowerCase().replace(" ", ".") + "@example.com");
        // set a test customerId so local tests have a non-null customerId
        event.setCustomerId("CUST-" + applicationId);
        event.setStatus("SUBMITTED");
        event.setEventType("APPLICATION_SUBMITTED");
        event.setTimestamp(LocalDateTime.now());

        sendApplicationEvent(event);
    }

}