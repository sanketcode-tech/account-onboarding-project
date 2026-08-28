package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Event published when the customer uploads and the document is signed.
 * Kafka Topic: document.signed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignedEvent {
private String applicationId;
private String documentId;
private String storageLocation; // File path or cloud URI
private Instant signedAt;
}


