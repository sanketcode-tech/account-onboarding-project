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
            @RequestParam(value = "applicantName", defaultValue = "Sanket Dhake") String applicantName) {

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
    public ResponseEntity<String> submitApplication(@RequestBody ApplicationEvent event) {
        try {
            if (event.getApplicationId() == null || event.getApplicationId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Application ID is required");
            }

            kafkaProducerService.sendApplicationEvent(event);
            log.info("Application submitted successfully: {}", event.getApplicationId());
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

}

