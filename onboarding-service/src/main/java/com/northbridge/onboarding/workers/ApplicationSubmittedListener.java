package com.northbridge.onboarding.workers;

import tools.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.Period;

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

            // Try to extract richer payload which may be provided either inline as 'payload' or as serialized JSON in 'payloadRef'
            Map<String, Object> innerPayload = null;
            Object payloadObj = payload.get("payload");
            if (payloadObj instanceof Map) {
                innerPayload = (Map<String, Object>) payloadObj;
            } else if (payload.get("payloadRef") instanceof String) {
                String payloadRefJson = String.valueOf(payload.get("payloadRef"));
                try {
                    innerPayload = objectMapper.readValue(payloadRefJson, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse payloadRef JSON for applicationId={}: {}", applicationId, e.getMessage());
                }
            }

            if (innerPayload != null) {
                try {
                    // personalDetails.dob -> compute age (years)
                    if (innerPayload.get("personalDetails") instanceof Map) {
                        Map<?, ?> pd = (Map<?, ?>) innerPayload.get("personalDetails");
                        Object dobObj = pd.get("dob");
                        if (dobObj != null) {
                            String dobStr = String.valueOf(dobObj);
                            try {
                                LocalDate dobDate = LocalDate.parse(dobStr);
                                int age = Period.between(dobDate, LocalDate.now()).getYears();
                                variables.put("age", age);
                            } catch (Exception ignore) {
                                log.debug("Could not parse dob='{}' into LocalDate for applicationId={}", dobStr, applicationId);
                            }
                        }
                    }

                    // employmentDetails -> annualIncome, creditRating, yearsOfExperience
                    if (innerPayload.get("employmentDetails") instanceof Map) {
                        Map<?, ?> ed = (Map<?, ?>) innerPayload.get("employmentDetails");
                        if (ed.containsKey("annualIncome")) {
                            variables.put("annualIncome", ed.get("annualIncome"));
                        }
                        if (ed.containsKey("creditRating")) {
                            variables.put("creditRating", ed.get("creditRating"));
                        }
                        if (ed.containsKey("yearsOfExperience")) {
                            variables.put("yearsOfExperience", ed.get("yearsOfExperience"));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract nested payload fields for applicationId={}: {}", applicationId, e.getMessage());
                }
            }

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
