package com.northbridge.onboarding.workers;

import tools.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for application.submitted events and starts a BPMN process instance in Camunda SaaS.
 * Accepts messages produced by different services (JSON payloads) and extracts applicationId.
 */
@Slf4j
@Component
public class ApplicationSubmittedListener {

    private final CamundaClient camundaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApplicationSubmittedListener(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @KafkaListener(topics = "application.submitted", groupId = "onboarding-service", containerFactory = "kafkaListenerContainerFactory")
    public void onApplicationSubmitted(String payloadJson) {
        if (payloadJson == null) {
            log.warn("Ignoring null application.submitted message");
            return;
        }

        Map<String, Object> payload;
        try {
            // payloadJson is raw JSON text; parse to Map
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

        log.info("Received application.submitted for applicationId={}", applicationId);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("applicationId", applicationId);
            variables.put("customerId", payload.getOrDefault("customerId", null));
            variables.put("productType", payload.getOrDefault("productType", null));
            variables.put("createdAt", payload.getOrDefault("createdAt", payload.getOrDefault("timestamp", null)));
            // pass applicant details through to BPMN workers
            variables.put("applicantName", payload.getOrDefault("applicantName", ""));
            variables.put("email", payload.getOrDefault("email", ""));

            var response = camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId("current-account-onboarding")
                    .latestVersion()
                    .variables(variables)
                    .send()
                    .join();

            log.info("Started BPMN process for applicationId={}, processInstanceKey={}", applicationId, response.getProcessInstanceKey());
        } catch (Exception ex) {
            log.error("Failed to start BPMN process for applicationId={}", applicationId, ex);
        }
    }
}
