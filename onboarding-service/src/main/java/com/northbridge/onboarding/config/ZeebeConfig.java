package com.northbridge.onboarding.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Legacy placeholder: kept to avoid bean resolution issues for classes that
 * reference `com.northbridge.onboarding.config.ZeebeConfig` elsewhere.
 * Real Camunda 8 client configuration is provided by `CamundaClientConfig`.
 */
@Slf4j
@Configuration
public class ZeebeConfig {
    // Intentionally left blank.
}

