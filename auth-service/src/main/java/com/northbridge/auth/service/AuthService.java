package com.northbridge.auth.service;

import com.northbridge.auth.dto.*;
import com.northbridge.auth.entity.UserEntity;
import com.northbridge.auth.repository.UserRepository;
import com.northbridge.auth.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service
 */
@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return mapToProfileResponse(user);
    }

    /**
     * Login user and return JWT token
     */
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new IllegalArgumentException("User account is inactive");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtUtil.getExpirationTimeMs())
                .user(AuthResponse.UserProfile.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .build())
                .build();
    }

    /**
     * Get user profile by ID
     */
    public UserProfileResponse getUserProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToProfileResponse(user);
    }

    /**
     * Map UserEntity to UserProfileResponse
     */
    private UserProfileResponse mapToProfileResponse(UserEntity user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .active(user.getActive())
                .build();
    }
}

