package com.northbridge.onboarding.workers;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.exception.BpmnError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.northbridge.onboarding.account.Account;
import com.northbridge.onboarding.account.AccountRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivateAccountWorker {

    private final AccountRepository accountRepository;

    @JobWorker(type = "activate-account")
    public void activateAccount(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));
        String accountId = String.valueOf(vars.getOrDefault("accountId", "ACCT-" + java.util.UUID.randomUUID()));

        log.info("[ActivateAccountWorker] activating account for applicationId={} accountId={}", applicationId, accountId);

        if (applicationId.isBlank()) {
            throw new BpmnError("ACTIVATION_FAILED", "Activation rejected: missing applicationId");
        }

        // Persist account record
        String customerId = String.valueOf(vars.getOrDefault("customerId", ""));
        BigDecimal offeredLimit = null;
        Object offeredObj = vars.get("offeredLimit");
        try {
            if (offeredObj != null) {
                offeredLimit = new BigDecimal(String.valueOf(offeredObj));
            }
        } catch (Exception ex) {
            log.warn("Failed to parse offeredLimit from variables: {}", offeredObj);
        }

        Account account = new Account();
        account.setApplicationId(applicationId);
        account.setAccountId(accountId);
        account.setCustomerId(customerId);
        account.setOfferedLimit(offeredLimit);
        account.setStatus("ACTIVATED");
        account.setActivatedAt(Instant.now());

        try {
            accountRepository.save(account);
            log.info("Persisted account for applicationId={} accountId={}", applicationId, accountId);
        } catch (Exception ex) {
            log.error("Failed to persist account for applicationId={} accountId={}", applicationId, accountId, ex);
            throw new BpmnError("ACTIVATION_FAILED", "Failed to persist account");
        }

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("accountActivated", true, "accountId", accountId))
                .send()
                .join();

        log.info("[ActivateAccountWorker] completed accountId={} for applicationId={}", accountId, applicationId);
    }
}