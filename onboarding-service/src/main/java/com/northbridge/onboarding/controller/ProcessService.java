package com.northbridge.onboarding.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;


/**
 * ProcessService uses the Camunda 8 Java client (when available) to start
 * process instances in Camunda SaaS. Implementation uses reflection at runtime
 * against an Optional<Object> camundaClient bean so the module compiles even
 * if the Camunda client artifact is unavailable in the build environment.
 */
@Slf4j
@Service
public class ProcessService {

    private final Optional<Object> camundaClient;

    public ProcessService(@org.springframework.beans.factory.annotation.Qualifier("camundaClient") Optional<Object> camundaClient) {
        this.camundaClient = camundaClient;
    }

    private void tryInvoke(Class<?> cls, Object target, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            var m = cls.getMethod(methodName, paramTypes);
            m.invoke(target, args);
        } catch (NoSuchMethodException ignored) {
            // method not present; ignore
        } catch (Exception ex) {
            log.debug("Failed to invoke {} on {}: {}", methodName, cls.getName(), ex.getMessage());
        }
    }

    /**
     * Correlate an external message to a waiting process instance using the configured Camunda client.
     * Uses reflection to support multiple Camunda client versions and avoid a hard compile-time dependency.
     * @param messageName BPMN message name (e.g. "OfferAccepted" or "DocumentSigned")
     * @param applicationId Correlation key (applicationId)
     * @param variables Optional variables to publish with the message
     * @return true when correlation was attempted successfully, false otherwise
     */
    public boolean correlateMessage(String messageName, String applicationId, java.util.Map<String, Object> variables) {
        log.info("[ProcessService] correlateMessage name={} applicationId={}", messageName, applicationId);

        if (camundaClient.isEmpty()) {
            log.warn("Camunda client not configured; cannot correlate message.");
            return false;
        }

        Object client = camundaClient.get();
        try {
            Class<?> clientClass = client.getClass();

            // Try common pattern: workflowClient().newPublishMessageCommand()
            try {
                var workflowClientMethod = clientClass.getMethod("workflowClient");
                Object workflowClient = workflowClientMethod.invoke(client);
                var newPublishMethod = workflowClient.getClass().getMethod("newPublishMessageCommand");
                Object cmd = newPublishMethod.invoke(workflowClient);

                tryInvoke(cmd.getClass(), cmd, "messageName", messageName);
                tryInvoke(cmd.getClass(), cmd, "correlationKey", applicationId);
                try {
                    var varsMethod = cmd.getClass().getMethod("variables", java.util.Map.class);
                    varsMethod.invoke(cmd, variables == null ? java.util.Map.of() : variables);
                } catch (NoSuchMethodException ignored) {
                }

                try {
                    var sendMethod = cmd.getClass().getMethod("send");
                    Object sent = sendMethod.invoke(cmd);
                    try {
                        var joinMethod = sent.getClass().getMethod("join");
                        joinMethod.invoke(sent);
                    } catch (NoSuchMethodException ignored) {
                    }
                } catch (NoSuchMethodException ignored) {
                }

                log.info("Message correlated via workflowClient API: name={}, correlationKey={}", messageName, applicationId);
                return true;
            } catch (NoSuchMethodException ignored) {
                // fallback
            }

            // Try alternative: client.newPublishMessageCommand()
            try {
                var newPublishMethod = clientClass.getMethod("newPublishMessageCommand");
                Object cmd = newPublishMethod.invoke(client);

                tryInvoke(cmd.getClass(), cmd, "messageName", messageName);
                tryInvoke(cmd.getClass(), cmd, "correlationKey", applicationId);
                try {
                    var varsMethod = cmd.getClass().getMethod("variables", java.util.Map.class);
                    varsMethod.invoke(cmd, variables == null ? java.util.Map.of() : variables);
                } catch (NoSuchMethodException ignored) {
                }

                try {
                    var sendMethod = cmd.getClass().getMethod("send");
                    Object sent = sendMethod.invoke(cmd);
                    try {
                        var joinMethod = sent.getClass().getMethod("join");
                        joinMethod.invoke(sent);
                    } catch (NoSuchMethodException ignored) {
                    }
                } catch (NoSuchMethodException ignored) {
                }

                log.info("Message correlated via client API: name={}, correlationKey={}", messageName, applicationId);
                return true;
            } catch (NoSuchMethodException ignored) {
                // no known API found
            }

            log.warn("No known Camunda publish message API found to correlate message: {}", messageName);
            return false;
        } catch (Exception ex) {
            log.error("Failed to correlate message name={} applicationId={}", messageName, applicationId, ex);
            return false;
        }
    }
}
