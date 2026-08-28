package com.northbridge.onboarding.workers;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.exception.BpmnError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ActivateAccountWorker {

    @JobWorker(type = "activate-account")
    public void activateAccount(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));
        String accountId = String.valueOf(vars.getOrDefault("accountId", "ACCT-" + java.util.UUID.randomUUID()));

        log.info("[ActivateAccountWorker] activating account for applicationId={} accountId={}", applicationId, accountId);

        if (applicationId.isBlank()) {
            throw new BpmnError("ACTIVATION_FAILED", "Activation rejected: missing applicationId");
        }

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("accountActivated", true, "accountId", accountId))
                .send()
                .join();

        log.info("[ActivateAccountWorker] completed accountId={} for applicationId={}", accountId, applicationId);
    }
}