package com.northbridge.application.service;

import com.northbridge.application.model.Application;
import com.northbridge.application.model.ApplicationStatus;
import com.northbridge.application.model.ApplicationEvent;
import com.northbridge.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service to manage Application persistence and status updates.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional
    public Application createSubmittedApplication(ApplicationEvent event) {
        String applicationId = event.getApplicationId();
        String customerId = event.getCustomerId();

        Application app = new Application();
        app.setApplicationId(applicationId);
        app.setCustomerId(customerId);
        app.setApplicantName(event.getApplicantName());
        app.setEmail(event.getEmail());
        app.setStatus(ApplicationStatus.SUBMITTED.name());
        app.setCreatedAt(Instant.now());

        return applicationRepository.save(app);
    }

    public java.util.Optional<Application> getLatestByCustomerId(String customerId) {
        return applicationRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
