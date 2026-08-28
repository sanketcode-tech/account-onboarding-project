package com.northbridge.document;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "documents", uniqueConstraints = {@UniqueConstraint(name = "uk_document_application_id", columnNames = "application_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "storage_location")
    private String storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "signed_at")
    private Instant signedAt;
}
