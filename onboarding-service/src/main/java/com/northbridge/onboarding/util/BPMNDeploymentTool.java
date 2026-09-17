package com.northbridge.onboarding.util;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Manual BPMN Deployment Tool for Camunda 8 SaaS
 *
 * This utility validates and deploys BPMN files to Camunda 8 SaaS using the Java Client.
 *
 * Usage:
 *   java -cp ".:target/classes:target/lib/*" com.northbridge.onboarding.util.BPMNDeploymentTool \
 *        --file /path/to/current-account-onboarding.bpmn \
 *        --cluster-id <your-camunda-cluster-id> \
 *        --region <your-camunda-region> \
 *        --client-id <your-camunda-client-id> \
 *        --client-secret "<your-camunda-client-secret>" \
 *        (or run with --env true to load CAMUNDA_* values from environment variables)
 */
@Slf4j
public class BPMNDeploymentTool {

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            DeploymentConfig config = parseArguments(args);

            if (config == null) {
                printUsage();
                System.exit(1);
            }

            log.info("========================================");
            log.info("BPMN Deployment Tool - Camunda 8 SaaS");
            log.info("========================================");

            // Step 1: Validate BPMN file
            log.info("\n[Step 1] Validating BPMN file: {}", config.bpmnFile);
            BPMNValidator.BPMNValidationResult validationResult = validateBPMN(config.bpmnFile);
            log.info("{}", validationResult);

            if (!validationResult.isValid) {
                log.error("BPMN validation failed. Cannot proceed with deployment.");
                System.exit(1);
            }

            // Step 2: Deploy to Camunda 8 SaaS
            log.info("\n[Step 2] Deploying BPMN to Camunda 8 SaaS");
            log.info("  Cluster ID: {}", config.clusterId);
            log.info("  Region: {}", config.region);
            log.info("  Client ID: {}***", config.clientId.substring(0, Math.min(10, config.clientId.length())));

            deployToCamunda(config);

