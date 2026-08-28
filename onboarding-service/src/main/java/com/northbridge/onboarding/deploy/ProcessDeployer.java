package com.northbridge.onboarding.deploy;

import io.camunda.client.CamundaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Deploys the BPMN model from the classpath to Camunda 8 SaaS during startup.
 */
@Slf4j
@Component
public class ProcessDeployer {

    private final Optional<CamundaClient> camundaClient;
    private final boolean deployEnabled;
    private final String bpmnPath;
    private final String formPaths;

    public ProcessDeployer(
            @Qualifier("camundaClient") Optional<CamundaClient> camundaClient,
            @Value("${process.deployer.enabled:false}") boolean deployEnabled,
            @Value("${bpmn.resource.path:/bpmn/current-account-onboarding.bpmn}") String bpmnPath,
            @Value("${forms.resource.paths:/forms/signing-ceremony-form.form,/forms/provisioning-approval-form.form}") String formPaths) {
        this.camundaClient = camundaClient;
        this.deployEnabled = deployEnabled;
        this.bpmnPath = bpmnPath;
        this.formPaths = formPaths;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!deployEnabled) {
            log.info("ProcessDeployer is disabled (process.deployer.enabled=false). Skipping BPMN deployment.");
            return;
        }

        if (camundaClient.isEmpty()) {
            log.warn("Camunda client not available; cannot deploy BPMN resources.");
            return;
        }

        String classpathResource = bpmnPath.startsWith("/") ? bpmnPath.substring(1) : bpmnPath;
        log.info("Deploying BPMN resource '{}' to Camunda 8 SaaS", classpathResource);

        try {
            var deployCmd = camundaClient.get().newDeployResourceCommand()
                    .addResourceFromClasspath(classpathResource);

            if (formPaths != null && !formPaths.isBlank()) {
                for (String p : formPaths.split(",")) {
                    String path = p.trim();
                    if (!path.isEmpty()) {
                        String cp = path.startsWith("/") ? path.substring(1) : path;
                        deployCmd.addResourceFromClasspath(cp);
                    }
                }
            }

            deployCmd.send().join();
            log.info("Successfully deployed resources: BPMN '{}' and forms: {}", classpathResource, formPaths);
        } catch (Exception ex) {
            log.error("Failed to deploy resources to Camunda 8 SaaS. BPMN: {}, forms: {}", classpathResource, formPaths, ex);
        }
    }
}

