package com.northbridge.onboarding.controller;

import io.camunda.client.CamundaClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Camunda workflow orchestration.
 * The application-service calls /api/workflows/start to start BPMN process instances.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
public class ProcessController {

    @Value("${process.start.api.enabled:false}")
    private boolean processStartApiEnabled;

    private final CamundaClient camundaClient;

    public ProcessController(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startWorkflow(@RequestBody WorkflowStartRequest request) {
            if (!processStartApiEnabled) {
                log.warn("Manual workflow start endpoint disabled by configuration");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Manual workflow start API is disabled"));
            }

            log.info("Received workflow start request for applicationId={} customerId={} applicantName={}",
                    request.getApplicationId(), request.getCustomerId(), request.getApplicantName());

        if (request == null || request.getApplicationId() == null || request.getApplicationId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "applicationId is required"));
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", request.getApplicationId());
        variables.put("customerId", request.getCustomerId());
        variables.put("applicantName", request.getApplicantName());
        variables.put("email", request.getEmail());
        variables.put("status", request.getStatus());
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }

        try {
            var response = camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId("current-account-onboarding")
                    .latestVersion()
                    .variables(variables)
                    .send()
                    .join();

            Map<String, Object> body = new HashMap<>();
            body.put("message", "Workflow started successfully");
            body.put("processInstanceKey", String.valueOf(response.getProcessInstanceKey()));
            body.put("applicationId", request.getApplicationId());
            return ResponseEntity.accepted().body(body);
        } catch (Exception ex) {
            log.error("Failed to start workflow for applicationId={}", request.getApplicationId(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Failed to start workflow: " + ex.getMessage(),
                    "applicationId", request.getApplicationId()
            ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Onboarding Service is running");
    }

    // ============================================================
    // DTO Classes for Request/Response
    // ============================================================

    @Data
    public static class WorkflowStartRequest {
        private String applicationId;
        private String customerId;
        private String applicantName;
        private String email;
        private String status = "SUBMITTED";
        private Map<String, Object> variables;
    }
}

