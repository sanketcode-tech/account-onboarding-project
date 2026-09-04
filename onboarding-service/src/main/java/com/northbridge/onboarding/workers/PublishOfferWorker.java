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
        // Default to a non-zero offered limit for testing when none is provided
        BigDecimal offeredLimit = BigDecimal.valueOf(1000.00);

        Object limitObj = vars.get("offeredLimit");
        if (limitObj != null) {
            try {
                offeredLimit = new BigDecimal(String.valueOf(limitObj));
            } catch (Exception ex) {
                log.warn("Failed to parse offeredLimit from variables: {} — using default {}", limitObj, offeredLimit);
            }
        }

        String customerId = String.valueOf(vars.getOrDefault("customerId", null));
        if ("null".equals(customerId)) customerId = null;
        OfferReadyEvent event = new OfferReadyEvent(applicationId, offerId, customerId, offeredLimit, java.time.Instant.now());
        kafkaTemplate.send("offer.ready", applicationId, event);

        log.info("[PublishOfferWorker] published OfferReadyEvent for applicationId={} to topic offer.ready", applicationId);

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("offerPublished", true, "offerId", offerId, "offeredLimit", offeredLimit != null ? offeredLimit.doubleValue() : null))
                .send()
                .join();
    }
}