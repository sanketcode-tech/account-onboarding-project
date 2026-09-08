package com.northbridge.onboarding.workers;

import com.northbridge.common.events.ApplicationDeclinedEvent;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * Camunda worker that publishes ApplicationDeclinedEvent when an application is declined.
 */
@Slf4j
@Component
public class PublishDeclinedWorker {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PublishDeclinedWorker(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @JobWorker(type = "publish-declined")
    public void publishDeclined(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));

                // Determine declineReason: prefer explicit variable, otherwise derive from signing/provisioning decisions
        Object declineObj = vars.get("declineReason");
        String declineReason = null;

                // Prefer explicit declineReason variable if present and non-null
        if (declineObj instanceof String && !((String) declineObj).isBlank() && !"null".equalsIgnoreCase((String) declineObj)) {
            declineReason = (String) declineObj;
        }

        // If not present, try to extract from DMN result object eligibilityResult (eligibilityResult.declineReason)
        if (declineReason == null) {
            Object eligibilityObj = vars.get("eligibilityResult");
            if (eligibilityObj instanceof Map) {
                Object nested = ((Map<?, ?>) eligibilityObj).get("declineReason");
                if (nested instanceof String && !((String) nested).isBlank() && !"null".equalsIgnoreCase((String) nested)) {
                    declineReason = (String) nested;
                }
            } else if (eligibilityObj instanceof String) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<?, ?> parsed = mapper.readValue(String.valueOf(eligibilityObj), Map.class);
                    Object nested = parsed.get("declineReason");
                    if (nested instanceof String && !((String) nested).isBlank() && !"null".equalsIgnoreCase((String) nested)) {
                        declineReason = (String) nested;
                    }
                } catch (Exception ex) {
                    log.debug("Failed to parse eligibilityResult JSON for applicationId={}: {}", applicationId, ex.getMessage());
                }
            }
        }

        if (declineReason == null) {
            Object signingDecisionObj = vars.get("signingDecision");
            Object signingNotesObj = vars.get("signingNotes");
            Object provisioningDecisionObj = vars.get("provisioningDecision");
            Object provisioningNotesObj = vars.get("provisioningNotes");

            if (signingDecisionObj != null && "REJECTED".equalsIgnoreCase(String.valueOf(signingDecisionObj))) {
                String notes = signingNotesObj != null ? String.valueOf(signingNotesObj) : "";
                declineReason = "Signing rejected" + (notes.isBlank() ? "" : ": " + notes);
            } else if (provisioningDecisionObj != null && "REJECTED".equalsIgnoreCase(String.valueOf(provisioningDecisionObj))) {
                String notes = provisioningNotesObj != null ? String.valueOf(provisioningNotesObj) : "";
                declineReason = "Provisioning rejected" + (notes.isBlank() ? "" : ": " + notes);
            } else {
                declineReason = "UNKNOWN_REASON";
            }
        }

        ApplicationDeclinedEvent event = new ApplicationDeclinedEvent(applicationId, declineReason, java.time.Instant.now());
        kafkaTemplate.send("application.declined", applicationId, event);

        log.info("[PublishDeclinedWorker] published ApplicationDeclinedEvent for applicationId={} reason={}", applicationId, declineReason);

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("applicationDeclined", true, "declineReason", declineReason))
                .send()
                .join();
    }
}
