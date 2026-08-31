package com.northbridge.auth.controller;

import com.northbridge.auth.dto.*;
import com.northbridge.auth.security.JwtAuthenticationToken;
import com.northbridge.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Authentication API controller
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        UserProfileResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user profile (requires JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Long userId = jwtAuth.getUserId();

        log.info("Getting profile for user ID: {}", userId);
        UserProfileResponse response = authService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate JWT token and return subject (email) if valid
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) token = authorization.substring(7);
        if (token == null || token.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header");
        if (!authService.isTokenValid(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        String subject = authService.getSubjectFromToken(token);
        return ResponseEntity.ok().body(Map.of("subject", subject));
    }
}

