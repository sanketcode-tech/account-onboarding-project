package com.northbridge.onboarding.decline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeclineRepository extends JpaRepository<DeclineRecord, Long> {
    Optional<DeclineRecord> findByApplicationId(String applicationId);
}
