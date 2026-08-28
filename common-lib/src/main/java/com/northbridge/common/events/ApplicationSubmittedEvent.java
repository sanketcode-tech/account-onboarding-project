package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Event published when a customer submits a current account application.
 * Kafka Topic: application.submitted
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSubmittedEvent {
private String applicationId;
private String customerId;
private String productType; // e.g., "CURRENT_ACCOUNT"
private Instant createdAt;
// Optional: include applicant details to let consumers validate without extra DB lookups
private String applicantName;
private String email;
private String payloadRef; // Optional: reference to Mongo document or full application JSON
}


