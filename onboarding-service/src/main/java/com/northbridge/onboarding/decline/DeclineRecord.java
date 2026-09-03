package com.northbridge.onboarding.decline;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "application_declines", uniqueConstraints = {@UniqueConstraint(name = "uk_decline_application_id", columnNames = "application_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeclineRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "declined_at")
    private Instant declinedAt;
}
