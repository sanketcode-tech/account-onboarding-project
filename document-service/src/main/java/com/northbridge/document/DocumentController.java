package com.northbridge.document;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * REST controller exposing document retrieval, file download/upload and signing endpoints for the customer-facing API.
 */
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

    @GetMapping("/{applicationId}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable String applicationId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            String owner;
            try {
                owner = offerClient.getCustomerIdForApplication(applicationId, authorization);
            } catch (org.springframework.web.client.HttpClientErrorException.Forbidden forbiddenEx) {
                // upstream service forbidden — allow if caller has OFFICER role
                if (authClient.tokenHasRole(authorization, "OFFICER")) {
                    owner = subject; // treat as allowed
                } else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner not found");
                }
            }
            if (owner == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner not found");
            if (!subject.equals(owner) && !authClient.tokenHasRole(authorization, "OFFICER")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this applicationId");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Document doc = documentService.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (doc.getStorageLocation() == null || doc.getStorageLocation().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file uploaded for this document");
        }

        try {
            Path file = Paths.get(doc.getStorageLocation());
            if (!Files.exists(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file not found");
            }
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
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
    public ResponseEntity<Document> signDocument(@PathVariable String applicationId, @RequestBody(required = false) SignRequest signRequest, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            String owner = offerClient.getCustomerIdForApplication(applicationId, authorization);
            if (owner == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner not found");
            if (!subject.equals(owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this applicationId");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        String signerName = signRequest != null ? signRequest.getSignerName() : null;
        if (signerName == null || signerName.isBlank()) {
            try {
                signerName = authClient.getUserFullName(authorization);
            } catch (Exception ex) {
                // fallback to subject (email) if profile lookup fails
                signerName = authClient.validateAndGetSubject(authorization);
            }
        }

        Document saved = documentService.signDocument(applicationId, signerName);
        return ResponseEntity.ok(saved);
    }
}
