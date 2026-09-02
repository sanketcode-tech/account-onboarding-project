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

    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

    private String normalizeAuth(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        String v = bearerToken.trim();
        if (!v.toLowerCase().startsWith("bearer ")) v = "Bearer " + v;
        return v;
    }

    public String validateAndGetSubject(String bearerToken) {
        String header = normalizeAuth(bearerToken);
        if (header == null) throw new IllegalArgumentException("Missing Authorization header");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, header);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = restTemplate.exchange(authServiceUrl + "/api/auth/validate", HttpMethod.GET, req, Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().get("subject") != null) {
            return String.valueOf(resp.getBody().get("subject"));
        }
        throw new IllegalArgumentException("Invalid token or unable to validate");
    }

    public String getUserFullName(String bearerToken) {
        String header = normalizeAuth(bearerToken);
        if (header == null) throw new IllegalArgumentException("Missing Authorization header");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, header);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = restTemplate.exchange(authServiceUrl + "/api/auth/me", HttpMethod.GET, req, Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().get("fullName") != null) {
            return String.valueOf(resp.getBody().get("fullName"));
        }
        throw new IllegalArgumentException("Unable to fetch user profile");
    }

    /**
     * Best-effort role check by decoding JWT payload (no signature validation).
     * Used only for authorizing officer users when upstream service rejects owner check.
     */
    public boolean tokenHasRole(String bearerToken, String role) {
        String header = normalizeAuth(bearerToken);
        if (header == null) return false;
        try {
            String token = header.substring(7); // strip 'Bearer '
            String[] parts = token.split("\\.");
            if (parts.length < 2) return false;
            String payload = parts[1];
            // Base64 URL decode
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(payload);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            Map map = new tools.jackson.databind.ObjectMapper().readValue(json, Map.class);
            if (map.get("roles") instanceof java.util.List) {
                for (Object r : (java.util.List) map.get("roles")) {
                    if (String.valueOf(r).equalsIgnoreCase(role)) return true;
                }
            }
            if (map.get("role") != null && String.valueOf(map.get("role")).equalsIgnoreCase(role)) return true;
            if (map.get("authorities") instanceof java.util.List) {
                for (Object r : (java.util.List) map.get("authorities")) {
                    if (String.valueOf(r).equalsIgnoreCase(role)) return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.debug("Failed to decode JWT for role check", ex);
            return false;
        }
    }
}
