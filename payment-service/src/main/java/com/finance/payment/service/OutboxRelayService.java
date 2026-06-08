package com.finance.payment.service;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.domain.PaymentOutbox;
import com.finance.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "payment.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayService {

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.outbox.relay.batch-size:100}")
    private int batchSize;

    /**
     * TODO: Complete relay loop (see README §6.5):
     * - findPendingWithLock(batchSize)
     * - markProcessing()
     * - kafkaTemplate.send(topic, partitionKey, jsonPayload).get(5, SECONDS)
     * - markPublished() or markFailed(error)
     * - On failure: consider resetToPending() for retry vs markFailed for DLQ
     */
    @Scheduled(fixedDelayString = "${payment.outbox.relay.fixed-delay-ms:1000}")
    @Transactional
    public void relay() {
        List<PaymentOutbox> pending = outboxRepository.findPendingWithLock(batchSize);
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Relaying {} outbox events", pending.size());

        for (PaymentOutbox event : pending) {
            try {
                event.markProcessing();
                String payload = objectMapper.writeValueAsString(buildKafkaMessage(event));

                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                        .get(5, TimeUnit.SECONDS);

                event.markPublished();
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed(e.getMessage());
                // TODO: increment attempts and reset to PENDING for transient Kafka errors
            }
        }
    }

    private java.util.Map<String, Object> buildKafkaMessage(PaymentOutbox event) {
        // TODO: Align envelope with PaymentEvent in common module
        return java.util.Map.of(
                "eventType", event.getEventType(),
                "paymentId", event.getAggregateId().toString(),
                "payload", event.getPayload()
        );
    }
}
