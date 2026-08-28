package com.northbridge.document;

import com.northbridge.common.events.DocumentSignedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Optional<Document> findByApplicationId(String applicationId) {
        return documentRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Document signDocument(String applicationId, String storageLocation) {
        Document doc = documentRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found for applicationId=" + applicationId));

        if (doc.getStatus() != DocumentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is not pending");
        }

        doc.setStatus(DocumentStatus.SIGNED);
        Instant now = Instant.now();
        doc.setSignedAt(now);
        if (storageLocation != null && !storageLocation.isBlank()) {
            doc.setStorageLocation(storageLocation);
        } else if (doc.getStorageLocation() == null || doc.getStorageLocation().isBlank()) {
            doc.setStorageLocation("/signed/" + doc.getDocumentId());
        }

        Document saved = documentRepository.save(doc);

        // Publish DocumentSignedEvent
        DocumentSignedEvent event = new DocumentSignedEvent(saved.getApplicationId(), saved.getDocumentId(), saved.getStorageLocation(), saved.getSignedAt());
        kafkaTemplate.send("document.signed", saved.getApplicationId(), event);
        log.info("Published document.signed for applicationId={} documentId={}", saved.getApplicationId(), saved.getDocumentId());

        return saved;
    }
}
