package com.finance.payment.service;

import com.finance.payment.config.PaymentEventMapper;
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

/**
 * Interroga {@code payment_outbox} e pubblica gli eventi in sospeso su Kafka (consegna at-least-once).
 * <p>
 * Usa {@code SELECT FOR UPDATE SKIP LOCKED} per consentire più istanze relay in parallelo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "payment.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayService {

    private static final int MAX_ATTEMPTS = 5;

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentEventMapper eventMapper;

    @Value("${payment.outbox.relay.batch-size:100}")
    private int batchSize;

    /** Ciclo schedulato: pubblica un batch di eventi outbox in sospeso. */
    @Scheduled(fixedDelayString = "${payment.outbox.relay.fixed-delay-ms:1000}")
    @Transactional
    public void relay() {
        List<PaymentOutbox> pending = outboxRepository.findPendingWithLock(batchSize);
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Relaying {} outbox events", pending.size());

        for (PaymentOutbox event : pending) {
            relaySingle(event);
        }
    }

    private void relaySingle(PaymentOutbox event) {
        try {
            event.markProcessing();
            String json = eventMapper.outboxToJson(event.getEventType(), event.getAggregateId(), event.getPayload());

            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), json)
                    .get(5, TimeUnit.SECONDS);

            event.markPublished();
            log.debug("Published outbox event id={} type={}", event.getId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
            if (event.getAttempts() < MAX_ATTEMPTS) {
                event.resetToPending();
                log.info("Outbox event {} scheduled for retry (attempt {})", event.getId(), event.getAttempts());
            } else {
                event.markFailed(e.getMessage());
            }
        }
    }
}
