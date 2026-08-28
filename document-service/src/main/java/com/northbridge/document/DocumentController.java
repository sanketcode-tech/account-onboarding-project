package com.northbridge.document;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{applicationId}")
    public ResponseEntity<Document> getDocument(@PathVariable String applicationId) {
        return documentService.findByApplicationId(applicationId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @PostMapping("/{applicationId}/sign")
    public ResponseEntity<Document> signDocument(@PathVariable String applicationId, @RequestBody(required = false) SignRequest body) {
        String storageLocation = body == null ? null : body.getStorageLocation();
        Document saved = documentService.signDocument(applicationId, storageLocation);
        return ResponseEntity.ok(saved);
    }

    public static class SignRequest {
        private String storageLocation;

        public String getStorageLocation() { return storageLocation; }
        public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    }
}
