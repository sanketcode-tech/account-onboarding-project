package com.northbridge.onboarding.account;

import com.northbridge.common.events.AccountActivatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Consumes AccountActivatedEvent messages and persists Account entities.
 */
@Slf4j
@Component
public class AccountActivatedListener {

    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccountActivatedListener(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @KafkaListener(topics = "account.activated", groupId = "onboarding-service", containerFactory = "kafkaListenerContainerFactory")
    public void onAccountActivated(String payloadJson) {
        try {
            AccountActivatedEvent event = objectMapper.readValue(payloadJson, AccountActivatedEvent.class);
            if (event.getApplicationId() == null || event.getApplicationId().isBlank()) {
                log.warn("Received account.activated with missing applicationId: {}", payloadJson);
                return;
            }

            // upsert account
            accountRepository.findByApplicationId(event.getApplicationId())
                    .ifPresentOrElse(existing -> {
                        existing.setAccountId(event.getAccountId());
                        existing.setActivatedAt(event.getActivatedAt() == null ? Instant.now() : event.getActivatedAt());
                        existing.setStatus("ACTIVE");
                        accountRepository.save(existing);
                        log.info("Updated account for applicationId={}", event.getApplicationId());
                    }, () -> {
                        com.northbridge.onboarding.account.Account a = new com.northbridge.onboarding.account.Account();
                        a.setApplicationId(event.getApplicationId());
                        a.setAccountId(event.getAccountId());
                        a.setStatus("ACTIVE");
                        a.setActivatedAt(event.getActivatedAt() == null ? Instant.now() : event.getActivatedAt());
                        accountRepository.save(a);
                        log.info("Saved new account for applicationId={}", event.getApplicationId());
                    });

        } catch (Exception ex) {
            log.error("Failed to process account.activated payload: {}", payloadJson, ex);
        }
    }
}
