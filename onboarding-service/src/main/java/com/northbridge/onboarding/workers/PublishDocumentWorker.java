package com.northbridge.onboarding.workers;

import com.northbridge.common.events.DocumentSigningRequestEvent;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PublishDocumentWorker {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PublishDocumentWorker(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @JobWorker(type = "publish-document")
    public void publishDocument(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String applicationId = String.valueOf(vars.getOrDefault("applicationId", ""));
        String documentId = String.valueOf(vars.getOrDefault("documentId", "DOC-" + java.util.UUID.randomUUID()));

        DocumentSigningRequestEvent event = new DocumentSigningRequestEvent(applicationId, documentId, java.time.Instant.now());
        kafkaTemplate.send("document.requested", applicationId, event);

        log.info("[PublishDocumentWorker] published DocumentSigningRequestEvent for applicationId={} to topic document.requested", applicationId);

        client.newCompleteCommand(job.getKey())
                .variables(Map.of("documentPublished", true, "documentId", documentId))
                .send()
                .join();
    }
}