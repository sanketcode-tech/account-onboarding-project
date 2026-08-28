package com.northbridge.onboarding.workers;

import com.northbridge.common.events.DocumentSignedEvent;
import com.northbridge.common.events.OfferAcceptedEvent;
import io.camunda.client.CamundaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Component
public class KafkaMessageCorrelator {

    private final CamundaClient camundaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaMessageCorrelator(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @KafkaListener(topics = "offer.accepted", groupId = "onboarding-service", containerFactory = "kafkaListenerContainerFactory")
    public void onOfferAccepted(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Ignoring null or blank offer.accepted message");
            return;
        }

        OfferAcceptedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, OfferAcceptedEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize offer.accepted payload: {}", payloadJson, ex);
            return;
        }

        if (event == null || event.getApplicationId() == null || event.getApplicationId().isBlank()) {
            log.warn("Ignoring offer.accepted event with missing applicationId");
            return;
        }

        log.info("Correlating OfferAccepted message for applicationId={}", event.getApplicationId());
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("applicationId", event.getApplicationId());
            if (event.getCustomerId() != null) vars.put("customerId", event.getCustomerId());
            vars.put("acceptedAt", event.getAcceptedAt().toString());

            camundaClient.newPublishMessageCommand()
                    .messageName("OfferAccepted")
                    .correlationKey(event.getApplicationId())
                    .variables(vars)
                    .send()
                    .join();
        } catch (Exception ex) {
            // Don't rethrow — log and continue so the Kafka listener doesn't retry infinitely
            log.error("Failed to publish Camunda message for applicationId={}. Will skip this record to avoid blocking.", event.getApplicationId(), ex);
        }
    }

    @KafkaListener(topics = "document.signed", groupId = "onboarding-service", containerFactory = "kafkaListenerContainerFactory")
    public void onDocumentSigned(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Ignoring null or blank document.signed message");
            return;
        }

        DocumentSignedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, DocumentSignedEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize document.signed payload: {}", payloadJson, ex);
            return;
        }

        if (event == null || event.getApplicationId() == null || event.getApplicationId().isBlank()) {
            log.warn("Ignoring document.signed event with missing applicationId");
            return;
        }

        log.info("Correlating DocumentSigned message for applicationId={}", event.getApplicationId());
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("applicationId", event.getApplicationId());
            if (event.getDocumentId() != null) vars.put("documentId", event.getDocumentId());
            if (event.getStorageLocation() != null) vars.put("storageLocation", event.getStorageLocation());
            if (event.getSignedAt() != null) vars.put("signedAt", event.getSignedAt().toString());

            camundaClient.newPublishMessageCommand()
                    .messageName("DocumentSigned")
                    .correlationKey(event.getApplicationId())
                    .variables(vars)
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Failed to publish Camunda message for applicationId={}. Will skip this record to avoid blocking.", event.getApplicationId(), ex);
        }
    }
}
