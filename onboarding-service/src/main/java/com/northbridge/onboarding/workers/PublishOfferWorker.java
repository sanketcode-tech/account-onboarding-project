package com.northbridge.onboarding.workers;

import com.northbridge.common.events.OfferReadyEvent;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

/**
 * Camunda worker that publishes OfferReadyEvent to Kafka when an offer is produced.
 */
@Slf4j
@Component
public class PublishOfferWorker {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PublishOfferWorker(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @JobWorker(type = "publish-offer")
    public void publishOffer(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));
        String offerId = "OFFER-" + java.util.UUID.randomUUID();
        // Read offeredLimit from process variables (DMN should compute this). Do not hardcode a test default here.
        BigDecimal offeredLimit = null;

        Object limitObj = vars.get("offeredLimit");
        if (limitObj == null) {
            // Fallback: DMN result may be returned as eligibilityResult object containing offeredLimit
            Object eligibilityObj = vars.get("eligibilityResult");
            if (eligibilityObj instanceof Map) {
                Object nested = ((Map<?, ?>) eligibilityObj).get("offeredLimit");
                if (nested != null) limitObj = nested;
            } else if (eligibilityObj instanceof String) {
                try {
                    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                    Map<?, ?> parsed = mapper.readValue(String.valueOf(eligibilityObj), Map.class);
                    if (parsed.get("offeredLimit") != null) limitObj = parsed.get("offeredLimit");
                } catch (Exception ex) {
                    log.debug("Failed to parse eligibilityResult JSON for applicationId={}: {}", applicationId, ex.getMessage());
                }
            }
        }

        if (limitObj != null) {
            try {
                offeredLimit = new BigDecimal(String.valueOf(limitObj));
            } catch (Exception ex) {
                log.warn("Failed to parse offeredLimit from variables: {} — leaving offeredLimit null", limitObj);
            }
        } else {
            log.warn("No offeredLimit process variable present for applicationId={}; downstream consumers may receive a null limit", applicationId);
        }

        String customerId = String.valueOf(vars.getOrDefault("customerId", null));
        if ("null".equals(customerId)) customerId = null;
        OfferReadyEvent event = new OfferReadyEvent(applicationId, offerId, customerId, offeredLimit, java.time.Instant.now());
        kafkaTemplate.send("offer.ready", applicationId, event);

        log.info("[PublishOfferWorker] published OfferReadyEvent for applicationId={} to topic offer.ready", applicationId);

        // Build completion variables in a null-safe way (Map.of does not accept null values)
        Map<String, Object> completionVars = new HashMap<>();
        completionVars.put("offerPublished", true);
        completionVars.put("offerId", offerId);
        if (offeredLimit != null) {
            completionVars.put("offeredLimit", offeredLimit.doubleValue());
        }

        client.newCompleteCommand(job.getKey())
                .variables(completionVars)
                .send()
                .join();
    }
}