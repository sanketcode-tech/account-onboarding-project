package com.northbridge.onboarding.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Legacy no-op placeholder; job workers are registered via @JobWorker annotations
 * on the worker classes in com.northbridge.onboarding.workers.
 */
@Slf4j
@Configuration
public class ZeebeWorkerRegistrar {
    public ZeebeWorkerRegistrar() {
        log.info("[ZeebeWorkerRegistrar] using @JobWorker registration path");
    }
}
