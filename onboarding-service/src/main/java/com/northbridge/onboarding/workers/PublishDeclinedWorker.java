package com.northbridge.onboarding.workers;

import com.northbridge.common.events.ApplicationDeclinedEvent;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        String declineReason;
        if (declineObj instanceof String && !((String) declineObj).isBlank() && !"null".equalsIgnoreCase((String) declineObj)) {
            declineReason = (String) declineObj;
        } else {
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
