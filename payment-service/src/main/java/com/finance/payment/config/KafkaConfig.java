package com.finance.payment.config;

import com.finance.payment.common.kafka.KafkaDefaults;
import com.finance.payment.common.kafka.TopicConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Configurazione Kafka del payment-service: topic, producer e template. */
@Configuration
public class KafkaConfig {

    /**
     * Provisioning dichiarativo dei topic — preferito a KAFKA_AUTO_CREATE_TOPICS_ENABLE.
     * Chiave di partizione = paymentId garantisce l'ordinamento per saga di pagamento.
     */
    @Bean
    NewTopic paymentEventsTopic() {
        return TopicBuilder.name(TopicConstants.PAYMENT_EVENTS)
                .partitions(KafkaDefaults.PAYMENT_EVENTS_PARTITIONS)
                .replicas(KafkaDefaults.REPLICATION_FACTOR_LOCAL)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    @Bean
    NewTopic paymentEventsDltTopic() {
        return TopicBuilder.name(TopicConstants.PAYMENT_EVENTS_DLT)
                .partitions(KafkaDefaults.DLT_PARTITIONS)
                .replicas(KafkaDefaults.REPLICATION_FACTOR_LOCAL)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(14).toMillis()))
                .build();
    }

    @Bean
    ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, KafkaDefaults.PRODUCER_ACKS);
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, KafkaDefaults.PRODUCER_MAX_IN_FLIGHT);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        return template;
    }
}
