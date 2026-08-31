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

    @Value("${application.service.url:http://localhost:8081}")
    private String applicationServiceUrl;

    public String getCustomerIdForApplication(String applicationId) {
        ResponseEntity<Map> resp = restTemplate.getForEntity(applicationServiceUrl + "/api/offers/" + applicationId, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
        Object cid = resp.getBody().get("customerId");
        return cid == null ? null : String.valueOf(cid);
    }
}
