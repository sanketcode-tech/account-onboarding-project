package com.northbridge.onboarding.controller;

import com.northbridge.onboarding.account.Account;
import com.northbridge.onboarding.account.AccountRepository;
import com.northbridge.onboarding.decline.DeclineRecord;
import com.northbridge.onboarding.decline.DeclineRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications")
public class ApplicationStatusController {

    private final AccountRepository accountRepository;
    private final DeclineRepository declineRepository;

    public ApplicationStatusController(AccountRepository accountRepository, DeclineRepository declineRepository) {
        this.accountRepository = accountRepository;
        this.declineRepository = declineRepository;
    }

    @GetMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationStatusResponse> getStatus(@PathVariable String applicationId) {
        Optional<DeclineRecord> declineOpt = declineRepository.findByApplicationId(applicationId);
        if (declineOpt.isPresent()) {
            DeclineRecord d = declineOpt.get();
            ApplicationStatusResponse resp = new ApplicationStatusResponse(
                    applicationId,
                    "DECLINED",
                    d.getDeclineReason(),
                    null,
                    null,
                    d.getDeclinedAt()
            );
            return ResponseEntity.ok(resp);
        }

        Optional<Account> accountOpt = accountRepository.findByApplicationId(applicationId);
        if (accountOpt.isPresent()) {
            Account a = accountOpt.get();
            ApplicationStatusResponse resp = new ApplicationStatusResponse(
                    applicationId,
                    "ACTIVATED",
                    null,
                    a.getAccountId(),
                    a.getOfferedLimit(),
                    a.getActivatedAt()
            );
            return ResponseEntity.ok(resp);
        }

        ApplicationStatusResponse resp = new ApplicationStatusResponse(applicationId, "IN_PROGRESS", null, null, null, null);
        return ResponseEntity.ok(resp);
    }

    public static record ApplicationStatusResponse(
            String applicationId,
            String overallStatus,
            String declineReason,
            String accountId,
            BigDecimal offeredLimit,
            Instant activatedAt
    ) {}
}
