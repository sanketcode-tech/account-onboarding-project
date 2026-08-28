package com.northbridge.application.service;

import com.northbridge.application.model.ApplicationEvent;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka Consumer Listener
 * Listens to the 'application.submitted' Kafka topic and processes events
 */
@Slf4j
@Service
public class KafkaConsumerListener {
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${workflow.start.enabled:false}")
    private boolean workflowStartEnabled;

    public KafkaConsumerListener(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    /**
     * Listener for application.submitted topic
     */
    @KafkaListener(
            topics = "application.submitted",
            groupId = "application-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeApplicationEvent(String payloadJson) {
        if (payloadJson == null) {
            log.warn("Ignoring null application.submitted message");
            return;
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize application.submitted payload: {}", payloadJson, ex);
            return;
        }

        String applicationId = null;
        if (payload.containsKey("applicationId")) {
            applicationId = String.valueOf(payload.get("applicationId"));
        } else if (payload.containsKey("payload") && payload.get("payload") instanceof Map) {
            Map<?, ?> inner = (Map<?, ?>) payload.get("payload");
            if (inner.containsKey("applicationId")) {
                applicationId = String.valueOf(inner.get("applicationId"));
            }
        }

        if (applicationId == null || applicationId.isBlank()) {
            log.warn("Ignoring application.submitted event with missing applicationId. Payload: {}", payload);
            return;
        }

        // Map to internal ApplicationEvent for workflow processing
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(applicationId);
        event.setApplicantName(String.valueOf(payload.getOrDefault("applicantName", payload.getOrDefault("applicantName", null))));
        event.setEmail(String.valueOf(payload.getOrDefault("email", null)));
        event.setStatus(String.valueOf(payload.getOrDefault("status", "SUBMITTED")));
        event.setPayload(payload.getOrDefault("payload", null));

        log.info("Received application.submitted for applicationId={}", applicationId);

        if (workflowStartEnabled) {
            try {
                workflowService.startWorkflow(event);
            } catch (Exception ex) {
                log.error("Failed to start workflow for applicationId={}", applicationId, ex);
            }
        } else {
            log.info("Workflow start disabled (workflow.start.enabled=false). Skipping start for applicationId={}", applicationId);
        }
    }

}