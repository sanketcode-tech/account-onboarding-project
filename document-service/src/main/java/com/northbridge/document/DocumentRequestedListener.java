package com.northbridge.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

/**
 * Listens for document.requested events and persists initial Document records for signing/upload.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRequestedListener {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "document.requested", groupId = "document-service", containerFactory = "kafkaListenerContainerFactory")
    public void onDocumentRequested(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Ignoring null or blank document.requested message");
            return;
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize document.requested payload: {}", payloadJson, ex);
            return;
        }

        String applicationId = payload.get("applicationId") == null ? null : String.valueOf(payload.get("applicationId"));
        String documentId = payload.get("documentId") == null ? null : String.valueOf(payload.get("documentId"));
        if (applicationId == null || applicationId.isBlank() || documentId == null || documentId.isBlank()) {
            log.warn("Ignoring document.requested event with missing applicationId or documentId. Payload: {}", payload);
            return;
        }

        if (documentRepository.findByApplicationId(applicationId).isPresent()) {
            log.info("Document already exists for applicationId={}, skipping duplicate document.requested event", applicationId);
            return;
        }

        Document doc = new Document();
        doc.setApplicationId(applicationId);
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setRequestedAt(parseInstant(payload.get("requestedAt"), Instant.now()));

        Document saved = documentRepository.save(doc);
        log.info("Persisted document for applicationId={} documentId={} status={}", saved.getApplicationId(), saved.getDocumentId(), saved.getStatus());
    }

    private Instant parseInstant(Object value, Instant fallback) {
        if (value == null) return fallback;
        try {
            return Instant.parse(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
