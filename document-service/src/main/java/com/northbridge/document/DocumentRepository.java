package com.northbridge.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for Document entities; allows lookup by applicationId.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByApplicationId(String applicationId);
}
