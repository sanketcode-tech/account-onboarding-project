package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published to request customer signature for a document (topic: document.requested).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSigningRequestEvent {
private String applicationId;
private String documentId;
private Instant requestedAt;
}
