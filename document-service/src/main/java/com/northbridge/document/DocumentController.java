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
    private final com.northbridge.document.auth.AuthClient authClient;
    private final com.northbridge.document.client.OfferClient offerClient;

    @GetMapping("/{applicationId}")
    public ResponseEntity<Document> getDocument(@PathVariable String applicationId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        // validate token and ownership
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            String owner = offerClient.getCustomerIdForApplication(applicationId, authorization);
            if (owner == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner not found");
            if (!subject.equals(owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this applicationId");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        return documentService.findByApplicationId(applicationId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @PostMapping("/{applicationId}/upload")
    public ResponseEntity<Document> uploadDocument(@PathVariable String applicationId, @RequestParam("file") MultipartFile file, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            String owner = offerClient.getCustomerIdForApplication(applicationId, authorization);
            if (owner == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner not found");
            if (!subject.equals(owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this applicationId");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Document saved = documentService.uploadDocument(applicationId, file);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{applicationId}/sign")
    public ResponseEntity<Document> signDocument(@PathVariable String applicationId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            String owner = offerClient.getCustomerIdForApplication(applicationId, authorization);
            if (owner == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner not found");
            if (!subject.equals(owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this applicationId");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Document saved = documentService.signDocument(applicationId);
        return ResponseEntity.ok(saved);
    }
}
