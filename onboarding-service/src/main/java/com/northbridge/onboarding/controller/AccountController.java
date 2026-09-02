package com.northbridge.onboarding.controller;

import com.northbridge.onboarding.account.Account;
import com.northbridge.onboarding.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;

    @GetMapping("/{applicationId}")
    public ResponseEntity<Account> getByApplicationId(@PathVariable String applicationId) {
        return accountRepository.findByApplicationId(applicationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
