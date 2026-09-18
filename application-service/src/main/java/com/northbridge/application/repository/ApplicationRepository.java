package com.northbridge.application.repository;

import com.northbridge.application.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Application entity.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    Optional<Application> findByApplicationId(String applicationId);
    Optional<Application> findTopByCustomerIdOrderByCreatedAtDesc(String customerId);
}
