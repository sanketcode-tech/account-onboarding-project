package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when an application is declined (topic: application.declined).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDeclinedEvent {
private String applicationId;
private String declineReason;
private Instant declinedAt;
}

