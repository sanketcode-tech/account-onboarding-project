package com.northbridge.document;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping("/{applicationId}/upload")
    public ResponseEntity<Document> uploadDocument(@PathVariable String applicationId, @RequestParam("file") MultipartFile file) {
        Document saved = documentService.uploadDocument(applicationId, file);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{applicationId}/sign")
    public ResponseEntity<Document> signDocument(@PathVariable String applicationId) {
        Document saved = documentService.signDocument(applicationId);
        return ResponseEntity.ok(saved);
    }
}
