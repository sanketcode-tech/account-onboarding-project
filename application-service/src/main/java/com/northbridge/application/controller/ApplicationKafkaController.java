package com.northbridge.application.controller;

import com.northbridge.application.model.ApplicationEvent;
import com.northbridge.application.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Application Kafka Test Controller
 * Provides endpoints to test Kafka producer and consumer functionality
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationKafkaController {

    private final KafkaProducerService kafkaProducerService;
    private final com.northbridge.application.auth.AuthClient authClient;
    private final com.northbridge.application.service.ApplicationService applicationService;

    /**
     * Test endpoint - Send a simple application event to Kafka
     *
     * @param applicationId The application ID
     * @param applicantName The applicant name
     * @return ResponseEntity with success message
     */
    @PostMapping("/test-send")
    public ResponseEntity<String> sendTestMessage(
            @RequestParam(value = "applicationId", defaultValue = "app-123") String applicationId,
            @RequestParam(value = "applicantName", defaultValue = "John Doe") String applicantName) {

        try {
            kafkaProducerService.sendTestMessage(applicationId, applicantName);
            log.info("Test message sent successfully: ID={}, Name={}", applicationId, applicantName);
            return ResponseEntity.ok()
                    .body("Message sent successfully to topic 'application.submitted'. "
                            + "Application ID: " + applicationId);
        } catch (Exception e) {
            log.error("Error sending test message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Send a custom application event to Kafka
     *
     * @param event The ApplicationEvent payload
     * @return ResponseEntity with success message
     */
    @PostMapping("/submit")
    public ResponseEntity<String> submitApplication(@RequestBody ApplicationEvent event, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            if (event.getApplicationId() == null || event.getApplicationId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Application ID is required");
            }

            // Derive customerId from JWT subject via auth-service
            try {
                String subject = authClient.validateAndGetSubject(authorization);
                event.setCustomerId(subject);
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid JWT token");
            }

            // Persist Application row immediately with status=SUBMITTED using JWT-derived customerId
            applicationService.createSubmittedApplication(event);

            kafkaProducerService.sendApplicationEvent(event);
            log.info("Application submitted successfully: {} for customerId={}", event.getApplicationId(), event.getCustomerId());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Application submitted successfully. Application ID: " + event.getApplicationId());
        } catch (Exception e) {
            log.error("Error submitting application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error submitting application: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application Service is running");
    }

    /**
     * Return the most recent application for the authenticated customer.
     * The customerId is derived from the validated JWT via AuthClient; do not trust client parameters.
     */
    @GetMapping("/my-application")
    public ResponseEntity<?> getMyApplication(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            var opt = applicationService.getLatestByCustomerId(subject);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No application found for this customer");
            }
            var app = opt.get();
            return ResponseEntity.ok(java.util.Map.of("applicationId", app.getApplicationId(), "status", app.getStatus()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid JWT token");
        } catch (Exception ex) {
            log.error("Error fetching my-application", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving application");
        }
    }

}

