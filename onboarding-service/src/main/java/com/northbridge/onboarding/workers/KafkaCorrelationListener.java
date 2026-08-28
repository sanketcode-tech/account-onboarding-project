package com.northbridge.onboarding.workers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Legacy listener kept as a no-op placeholder.
 * Message correlation is handled by KafkaMessageCorrelator to ensure a single
 * consumer for each topic and avoid duplicate processing.
 */
@Slf4j
@Component
public class KafkaCorrelationListener {
    public KafkaCorrelationListener() {
        log.info("[KafkaCorrelationListener] legacy listener placeholder initialized");
    }
}
