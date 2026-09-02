package com.northbridge.document.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
            headers.set(org.springframework.http.HttpHeaders.AUTHORIZATION, authorization);
        }
        org.springframework.http.HttpEntity<Void> req = new org.springframework.http.HttpEntity<>(headers);
        org.springframework.http.ResponseEntity<Map> resp = restTemplate.exchange(applicationServiceUrl + "/api/offers/" + applicationId, org.springframework.http.HttpMethod.GET, req, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
        Object cid = resp.getBody().get("customerId");
        return cid == null ? null : String.valueOf(cid);
    }
}