            log.info("\n========================================");
            log.info("✓ Deployment completed successfully!");
            log.info("========================================\n");

        } catch (Exception e) {
            log.error("Deployment failed with error:", e);
            System.exit(1);
        }
    }

    /**
     * Validates the BPMN file
     */
    private static BPMNValidator.BPMNValidationResult validateBPMN(String bpmnFile) throws Exception {
        try (InputStream inputStream = Files.newInputStream(Paths.get(bpmnFile))) {
            return BPMNValidator.validate(inputStream);
        }
    }

    /**
     * Deploys the BPMN to Camunda 8 SaaS using the Java Client
     */
    private static void deployToCamunda(DeploymentConfig config) throws Exception {
        try {
            log.info("Attempting to create Camunda client...");

            // Use reflection to load and instantiate Camunda client dynamically
            // This avoids direct compile-time dependency on Camunda client classes
            Class<?> camundaClientBuilderClass = Class.forName("io.camunda.client.CamundaClientBuilder");

            Object builder = camundaClientBuilderClass.getMethod("newClientBuilder").invoke(null);

            // Configure cloud connection
            builder.getClass().getMethod("withCloudRegion", String.class).invoke(builder, config.region);
            builder.getClass().getMethod("withClusterId", String.class).invoke(builder, config.clusterId);
            builder.getClass().getMethod("withClientId", String.class).invoke(builder, config.clientId);
            builder.getClass().getMethod("withClientSecret", String.class).invoke(builder, config.clientSecret);

            // Build the client
            Object camundaClient = builder.getClass().getMethod("build").invoke(builder);

            log.info("✓ Camunda client created successfully");

            // Prepare BPMN file for deployment
            log.info("Reading BPMN file from disk...");
            byte[] bpmnContent = Files.readAllBytes(Paths.get(config.bpmnFile));
            String filename = Paths.get(config.bpmnFile).getFileName().toString();

            log.info("Deploying BPMN resource: {} ({} bytes)", filename, bpmnContent.length);

            // Get the deployments manager
            Object deploymentsManager = camundaClient.getClass()
                .getMethod("newDeployResourceCommand").invoke(camundaClient);

            // Add resource
            Object deploymentWithResource = deploymentsManager.getClass()
                .getMethod("addResourceBytes", byte[].class, String.class)
                .invoke(deploymentsManager, bpmnContent, filename);

            // Send deployment
            Object response = deploymentWithResource.getClass()
                .getMethod("send").invoke(deploymentWithResource);

            // Extract deployment key from response
            String deploymentKey = response.getClass().getMethod("key").invoke(response).toString();
            log.info("✓ BPMN deployed successfully!");
            log.info("  Deployment Key: {}", deploymentKey);

            // List deployed processes
            Object processes = response.getClass().getMethod("processes").invoke(response);
            log.info("  Processes deployed: {}", processes);

            // Close the client
            if (camundaClient instanceof AutoCloseable) {
                ((AutoCloseable) camundaClient).close();
                log.info("✓ Camunda client closed");
            }

        } catch (ClassNotFoundException e) {
            log.error("Camunda client classes not found on classpath. Make sure the dependency is properly configured.");
            log.error("Add the following dependency to pom.xml:");
            log.error("  <groupId>io.camunda</groupId>");
            log.error("  <artifactId>camunda-spring-boot-starter</artifactId>");
            throw new RuntimeException("Camunda client not available", e);
        } catch (NoSuchMethodException e) {
            log.error("Camunda client API method not found. Possible version mismatch.");
            throw new RuntimeException("Camunda client API error", e);
        } catch (Exception e) {
            log.error("Failed to deploy BPMN to Camunda:", e);
            throw e;
        }
    }

    /**
     * Parses command-line arguments
     */
    private static DeploymentConfig parseArguments(String[] args) {
        DeploymentConfig config = new DeploymentConfig();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file":
                    config.bpmnFile = (i + 1 < args.length) ? args[++i] : null;
                    break;
                case "--cluster-id":
                    config.clusterId = (i + 1 < args.length) ? args[++i] : null;
                    break;
                case "--region":
                    config.region = (i + 1 < args.length) ? args[++i] : null;
                    break;
                case "--client-id":
                    config.clientId = (i + 1 < args.length) ? args[++i] : null;
                    break;
                case "--client-secret":
                    config.clientSecret = (i + 1 < args.length) ? args[++i] : null;
                    break;
                case "--env":
                    // Load from environment variables
                    if (i + 1 < args.length && "true".equals(args[++i])) {
                        config.loadFromEnvironment();
                    }
                    break;
            }
        }

        // Validate required parameters
        if (config.bpmnFile == null || config.bpmnFile.isEmpty()) {
            log.error("Missing required parameter: --file");
            return null;
        }
        if (config.clusterId == null || config.clusterId.isEmpty()) {
            log.error("Missing required parameter: --cluster-id");
            return null;
        }
        if (config.region == null || config.region.isEmpty()) {
            log.error("Missing required parameter: --region");
            return null;
        }
        if (config.clientId == null || config.clientId.isEmpty()) {
            log.error("Missing required parameter: --client-id");
            return null;
        }
        if (config.clientSecret == null || config.clientSecret.isEmpty()) {
            log.error("Missing required parameter: --client-secret");
            return null;
        }

        return config;
    }

    /**
     * Prints usage information
     */
    private static void printUsage() {
        System.out.println("\n=== BPMN Deployment Tool Usage ===\n");
        System.out.println("java com.northbridge.onboarding.util.BPMNDeploymentTool \\");
        System.out.println("  --file <path>           Path to BPMN file");
        System.out.println("  --cluster-id <id>       Camunda cluster ID");
        System.out.println("  --region <region>       Camunda region (e.g., sin-2, us-1)");
        System.out.println("  --client-id <id>        Client ID for authentication");
        System.out.println("  --client-secret <secret> Client secret for authentication");
        System.out.println("  --env true              Load credentials from environment variables");
        System.out.println("\nExample:");
        System.out.println("java com.northbridge.onboarding.util.BPMNDeploymentTool \\");
        System.out.println("  --file current-account-onboarding.bpmn \\");
        System.out.println("  --cluster-id <your-camunda-cluster-id> \\");
        System.out.println("  --region <your-camunda-region> \\");
        System.out.println("  --client-id <your-camunda-client-id> \\");
        System.out.println("  --client-secret \"<your-camunda-client-secret>\"\n");
    }

    /**
     * Configuration for BPMN deployment
     */
    static class DeploymentConfig {
        String bpmnFile;
        String clusterId;
        String region;
        String clientId;
        String clientSecret;

        void loadFromEnvironment() {
            this.clusterId = System.getenv("CAMUNDA_CLUSTER_ID");
            this.region = System.getenv("CAMUNDA_REGION");
            this.clientId = System.getenv("CAMUNDA_CLIENT_ID");
            this.clientSecret = System.getenv("CAMUNDA_CLIENT_SECRET");
        }
    }
}

