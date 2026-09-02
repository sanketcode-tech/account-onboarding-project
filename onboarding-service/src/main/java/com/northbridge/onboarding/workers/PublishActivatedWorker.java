package com.northbridge.onboarding.workers;

import com.northbridge.common.events.AccountActivatedEvent;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Camunda worker that publishes AccountActivatedEvent after account provisioning completes.
 */
@Slf4j
@Component
public class PublishActivatedWorker {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PublishActivatedWorker(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @JobWorker(type = "publish-activated")
    public void publishActivated(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));
        String accountId = String.valueOf(vars.getOrDefault("accountId", "ACCT-" + java.util.UUID.randomUUID()));

        AccountActivatedEvent event = new AccountActivatedEvent(applicationId, accountId, java.time.Instant.now());
        kafkaTemplate.send("account.activated", applicationId, event);

        log.info("[PublishActivatedWorker] published AccountActivatedEvent for applicationId={} to topic account.activated", applicationId);

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("accountPublished", true, "accountId", accountId))
                .send()
                .join();
    }
}