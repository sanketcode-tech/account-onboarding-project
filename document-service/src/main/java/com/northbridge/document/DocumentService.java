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
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.UUID;

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
    public Document uploadDocument(String applicationId, MultipartFile file) {
        Document doc = documentRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found for applicationId=" + applicationId));

        if (doc.getStatus() != DocumentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is not pending");
        }

        try {
            Path uploadDir = Paths.get("uploads");
            Files.createDirectories(uploadDir);
            String filename = applicationId + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path dest = uploadDir.resolve(filename);
            // Use stream copy to avoid transferTo platform/temp-file issues
            try (var in = file.getInputStream()) {
                Files.copy(in, dest);
            }
            doc.setStorageLocation(dest.toAbsolutePath().toString());
            doc.setStatus(DocumentStatus.UPLOADED);
            Document saved = documentRepository.save(doc);
            // Log upload event to console with status
            log.info("Document uploaded for applicationId={} documentId={} status={} storageLocation={}",
                    saved.getApplicationId(), saved.getDocumentId(), saved.getStatus(), saved.getStorageLocation());
            return saved;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file", e);
        }
    }

    @Transactional
    public Document signDocument(String applicationId) {
        Document doc = documentRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found for applicationId=" + applicationId));

        if (doc.getStatus() != DocumentStatus.UPLOADED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be uploaded before signing");
        }

        doc.setStatus(DocumentStatus.SIGNED);
        Instant now = Instant.now();
        doc.setSignedAt(now);
        if (doc.getStorageLocation() == null || doc.getStorageLocation().isBlank()) {
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
