package com.northbridge.common.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that attempts to auto-detect a WSL IP address when
 * SPRING_KAFKA_BOOTSTRAP_SERVERS / KAFKA_BOOTSTRAP_SERVERS are not provided.
 *
 * This helps Windows developers running Kafka inside WSL2: instead of requiring
 * a manual script, the application will try to call `wsl hostname -I` at
 * startup and use the first IP returned as the bootstrap server host (port 9092).
 *
 * If WSL is not available or detection fails, this processor does nothing.
 */
public class WslKafkaEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SPRING_PROP = "spring.kafka.bootstrap-servers";
    private static final String LEGACY_PROP = "KAFKA_BOOTSTRAP_SERVERS";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String existing = environment.getProperty(SPRING_PROP);
        String existingLegacy = environment.getProperty(LEGACY_PROP);
        if (existing != null && !existing.isBlank()) return;
        if (existingLegacy != null && !existingLegacy.isBlank()) return;

        try {
        // Try variants: "wsl" (default) and "wsl.exe" for robustness
        String[] cmds = new String[]{"wsl", "wsl.exe"};
        String outputLine = null;
        for (String cmd : cmds) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "hostname", "-I");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = r.readLine();
                    if (line != null && !line.isBlank()) {
                        outputLine = line.trim();
                        break;
                    }
                }
            } catch (Exception e) {
                // try next
            }
        }

        if (outputLine == null || outputLine.isBlank()) {
            System.out.println("WSL hostname detection returned empty output or 'wsl' not available");
            return;
            }

        // hostname -I may return multiple addresses; use first
        String wslIp = outputLine.split("\\s+")[0];
        if (wslIp != null && !wslIp.isBlank()) {
            String kafka = wslIp + ":9092";
            Map<String, Object> m = new HashMap<>();
            m.put(SPRING_PROP, kafka);
            m.put(LEGACY_PROP, kafka);
            MutablePropertySources sources = environment.getPropertySources();
            MapPropertySource ps = new MapPropertySource("wsl-kafka-bootstrap", m);
            // Put with highest precedence so developer-provided env vars still win
            sources.addFirst(ps);
            System.out.println("Injected WSL Kafka bootstrap servers: " + kafka);
        }
        } catch (Exception ex) {
            System.out.println("WSL Kafka detection failed (wsl may not be available): " + ex.getMessage());
        }
    }
}

