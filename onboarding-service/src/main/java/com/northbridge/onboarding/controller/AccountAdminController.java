package com.northbridge.onboarding.controller;

import com.northbridge.onboarding.account.Account;
import com.northbridge.onboarding.account.AccountRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Admin helper for creating accounts in development only.
 * Disabled by default via property onboarding.admin.enabled=false.
 */
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AccountAdminController {

    private final AccountRepository accountRepository;

    @Value("${onboarding.admin.enabled:false}")
    private boolean enabled;

    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(@RequestBody CreateRequest req) {
        if (!enabled) return ResponseEntity.notFound().build();
        if (req.getApplicationId() == null || req.getApplicationId().isBlank()) return ResponseEntity.badRequest().build();

        Account a = accountRepository.findByApplicationId(req.getApplicationId())
                .orElseGet(() -> new Account());
        a.setApplicationId(req.getApplicationId());
        a.setAccountId(req.getAccountId() == null ? ("ACCT-" + java.util.UUID.randomUUID()) : req.getAccountId());
        a.setOfferedLimit(req.getOfferedLimit());
        a.setStatus(req.getStatus() == null ? "ACTIVE" : req.getStatus());
        a.setActivatedAt(req.getActivatedAt() == null ? Instant.now() : req.getActivatedAt());

        Account saved = accountRepository.save(a);
        return ResponseEntity.ok(saved);
    }

    @Data
    public static class CreateRequest {
        private String applicationId;
        private String accountId;
        private BigDecimal offeredLimit;
        private String status;
        private Instant activatedAt;
    }
}
