package com.northbridge.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity representing a persisted Application row.
 */
@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "email")
    private String email;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
