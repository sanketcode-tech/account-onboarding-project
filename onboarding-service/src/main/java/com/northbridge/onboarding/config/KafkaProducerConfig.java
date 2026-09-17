package com.northbridge.onboarding.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}}")
    private String bootstrapServers;

    private String resolveBootstrap(String configured) {
        if (configured == null || configured.isBlank()) return configured;
        if (!configured.contains("localhost") && !configured.contains("127.0.0.1")) return configured;
        try {
            ProcessBuilder pb = new ProcessBuilder("wsl", "hostname", "-I");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line == null || line.isBlank()) return configured;
                String ip = line.trim().split("\\s+")[0];
                if (ip == null || ip.isBlank()) return configured;
                String resolved = ip + ":9092";
                System.out.println("Resolved WSL Kafka bootstrap: " + resolved);
                return resolved;
            }
        } catch (Exception ex) {
            System.out.println("WSL IP detection failed: " + ex.getMessage());
            return configured;
        }
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, resolveBootstrap(bootstrapServers));
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, resolveBootstrap(bootstrapServers));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "onboarding-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Keep trusted packages entry only (harmless). Removed Jackson-specific type headers/default-type properties.
        props.put("spring.json.trusted.packages", "*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // DefaultErrorHandler: retry twice with 1s backoff then skip-and-log the record to avoid consumer stall on bad messages
        org.springframework.kafka.listener.DefaultErrorHandler errorHandler = new org.springframework.kafka.listener.DefaultErrorHandler(
                (record, ex) -> {
                    System.err.println("Skipping bad record at " + (record == null ? "unknown" : (record.topic() + "-" + record.partition() + ":" + record.offset())) + " cause=" + ex.getMessage());
                },
                new org.springframework.util.backoff.FixedBackOff(1000L, 2)
        );
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

}
