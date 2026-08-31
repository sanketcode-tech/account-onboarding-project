package com.northbridge.onboarding.account;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts", uniqueConstraints = {@UniqueConstraint(name = "uk_account_application_id", columnNames = "application_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "offered_limit")
    private BigDecimal offeredLimit;

    @Column(name = "status")
    private String status;

    @Column(name = "activated_at")
    private Instant activatedAt;
}
