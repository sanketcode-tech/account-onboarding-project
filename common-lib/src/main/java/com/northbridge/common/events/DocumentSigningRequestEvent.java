package com.northbridge.common.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSigningRequestEvent {
private String applicationId;
private String documentId;
private Instant requestedAt;
}
