package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Event published when the account has been fully provisioned and activated.
 * Kafka Topic: account.activated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivatedEvent {
private String applicationId;
private String accountId; // Generated account number
private Instant activatedAt;
}


