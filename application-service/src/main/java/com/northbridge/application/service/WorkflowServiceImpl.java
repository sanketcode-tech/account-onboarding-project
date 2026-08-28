package com.northbridge.application.service;

import com.northbridge.application.model.ApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Basic implementation of WorkflowService.
 *
 * This implementation calls the onboarding-service REST API to start workflow processes
 * via the camunda java client. The onboarding-service handles all camunda client interactions
 * and BPMN deployment.
 */
@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

    @Value("${CAMUNDA_CLUSTER_ID:}")
    private String camundaClusterId;

    @Value("${CAMUNDA_REGION:}")
    private String camundaRegion;

    @Value("${CAMUNDA_CLIENT_ID:}")
    private String camundaClientId;

    // Do not log secrets; keep the value for runtime use when integrating Zeebe client
    @Value("${CAMUNDA_CLIENT_SECRET:}")
    private String camundaClientSecret;

    @Value("${ONBOARDING_SERVICE_URL:http://localhost:8083}")
    private String onboardingServiceUrl;

    @Override
    public void startWorkflow(ApplicationEvent event) {
        log.info("[WorkflowService] Request to start workflow for applicationId={} applicantName={}",
                event.getApplicationId(), event.getApplicantName());

        // Previously we checked for CAMUNDA_* env vars here; the onboarding-service
        // is responsible for Camunda/SaaS credentials and starting the process engine.
        // Always attempt to call the onboarding-service REST endpoint to start the process.
        try {
            String url = UriComponentsBuilder.fromUriString(onboardingServiceUrl)
                    .pathSegment("api", "workflows", "start")
                    .build()
                    .toUriString();

            ProcessStartRequest request = new ProcessStartRequest(
                    event.getApplicationId(),
                    null,
                    event.getApplicantName(),
                    event.getEmail(),
                    "SUBMITTED",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ProcessStartRequest> httpEntity = new HttpEntity<>(request, headers);

            RestTemplate rt = new RestTemplate();
            rt.postForEntity(url, httpEntity, String.class);
            log.info("Requested onboarding-service to start process for applicationId={}", event.getApplicationId());
        } catch (Exception ex) {
            log.error("Failed to request onboarding-service to start process for applicationId={}", event.getApplicationId(), ex);
        }
    }

    /**
     * DTO for process start request to onboarding-service
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProcessStartRequest {
        private String applicationId;
        private String customerId;
        private String applicantName;
        private String email;
        private String status;
        private java.util.Map<String, Object> variables;
    }
}

