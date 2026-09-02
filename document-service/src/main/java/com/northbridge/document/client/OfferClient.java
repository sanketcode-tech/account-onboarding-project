package com.northbridge.document.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client to query application-service for the owner/customerId of an application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfferClient {

    private final RestTemplate restTemplate;

    @Value("${application.service.url:http://localhost:8082}")
    private String applicationServiceUrl;

    public String getCustomerIdForApplication(String applicationId, String authorization) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (authorization != null && !authorization.isBlank()) {
            // Ensure Authorization header uses Bearer prefix if caller sent raw token
            String value = authorization.trim();
            if (!value.toLowerCase().startsWith("bearer ")) {
                value = "Bearer " + value;
            }
            headers.set(org.springframework.http.HttpHeaders.AUTHORIZATION, value);
            log.debug("Forwarding Authorization header to application-service for applicationId={}", applicationId);
        } else {
            log.debug("No Authorization header available to forward for applicationId={}", applicationId);
        }

        org.springframework.http.HttpEntity<Void> req = new org.springframework.http.HttpEntity<>(headers);
        org.springframework.http.ResponseEntity<Map> resp = restTemplate.exchange(applicationServiceUrl + "/api/offers/" + applicationId, org.springframework.http.HttpMethod.GET, req, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
        Object cid = resp.getBody().get("customerId");
        return cid == null ? null : String.valueOf(cid);
    }
}
