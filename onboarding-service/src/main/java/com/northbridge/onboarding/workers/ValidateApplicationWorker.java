package com.northbridge.onboarding.workers;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.exception.BpmnError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Camunda worker that validates required application fields during onboarding.
 */
@Slf4j
@Component
public class ValidateApplicationWorker {

    @JobWorker(type = "validate-application")
    public void validateApplication(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));
        String applicantName = String.valueOf(vars.getOrDefault("applicantName", ""));
        String email = String.valueOf(vars.getOrDefault("email", ""));

        log.info("[ValidateApplicationWorker] processing applicationId={} applicantName={} email={}", applicationId, applicantName, email);

        if (applicationId.isBlank() || applicantName.isBlank() || email.isBlank()) {
            throw new BpmnError("VALIDATION_FAILED", "Application validation rejected: missing required fields");
        }

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("validationPassed", true, "validationResult", "APPROVED"))
                .send()
                .join();

        log.info("[ValidateApplicationWorker] completed applicationId={}", applicationId);
    }
}