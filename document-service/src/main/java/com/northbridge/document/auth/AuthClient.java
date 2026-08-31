package com.northbridge.document.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://localhost:8080}")
    private String authServiceUrl;

    public String validateAndGetSubject(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) throw new IllegalArgumentException("Missing Authorization header");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = restTemplate.exchange(authServiceUrl + "/api/auth/validate", HttpMethod.GET, req, Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().get("subject") != null) {
            return String.valueOf(resp.getBody().get("subject"));
        }
        throw new IllegalArgumentException("Invalid token or unable to validate");
    }
}
